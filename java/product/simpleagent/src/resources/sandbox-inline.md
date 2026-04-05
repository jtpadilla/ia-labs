---
kind: remote
name: a2a-sandbox
agent_card_json: |
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
    "capabilities": {},
    "defaultInputModes": ["text/plain"],
    "defaultOutputModes": ["text/plain"],
    "skills": [
      {
        "id": "echo",
        "name": "Echo",
        "description": "Echoes input",
        "tags": ["echo"]
      }
    ]
  }
---

A2A sandbox agent local (Helidon gRPC, puerto 8080).
La agent card está incrustada: no es necesario que el servidor esté arriba para registrar el agente.