package io.github.jtpadilla.example.langchain4j.toolfullexample1.schema;

import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import io.github.jtpadilla.example.util.SchemaGson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CityDataListSchema implements SchemaEnabled {

    static final String LIST_PROPERTY = "list";

    static public Schema SCHEMA = Schema.builder()
            .type(Type.Known.OBJECT)
            .description("Lista de temperatures registradas por ciudades y en que hora se registro")
            .properties(Map.of(
                    LIST_PROPERTY,
                    Schema.builder()
                            .type(Type.Known.ARRAY)
                            .items(CityDataSchema.SCHEMA)
                            .description("Lista de temperatures registradas por ciudades y en que hora se registro")
                            .build()
                    )
            )
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
