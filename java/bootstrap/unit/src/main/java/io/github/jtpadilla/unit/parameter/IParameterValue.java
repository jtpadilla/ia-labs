package io.github.jtpadilla.unit.parameter;

import java.util.Optional;

public interface IParameterValue {

    String DEFAULT_VALUE_LABEL = "(Seleccionado el valor por defecto)";

    default String getStringValue() {
        throw new IllegalArgumentException("El parametro no tiene un valor de tipo String");
    }

    default Optional<String> getStringOptionalValue() {
        throw new IllegalArgumentException("El parametro no tiene un valor de tipo String");
    }

    default Integer getIntegerValue() {
        throw new IllegalArgumentException("El parametro no tiene un valor de tipo Int");
    }

    default Optional<Integer> getIntegerOptionalValue() {
        throw new IllegalArgumentException("El parametro no tiene un valor de tipo Int");
    }

    default Boolean getBooleanValue() {
        throw new IllegalArgumentException("El parametro no tiene un valor de tipo Boolean");
    }

    default Optional<Boolean> getBooleanOptionalValue() {
        throw new IllegalArgumentException("El parametro no tiene un valor de tipo Boolean");
    }

    boolean isDefaultValue();

    boolean hasError();

}
