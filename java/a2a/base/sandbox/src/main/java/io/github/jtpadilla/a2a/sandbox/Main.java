package io.github.jtpadilla.a2a.sandbox;

import com.google.lf.a2a.v1.AgentCard;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.grpc.GrpcRouting;
import io.helidon.webserver.http.HttpRouting;

public class Main {

    public static void main(String[] args) {

        AgentCard agentCard = AgentCardFactory.create();

        final HttpRouting.Builder httpRouting = HttpRouting.builder()
                .register("/.well-known", new WellKnownHandler(agentCard));

        final GrpcRouting.Builder grpcRouting = GrpcRouting.builder()
                .service(new A2AServiceImpl(agentCard));

        final WebServer server = WebServer.builder()
                .port(8080)
                .addRouting(httpRouting)
                .addRouting(grpcRouting)
                .build()
                .start();

        System.out.println("A2A sandbox running — port " + server.port());
        System.out.println("Agent card : http://localhost:" + server.port() + "/.well-known/agent.json");
        System.out.println("gRPC       : lf.a2a.v1.A2AService on port " + server.port());
    }
}
