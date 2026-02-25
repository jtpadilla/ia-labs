package io.github.jtpadilla.unit.parameter.impl.booleanparameter;

import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;

public class BooleanParameterDescriptorRequired extends BooleanParameterDescriptor {

    public BooleanParameterDescriptorRequired(String name, String description) {
        super(name, description);
    }

    protected BooleanParameterValue createBooleanParameterValue(String rawValue) throws ParameterValueException {
        if (rawValue == null) {
            throw new ParameterValueException("El parametro no esta informado.");
        } else {
            return new BooleanParameterValue(false, false, parseBoolean(rawValue));
        }
    }

}
