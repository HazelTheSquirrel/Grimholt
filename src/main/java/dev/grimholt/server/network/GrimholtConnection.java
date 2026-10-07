package dev.grimholt.server.network;

import dev.grimholt.api.*;
import dev.grimholt.server.api.GrimholtServerImpl;
import dev.grimholt.server.command.GrimholtCommandDispatcher;
import dev.grimholt.server.vanilla.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

public final class GrimholtConnection implements CommandSender, AutoCloseable {
    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(1);
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
    private final Consumer<GrimholtConnection> closed;
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
                              VanillaServerKernel kernel, Consumer<GrimholtConnection> closed) throws IOException {
        this.socket = Objects.requireNonNull(socket);
        this.server = Objects.requireNonNull(server);
        this.commands = Objects.requireNonNull(commands);
        this.catalog = Objects.requireNonNull(catalog);
        this.generated = Objects.requireNonNull(generated);
        this.kernel = Objects.requireNonNull(kernel);
        this.closed = Objects.requireNonNull(closed);
        this.transport = new GrimholtPacketTransport(socket, 2 * 1024 * 1024);
        this.configuration = new VanillaConfigurationSync(generated);
    }

    public void run() {
        try {
            socket.setTcpNoDelay(true);
            while (!closing.get()) handle(transport.read());
        } catch (EOFException ignored) {
        } catch (IOException | RuntimeException ignored) {
        } finally {
            close();
        }
    }

    private void handle(VanillaProtocol26_2.Frame frame) throws IOException {
        switch (state) {
            case HANDSHAKE -> handleHandshake(frame);
            case STATUS -> handleStatus(frame);
            case LOGIN -> handleLogin(frame);
            case CONFIGURATION -> handleConfiguration(frame);
            case PLAY -> handlePlay(frame);
            case CLOSED -> throw new EOFException("connection closed");
        }
    }

    private void handleHandshake(VanillaProtocol26_2.Frame frame) throws IOException {
        if (frame.packetId() != 0) throw new IOException("Expected handshake packet");
        ByteArrayInputStream in = new ByteArrayInputStream(frame.payload());
        int protocol = VanillaProtocol26_2.readVarInt(in);
        VanillaProtocol26_2.requireProtocol(protocol);
        VanillaProtocolCodec.readString(in, 255);
        if (in.available() < 2) throw new EOFException("Handshake missing port");
        in.skipNBytes(2);
        int next = VanillaProtocol26_2.readVarInt(in);
        if (next == 1) state = ConnectionState.STATUS;
        else if (next == 2) state = ConnectionState.LOGIN;
        else throw new IOException("Unsupported handshake next state: " + next);
    }

    private void handleStatus(VanillaProtocol26_2.Frame frame) throws IOException {
        if (frame.packetId() == 0) {
            String json = "{\"version\":{\"name\":\"26.2\",\"protocol\":" + VanillaSnapshot26_2.PROTOCOL +
                    "},\"players\":{\"max\":" + server.maxPlayers() + ",\"online\":" + server.players().size() +
                    "},\"description\":{\"text\":\"Grimholt\"}}";
            send(VanillaProtocol26_2.State.STATUS, "minecraft:status_response",
                    out -> VanillaProtocolCodec.writeString(out, json, 32767));
        } else if (frame.packetId() == 1) {
            long payload = new DataInputStream(new ByteArrayInputStream(frame.payload())).readLong();
            send(VanillaProtocol26_2.State.STATUS, "minecraft:pong",
                    out -> new DataOutputStream(out).writeLong(payload));
        } else throw new IOException("Unknown status packet: " + frame.packetId());
    }

    private void handleLogin(VanillaProtocol26_2.Frame frame) throws IOException {
        if (frame.packetId() == 0) {
            ByteArrayInputStream in = new ByteArrayInputStream(frame.payload());
            username = VanillaProtocolCodec.readString(in, 16);
            if (username.isBlank()) throw new IOException("Empty username");
            uuid = in.available() >= 16 ? VanillaProtocolCodec.readUuid(in)
                    : UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
            if (server.players().size() >= server.maxPlayers()) {
                closeWithLoginDisconnect("Server is full.");
                return;
            }
            sendLoginSuccess();
            return;
        }

        String name = packetName(VanillaProtocol26_2.State.LOGIN,
                VanillaProtocol26_2.Direction.SERVERBOUND, frame.packetId()).orElse("");
        if (name.contains("login_acknowledged")) {
            state = ConnectionState.CONFIGURATION;
            sendConfigurationStart();
            return;
        }
        throw new IOException("Unsupported login packet: " + frame.packetId());
    }

    private void sendLoginSuccess() throws IOException {
        UUID sessionId = UUID.randomUUID();
        send(VanillaProtocol26_2.State.LOGIN, "minecraft:login_finished", out -> {
            VanillaProtocolCodec.writeUuid(out, uuid);
            VanillaProtocolCodec.writeString(out, username, 16);
            VanillaProtocol26_2.writeVarInt(out, 0); // profile properties
            VanillaProtocolCodec.writeUuid(out, sessionId); // 26.2 session id
        });
    }

    private void sendConfigurationStart() throws IOException {
        send(VanillaProtocol26_2.State.CONFIGURATION, "minecraft:select_known_packs",
                out -> out.write(configuration.selectKnownPacks()));
    }

    private void handleConfiguration(VanillaProtocol26_2.Frame frame) throws IOException {
        String name = packetName(VanillaProtocol26_2.State.CONFIGURATION,
                VanillaProtocol26_2.Direction.SERVERBOUND, frame.packetId()).orElse("");
        if (name.contains("known_packs")) {
            configuration.readKnownPacks(frame.payload());
            for (byte[] registry : configuration.registryDataPackets()) {
                send(VanillaProtocol26_2.State.CONFIGURATION, "minecraft:registry_data",
                        out -> out.write(registry));
            }
            send(VanillaProtocol26_2.State.CONFIGURATION, "minecraft:finish_configuration", out -> {});
            return;
        }
        if (name.contains("acknowledge_finish_configuration")) {
            enterPlay();
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

        send(VanillaProtocol26_2.State.PLAY, "minecraft:login",
                out -> out.write(playProtocol.login(new GrimholtPlayProtocol.Bootstrap(
                        entityId, "minecraft:overworld", server.maxPlayers(),
                        GrimholtPlayProtocol.DEFAULT_VIEW_DISTANCE,
                        GrimholtPlayProtocol.DEFAULT_SIMULATION_DISTANCE,
                        0L, position, false))));
        send(VanillaProtocol26_2.State.PLAY, "minecraft:player_info_update",
                out -> out.write(playProtocol.playerInfoAdd(uuid, username)));
        send(VanillaProtocol26_2.State.PLAY, "minecraft:game_event",
                out -> out.write(playProtocol.startWaitingForChunks()));
        send(VanillaProtocol26_2.State.PLAY, "minecraft:set_default_spawn_position",
                out -> out.write(playProtocol.defaultSpawn(position)));

        int teleport = nextTeleportId.getAndIncrement();
        pendingTeleportId = teleport;
        send(VanillaProtocol26_2.State.PLAY, "minecraft:player_position",
                out -> out.write(playProtocol.synchronizePosition(teleport, position)));
        streamInitialChunks();
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

    private void handlePlay(VanillaProtocol26_2.Frame frame) throws IOException {
        String name = packetName(VanillaProtocol26_2.State.PLAY,
                VanillaProtocol26_2.Direction.SERVERBOUND, frame.packetId()).orElse("");

        if (name.contains("confirm_teleportation")) {
            int teleport = VanillaProtocol26_2.readVarInt(new ByteArrayInputStream(frame.payload()));
            if (teleport == pendingTeleportId) pendingTeleportId = -1;
            return;
        }

        if (name.contains("keep_alive")) {
            long id = new DataInputStream(new ByteArrayInputStream(frame.payload())).readLong();
            send(VanillaProtocol26_2.State.PLAY, "minecraft:keep_alive",
                    out -> new DataOutputStream(out).writeLong(id));
            return;
        }

        if (name.contains("ping_request")) {
            int id = new DataInputStream(new ByteArrayInputStream(frame.payload())).readInt();
            send(VanillaProtocol26_2.State.PLAY, "minecraft:pong_response",
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
            } catch (IOException ignored) {}
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
        } catch (IOException ignored) {
            close();
        }
    }

    private void closeWithLoginDisconnect(String reason) throws IOException {
        send(VanillaProtocol26_2.State.LOGIN, "minecraft:disconnect",
                out -> VanillaProtocolCodec.writeString(out, "{\"text\":" + quote(reason) + "}", 32767));
        close();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void send(VanillaProtocol26_2.State state, String packet, IOEncoder encoder) throws IOException {
        int id = catalog.requireId(state, VanillaProtocol26_2.Direction.CLIENTBOUND, packet);
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        encoder.write(payload);
        transport.write(new VanillaProtocol26_2.Frame(id, payload.toByteArray()));
    }

    private void sendBestEffortPlay(String packet, IOEncoder encoder) {
        try { send(VanillaProtocol26_2.State.PLAY, packet, encoder); }
        catch (IOException | RuntimeException ignored) {}
    }

    private Optional<String> packetName(VanillaProtocol26_2.State state,
                                         VanillaProtocol26_2.Direction direction, int id) {
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
