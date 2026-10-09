package dev.grimholt.server.network;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class GrimholtPacketRateLimiterTest {
    @Test
    void limitsPacketCountWithinWindow() {
        AtomicLong clock = new AtomicLong(10);
        GrimholtPacketRateLimiter limiter = new GrimholtPacketRateLimiter(1_000, 2, 100, clock::get);

        assertTrue(limiter.tryAcquire(10));
        assertTrue(limiter.tryAcquire(10));
        assertFalse(limiter.tryAcquire(0));
        assertEquals(2, limiter.packetsInWindow());
        assertEquals(20, limiter.bytesInWindow());
    }

    @Test
    void limitsBytesWithoutConsumingBudgetOnRejectedPacket() {
        AtomicLong clock = new AtomicLong(10);
        GrimholtPacketRateLimiter limiter = new GrimholtPacketRateLimiter(1_000, 10, 8, clock::get);

        assertTrue(limiter.tryAcquire(5));
        assertFalse(limiter.tryAcquire(4));
        assertEquals(1, limiter.packetsInWindow());
        assertEquals(5, limiter.bytesInWindow());
        assertTrue(limiter.tryAcquire(3));
    }

    @Test
    void resetsBudgetsAtWindowBoundary() {
        AtomicLong clock = new AtomicLong(10);
        GrimholtPacketRateLimiter limiter = new GrimholtPacketRateLimiter(1_000, 1, 8, clock::get);

        assertTrue(limiter.tryAcquire(8));
        assertFalse(limiter.tryAcquire(0));
        clock.addAndGet(1_000);
        assertTrue(limiter.tryAcquire(8));
        assertEquals(1, limiter.packetsInWindow());
        assertEquals(8, limiter.bytesInWindow());
    }

    @Test
    void rejectsInvalidConfigurationAndNegativePayloadSize() {
        AtomicLong clock = new AtomicLong();
        assertThrows(IllegalArgumentException.class,
                () -> new GrimholtPacketRateLimiter(0, 1, 1, clock::get));
        GrimholtPacketRateLimiter limiter = new GrimholtPacketRateLimiter(1, 1, 1, clock::get);
        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(-1));
    }
}
