package io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.systeminfo;

import com.google.common.collect.ImmutableMap;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;

import java.util.Arrays;
import java.util.Map;

public class SystemInfoDef {

    static public final String NAME = "get_system_info";

    static public final FunctionDeclaration DECLARATION = FunctionDeclaration.builder()
            .name(NAME)
            .description("Obtiene información del sistema como fecha/hora actual, zona horaria")
            .parameters(parameters())
            .response(response())
            .build();

    static private Schema parameters() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "info_type", Schema.builder()
                                .type(Type.Known.STRING)
                                .description("Tipo de información solicitada")
                                .enum_(Arrays.asList("current_time", "timezone", "system_stats"))
                                .build(),
                        "format", Schema.builder()
                                .type(Type.Known.STRING)
                                .description("Formato de la respuesta")
                                .enum_(Arrays.asList("iso", "readable", "timestamp"))
                                .build()
                ))
                .required("info_type")
                .build();
    }

    static private Schema response() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(ImmutableMap.of(
                        "result", Schema.builder()
                                .type(Type.Known.STRING)
                                .description("Resultado de la llamada a la funcion")
                                .build()))
                .required("result")
                .build();
    }

}
