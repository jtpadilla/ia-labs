package io.github.jtpadilla.example.helidon.injection.qualifiers.custom;

import io.helidon.service.registry.Service;

@Blue
@Service.Singleton
public class BlueColor implements Color {

    @Override
    public String hexCode() {
        return "0000FF";
    }

    @Override
    public String toString() {
        return hexCode();
    }

}
