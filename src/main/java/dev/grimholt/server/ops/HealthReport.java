package dev.grimholt.server.ops;
import java.util.List;
public record HealthReport(Status status,List<String> checks){public enum Status{HEALTHY,DEGRADED,FAILED}public HealthReport{checks=List.copyOf(checks);}}
