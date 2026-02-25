package io.github.jtpadilla.gcloud.genai.schema;

import io.github.jtpadilla.gcloud.genai.GenAIWrapperException;

public class SchemaException extends GenAIWrapperException {

    public SchemaException(String message) {
        super(message);
    }

    public SchemaException(String message, Throwable cause) {
        super(message, cause);
    }

}
