package io.github.jtpadilla.unit.parameter.impl.booleanparameter;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.parameter.IParameterDescriptor;
import io.github.jtpadilla.unit.parameter.IParameterDriver;
import io.github.jtpadilla.unit.parameter.IParameterValue;
import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;

abstract public class BooleanParameterDescriptor implements IParameterDescriptor {

    private final String name;
    private final String description;

    public BooleanParameterDescriptor(String name, String description) {
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
            return createBooleanParameterValue(parameterDriver.resolveParameterName(getName()).orElse(null));
        } catch (ParameterValueException e) {
            return new BooleanParameterValue(e);
        }
    }

    abstract protected BooleanParameterValue createBooleanParameterValue(String rawValue) throws ParameterValueException;

    ////////////////////////////////////////////////////
    // Utilidades
    ////////////////////////////////////////////////////

    Boolean parseBoolean(String rawValue) throws ParameterValueException {
        try {
            return Boolean.parseBoolean(rawValue);
        } catch (NumberFormatException e) {
            throw new ParameterValueException("El valor no se puede interpretar correctamente");
        }
    }

}
