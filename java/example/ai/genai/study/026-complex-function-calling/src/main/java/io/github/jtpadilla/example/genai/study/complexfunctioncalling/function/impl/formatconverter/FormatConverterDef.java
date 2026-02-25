package io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.formatconverter;

import com.google.common.collect.ImmutableMap;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;

import java.util.Arrays;
import java.util.Map;

public class FormatConverterDef {

    static public final String NAME = "format_converter";

    static public final FunctionDeclaration DECLARATION = FunctionDeclaration.builder()
            .name(NAME)
            .description("Convierte datos a diferentes formatos como JSON, XML, CSV")
            .parameters(parameters())
            .response(response())
            .build();

    static private Schema parameters() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "data", Schema.builder()
                                .type(Type.Known.STRING)
                                .description("Datos a convertir")
                                .build(),
                        "target_format", Schema.builder()
                                .type(Type.Known.STRING)
                                .description("Formato destino")
                                .enum_(Arrays.asList("json", "xml", "csv", "yaml"))
                                .build(),
                        "include_metadata", Schema.builder()
                                .type(Type.Known.BOOLEAN)
                                .description("Incluir metadatos en la conversión")
                                .build()
                ))
                .required(Arrays.asList("data", "target_format"))
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
