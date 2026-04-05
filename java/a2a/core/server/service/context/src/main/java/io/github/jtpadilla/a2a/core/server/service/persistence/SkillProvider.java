package io.github.jtpadilla.a2a.core.server.service.persistence;

import com.google.lf.a2a.v1.AgentSkill;

public interface SkillProvider {
    AgentSkill getSkillCard();
    A2APersistentContext executeRequest(A2ARequestContext context);
}
