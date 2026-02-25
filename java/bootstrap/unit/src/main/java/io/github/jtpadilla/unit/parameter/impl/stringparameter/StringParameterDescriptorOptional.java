package io.github.jtpadilla.unit.parameter.impl.stringparameter;

import io.github.jtpadilla.unit.logger.IUnitLogger;

public class StringParameterDescriptorOptional extends StringParameterDescriptor {

    public StringParameterDescriptorOptional(String name, String description) {
        super(name, description);
    }

    @Override
    public void printInfo(IUnitLogger logger) {
        super.printInfo(logger);
        logger.info("=   - Is optional");
    }

    @Override
    protected StringParameterValue createStringParameterValue(String rawValue) {
        if (rawValue == null) {
            return new StringParameterValue(true, false, null);
        } else {
            return new StringParameterValue(true, false, rawValue);
        }
    }

}
