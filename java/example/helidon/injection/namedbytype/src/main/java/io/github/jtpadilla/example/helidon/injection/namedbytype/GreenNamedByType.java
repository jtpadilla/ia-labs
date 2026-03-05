package io.github.jtpadilla.example.helidon.injection.namedbytype;

import io.helidon.service.registry.Service;

@Service.Singleton
@Service.NamedByType(GreenNamedByType.class)
public class GreenNamedByType implements Color {

    @Override
    public String hexCode() {
        return "008000";
    }

    @Override
    public String toString() {
        return hexCode();
    }

}