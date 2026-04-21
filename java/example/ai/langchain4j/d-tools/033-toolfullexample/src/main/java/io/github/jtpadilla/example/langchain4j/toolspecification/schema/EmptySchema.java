package io.github.jtpadilla.example.langchain4j.toolspecification.schema;

import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import io.github.jtpadilla.example.util.SchemaEnabled;
import io.github.jtpadilla.example.util.SchemaException;

public class EmptySchema implements SchemaEnabled {

    static public final JsonObjectSchema SPEC = JsonObjectSchema.builder().build();

    public static EmptySchema fromJson(String argumentsJson) throws SchemaException {
        return new EmptySchema();
    }

    static public EmptySchema DEFAULT_INSTANCE = new EmptySchema();

    @Override
    public String toJson() {
        return "{}";
    }

}
