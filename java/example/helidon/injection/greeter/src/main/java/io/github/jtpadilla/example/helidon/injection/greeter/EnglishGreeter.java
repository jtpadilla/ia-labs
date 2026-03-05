package io.github.jtpadilla.example.helidon.injection.greeter;

import io.helidon.service.registry.Service;

@Service.Singleton
@Service.Named("english")
public class EnglishGreeter implements Greeter {
    @Override
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
