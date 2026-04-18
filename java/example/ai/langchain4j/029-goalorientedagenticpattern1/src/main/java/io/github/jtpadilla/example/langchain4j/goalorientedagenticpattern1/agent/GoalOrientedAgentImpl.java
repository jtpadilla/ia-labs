package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.horoscopegenerator.HoroscopeGenerator;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.horoscopegenerator.HoroscopeGeneratorImpl;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.personextractor.PersonExtractor;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.personextractor.PersonExtractorImpl;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.signextractor.SignExtractor;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.signextractor.SignExtractorImpl;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.storyfinder.StoryFinder;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.storyfinder.StoryFinderImpl;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.writer.Writer;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.writer.WriterImpl;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.planner.GoalOrientedPlanner;

public class GoalOrientedAgentImpl {

    // El planner resuelve automáticamente el orden: prompt→person, prompt→sign,
    // person+sign→horoscope, person+horoscope→story, person+horoscope+story→writeup
    static public UntypedAgent build(ChatModel chatModel) {

        HoroscopeGenerator horoscopeGenerator = HoroscopeGeneratorImpl.build(chatModel);
        PersonExtractor personExtractor = PersonExtractorImpl.build(chatModel);
        SignExtractor signExtractor = SignExtractorImpl.build(chatModel);
        Writer writer = WriterImpl.build(chatModel);
        StoryFinder storyFinder = StoryFinderImpl.build(chatModel);

        // outputKey "writeup" es el goal que GoalOrientedPlanner intentará alcanzar
        return AgenticServices.plannerBuilder()
                .subAgents(horoscopeGenerator, personExtractor, signExtractor, writer, storyFinder)
                .outputKey("writeup")
                .planner(GoalOrientedPlanner::new)
                .build();
    }

}
