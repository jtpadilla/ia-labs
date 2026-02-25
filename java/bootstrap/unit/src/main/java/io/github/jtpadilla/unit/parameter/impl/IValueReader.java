package io.github.jtpadilla.unit.parameter.impl;

import io.github.jtpadilla.unit.parameter.IParameterDriver;
import io.github.jtpadilla.unit.parameter.IParameterValue;

public interface IValueReader {
    IParameterValue getValue(IParameterDriver driver) throws ParameterValueException;
}
