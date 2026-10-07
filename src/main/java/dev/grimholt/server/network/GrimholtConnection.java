package dev.grimholt.server.network;

import dev.grimholt.api.*;
import dev.grimholt.server.api.GrimholtServerImpl;
import dev.grimholt.server.command.GrimholtCommandDispatcher;
import dev.grimholt.server.vanilla.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class GrimholtConnection implements CommandSender, AutoCloseable {
    private final Socket socket;
    private final GrimholtServerImpl server;
    private final GrimholtCommandDispatcher commands;
    private final VanillaPacketCatalog catalog;
    private final GrimholtPacketTransport transport;
    private final Consumer<GrimholtConnection> closed;
    private final AtomicBoolean closing = new AtomicBoolean();
    private volatile ConnectionState state = ConnectionState.HANDSHAKE;
    private volatile UUID uuid;
    private volatile String username;
    private volatile Position position = new Position(0, 64, 0, 0, 0);

    public GrimholtConnection(Socket socket, GrimholtServerImpl server, GrimholtCommandDispatcher commands,
                              VanillaPacketCatalog catalog, Consumer<GrimholtConnection> closed) throws IOException {
        this.socket = Objects.requireNonNull(socket);
        this.server = Objects.requireNonNull(server);
        this.commands = Objects.requireNonNull(commands);
        this.catalog = Objects.requireNonNull(catalog);
        this.closed = Objects.requireNonNull(closed);
        this.transport = new GrimholtPacketTransport(socket, 2 * 1024 * 1024);
    }

    public void run() {
        try {
            socket.setTcpNoDelay(true);
            while (!closing.get()) handle(transport.read());
        } catch (EOFException ignored) {
        } catch (IOException ignored) {
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
        if (frame.packetId() != 0) throw new IOException("Expected Login Start");
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
        state = ConnectionState.CONFIGURATION;
        server.attachConnection(this);
    }

    private void sendLoginSuccess() throws IOException {
        send(VanillaProtocol26_2.State.LOGIN, "minecraft:login", out -> {
            VanillaProtocolCodec.writeUuid(out, uuid);
            VanillaProtocolCodec.writeString(out, username, 16);
            VanillaProtocol26_2.writeVarInt(out, 0);
            out.write(0);
        });
    }

    private void handleConfiguration(VanillaProtocol26_2.Frame frame) throws IOException {
        String name = packetName(VanillaProtocol26_2.State.CONFIGURATION,
                VanillaProtocol26_2.Direction.SERVERBOUND, frame.packetId()).orElse("");
        if (name.contains("known_packs")) {
            send(VanillaProtocol26_2.State.CONFIGURATION, "minecraft:finish_configuration", out -> {});
            return;
        }
        if (name.contains("acknowledge_finish_configuration")) {
            enterPlay();
            return;
        }
        throw new IOException("Unsupported configuration packet: " + frame.packetId());
    }

    private void enterPlay() {
        state = ConnectionState.PLAY;
        server.playerConnected(uuid, username, this);
    }

    private void handlePlay(VanillaProtocol26_2.Frame frame) {
        String name = packetName(VanillaProtocol26_2.State.PLAY,
                VanillaProtocol26_2.Direction.SERVERBOUND, frame.packetId()).orElse("");
        if (name.endsWith("chat_command") || name.endsWith("chat_command_signed")) {
            try {
                String command = VanillaProtocolCodec.readString(new ByteArrayInputStream(frame.payload()), 32767);
                commands.execute(this, command);
            } catch (IOException ignored) {}
        }
    }

    public void sendChat(String message) {
        sendBestEffortPlay("minecraft:system_chat", out -> {
            VanillaProtocolCodec.writeString(out, "{\"text\":" + quote(message) + "}", 32767);
            out.write(0);
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
    public Position position() { return position; }

    public void position(Position value) {
        position = Objects.requireNonNull(value);
        if (uuid != null) server.updatePlayerPosition(uuid, value);
    }

    @Override public String name() { return username == null ? "Connection" : username; }
    @Override public boolean isPlayer() { return username != null; }
    @Override public void sendMessage(String message) { sendChat(message); }

    @Override public void close() {
        if (!closing.compareAndSet(false, true)) return;
        if (uuid != null) {
            try { server.playerDisconnected(uuid); } catch (RuntimeException ignored) {}
        }
        try { transport.close(); } catch (IOException ignored) {}
        state = ConnectionState.CLOSED;
        closed.accept(this);
    }

    @FunctionalInterface private interface IOEncoder { void write(OutputStream out) throws IOException; }
}
