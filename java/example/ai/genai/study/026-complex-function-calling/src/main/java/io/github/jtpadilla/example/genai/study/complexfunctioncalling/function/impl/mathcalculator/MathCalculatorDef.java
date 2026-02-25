package io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.mathcalculator;

import com.google.common.collect.ImmutableMap;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;

import java.util.Arrays;
import java.util.Map;

public class MathCalculatorDef {

    static public final String NAME = "math_calculator";

    static public final FunctionDeclaration DECLARATION = FunctionDeclaration.builder()
            .name(NAME)
            .description("Realiza operaciones matemáticas complejas como factorial, potencia, raíz cuadrada")
            .parameters(parameters())
            .response(response())
            .build();

    static private Schema parameters() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "operation", Schema.builder()
                                .type(Type.Known.STRING)
                                .description("Tipo de operación: factorial, power, sqrt, sum")
                                .enum_(Arrays.asList("factorial", "power", "sqrt", "sum"))
                                .build(),
                        "number", Schema.builder()
                                .type(Type.Known.NUMBER)
                                .description("Número principal para la operación")
                                .build(),
                        "second_number", Schema.builder()
                                .type(Type.Known.NUMBER)
                                .description("Segundo número (opcional, para operaciones como potencia)")
                                .build(),
                        "numbers", Schema.builder()
                                .type(Type.Known.ARRAY)
                                .items(Schema.builder().type(Type.Known.NUMBER).build())
                                .description("Array de números (para operaciones como suma)")
                                .build()
                ))
                .required(Arrays.asList("operation", "number"))
                .build();
    }

    static private Schema response() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(ImmutableMap.of(
                        "result", Schema.builder()
                                .type(Type.Known.NUMBER)
                                .description("Resultado de la llamada a la funcion")
                                .build()))
                .required("result")
                .build();
    }

}
