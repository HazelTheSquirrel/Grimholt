package dev.grimholt.server.network;

import dev.grimholt.server.api.GrimholtServerImpl;
import dev.grimholt.server.command.GrimholtCommandDispatcher;
import dev.grimholt.server.config.GrimholtConfig;
import dev.grimholt.server.vanilla.*;
import java.io.IOException;
import java.net.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

public final class GrimholtNetworkServer implements AutoCloseable {
    private final GrimholtServerImpl server;
    private final GrimholtCommandDispatcher commands;
    private final Set<GrimholtConnection> connections = ConcurrentHashMap.newKeySet();
    private volatile ServerSocket socket;
    private volatile boolean running;

    public GrimholtNetworkServer(GrimholtServerImpl server, GrimholtCommandDispatcher commands) {
        this.server = server;
        this.commands = commands;
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
        running = true;
        acceptLoop();
    }

    private void acceptLoop() {
        Thread.ofVirtual().name("Grimholt-Acceptor").start(() -> {
            while (running) {
                try {
                    Socket client = socket.accept();
                    VanillaPacketCatalog catalog = VanillaPacketCatalog.load(new VanillaGeneratedData());
                    GrimholtConnection connection = new GrimholtConnection(client, server, commands, catalog, connections::remove);
                    connections.add(connection);
                    Thread.ofVirtual().name("Grimholt-Connection").start(connection::run);
                } catch (IOException | RuntimeException failure) {
                    if (running) break;
                }
            }
        });
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
