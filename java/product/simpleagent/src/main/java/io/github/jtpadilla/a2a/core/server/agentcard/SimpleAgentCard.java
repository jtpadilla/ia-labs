package io.github.jtpadilla.a2a.core.server.agentcard;

import com.google.lf.a2a.v1.AgentCapabilities;
import com.google.lf.a2a.v1.AgentCard;
import com.google.lf.a2a.v1.AgentInterface;
import com.google.lf.a2a.v1.AgentSkill;
import io.github.jtpadilla.a2a.server.service.agentcard.AgentCardService;
import io.github.jtpadilla.a2a.server.service.skill.SkillService;
import io.github.jtpadilla.a2a.server.service.skill.spi.SkillProvider;
import io.helidon.service.registry.Service;

import java.util.List;

@Service.Singleton
public class SimpleAgentCard implements AgentCardService {

    final private AgentCard agentCard;

    public SimpleAgentCard(SkillService skillService) {
        final List<AgentSkill> agentSkills = skillService.skillList().stream()
                .map(SkillProvider::getSkillCard)
                .toList();
        this.agentCard = agentCardBuilder().addAllSkills(agentSkills).build();
    }

    @Override
    public AgentCard agentCard() {
        return agentCard;
    }

    private AgentCard.Builder agentCardBuilder() {
        return AgentCard.newBuilder()
                .setName("IAtevale Agent")
                .setDescription("Pemite evaluar la tecnologia A2A")
                .setVersion("0.0.1")
                .addSupportedInterfaces(AgentInterface.newBuilder()
                        .setUrl("http://localhost:8080/lf.a2a.v1.A2AService/GetTask")
                        .setProtocolBinding("GRPC")
                        .setProtocolVersion("1.0")
                        .build())
                .setCapabilities(AgentCapabilities.newBuilder()
                        .setStreaming(false)
                        .build())
                .addDefaultInputModes("text/plain")
                .addDefaultOutputModes("text/plain");
    }

}
