package io.github.jtpadilla.example.langchain4j.toolspecification.schema;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import io.github.jtpadilla.example.util.SchemaEnabled;
import io.github.jtpadilla.example.util.SchemaException;
import io.github.jtpadilla.example.util.SchemaGson;

import java.util.ArrayList;
import java.util.List;

public class CityDataListSchema implements SchemaEnabled {

    static final String LIST_PROPERTY = "list";

    static public JsonObjectSchema SPEC = JsonObjectSchema.builder()
            .description("Lista de temperatures registradas por ciudades y en que hora se registro")
            .addProperty(LIST_PROPERTY, JsonArraySchema.builder()
                    .description("Lista de temperatures registradas por ciudades y en que hora se registro")
                    .items(CityDataSchema.SPEC)
                    .build())
            .required(LIST_PROPERTY)
            .build();

    private static final Gson gson = SchemaGson.DEFAULT_GSON;

    public static CityDataListSchema fromJson(String jsonString) throws SchemaException {
        try {
            return gson.fromJson(jsonString, CityDataListSchema.class);
        } catch (JsonSyntaxException e) {
            throw new SchemaException("CityDataListSchema: json has invalid format", e);
        }
    }

    @SerializedName(LIST_PROPERTY)
    private final List<CityDataSchema> list;

    private CityDataListSchema(List<CityDataSchema> list) {
        this.list = new ArrayList<>(list);
    }

    public List<CityDataSchema> getList() {
        return new ArrayList<>(list);
    }

    @Override
    public String toJson() {
        return gson.toJson(this);
    }

    @Override
    public String toString() {
        return "CityDataListSchema{" +
                "list=" + list +
                '}';
    }

}
