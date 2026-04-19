package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.scorer;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.tool.ArxivCrawler;

public class ScorerAgentImpl {

    public static ScorerAgent build(ChatModel chatModel, ArxivCrawler arxivCrawler) {
        return AgenticServices.agentBuilder(ScorerAgent.class)
                .chatModel(chatModel)
                .tools(arxivCrawler)
                .outputKey("score")
                .build();
    }

}
