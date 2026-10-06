package dev.grimholt.server.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TickOwnershipTest {
    @Test
    void ownershipIsExclusiveAndReentrantForTheCurrentThread() {
        var ownership = new TickOwnership();

        ownership.enter();
        assertTrue(ownership.isOwnedByCurrentThread());
        ownership.enter();
        ownership.assertOwner();
        ownership.exit();
        assertTrue(ownership.isOwnedByCurrentThread());
        ownership.exit();
        assertFalse(ownership.isOwnedByCurrentThread());
    }

    @Test
    void anotherThreadCannotEnterOwnedState() throws Exception {
        var ownership = new TickOwnership();
        ownership.enter();

        var failure = new AtomicReference<Throwable>();
        var done = new CountDownLatch(1);
        Thread other = new Thread(() -> {
            try {
                ownership.enter();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        }, "ownership-test");

        other.start();
        assertTrue(done.await(2, TimeUnit.SECONDS));
        assertInstanceOf(IllegalStateException.class, failure.get());

        ownership.exit();
        other.join(2_000);
    }

    @Test
    void exitWithoutOwnershipFails() {
        var ownership = new TickOwnership();
        assertThrows(IllegalStateException.class, ownership::exit);
    }

    @Test
    void scopeReleasesOwnershipWhenActionFails() {
        var ownership = new TickOwnership();

        assertThrows(IllegalStateException.class,
                () -> ownership.run(() -> { throw new IllegalStateException("boom"); }));

        assertFalse(ownership.isOwnedByCurrentThread());
    }
}
