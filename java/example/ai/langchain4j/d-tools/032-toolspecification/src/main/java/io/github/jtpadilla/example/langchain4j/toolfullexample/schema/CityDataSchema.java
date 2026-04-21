package io.github.jtpadilla.example.langchain4j.toolfullexample.schema;

import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import io.github.jtpadilla.example.util.SchemaGson;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

public class CityDataSchema implements SchemaEnabled {

    static final private String CITY_PROPERTY = "city";
    static final private String LOCALDATETIME_PROPERTY = "localdatetime";
    static final private String TEMPERATURE_PROPERTY = "temperature";

    static public Schema SCHEMA = Schema.builder()
            .type(Type.Known.OBJECT)
            .description("Registro de temperatura de una ciudad en un momento determinado")
            .properties(Map.of(
                    CITY_PROPERTY,
                    Schema.builder()
                            .type(Type.Known.STRING)
                            .description("Nombre de la ciudad")
                            .build(),
                    LOCALDATETIME_PROPERTY,
                    Schema.builder()
                            .type(Type.Known.STRING)
                            .description("Fecha y hora de la medición de la temperatura en formato ISO-8601")
                            .build(),
                    TEMPERATURE_PROPERTY,
                    Schema.builder()
                            .type(Type.Known.NUMBER)
                            .description("Temperatura en grados centígrados")
                            .build())
            )
            .required(CITY_PROPERTY, LOCALDATETIME_PROPERTY, TEMPERATURE_PROPERTY)
            .build();

    private static final Gson gson = SchemaGson.DEFAULT_GSON;

    public static CityDataSchema fromJson(String jsonString) throws SchemaException {
        try {
            return gson.fromJson(jsonString, CityDataSchema.class);
        } catch (JsonSyntaxException e) {
            throw new SchemaException("CityDataSchema: json has invalid format", e);
        }
    }

    @SerializedName(CITY_PROPERTY)
    private final String city;

    @SerializedName(LOCALDATETIME_PROPERTY)
    public final LocalDateTime localDateTime;

    @SerializedName(TEMPERATURE_PROPERTY)
    private final Double temperature;

    public CityDataSchema(String city, LocalDateTime localDateTime, double temperature) {
        this.city = city;
        this.localDateTime = localDateTime;
        this.temperature = temperature;
    }

    public String city() {
        return city;
    }

    public LocalDateTime localDateTime() {
        return localDateTime;
    }

    public double temperature() {
        return temperature;
    }

    @Override
    public String toJson() {
        return gson.toJson(this);
    }

    @Override
    public String toString() {
        return "CityDataSchema{" +
                "city='" + city + '\'' +
                ", localDateTime=" + localDateTime +
                ", temperature=" + temperature +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CityDataSchema that = (CityDataSchema) o;
        return Objects.equals(city, that.city) && Objects.equals(localDateTime, that.localDateTime) && Objects.equals(temperature, that.temperature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, localDateTime, temperature);
    }

}
