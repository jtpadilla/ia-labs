# A2A Well-Known URI Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Añadir un servidor HTTP Helidon SE ejecutable al paquete `//java/a2a/base/sandbox` que sirva un `AgentCard` proto hardcodeado en `GET /.well-known/agent.json` según el spec de agent discovery A2A.

**Architecture:** `AgentCardFactory` construye el `AgentCard` proto con valores fijos; `WellKnownHandler` lo serializa a JSON con `protobuf-java-util` y lo sirve via Helidon SE `HttpService`; `Main` ensambla el `WebServer` en el puerto 8080.

**Tech Stack:** Helidon SE 4.4.0 (`WebServer`, `HttpRouting`, `HttpService`), Protocol Buffers (`AgentCard` de `//proto/lf/a2a/v1`), `protobuf-java-util` (`JsonFormat`), Bazel 8.x (Bzlmod).

---

## File Map

| Acción | Archivo |
|---|---|
| Modify | `MODULE.bazel` |
| Modify | `java/a2a/base/sandbox/BUILD.bazel` |
| Create | `java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/AgentCardFactory.java` |
| Create | `java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/WellKnownHandler.java` |
| Create | `java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/Main.java` |

---

### Task 1: Añadir `protobuf-java-util` a MODULE.bazel

**Files:**
- Modify: `MODULE.bazel`

`JsonFormat.printer()` está en `com.google.protobuf:protobuf-java-util`, que no está listado en el maven.install actual.

- [ ] **Step 1: Añadir el artefacto Maven**

En `MODULE.bazel`, dentro del bloque `maven.install(artifacts = [...])`, añadir justo después de la línea `## a2a`:

```
        "com.google.protobuf:protobuf-java-util:4.28.3",
```

El bloque queda así (extracto):

```
        ## a2a
        "com.google.code.gson:gson:2.13.2",
        "com.google.protobuf:protobuf-java-util:4.28.3",
        "org.slf4j:slf4j-api:2.0.7",
```

> **Nota:** si al compilar aparece un error de versión en conflicto con `protobuf-java`, ajustar este número para que coincida con la versión ya resuelta de `com.google.protobuf:protobuf-java` en el grafo de dependencias.

- [ ] **Step 2: Commit**

```bash
git add MODULE.bazel
git commit -m "deps: add protobuf-java-util for JsonFormat in sandbox"
```

---

### Task 2: Convertir BUILD.bazel a `java_binary`

**Files:**
- Modify: `java/a2a/base/sandbox/BUILD.bazel`

El target actual es `java_library`. Hay que convertirlo a `java_binary` y añadir las deps de Helidon y protobuf-java-util.

- [ ] **Step 1: Reemplazar el contenido de BUILD.bazel**

```python
load("@rules_java//java:defs.bzl", "java_binary")

package(default_visibility = ["//visibility:public"])

java_binary(
    name = "sandbox",
    srcs = glob(["src/main/java/**/*.java"]),
    main_class = "io.github.jtpadilla.a2a.sandbox.Main",
    deps = [
        "//proto/lf/a2a/v1:a2a_java_proto",
        "@maven//:io_helidon_webserver_helidon_webserver",
        "@maven//:com_google_protobuf_protobuf_java_util",
    ],
)
```

> `a2a_java_grpc` y los deps de gRPC se eliminan porque este binario solo usa el servidor HTTP, no gRPC. Los mensajes proto (`AgentCard`, etc.) los provee `a2a_java_proto`.

- [ ] **Step 2: Commit**

```bash
git add java/a2a/base/sandbox/BUILD.bazel
git commit -m "build: convert sandbox to java_binary with helidon-webserver dep"
```

---

### Task 3: Implementar `AgentCardFactory`

**Files:**
- Create: `java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/AgentCardFactory.java`

Construye el `AgentCard` proto con valores de ejemplo hardcodeados.

- [ ] **Step 1: Crear el archivo**

