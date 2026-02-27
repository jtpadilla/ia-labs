package io.github.jtpadilla.example.interactions.demo.researchfrontend.impl;

import io.github.glaforge.gemini.interactions.GeminiInteractionsClient;
import io.github.glaforge.gemini.interactions.model.*;
import io.javelit.core.Jt;
import io.javelit.core.JtRunnable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class UserInterface implements JtRunnable {

    final private GeminiInteractionsClient client;
    final private Interactions interactions;

    public UserInterface(GeminiInteractionsClient client) {
        this.client = client;
        this.interactions = new Interactions(client);
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

        // Ya tenemos la selección
        research(subject.get(), selectedTopics);

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
            return Util.getTopics(interactions.createPlan(subject));
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

    private void research(String subject, List<String> selectedTopics) {

        // reporting tabs
        Jt.header("Report").use();

        var tabs = Jt.tabs(List.of("Full Report", "Summary", "Infographic")).use();

        var reportContainer = Jt.container().key("reportContainer").use(tabs.tab(0));
        var reportPlaceholder = Jt.empty().key("fullReport").use(reportContainer);
        var summaryPlaceholder = Jt.empty().key("summary").use(tabs.tab(1));
        var infographicPlaceholder = Jt.empty().key("infographic").use(tabs.tab(2));

        // put placeholders while results are being computed
        Jt.info("Preparing full report...").icon(":hourglass:").use(reportPlaceholder);
        Jt.info("Preparing summary...").icon(":hourglass:").use(summaryPlaceholder);
        Jt.info("Preparing infographic...").icon(":hourglass:").use(infographicPlaceholder);

        // compute/fetch report section

        final BiConsumer<Long, String> contentConsumer = (elapsed, text) -> {
            String timeString = String.format("%dm%ds", elapsed / 60000, (elapsed % 60000) / 1000);
            Jt.markdown("⏱️ `" + timeString + "` " + text).use(reportPlaceholder);
        };

        final Consumer<String> deltaConsumer = (text) -> {
            Jt.markdown(Util.transformCitations(text)).use(reportPlaceholder);
        };

        final String report = interactions.createTopics(subject, selectedTopics, contentConsumer, deltaConsumer);

        var rawReportExpander = Jt.expander("Raw Markdown Report").use(reportContainer);
        Jt.text(Util.transformCitations(report)).use(rawReportExpander);

        // compute/fetch summary
        InteractionParams.ModelInteractionParams summaryParams = InteractionParams.ModelInteractionParams.builder()
                .model("gemini-3-pro-preview")
                .input(String.format("""
                            Create a concise summary of the research below.
                            Go straight with the summary, don't introduce the summary
                            (don't write "Here's a summary..." or equivalent).

                            %s
                            """, report))
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
