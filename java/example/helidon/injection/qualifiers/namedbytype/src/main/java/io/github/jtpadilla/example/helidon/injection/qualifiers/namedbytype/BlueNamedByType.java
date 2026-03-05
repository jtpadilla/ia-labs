package io.github.jtpadilla.example.helidon.injection.qualifiers.namedbytype;

import io.helidon.service.registry.Service;

@Service.Singleton
@Service.NamedByType(BlueNamedByType.class)
public class BlueNamedByType implements Color {

    @Override
    public String hexCode() {
        return "0000FF";
    }

    @Override
    public String toString() {
        return hexCode();
    }

}
