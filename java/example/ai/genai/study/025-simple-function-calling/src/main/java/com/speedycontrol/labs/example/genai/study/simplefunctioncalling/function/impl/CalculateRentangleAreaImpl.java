package com.speedycontrol.labs.example.genai.study.simplefunctioncalling.function.impl;

import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

import java.util.Map;

public class CalculateRentangleAreaImpl {

    public static Map<String, Object> execute(Map<String, Object> args) throws FunctionGatewayException {
        return execute(Parameters.create(args)).toMap();
    }

    private static Response execute(Parameters parameters) {
        return new Response(parameters.length() * parameters.width());
    }

}
