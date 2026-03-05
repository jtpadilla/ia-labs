package io.github.jtpadilla.example.helidon.injection.qualifiers.custom;

import io.helidon.service.registry.Service;

@Service.Singleton
public record GreenCircle(@Service.NamedByType(GreenColor.class) Color color) {
}