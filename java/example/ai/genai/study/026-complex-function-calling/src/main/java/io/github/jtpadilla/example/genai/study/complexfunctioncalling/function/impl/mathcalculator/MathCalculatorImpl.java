package io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.mathcalculator;

import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

import java.util.Map;

public class MathCalculatorImpl {

    public static Map<String, Object> execute(Map<String, Object> args) throws FunctionGatewayException {
        return execute(Parameters.create(args)).toMap();
    }

    private static Response execute(Parameters parameters) throws FunctionGatewayException {

        final String operation = parameters.operation();
        final Double number = parameters.number();

        return switch (operation) {
            case "factorial" -> new Response(calculateFactorial(number.intValue()));
            case "power" -> new Response(Math.pow(number, parameters.secondNumber().orElse(2.0)));
            case "sqrt" -> new Response(Math.sqrt(number));
            case "sum" -> new Response(parameters.numbers().stream().mapToDouble(Double::doubleValue).sum());
            default -> throw new FunctionGatewayException("MathCalculatorFunction.Impl operation not soported: " + operation);
        };

    }

    private static long calculateFactorial(int n) {
        if (n <= 1) return 1;
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

}
