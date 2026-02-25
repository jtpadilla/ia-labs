package io.github.jtpadilla.unit.parameter.impl.stringchoice;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;
import io.github.jtpadilla.unit.parameter.impl.stringparameter.StringParameterValue;

import java.util.Collection;

public class StringChoiceParameterDescriptorOptional extends StringChoiceParameterDescriptor {

    public StringChoiceParameterDescriptorOptional(String name, String description, Collection<String> choices) {
        super(name, description, choices);
    }

    @Override
    public void printInfo(IUnitLogger logger) {
        super.printInfo(logger);
        logger.info("=   - Is optional");
    }

    @Override
    protected StringParameterValue createStringParameterValue(String rawValue) throws ParameterValueException {
        if (rawValue == null) {
            return new StringParameterValue(true, false, null);
        } else {
            return new StringParameterValue(false, false, selectChoice(rawValue));
        }
    }

}
