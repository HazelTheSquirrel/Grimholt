package dev.grimholt.server.vanilla;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WinterDropMechanicsTest {
    @Test
    void iceBallHasFourDamageAtFullVelocity() {
        IceBallImpact impact = IceBallImpact.fromVelocity(1, 0, 0);
        assertEquals(4.0, impact.damage(), 0.0001);
        assertTrue(impact.knockbackX() > 0);
    }

    @Test
    void freezingStateTracksDuration() {
        FreezingState state = new FreezingState();
        state.apply(200);
        assertTrue(state.active());
        assertFalse(state.tick());
        for (int i = 1; i < FreezingState.FREEZE_THRESHOLD_TICKS; i++) {
            assertFalse(state.tick());
        }
        assertTrue(state.fullyFrozen());
        assertTrue(state.tick());
        state.clear();
        assertFalse(state.active());
    }

    @Test
    void icicleImpactScalesWithFallDistance() {
        IcicleImpact impact = IcicleImpact.fromFallDistance(6);
        assertEquals(6.0, impact.fallDamage(), 0.0001);
        assertEquals(3.0, impact.landingBonusDamage(), 0.0001);
    }
}
