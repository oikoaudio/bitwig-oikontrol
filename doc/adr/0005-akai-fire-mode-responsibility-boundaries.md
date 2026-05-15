# ADR 0005 - Akai Fire Mode Responsibility Boundaries

## Context

- Recent Akai Fire refactors split several large mode classes into named collaborators.
- The most important repeated problem is not only class size. It is that equivalent responsibilities had different names and locations across modes, increasing the cost of understanding, changing, or borrowing the extension code.
- Chord Step proved the pattern outside generated-pattern modes: it was extracted from live pad surface inheritance into a chord-step-owned mode and collaborators under `com.oikoaudio.fire.chordstep`.
- Nested Rhythm and Melodic Step proved complementary parts of the same pattern through pad interaction collaborators, generated/editable pattern state, and clip writer collaborators.
- Drum XOX, Perform, Fugue, and remaining live pad surfaces do not all fit the same domain model, but they should still use the same vocabulary when they own equivalent responsibilities.

## Decision

Adopt a shared Akai Fire mode-boundary vocabulary and apply it incrementally. The names below are grouped by the kind of responsibility they describe.

### Mode Shell And Activation

- `Mode` owns lifecycle and high-level composition. It should make mode activation, collaborators, and top-level services easy to find.
- A top-level hardware mode should not be implemented as a thin wrapper around another layer that represents the same mode. Chord Step is its own top-level mode under its own physical button, not a live-note sub-surface.
- The former `ChordStepSurfaceLayer` was a transitional extraction artifact, not the target architecture. It removed obsolete live-note inheritance first, then disappeared once activation, binding, and collaborator composition moved into `ChordStepMode` plus named `ControlBindings`, `PadControls`, `EncoderControls`, `ButtonControls`, clip, observation, and edit collaborators.
- Use an internal Bitwig `Layer` only when there is a real activation scope to model, not to work around a large mode class.

### Physical Control Boundary

- `ControlBindings` / `PhysicalControls` owns the physical controller boundary for a mode: pads, encoders, the main/select knob, mode buttons, bank buttons, mute buttons, and other hardware inputs/lights that need binding to the mode's Bitwig `Layer`.
- A mode-package `ControlBindings` class can look generic while still being mode-specific. Keep the hardware inventory, binding idioms, and reusable button-group mechanics centralized enough that new modes configure known controls rather than reimplementing raw bindings. Keep the mode's semantic host contract in the mode package when it names that mode's gestures, button meanings, light policies, or activation lifecycle.
- Promote shared binding helpers for repeated physical-control mechanics or repeated configurable button groups. Do not promote a mode's semantic button contract merely because another mode uses the same physical buttons with different meanings.
- Shared physical-control groups are acceptable when they describe the controller shape rather than a mode meaning, for example the pad matrix, bank-button pair, or an indexed button row.
- `PadControls` is the pad-specific part of that boundary when the 64-pad matrix is complex enough to name separately: mapping raw pad indexes to regions, routing pad press/release input, and returning pad-light state.
- `EncoderControls` owns mode-specific encoder page construction and encoder turn/touch behavior. Shared encoder binding infrastructure such as `StepSequencerEncoderHandler` remains separate from mode-specific encoder maps.
- `ButtonControls` owns mode-specific button gestures when a group of buttons has a cohesive policy, for example chord-step bank, pattern, accent, step, or pitch-context buttons.
- Velocity center, velocity sensitivity, and raw-velocity blending are shared control policy when a mode maps pad input velocity through encoder-adjustable targets. Modes that use fixed velocity, generated velocity, or accent-only velocity may keep narrower mode-local policies.

### Interaction State And Routing

- `PadController` owns pad gesture interpretation and delegates musical edits to mode-specific hosts or controllers.
- `PadInteractionState` owns held-pad state, gesture consumption, recurrence-row hold state, and other pure pad-session state. Existing classes named `*PadSurface` often combine some of these responsibilities; keep them where useful, but prefer the more explicit names when making new splits.
- `Controller` and `Controls` names are allowed for smaller interaction owners when a more specific name would be forced. Prefer concrete names such as `ChordStepAccentControls`, `ChordStepBankButtonControls`, or `ChordStepPitchContextControls` over generic routers.

### Musical And Clip State

- `PatternState` owns editable/current/base/observed pattern state when a mode has a step-pattern model. Use `EditablePattern` when the model is generated events plus local overlays.
- `ClipWriter` owns writing a mode-owned pattern model to a Bitwig clip and any pending writeback patching. Use `ClipController` when the collaborator owns selected clip availability, clip creation, navigation, observation coordination, or direct clip editing workflows.
- `ObservationController` owns Bitwig note/clip observers, observed caches, delayed refreshes, and conversion into focused read models.

### Presentation

- `Presenter`, `Labels`, or `Display` helpers own reusable formatting for OLED/popup values only after equivalent display semantics have been compared across modes.

These names describe responsibilities, not a framework. Do not create empty classes just to satisfy the vocabulary, and do not force direct-edit modes into generated-pattern abstractions.

## Status

Accepted - incremental adoption in progress.

## Consequences

- New mode refactors should first identify which responsibility slice is moving, then choose the matching name from this vocabulary.
- Existing classes do not need cosmetic renames when their current names are accurate, but stale names that preserve obsolete coupling are valid refactor targets.
- Chord Step is now the reference for removing cross-mode inheritance coupling: sequencing behavior should live in the mode package that owns it.
- `ChordStepMode` is now the top-level mode implementation rather than a shell around `ChordStepSurfaceLayer`; do not copy the removed wrapper-layer pattern.
- Nested Rhythm is the reference for generated editable patterns and clip-write pending patching.
- Melodic Step is the reference for current/base pattern state and generated-pattern clip writing, with pitch-pool and encoder controls still candidates for extraction.
- Drum XOX should be used to check that the vocabulary also works for simple direct-step editing, especially before adding shared abstractions.
- Shared helpers should emerge from repeated behavior in at least two real modes. Small behavior-specific helpers such as recurrence-row interaction are preferred over generic mode routers.
- ADR 0002 remains the Launch Control XL controller-layer separation decision. This ADR records the later Akai Fire mode-level boundary vocabulary.
