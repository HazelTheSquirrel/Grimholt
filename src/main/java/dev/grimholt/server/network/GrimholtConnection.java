package dev.grimholt.server.network;

import dev.grimholt.api.*;
import dev.grimholt.server.api.GrimholtServerImpl;
import dev.grimholt.server.command.GrimholtCommandDispatcher;
import dev.grimholt.server.vanilla.*;
import dev.grimholt.server.logging.Logging;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

public final class GrimholtConnection implements CommandSender, AutoCloseable {
    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(1);
    private static final int LOGIN_COMPRESSION_THRESHOLD = 256;
    private static final int PRE_PLAY_IDLE_TIMEOUT_MILLIS = 30_000;
    private final Socket socket;
    private final GrimholtServerImpl server;
    private final GrimholtCommandDispatcher commands;
    private final VanillaPacketCatalog catalog;
    private final VanillaGeneratedData generated;
    private final VanillaServerKernel kernel;
    private final GrimholtPacketTransport transport;
    private final VanillaConfigurationSync configuration;
    private final GrimholtPlayProtocol playProtocol = new GrimholtPlayProtocol();
    private final VanillaChunkWireCodec chunkCodec = new VanillaChunkWireCodec();
    private final GrimholtCommandTreeWire commandTreeWire = new GrimholtCommandTreeWire();
    private final GrimholtPlayerInventory inventory = new GrimholtPlayerInventory();
    private final Consumer<GrimholtConnection> closed;
    private final GrimholtPacketRateLimiter inboundRateLimiter = GrimholtPacketRateLimiter.defaults();
    private final boolean onlineMode;
    private final GrimholtOnlineAuthentication authentication;
    private volatile boolean authenticated;
    private final AtomicBoolean closing = new AtomicBoolean();
    private final int entityId = NEXT_ENTITY_ID.getAndIncrement();
    private final AtomicInteger nextTeleportId = new AtomicInteger(1);
    private volatile int pendingTeleportId = -1;
    private volatile ConnectionState state = ConnectionState.HANDSHAKE;
    private volatile UUID uuid;
    private volatile String username;
    private volatile Position position = new Position(0, 64, 0, 0, 0);

    public GrimholtConnection(Socket socket, GrimholtServerImpl server, GrimholtCommandDispatcher commands,
                              VanillaPacketCatalog catalog, VanillaGeneratedData generated,
                              VanillaServerKernel kernel, boolean onlineMode,
                              Consumer<GrimholtConnection> closed) throws IOException {
        this.socket = Objects.requireNonNull(socket);
        this.server = Objects.requireNonNull(server);
        this.commands = Objects.requireNonNull(commands);
        this.catalog = Objects.requireNonNull(catalog);
        this.generated = Objects.requireNonNull(generated);
        this.kernel = Objects.requireNonNull(kernel);
        this.onlineMode = onlineMode;
        this.authentication = onlineMode ? new GrimholtOnlineAuthentication() : null;
        this.closed = Objects.requireNonNull(closed);
        this.transport = new GrimholtPacketTransport(socket, 2 * 1024 * 1024);
        this.configuration = new VanillaConfigurationSync(generated);
    }

    public void run() {
        String remote = String.valueOf(socket.getRemoteSocketAddress());
        Logging.connectionOpened(remote);
        String closeReason = "peer disconnected";
        try {
            socket.setTcpNoDelay(true);
            while (!closing.get()) {
                // Unauthenticated or configuring clients must not hold a socket
                // forever. Once in PLAY, vanilla keepalive handling owns liveness.
                socket.setSoTimeout(state == ConnectionState.PLAY ? 0 : PRE_PLAY_IDLE_TIMEOUT_MILLIS);
                VanillaProtocol.Frame frame = transport.read();
                if (!inboundRateLimiter.tryAcquire(frame.payload().length)) {
                    throw new IOException("Inbound packet rate limit exceeded");
                }
                handle(frame);
            }
        } catch (EOFException ignored) {
            closeReason = "peer disconnected";
        } catch (java.net.SocketTimeoutException timeout) {
            closeReason = "idle timeout";
            Logging.connectionFailure(remote, state.name(), timeout);
        } catch (IOException | RuntimeException failure) {
            closeReason = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            Logging.connectionFailure(remote, state.name(), failure);
        } finally {
            Logging.connectionClosed(remote, state.name(), closeReason);
            close();
        }
    }

