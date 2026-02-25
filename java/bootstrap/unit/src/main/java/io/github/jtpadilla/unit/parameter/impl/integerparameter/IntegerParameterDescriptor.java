package io.github.jtpadilla.unit.parameter.impl.integerparameter;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.parameter.IParameterDescriptor;
import io.github.jtpadilla.unit.parameter.IParameterDriver;
import io.github.jtpadilla.unit.parameter.IParameterValue;
import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;

abstract public class IntegerParameterDescriptor implements IParameterDescriptor {

    private final String name;
    private final String description;
    private final int minValue;
    private final int maxValue;

    IntegerParameterDescriptor(String name, String description, int minValue, int maxValue) {
        this.name = name;
        this.description = description;
        this.minValue = minValue;
        this.maxValue = maxValue;
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
        logger.info(String.format("=   - minValue=%d", minValue));
        logger.info(String.format("=   - maxValue=%d", maxValue));
    }

    public IParameterValue createValue(IParameterDriver parameterDriver) {
        try {
            return createIntegerParameterValue(parameterDriver.resolveParameterName(getName()).orElse(null));
        } catch (ParameterValueException e) {
            return new IntegerParameterValue(e);
        }
    }

    abstract protected IntegerParameterValue createIntegerParameterValue(String rawValue) throws ParameterValueException;

    ////////////////////////////////////////////////////
    // Utilidades
    ////////////////////////////////////////////////////

    int checkInteger(int value) throws ParameterValueException {
        if (value < minValue) {
            throw new ParameterValueException(String.format("El valor numerico de %s no puede ser menor de %d", getName(), minValue));
        } else if (value > maxValue) {
            throw new ParameterValueException(String.format("El valor numerico de %s no puede ser mayor de %d", getName(), maxValue));
        } else {
            return value;
        }
    }

    int parseInteger(String rawValue) throws ParameterValueException {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            throw new ParameterValueException("El valor no se puede interpretar correctamente");
        }
    }

}
