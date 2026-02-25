package io.github.jtpadilla.unit.parameter.impl.booleanparameter;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;

public class BooleanParameterDescriptorDefault extends BooleanParameterDescriptor {

    final private boolean defaultValue;

    public BooleanParameterDescriptorDefault(String name, String description, boolean defaultValue) {
        super(name, description);
        this.defaultValue = defaultValue;
    }

    @Override
    public void printInfo(IUnitLogger logger) {
        super.printInfo(logger);
        logger.info(String.format("=   - DefaultValue=%b", defaultValue));
    }

    protected BooleanParameterValue createBooleanParameterValue(String rawValue) throws ParameterValueException {
        if (rawValue == null) {
            return new BooleanParameterValue(false, true, defaultValue);
        } else {
            return new BooleanParameterValue(false, false, parseBoolean(rawValue));
        }
    }

}
