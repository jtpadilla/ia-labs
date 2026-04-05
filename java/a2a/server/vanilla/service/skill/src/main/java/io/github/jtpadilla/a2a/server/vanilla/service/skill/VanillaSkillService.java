package io.github.jtpadilla.a2a.server.vanilla.service.skill;

import com.google.lf.a2a.v1.AgentCapabilities;
import com.google.lf.a2a.v1.AgentCard;
import com.google.lf.a2a.v1.AgentInterface;
import io.github.jtpadilla.a2a.server.service.skill.*;
import io.github.jtpadilla.a2a.server.service.skill.spi.SkillProvider;

import java.util.List;

public class VanillaSkillService implements SkillService {

    @Override
    public List<SkillProvider> skillList() {
        return List.of();
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
                .addDefaultOutputModes("text/plain")
                .addSkills(helloSkill)
                .build();
    }

}
