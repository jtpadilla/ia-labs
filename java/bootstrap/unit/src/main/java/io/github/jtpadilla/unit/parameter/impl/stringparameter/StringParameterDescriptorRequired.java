package io.github.jtpadilla.unit.parameter.impl.stringparameter;

import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;

public class StringParameterDescriptorRequired extends StringParameterDescriptor {

    public StringParameterDescriptorRequired(String name, String description) {
        super(name, description);
    }

    @Override
    protected StringParameterValue createStringParameterValue(String rawValue) throws ParameterValueException {
        if (rawValue == null) {
            throw new ParameterValueException("El parametro no esta informado.");
        } else {
            return new StringParameterValue(false, false, rawValue);
        }
    }

}
