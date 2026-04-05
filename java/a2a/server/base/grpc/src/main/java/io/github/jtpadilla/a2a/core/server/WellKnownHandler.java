package io.github.jtpadilla.a2a.core.server;

import com.google.lf.a2a.v1.AgentCard;
import com.google.protobuf.util.JsonFormat;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

public class WellKnownHandler implements HttpService {

    private final String agentCardJson;

    public WellKnownHandler(AgentCard agentCard) {
        try {
            this.agentCardJson = JsonFormat.printer()
                    .alwaysPrintFieldsWithNoPresence()
                    .print(agentCard);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize AgentCard at startup", e);
        }
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/agent.json", this::handle);
    }

    private void handle(ServerRequest req, ServerResponse res) {
        res.headers().add(
                io.helidon.http.HeaderNames.CONTENT_TYPE,
                "application/json");
        res.send(agentCardJson);
    }

}
