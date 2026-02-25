package io.github.jtpadilla.gcloud.genai.util;

import io.github.jtpadilla.gcloud.genai.GenAIWrapperException;

public class FethPartException extends GenAIWrapperException {

    public FethPartException(String message) {
        super(message);
    }

    public FethPartException(String message, Throwable cause) {
        super(message, cause);
    }

}
