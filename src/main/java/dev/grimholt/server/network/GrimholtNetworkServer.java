package dev.grimholt.server.network;

import dev.grimholt.server.api.GrimholtServerImpl;
import dev.grimholt.server.command.GrimholtCommandDispatcher;
import dev.grimholt.server.config.GrimholtConfig;
import dev.grimholt.server.vanilla.*;
import dev.grimholt.server.logging.Logging;
import java.io.IOException;
import java.net.*;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the network ingress for Grimholt.
 *
 * <p>Startup is deliberately staged: the kernel and world must already be
 * ready, protocol metadata is loaded, and only then is the listening socket
 * bound and the accept loop started. This prevents a reachable endpoint from
 * advertising readiness while gameplay dependencies are still unavailable.</p>
 */
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
        this.server = Objects.requireNonNull(server, "server");
        this.commands = Objects.requireNonNull(commands, "commands");
        this.kernel = Objects.requireNonNull(kernel, "kernel");
        this.onlineMode = onlineMode;
    }

    public synchronized void start(GrimholtConfig config) {
        Objects.requireNonNull(config, "config");
        if (running || socket != null) throw new IllegalStateException("Network server already running");

        // Fail before opening a port if the gameplay runtime has not been bootstrapped.
        if (!kernel.running()) {
            throw new IllegalStateException("Vanilla kernel must be running before the network server starts");
        }
        if (kernel.worldCount() == 0) {
            throw new IllegalStateException("At least one world must be registered before the network server starts");
        }

        final VanillaGeneratedData data;
        final VanillaPacketCatalog packets;
        try {
            data = new VanillaGeneratedData();
            data.requireAvailable();
            packets = VanillaPacketCatalog.load(data);
        } catch (RuntimeException failure) {
            throw new IllegalStateException("Cannot initialize the pinned Minecraft 26.4 packet/data catalog", failure);
        }

        final ServerSocket candidate;
        try {
            candidate = new ServerSocket();
            try {
                candidate.setReuseAddress(true);
                candidate.bind(config.socketAddress());
            } catch (IOException failure) {
                try {
                    candidate.close();
                } catch (IOException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
                throw failure;
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot bind Grimholt network socket", failure);
        }

        // Publish dependencies before making the socket visible to the accept loop.
        onlineMode = config.onlineMode();
        generatedData = data;
        packetCatalog = packets;
        socket = candidate;
        running = true;
        acceptLoop();
    }

    private void acceptLoop() {
        Thread.ofVirtual().name("Grimholt-Acceptor").start(() -> {
            while (running) {
                final Socket client;
                try {
                    ServerSocket listeningSocket = socket;
                    if (listeningSocket == null) break;
                    client = listeningSocket.accept();
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

    @Override public synchronized void close() {
        running = false;
        ServerSocket current = socket;
        socket = null;
        if (current != null) try { current.close(); } catch (IOException ignored) {}
        connections.forEach(GrimholtConnection::close);
        connections.clear();
    }
}
