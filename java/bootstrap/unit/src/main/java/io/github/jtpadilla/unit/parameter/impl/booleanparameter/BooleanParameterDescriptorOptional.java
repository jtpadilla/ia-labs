package io.github.jtpadilla.unit.parameter.impl.booleanparameter;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;

public class BooleanParameterDescriptorOptional extends BooleanParameterDescriptor {

    public BooleanParameterDescriptorOptional(String name, String description) {
        super(name, description);
    }

    @Override
    public void printInfo(IUnitLogger logger) {
        super.printInfo(logger);
        logger.info("=   - Is optional");
    }

    protected BooleanParameterValue createBooleanParameterValue(String rawValue) throws ParameterValueException {
        if (rawValue == null) {
            return new BooleanParameterValue(true, false, null);
        } else {
            return new BooleanParameterValue(true, false, parseBoolean(rawValue));
        }
    }

}
