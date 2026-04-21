package io.github.jtpadilla.example.langchain4j.toolspecification.schema;

import io.github.jtpadilla.example.util.SchemaException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CityListSchemaTest {

    private static final String THREE_CITIES_JSON =
            "{\"list\":[\"Madrid\",\"Barcelona\",\"Sevilla\"]}";

    // --- JSON → Java ---

    @Test
    public void fromJson_parsesListSize() throws SchemaException {
        CityListSchema schema = CityListSchema.fromJson(THREE_CITIES_JSON);
        assertEquals(3, schema.getList().size());
    }

    @Test
    public void fromJson_parsesListContent() throws SchemaException {
        CityListSchema schema = CityListSchema.fromJson(THREE_CITIES_JSON);
        List<String> list = schema.getList();
        assertEquals("Madrid", list.get(0));
        assertEquals("Barcelona", list.get(1));
        assertEquals("Sevilla", list.get(2));
    }

    @Test
    public void fromJson_emptyList_parsesOk() throws SchemaException {
        CityListSchema schema = CityListSchema.fromJson("{\"list\":[]}");
        assertTrue(schema.getList().isEmpty());
    }

    @Test(expected = SchemaException.class)
    public void fromJson_invalidJson_throwsSchemaException() throws SchemaException {
        CityListSchema.fromJson("not valid json {{{");
    }

    // --- Java → JSON ---

    @Test
    public void toJson_containsListField() throws SchemaException {
        CityListSchema schema = CityListSchema.fromJson(THREE_CITIES_JSON);
        String json = schema.toJson();
        assertTrue(json.contains("\"list\""));
    }

    @Test
    public void toJson_containsCityNames() throws SchemaException {
        CityListSchema schema = CityListSchema.fromJson(THREE_CITIES_JSON);
        String json = schema.toJson();
        assertTrue(json.contains("\"Madrid\""));
        assertTrue(json.contains("\"Barcelona\""));
        assertTrue(json.contains("\"Sevilla\""));
    }

    // --- Round-trip ---

    @Test
    public void roundTrip_jsonToJavaToJson_preservesList() throws SchemaException {
        CityListSchema first = CityListSchema.fromJson(THREE_CITIES_JSON);
        CityListSchema second = CityListSchema.fromJson(first.toJson());
        assertEquals(first.getList(), second.getList());
    }

}
