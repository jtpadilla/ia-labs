# A2A Well-Known URI — Skeleton Design

**Date:** 2026-04-03  
**Package:** `//java/a2a/base/sandbox`  
**Scope:** Executable skeleton that serves `GET /.well-known/agent.json` per the A2A agent discovery spec.

---

## Architecture

Three classes inside `io.github.jtpadilla.a2a.sandbox`:

| Class | Responsibility |
|---|---|
| `Main` | Builds the `AgentCard`, wires `WellKnownHandler`, starts Helidon SE `WebServer` on port 8080 |
| `WellKnownHandler` | Handles `GET /.well-known/agent.json`; serializes `AgentCard` proto to JSON via `JsonFormat.printer()` |
| `AgentCardFactory` | Constructs a hardcoded `AgentCard` proto with placeholder values |

## File Layout

```
java/a2a/base/sandbox/
├── BUILD.bazel
└── src/main/java/io/github/jtpadilla/a2a/sandbox/
    ├── Main.java
    ├── WellKnownHandler.java
    └── AgentCardFactory.java
```

## BUILD.bazel Changes

- Target renamed from `java_library` → `java_binary` with `main_class = "io.github.jtpadilla.a2a.sandbox.Main"`
- Added deps:
  - `@maven//:io_helidon_webserver_helidon_webserver`
  - `@maven//:com_google_protobuf_protobuf_java_util` (for `JsonFormat`)

## Data Flow

1. `Main.main()` calls `AgentCardFactory.create()` → `AgentCard` proto instance
2. `Main` creates `WellKnownHandler(agentCard)` and registers it on `HttpRouting` at `GET /.well-known/agent.json`
3. `Main` starts `WebServer` on port 8080
4. On request, `WellKnownHandler` calls `JsonFormat.printer().print(agentCard)` and writes the result with `Content-Type: application/json`

## Hardcoded AgentCard Values

```
name:                  "Sandbox Agent"
description:           "A2A sandbox agent"
version:               "0.0.1"
default_input_modes:   ["text/plain"]
default_output_modes:  ["text/plain"]
capabilities:          (empty — streaming: false, push_notifications: false)
skills[0]:
  id:          "echo"
  name:        "Echo"
  description: "Echoes input"
  tags:        ["echo"]
```

## Error Handling

- Proto serialization errors are caught in the handler and responded with HTTP 500.
- No additional validation — this is a sandbox skeleton.

## Out of Scope

- Authentication / extended agent card endpoint
- Configurable port or AgentCard values
- Helidon Service Registry integration (intentionally deferred for future refactor)
