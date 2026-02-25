package com.speedycontrol.labs.example.genai.study.complexfunctioncalling;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import io.github.jtpadilla.gcloud.genai.function.FunctionGateway;
import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;
import com.speedycontrol.labs.example.genai.common.GenAIServiceSelector;
import com.speedycontrol.labs.example.genai.study.complexfunctioncalling.function.FunctionDirectory;

public final class ComplexFunctionCaller {

    static final String SYSTEM_INSTRUCTION = """
Eres un asistente inteligente con acceso a herramientas matemáticas,
de información y manipulación de datos. Utiliza las herramientas cuando sea necesario
para responder de manera precisa
""";

    static final String USER_QUERY_CALLS = """
Necesito que calcules el factorial de 5, luego conviertas el resultado a
formato JSON, obtengas la hora actual y finalmente busques información
sobre 'algoritmos de factorial'. Presenta todo de manera organizada.
""";

    static final String USER_QUERY_SIMPLE = """
Quien eres?
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
                            //Content.fromParts(Part.fromText(USER_QUERY_SIMPLE)),
                            Content.fromParts(Part.fromText(USER_QUERY_CALLS)),
                            FunctionDirectory::executeTool)
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
