# ADR 0006 - Multiclip Seq Context Ownership

## Context

Multiclip Seq edits launcher clips on direct child tracks while the parent group hosts their shared receiving instrument. Bitwig's controller API exposes singular clip selection and cannot reconstruct an additive multi-clip Detail Editor selection. It also does not expose child-track note-output destinations, so routing remains a documented project contract.

## Decision

Multiclip Seq keeps the parent and lane identities separate:

- a named, pinned parent cursor owns only the PolySeq group and its direct-child TrackBank;
- the selected group or nearest parent group is authoritative when it has a MIDI-capable direct child; otherwise a paged flat project bank finds exactly one group marked `PolySeq` (case- and separator-insensitive), reporting no or multiple matches instead of guessing;
- row 1 launches a child-only project scene and follows it as the editing scene; `ALT` or the select modifier chooses an editing scene without launching it, row 2 selects one of sixteen direct child tracks, and rows 3-4 edit 32 steps of that one child clip;
- the ordinary selected-track cursor follows the active child track and owns one unpinned launcher cursor clip;
- targeting selects the exact child `ClipLauncherSlot`, then enables edits only after the selected-track position and cursor-clip scene match;
- selecting a nonexistent scene appends empty project scenes with `Project.createScene()`; it does not create a clip on any track;
- the first inserted step creates a clip only in the exact active child slot;
- direct child position, absolute scene, and a target generation identify every delayed write;
- the active Track Lane is the active Bitwig child track;
- a group-rooted first-instrument cursor owns corresponding Drum Machine pad mixer parameters when available, then corresponding materialized device/output-chain mixer parameters when exposed by Bitwig; the active child clip exposes note objects to the shared Drum XOX encoder pages, missing mixer channels remain unmapped, and remote-page navigation falls back to the group instrument;
- copy/paste uses the current editing scene and Track Lane as a live source and targets child slots directly; it has no captured buffer and never includes the group slot;
- GUI presentation selects only the active child Track Lane and its Lane Clip; group scene/sub-scene selection is never used as an editing target.

Fire sequencing never depends on additive Bitwig clip selection or on the order in which GUI clips were selected.

## Consequences

- Every child clip retains independent notes, loop values, and play start, but Fire observes one child clip at a time.
- Changing the lane or scene deliberately changes Bitwig's selected track and selected child clip once.
- Launching a scene follows it as the editing scene, while `ALT` or the select modifier can target a different non-playing scene for preparation.
- Scene, lane, and time retargeting invalidates delayed clip-creation work.
- Track and clip names remain descriptive and positional child order is authoritative; only the parent group's optional `PolySeq` marker participates in fallback discovery.
- The Fire pattern and Bitwig Detail Editor show the same active Lane Clip.
- Runtime cursor behavior and repaint load require physical Bitwig/controller smoke testing after automated integration.

## Status

Accepted — implemented by Multiclip Seq against Bitwig API 25.
