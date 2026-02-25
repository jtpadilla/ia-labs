package io.github.jtpadilla.unit.parameter.impl.stringparameter;

import io.github.jtpadilla.unit.parameter.IParameterValue;
import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;

import java.util.Optional;

public class StringParameterValue implements IParameterValue {

    final private boolean isOptional;
    final private boolean usedDefaultValue;
    final private String value;

    final private ParameterValueException ex;

    public StringParameterValue(boolean isOptional, boolean usedDefaultValue, String value) {
        this.isOptional = isOptional;
        this.usedDefaultValue = usedDefaultValue;
        this.value = value;
        this.ex = null;
    }

    public StringParameterValue(ParameterValueException ex) {
        this.isOptional = false;
        this.usedDefaultValue = false;
        this.value = null;
        this.ex = ex;
    }

    @Override
    public String getStringValue() {
        if (isOptional) {
            throw new IllegalStateException("Los parametros definidos como opcionales deben leerse con el accesor opcional.");
        }
        return value;
    }

    @Override
    public Optional<String> getStringOptionalValue() {
        return Optional.ofNullable(value);
    }

    @Override
    public boolean isDefaultValue() {
        return usedDefaultValue;
    }

    @Override
    public boolean hasError() {
        return ex != null;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        if (!hasError()) {

            // Hay un valor
            builder.append(value == null ? "null" : value.toString());

            // Y se utilizo el valor por defecto porque no entrego ninguno
            if (usedDefaultValue) {
                builder.append("  ");
                builder.append(DEFAULT_VALUE_LABEL);
            }

        } else {

            // Se produjo un error al intentar obtener el valor desde el driver
            builder.append(String.format("ERROR: %s",  ex.getMessage()));

        }

        return builder.toString();

    }

}
