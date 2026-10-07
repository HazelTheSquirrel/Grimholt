package dev.grimholt.server.network;

import dev.grimholt.server.vanilla.VanillaProtocol26_2;
import java.io.*;
import java.net.Socket;
import java.util.Objects;

public final class GrimholtPacketTransport implements Closeable {
    private final Socket socket;
    private InputStream input;
    private OutputStream output;
    private final int maxFrameBytes;
    private volatile int compressionThreshold = -1;

    public GrimholtPacketTransport(Socket socket, int maxFrameBytes) throws IOException {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.input = socket.getInputStream();
        this.output = socket.getOutputStream();
        this.maxFrameBytes = maxFrameBytes;
    }

    public VanillaProtocol26_2.Frame read() throws IOException {
        return VanillaProtocol26_2.decodeFrame(input, maxFrameBytes, compressionThreshold);
    }

    public synchronized void write(VanillaProtocol26_2.Frame frame) throws IOException {
        output.write(VanillaProtocol26_2.encodeFrame(frame, compressionThreshold, maxFrameBytes));
        output.flush();
    }

    /** Enable the Minecraft zlib packet layer after the compression negotiation packet itself. */
    public synchronized void enableCompression(int threshold) {
        if (threshold < 0) throw new IllegalArgumentException("threshold");
        compressionThreshold = threshold;
    }

    public int compressionThreshold() { return compressionThreshold; }

    public void enableEncryption(byte[] secret) throws IOException {
        try {
            input = GrimholtCipher.input(input, secret);
            output = GrimholtCipher.output(output, secret);
        } catch (java.security.GeneralSecurityException e) {
            throw new IOException("Unable to enable Minecraft AES transport encryption", e);
        }
    }

    public boolean open() { return !socket.isClosed(); }

    @Override public void close() throws IOException { socket.close(); }
}
