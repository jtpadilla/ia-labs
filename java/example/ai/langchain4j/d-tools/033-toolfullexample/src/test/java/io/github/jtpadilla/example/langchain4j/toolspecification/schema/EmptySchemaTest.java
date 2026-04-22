package io.github.jtpadilla.example.langchain4j.toolspecification.schema;

import io.github.jtpadilla.example.langchain4j.toolfullexample1.schema.EmptySchema;
import io.github.jtpadilla.example.util.SchemaException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EmptySchemaTest {

    // --- Java → JSON ---

    @Test
    public void toJson_returnsEmptyObject() {
        assertEquals("{}", new EmptySchema().toJson());
    }

    @Test
    public void toJson_defaultInstance_returnsEmptyObject() {
        assertEquals("{}", EmptySchema.DEFAULT_INSTANCE.toJson());
    }

    // --- JSON → Java ---

    @Test
    public void fromJson_emptyObject_returnsInstance() throws SchemaException {
        assertNotNull(EmptySchema.fromJson("{}"));
    }

    @Test
    public void fromJson_anyJson_returnsInstance() throws SchemaException {
        assertNotNull(EmptySchema.fromJson("{\"ignored\":\"field\"}"));
    }

    // --- Round-trip ---

    @Test
    public void roundTrip_toJsonFromJson_returnsEmptyObject() throws SchemaException {
        EmptySchema recovered = EmptySchema.fromJson(new EmptySchema().toJson());
        assertEquals("{}", recovered.toJson());
    }

    // --- DEFAULT_INSTANCE ---

    @Test
    public void defaultInstance_isNotNull() {
        assertNotNull(EmptySchema.DEFAULT_INSTANCE);
    }


}
