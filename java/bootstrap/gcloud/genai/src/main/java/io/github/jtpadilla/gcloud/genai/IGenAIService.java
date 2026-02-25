package io.github.jtpadilla.gcloud.genai;

import com.google.genai.Client;

import java.util.List;

public interface IGenAIService {

    List<String> SCOPE_LIST = List.of(
            "https://www.googleapis.com/auth/cloud-platform"
    );

    Client createClient();
    String getLlmModel();
    String getEmbedModel();

}
