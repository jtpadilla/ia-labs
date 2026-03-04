package io.github.jtpadilla.example.dagger.health.checks;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Random;

@Singleton
public class CpuCheck implements HealthCheck {

    private final Random random = new Random();

    @Inject
    public CpuCheck() {}

    @Override
    public String getName() {
        return "CPU";
    }

    @Override
    public boolean isHealthy() {
        return random.nextDouble() < 0.95;
    }

    @Override
    public String getStatus() {
        return "Load: " + (random.nextInt(100)) + "%";
    }

}
