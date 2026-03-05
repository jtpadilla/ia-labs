package io.github.jtpadilla.example.helidon.injection.namedbytype;

import io.helidon.service.registry.Service;

@Service.Singleton
public record GreenCircle(@Service.NamedByType(Green.class) Color color) {
}