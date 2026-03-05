package io.github.jtpadilla.example.helidon.injection.greeter;

import io.helidon.service.registry.Services;

public class Main {

    public static void main(String[] args) {

        // En Helidon 4.3.4, Services.get() es la forma más directa de acceder al registro global
        GreetingService service = Services.get(GreetingService.class);

        // Llamar al método del servicio
        service.sayHello("IA Lab");

    }

}
