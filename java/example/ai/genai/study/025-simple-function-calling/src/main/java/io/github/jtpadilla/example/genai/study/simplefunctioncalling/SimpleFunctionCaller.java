package io.github.jtpadilla.example.genai.study.simplefunctioncalling;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.github.jtpadilla.example.genai.common.GenAIServiceSelector;
import io.github.jtpadilla.example.genai.study.simplefunctioncalling.function.FunctionDirectory;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import io.github.jtpadilla.gcloud.genai.function.FunctionGateway;
import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

public final class SimpleFunctionCaller {

    static final String SYSTEM_INSTRUCTION = """
Eres un asistente inteligente con acceso a herramientas matemáticas,
de información y manipulación de datos. Utiliza las herramientas cuando sea necesario
para responder de manera precisa.
""";

    static final String USER_QUERY_CALLS = """
Calcula el área de un rectángulo con longitud 5 metros y ancho 3 metros
""";

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            System.out.println("🚀 Consulta del usuario: " + USER_QUERY_CALLS);

            System.out.println("=".repeat(80));

            final String response = FunctionGateway.builder(
                            genAIService.getLlmModel(),
                            client,
                            Content.fromParts(Part.fromText(SYSTEM_INSTRUCTION)),
                            Content.fromParts(Part.fromText(USER_QUERY_CALLS)),
                            FunctionDirectory::execute)
                    .addTool(FunctionDirectory.createTool())
                    .generate();
            System.out.println(response);

        } catch (GenAiIOException e) {
            System.out.println("❌ Error al trabajar con GenAI: " + e.getMessage());
        } catch (FunctionGatewayException e) {
            throw new RuntimeException(e);
        }
    }

}
