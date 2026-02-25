package com.speedycontrol.labs.example.genai.study.complexfunctioncalling.function.impl.informationsearcher;

import com.google.common.collect.ImmutableMap;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;

import java.util.Arrays;
import java.util.Map;

public class InformationSearcherDef {

    static public final String NAME = "search_information";

    static public final FunctionDeclaration DECLARATION = FunctionDeclaration.builder()
            .name(NAME)
            .description("Busca información específica en una base de conocimientos simulada")
            .parameters(parameters())
            .response(response())
            .build();

    static private Schema parameters() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "query", Schema.builder()
                                .type(Type.Known.STRING)
                                .description("Consulta de búsqueda")
                                .build(),
                        "category", Schema.builder()
                                .type(Type.Known.STRING)
                                .description("Categoría de búsqueda")
                                .enum_(Arrays.asList("algorithms", "mathematics", "programming", "general"))
                                .build(),
                        "max_results", Schema.builder()
                                .type(Type.Known.INTEGER)
                                .description("Número máximo de resultados")
                                .build()
                ))
                .required(Arrays.asList("query"))
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

