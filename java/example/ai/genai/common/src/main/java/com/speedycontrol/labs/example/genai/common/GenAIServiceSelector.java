package com.speedycontrol.labs.example.genai.common;

import io.github.jtpadilla.gcloud.context.DefaultGCloudContextService;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import io.github.jtpadilla.gcloud.genai.impl.GenAIServiceDefault;
import io.github.jtpadilla.gcloud.genai.impl.GenAIServiceGemma3ApiKey;

public class GenAIServiceSelector {

    static public IGenAIService fromDefault() {
        return fromFramework();
    }

    static public IGenAIService fromFramework() {
        return new GenAIServiceDefault(new DefaultGCloudContextService());
    }

    static public IGenAIService fromGemma3() {
        return new GenAIServiceGemma3ApiKey();
    }

}
