package io.github.jtpadilla.gcloud.genai.function;

import com.google.genai.Client;
import com.google.genai.types.*;
import io.github.jtpadilla.gcloud.genai.function.part.PartFunctionCallWrapper;
import io.github.jtpadilla.gcloud.genai.function.part.PartWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FunctionGateway {

    static public class Builder {

        final private String model;
        final private Client client;
        final private Content systemPrompt;
        final private Content userPrompt;
        final private FunctionGatewayCall toolImpl;
        final private List<Tool> tools;
        private Schema schema;

        private Builder(String model, Client client, Content systemPrompt, Content userPrompt, FunctionGatewayCall toolImpl) {
            this.model = model;
            this.client = client;
            this.systemPrompt = systemPrompt;
            this.userPrompt = userPrompt;
            this.toolImpl = toolImpl;
            this.tools = new ArrayList<>();
        }

        public Builder addTool(Tool tool) {
            tools.add(tool);
            return this;
        }

        public Builder setSchema(Schema schema) {
            this.schema = schema;
            return this;
        }

        public String generate() throws FunctionGatewayException {
            final FunctionGateway gateway = new FunctionGateway(model, client, systemPrompt, userPrompt, toolImpl, tools, schema);
            return gateway.generate();
        }

    }

    static public Builder builder(String model, Client client, Content systemPrompt, Content userPrompt, FunctionGatewayCall toolImpl) {
        return new Builder(model, client, systemPrompt, userPrompt, toolImpl);
    }

    final private String model;
    final private Client client;
    final private FunctionGatewayCall toolImpl;
    final private List<Content> conversationHistory;
    final private GenerateContentConfig generateContentConfig;

    public FunctionGateway(
            String model,
            Client client,
            Content systemPrompt,
            Content userPrompt,
            FunctionGatewayCall toolImpl,
            List<Tool> tools,
            Schema schema) {
        this.model = model;
        this.client = client;
        this.toolImpl = toolImpl;
        this.conversationHistory = new ArrayList<>();
        this.conversationHistory.add(userPrompt);
        final GenerateContentConfig.Builder builder = GenerateContentConfig.builder()
                .tools(tools)
                .systemInstruction(systemPrompt)
                .candidateCount(1);
        if (schema != null) {
            builder.responseSchema(schema);
            builder.responseMimeType("application/json");
        }
        generateContentConfig = builder.build();
    }

    public String generate() throws FunctionGatewayException {

        int limit = 5;

        while(true) {

            if (--limit < 0) {
                throw new FunctionGatewayException("Conversation has many tries");
            }

            final PartWrapper.Sumary sumary = PartWrapper.extractSumary(
                    client.models.generateContent(
                            model,
                            conversationHistory,
                            generateContentConfig
                    )
            );

            if (!sumary.textList().isEmpty() && sumary.functionCallList().isEmpty() && sumary.otherList().isEmpty()) {
                // Unicamente hay texto coo resultado
                return sumary.content().text();

            } else if (sumary.textList().isEmpty() && !sumary.functionCallList().isEmpty() && sumary.otherList().isEmpty()) {
                // Unicamente hay funciones como resultado
                // Se incorpora en la historia las llamadeas que ha hecho el Llm a las funciones
                conversationHistory.add(sumary.content());
                // Se incorpora en la historia el resultado de las llamadas
                conversationHistory.add(executeFunctions(sumary.functionCallList()));

            } else {
                throw new FunctionGatewayException("El resultado tiene una combinacion de Parts que no se como tratar!");
            }

        }

    }

    private Content executeFunctions(List<PartFunctionCallWrapper> functionCallList) throws FunctionGatewayException {

        final List<Part> results = new ArrayList<>();

        // Para cada una de las funciones...
        for (PartFunctionCallWrapper functionCallWrapper : functionCallList) {

            // Se obtiene el nombre de la funcion que el LLM esta invocando
            final FunctionCall functionCall = functionCallWrapper.functionCall();

            // Se ejecuta la funcion que reside en nuestro codigo
            final Map<String,Object> returnValue = toolImpl.execute(functionCall);

            // El valor de retorno se incorpora al historial de la conversacion
            // para que la siguiente invocacion al Llm los tenga disponibles.
            results.add(Part.fromFunctionResponse(functionCallWrapper.name(), returnValue));

        }

        return Content.builder().parts(results).build();

    }

}