    private void handle(VanillaProtocol.Frame frame) throws IOException {
        switch (state) {
            case HANDSHAKE -> handleHandshake(frame);
            case STATUS -> handleStatus(frame);
            case LOGIN -> handleLogin(frame);
            case CONFIGURATION -> handleConfiguration(frame);
            case PLAY -> handlePlay(frame);
            case CLOSED -> throw new EOFException("connection closed");
        }
    }

    private void handleHandshake(VanillaProtocol.Frame frame) throws IOException {
        VanillaProtocol.Handshake handshake = VanillaProtocol.decodeHandshake(frame);
        state = handshake.nextState() == 1 ? ConnectionState.STATUS : ConnectionState.LOGIN;
    }

    private void handleStatus(VanillaProtocol.Frame frame) throws IOException {
        if (frame.packetId() == 0) {
            if (frame.payload().length != 0) throw new IOException("Status request packet must be empty");
            String json = "{\"version\":{\"name\":\"" + VanillaSnapshot.VERSION + "\",\"protocol\":" + VanillaSnapshot.PROTOCOL +
                    "},\"players\":{\"max\":" + server.maxPlayers() + ",\"online\":" + server.players().size() +
                    "},\"description\":{\"text\":\"Grimholt\"}}";
            send(VanillaProtocol.State.STATUS, "minecraft:status_response",
                    out -> VanillaProtocolCodec.writeString(out, json, 32767));
        } else if (frame.packetId() == 1) {
            if (frame.payload().length != Long.BYTES) throw new IOException("Ping packet must contain exactly 8 bytes");
            long payload = new DataInputStream(new ByteArrayInputStream(frame.payload())).readLong();
            String pongPacket = List.of("minecraft:pong_response", "minecraft:pong").stream()
                    .filter(candidate -> catalog.id(VanillaProtocol.State.STATUS,
                            VanillaProtocol.Direction.CLIENTBOUND, candidate).isPresent())
                    .findFirst()
                    .orElseThrow(() -> new IOException("26.4 status pong packet is missing from Mojang packet catalog"));
            send(VanillaProtocol.State.STATUS, pongPacket,
                    out -> new DataOutputStream(out).writeLong(payload));
        } else throw new IOException("Unknown status packet: " + frame.packetId());
    }

    private void handleLogin(VanillaProtocol.Frame frame) throws IOException {
        if (frame.packetId() == 0) {
            ByteArrayInputStream in = new ByteArrayInputStream(frame.payload());
            username = VanillaProtocolCodec.readString(in, 16);
            if (!username.matches("[A-Za-z0-9_]{1,16}")) throw new IOException("Invalid Minecraft username");
            if (in.available() != 0 && in.available() != 16) {
                throw new IOException("Login start contains an invalid trailing UUID payload");
            }
            if (server.players().size() >= server.maxPlayers()) {
                closeWithLoginDisconnect("Server is full.");
                return;
            }
            if (onlineMode) {
                sendEncryptionRequest();
            } else {
                // Offline-mode identity is server-derived; never trust an optional
                // client-supplied UUID to impersonate another offline player.
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
                authenticated = true;
                enableLoginCompression();
                sendLoginSuccess();
            }
            return;
        }

        String name = packetName(VanillaProtocol.State.LOGIN,
                VanillaProtocol.Direction.SERVERBOUND, frame.packetId()).orElse("");
        if (name.equals("minecraft:key") || name.contains("encryption_response")) {
            if (!onlineMode || authenticated) throw new IOException("Unexpected encryption response");
            handleEncryptionResponse(frame.payload());
            return;
        }
        if (name.contains("login_acknowledged")) {
            if (frame.payload().length != 0) throw new IOException("Login acknowledgement packet must be empty");
            if (!authenticated) throw new IOException("Login acknowledgement before authentication");
            state = ConnectionState.CONFIGURATION;
            sendConfigurationStart();
            return;
        }
        throw new IOException("Unsupported login packet: " + frame.packetId());
    }

