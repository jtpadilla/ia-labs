package io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1.agent.diagnostic;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;

public class DiagnosticAgentImpl {

    public static DiagnosticAgent build(ChatModel chatModel) {
        return AgenticServices.agentBuilder(DiagnosticAgent.class)
                .chatModel(chatModel)
                // chatMemoryProvider activa el registro de AgenticScope por sessionId
                // (el valor de @MemoryId en el método del sub-agente)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("diagnosis")
                .build();
    }
}
