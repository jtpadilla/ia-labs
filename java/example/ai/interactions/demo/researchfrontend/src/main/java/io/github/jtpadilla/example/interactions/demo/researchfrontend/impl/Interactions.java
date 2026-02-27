package io.github.jtpadilla.example.interactions.demo.researchfrontend.impl;

import io.github.glaforge.gemini.interactions.GeminiInteractionsClient;
import io.github.glaforge.gemini.interactions.model.*;
import io.github.glaforge.gemini.schema.GSchema;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    public String createReport(
            String subject,
            List<String> selectedTopics,
            BiConsumer<Long, String> contentConsumer,
            Consumer<String> deltaConsumer) {

        var topicsList = selectedTopics.stream().map(t -> "- " + t).collect(Collectors.joining("\n"));

        final InteractionParams.AgentInteractionParams researchParams = InteractionParams.AgentInteractionParams.builder()
                .agent(REPORT_AGENT)
                .input(String.format("""
                            Write a concise research report on the following subject:
                            <subject>
                            %s
                            </subject>

                            By focusing on the following topics:
                            <topics>
                            %s
                            </topics>
                            """, subject, topicsList))
                .background(true)
                .stream(true)
                .agentConfig(new Config.DeepResearchAgentConfig(Config.ThinkingSummaries.AUTO))
                .store(true)
                .build();

        StringBuilder reportBuilder = new StringBuilder();

        long startTime = System.currentTimeMillis();

        client.stream(researchParams).forEach(event -> {
            if (event instanceof Events.ContentDelta delta) {
                if (delta.delta() instanceof Events.ThoughtSummaryDelta thought) {
                    if (thought.content() instanceof Content.TextContent textContent) {
                        contentConsumer.accept(System.currentTimeMillis() - startTime, textContent.text());
                    }
                } else if (delta.delta() instanceof Events.TextDelta textPart) {
                    reportBuilder.append(textPart.text());
                    deltaConsumer.accept(reportBuilder.toString());
                }
            } else {
                System.out.printf("%nEVENT: %s\n", event);
            }
        });

        return reportBuilder.toString();

    }

    public String createSummary(String report) {

        // compute/fetch summary
        InteractionParams.ModelInteractionParams summaryParams = InteractionParams.ModelInteractionParams.builder()
                .model(SUMMARY_MODEL)
                .input(String.format("""
                            Create a concise summary of the research below.
                            Go straight with the summary, don't introduce the summary
                            (don't write "Here's a summary..." or equivalent).

                            %s
                            """, report))
                .store(true)
                .build();

        Interaction summaryInteraction = client.create(summaryParams);
        return Util.getText(summaryInteraction);

    }

    public byte[] createInfographic(String summaryText) {

        InteractionParams.ModelInteractionParams infographicParams = InteractionParams.ModelInteractionParams.builder()
                .model("gemini-3-pro-image-preview")
                .input(String.format("""
                            Create a hand-drawn and hand-written sketchnote style summary infographic,
                            with a pure white background, use fluo highlighters for the key points,
                            about the following information:

                            %s
                            """, summaryText))
                .responseModalities(Interaction.Modality.IMAGE)
                .build();

        final Interaction infographicInteraction = client.create(infographicParams);

        return Util.getInfographicData(infographicInteraction);

    }

}
