package dev.grimholt.server.api;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration; import java.util.concurrent.CountDownLatch; import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
class BoundedSchedulerTest {
 @Test void asyncTaskRunsAndCanBeCancelled(){try(var scheduler=new BoundedScheduler(1,4)){var latch=new CountDownLatch(1);scheduler.runAsync(latch::countDown);assertTimeoutPreemptively(Duration.ofSeconds(2),()->assertTrue(latch.await(2,TimeUnit.SECONDS)));var task=scheduler.runLater(Duration.ofSeconds(5),()->fail("cancelled task ran"));assertTrue(task.cancel());assertTrue(task.cancelled());}}
}