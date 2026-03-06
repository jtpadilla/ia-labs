package io.github.jtpadilla.example.helidon.injection.factory.supplier;

import io.helidon.service.registry.Service;

import java.util.function.Supplier;

@Service.Singleton
public class MyServiceProvider implements Supplier<MyService> {

    @Override
    public MyService get() {
        return new MyService();
    }

}
