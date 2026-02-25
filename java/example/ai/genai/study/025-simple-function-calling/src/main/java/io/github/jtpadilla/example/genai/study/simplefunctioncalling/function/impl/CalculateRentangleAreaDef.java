package io.github.jtpadilla.example.genai.study.simplefunctioncalling.function.impl;

import com.google.common.collect.ImmutableMap;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;

public class CalculateRentangleAreaDef {

    static public final String NAME = "calculate_rectangle_area";

    static public final FunctionDeclaration DECLARATION = FunctionDeclaration.builder()
            .name(NAME)
            .description("Calcula el área de un rectángulo dada su longitud y ancho")
            .parameters(parameters())
            .response(response())
            .build();

    static private Schema parameters() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(ImmutableMap.of(
                        "length", Schema.builder()
                                .type(Type.Known.NUMBER)
                                .description("Longitud del rectángulo en metros")
                                .build(),
                        "width", Schema.builder()
                                .type(Type.Known.NUMBER)
                                .description("Ancho del rectángulo en metros")
                                .build()))
                .required("length")
                .required("width")
                .build();
    }

    static private Schema response() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(ImmutableMap.of(
                        "area", Schema.builder()
                                .type(Type.Known.NUMBER)
                                .description("Área del rectángulo en metros cuadrados")
                                .build()))
                .required("area")
                .build();
    }

}
