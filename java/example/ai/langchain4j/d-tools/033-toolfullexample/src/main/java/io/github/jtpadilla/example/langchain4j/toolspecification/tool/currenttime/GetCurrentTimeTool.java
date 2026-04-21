package io.github.jtpadilla.example.langchain4j.toolspecification.tool.currenttime;

import dev.langchain4j.agent.tool.ToolSpecification;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.EmptySchema;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.Response;

import java.time.LocalDateTime;

public class GetCurrentTimeTool {

    static public final String NAME = "get_current_time";

    static public final ToolSpecification SPEC = ToolSpecification.builder()
            .name(NAME)
            .description("Obtiene la fecha y hora actual sin información de zona horaria")
            .parameters(EmptySchema.SPEC)
            .build();

    public static String execute(String argumentsJson) {
        return new Response(LocalDateTime.now()).toJson();
    }

}
