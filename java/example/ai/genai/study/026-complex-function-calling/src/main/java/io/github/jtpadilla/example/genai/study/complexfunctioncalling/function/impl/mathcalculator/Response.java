package io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.mathcalculator;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

class Response {

    final private double result;

    public Response(double result) {
        this.result = result;
    }

    public Map<String, Object> toMap() {
        return ImmutableMap.of("result", result);
    }

}
