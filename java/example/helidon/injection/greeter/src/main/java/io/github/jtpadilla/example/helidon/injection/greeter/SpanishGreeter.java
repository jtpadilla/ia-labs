package io.github.jtpadilla.example.helidon.injection.greeter;

import io.helidon.service.registry.Service;

@Service.Singleton
@Service.Named("spanish")
public class SpanishGreeter implements Greeter {
    @Override
    public String greet(String name) {
        return "¡Hola, " + name + "!";
    }
}
