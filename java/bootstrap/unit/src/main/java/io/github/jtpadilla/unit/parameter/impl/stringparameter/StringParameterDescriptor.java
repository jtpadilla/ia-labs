package io.github.jtpadilla.unit.parameter.impl.stringparameter;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.parameter.IParameterDescriptor;
import io.github.jtpadilla.unit.parameter.IParameterDriver;
import io.github.jtpadilla.unit.parameter.IParameterValue;
import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;

abstract public class StringParameterDescriptor implements IParameterDescriptor {

    private final String name;
    private final String description;

    StringParameterDescriptor(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void printInfo(IUnitLogger logger) {
        logger.info(String.format("= %s", getName()));
        logger.info(String.format("=   - Description=%s", getDescription()));
    }

    public IParameterValue createValue(IParameterDriver parameterDriver) {
        try {
            return createStringParameterValue(parameterDriver.resolveParameterName(getName()).orElse(null));
        } catch (ParameterValueException e) {
            return new StringParameterValue(e);
        }
    }

    abstract protected StringParameterValue createStringParameterValue(String rawValue) throws ParameterValueException;

}
