package dev.grimholt.server.lifecycle;

import java.util.concurrent.atomic.AtomicReference;

public final class Lifecycle {
    private final AtomicReference<LifecycleState> state = new AtomicReference<>(LifecycleState.NEW);

    public LifecycleState state() { return state.get(); }
    public boolean beginStart() { return state.compareAndSet(LifecycleState.NEW, LifecycleState.STARTING); }

    public void started() {
        require(LifecycleState.STARTING);
        state.set(LifecycleState.RUNNING);
    }

    public boolean beginStop() {
        return state.compareAndSet(LifecycleState.RUNNING, LifecycleState.STOPPING)
                || state.compareAndSet(LifecycleState.STARTING, LifecycleState.STOPPING);
    }

    public void stopped() {
        LifecycleState previous = state.getAndSet(LifecycleState.STOPPED);
        if (previous != LifecycleState.STOPPING && previous != LifecycleState.STOPPED) {
            throw new IllegalStateException("Cannot enter STOPPED from " + previous);
        }
    }

    public void failed() { state.set(LifecycleState.FAILED); }

    private void require(LifecycleState expected) {
        LifecycleState actual = state.get();
        if (actual != expected) throw new IllegalStateException("Expected " + expected + " but was " + actual);
    }
}
