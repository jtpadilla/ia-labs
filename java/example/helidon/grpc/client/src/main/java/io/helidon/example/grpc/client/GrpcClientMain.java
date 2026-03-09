package io.helidon.example.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.helidon.example.grpc.EchoRequest;
import io.helidon.example.grpc.EchoResponse;
import io.helidon.example.grpc.EchoServiceGrpc;

public class GrpcClientMain {

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build();

        try {
            EchoServiceGrpc.EchoServiceBlockingStub stub = EchoServiceGrpc.newBlockingStub(channel);

            EchoRequest request = EchoRequest.newBuilder()
                    .setMessage("Hello from separated gRPC Client!")
                    .build();

            EchoResponse response = stub.echo(request);

            System.out.println("Response from server: " + response.getMessage());
        } finally {
            channel.shutdownNow();
        }
    }
}
