# ADR 0007 - Sequencer Pattern And Grid Gesture Grammar

## Context

Akai Fire sequencers had assigned the same physical controls to different kinds of timing changes. In particular, some modes used the GRID buttons to page a view, some rewrote notes, and some moved a clip's play start. This made equivalent gestures difficult to predict and made shared-clip modes unsafe to align mechanically.

## Decision

- `PATTERN UP/DOWN` navigates or transforms the current pattern context. When a mode has both time and scene paging, plain `PATTERN` pages time and `SHIFT + PATTERN` pages scenes.
- `GRID LEFT/RIGHT` rotates the current musical target.
- When the target is a whole clip, rotation changes Bitwig's clip play start inside its loop window and does not rewrite its notes.
- When the target is a subtarget sharing a clip with other material, such as a Drum XOX lane or Fugue derived line, rotation changes only that target's notes or line offset.
- `SHIFT + GRID` selects a finer timing increment when the target supports it.
- Holding steps while pressing `GRID` gives those explicit steps priority over whole-target rotation.
- `ALT + GRID LEFT/RIGHT` halves or doubles clip length in sequencer modes that expose relative length changes. Mode-specific `SHIFT + ALT` gestures remain explicit exceptions.
- User-facing text calls the physical buttons `GRID LEFT/RIGHT`; historical `bank` names may remain at hardware-binding boundaries in code.

## Consequences

- Paging changes only what the controller displays; clip rotation changes playback phase; event rotation changes musical content or a derived-line offset.
- Melo Gen and a Fugue source line move clip play start. A Fugue derived line retains its independent line offset.
- Multiclip uses `PATTERN` for its 32-step time pages, `SHIFT + PATTERN` for scene pages, and `GRID` for the active Lane Clip's play start. Held-step and whole-lane fine nudges remain explicit note-editing gestures.
- Modes must provide visible shifted-play-start feedback when their pad layout can represent it.

## Status

Accepted — applied to the Akai Fire sequencer gesture mappings.
