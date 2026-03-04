package io.github.jtpadilla.example.dagger.health.checks;

public interface HealthCheck {
    String getName();
    boolean isHealthy();
    String getStatus();
}
