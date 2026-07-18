# ADR 0006 - Multiclip Seq Context Ownership

## Context

Multiclip Seq edits four launcher clips on direct child tracks while the parent group hosts the Drum Machine. Bitwig's controller API exposes singular clip and scene selection but no additive operation for reconstructing a multi-clip Detail Editor selection. Child-track selection can also move a selection-following device context away from the parent group.

## Decision

Multiclip Seq keeps the parent and lane identities separate:

- a named, pinned parent cursor owns the Drum Machine group and its direct-child TrackBank;
- four named child-track cursors own the visible Lane Clips;
- direct child position, absolute scene, and a target generation identify every delayed write;
- the active Track Lane is the active Bitwig child track;
- GUI presentation selects only the active child Track Lane and its Lane Clip; group scene/sub-scene selection is never used as an editing target.

Fire sequencing never depends on additive Bitwig clip selection or on the order in which GUI clips were selected.

## Consequences

- Four Lane Clips can retain independent note grids, loop values, play starts, and playheads.
- Scene, lane, and time retargeting must invalidate delayed clip-creation work.
- Track and clip names remain descriptive; positional child order is authoritative.
- The Detail Editor shows the active Lane Clip while Fire continues to edit all four independently pinned child clips.
- Runtime cursor behavior and repaint load require physical Bitwig/controller smoke testing after automated integration.

## Status

Accepted — implemented by Multiclip Seq against Bitwig API 25.
