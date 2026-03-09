package io.helidon.example.grpc;

import io.grpc.stub.StreamObserver;

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
