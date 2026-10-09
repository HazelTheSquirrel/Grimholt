package dev.grimholt.server.network;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Per-connection inbound packet/byte budget. The connection closes when either
 * budget is exceeded; rejected packets do not consume additional budget.
 */
public final class GrimholtPacketRateLimiter {
    private static final long DEFAULT_WINDOW_NANOS = 1_000_000_000L;
    private static final int DEFAULT_MAX_PACKETS = 500;
    private static final long DEFAULT_MAX_BYTES = 8L * 1024 * 1024;

    private final long windowNanos;
    private final int maxPackets;
    private final long maxBytes;
    private final LongSupplier nanoClock;
    private long windowStart;
    private int packets;
    private long bytes;

    public GrimholtPacketRateLimiter(long windowNanos, int maxPackets, long maxBytes,
                                     LongSupplier nanoClock) {
        if (windowNanos <= 0 || maxPackets <= 0 || maxBytes <= 0) {
            throw new IllegalArgumentException("Rate-limit budgets must be positive");
        }
        this.windowNanos = windowNanos;
        this.maxPackets = maxPackets;
        this.maxBytes = maxBytes;
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
        this.windowStart = nanoClock.getAsLong();
    }

    public static GrimholtPacketRateLimiter defaults() {
        return new GrimholtPacketRateLimiter(
                DEFAULT_WINDOW_NANOS, DEFAULT_MAX_PACKETS, DEFAULT_MAX_BYTES, System::nanoTime);
    }

    /**
     * Accounts for one complete decoded packet payload. Returns false once the
     * current window would exceed either budget.
     */
    public synchronized boolean tryAcquire(int payloadBytes) {
        if (payloadBytes < 0) throw new IllegalArgumentException("payloadBytes");
        long now = nanoClock.getAsLong();
        long elapsed = now - windowStart;
        if (elapsed < 0 || elapsed >= windowNanos) {
            windowStart = now;
            packets = 0;
            bytes = 0;
        }
        if (packets >= maxPackets || payloadBytes > maxBytes - bytes) return false;
        packets++;
        bytes += payloadBytes;
        return true;
    }

    public synchronized int packetsInWindow() { return packets; }
    public synchronized long bytesInWindow() { return bytes; }
}
