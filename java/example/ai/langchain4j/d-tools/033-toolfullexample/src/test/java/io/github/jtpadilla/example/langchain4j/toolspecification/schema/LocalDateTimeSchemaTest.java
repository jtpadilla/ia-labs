package io.github.jtpadilla.example.langchain4j.toolspecification.schema;

import io.github.jtpadilla.example.langchain4j.toolfullexample1.schema.LocalDateTimeSchema;
import io.github.jtpadilla.example.util.SchemaException;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LocalDateTimeSchemaTest {

    private static final LocalDateTime DATETIME = LocalDateTime.of(2024, 3, 15, 10, 30, 0);
    private static final String DATETIME_JSON = "{\"localdatetime\":\"2024-03-15T10:30:00\"}";

    // --- Java → JSON ---

    @Test
    public void toJson_containsLocalDateTimeField() {
        String json = new LocalDateTimeSchema(DATETIME).toJson();
        assertTrue(json.contains("\"localdatetime\""));
    }

    @Test
    public void toJson_containsIso8601Value() {
        String json = new LocalDateTimeSchema(DATETIME).toJson();
        assertTrue(json.contains("2024-03-15T10:30:00"));
    }

    // --- JSON → Java ---

    @Test
    public void fromJson_parsesLocalDateTime() throws SchemaException {
        LocalDateTimeSchema schema = LocalDateTimeSchema.fromJson(DATETIME_JSON);
        assertEquals(DATETIME, schema.localDateTime());
    }

    @Test
    public void fromJson_parsesYear() throws SchemaException {
        LocalDateTimeSchema schema = LocalDateTimeSchema.fromJson(DATETIME_JSON);
        assertEquals(2024, schema.localDateTime().getYear());
    }

    @Test
    public void fromJson_parsesTime() throws SchemaException {
        LocalDateTimeSchema schema = LocalDateTimeSchema.fromJson(DATETIME_JSON);
        assertEquals(10, schema.localDateTime().getHour());
        assertEquals(30, schema.localDateTime().getMinute());
    }

    @Test(expected = SchemaException.class)
    public void fromJson_invalidJson_throwsSchemaException() throws SchemaException {
        LocalDateTimeSchema.fromJson("not valid json {{{");
    }

    // --- Round-trip ---

    @Test
    public void roundTrip_javaToJsonToJava_preservesDateTime() throws SchemaException {
        LocalDateTimeSchema original = new LocalDateTimeSchema(DATETIME);
        LocalDateTimeSchema recovered = LocalDateTimeSchema.fromJson(original.toJson());
        assertEquals(original, recovered);
    }

    @Test
    public void roundTrip_preservesNanoseconds() throws SchemaException {
        LocalDateTime withNanos = LocalDateTime.of(2024, 1, 1, 0, 0, 0, 123456789);
        LocalDateTimeSchema original = new LocalDateTimeSchema(withNanos);
        LocalDateTimeSchema recovered = LocalDateTimeSchema.fromJson(original.toJson());
        assertEquals(original, recovered);
    }

}
