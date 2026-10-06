package dev.grimholt.server.metrics;
import java.util.concurrent.atomic.AtomicLong;
public final class MetricsRegistry {
 private final AtomicLong joins=new AtomicLong(), quits=new AtomicLong(), commands=new AtomicLong(), pluginFailures=new AtomicLong(), rejectedTasks=new AtomicLong();
 private final AtomicLong tickNanos=new AtomicLong(), tickSamples=new AtomicLong();
 public void joined(){joins.incrementAndGet();} public void quit(){quits.incrementAndGet();} public void command(){commands.incrementAndGet();} public void pluginFailure(){pluginFailures.incrementAndGet();} public void rejectedTask(){rejectedTasks.incrementAndGet();}
 public void tick(long nanos){tickNanos.addAndGet(nanos);tickSamples.incrementAndGet();}
 public Snapshot snapshot(){long n=tickSamples.get();return new Snapshot(joins.get(),quits.get(),commands.get(),pluginFailures.get(),rejectedTasks.get(),n==0?0:tickNanos.get()/n,n);}
 public record Snapshot(long joins,long quits,long commands,long pluginFailures,long rejectedTasks,long averageTickNanos,long tickSamples){}
}