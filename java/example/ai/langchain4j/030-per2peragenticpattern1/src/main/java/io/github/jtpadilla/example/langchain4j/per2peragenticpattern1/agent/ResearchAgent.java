package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

public interface ResearchAgent {

    @Agent("Realiza investigación iterativa sobre un tema: recopila literatura, formula una hipótesis, critícala, valídala y puntúala hasta que la puntuación sea satisfactoria.")
    String research(@V("topic") String topic);

}
