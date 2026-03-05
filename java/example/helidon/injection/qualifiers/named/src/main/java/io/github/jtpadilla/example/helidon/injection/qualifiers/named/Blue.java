package io.github.jtpadilla.example.helidon.injection.qualifiers.named;

import io.helidon.service.registry.Service;

@Service.Named("blue")
@Service.Singleton
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
