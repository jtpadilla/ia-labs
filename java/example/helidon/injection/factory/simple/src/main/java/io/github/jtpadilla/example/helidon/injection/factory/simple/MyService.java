package io.github.jtpadilla.example.helidon.injection.factory.simple;

import io.helidon.service.registry.Service;

@Service.Singleton
public class MyService implements MyContract {

    @Override
    public String getText() {
        return "Hola";
    }

}
