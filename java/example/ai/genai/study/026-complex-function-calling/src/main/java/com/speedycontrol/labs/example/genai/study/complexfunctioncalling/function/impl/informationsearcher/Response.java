package com.speedycontrol.labs.example.genai.study.complexfunctioncalling.function.impl.informationsearcher;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

class Response {

    final private String result;

    public Response(String result) {
        this.result = result;
    }

    public Map<String, Object> toMap() {
        return ImmutableMap.of("result", result);
    }

}
