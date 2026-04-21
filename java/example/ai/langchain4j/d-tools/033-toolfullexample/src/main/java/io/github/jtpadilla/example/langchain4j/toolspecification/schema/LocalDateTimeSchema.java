package io.github.jtpadilla.example.langchain4j.toolspecification.schema;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import io.github.jtpadilla.example.util.SchemaEnabled;
import io.github.jtpadilla.example.util.SchemaException;
import io.github.jtpadilla.example.util.SchemaGson;

import java.time.LocalDateTime;
import java.util.Objects;

public class LocalDateTimeSchema implements SchemaEnabled {

    private static final String LOCALDATETIME_PROPERTY = "localdatetime";

    static public final JsonObjectSchema SPEC = JsonObjectSchema.builder()
            .description("Fecha y hora en formato ISO-8601")
            .addStringProperty(LOCALDATETIME_PROPERTY, "Fecha y hora en formato ISO-8601")
            .required(LOCALDATETIME_PROPERTY)
            .build();

    private static final Gson gson = SchemaGson.DEFAULT_GSON;

    public static LocalDateTimeSchema fromJson(String jsonString) throws SchemaException {
        try {
            return gson.fromJson(jsonString, LocalDateTimeSchema.class);
        } catch (JsonSyntaxException e) {
            throw new SchemaException("LocalDateTimeSchema: json has invalid format", e);
        }
    }

    @SerializedName(LOCALDATETIME_PROPERTY)
    private final LocalDateTime localDateTime;

    public LocalDateTimeSchema(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public LocalDateTime localDateTime() {
        return localDateTime;
    }

    @Override
    public String toJson() {
        return gson.toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LocalDateTimeSchema that = (LocalDateTimeSchema) o;
        return Objects.equals(localDateTime, that.localDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localDateTime);
    }

    @Override
    public String toString() {
        return "LocalDateTimeSchema{localDateTime=" + localDateTime + '}';
    }

}
