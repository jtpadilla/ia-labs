package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level0;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ExpertRouterAgent {

    @UserMessage("{{request}}")
    @Agent("Enruta la solicitud al experto adecuado y devuelve su respuesta")
    String ask(@V("request") String request);

}
