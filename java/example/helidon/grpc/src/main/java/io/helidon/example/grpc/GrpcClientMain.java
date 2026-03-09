package io.helidon.example.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GrpcClientMain {

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build();

        try {
            EchoServiceGrpc.EchoServiceBlockingStub stub = EchoServiceGrpc.newBlockingStub(channel);

            EchoRequest request = EchoRequest.newBuilder()
                    .setMessage("Hello from standard gRPC Client!")
                    .build();

            EchoResponse response = stub.echo(request);

            System.out.println("Response from server: " + response.getMessage());
        } finally {
            channel.shutdownNow();
        }
    }
}
