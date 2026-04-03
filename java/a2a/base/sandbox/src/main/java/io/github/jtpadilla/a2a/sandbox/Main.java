package io.github.jtpadilla.a2a.sandbox;

import com.google.lf.a2a.v1.AgentCard;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;

public class Main {

    public static void main(String[] args) {
        AgentCard agentCard = AgentCardFactory.create();

        WebServer server = WebServer.builder()
                .port(8080)
                .addRouting(HttpRouting.builder()
                        .register(new WellKnownHandler(agentCard)))
                .build()
                .start();

        System.out.println("A2A sandbox running — port " + server.port());
        System.out.println("Agent card: http://localhost:" + server.port() + "/.well-known/agent.json");
    }
}
