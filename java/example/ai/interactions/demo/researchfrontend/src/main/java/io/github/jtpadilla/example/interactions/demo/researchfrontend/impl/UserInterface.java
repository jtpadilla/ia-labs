package io.github.jtpadilla.example.interactions.demo.researchfrontend.impl;

import io.github.glaforge.gemini.interactions.GeminiInteractionsClient;
import io.github.glaforge.gemini.interactions.model.*;
import io.github.glaforge.gemini.schema.GSchema;
import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserInterface implements JtRunnable {

    final private GeminiInteractionsClient client;

    public UserInterface(GeminiInteractionsClient client) {
        this.client = client;
    }

    private Optional<String> readSubject() {

        // Titulo
        Jt.title("🔎 Deep Research Agent").use();

        // Formulario
        final var formSubject = Jt.form().key("form_subject").use();

        final String subject = Jt.textArea("Subject")
                .key("subject")
                .placeholder("Enter the subject you want to research...")
                .use(formSubject);

        final var columns = Jt.columns(2)
                .widths(List.of(0.9, 0.1))
                .use(formSubject);

        Jt.formSubmitButton("Explore Topics")
                .type("primary")
                .onClick(b -> Jt.sessionState().remove("topics"))
                .use(columns.col(0));

        Jt.formSubmitButton("Clear All")
                .onClick(b -> {
                    Jt.setComponentState("subject", "");
                    Jt.sessionState().remove("topics");
                })
                .use(columns.col(1));

        if (subject == null || subject.isBlank()) {
            // Esperamos a que informa un subject y que pulse el boton de submit
            return Optional.empty();

        }

        return Optional.of(subject);

    }

    private List<String> topicSelector(String subject) {
        // Titulo
        Jt.header("Topics").use();

        // Formulario
        var formTopics = Jt.form().key("form_topics").use();

        // Contenedor para la lista de topics
        var topicsContainer = Jt.empty().key("topics_container").use(formTopics);

        @SuppressWarnings("unchecked")
        List<String> topics = (List<String>) Jt.sessionState().computeIfAbsent("topics", k -> {
            Jt.info("Preparing topics...").icon(":hourglass:").use(topicsContainer);

            InteractionParams.ModelInteractionParams planParams = InteractionParams.ModelInteractionParams.builder()
                    .model("gemini-3-flash-preview")
                    .input(String.format("""
                                Find a list of topics to research on the following subject:
                                %s
                                """, subject))
                    .responseFormat(GSchema.fromClass(String[].class))
                    .tools(new Tool.GoogleSearch())
                    .store(true)
                    .build();

            Interaction planInteraction = client.create(planParams);

            return Util.getTopics(planInteraction);
        });

        var topicSelectionContainer = Jt.container().key("topics").use(topicsContainer);

        List<String> selectedTopics = topics.stream()
                .filter(topic -> Jt.checkbox(topic).use(topicSelectionContainer))
                .toList();

        Jt.formSubmitButton("Launch Research").type("primary").use(formTopics);

        if (selectedTopics.isEmpty()) {
            // wait for user to select topics and hit form submit button
            return Collections.emptyList();
        }

        return selectedTopics;

    }


    @Override
    public void run() {

        // Se obtiene el subject
        final Optional<String> subject = readSubject();
        if (subject.isEmpty()) {
            return;
        }

        // Se obtiene la lista de topic que ha seleccionado el usuario
        final List<String> selectedTopics = topicSelector(subject.get());
        if (selectedTopics.isEmpty()) {
            return;
        }




        // reporting tabs
        Jt.header("Report").use();

        var tabs = Jt.tabs(List.of("Full Report", "Summary", "Infographic")).use();

        var reportContainer = Jt.container().key("reportContainer").use(tabs.tab(0));
        var reportPlaceholder = Jt.empty().key("fullReport").use(reportContainer);
        var summaryPlaceholder = Jt.empty().key("summary").use(tabs.tab(1));
        var infographicPlaceholder = Jt.empty().key("infographic").use(tabs.tab(2));

        /// put placeholders while results are being computed
        Jt.info("Preparing full report...").icon(":hourglass:").use(reportPlaceholder);
        Jt.info("Preparing summary...").icon(":hourglass:").use(summaryPlaceholder);
        Jt.info("Preparing infographic...").icon(":hourglass:").use(infographicPlaceholder);

        /// compute/fetch report section
        var topicsList = selectedTopics.stream().map(t -> "- " + t).collect(Collectors.joining("\n"));
        InteractionParams.AgentInteractionParams researchParams = InteractionParams.AgentInteractionParams.builder()
                .agent("deep-research-pro-preview-12-2025")
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
                        long elapsed = System.currentTimeMillis() - startTime;
                        String timeString = String.format("%dm%ds", elapsed / 60000, (elapsed % 60000) / 1000);
                        Jt.markdown("⏱️ `" + timeString + "` " + textContent.text()).use(reportPlaceholder);
                    }
                } else if (delta.delta() instanceof Events.TextDelta textPart) {
                    reportBuilder.append(textPart.text());
                    Jt.markdown(Util.transformCitations(reportBuilder.toString())).use(reportPlaceholder);
                }
            } else {
                System.out.printf("%nEVENT: %s\n", event);
            }
        });

        var rawReportExpander = Jt.expander("Raw Markdown Report").use(reportContainer);
        Jt.text(Util.transformCitations(reportBuilder.toString())).use(rawReportExpander);

        // compute/fetch summary
        InteractionParams.ModelInteractionParams summaryParams = InteractionParams.ModelInteractionParams.builder()
                .model("gemini-3-pro-preview")
                .input(String.format("""
                            Create a concise summary of the research below.
                            Go straight with the summary, don't introduce the summary
                            (don't write "Here's a summary..." or equivalent).

                            %s
                            """, reportBuilder))
                .store(true)
                .build();

        Interaction summaryInteraction = client.create(summaryParams);
        String summaryText = Util.getText(summaryInteraction);
        Jt.markdown(summaryText).use(summaryPlaceholder);

        // compute/fetch infographics
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

        Interaction infographicInteraction = client.create(infographicParams);
        var imageBytes = Util.getInfographicData(infographicInteraction);

        Jt.image(imageBytes).use(infographicPlaceholder);

    }

}