```java
package io.github.jtpadilla.a2a.sandbox;

import com.google.lf.a2a.v1.AgentCard;
import com.google.lf.a2a.v1.AgentCapabilities;
import com.google.lf.a2a.v1.AgentInterface;
import com.google.lf.a2a.v1.AgentSkill;

public final class AgentCardFactory {

    private AgentCardFactory() {}

    public static AgentCard create() {
        return AgentCard.newBuilder()
                .setName("Sandbox Agent")
                .setDescription("A2A sandbox agent")
                .setVersion("0.0.1")
                .addSupportedInterfaces(AgentInterface.newBuilder()
                        .setUrl("http://localhost:8080")
                        .setProtocolBinding("GRPC")
                        .setProtocolVersion("0.3")
                        .build())
                .setCapabilities(AgentCapabilities.newBuilder().build())
                .addDefaultInputModes("text/plain")
                .addDefaultOutputModes("text/plain")
                .addSkills(AgentSkill.newBuilder()
                        .setId("echo")
                        .setName("Echo")
                        .setDescription("Echoes input")
                        .addTags("echo")
                        .build())
                .build();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/AgentCardFactory.java
git commit -m "feat(sandbox): add AgentCardFactory with hardcoded AgentCard"
```

---

### Task 4: Implementar `WellKnownHandler`

**Files:**
- Create: `java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/WellKnownHandler.java`

`HttpService` de Helidon SE 4 que registra `GET /.well-known/agent.json` y serializa el `AgentCard` con `JsonFormat`.

- [ ] **Step 1: Crear el archivo**

```java
package io.github.jtpadilla.a2a.sandbox;

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
                    .includingDefaultValueFields()
                    .print(agentCard);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize AgentCard at startup", e);
        }
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/.well-known/agent.json", this::handle);
    }

    private void handle(ServerRequest req, ServerResponse res) {
        res.headers().add(
                io.helidon.http.HeaderNames.CONTENT_TYPE,
                "application/json");
        res.send(agentCardJson);
    }
}
```

> La serialización se hace una sola vez en el constructor (el `AgentCard` es inmutable y hardcodeado). El resultado se guarda en `agentCardJson` para no repetir el trabajo por cada request.
>
> `includingDefaultValueFields()` garantiza que campos con valor proto-default (false, 0, "") aparecen en el JSON, lo que es más informativo para un consumidor A2A.

- [ ] **Step 2: Commit**

```bash
git add java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/WellKnownHandler.java
git commit -m "feat(sandbox): add WellKnownHandler serving /.well-known/agent.json"
```

---

### Task 5: Implementar `Main`

**Files:**
- Create: `java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/Main.java`

Ensambla `AgentCardFactory` + `WellKnownHandler` y arranca el `WebServer` de Helidon SE en el puerto 8080.

- [ ] **Step 1: Crear el archivo**

```java
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
```

- [ ] **Step 2: Commit**

```bash
git add java/a2a/base/sandbox/src/main/java/io/github/jtpadilla/a2a/sandbox/Main.java
git commit -m "feat(sandbox): add Main wiring Helidon WebServer with WellKnownHandler"
```

---

### Task 6: Smoke test manual

Verificar que el servidor arranca y devuelve el `AgentCard` correcto.

- [ ] **Step 1: Arrancar el servidor** (bajo petición explícita del usuario — ver CLAUDE.md)

```bash
bazel run //java/a2a/base/sandbox:sandbox
```

Salida esperada en stdout:
```
A2A sandbox running — port 8080
Agent card: http://localhost:8080/.well-known/agent.json
```

- [ ] **Step 2: Verificar el endpoint en otro terminal**

```bash
curl -s http://localhost:8080/.well-known/agent.json | python3 -m json.tool
```

Salida esperada (extracto):
```json
{
  "name": "Sandbox Agent",
  "description": "A2A sandbox agent",
  "version": "0.0.1",
  "supportedInterfaces": [
    {
      "url": "http://localhost:8080",
      "protocolBinding": "GRPC",
      "protocolVersion": "0.3"
    }
  ],
  "skills": [
    {
      "id": "echo",
      "name": "Echo",
      "description": "Echoes input",
      "tags": ["echo"]
    }
  ]
}
```

- [ ] **Step 3: Parar el servidor**

`Ctrl+C` en el terminal donde corre Bazel.
