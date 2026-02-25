package io.github.jtpadilla.gcloud.genai.function;

import com.google.genai.types.FunctionCall;

import java.util.Map;

@FunctionalInterface
public interface FunctionGatewayCall {
    Map<String, Object> execute(FunctionCall functionCall) throws FunctionGatewayException;
}
