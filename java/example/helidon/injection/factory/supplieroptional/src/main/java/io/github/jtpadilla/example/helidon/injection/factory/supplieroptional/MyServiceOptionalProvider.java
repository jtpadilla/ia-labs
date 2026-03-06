package io.github.jtpadilla.example.helidon.injection.factory.supplieroptional;

import io.helidon.service.registry.Service;

import java.util.Optional;
import java.util.function.Supplier;

@Service.Singleton
public class MyServiceOptionalProvider implements Supplier<Optional<MyService>> {

    @Override
    public Optional<MyService> get() {
        System.out.println("MyServiceOptionalProvider.get()");
        //return Optional.of(new MyService());
        return Optional.empty();
    }

}
