package io.github.jtpadilla.a2a.server.service.skill;

import com.google.lf.a2a.v1.SendMessageRequest;
import com.google.lf.a2a.v1.StreamResponse;

final public class SkillRequestStream extends SkillRequest {

    final private StreamResponse responseObsever;

    public SkillRequestStream(SendMessageRequest request, StreamResponse responseObsever) {
        super(request);
        this.responseObsever = responseObsever;
    }

    public StreamResponse responseObsever() {
        return responseObsever;
    }

}
