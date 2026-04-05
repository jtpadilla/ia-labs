package io.github.jtpadilla.a2a.server.service.skill;

import com.google.lf.a2a.v1.SendMessageRequest;
import com.google.lf.a2a.v1.SendMessageResponse;
import io.grpc.stub.StreamObserver;

final public class SkillRequestSimple extends SkillRequest {

    final private StreamObserver<SendMessageResponse> responseObserver;

    public SkillRequestSimple(SendMessageRequest request, StreamObserver<SendMessageResponse> responseObserver) {
        super(request);
        this.responseObserver = responseObserver;
    }

    public StreamObserver<SendMessageResponse> responseObserver() {
        return responseObserver;
    }

}
