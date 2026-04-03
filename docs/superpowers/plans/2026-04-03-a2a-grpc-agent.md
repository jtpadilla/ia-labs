# A2A gRPC Agent Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Añadir la implementación gRPC de `A2AService` al sandbox existente, en el mismo puerto 8080 que el HTTP well-known endpoint, con `sendMessage` (echo) y `getExtendedAgentCard` funcionales.

**Architecture:** `A2AServiceImpl` extiende `A2AServiceGrpc.A2AServiceImplBase`; `sendMessage` hace echo del texto del mensaje entrante; `getExtendedAgentCard` devuelve el `AgentCard` hardcodeado. `Main` añade un segundo `GrpcRouting` al `WebServer` ya existente en el puerto 8080.

**Tech Stack:** Helidon SE 4.4.0 (`GrpcRouting`, `WebServer`), gRPC Java (`A2AServiceGrpc`, `StreamObserver`), Proto `A2AService` de `//proto/lf/a2a/v1`, Bazel 8.x (Bzlmod).

---

## File Map

| Acción | Archivo |
|---|---|
| Modify | `java/a2a/base/sandbox/BUILD.bazel` |
| Create | `java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/A2AServiceImpl.java` |
| Modify | `java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/Main.java` |

---

### Task 1: Añadir deps gRPC al BUILD.bazel

**Files:**
- Modify: `java/a2a/base/sandbox/BUILD.bazel`

El BUILD.bazel actual carece de `a2a_java_grpc`, `helidon-webserver-grpc`, `grpc-api` y `grpc-stub`. Sin estas deps el compilador no encontrará `A2AServiceGrpc`, `GrpcRouting` ni `StreamObserver`.

- [ ] **Step 1: Reemplazar el contenido de BUILD.bazel**

```python
load("@rules_java//java:defs.bzl", "java_binary")

package(default_visibility = ["//visibility:public"])

java_binary(
    name = "sandbox",
    srcs = glob(["src/main/java/**/*.java"]),
    main_class = "io.github.jtpadilla.a2a.sandbox.Main",
    deps = [
        "//proto/lf/a2a/v1:a2a_java_grpc",
        "//proto/lf/a2a/v1:a2a_java_proto",
        "@maven//:io_grpc_grpc_api",
        "@maven//:io_grpc_grpc_stub",
        "@maven//:io_helidon_http_helidon_http",
        "@maven//:io_helidon_webserver_helidon_webserver",
        "@maven//:io_helidon_webserver_helidon_webserver_grpc",
        "@protobuf//:protobuf_java_util",
    ],
)
```

> `a2a_java_grpc` provee `A2AServiceGrpc.A2AServiceImplBase` y el descriptor del servicio que Helidon usa para registrar el routing gRPC.  
> `helidon-webserver-grpc` provee `GrpcRouting`.  
> `grpc-stub` provee `StreamObserver`.

- [ ] **Step 2: Commit**

```bash
git add java/a2a/base/sandbox/BUILD.bazel
git commit -m "build(sandbox): add grpc deps for A2AService implementation"
```

---

### Task 2: Crear `A2AServiceImpl`

**Files:**
- Create: `java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/A2AServiceImpl.java`

Implementación de `A2AServiceGrpc.A2AServiceImplBase`. Recibe el `AgentCard` en el constructor. Implementa `sendMessage` (echo del primer `Part` de texto) y `getExtendedAgentCard`. El resto de RPCs hereda `super.xxx()` que ya envía `Status.UNIMPLEMENTED`.

- [ ] **Step 1: Crear el archivo**

```java
package io.github.jtpadilla.a2a.sandbox;

import com.google.lf.a2a.v1.A2AServiceGrpc;
import com.google.lf.a2a.v1.AgentCard;
import com.google.lf.a2a.v1.GetExtendedAgentCardRequest;
import com.google.lf.a2a.v1.Message;
import com.google.lf.a2a.v1.Part;
import com.google.lf.a2a.v1.Role;
import com.google.lf.a2a.v1.SendMessageRequest;
import com.google.lf.a2a.v1.SendMessageResponse;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

public class A2AServiceImpl extends A2AServiceGrpc.A2AServiceImplBase {

    private final AgentCard agentCard;

    public A2AServiceImpl(AgentCard agentCard) {
        this.agentCard = agentCard;
    }

    @Override
    public void sendMessage(SendMessageRequest request, StreamObserver<SendMessageResponse> responseObserver) {
        String text = request.getMessage().getPartsList().stream()
                .filter(Part::hasText)
                .map(Part::getText)
                .findFirst()
                .orElse("");

        Message echoMessage = Message.newBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setContextId(request.getMessage().getContextId())
                .setRole(Role.ROLE_AGENT)
                .addParts(Part.newBuilder().setText(text).build())
                .build();

        responseObserver.onNext(SendMessageResponse.newBuilder()
                .setMessage(echoMessage)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getExtendedAgentCard(GetExtendedAgentCardRequest request, StreamObserver<AgentCard> responseObserver) {
        responseObserver.onNext(agentCard);
        responseObserver.onCompleted();
    }
}
```

