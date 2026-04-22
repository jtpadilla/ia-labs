package io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1.agent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1.agent.diagnostic.DiagnosticAgentImpl;
import io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1.agent.solution.SolutionAgentImpl;

public class SupportAgentImpl {

    public static SupportAgent build(ChatModel chatModel) {
        // sequenceBuilder crea un AgenticScope por invocación donde los sub-agentes
        // almacenan sus salidas. Los sub-agentes tienen su propia chatMemoryProvider
        // con @MemoryId, lo que activa el registro y el AgenticScopePersister.
        return AgenticServices.sequenceBuilder(SupportAgent.class)
                .subAgents(
                        DiagnosticAgentImpl.build(chatModel),
                        SolutionAgentImpl.build(chatModel)
                )
                .outputKey("solution")
                .build();
    }
}
