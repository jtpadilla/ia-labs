package io.github.jtpadilla.example.helidon.injection.greeter;

import io.helidon.service.registry.Service;

@Service.Singleton
public class GreetingService {
    private final Greeter englishGreeter;
    private final Greeter spanishGreeter;

    @Service.Inject
    public GreetingService(
            @Service.Named("english") Greeter englishGreeter,
            @Service.Named("spanish") Greeter spanishGreeter) {
        this.englishGreeter = englishGreeter;
        this.spanishGreeter = spanishGreeter;
    }

    public void sayHello(String name) {
        System.out.println(englishGreeter.greet(name));
        System.out.println(spanishGreeter.greet(name));
    }
}
