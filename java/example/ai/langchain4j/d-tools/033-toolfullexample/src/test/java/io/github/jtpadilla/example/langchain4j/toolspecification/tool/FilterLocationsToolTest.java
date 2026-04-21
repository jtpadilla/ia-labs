package io.github.jtpadilla.example.langchain4j.toolspecification.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.CityListSchema;
import io.github.jtpadilla.example.langchain4j.toolspecification.tool.filterlocations.FilterLocationsTool;
import io.github.jtpadilla.example.util.SchemaException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class FilterLocationsToolTest {

    private static final List<String> VALID_CITIES = List.of("Madrid", "Barcelona", "Sevilla");

    // --- Constantes ---

    @Test
    public void name_hasExpectedValue() {
        assertEquals("filter_locations_tool", FilterLocationsTool.NAME);
    }

    // --- SPEC ---

    @Test
    public void spec_hasCorrectName() {
        assertEquals(FilterLocationsTool.NAME, FilterLocationsTool.SPEC.name());
    }

    @Test
    public void spec_hasDescription() {
        assertNotNull(FilterLocationsTool.SPEC.description());
        assertFalse(FilterLocationsTool.SPEC.description().isBlank());
    }

    // --- execute ---

    @Test
    public void execute_keepsValidCities() {
        FilterLocationsTool tool = new FilterLocationsTool(VALID_CITIES);
        CityListSchema input = new CityListSchema(List.of("Madrid", "Barcelona"));
        CityListSchema result = tool.execute(input);
        assertEquals(2, result.getList().size());
        assertTrue(result.getList().contains("Madrid"));
        assertTrue(result.getList().contains("Barcelona"));
    }

    @Test
    public void execute_removesInvalidCities() {
        FilterLocationsTool tool = new FilterLocationsTool(VALID_CITIES);
        CityListSchema input = new CityListSchema(List.of("Madrid", "Valencia", "Bilbao"));
        CityListSchema result = tool.execute(input);
        assertEquals(1, result.getList().size());
        assertEquals("Madrid", result.getList().get(0));
    }

    @Test
    public void execute_isCaseInsensitive() {
        FilterLocationsTool tool = new FilterLocationsTool(VALID_CITIES);
        CityListSchema input = new CityListSchema(List.of("MADRID", "barcelona", "SeViLlA"));
        CityListSchema result = tool.execute(input);
        assertEquals(3, result.getList().size());
    }

    @Test
    public void execute_emptyInput_returnsEmpty() {
        FilterLocationsTool tool = new FilterLocationsTool(VALID_CITIES);
        CityListSchema input = new CityListSchema(List.of());
        CityListSchema result = tool.execute(input);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    public void execute_allInvalid_returnsEmpty() {
        FilterLocationsTool tool = new FilterLocationsTool(VALID_CITIES);
        CityListSchema input = new CityListSchema(List.of("Tokio", "Londres", "París"));
        CityListSchema result = tool.execute(input);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    public void execute_emptyValidCities_returnsEmpty() {
        FilterLocationsTool tool = new FilterLocationsTool(List.of());
        CityListSchema input = new CityListSchema(List.of("Madrid", "Barcelona"));
        CityListSchema result = tool.execute(input);
        assertTrue(result.getList().isEmpty());
    }

    // --- executor ---

    @Test
    public void executor_returnsNonNull() {
        assertNotNull(new FilterLocationsTool(VALID_CITIES).executor());
    }

    @Test
    public void executor_withValidJson_returnsFilteredCitiesJson() throws SchemaException {
        FilterLocationsTool tool = new FilterLocationsTool(VALID_CITIES);
        ToolExecutor executor = tool.executor();
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("test-id")
                .name(FilterLocationsTool.NAME)
                .arguments("{\"list\":[\"Madrid\",\"Valencia\"]}")
                .build();
        String result = executor.execute(request, "mem-1");
        CityListSchema schema = CityListSchema.fromJson(result);
        assertEquals(1, schema.getList().size());
        assertEquals("Madrid", schema.getList().get(0));
    }

    @Test
    public void executor_withInvalidJson_returnsErrorMessage() {
        FilterLocationsTool tool = new FilterLocationsTool(VALID_CITIES);
        ToolExecutor executor = tool.executor();
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("test-id")
                .name(FilterLocationsTool.NAME)
                .arguments("not valid json {{{")
                .build();
        String result = executor.execute(request, "mem-1");
        assertTrue(result.startsWith("Error:"));
    }

}
