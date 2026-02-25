package io.github.jtpadilla.unit.parameter.impl.integerparameter;

import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;

public class IntegerParameterDescriptorRequired extends IntegerParameterDescriptor {

    public IntegerParameterDescriptorRequired(String name, String description, int minValue, int maxValue) {
        super(name, description, minValue, maxValue);
    }

    @Override
    public IntegerParameterValue createIntegerParameterValue(String rawValue) throws ParameterValueException {
        if (rawValue == null) {
            throw new ParameterValueException("El parametro no esta informado.");
        } else {
            return new IntegerParameterValue(false, false, checkInteger(parseInteger(rawValue)));
        }
    }

}
