package io.github.jtpadilla.unit.parameter.impl.stringparameter;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;

public class StringParameterDescriptorDefault extends StringParameterDescriptor {

    final private String defaultValue;

    public StringParameterDescriptorDefault(String name, String description, String defaultValue) {
        super(name, description);
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
            return new StringParameterValue(false, false, rawValue);
        }
    }

}
