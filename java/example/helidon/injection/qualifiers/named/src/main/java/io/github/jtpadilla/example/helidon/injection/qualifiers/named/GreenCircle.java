package io.github.jtpadilla.example.helidon.injection.qualifiers.named;

import io.helidon.service.registry.Service;

@Service.Singleton
public record GreenCircle(@Service.Named("green") Color color) {
}