# ADR 0006 - Multiclip Seq Context Ownership

## Context

Multiclip Seq edits launcher clips on direct child tracks while the parent group hosts the Drum Machine. Bitwig's controller API exposes singular clip selection and cannot reconstruct an additive multi-clip Detail Editor selection. Experiments with four independent cursor tracks also proved unable to target child slots reliably: a cursor could remain attached to the group cell or a previously selected clip.

## Decision

Multiclip Seq keeps the parent and lane identities separate:

- a named, pinned parent cursor owns only the Drum Machine group and its direct-child TrackBank;
- row 1 launches a child-only project scene; `ALT` or the select modifier chooses the editing scene, row 2 selects one of sixteen direct child tracks, and rows 3-4 edit 32 steps of that one child clip;
- the ordinary selected-track cursor follows the active child track and owns one unpinned launcher cursor clip;
- targeting selects the exact child `ClipLauncherSlot`, then enables edits only after the selected-track position and cursor-clip scene match;
- selecting a nonexistent scene appends empty project scenes with `Project.createScene()`; it does not create a clip on any track;
- the first inserted step creates a clip only in the exact active child slot;
- direct child position, absolute scene, and a target generation identify every delayed write;
- the active Track Lane is the active Bitwig child track;
- a group-rooted first-instrument cursor owns the corresponding Drum Machine pad mixer parameters, while the active child clip exposes note objects to the shared Drum XOX encoder pages; encoder control therefore does not retarget the selected-child cursor;
- copy/paste uses the current editing scene and Track Lane as a live source and targets child slots directly; it has no captured buffer and never includes the group slot;
- GUI presentation selects only the active child Track Lane and its Lane Clip; group scene/sub-scene selection is never used as an editing target.

Fire sequencing never depends on additive Bitwig clip selection or on the order in which GUI clips were selected.

## Consequences

- Every child clip retains independent notes, loop values, and play start, but Fire observes one child clip at a time.
- Changing the lane or scene deliberately changes Bitwig's selected track and selected child clip once.
- Launching a scene does not change the editing scene; selection remains an explicit operation.
- Scene, lane, and time retargeting invalidates delayed clip-creation work.
- Track and clip names remain descriptive; positional child order is authoritative.
- The Fire pattern and Bitwig Detail Editor show the same active Lane Clip.
- Runtime cursor behavior and repaint load require physical Bitwig/controller smoke testing after automated integration.

## Status

Accepted — implemented by Multiclip Seq against Bitwig API 25.
