package dev.grimholt.server.ops;
import dev.grimholt.api.GrimholtServer;
import java.util.ArrayList;
public final class HealthCheck {
 private HealthCheck(){}
 public static HealthReport check(GrimholtServer server){var checks=new ArrayList<String>();if(server.state()!=dev.grimholt.api.ServerState.RUNNING)checks.add("server-state="+server.state());if(server.worlds().isEmpty())checks.add("no-world");if(checks.isEmpty())return new HealthReport(HealthReport.Status.HEALTHY,checks);return new HealthReport(HealthReport.Status.DEGRADED,checks);}
}