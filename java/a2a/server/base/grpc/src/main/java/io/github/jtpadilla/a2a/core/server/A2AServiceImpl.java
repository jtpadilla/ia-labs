package io.github.jtpadilla.a2a.core.server;

import com.google.lf.a2a.v1.*;
import io.grpc.stub.StreamObserver;
import io.helidon.service.registry.Service;
import io.github.jtpadilla.a2a.server.service.skill.SkillService;
import io.github.jtpadilla.a2a.server.service.agentcard.AgentCardService;

import java.util.UUID;
import java.util.logging.Logger;

@Service.Singleton
public class A2AServiceImpl extends A2AServiceGrpc.A2AServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(A2AServiceImpl.class.getName());

    private SkillService skillService;
    private AgentCardService agentCardService;

    @Service.Inject
    public A2AServiceImpl(SkillService skillService, AgentCardService agentCardService) {
        this.skillService = skillService;
        this.agentCardService = agentCardService;
    }

    @Override
    public void sendMessage(SendMessageRequest request, StreamObserver<SendMessageResponse> responseObserver) {
        LOGGER.info("Received sendMessage: " + request.getMessage().getMessageId());
        String text = request.getMessage().getPartsList().stream()
                .filter(Part::hasText)
                .map(Part::getText)
                .findFirst()
                .orElse("");

        Message echoMessage = Message.newBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setContextId(request.getMessage().getContextId())
                .setRole(Role.ROLE_AGENT)
                .addParts(Part.newBuilder().setText(text).build())
                .build();

        responseObserver.onNext(SendMessageResponse.newBuilder()
                .setMessage(echoMessage)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendStreamingMessage(SendMessageRequest request, StreamObserver<StreamResponse> responseObserver) {
        LOGGER.info("Received sendStreamingMessage: " + request.getMessage().getMessageId());
        String text = request.getMessage().getPartsList().stream()
                .filter(Part::hasText)
                .map(Part::getText)
                .findFirst()
                .orElse("");

        Message echoMessage = Message.newBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setContextId(request.getMessage().getContextId())
                .setRole(Role.ROLE_AGENT)
                .addParts(Part.newBuilder().setText(text).build())
                .build();

        responseObserver.onNext(StreamResponse.newBuilder()
                .setStatusUpdate(TaskStatusUpdateEvent.newBuilder()
                        .setContextId(request.getMessage().getContextId())
                        .setStatus(TaskStatus.newBuilder()
                                .setState(TaskState.TASK_STATE_WORKING)
                                .build())
                        .build())
                .build());

        responseObserver.onNext(StreamResponse.newBuilder()
                .setMessage(echoMessage)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getExtendedAgentCard(GetExtendedAgentCardRequest request, StreamObserver<AgentCard> responseObserver) {
        responseObserver.onNext(agentCardService.agentCard());
        responseObserver.onCompleted();
    }
}
