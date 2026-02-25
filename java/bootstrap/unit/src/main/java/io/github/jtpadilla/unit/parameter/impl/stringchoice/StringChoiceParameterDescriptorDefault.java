package io.github.jtpadilla.unit.parameter.impl.stringchoice;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;
import io.github.jtpadilla.unit.parameter.impl.stringparameter.StringParameterValue;

import java.util.Collection;

public class StringChoiceParameterDescriptorDefault extends StringChoiceParameterDescriptor {

    final private String defaultValue;

    public StringChoiceParameterDescriptorDefault(String name, String description, Collection<String> choices, String defaultValue) {
        super(name, description, choices);
        this.defaultValue = defaultValue;
    }

    @Override
    public void printInfo(IUnitLogger logger) {
        super.printInfo(logger);
        logger.info(String.format("=   - DefaultValue=%s", defaultValue));
    }

    @Override
    protected StringParameterValue createStringParameterValue(String rawValue) throws ParameterValueException {
        if (rawValue == null) {
            return new StringParameterValue(false, true, defaultValue);
        } else {
            return new StringParameterValue(false, false, selectChoice(rawValue));
        }
    }

}
