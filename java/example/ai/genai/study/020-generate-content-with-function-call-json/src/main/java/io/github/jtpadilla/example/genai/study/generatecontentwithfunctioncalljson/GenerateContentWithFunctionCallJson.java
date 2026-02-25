package io.github.jtpadilla.example.genai.study.generatecontentwithfunctioncalljson;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Tool;
import io.github.jtpadilla.example.genai.common.GenAIServiceSelector;
import io.github.jtpadilla.gcloud.genai.IGenAIService;

public class GenerateContentWithFunctionCallJson {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            // Define the schema for the function declaration, in Json format.
            final ImmutableMap<String, Object> schema = ImmutableMap.of(
                        "type", "object",
                        "properties", ImmutableMap.of("location", ImmutableMap.of("type", "string")),
                        "required", ImmutableList.of("location")
            );

            // Define the tool with the function declaration.
            final Tool toolWithFunctionDeclarations = Tool.builder()
                    .functionDeclarations(
                            FunctionDeclaration.builder()
                                    .name("get_weather")
                                    .description("Returns the weather in a given location.")
                                    .parametersJsonSchema(schema))
                            .build();

            // Add the tool to the GenerateContentConfig.
            final GenerateContentConfig config =
                    GenerateContentConfig.builder().tools(toolWithFunctionDeclarations).build();

            final GenerateContentResponse response =
                    client.models.generateContent(genAIService.getLlmModel(), "What is the weather in Vancouver?", config);

            System.out.println("The response is: " + response.functionCalls());

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }

    }

}
