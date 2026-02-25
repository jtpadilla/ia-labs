package io.github.jtpadilla.unit.parameter.impl.integerparameter;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;

public class IntegerParameterDescriptorOptional extends IntegerParameterDescriptor {

    public IntegerParameterDescriptorOptional(String name, String description, int minValue, int maxValue) {
        super(name, description, minValue, maxValue);
    }

    @Override
    public void printInfo(IUnitLogger logger) {
        super.printInfo(logger);
        logger.info("=   - Is optional");
    }

    @Override
    protected IntegerParameterValue createIntegerParameterValue(String rawValue) throws ParameterValueException {
        if (rawValue == null) {
            return new IntegerParameterValue(true, false, null);
        } else {
            return new IntegerParameterValue(true, false, checkInteger(parseInteger(rawValue)));
        }
    }

}
