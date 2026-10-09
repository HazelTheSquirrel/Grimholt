package dev.grimholt.runtime;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionMailboxTest {
    @Test void isBoundedAndDrainsOnlyUpToTheRequestedLimit() {
        RegionMailbox mailbox = new RegionMailbox(2);
        AtomicInteger applied = new AtomicInteger();
        assertTrue(mailbox.submit(applied::incrementAndGet));
        assertTrue(mailbox.submit(applied::incrementAndGet));
        assertFalse(mailbox.submit(applied::incrementAndGet));
        assertEquals(1, mailbox.drain(1)); assertEquals(1, applied.get());
        assertEquals(1, mailbox.queuedCommands());
        assertEquals(1, mailbox.drain(10)); assertEquals(2, applied.get());
    }
    @Test void rejectsDrainingFromAnotherThread() throws InterruptedException {
        RegionMailbox mailbox = new RegionMailbox(2);
        mailbox.drain(1);
        AtomicInteger rejected = new AtomicInteger();
        Thread other = new Thread(() -> {
            try { mailbox.drain(1); } catch (IllegalStateException expected) { rejected.incrementAndGet(); }
        });
        other.start(); other.join(); assertEquals(1, rejected.get());
    }
    @Test void requiresPositiveDrainLimit() {
        RegionMailbox mailbox = new RegionMailbox(2);
        assertThrows(IllegalArgumentException.class, () -> mailbox.drain(0));
    }
}
