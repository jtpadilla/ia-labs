package io.github.jtpadilla.unit.parameter.impl;

import io.github.jtpadilla.unit.parameter.IParameterDescriptor;
import io.github.jtpadilla.unit.parameter.IParameterValue;

import java.util.Objects;

class ParameterReaderEntry {

    final private IParameterDescriptor descriptor;
    final private IParameterValue value;

    ParameterReaderEntry(IParameterDescriptor descriptor, IParameterValue value) {
        Objects.requireNonNull(descriptor, ()->"El parametro 'descriptor' es null");
        Objects.requireNonNull(value, ()->"El parametro 'value' es null");
        this.descriptor = descriptor;
        this.value = value;
    }

    IParameterDescriptor getDescriptor() {
        return descriptor;
    }

    IParameterValue getValue() {
        return value;
    }

}
