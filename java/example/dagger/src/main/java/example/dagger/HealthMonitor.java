package example.dagger;

import example.dagger.checks.HealthCheck;
import example.dagger.qualifiers.FastCheck;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Set;

@Singleton
public class HealthMonitor {

    private final Set<HealthCheck> allChecks;
    private final Map<String, HealthCheck> checkMap;
    private final Set<HealthCheck> fastChecks;
    private final String environment;

    @Inject
    public HealthMonitor(
            Set<HealthCheck> allChecks,
            Map<String, HealthCheck> checkMap,
            @FastCheck Set<HealthCheck> fastChecks,
            @Named("environment") String environment) {
        this.allChecks = allChecks;
        this.checkMap = checkMap;
        this.fastChecks = fastChecks;
        this.environment = environment;
    }

    public void runDiagnostics() {
        System.out.println("--- Starting Health Diagnostics for [" + environment + "] ---");
        System.out.println("All checks registered: " + allChecks.size());
        System.out.println("Fast checks registered: " + fastChecks.size());

        boolean allHealthy = true;
        for (HealthCheck check : allChecks) {
            boolean status = check.isHealthy();
            allHealthy &= status;
            System.out.printf("[%s] %s - %s\n",
                status ? "OK" : "FAIL",
                check.getName(),
                check.getStatus());
        }

        System.out.println("\nChecking specific components via Map:");
        if (checkMap.containsKey("cpu_status")) {
            System.out.println("CPU Map status: " + checkMap.get("cpu_status").getStatus());
        }

        System.out.println("--- Diagnostics Finished: " + (allHealthy ? "SYSTEM HEALTHY" : "SYSTEM DEGRADED") + " ---");
    }

}
