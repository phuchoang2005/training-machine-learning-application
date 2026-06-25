# Physical Database Design

## Purpose

This folder contains the low-level physical database design for the AI Training Management Platform MVP. It expands the logical model into MongoDB collections, document shapes, indexes, and operational rules.

## Files

| File | Description |
| --- | --- |
| [physical-erd.puml](./physical-erd.puml) | PlantUML collection model with document fields, ids, and references |
| [physical-schema-design.md](./physical-schema-design.md) | Detailed collection notes, enum values, indexes, and retention rules |

## Render Command

PlantUML CLI is not currently installed in this workspace. When available:

```bash
rtk plantuml -tsvg docs/sa/LLD/database-design/physical-erd.puml
```

The `.puml` source is the canonical docs-as-code artifact.
