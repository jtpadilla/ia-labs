package io.github.jtpadilla.example.langchain4j.toolfullexample1.schema;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import io.github.jtpadilla.example.util.SchemaEnabled;
import io.github.jtpadilla.example.util.SchemaException;
import io.github.jtpadilla.example.util.SchemaGson;

import java.time.LocalDateTime;
import java.util.Objects;

public class CityDataSchema implements SchemaEnabled {

    static final private String CITY_PROPERTY = "city";
    static final private String LOCALDATETIME_PROPERTY = "localdatetime";
    static final private String TEMPERATURE_PROPERTY = "temperature";

    static public JsonObjectSchema SPEC = JsonObjectSchema.builder()
            .description("Registro de temperatura de una ciudad en un momento determinado")
            .addStringProperty(CITY_PROPERTY, "Nombre de la ciudad")
            .addStringProperty(LOCALDATETIME_PROPERTY, "Fecha y hora de la medición de la temperatura en formato ISO-8601")
            .addNumberProperty(TEMPERATURE_PROPERTY, "Temperatura en grados centígrados")
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
