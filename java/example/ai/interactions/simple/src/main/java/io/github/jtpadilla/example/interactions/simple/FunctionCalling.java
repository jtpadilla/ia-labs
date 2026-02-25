package io.github.jtpadilla.example.interactions.simple;

import io.github.jtpadilla.example.interactions.util.Util;
import io.github.glaforge.gemini.interactions.GeminiInteractionsClient;
import io.github.glaforge.gemini.interactions.model.Content;
import io.github.glaforge.gemini.interactions.model.Content.*;
import io.github.glaforge.gemini.interactions.model.Interaction;
import io.github.glaforge.gemini.interactions.model.InteractionParams.ModelInteractionParams;
import io.github.glaforge.gemini.interactions.model.Tool.Function;

import java.util.List;
import java.util.Map;

public class FunctionCalling {

    static void main() {

        final GeminiInteractionsClient client = GeminiInteractionsClient.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .build();

        // 1. Define the tool
        Function weatherTool = new Function(
                "get_weather",
                "Get the current weather",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "location", Map.of("type", "string")
                        ),
                        "required", List.of("location")
                )
        );

        // 2. Initial Request with Tools
        ModelInteractionParams request = ModelInteractionParams.builder()
                .model("gemini-2.5-flash")
                .input("What is the weather in London?")
                .tools(weatherTool)
                .build();
        Interaction interaction = client.create(request);

        // 3. Handle Function Call
        Content lastOutput = interaction.outputs().getLast();
        if (lastOutput instanceof FunctionCallContent call) {
            if ("get_weather".equals(call.name())) {
                String location = (String) call.arguments().get("location");
                // Execute local logic...
                String weather = "Rainy, 15°C"; // Simulated result

                // 4. Send Function Result
                ModelInteractionParams continuation = ModelInteractionParams.builder()
                        .model("gemini-2.5-flash")
                        .previousInteractionId(interaction.id())
                        .input(new FunctionResultContent(
                                "function_result",
                                call.id(),
                                call.name(),
                                false,
                                Map.of("weather", weather)
                        ))
                        .build();

                Interaction finalResponse = client.create(continuation);
                Util.dumpText(Util.getText(finalResponse));
            }
        }

    }

}
