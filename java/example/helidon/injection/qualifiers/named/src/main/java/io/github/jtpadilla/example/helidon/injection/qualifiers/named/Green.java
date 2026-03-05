package io.github.jtpadilla.example.helidon.injection.qualifiers.named;

import io.helidon.service.registry.Service;

@Service.Named("green")
@Service.Singleton
public class Green implements Color {

    @Override
    public String hexCode() {
        return "008000";
    }

    @Override
    public String toString() {
        return hexCode();
    }

}