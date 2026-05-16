# Collab Sheets and Board v2.1.3 Placeholder Design

This placeholder exists because `docs/v212-migration-spec.md §18.4.3` explicitly defers Sheets and board CRDT collaboration to v2.1.3 while requiring a v2.1.2 sub-spec record.

## v2.1.2 Boundary

v2.1.2 delivers the shared CRDT infrastructure and Docs editor integration only:

- collaboration WebSocket connection and authentication,
- CRDT update persistence,
- snapshot restore,
- awareness broadcast,
- Docs editor binding.

Sheets and board realtime collaboration are not shipped as active workflows in v2.1.2.

## Deferred Scope

### Sheets

The Sheets integration must define:

- worksheet cell CRDT document shape,
- formula recalculation boundaries after remote edits,
- protected range conflict behavior,
- cursor and selection awareness,
- snapshot and restore compatibility with existing workbook persistence.

### Board

The board integration must define:

- card move CRDT operation shape,
- interaction with persisted lexorank ordering,
- column and swimlane conflict behavior,
- card selection awareness,
- snapshot merge behavior after offline edits.

## Runtime Rules

- No mock success paths.
- No silent fallback.
- v2.1.2 UI must show an explicit unavailable or planned state for Sheets and board realtime collaboration.
- Existing board task move and Sheets formula APIs remain normal non-CRDT workflows.
- Any later v2.1.3 implementation must reuse the v2.1.2 CRDT transport instead of introducing a second realtime stack.

## Verification Entry Criteria

Before v2.1.3 implementation starts, this placeholder must be replaced or extended with:

- protocol message contracts,
- storage and snapshot schema,
- two-tab browser scenarios,
- offline reconnect scenarios,
- failure handling for rejected updates,
- rollback and migration notes.
