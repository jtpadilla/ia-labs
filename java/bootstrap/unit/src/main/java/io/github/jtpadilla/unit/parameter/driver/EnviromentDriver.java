package io.github.jtpadilla.unit.parameter.driver;

import io.github.jtpadilla.unit.parameter.IParameterDriver;

import java.util.Map;
import java.util.Optional;

public class EnviromentDriver implements IParameterDriver {

    final private Map<String, String> env;

    EnviromentDriver() {
        this.env = System.getenv();
    }

    @Override
    public Optional<String> resolveParameterName(String name) {
        return Optional.ofNullable(env.get(name));
    }

}
