package io.github.jtpadilla.gcloud.genai.agent;

import io.github.jtpadilla.gcloud.genai.GenAIWrapperException;

public class AgentException extends GenAIWrapperException {

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }

}
