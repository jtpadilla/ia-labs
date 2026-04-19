package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.writer;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.domain.Person;

public interface Writer {

    @UserMessage("""
            Crea un texto divertido para {{person}} basándote en lo siguiente:
            - su horóscopo: {{horoscope}}
            - una noticia actual: {{story}}
            """)
    @Agent("Crea un texto divertido para la persona objetivo basándose en su horóscopo y noticias actuales")
    String write(@V("person") Person person, @V("horoscope") String horoscope, @V("story") String story);

}