> `Part::hasText` funciona porque `Part` tiene un campo `oneof content { string text = 1; ... }` en proto3: el compilador genera `hasText()` para campos `oneof`.
>
> Si el mensaje entrante no tiene ningún `Part` de tipo texto, el echo devuelve `""`. Comportamiento explícito para el skeleton.

- [ ] **Step 2: Commit**

```bash
git add java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/A2AServiceImpl.java
git commit -m "feat(sandbox): add A2AServiceImpl with sendMessage echo and getExtendedAgentCard"
```

---

### Task 3: Modificar `Main` para añadir `GrpcRouting`

**Files:**
- Modify: `java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/Main.java`

Añadir `GrpcRouting.builder().service(new A2AServiceImpl(agentCard))` al `WebServer` ya existente. Helidon SE 4 permite múltiples routings en el mismo puerto; el protocolo (HTTP/1.1 vs HTTP/2 gRPC) se negocia por `Content-Type`.

- [ ] **Step 1: Reemplazar el contenido de Main.java**

```java
package io.github.jtpadilla.a2a.sandbox;

import com.google.lf.a2a.v1.AgentCard;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.grpc.GrpcRouting;
import io.helidon.webserver.http.HttpRouting;

public class Main {

    public static void main(String[] args) {
        AgentCard agentCard = AgentCardFactory.create();

        WebServer server = WebServer.builder()
                .port(8080)
                .addRouting(HttpRouting.builder()
                        .register("/.well-known", new WellKnownHandler(agentCard)))
                .addRouting(GrpcRouting.builder()
                        .service(new A2AServiceImpl(agentCard)))
                .build()
                .start();

        System.out.println("A2A sandbox running — port " + server.port());
        System.out.println("Agent card : http://localhost:" + server.port() + "/.well-known/agent.json");
        System.out.println("gRPC       : lf.a2a.v1.A2AService on port " + server.port());
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/Main.java
git commit -m "feat(sandbox): wire A2AServiceImpl into WebServer via GrpcRouting"
```

---

### Task 4: Smoke test manual

Verificar que el servidor arranca y que el endpoint gRPC responde. Este paso lo ejecuta el usuario bajo petición explícita (ver CLAUDE.md: no ejecutar Bazel proactivamente).

- [ ] **Step 1: Arrancar el servidor**

```bash
bazel run //java/a2a/base/sandbox:sandbox
```

Salida esperada:
```
A2A sandbox running — port 8080
Agent card : http://localhost:8080/.well-known/agent.json
gRPC       : lf.a2a.v1.A2AService on port 8080
```

- [ ] **Step 2: Verificar el well-known endpoint (sigue funcionando)**

```bash
curl -s http://localhost:8080/.well-known/agent.json | python3 -m json.tool
```

Salida esperada: JSON con `name: "Sandbox Agent"`, `protocolBinding: "GRPC"`.

- [ ] **Step 3: Verificar el endpoint gRPC con grpcurl (si disponible)**

```bash
# Listar servicios disponibles
grpcurl -plaintext localhost:8080 list

# Llamar a GetExtendedAgentCard
grpcurl -plaintext -d '{}' localhost:8080 lf.a2a.v1.A2AService/GetExtendedAgentCard

# Llamar a SendMessage con eco
grpcurl -plaintext \
  -d '{
    "message": {
      "message_id": "test-1",
      "role": "ROLE_USER",
      "parts": [{"text": "hola mundo"}]
    }
  }' \
  localhost:8080 lf.a2a.v1.A2AService/SendMessage
```

Salida esperada de `SendMessage`:
```json
{
  "message": {
    "messageId": "<uuid>",
    "role": "ROLE_AGENT",
    "parts": [{"text": "hola mundo"}]
  }
}
```

- [ ] **Step 4: Parar el servidor**

`Ctrl+C` en el terminal donde corre Bazel.
