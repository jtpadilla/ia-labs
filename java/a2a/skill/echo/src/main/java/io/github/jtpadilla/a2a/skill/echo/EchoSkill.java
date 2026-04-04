package io.github.jtpadilla.a2a.skill.echo;

import com.google.lf.a2a.v1.AgentSkill;
import io.github.jtpadilla.a2a.core.server.SkillProvider;

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

}
