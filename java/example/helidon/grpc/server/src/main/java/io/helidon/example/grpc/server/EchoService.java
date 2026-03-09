package io.helidon.example.grpc.server;

import io.grpc.stub.StreamObserver;
import io.helidon.example.grpc.EchoRequest;
import io.helidon.example.grpc.EchoResponse;
import io.helidon.example.grpc.EchoServiceGrpc;

public class EchoService extends EchoServiceGrpc.EchoServiceImplBase {

    @Override
    public void echo(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
        String message = request.getMessage();
        EchoResponse response = EchoResponse.newBuilder()
                .setMessage("Echo: " + message)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
