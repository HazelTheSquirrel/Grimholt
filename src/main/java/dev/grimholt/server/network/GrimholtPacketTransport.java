package dev.grimholt.server.network;

import dev.grimholt.server.vanilla.VanillaProtocol26_2;
import java.io.*;
import java.net.Socket;
import java.util.Objects;

public final class GrimholtPacketTransport implements Closeable {
    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;
    private final int maxFrameBytes;

    public GrimholtPacketTransport(Socket socket, int maxFrameBytes) throws IOException {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.input = socket.getInputStream();
        this.output = socket.getOutputStream();
        this.maxFrameBytes = maxFrameBytes;
    }

    public VanillaProtocol26_2.Frame read() throws IOException {
        return VanillaProtocol26_2.decodeFrame(input, maxFrameBytes);
    }

    public synchronized void write(VanillaProtocol26_2.Frame frame) throws IOException {
        output.write(VanillaProtocol26_2.encodeFrame(frame));
        output.flush();
    }

    public boolean open() { return !socket.isClosed(); }

    @Override public void close() throws IOException { socket.close(); }
}
