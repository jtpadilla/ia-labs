package io.github.jtpadilla.example.langchain4j.toolspecification.schema;

import io.github.jtpadilla.example.util.SchemaException;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CityDataSchemaTest {

    private static final String CITY = "Madrid";
    private static final LocalDateTime DATETIME = LocalDateTime.of(2024, 6, 15, 12, 30, 0);
    private static final double TEMPERATURE = 28.5;

    // --- Java → JSON ---

    @Test
    public void toJson_containsCityField() {
        CityDataSchema schema = new CityDataSchema(CITY, DATETIME, TEMPERATURE);
        String json = schema.toJson();
        assertTrue(json.contains("\"city\""));
        assertTrue(json.contains("\"Madrid\""));
    }

    @Test
    public void toJson_containsLocalDateTimeField() {
        CityDataSchema schema = new CityDataSchema(CITY, DATETIME, TEMPERATURE);
        String json = schema.toJson();
        assertTrue(json.contains("\"localdatetime\""));
        assertTrue(json.contains("2024-06-15T12:30:00"));
    }

    @Test
    public void toJson_containsTemperatureField() {
        CityDataSchema schema = new CityDataSchema(CITY, DATETIME, TEMPERATURE);
        String json = schema.toJson();
        assertTrue(json.contains("\"temperature\""));
        assertTrue(json.contains("28.5"));
    }

    // --- JSON → Java ---

    @Test
    public void fromJson_parsesCity() throws SchemaException {
        String json = "{\"city\":\"Barcelona\",\"localdatetime\":\"2024-01-10T08:00:00\",\"temperature\":15.0}";
        CityDataSchema schema = CityDataSchema.fromJson(json);
        assertEquals("Barcelona", schema.city());
    }

    @Test
    public void fromJson_parsesLocalDateTime() throws SchemaException {
        String json = "{\"city\":\"Sevilla\",\"localdatetime\":\"2024-07-20T14:00:00\",\"temperature\":38.0}";
        CityDataSchema schema = CityDataSchema.fromJson(json);
        assertEquals(LocalDateTime.of(2024, 7, 20, 14, 0, 0), schema.localDateTime());
    }

    @Test
    public void fromJson_parsesTemperature() throws SchemaException {
        String json = "{\"city\":\"Bilbao\",\"localdatetime\":\"2024-03-05T09:00:00\",\"temperature\":12.3}";
        CityDataSchema schema = CityDataSchema.fromJson(json);
        assertEquals(12.3, schema.temperature(), 0.001);
    }

    @Test(expected = SchemaException.class)
    public void fromJson_invalidJson_throwsSchemaException() throws SchemaException {
        CityDataSchema.fromJson("not valid json {{{");
    }

    // --- Round-trip ---

    @Test
    public void roundTrip_javaToJsonToJava_preservesData() throws SchemaException {
        CityDataSchema original = new CityDataSchema(CITY, DATETIME, TEMPERATURE);
        CityDataSchema recovered = CityDataSchema.fromJson(original.toJson());
        assertEquals(original, recovered);
    }

}
