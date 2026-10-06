package dev.grimholt.api;
import java.time.Duration;
public interface Scheduler { Task run(Runnable task); Task runLater(Duration delay, Runnable task); Task runRepeating(Duration initialDelay, Duration period, Runnable task); Task runAsync(Runnable task); }
