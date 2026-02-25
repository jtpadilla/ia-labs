package io.github.jtpadilla.example.genai.study.complexfunctioncalling.function;

import com.google.genai.types.FunctionCall;
import com.google.genai.types.Tool;
import io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.formatconverter.FormatConverterDef;
import io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.formatconverter.FormatConverterImpl;
import io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.informationsearcher.InformationSearcherDef;
import io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.informationsearcher.InformationSearcherImpl;
import io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.mathcalculator.MathCalculatorDef;
import io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.mathcalculator.MathCalculatorImpl;
import io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.systeminfo.SystemInfoDef;
import io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.systeminfo.SystemInfoImpl;
import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

import java.util.HashMap;
import java.util.Map;

public class FunctionDirectory {

    public static Tool createTool() {
        return Tool.builder()
                .functionDeclarations(
                        FormatConverterDef.DECLARATION,
                        InformationSearcherDef.DECLARATION,
                        MathCalculatorDef.DECLARATION,
                        SystemInfoDef.DECLARATION)
                .build();
    }

    public static Map<String, Object> executeTool(FunctionCall functionCall) throws FunctionGatewayException {
        if (functionCall.name().isEmpty()) {
            throw new FunctionGatewayException("No se ha proporcionado el nombre de la funcion");
        }
        final String functionName = functionCall.name().get();
        final Map<String, Object> args = functionCall.args().orElse(new HashMap<>());

        return switch (functionName) {
            case MathCalculatorDef.NAME -> MathCalculatorImpl.execute(args);
            case FormatConverterDef.NAME -> FormatConverterImpl.execute(args);
            case SystemInfoDef.NAME -> SystemInfoImpl.execute(args);
            case InformationSearcherDef.NAME -> InformationSearcherImpl.execute(args);
            default -> throw new FunctionGatewayException("Función no reconocida: " + functionName);
        };
    }

}
