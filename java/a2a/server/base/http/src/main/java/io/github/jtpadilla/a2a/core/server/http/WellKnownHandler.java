package io.github.jtpadilla.a2a.core.server.http;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.github.jtpadilla.a2a.server.service.agentcard.AgentCardService;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

@Service.Singleton
public class WellKnownHandler implements HttpService {

    private final String agentCardJson;

    @Service.Inject
    public WellKnownHandler(AgentCardService agentCardService) {
        try {
            this.agentCardJson = JsonFormat.printer()
                        .alwaysPrintFieldsWithNoPresence()
                        .print(agentCardService.agentCard());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("No se ha podido convertir a JSON el AgentCard", e);
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
