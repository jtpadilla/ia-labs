package io.github.jtpadilla.example.interactions.demo.researchfrontend.impl;

import io.github.glaforge.gemini.interactions.GeminiInteractionsClient;
import io.github.glaforge.gemini.interactions.model.Interaction;
import io.github.glaforge.gemini.interactions.model.InteractionParams;
import io.github.glaforge.gemini.interactions.model.Tool;
import io.github.glaforge.gemini.schema.GSchema;

public class Interactions {

    public static final String TOPICS_MODEL = "gemini-3-flash-preview";
    public static final String REPORT_AGENT = "deep-research-pro-preview-12-2025";
    public static final String SUMMARY_MODEL = "gemini-3-pro-preview";
    public static final String INFOGRAPHIC_MODEL = "gemini-3-pro-image-preview";

    final private GeminiInteractionsClient client;

    public Interactions(GeminiInteractionsClient client) {
        this.client = client;
    }

    public Interaction createPlan(String subject) {

        final InteractionParams.ModelInteractionParams planParams = InteractionParams.ModelInteractionParams.builder()
                .model(TOPICS_MODEL)
                .input(String.format("""
                                Find a list of topics to research on the following subject:
                                %s
                                """, subject))
                .responseFormat(GSchema.fromClass(String[].class))
                .tools(new Tool.GoogleSearch())
                .store(true)
                .build();

        return client.create(planParams);

    }


}
