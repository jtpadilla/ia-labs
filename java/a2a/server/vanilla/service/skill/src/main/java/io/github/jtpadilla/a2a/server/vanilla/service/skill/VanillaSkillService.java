package io.github.jtpadilla.a2a.server.vanilla.service.skill;

import io.github.jtpadilla.a2a.server.service.skill.SkillService;
import io.github.jtpadilla.a2a.server.service.skill.spi.SkillProvider;

import java.util.List;

public class VanillaSkillService implements SkillService {

    @Override
    public List<SkillProvider> skillList() {
        return List.of();
    }


}
