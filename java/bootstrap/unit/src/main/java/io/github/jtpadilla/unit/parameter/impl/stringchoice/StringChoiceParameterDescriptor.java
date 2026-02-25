package io.github.jtpadilla.unit.parameter.impl.stringchoice;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.parameter.IParameterDescriptor;
import io.github.jtpadilla.unit.parameter.IParameterDriver;
import io.github.jtpadilla.unit.parameter.IParameterValue;
import io.github.jtpadilla.unit.parameter.impl.ParameterValueException;
import io.github.jtpadilla.unit.parameter.impl.stringparameter.StringParameterValue;

import java.util.Collection;
import java.util.Optional;

public abstract class StringChoiceParameterDescriptor implements IParameterDescriptor {

    private final String name;
    private final String description;
    private final Collection<String> choices;

    public StringChoiceParameterDescriptor(String name, String description, Collection<String> choices) {
        this.name = name;
        this.description = description;
        this.choices = choices;
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
        logger.info(String.format("=   - validValues=[%s]", String.join(",", choices)));
    }

    public IParameterValue createValue(IParameterDriver parameterDriver) {
        try {
            return createStringParameterValue(parameterDriver.resolveParameterName(getName()).orElse(null));
        } catch (ParameterValueException e) {
            return new StringParameterValue(e);
        }
    }

    abstract protected StringParameterValue createStringParameterValue(String rawValue) throws ParameterValueException;

    //////////////////////////////////////////////////////////
    // Utilidades...
    //////////////////////////////////////////////////////////

    String selectChoice(String rawValue) throws ParameterValueException {
        Optional<String> choice = choices.stream().filter(c->c.equals(rawValue)).findFirst();
        return choice.orElseThrow(()->new ParameterValueException(String.format("La opcion '%s' que es invalida", rawValue)));
    }

}
