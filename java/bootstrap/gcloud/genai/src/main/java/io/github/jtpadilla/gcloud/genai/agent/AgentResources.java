package io.github.jtpadilla.gcloud.genai.agent;

import com.google.genai.Client;

public record AgentResources(Client client, String llmModel) {
}
