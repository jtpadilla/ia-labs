package io.github.jtpadilla.example.langchain4j.toolfullexample1.tool;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import io.github.jtpadilla.example.langchain4j.toolfullexample1.schema.EmptySchema;
import io.github.jtpadilla.example.langchain4j.toolfullexample1.schema.LocalDateTimeSchema;

import java.time.LocalDateTime;

public class GetCurrentTimeTool {

    static public final String NAME = "get_current_time_tool";

    static public final ToolSpecification SPEC = ToolSpecification.builder()
            .name(NAME)
            .description("Obtiene la fecha y hora actual sin información de zona horaria. " + LocalDateTimeSchema.RETURN_DESCRIPTION)
            .parameters(EmptySchema.SPEC)
            .build();

    public static ToolExecutor executor() {
        return (request, memoryId) -> execute(request.arguments());
    }

    public static String execute(String argumentsJson) {
        return execute(EmptySchema.DEFAULT_INSTANCE).toJson();
    }

    private static LocalDateTimeSchema execute(EmptySchema emptySchema) {
        return new LocalDateTimeSchema(LocalDateTime.now());
    }

    @Tool("Obtiene la fecha y hora actual sin información de zona horaria. " + LocalDateTimeSchema.RETURN_DESCRIPTION)
    public String getCurrentTime() {
        return new LocalDateTimeSchema(LocalDateTime.now()).toJson();
    }

}
