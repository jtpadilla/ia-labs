package io.github.jtpadilla.unit.parameter;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.parameter.impl.booleanparameter.BooleanParameterDescriptorDefault;
import io.github.jtpadilla.unit.parameter.impl.booleanparameter.BooleanParameterDescriptorOptional;
import io.github.jtpadilla.unit.parameter.impl.booleanparameter.BooleanParameterDescriptorRequired;
import io.github.jtpadilla.unit.parameter.impl.integerparameter.IntegerParameterDescriptorDefault;
import io.github.jtpadilla.unit.parameter.impl.integerparameter.IntegerParameterDescriptorOptional;
import io.github.jtpadilla.unit.parameter.impl.integerparameter.IntegerParameterDescriptorRequired;
import io.github.jtpadilla.unit.parameter.impl.stringchoice.StringChoiceParameterDescriptorDefault;
import io.github.jtpadilla.unit.parameter.impl.stringchoice.StringChoiceParameterDescriptorOptional;
import io.github.jtpadilla.unit.parameter.impl.stringchoice.StringChoiceParameterDescriptorRequired;
import io.github.jtpadilla.unit.parameter.impl.stringparameter.StringParameterDescriptorDefault;
import io.github.jtpadilla.unit.parameter.impl.stringparameter.StringParameterDescriptorOptional;
import io.github.jtpadilla.unit.parameter.impl.stringparameter.StringParameterDescriptorRequired;

import java.util.Collection;

public interface IParameterDescriptor {

    String getName();
    String getDescription();

    void printInfo(IUnitLogger logger);

    IParameterValue createValue(IParameterDriver parameterDriver);

    static IParameterDescriptor createIntegerParameterDescriptor(String name, String description, int minValue, int maxValue) {
        return new IntegerParameterDescriptorRequired(name, description, minValue, maxValue);
    }

    static IParameterDescriptor createIntegerParameterOptionalDescriptor(String name, String description, int minValue, int maxValue) {
        return new IntegerParameterDescriptorOptional(name, description, minValue, maxValue);
    }

    static IParameterDescriptor createIntegerParameterDefaultDescriptor(String name, String description, int minValue, int maxValue, int defaultValue) {
        return new IntegerParameterDescriptorDefault(name, description, minValue, maxValue, defaultValue);
    }

    static IParameterDescriptor createBooleanParameterDescriptor(String name, String description) {
        return new BooleanParameterDescriptorRequired(name, description);
    }

    static IParameterDescriptor createBooleanParameterOptionalDescriptor(String name, String description) {
        return new BooleanParameterDescriptorOptional(name, description);
    }

    static IParameterDescriptor createBooleanParameterDefaultDescriptor(String name, String description, boolean defaultValue) {
        return new BooleanParameterDescriptorDefault(name, description, defaultValue);
    }

    static IParameterDescriptor createStringParameterDescriptor(String name, String description) {
        return new StringParameterDescriptorRequired(name, description);
    }

    static IParameterDescriptor createStringParameterDefaultDescriptor(String name, String description, String defaultValue) {
        return new StringParameterDescriptorDefault(name, description, defaultValue);
    }

    static IParameterDescriptor createStringParameterOptionalDescriptor(String name, String description) {
        return new StringParameterDescriptorOptional(name, description);
    }

    static IParameterDescriptor createStringChoiceParameterDescriptor(String name, String description, Collection<String> choices) {
        return new StringChoiceParameterDescriptorRequired(name, description, choices);
    }

    static IParameterDescriptor createStringChoiceParameterDefaultDescriptor(String name, String description, Collection<String> choices, String defaultValue) {
        return new StringChoiceParameterDescriptorDefault(name, description, choices, defaultValue);
    }

    static IParameterDescriptor createStringChoiceParameterOptionalDescriptor(String name, String description, Collection<String> choices) {
        return new StringChoiceParameterDescriptorOptional(name, description, choices);
    }

}
