package io.github.jtpadilla.unit.parameter.impl.integerparameter;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;

public class IntegerParameterDescriptorDefault extends IntegerParameterDescriptor {

    final private int defaultValue;

    public IntegerParameterDescriptorDefault(String name, String description, int minValue, int maxValue, int defaultValue) {
        super(name, description, minValue, maxValue);
        this.defaultValue = defaultValue;
    }

    @Override
    public void printInfo(IUnitLogger logger) {
        super.printInfo(logger);
        logger.info(String.format("=   - DefaultValue=%d", defaultValue));
    }

    @Override
    public IntegerParameterValue createIntegerParameterValue(String rawValue) throws ParameterValueException {
        if (rawValue == null) {
            return new IntegerParameterValue(false, true, defaultValue);
        } else {
            return new IntegerParameterValue(false, false, checkInteger(parseInteger(rawValue)));
        }
    }

}
