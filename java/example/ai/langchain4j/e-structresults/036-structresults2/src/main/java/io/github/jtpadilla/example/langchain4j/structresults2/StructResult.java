package io.github.jtpadilla.example.langchain4j.structresults2;

import com.google.gson.Gson;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.util.GoogleModels;
import io.helidon.config.Config;

import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;

public class StructResult {

    private static final Gson gson = new Gson();

    private static final String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("La clave de configuración 'gemini-api-key' es obligatoria"));

    // Sin Google Search para pasos que usan function calling personalizado
    static final ChatModel chatModel = GoogleAiGeminiChatModel.builder()
            .apiKey(API_KEY)
            .modelName(GoogleModels.geminiFlashLite())
            .logRequestsAndResponses(true)
            .build();

    record Person(String name, int age, double height, boolean married) {}

    public static void main(String[] args) {

        final JsonObjectSchema jsonObjectSchema = JsonObjectSchema.builder() // see [1] below
                .addStringProperty("name", "Full name of the person")
                .addIntegerProperty("age", "Age of the person in years")
                .addNumberProperty("height", "Height of the person in meters")
                .addBooleanProperty("married", "Whether the person is currently married")
                .required("name", "age", "height", "married") // see [2] below
                .build();

        final JsonSchema jsonSchema = JsonSchema.builder()
                .name("Person") // OpenAI requires specifying the name for the schema
                .rootElement(jsonObjectSchema)
                .build();

        final ResponseFormat responseFormat = ResponseFormat.builder()
                .type(JSON) // type can be either TEXT (default) or JSON
                .jsonSchema(jsonSchema)
                .build();

        UserMessage userMessage = UserMessage.from("""
        John is 42 years old and lives an independent life.
        He stands 1.75 meters tall and carries himself with confidence.
        Currently unmarried, he enjoys the freedom to focus on his personal goals and interests.
        """);

        ChatRequest chatRequest = ChatRequest.builder()
                .responseFormat(responseFormat)
                .messages(userMessage)
                .build();

        ChatResponse chatResponse = chatModel.chat(chatRequest);

        String output = chatResponse.aiMessage().text();
        System.out.println(output); // {"name":"John","age":42,"height":1.75,"married":false}

        Person person = gson.fromJson(output, Person.class);
        System.out.println(person); // Person[name=John, age=42, height=1.75, married=false]

    }

}
