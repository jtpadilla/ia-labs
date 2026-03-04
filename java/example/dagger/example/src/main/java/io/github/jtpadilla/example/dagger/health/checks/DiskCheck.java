package io.github.jtpadilla.example.dagger.health.checks;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Random;

@Singleton
public class DiskCheck implements HealthCheck {

    private final Random random = new Random();

    @Inject
    public DiskCheck() {}

    @Override
    public String getName() {
        return "Disk";
    }

    @Override
    public boolean isHealthy() {
        return random.nextDouble() < 0.99;
    }

    @Override
    public String getStatus() {
        return "Free: " + (random.nextInt(1024)) + "GB";
    }

}
