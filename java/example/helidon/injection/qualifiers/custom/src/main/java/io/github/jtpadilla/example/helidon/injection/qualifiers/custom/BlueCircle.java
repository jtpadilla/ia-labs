package io.github.jtpadilla.example.helidon.injection.qualifiers.custom;

import io.helidon.service.registry.Service;

@Service.Singleton
public record BlueCircle(@Service.NamedByType(BlueColor.class) Color color) {
}
