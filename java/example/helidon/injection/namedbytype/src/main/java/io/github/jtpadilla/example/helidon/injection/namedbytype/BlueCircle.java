package io.github.jtpadilla.example.helidon.injection.namedbytype;

import io.helidon.service.registry.Service;

@Service.Singleton
public record BlueCircle(@Service.NamedByType(BlueNamedByType.class) Color color) {
}
