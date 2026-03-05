package io.github.jtpadilla.example.helidon.injection.qualifiers.custom;

import io.helidon.service.registry.Service;

@Green
@Service.Singleton
public class GreenColor implements Color {

    @Override
    public String hexCode() {
        return "008000";
    }

    @Override
    public String toString() {
        return hexCode();
    }

}