package dev.grimholt.server.lifecycle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LifecycleTest {
    @Test void followsExpectedTransitions() {
        Lifecycle lifecycle = new Lifecycle();
        assertEquals(LifecycleState.NEW, lifecycle.state());
        assertTrue(lifecycle.beginStart());
        lifecycle.started();
        assertEquals(LifecycleState.RUNNING, lifecycle.state());
        assertTrue(lifecycle.beginStop());
        lifecycle.stopped();
        assertEquals(LifecycleState.STOPPED, lifecycle.state());
    }
    @Test void rejectsDoubleStartAndMakesStopIdempotent() {
        Lifecycle lifecycle = new Lifecycle();
        assertTrue(lifecycle.beginStart());
        assertFalse(lifecycle.beginStart());
        lifecycle.started();
        assertTrue(lifecycle.beginStop());
        lifecycle.stopped();
        assertFalse(lifecycle.beginStop());
    }
    @Test void startupFailureCanBeRecorded() {
        Lifecycle lifecycle = new Lifecycle();
        assertTrue(lifecycle.beginStart());
        lifecycle.failed();
        assertEquals(LifecycleState.FAILED, lifecycle.state());
    }
}
