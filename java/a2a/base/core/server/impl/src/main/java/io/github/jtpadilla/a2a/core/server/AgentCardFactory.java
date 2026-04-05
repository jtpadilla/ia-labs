package io.github.jtpadilla.a2a.core.server;

import com.google.lf.a2a.v1.AgentCapabilities;
import com.google.lf.a2a.v1.AgentCard;
import com.google.lf.a2a.v1.AgentInterface;
import com.google.lf.a2a.v1.AgentSkill;

public final class AgentCardFactory {

    private AgentCardFactory() {}

    public static AgentCard create() {

        final AgentSkill helloSkill = AgentSkill.newBuilder()
                .setId("echo")
                .setName("Echo")
                .setDescription("Echoes input")
                .addTags("echo")
                .build();

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
