package io.github.jtpadilla.example.dagger.health.checks;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Random;

@Singleton
public class MemoryCheck implements HealthCheck {

    private final Random random = new Random();

    @Inject
    public MemoryCheck() {}

    @Override
    public String getName() {
        return "Memory";
    }

    @Override
    public boolean isHealthy() {
        return random.nextDouble() < 0.98;
    }

    @Override
    public String getStatus() {
        return "Used: " + (random.nextInt(16384)) + "MB";
    }

}
