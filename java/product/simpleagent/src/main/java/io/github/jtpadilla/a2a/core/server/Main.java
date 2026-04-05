package io.github.jtpadilla.a2a.core.server;

import com.google.lf.a2a.v1.A2A;
import com.google.lf.a2a.v1.AgentCard;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.grpc.GrpcReflectionFeature;
import io.helidon.webserver.grpc.GrpcRouting;
import io.helidon.webserver.http.HttpRouting;

public class Main {

    public static void main(String[] args) {

        AgentCard agentCard = AgentCardFactory.create();

        final HttpRouting.Builder httpRouting = HttpRouting.builder()
                .register("/.well-known", new WellKnownHandler(agentCard));

        final GrpcRouting.Builder grpcRouting = GrpcRouting.builder()
                .service(A2A.getDescriptor().getFile(), new A2AServiceImpl(agentCard));

        final WebServer server = WebServer.builder()
                .port(8080)
                .addRouting(httpRouting)
                .addRouting(grpcRouting)
                .addFeature(GrpcReflectionFeature.create(cfg -> cfg.enabled(true)))
                .build()
                .start();

    }
}
