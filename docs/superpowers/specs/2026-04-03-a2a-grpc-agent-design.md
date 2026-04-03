# A2A gRPC Agent Skeleton — Design

**Date:** 2026-04-03  
**Package:** `//java/a2a/base/sandbox`  
**Scope:** Añadir implementación gRPC de `A2AService` al sandbox existente, en el mismo puerto 8080 que el HTTP well-known endpoint.

---

## Architecture

Un archivo nuevo + modificación de `Main` y `BUILD.bazel`:

| Componente | Acción | Responsabilidad |
|---|---|---|
| `A2AServiceImpl` | Nuevo | Extiende `A2AServiceGrpc.A2AServiceImplBase`; implementa `sendMessage` (echo) y `getExtendedAgentCard`; hereda UNIMPLEMENTED para el resto |
| `Main` | Modificar | Añade `GrpcRouting` con `A2AServiceImpl` al `WebServer` existente |
| `BUILD.bazel` | Modificar | Añade deps gRPC al binary existente |
| `AgentCardFactory`, `WellKnownHandler` | Sin cambios | — |

## File Layout

```
java/a2a/base/sandbox/
├── BUILD.bazel
└── src/main/java/io/github/jtpadilla/a2a/sandbox/
    ├── A2AServiceImpl.java   ← nuevo
    ├── Main.java             ← modificado
    ├── AgentCardFactory.java ← sin cambios
    └── WellKnownHandler.java ← sin cambios
```

## BUILD.bazel Changes

Añadir a los `deps` del `java_binary`:

```python
"//proto/lf/a2a/v1:a2a_java_grpc",
"@maven//:io_helidon_webserver_helidon_webserver_grpc",
"@maven//:io_grpc_grpc_api",
"@maven//:io_grpc_grpc_stub",
```

## Data Flow

### `sendMessage` (echo)

1. Extrae el primer `Part` con texto del `request.getMessage().getPartsList()`
2. Construye un `Message` de respuesta:
   - `message_id`: `UUID.randomUUID().toString()`
   - `context_id`: copiado de `request.getMessage().getContextId()`
   - `role`: `ROLE_AGENT`
   - `parts`: un `Part` con el texto extraído (o `""` si no hay texto)
3. Responde con `SendMessageResponse.newBuilder().setMessage(echoMessage).build()`

### `getExtendedAgentCard`

Devuelve directamente el `AgentCard` inyectado en el constructor.

### Resto de RPCs

Heredan `super.xxx()` → envían `Status.UNIMPLEMENTED` al cliente automáticamente.

## Main Changes

```java
AgentCard agentCard = AgentCardFactory.create();

WebServer server = WebServer.builder()
        .port(8080)
        .addRouting(HttpRouting.builder()
                .register("/.well-known", new WellKnownHandler(agentCard)))
        .addRouting(GrpcRouting.builder()
                .service(new A2AServiceImpl(agentCard)))
        .build()
        .start();
```

## AgentCard Update

El `AgentInterface` en `AgentCardFactory` ya declara `protocolBinding: "GRPC"` y `url: "http://localhost:8080"`, que es coherente con la implementación.

## Out of Scope

- Streaming RPCs (`sendStreamingMessage`, `subscribeToTask`)
- Persistencia de tareas (`GetTask`, `ListTasks`, `CancelTask`)
- Push notification configs
- Autenticación / TLS
- Helidon Service Registry
