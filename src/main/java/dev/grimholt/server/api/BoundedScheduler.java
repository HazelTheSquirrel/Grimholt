package dev.grimholt.server.api;
import dev.grimholt.api.*; import java.time.Duration; import java.util.concurrent.*; import java.util.concurrent.atomic.AtomicBoolean;
public final class BoundedScheduler implements Scheduler,AutoCloseable {
 private final ScheduledThreadPoolExecutor timer; private final ThreadPoolExecutor async; private final Semaphore admission;
 public BoundedScheduler(int workers,int maxQueued){if(workers<1||maxQueued<1)throw new IllegalArgumentException();admission=new Semaphore(maxQueued);timer=new ScheduledThreadPoolExecutor(1,r->new Thread(r,"Grimholt-Scheduler"));timer.setRemoveOnCancelPolicy(true);async=new ThreadPoolExecutor(workers,workers,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(maxQueued),r->new Thread(r,"Grimholt-Async"),new ThreadPoolExecutor.AbortPolicy());}
 private void acquire(){if(!admission.tryAcquire())throw new RejectedExecutionException("Grimholt scheduler queue is full");}
 public Task run(Runnable task){return oneShot(task,false,0);}
 public Task runAsync(Runnable task){return oneShot(task,true,0);}
 private Task oneShot(Runnable task,boolean asyncRun,long delayNanos){acquire();var h=new H(false);Runnable work=()->{try{if(!h.cancelled())task.run();}finally{h.release();}};try{if(delayNanos>0)h.future=timer.schedule(work,delayNanos,TimeUnit.NANOSECONDS);else if(asyncRun)async.execute(work);else timer.execute(work);return h;}catch(RuntimeException e){h.release();throw e;}}
 public Task runLater(Duration d,Runnable task){if(d.isNegative())throw new IllegalArgumentException("delay < 0");return oneShot(task,false,d.toNanos());}
 public Task runRepeating(Duration initial,Duration period,Runnable task){if(initial.isNegative()||period.isZero()||period.isNegative())throw new IllegalArgumentException();acquire();var h=new H(true);try{h.future=timer.scheduleAtFixedRate(()->{if(!h.cancelled())try{task.run();}catch(Throwable ignored){}},initial.toNanos(),period.toNanos(),TimeUnit.NANOSECONDS);return h;}catch(RuntimeException e){h.release();throw e;}}
 public void close(){timer.shutdownNow();async.shutdownNow();admission.drainPermits();}
 private final class H implements Task {private final boolean repeating;private final AtomicBoolean cancelled=new AtomicBoolean();private final AtomicBoolean released=new AtomicBoolean();private Future<?> future;H(boolean repeating){this.repeating=repeating;}void release(){if(released.compareAndSet(false,true))admission.release();}public boolean cancel(){if(!cancelled.compareAndSet(false,true))return false;if(future!=null)future.cancel(false);release();return true;}public boolean cancelled(){return cancelled.get();}}
}
