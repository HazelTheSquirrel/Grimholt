package dev.grimholt.transport;

import dev.grimholt.protocol.StatusResponse;
import dev.grimholt.protocol.StatusSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Non-blocking TCP listener for Minecraft STATUS queries; LOGIN and gameplay are not enabled. */
public final class StatusServer implements AutoCloseable {
    private static final int MAX_CONNECTIONS = 512;
    private static final int MAX_FRAME_LENGTH = 16 * 1024;
    private static final int BUFFER_BYTES = 32 * 1024;
    private static final long IDLE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(30);

    private final Selector selector;
    private final ServerSocketChannel listener;
    private final Set<Connection> connections = new HashSet<>();
    private final StatusResponse statusResponse;
    private volatile boolean running = true;
    private boolean resourcesClosed;

    public StatusServer(String host, int port, StatusResponse statusResponse) throws IOException {
        if (host == null || host.isBlank()) throw new IllegalArgumentException("host must not be blank");
        if (port < 0 || port > 65_535) throw new IllegalArgumentException("port must be in [0, 65535]");
        this.statusResponse = java.util.Objects.requireNonNull(statusResponse, "statusResponse");
        selector = Selector.open();
        ServerSocketChannel opened = ServerSocketChannel.open();
        try {
            opened.configureBlocking(false);
            opened.bind(new InetSocketAddress(host, port), 128);
            opened.register(selector, SelectionKey.OP_ACCEPT);
            listener = opened;
        } catch (IOException | RuntimeException failure) {
            try { opened.close(); } catch (IOException suppressed) { failure.addSuppressed(suppressed); }
            try { selector.close(); } catch (IOException suppressed) { failure.addSuppressed(suppressed); }
            throw failure;
        }
    }

    public void run() throws IOException {
        System.out.println("Grimholt STATUS listener active on " + listener.getLocalAddress());
        try {
            while (running) {
                selector.select(1_000L);
                if (!running) break;
                Iterator<SelectionKey> selected = selector.selectedKeys().iterator();
                while (selected.hasNext()) {
                    SelectionKey key = selected.next();
                    selected.remove();
                    if (!key.isValid()) continue;
                    try {
                        if (key.isAcceptable()) {
                            acceptConnections();
                        } else {
                            Connection connection = (Connection) key.attachment();
                            if (key.isReadable()) read(connection);
                            if (key.isValid() && key.isWritable()) write(connection);
                        }
                    } catch (IOException | RuntimeException malformedOrDisconnected) {
                        Object attachment = key.attachment();
                        if (attachment instanceof Connection connection) closeConnection(connection);
                        else if (key.isValid()) key.cancel();
                    }
                }
                expireIdleConnections(System.nanoTime());
            }
        } catch (ClosedSelectorException stopped) {
            if (running) throw stopped;
        } finally {
            closeResources();
        }
    }

    private void acceptConnections() throws IOException {
        SocketChannel channel;
        while ((channel = listener.accept()) != null) {
            if (connections.size() >= MAX_CONNECTIONS) {
                channel.close();
                continue;
            }
            try {
                channel.configureBlocking(false);
                channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                Connection connection = new Connection(channel, new StatusSession(statusResponse));
                connection.key = channel.register(selector, SelectionKey.OP_READ, connection);
                connections.add(connection);
            } catch (IOException | RuntimeException failure) {
                try { channel.close(); } catch (IOException ignored) { }
            }
        }
    }

    private void read(Connection connection) throws IOException {
        int count = connection.channel.read(connection.input);
        if (count < 0) {
            closeConnection(connection);
            return;
        }
        if (count == 0) return;
        connection.lastActivityNanos = System.nanoTime();
        connection.input.flip();
        int consumed = PacketFrameDecoder.decodeAvailable(
                connection.input.array(), connection.input.position(), connection.input.remaining(),
                MAX_FRAME_LENGTH, (payload, offset, length) -> {
                    int written = connection.session.handleFrame(payload, offset, length,
                            connection.output.array(), connection.output.position());
                    if (written > connection.output.remaining()) {
                        throw new IOException("Outbound STATUS buffer capacity exceeded");
                    }
                    connection.output.position(connection.output.position() + written);
                });
        connection.input.position(connection.input.position() + consumed);
        connection.input.compact();
        if (connection.input.position() == connection.input.capacity()) {
            throw new IOException("Receive buffer filled without a complete packet");
        }
        updateInterest(connection);
    }

    private void write(Connection connection) throws IOException {
        connection.output.flip();
        int written = connection.channel.write(connection.output);
        connection.output.compact();
        if (written > 0) connection.lastActivityNanos = System.nanoTime();
        if (connection.output.position() == 0
                && connection.session.state() == StatusSession.State.CLOSE_AFTER_WRITE) {
            closeConnection(connection);
            return;
        }
        updateInterest(connection);
    }

    private void updateInterest(Connection connection) {
        if (!connection.key.isValid()) return;
        int ops = 0;
        if (connection.session.state() != StatusSession.State.CLOSE_AFTER_WRITE) ops |= SelectionKey.OP_READ;
        if (connection.output.position() > 0) ops |= SelectionKey.OP_WRITE;
        if (ops == 0) closeConnection(connection);
        else connection.key.interestOps(ops);
    }

    private void expireIdleConnections(long now) {
        for (Iterator<Connection> iterator = connections.iterator(); iterator.hasNext();) {
            Connection connection = iterator.next();
            if (now - connection.lastActivityNanos > IDLE_TIMEOUT_NANOS) {
                iterator.remove();
                connection.session.markClosed();
                connection.key.cancel();
                try { connection.channel.close(); } catch (IOException ignored) { }
            }
        }
    }

    private void closeConnection(Connection connection) {
        if (!connections.remove(connection)) return;
        connection.session.markClosed();
        connection.key.cancel();
        try { connection.channel.close(); } catch (IOException ignored) { }
    }

    @Override
    public void close() {
        running = false;
        selector.wakeup();
    }

    private synchronized void closeResources() {
        if (resourcesClosed) return;
        resourcesClosed = true;
        for (Connection connection : connections.toArray(Connection[]::new)) closeConnection(connection);
        try { listener.close(); } catch (IOException ignored) { }
        try { selector.close(); } catch (IOException ignored) { }
    }

    private static final class Connection {
        private final SocketChannel channel;
        private final ByteBuffer input = ByteBuffer.allocate(BUFFER_BYTES);
        private final ByteBuffer output = ByteBuffer.allocate(BUFFER_BYTES);
        private final StatusSession session;
        private SelectionKey key;
        private long lastActivityNanos = System.nanoTime();

        private Connection(SocketChannel channel, StatusSession session) {
            this.channel = channel;
            this.session = session;
        }
    }
}
