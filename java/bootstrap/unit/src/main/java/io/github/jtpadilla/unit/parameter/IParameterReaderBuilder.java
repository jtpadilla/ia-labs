package io.github.jtpadilla.unit.parameter;

import io.github.jtpadilla.unit.Unit;
import io.github.jtpadilla.unit.parameter.driver.DriverSelector;
import io.github.jtpadilla.unit.parameter.impl.ParameterReaderBuilder;

public interface IParameterReaderBuilder {

    IParameterReaderBuilder addParameterDescriptor(IParameterDescriptor parameterDescriptor);

    IParameterReader build();

    static IParameterReaderBuilder create(Unit unit) {
        return new ParameterReaderBuilder(unit, DriverSelector.getDriver());
    }

}
