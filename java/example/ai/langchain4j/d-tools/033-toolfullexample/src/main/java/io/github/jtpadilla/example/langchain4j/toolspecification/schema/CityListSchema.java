package io.github.jtpadilla.example.langchain4j.toolspecification.schema;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import io.github.jtpadilla.example.util.SchemaEnabled;
import io.github.jtpadilla.example.util.SchemaException;
import io.github.jtpadilla.example.util.SchemaGson;

import java.util.ArrayList;
import java.util.List;

public class CityListSchema implements SchemaEnabled {

    static final private String LIST_PROPERTY = "list";

    static final public JsonObjectSchema SPEC = JsonObjectSchema.builder()
            .description("Lista de ciudades")
            .addProperty(LIST_PROPERTY, JsonArraySchema.builder()
                    .description("Lista de ciudades")
                    .items(JsonStringSchema.builder().build())
                    .build())
            .required(LIST_PROPERTY)
            .build();

    private static final Gson gson = SchemaGson.DEFAULT_GSON;

    public static CityListSchema fromJson(String jsonString) throws SchemaException {
        try {
            return gson.fromJson(jsonString, CityListSchema.class);
        } catch (JsonSyntaxException e) {
            throw new SchemaException("CityListSchema: json has invalid format", e);
        }
    }

    public static CityListSchema fromJsonFlex(String jsonString) {
        JsonObject json = gson.fromJson(jsonString, JsonObject.class);
        List<String> poblaciones = new ArrayList<>();
        if (json.has(LIST_PROPERTY)) {
            JsonArray array = json.getAsJsonArray(LIST_PROPERTY);
            array.forEach(e -> poblaciones.add(e.getAsString()));
        }
        return new CityListSchema(poblaciones);
    }

    @SerializedName(LIST_PROPERTY)
    private final List<String> list;

    public CityListSchema(List<String> list) {
        this.list = new ArrayList<>(list);
    }

    public List<String> getList() {
        return new ArrayList<>(list);
    }

    @Override
    public String toJson() {
        return gson.toJson(this);
    }

    @Override
    public String toString() {
        return "CityListSchema{" +
                "list=" + list +
                '}';
    }

}
