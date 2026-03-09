package io.helidon.example.grpc.server;

import io.helidon.logging.common.LogConfig;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.grpc.GrpcRouting;

public class GrpcServerMain {

    public static void main(String[] args) {
        LogConfig.configureRuntime();

        WebServer server = WebServer.builder()
                .port(8080)
                .addRouting(GrpcRouting.builder()
                        .intercept(new ApiKeyInterceptor())
                        .service(new EchoService()))
                .build()
                .start();

        System.out.println("gRPC server started at http://localhost:" + server.port());
    }
}
