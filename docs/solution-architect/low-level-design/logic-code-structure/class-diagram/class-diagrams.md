# Class Diagrams

## Purpose

This folder contains modular PlantUML class diagrams for the AI Training Management Platform LLD. The diagrams describe the main domain entities, Spring Boot services, API DTO contracts, and WebSocket/event classes.

## Files

| File | Description |
| --- | --- |
| [class-diagrams.puml](./class-diagrams.puml) | Standalone index diagram for the class diagram set |
| [diagrams/01-domain-model-class-diagram.puml](./diagrams/01-domain-model-class-diagram.puml) | Domain entities and persistence relationships |
| [diagrams/02-backend-service-class-diagram.puml](./diagrams/02-backend-service-class-diagram.puml) | Spring Boot controllers, services, workers, repositories, and infrastructure adapters |
| [diagrams/03-api-dto-class-diagram.puml](./diagrams/03-api-dto-class-diagram.puml) | REST request/response DTOs aligned with OpenAPI |
| [diagrams/04-websocket-event-class-diagram.puml](./diagrams/04-websocket-event-class-diagram.puml) | WebSocket message envelopes, payloads, and stream services |

## Render Commands

PlantUML CLI is not currently installed in this workspace. When available, render the index:

```bash
rtk plantuml -tsvg docs/sa/LLD/class-diagram/class-diagrams.puml
```

Render one module:

```bash
rtk plantuml -tsvg docs/sa/LLD/class-diagram/diagrams/01-domain-model-class-diagram.puml
```

The `.puml` files are the canonical docs-as-code artifacts.
