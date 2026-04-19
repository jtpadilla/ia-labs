package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.personextractor;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.domain.Person;

public interface PersonExtractor {

    @UserMessage("Extrae una persona del siguiente texto: {{prompt}}")
    @Agent("Extrae una persona del texto del usuario")
    Person extractPerson(@V("prompt") String prompt);

}
