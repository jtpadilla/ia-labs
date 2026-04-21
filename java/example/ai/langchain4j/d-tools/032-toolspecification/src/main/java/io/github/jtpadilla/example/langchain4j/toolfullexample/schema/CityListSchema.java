package io.github.jtpadilla.example.langchain4j.toolfullexample.schema;

import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import io.github.jtpadilla.example.util.SchemaGson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CityListSchema implements SchemaEnabled {

    static final private String LIST_PROPERTY = "list";

    static final public Schema SCHEMA = Schema.builder()
            .type(Type.Known.OBJECT)
            .description("Lista de ciudades")
            .properties(Map.of(
                    LIST_PROPERTY,
                    Schema.builder()
                            .type(Type.Known.ARRAY)
                            .items(Schema.builder().type(Type.Known.STRING).build())
                            .description("Lista de ciudades")
                            .build()
                    )
            )
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

    @SerializedName(LIST_PROPERTY)
    private final List<String> list;

    private CityListSchema(List<String> list) {
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
