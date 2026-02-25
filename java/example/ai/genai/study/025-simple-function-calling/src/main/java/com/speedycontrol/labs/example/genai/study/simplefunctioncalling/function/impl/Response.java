package com.speedycontrol.labs.example.genai.study.simplefunctioncalling.function.impl;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

class Response {

    final private double area;

    public Response(double area) {
        this.area = area;
    }

    public Map<String, Object> toMap() {
        return ImmutableMap.of("area", area);
    }

}
