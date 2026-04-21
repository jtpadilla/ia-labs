package io.github.jtpadilla.example.langchain4j.toolspecification.schema;

import io.github.jtpadilla.example.util.SchemaException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CityDataListSchemaTest {

    private static final String TWO_CITIES_JSON =
            "{\"list\":[" +
            "{\"city\":\"Madrid\",\"localdatetime\":\"2024-06-15T12:00:00\",\"temperature\":28.0}," +
            "{\"city\":\"Valencia\",\"localdatetime\":\"2024-06-15T12:00:00\",\"temperature\":30.0}" +
            "]}";

    // --- JSON → Java ---

    @Test
    public void fromJson_parsesListSize() throws SchemaException {
        CityDataListSchema schema = CityDataListSchema.fromJson(TWO_CITIES_JSON);
        assertEquals(2, schema.getList().size());
    }

    @Test
    public void fromJson_parsesFirstElement() throws SchemaException {
        CityDataListSchema schema = CityDataListSchema.fromJson(TWO_CITIES_JSON);
        CityDataSchema first = schema.getList().getFirst();
        assertEquals("Madrid", first.city());
        assertEquals(28.0, first.temperature(), 0.001);
    }

    @Test
    public void fromJson_parsesSecondElement() throws SchemaException {
        CityDataListSchema schema = CityDataListSchema.fromJson(TWO_CITIES_JSON);
        CityDataSchema second = schema.getList().get(1);
        assertEquals("Valencia", second.city());
        assertEquals(30.0, second.temperature(), 0.001);
    }

    @Test
    public void fromJson_emptyList_parsesOk() throws SchemaException {
        CityDataListSchema schema = CityDataListSchema.fromJson("{\"list\":[]}");
        assertTrue(schema.getList().isEmpty());
    }

    @Test(expected = SchemaException.class)
    public void fromJson_invalidJson_throwsSchemaException() throws SchemaException {
        CityDataListSchema.fromJson("not valid json {{{");
    }

    // --- Java → JSON ---

    @Test
    public void toJson_containsListField() throws SchemaException {
        CityDataListSchema schema = CityDataListSchema.fromJson(TWO_CITIES_JSON);
        String json = schema.toJson();
        assertTrue(json.contains("\"list\""));
    }

    @Test
    public void toJson_containsCityNames() throws SchemaException {
        CityDataListSchema schema = CityDataListSchema.fromJson(TWO_CITIES_JSON);
        String json = schema.toJson();
        assertTrue(json.contains("\"Madrid\""));
        assertTrue(json.contains("\"Valencia\""));
    }

    // --- Round-trip ---

    @Test
    public void roundTrip_jsonToJavaToJson_preservesListSize() throws SchemaException {
        CityDataListSchema first = CityDataListSchema.fromJson(TWO_CITIES_JSON);
        CityDataListSchema second = CityDataListSchema.fromJson(first.toJson());
        assertEquals(first.getList().size(), second.getList().size());
    }

    @Test
    public void roundTrip_jsonToJavaToJson_preservesCityNames() throws SchemaException {
        CityDataListSchema first = CityDataListSchema.fromJson(TWO_CITIES_JSON);
        List<CityDataSchema> firstList = first.getList();
        CityDataListSchema second = CityDataListSchema.fromJson(first.toJson());
        List<CityDataSchema> secondList = second.getList();
        for (int i = 0; i < firstList.size(); i++) {
            assertEquals(firstList.get(i).city(), secondList.get(i).city());
            assertEquals(firstList.get(i).temperature(), secondList.get(i).temperature(), 0.001);
        }
    }

}
