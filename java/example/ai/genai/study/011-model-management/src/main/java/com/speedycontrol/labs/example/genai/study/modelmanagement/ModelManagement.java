package com.speedycontrol.labs.example.genai.study.modelmanagement;

import com.google.genai.Client;
import com.google.genai.Pager;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.ListModelsConfig;
import com.google.genai.types.Model;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import com.speedycontrol.labs.example.genai.common.GenAIServiceSelector;

// Es posible que el comando client.models.list(..) no funciona en modo Vertex
public class ModelManagement {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            // Obtiene el modelo que utlizamos por defecto
            final Model modelResponse = client.models.get(genAIService.getLlmModel(), null);
            System.out.println("Current model: " + modelResponse);

            // Update the model if it's a tuned model.
            // De momento no quiero saber nada de tuned Models...
//            if (modelResponse.name().get().startsWith("tunedModels")) {
//                Model updatedModel =
//                        client.models.update(
//                                MODEL,
//                                UpdateModelConfig.builder()
//                                        .displayName("My updated model")
//                                        .description("My updated description")
//                                        .build());
//                System.out.println("Update Tuned Model response: " + updatedModel);
//            }

            // Lista de todos los modelos base
            // Por algun motivo no recupera la lista de modelos disponibles...
            final Pager<Model> modelList = client.models.list(ListModelsConfig.builder().pageSize(10).build());
            for (Model model : modelList) {
                System.out.println("Model: " + model.name().get());
            }

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }

    }

}
