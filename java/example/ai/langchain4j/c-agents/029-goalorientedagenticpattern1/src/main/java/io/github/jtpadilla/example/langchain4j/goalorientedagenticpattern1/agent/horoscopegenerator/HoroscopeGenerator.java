package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.horoscopegenerator;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.domain.Person;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.domain.Sign;

public interface HoroscopeGenerator {

    @SystemMessage("Eres un astrólogo que genera horóscopos basándose en el nombre y el signo zodiacal del usuario.")
    @UserMessage("Genera el horóscopo para {{person}} que es {{sign}}.")
    @Agent("Un astrólogo que genera horóscopos basándose en el nombre y el signo zodiacal del usuario.")
    String horoscope(@V("person") Person person, @V("sign") Sign sign);

}
