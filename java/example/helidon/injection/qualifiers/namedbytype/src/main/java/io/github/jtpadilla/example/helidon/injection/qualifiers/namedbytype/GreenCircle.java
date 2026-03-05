package io.github.jtpadilla.example.helidon.injection.qualifiers.namedbytype;

import io.helidon.service.registry.Service;

@Service.Singleton
public record GreenCircle(@Service.NamedByType(GreenNamedByType.class) Color color) {
}