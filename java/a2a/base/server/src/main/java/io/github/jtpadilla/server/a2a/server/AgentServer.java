package io.github.jtpadilla.a2a.server;

import com.google.lf.a2a.v1.*;
import io.grpc.stub.StreamObserver;

public class AgentServer extends A2AServiceGrpc.A2AServiceImplBase {

    @Override
    public void sendMessage(SendMessageRequest request, StreamObserver<SendMessageResponse> responseObserver) {
        super.sendMessage(request, responseObserver);
    }

    @Override
    public void sendStreamingMessage(SendMessageRequest request, StreamObserver<StreamResponse> responseObserver) {
        super.sendStreamingMessage(request, responseObserver);
    }

    @Override
    public void getTask(GetTaskRequest request, StreamObserver<Task> responseObserver) {
        super.getTask(request, responseObserver);
    }

    @Override
    public void listTasks(ListTasksRequest request, StreamObserver<ListTasksResponse> responseObserver) {
        super.listTasks(request, responseObserver);
    }

    @Override
    public void cancelTask(CancelTaskRequest request, StreamObserver<Task> responseObserver) {
        super.cancelTask(request, responseObserver);
    }

    @Override
    public void subscribeToTask(SubscribeToTaskRequest request, StreamObserver<StreamResponse> responseObserver) {
        super.subscribeToTask(request, responseObserver);
    }

    @Override
    public void getExtendedAgentCard(GetExtendedAgentCardRequest request, StreamObserver<AgentCard> responseObserver) {
        super.getExtendedAgentCard(request, responseObserver);
    }

}
