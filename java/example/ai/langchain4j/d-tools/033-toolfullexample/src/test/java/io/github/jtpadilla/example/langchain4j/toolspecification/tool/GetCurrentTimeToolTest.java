package io.github.jtpadilla.example.langchain4j.toolspecification.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.LocalDateTimeSchema;
import io.github.jtpadilla.example.langchain4j.toolspecification.tool.currenttime.GetCurrentTimeTool;
import io.github.jtpadilla.example.util.SchemaException;
import org.junit.Test;

import static org.junit.Assert.*;

public class GetCurrentTimeToolTest {

    // --- Constantes ---

    @Test
    public void name_hasExpectedValue() {
        assertEquals("get_current_time_tool", GetCurrentTimeTool.NAME);
    }

    // --- SPEC ---

    @Test
    public void spec_hasCorrectName() {
        assertEquals(GetCurrentTimeTool.NAME, GetCurrentTimeTool.SPEC.name());
    }

    @Test
    public void spec_hasDescription() {
        assertNotNull(GetCurrentTimeTool.SPEC.description());
        assertFalse(GetCurrentTimeTool.SPEC.description().isBlank());
    }

    // --- execute ---

    @Test
    public void execute_returnsJsonWithLocalDateTimeField() {
        String json = GetCurrentTimeTool.execute("{}");
        assertTrue(json.contains("\"localdatetime\""));
    }

    @Test
    public void execute_returnsParsableLocalDateTimeSchema() throws SchemaException {
        String json = GetCurrentTimeTool.execute("{}");
        LocalDateTimeSchema schema = LocalDateTimeSchema.fromJson(json);
        assertNotNull(schema.localDateTime());
    }

    @Test
    public void execute_returnsCurrentYear() throws SchemaException {
        String json = GetCurrentTimeTool.execute("{}");
        LocalDateTimeSchema schema = LocalDateTimeSchema.fromJson(json);
        int year = schema.localDateTime().getYear();
        assertTrue("El año debe ser razonable", year >= 2024);
    }

    // --- executor ---

    @Test
    public void executor_returnsNonNull() {
        assertNotNull(GetCurrentTimeTool.executor());
    }

    @Test
    public void executor_invokesExecuteAndReturnsJson() throws SchemaException {
        ToolExecutor executor = GetCurrentTimeTool.executor();
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("test-id")
                .name(GetCurrentTimeTool.NAME)
                .arguments("{}")
                .build();
        String result = executor.execute(request, "mem-1");
        assertNotNull(result);
        LocalDateTimeSchema schema = LocalDateTimeSchema.fromJson(result);
        assertNotNull(schema.localDateTime());
    }

}
