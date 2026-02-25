package com.speedycontrol.labs.example.vertextai.common;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.VertexAI;
import io.github.jtpadilla.gcloud.context.DefaultGCloudContextService;
import io.github.jtpadilla.gcloud.context.IGCloudContextService;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import io.github.jtpadilla.gcloud.genai.impl.GenAIServiceDefault;

import java.io.IOException;

public class VertextaiUtil {

    static final IGCloudContextService GCLOUD_CONTEXT_SERVICE;
    static final IGenAIService GEN_AI_SERVICE;

    static {
        GCLOUD_CONTEXT_SERVICE = new DefaultGCloudContextService();
        GEN_AI_SERVICE = new GenAIServiceDefault(GCLOUD_CONTEXT_SERVICE);
    }

    static public VertexAI.Builder vertexBuilder() throws IOException {
        final Credentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(IGenAIService.SCOPE_LIST);
        return new VertexAI.Builder()
                .setProjectId(GCLOUD_CONTEXT_SERVICE.getGCloudProjectId())
                .setCredentials(credentials);
    }

}
