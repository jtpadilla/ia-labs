package io.github.jtpadilla.example.genai.study.cachedconentoperations;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.UpdateCachedContentConfig;
import io.github.jtpadilla.example.genai.common.GenAIServiceSelector;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import io.github.jtpadilla.gcloud.genai.cached.CachedContentException;
import io.github.jtpadilla.gcloud.genai.cached.CachedContentWrapper;
import io.github.jtpadilla.gcloud.genai.cached.CachesWrapper;
import io.github.jtpadilla.gcloud.genai.util.FethPartException;
import io.github.jtpadilla.gcloud.genai.util.FethPartUtil;

import java.time.Duration;
import java.util.List;

public final class CachedContentOperations {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            // Se prepara el contenido
            System.out.format("Descargando PDF...%n");
            final Content content =
                    Content.fromParts(
                            FethPartUtil.fetchPdfPart(
                                    "https://storage.googleapis.com/cloud-samples-data/generative-ai/pdf/2403.05530.pdf")
                    );
            displayAll(client, "Despues descarga");

            // Se carga la cache
            System.out.format("CREANDO RECURSO...%n");
            final CachedContentWrapper cachedContentWrapper = CachesWrapper.create(genAIService.getLlmModel(), client, content, "nombre del recurso");
            System.out.format("CREATED -> %s%n", cachedContentWrapper);
            displayAll(client, "Despues creacion entrada en cache");

            // Se obtiene copntenido de la cache por nombre
            final CachedContentWrapper gettedContent = CachesWrapper.get(client, cachedContentWrapper.cachedContentName());
            System.out.format("GETTED -> %s%n", gettedContent);
            displayAll(client, "Despues obtener entrada desde la cache");

            // Se actualiza la cache
            final CachedContentWrapper updatedContentWrapper = CachesWrapper.update(
                    client,
                    cachedContentWrapper.cachedContentName(),
                    UpdateCachedContentConfig.builder().ttl(Duration.ofMinutes(10)).build()
            );
            System.out.format("UPDATED -> %s%n", updatedContentWrapper);
            displayAll(client, "Despues actualizacion entrada en la cache");

            // Delete the cached content.
            CachesWrapper.delete(client, cachedContentWrapper.cachedContentName());
            System.out.println("Deleted cached content: " + cachedContentWrapper.cachedContentName().name());
            displayAll(client, "Despues borrado entrada en la cache");

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        } catch (FethPartException | CachedContentException e) {
            throw new RuntimeException(e);
        }

    }

    static private void displayAll(Client client, String section) throws CachedContentException {
        System.out.println(section);
        int i = 0;
        final List<CachedContentWrapper> list = CachesWrapper.list(client, 10);
        for (var wrapper : list) {
            System.out.format("cacheEntry[%d/%d] -> %s%n", i+1, list.size(), wrapper.toString());
        }
        System.out.println();
    }

}
