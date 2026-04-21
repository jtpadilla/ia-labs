package io.github.jtpadilla.example.langchain4j.toolspecification.tool.filterlocations;

import dev.langchain4j.agent.tool.ToolSpecification;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.CityListSchema;
import io.github.jtpadilla.example.util.SchemaException;

import java.util.List;

public class FilterLocationsTool {

    static public final String NAME = "filter_locations_tool";

    static public final ToolSpecification SPEC = ToolSpecification.builder()
            .name(NAME)
            .description("Proporciona la lista de ciudades en la que estamos interesados")
            .parameters(CityListSchema.SPEC)
            .build();

    private final List<String> validCities;

    public FilterLocationsTool(List<String> validCities) {
        this.validCities = validCities;
    }

    public String execute(String argumentsJson) throws SchemaException {
        return execute(CityListSchema.fromJson(argumentsJson)).toJson();
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

}