    private void sendEncryptionRequest() throws IOException {
        send(VanillaProtocol.State.LOGIN, "minecraft:hello", out -> {
            VanillaProtocolCodec.writeString(out, "", 20);
            VanillaProtocolCodec.writeByteArray(out, authentication.publicKey(), 1024);
            VanillaProtocolCodec.writeByteArray(out, authentication.verifyToken(), 16);
            VanillaProtocolCodec.writeBoolean(out, true);
        });
    }

    private void handleEncryptionResponse(byte[] payload) throws IOException {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(payload);
            byte[] encryptedSecret = VanillaProtocolCodec.readByteArray(in, 512);
            byte[] encryptedToken = VanillaProtocolCodec.readByteArray(in, 512);
            if (in.available() != 0) throw new IOException("Trailing bytes in encryption response");
            byte[] secret = authentication.decryptRsa(encryptedSecret);
            byte[] token = authentication.decryptRsa(encryptedToken);
            if (!java.util.Arrays.equals(token, authentication.verifyToken()))
                throw new IOException("Invalid Minecraft encryption verify token");
            String serverId = authentication.serverIdDigest(secret);
            GrimholtOnlineAuthentication.AuthenticatedProfile profile =
                    authentication.verifyJoined(username, serverId);
            uuid = profile.uuid();
            username = profile.username();
            transport.enableEncryption(secret);
            authenticated = true;
            enableLoginCompression();
            sendLoginSuccess();
        } catch (java.security.GeneralSecurityException e) {
            throw new IOException("Invalid Minecraft encryption response", e);
        }
    }

    private void sendLoginSuccess() throws IOException {
        UUID sessionId = UUID.randomUUID();
        send(VanillaProtocol.State.LOGIN, "minecraft:login_finished", out -> {
            VanillaProtocolCodec.writeUuid(out, uuid);
            VanillaProtocolCodec.writeString(out, username, 16);
            VanillaProtocol.writeVarInt(out, 0); // profile properties
            VanillaProtocolCodec.writeUuid(out, sessionId); // session id
        });
    }

    private void enableLoginCompression() throws IOException {
        String compressionPacket = null;
        for (String candidate : new String[] {"minecraft:login_compression", "minecraft:set_compression"}) {
            if (catalog.id(VanillaProtocol.State.LOGIN, VanillaProtocol.Direction.CLIENTBOUND, candidate).isPresent()) {
                compressionPacket = candidate;
                break;
            }
        }
        if (compressionPacket == null) {
            throw new IOException("Minecraft 26.4 login compression packet is missing from Mojang packet catalog");
        }
        final String packet = compressionPacket;
        send(VanillaProtocol.State.LOGIN, packet,
                out -> VanillaProtocol.writeVarInt(out, LOGIN_COMPRESSION_THRESHOLD));
        transport.enableCompression(LOGIN_COMPRESSION_THRESHOLD);
    }

    private void sendConfigurationStart() throws IOException {
        send(VanillaProtocol.State.CONFIGURATION, "minecraft:select_known_packs",
                out -> out.write(configuration.selectKnownPacks()));
    }

    private void handleConfiguration(VanillaProtocol.Frame frame) throws IOException {
        String name = packetName(VanillaProtocol.State.CONFIGURATION,
                VanillaProtocol.Direction.SERVERBOUND, frame.packetId()).orElse("");
        if (name.contains("known_packs")) {
            configuration.readKnownPacks(frame.payload());
            for (byte[] registry : configuration.registryDataPackets()) {
                send(VanillaProtocol.State.CONFIGURATION, "minecraft:registry_data",
                        out -> out.write(registry));
            }
            if (catalog.id(VanillaProtocol.State.CONFIGURATION,
                    VanillaProtocol.Direction.CLIENTBOUND, "minecraft:update_enabled_features").isPresent()) {
                send(VanillaProtocol.State.CONFIGURATION, "minecraft:update_enabled_features", out -> {
                    VanillaProtocol.writeVarInt(out, 1);
                    VanillaProtocolCodec.writeIdentifier(out, "minecraft:vanilla");
                });
            }
            for (byte[] tags : configuration.updateTagsPackets()) {
                if (catalog.id(VanillaProtocol.State.CONFIGURATION,
                        VanillaProtocol.Direction.CLIENTBOUND, "minecraft:update_tags").isPresent()) {
                    send(VanillaProtocol.State.CONFIGURATION, "minecraft:update_tags",
                            out -> out.write(tags));
                }
            }
            send(VanillaProtocol.State.CONFIGURATION, "minecraft:finish_configuration", out -> {});
            return;
        }
        if (name.equals("minecraft:finish_configuration") || name.contains("acknowledge_finish_configuration")) {
            enterPlay();
            return;
        }

        // Configuration is a bidirectional phase. Vanilla clients may send
        // settings, plugin payloads, cookies and resource-pack responses while
        // the server is still synchronizing registries. These packets are
        // legitimate and must not tear down an otherwise valid connection.
        if (name.equals("minecraft:client_information")
                || name.equals("minecraft:custom_payload")
                || name.equals("minecraft:cookie_response")
                || name.equals("minecraft:resource_pack")) {
            return;
        }

        if (name.equals("minecraft:keep_alive")) {
            long id = new DataInputStream(new ByteArrayInputStream(frame.payload())).readLong();
            if (catalog.id(VanillaProtocol.State.CONFIGURATION,
                    VanillaProtocol.Direction.CLIENTBOUND, "minecraft:keep_alive").isPresent()) {
                send(VanillaProtocol.State.CONFIGURATION, "minecraft:keep_alive",
                        out -> new DataOutputStream(out).writeLong(id));
            }
            return;
        }

        if (name.equals("minecraft:pong")) {
            int id = VanillaProtocol.readVarInt(new ByteArrayInputStream(frame.payload()));
            if (catalog.id(VanillaProtocol.State.CONFIGURATION,
                    VanillaProtocol.Direction.CLIENTBOUND, "minecraft:ping").isPresent()) {
                send(VanillaProtocol.State.CONFIGURATION, "minecraft:ping",
                        out -> VanillaProtocol.writeVarInt(out, id));
            }
            return;
        }

        throw new IOException("Unsupported configuration packet: " + frame.packetId());
    }

    private void enterPlay() throws IOException {
        state = ConnectionState.PLAY;
        kernel.registerPlayer(overworldId(), uuid).connect(username);
        kernel.updatePlayerPosition(overworldId(), uuid, position.x(), position.y(), position.z(),
                position.yaw(), position.pitch(), false);
        server.playerConnected(uuid, username, this);

        send(VanillaProtocol.State.PLAY, "minecraft:login",
                out -> out.write(playProtocol.login(new GrimholtPlayProtocol.Bootstrap(
                        entityId, "minecraft:overworld", server.maxPlayers(),
                        GrimholtPlayProtocol.DEFAULT_VIEW_DISTANCE,
                        GrimholtPlayProtocol.DEFAULT_SIMULATION_DISTANCE,
                        0L, position, false))));
        send(VanillaProtocol.State.PLAY, "minecraft:player_info_update",
                out -> out.write(playProtocol.playerInfoAdd(uuid, username)));
        send(VanillaProtocol.State.PLAY, "minecraft:commands",
                out -> out.write(commandTreeWire.encode(commands)));
        send(VanillaProtocol.State.PLAY, "minecraft:game_event",
                out -> out.write(playProtocol.startWaitingForChunks()));
        send(VanillaProtocol.State.PLAY, "minecraft:set_default_spawn_position",
                out -> out.write(playProtocol.defaultSpawn(position)));

        int teleport = nextTeleportId.getAndIncrement();
        pendingTeleportId = teleport;
        send(VanillaProtocol.State.PLAY, "minecraft:player_position",
                out -> out.write(playProtocol.synchronizePosition(teleport, position)));
        sendInitialInventory();
        streamInitialChunks();
    }

    private void sendInitialInventory() {
        for (int slot = 0; slot < inventory.size(); slot++) {
            final int inventorySlot = slot;
            sendBestEffortPlay("minecraft:set_player_inventory", out -> {
                VanillaProtocol.writeVarInt(out, inventorySlot);
                VanillaProtocol.writeVarInt(out, 0); // empty ItemStack
            });
        }
        sendBestEffortPlay("minecraft:set_held_slot",
                out -> VanillaProtocol.writeVarInt(out, inventory.selectedHotbarSlot()));
    }

    private UUID overworldId() {
        return UUID.nameUUIDFromBytes("minecraft:overworld".getBytes(StandardCharsets.UTF_8));
    }

    private void streamInitialChunks() {
        int centerX = Math.floorDiv((int) Math.floor(position.x()), 16);
        int centerZ = Math.floorDiv((int) Math.floor(position.z()), 16);
        int radius = 2;
        for (int z = centerZ - radius; z <= centerZ + radius; z++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                final int chunkX = x, chunkZ = z;
                kernel.execute(overworldId(), chunkX, chunkZ, () -> {
                    if (closing.get()) return;
                    VanillaChunk chunk = kernel.region(overworldId(), chunkX, chunkZ).chunk(chunkX, chunkZ);
                    sendBestEffortPlay("minecraft:level_chunk_with_light",
                            out -> out.write(chunkCodec.encodeLevelChunkWithLight(chunk)));
                });
            }
        }
    }

    private void handlePlay(VanillaProtocol.Frame frame) throws IOException {
        String name = packetName(VanillaProtocol.State.PLAY,
                VanillaProtocol.Direction.SERVERBOUND, frame.packetId()).orElse("");

        if (name.contains("confirm_teleportation")) {
            int teleport = VanillaProtocol.readVarInt(new ByteArrayInputStream(frame.payload()));
            if (teleport == pendingTeleportId) pendingTeleportId = -1;
            return;
        }

        if (name.contains("set_carried_item")) {
            int slot = VanillaProtocol.readVarInt(new ByteArrayInputStream(frame.payload()));
            inventory.selectedHotbarSlot(slot);
            sendBestEffortPlay("minecraft:set_held_slot",
                    out -> VanillaProtocol.writeVarInt(out, inventory.selectedHotbarSlot()));
            return;
        }

        if (name.contains("keep_alive")) {
            long id = new DataInputStream(new ByteArrayInputStream(frame.payload())).readLong();
            send(VanillaProtocol.State.PLAY, "minecraft:keep_alive",
                    out -> new DataOutputStream(out).writeLong(id));
            return;
        }

        if (name.contains("ping_request")) {
            int id = new DataInputStream(new ByteArrayInputStream(frame.payload())).readInt();
            send(VanillaProtocol.State.PLAY, "minecraft:pong_response",
                    out -> new DataOutputStream(out).writeLong(id));
            return;
        }

        if (pendingTeleportId >= 0) return;

        if (name.contains("set_player_position_and_rotation")) {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(frame.payload()));
            position = new Position(in.readDouble(), in.readDouble(), in.readDouble(), in.readFloat(), in.readFloat());
            updateKernelPosition();
            return;
        }
        if (name.contains("set_player_position")) {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(frame.payload()));
            position = new Position(in.readDouble(), in.readDouble(), in.readDouble(), position.yaw(), position.pitch());
            updateKernelPosition();
            return;
        }
        if (name.contains("set_player_rotation")) {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(frame.payload()));
            position = new Position(position.x(), position.y(), position.z(), in.readFloat(), in.readFloat());
            updateKernelPosition();
            return;
        }

        if (name.endsWith("chat_command") || name.endsWith("chat_command_signed")) {
            try {
                String command = VanillaProtocolCodec.readString(new ByteArrayInputStream(frame.payload()), 32767);
                commands.execute(this, command);
            } catch (IOException failure) {
                Logging.connectionFailure(String.valueOf(socket.getRemoteSocketAddress()), state.name(), failure);
            }
        }
    }

    private void updateKernelPosition() {
        kernel.updatePlayerPosition(overworldId(), uuid, position.x(), position.y(), position.z(),
                position.yaw(), position.pitch(), false);
        server.updatePlayerPosition(uuid, position);
    }

    public void sendChat(String message) {
        sendBestEffortPlay("minecraft:system_chat", out -> {
            VanillaProtocolCodec.writeString(out, "{\"text\":" + quote(message) + "}", 32767);
            VanillaProtocolCodec.writeBoolean(out, false);
        });
    }

    public void kick(String reason) {
        try {
            if (state == ConnectionState.LOGIN) closeWithLoginDisconnect(reason);
            else if (state == ConnectionState.PLAY) {
                sendBestEffortPlay("minecraft:disconnect",
                        out -> VanillaProtocolCodec.writeString(out, "{\"text\":" + quote(reason) + "}", 32767));
                close();
            } else close();
        } catch (IOException failure) {
            Logging.connectionFailure(String.valueOf(socket.getRemoteSocketAddress()), state.name(), failure);
            close();
        }
    }

    private void closeWithLoginDisconnect(String reason) throws IOException {
        send(VanillaProtocol.State.LOGIN, "minecraft:disconnect",
                out -> VanillaProtocolCodec.writeString(out, "{\"text\":" + quote(reason) + "}", 32767));
        close();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void send(VanillaProtocol.State state, String packet, IOEncoder encoder) throws IOException {
        int id = catalog.requireId(state, VanillaProtocol.Direction.CLIENTBOUND, packet);
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        encoder.write(payload);
        transport.write(new VanillaProtocol.Frame(id, payload.toByteArray()));
    }

    private void sendBestEffortPlay(String packet, IOEncoder encoder) {
        try { send(VanillaProtocol.State.PLAY, packet, encoder); }
        catch (IOException | RuntimeException failure) {
            Logging.connectionFailure(String.valueOf(socket.getRemoteSocketAddress()), state.name(), failure);
        }
    }

    private Optional<String> packetName(VanillaProtocol.State state,
                                         VanillaProtocol.Direction direction, int id) {
        return catalog.names(state, direction).stream()
                .filter(name -> catalog.id(state, direction, name).orElse(-1) == id)
                .findFirst();
    }

    public ConnectionState state() { return state; }
    public UUID uuid() { return uuid; }
    public String username() { return username; }
    public int entityId() { return entityId; }
    public Position position() { return position; }

    public void position(Position value) {
        position = Objects.requireNonNull(value);
        if (uuid != null) {
            server.updatePlayerPosition(uuid, value);
            if (state == ConnectionState.PLAY) {
                int teleport = nextTeleportId.getAndIncrement();
                pendingTeleportId = teleport;
                sendBestEffortPlay("minecraft:player_position",
                        out -> out.write(playProtocol.synchronizePosition(teleport, value)));
            }
        }
    }

    @Override public String name() { return username == null ? "Connection" : username; }
    @Override public boolean isPlayer() { return username != null; }
    @Override public void sendMessage(String message) { sendChat(message); }

    @Override public void close() {
        if (!closing.compareAndSet(false, true)) return;
        if (uuid != null) {
            try { kernel.removePlayer(uuid); } catch (RuntimeException ignored) {}
            try { server.playerDisconnected(uuid); } catch (RuntimeException ignored) {}
        }
        try { transport.close(); } catch (IOException ignored) {}
        state = ConnectionState.CLOSED;
        closed.accept(this);
    }

    @FunctionalInterface private interface IOEncoder { void write(OutputStream out) throws IOException; }
}
