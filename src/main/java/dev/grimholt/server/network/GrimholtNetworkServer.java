package dev.grimholt.server.network;

import dev.grimholt.server.api.GrimholtServerImpl;
import dev.grimholt.server.command.GrimholtCommandDispatcher;
import dev.grimholt.server.config.GrimholtConfig;
import dev.grimholt.server.vanilla.*;
import dev.grimholt.server.logging.Logging;
import java.io.IOException;
import java.net.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

public final class GrimholtNetworkServer implements AutoCloseable {
    private static final int MAX_PENDING_CONNECTIONS = 64;
    private final GrimholtServerImpl server;
    private final GrimholtCommandDispatcher commands;
    private final VanillaServerKernel kernel;
    private volatile boolean onlineMode;
    private volatile VanillaGeneratedData generatedData;
    private volatile VanillaPacketCatalog packetCatalog;
    private final Set<GrimholtConnection> connections = ConcurrentHashMap.newKeySet();
    private volatile ServerSocket socket;
    private volatile boolean running;

    public GrimholtNetworkServer(GrimholtServerImpl server, GrimholtCommandDispatcher commands, VanillaServerKernel kernel) {
        this(server, commands, kernel, true);
    }

    public GrimholtNetworkServer(GrimholtServerImpl server, GrimholtCommandDispatcher commands, VanillaServerKernel kernel, boolean onlineMode) {
        this.server = server;
        this.commands = commands;
        this.kernel = kernel;
        this.onlineMode = onlineMode;
    }

    public void start(GrimholtConfig config) {
        if (running) throw new IllegalStateException("Network server already running");
        try {
            socket = new ServerSocket();
            socket.setReuseAddress(true);
            socket.bind(config.socketAddress());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot bind Grimholt network socket", e);
        }
        onlineMode = config.onlineMode();
        try {
            VanillaGeneratedData data = new VanillaGeneratedData();
            data.requireAvailable();
            VanillaPacketCatalog packets = VanillaPacketCatalog.load(data);
            generatedData = data;
            packetCatalog = packets;
        } catch (RuntimeException failure) {
            try { socket.close(); } catch (IOException closeFailure) { failure.addSuppressed(closeFailure); }
            socket = null;
            throw new IllegalStateException("Cannot initialize the pinned Minecraft 26.4 packet/data catalog", failure);
        }
        running = true;
        acceptLoop();
    }

    private void acceptLoop() {
        Thread.ofVirtual().name("Grimholt-Acceptor").start(() -> {
            while (running) {
                final Socket client;
                try {
                    client = socket.accept();
                } catch (IOException failure) {
                    if (running) Logging.networkFailure(failure);
                    break;
                }

                // Bound pre-authentication resource usage independently of the
                // configured player cap; the acceptor is the sole admission writer.
                if (connections.size() >= server.maxPlayers() + MAX_PENDING_CONNECTIONS) {
                    closeQuietly(client);
                    continue;
                }

                GrimholtConnection connection = null;
                try {
                    connection = new GrimholtConnection(
                            client, server, commands, packetCatalog, generatedData, kernel, onlineMode, connections::remove);
                    connections.add(connection);
                    GrimholtConnection accepted = connection;
                    Thread.ofVirtual().name("Grimholt-Connection").start(accepted::run);
                } catch (IOException | RuntimeException failure) {
                    Logging.connectionFailure(String.valueOf(client.getRemoteSocketAddress()), "ACCEPT", failure);
                    if (connection != null) {
                        connections.remove(connection);
                        connection.close();
                    } else {
                        closeQuietly(client);
                    }
                    // Malformed/short-lived peers must never terminate the accept loop.
                }
            }
        });
    }

    private static void closeQuietly(Socket socket) {
        try { socket.close(); } catch (IOException ignored) {}
    }

    public int boundPort() { return socket == null ? -1 : socket.getLocalPort(); }
    public int connectionCount() { return connections.size(); }

    @Override public void close() {
        running = false;
        ServerSocket current = socket;
        socket = null;
        if (current != null) try { current.close(); } catch (IOException ignored) {}
        connections.forEach(GrimholtConnection::close);
        connections.clear();
    }
}
