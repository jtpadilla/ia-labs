package io.github.jtpadilla.example.helidon.injection.greeter;

import io.helidon.service.registry.Service;

@Service.Contract
public interface Greeter {
    String greet(String name);
}
