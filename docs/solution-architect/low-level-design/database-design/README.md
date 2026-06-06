# Physical Database Design

## Purpose

This folder contains the low-level physical database design for the AI Training Management Platform MVP. It expands the logical ERD into PostgreSQL-oriented table structures, constraints, indexes, and operational rules.

## Files

| File | Description |
| --- | --- |
| [physical-erd.puml](./physical-erd.puml) | PlantUML physical ERD with PostgreSQL tables, columns, keys, and relationships |
| [physical-schema-design.md](./physical-schema-design.md) | Detailed schema notes, enum definitions, constraints, indexes, and migration guidance |

## Render Command

PlantUML CLI is not currently installed in this workspace. When available:

```bash
rtk plantuml -tsvg docs/sa/LLD/database-design/physical-erd.puml
```

The `.puml` source is the canonical docs-as-code artifact.
