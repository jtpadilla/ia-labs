package io.github.jtpadilla.example.genai.study.simplefunctioncalling.function;

import com.google.genai.types.FunctionCall;
import com.google.genai.types.Tool;
import io.github.jtpadilla.example.genai.study.simplefunctioncalling.function.impl.CalculateRentangleAreaDef;
import io.github.jtpadilla.example.genai.study.simplefunctioncalling.function.impl.CalculateRentangleAreaImpl;
import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

import java.util.HashMap;
import java.util.Map;

public class FunctionDirectory {

    public static Tool createTool() {
        return Tool.builder()
                .functionDeclarations(
                        CalculateRentangleAreaDef.DECLARATION
                ).build();
    }

    public static Map<String, Object> execute(FunctionCall functionCall) throws FunctionGatewayException {

        // Precondiciones
        if (functionCall.name().isEmpty()) {
            throw new FunctionGatewayException("FunctionCall has empty function name: " + functionCall.toString());
        }

        // Se extraen los datos de la funcion
        final String functionName = functionCall.name().get();
        final Map<String, Object> args = functionCall.args().orElse(new HashMap<>());

        // Se ejecuta la funcion
        return switch (functionName) {
            case CalculateRentangleAreaDef.NAME -> CalculateRentangleAreaImpl.execute(args);
            default -> throw new FunctionGatewayException("FunctionCall has unknown function name: " + functionName);
        };

    }

}
