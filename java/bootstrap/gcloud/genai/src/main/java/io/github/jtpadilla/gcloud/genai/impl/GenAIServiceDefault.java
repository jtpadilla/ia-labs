package io.github.jtpadilla.gcloud.genai.impl;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;
import com.google.genai.types.ClientOptions;
import com.google.genai.types.HttpOptions;
import com.google.inject.Inject;
import io.github.jtpadilla.gcloud.context.IGCloudContextService;
import io.github.jtpadilla.gcloud.genai.IGenAIService;

public class GenAIServiceDefault implements IGenAIService {

    final private IGCloudContextService gcloudContextService;
    final GoogleCredentials scopedCredentials;

    @Inject
    public GenAIServiceDefault(IGCloudContextService gcloudContextService) {
        this.gcloudContextService = gcloudContextService;
        this.scopedCredentials = gcloudContextService.getGcloudCredentials()
                .createScoped(SCOPE_LIST);
    }

    @Override
    public Client createClient() {

        // Builder general
        final Client.Builder clientBuilder = Client.builder()
                .vertexAI(true)
                .credentials(scopedCredentials)
                .project(gcloudContextService.getGCloudProjectId())
                .location(gcloudContextService.getGenAILocation());

        // httpOptions
        final HttpOptions.Builder httpOptionsBuilder = HttpOptions.builder()
                .apiVersion("v1");
        gcloudContextService.getGenAIHttpTimeout().ifPresent(httpOptionsBuilder::timeout);
        gcloudContextService.getGenAIHttpBaseurl().ifPresent(httpOptionsBuilder::baseUrl);
        clientBuilder.httpOptions(httpOptionsBuilder.build());

        // clientOptions
        if (gcloudContextService.getGenAIClientMaxConnections().isPresent() || gcloudContextService.getGenIClientMaxConnectionsPerhost().isPresent()) {
            final ClientOptions.Builder clientOptionsBuilder = ClientOptions.builder();
            gcloudContextService.getGenAIClientMaxConnections().ifPresent(clientOptionsBuilder::maxConnections);
            gcloudContextService.getGenIClientMaxConnectionsPerhost().ifPresent(clientOptionsBuilder::maxConnectionsPerHost);
            clientBuilder.clientOptions(clientOptionsBuilder.build());
        }

        // Final
        return clientBuilder.build();
    }

    @Override
    public String getLlmModel() {
        return gcloudContextService.getGenAILlmModel();
    }

    @Override
    public String getEmbedModel() {
        return gcloudContextService.getGenAIEmbedModel();
    }

}
