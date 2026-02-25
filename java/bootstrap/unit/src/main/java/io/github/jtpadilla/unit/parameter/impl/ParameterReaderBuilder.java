package io.github.jtpadilla.unit.parameter.impl;

import io.github.jtpadilla.unit.Unit;
import io.github.jtpadilla.unit.parameter.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ParameterReaderBuilder implements IParameterReaderBuilder {

    final private Unit unit;

    private IParameterDriver currentDriver;
    final private List<ParameterReaderEntry> parameters = new ArrayList<>();

    public ParameterReaderBuilder(Unit unit, IParameterDriver defaultDriver) {
        Objects.requireNonNull(unit, "El parametro 'clientId' no pude ser null");
        Objects.requireNonNull(defaultDriver, "El parametro 'defaultDriver' no puede ser null");
        this.unit = unit;
        this.currentDriver = defaultDriver;
    }

    @Override
    public IParameterReaderBuilder addParameterDescriptor(IParameterDescriptor parameterDescriptor) {

        // No puede existir un parametro con el mismo nombra.
        if (parameters.stream().anyMatch(d -> d.getDescriptor().getName().equals(parameterDescriptor.getName()))) {
            throw new IllegalArgumentException(String.format("El parametro %s ya ha sido declarado previamente", parameterDescriptor.getName()));
        }

        // Se intenta obtener el valor del parametro
        IParameterValue value = parameterDescriptor.createValue(currentDriver);

        // Se incorpora el parametro
        parameters.add(new ParameterReaderEntry(parameterDescriptor, value));

        // Interface corrido
        return this;

    }

    @Override
    public IParameterReader build() {
        return new ParameterReader(unit, List.copyOf(parameters));
    }

}
