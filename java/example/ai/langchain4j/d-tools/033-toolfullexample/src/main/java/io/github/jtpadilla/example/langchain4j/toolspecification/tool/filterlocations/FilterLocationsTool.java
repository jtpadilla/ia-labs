package io.github.jtpadilla.example.langchain4j.toolspecification.tool.filterlocations;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.CityListSchema;
import io.github.jtpadilla.example.util.SchemaException;

import java.util.List;

public class FilterLocationsTool {

    static public final String NAME = "filter_locations_tool";

    static public final ToolSpecification SPEC = ToolSpecification.builder()
            .name(NAME)
            .description("Proporciona la lista de ciudades en la que estamos interesados. " + CityListSchema.RETURN_DESCRIPTION)
            .parameters(CityListSchema.SPEC)
            .build();

    private final List<String> validCities;

    public FilterLocationsTool(List<String> validCities) {
        this.validCities = validCities;
    }

    public ToolExecutor executor() {
        return (request, memoryId) -> {
            try {
                return execute(CityListSchema.fromJson(request.arguments())).toJson();
            } catch (SchemaException e) {
                return "Error: " + e.getMessage();
            }
        };
    }

    public CityListSchema execute(CityListSchema parameter) {
        List<String> filtered = parameter.getList().stream()
                .filter(this::isValidCity)
                .toList();
        return new CityListSchema(filtered);
    }

    private boolean isValidCity(String required) {
        return validCities.stream()
                .map(String::toUpperCase)
                .anyMatch(query -> query.equals(required.toUpperCase()));
    }

    @Tool("Proporciona la lista de ciudades en la que estamos interesados dado una lista de ciudades de entrada. " + CityListSchema.RETURN_DESCRIPTION)
    public String filterLocations(@P("JSON con la lista de ciudades a filtrar, en el formato: {\"list\":[\"ciudad1\",\"ciudad2\",...]}") String citiesJson) {
        try {
            return execute(CityListSchema.fromJson(citiesJson)).toJson();
        } catch (SchemaException e) {
            return "Error al procesar la lista de ciudades: " + e.getMessage();
        }
    }

}
