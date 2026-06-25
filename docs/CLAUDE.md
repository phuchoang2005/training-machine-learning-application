# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this directory is

An **Obsidian vault** containing all product, architecture, UX, and delivery documentation for the Future AI Training Management Platform. It is structured as a **Zettelkasten** — atomic notes connected via `[[wikilinks]]` forming a navigable knowledge graph.

Start navigation at `_index.md` (the home MOC). It links to four domain MOCs:
- `requirements-moc.md` — BRS, product requirements, SA clarifications, NFRs
- `architecture-moc.md` — HLD, ADRs (15 atomic files), design patterns, security model
- `design-moc.md` — LLD, database design, API contracts, class/sequence/activity diagrams
- `frontend-moc.md` — Frontend architecture, state flow, UX, UI screens, design system

## Vault structure

```
docs/
├── _index.md                    ← Home MOC (start here)
├── *-moc.md                     ← 4 domain Maps of Content
├── ba-refine.md, po-requirement.md, github-commit-strategy.md
├── solution-architect/
│   ├── adr/                     ← 15 atomic ADR files + adr-index.md
│   ├── high-level-design/
│   │   ├── diagram/             ← 19 Mermaid companion .md files
│   │   ├── access-control-matrix.md
│   │   └── failure-handling-matrix.md
│   ├── low-level-design/
│   │   ├── api-contracts/openapi.yaml   ← canonical API contract
│   │   ├── database-design/
│   │   ├── logic-code-structure/
│   │   │   ├── class-diagram/diagrams/  ← .puml files
│   │   │   ├── sequence-diagram/diagrams/
│   │   │   └── activity-diagram/diagrams/
│   │   ├── low-level-design.md, non-functional-requirements.md
│   ├── md/                      ← narrative docs (sa-refinement, design-patterns, security-model, HLD, ADR monolith)
│   ├── frontend-architecture-document/
│   │   └── diagrams/            ← 5 frontend diagram companion .md files
│   ├── api-integration-and-client-side-caching-strategy/
│   ├── design-system-and-component-specification/
│   └── performance-and-ux-optimization-design/
└── ui-ux/
    ├── ux-and-structure/
    │   └── diagrams/            ← 2 UX diagram companion .md files
    └── ui/
        ├── admin/               ← admin wireframe SVGs + admin-screens.md
        └── user/                ← user wireframe SVGs + user-screens.md
```

## Conventions for every `.md` file in the vault

**Always add YAML frontmatter:**
```yaml
---
title: "Descriptive Title"
tags: [relevant, tags]
aliases: [Short Name, slug-name]
related:
  - "[[other-note]]"
---
```

**Wikilinks over file paths:** Use `[[note-slug]]` (basename without `.md`), never relative paths like `../some/file.md`, inside `related:` and inline text. Obsidian resolves wikilinks by filename across the vault regardless of directory depth.

**Filenames are graph node labels** — use descriptive kebab-case names. Never name files `README.md`; they all resolve to the same "README" node in the graph.

## Diagrams

- **Mermaid** (`.mermaid` files): Each has a companion `.md` in the same directory that embeds the diagram inline in a fenced ` ```mermaid ``` ` block, adds context, and wikilinks to related notes. The companion file is the Obsidian node; the `.mermaid` file is the source.
- **PlantUML** (`.puml` files): Obsidian cannot render PlantUML natively. Companion `.md` files describe the diagram, list the source path, and link related notes. They do **not** embed the PlantUML source inline.
- When adding a new diagram file, always create the companion `.md` in the same directory.

## ADRs

Individual atomic files live in `solution-architect/adr/ADR-NNN.md`. The file `solution-architect/md/architectural-decision-records.md` is preserved as the original monolith for reference but the adr/ atoms are the navigable source of truth. When adding a new decision, create a new `ADR-NNN.md` and update `adr-index.md`.

## API contract

`solution-architect/low-level-design/api-contracts/openapi.yaml` is canonical. Any change to routes, request/response shapes, or status codes must be reflected here first, then propagated to the backend implementation and `frontend/src/shared/api/types/` by hand in the same commit.
