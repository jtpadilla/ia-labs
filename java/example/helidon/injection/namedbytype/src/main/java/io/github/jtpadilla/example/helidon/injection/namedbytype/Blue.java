package io.github.jtpadilla.example.helidon.injection.namedbytype;

import io.helidon.service.registry.Service;

@Service.Singleton
@Service.NamedByType(Blue.class)
public class Blue implements Color {

    @Override
    public String hexCode() {
        return "0000FF";
    }

    @Override
    public String toString() {
        return hexCode();
    }

}
