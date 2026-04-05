package io.github.jtpadilla.a2a.server.service.skill;

import com.google.lf.a2a.v1.SendMessageRequest;

public sealed class SkillRequest permits SkillRequestSimple, SkillRequestStream {

    final private SendMessageRequest request;

    public SkillRequest(SendMessageRequest request) {
        this.request = request;
    }

    public SendMessageRequest request() {
        return request;
    }

}
