package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.storyfinder;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.domain.Person;

public interface StoryFinder {

    @SystemMessage("""
            Eres un buscador de historias. Usa las herramientas de búsqueda web proporcionadas,
            llamándolas una única vez, para encontrar una historia ficticia y divertida en internet
            sobre el tema indicado por el usuario.
            """)
    @UserMessage("""
            Busca en internet una historia para {{person}} que tiene el siguiente horóscopo: {{horoscope}}.
            """)
    @Agent("Busca en internet una historia para una persona dado su horóscopo")
    String findStory(@V("person") Person person, @V("horoscope") String horoscope);

}
