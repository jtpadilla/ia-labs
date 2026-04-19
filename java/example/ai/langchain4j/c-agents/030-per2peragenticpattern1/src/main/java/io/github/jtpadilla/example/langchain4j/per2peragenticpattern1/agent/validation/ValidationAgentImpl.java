package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.validation;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.tool.ArxivCrawler;

public class ValidationAgentImpl {

    public static ValidationAgent build(ChatModel chatModel, ArxivCrawler arxivCrawler) {
        return AgenticServices.agentBuilder(ValidationAgent.class)
                .chatModel(chatModel)
                .tools(arxivCrawler)
                .outputKey("hypothesis")
                .build();
    }

}
