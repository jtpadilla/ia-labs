package io.github.jtpadilla.unit.parameter.impl.stringchoice;

import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;
import io.github.jtpadilla.unit.parameter.impl.stringparameter.StringParameterValue;

import java.util.Collection;

public class StringChoiceParameterDescriptorRequired extends StringChoiceParameterDescriptor {

    public StringChoiceParameterDescriptorRequired(String name, String description, Collection<String> choices) {
        super(name, description, choices);
    }

    @Override
    protected StringParameterValue createStringParameterValue(String rawValue) throws ParameterValueException {
        if (rawValue == null) {
            throw new ParameterValueException("El parametro no esta informado.");
        } else {
            return new StringParameterValue(false, false, selectChoice(rawValue));
        }
    }

}
