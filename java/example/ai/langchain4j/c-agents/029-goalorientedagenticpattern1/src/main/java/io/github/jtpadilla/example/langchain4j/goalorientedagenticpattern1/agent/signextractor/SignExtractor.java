package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.signextractor;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.domain.Sign;

public interface SignExtractor {

    @UserMessage("Extrae el signo zodiacal de una persona del siguiente texto: {{prompt}}")
    @Agent("Extrae el signo zodiacal del texto del usuario")
    Sign extractSign(@V("prompt") String prompt);

}
