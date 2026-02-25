package io.github.jtpadilla.gcloud.genai.impl;

import com.google.genai.Client;
import io.github.jtpadilla.gcloud.context.DefaultGCloudContextService;
import io.github.jtpadilla.gcloud.context.IGCloudContextService;
import io.github.jtpadilla.gcloud.genai.IGenAIService;

public class GenAIServiceGemma3ApiKey implements IGenAIService{

    // Con el acceso a traves del Endpoint de AIStudio en el momento de
    // hacer las pruebas me he encontrado que casi todas las funcionalidades
    // extras estan deshabilitadas.
    // ------------------------------------------------------------------
    // -> Function calling is not enabled for models/gemma-3-4b-it
    // -> Enum mode is not enabled for models/gemma-3-4b-it
    // -> JSON mode is not enabled for models/gemma-3-4b-it
    // -> Developer instruction is not enabled for models/gemma-3-4b-it
    // -> Url Context as tool is not enabled for models/gemma-3-4b-it
    // Tambien lo he intentado accediento por el Endpoint de Vertex AI
    // como lo hacemos para gemini-2-5 y no encuentra los modelos
    // y eso que he cambiado a "us-central1" que segun Gemini es por
    // donde se empiezan a desplegar los modelos.
    // ------------------------------------------------------------------
    // Es posible por lo que veo en le doc de Google que simplemente
    // no este todavia disponible Gemma 3 ya que en la documentacion
    // unicamente habla de Gemma 2.
    // Pero tambien he intentado utilizan un modelo de gemma 2 y no ha funcionado...
    // ------------------------------------------------------------------
    // Ver -> https://ai.google.dev/gemma
    // Ver -> https://deepmind.google/models/gemma/
    // Ver -> https://ai.google.dev/gemma/docs/integrations/google-cloud?hl=es-419
    // Ver -> https://cloud.google.com/vertex-ai/generative-ai/docs/open-models/use-gemma?hl=es-419

    // Modelo de 4 mil millones de parámetros, buen balance entre rendimiento y velocidad.
    final static private String GEMMA_3_4B = "gemma-3-4b-it";

    // Modelo de 27 mil millones de parámetros, más potente y con mayor capacidad de razonamiento.
    final static private String GEMMA_3_27B = "gemma-3-27b-it";

    // El modelo más ligero, ideal para tareas rápidas o entornos con menos recursos.
    final static private String GEMMA_3_270M = "gemma-3-270m-it";

    final static private String GEMMA_CURRENT = GEMMA_3_27B;
    final static private String API_KEY = "AIzaSyAv7OtguKa0JVYkQ3OAfhTckdVbqxK_o-8";

    final IGCloudContextService gcloudContextService;

    public GenAIServiceGemma3ApiKey() {
        gcloudContextService = new DefaultGCloudContextService();
    }

    @Override
    public Client createClient() {
        return Client.builder()
                .apiKey(API_KEY)
                .build();
    }

    @Override
    public String getLlmModel() {
        return GEMMA_CURRENT;
    }

    @Override
    public String getEmbedModel() {
        return gcloudContextService.getGenAIEmbedModel();
    }

}
