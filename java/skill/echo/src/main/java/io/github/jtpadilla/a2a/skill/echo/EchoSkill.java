package io.github.jtpadilla.a2a.skill.echo;

import com.google.lf.a2a.v1.AgentSkill;
import io.github.jtpadilla.a2a.server.service.skill.SkillContext;
import io.github.jtpadilla.a2a.server.service.skill.spi.SkillProvider;
import io.helidon.service.registry.Service;

@Service.Singleton
public class EchoSkill implements SkillProvider {

    @Override
    public AgentSkill getSkillCard() {
        return AgentSkill.newBuilder()
                .setId("echo")
                .setName("Echo")
                .setDescription("Echoes input")
                .addTags("echo")
                .build();
    }

    @Override
    public void executeSkill(SkillContext context) {
    }

}
