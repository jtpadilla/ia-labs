package io.github.jtpadilla.unit.test.parameter;

import io.github.jtpadilla.unit.parameter.IParameterDescriptor;
import io.github.jtpadilla.unit.parameter.IParameterReader;
import io.github.jtpadilla.unit.parameter.IParameterReaderBuilder;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class ParameterTest {

    static private final IParameterDescriptor INTEGER_REQUIRED = IParameterDescriptor.createIntegerParameterDescriptor(
            "INTEGER_REQUIRED",
            "Parametro Integer requerido",
            0,
            10
    );

    static private final IParameterDescriptor INTEGER_DEFAULT = IParameterDescriptor.createIntegerParameterDefaultDescriptor(
            "INTEGER_DEFAULT",
            "Parametro Integer por defecto",
            0,
            10,
            5
    );

    static private final IParameterDescriptor INTEGER_OPTIONAL = IParameterDescriptor.createIntegerParameterOptionalDescriptor(
            "INTEGER_OPTIONAL",
            "Parametro Integer opcional",
            0,
            10
    );

    static private final IParameterDescriptor BOOLEAN_REQUIRED = IParameterDescriptor.createBooleanParameterDescriptor(
            "BOOLEAN_REQUIRED",
            "Parametro boolean requerido"
    );

    static private final IParameterDescriptor BOOLEAN_DEFAULT = IParameterDescriptor.createBooleanParameterDefaultDescriptor(
            "BOOLEAN_DEFAULT",
            "Parametro boolean por defecto",
            true
    );

    static private final IParameterDescriptor BOOLEAN_OPTIONAL = IParameterDescriptor.createBooleanParameterOptionalDescriptor(
            "BOOLEAN_OPTIONAL",
            "Parametro boolean opcional"
    );

    static private final IParameterDescriptor STRING_REQUIRED = IParameterDescriptor.createStringParameterDescriptor(
            "STRING_REQUIRED",
            "Parametro string requerido"
    );

    static private final IParameterDescriptor STRING_DEFAULT = IParameterDescriptor.createStringParameterDefaultDescriptor(
            "STRING_DEFAULT",
            "Parametro string por defecto",
            "default-value-xxxx"
    );

    static private final IParameterDescriptor STRING_OPTIONAL = IParameterDescriptor.createStringParameterOptionalDescriptor(
            "STRING_OPTIONAL",
            "Parametro string opcional"
    );

    static private final IParameterDescriptor CHOICE_REQUIRED = IParameterDescriptor.createStringChoiceParameterDescriptor(
            "CHOICE_REQUIRED",
            "Parametro de tipo choice",
            Stream.of(ParameterChoiceType.values()).map(Enum::toString).collect(Collectors.toList())
    );

    static private final IParameterDescriptor CHOICE_DEFAULT = IParameterDescriptor.createStringChoiceParameterDefaultDescriptor(
            "CHOICE_DEFAULT",
            "Parametro de tipo choice",
            Stream.of(ParameterChoiceType.values()).map(Enum::toString).collect(Collectors.toList()),
            ParameterChoiceType.TYPE_A.toString()
    );

    static private final IParameterDescriptor CHOICE_OPTIONAL = IParameterDescriptor.createStringChoiceParameterOptionalDescriptor(
            "CHOICE_OPTIONAL",
            "Parametro de tuipo choice",
            Stream.of(ParameterChoiceType.values()).map(Enum::toString).collect(Collectors.toList())
    );

    @Test
    public void testParameterReader() {

        final IParameterReader parameterReader = IParameterReaderBuilder.create(UnitResource.UNIT)
                .addParameterDescriptor(INTEGER_REQUIRED)
                .addParameterDescriptor(INTEGER_DEFAULT)
                .addParameterDescriptor(INTEGER_OPTIONAL)
                .addParameterDescriptor(BOOLEAN_REQUIRED)
                .addParameterDescriptor(BOOLEAN_DEFAULT)
                .addParameterDescriptor(BOOLEAN_OPTIONAL)
                .addParameterDescriptor(STRING_REQUIRED)
                .addParameterDescriptor(STRING_DEFAULT)
                .addParameterDescriptor(STRING_OPTIONAL)
                .addParameterDescriptor(CHOICE_REQUIRED)
                .addParameterDescriptor(CHOICE_DEFAULT)
                .addParameterDescriptor(CHOICE_OPTIONAL)
                .build();

        assertEquals(1, parameterReader.getValue(INTEGER_REQUIRED).getIntegerValue().intValue());
        assertEquals(2, parameterReader.getValue(INTEGER_DEFAULT).getIntegerValue().intValue());
        assertEquals(-11111111, parameterReader.getValue(INTEGER_OPTIONAL).getIntegerOptionalValue().orElse(-11111111).intValue());

        assertEquals(true, parameterReader.getValue(BOOLEAN_REQUIRED).getBooleanValue());
        assertEquals(false, parameterReader.getValue(BOOLEAN_DEFAULT).getBooleanValue());
        assertEquals(false, parameterReader.getValue(BOOLEAN_OPTIONAL).getBooleanOptionalValue().orElse(true));

        assertEquals("lacadena", parameterReader.getValue(STRING_REQUIRED).getStringValue());
        assertEquals("default-cadena-resource", parameterReader.getValue(STRING_DEFAULT).getStringValue());
        assertEquals("optional-cadena-resource", parameterReader.getValue(STRING_OPTIONAL).getStringOptionalValue().orElse("optional-value-1111"));

        assertEquals(ParameterChoiceType.TYPE_A, ParameterChoiceType.valueOf(parameterReader.getValue(CHOICE_REQUIRED).getStringValue()));
        assertEquals(ParameterChoiceType.TYPE_B, ParameterChoiceType.valueOf(parameterReader.getValue(CHOICE_DEFAULT).getStringValue()));
        assertEquals(ParameterChoiceType.TYPE_C, ParameterChoiceType.valueOf(parameterReader.getValue(CHOICE_OPTIONAL).getStringOptionalValue().orElse("TYPE_C")));

    }

}
