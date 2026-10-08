package dev.grimholt.server.vanilla;

import dev.grimholt.server.concurrency.OwnedRegion;
import dev.grimholt.server.concurrency.RegionKey;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class VanillaRegionTickSchedulingTest {
    @Test
    void coalescesTickRequestsWhileRegionWorkIsQueued() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        UUID worldId = UUID.randomUUID();
        OwnedRegion owner = new OwnedRegion(
                new RegionKey(worldId, 0, 0), 8, scheduled::add);
        VanillaRegionRuntime region = new VanillaRegionRuntime(
                owner, new VanillaWorldModel(worldId));

        assertTrue(region.requestTick());
        assertTrue(region.tickPending());
        assertFalse(region.requestTick());
        assertEquals(1, scheduled.size(), "a slow region must not accumulate stale tick tasks");

        scheduled.remove().run();

        assertFalse(region.tickPending());
        assertTrue(region.requestTick(), "the next tick may be queued after the previous one finishes");
        assertEquals(1, scheduled.size());

        region.forceClose();
    }

    @Test
    void clearsPendingFlagWhenSchedulingIsRejected() {
        UUID worldId = UUID.randomUUID();
        OwnedRegion owner = new OwnedRegion(
                new RegionKey(worldId, 0, 0), 8,
                ignored -> { throw new RejectedExecutionException("scheduler closed"); });
        VanillaRegionRuntime region = new VanillaRegionRuntime(
                owner, new VanillaWorldModel(worldId));

        assertThrows(RejectedExecutionException.class, region::requestTick);
        assertFalse(region.tickPending(), "failed scheduling must not permanently wedge the region");

        region.forceClose();
    }
}
