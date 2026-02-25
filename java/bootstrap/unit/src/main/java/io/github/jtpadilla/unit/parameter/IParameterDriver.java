package io.github.jtpadilla.unit.parameter;

import java.util.Optional;

public interface IParameterDriver {
    Optional<String> resolveParameterName(String name);
}
