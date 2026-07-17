# ADR 0005 - Akai Fire Mode Responsibility Boundaries

## Context

- Recent Akai Fire refactors split several large mode classes into named collaborators.
- The most important repeated problem is not only class size. It is that equivalent responsibilities had different names and locations across modes, increasing the cost of understanding, changing, or borrowing the extension code.
- Poly Step (implemented under the historical `com.oikoaudio.fire.chordstep` package and `ChordStep*` class names) proved the pattern outside generated-pattern modes: it was extracted from live pad surface inheritance into a mode-owned set of collaborators.
- Nested Rhythm and Melodic Step proved complementary parts of the same pattern through pad interaction collaborators, generated/editable pattern state, and clip writer collaborators.
- Drum XOX, Perform, Fugue, and remaining live pad surfaces do not all fit the same domain model, but they should still use the same vocabulary when they own equivalent responsibilities.

## Decision

Adopt a shared Akai Fire mode-boundary vocabulary and apply it incrementally. The names below are grouped by the kind of responsibility they describe.

### Mode Shell And Activation

- `Mode` owns lifecycle and high-level composition. It should make mode activation, collaborators, and top-level services easy to find.
- A top-level hardware mode should not be implemented as a thin wrapper around another layer that represents the same mode. Chord Step is its own top-level mode under its own physical button, not a live-note sub-surface.
- The former `ChordStepSurfaceLayer` was a transitional extraction artifact, not the target architecture. It removed obsolete live-note inheritance first, then disappeared once activation, binding, and collaborator composition moved into `ChordStepMode` plus direct pad, button, encoder, clip, observation, and edit owners. Forwarding-only `ChordStepSurfaceController`, `ChordStepController`, and `ChordStepPadControls` were also transitional rather than durable boundaries.
- Use an internal Bitwig `Layer` only when there is a real activation scope to model, not to work around a large mode class.

### Physical Control Boundary

- `ControlBindings` / `PhysicalControls` owns the physical controller boundary for a mode: pads, encoders, the main/select knob, mode buttons, bank buttons, mute buttons, and other hardware inputs/lights that need binding to the mode's Bitwig `Layer`.
- A mode-package `ControlBindings` class can look generic while still being mode-specific. Keep the hardware inventory, binding idioms, and reusable button-group mechanics centralized enough that new modes configure known controls rather than reimplementing raw bindings. Keep the mode's semantic host contract in the mode package when it names that mode's gestures, button meanings, light policies, or activation lifecycle.
- Promote shared binding helpers for repeated physical-control mechanics or repeated configurable button groups. Do not promote a mode's semantic button contract merely because another mode uses the same physical buttons with different meanings.
- Shared physical-control groups are acceptable when they describe the controller shape rather than a mode meaning, for example the pad matrix, bank-button pair, or an indexed button row.
- `PadController` owns semantic pad-region routing; `PadSurface` may own held-pad interaction state; `PadLightRenderer` owns feedback. Do not add a `PadControls` facade when it merely forwards among those owners.
- `Layout` denotes immutable structural mapping data. `EncoderControls` denotes semantic encoder turn/touch behavior and its input state. Shared physical binding/lifecycle remains in `StepSequencerEncoderLayer`.
- Drum XOX, Melodic Step, Chord Step, and Nested Rhythm construct `encoderBankLayout` before `encoderLayer`, pass the completed layout explicitly, and activate/deactivate the layer in the owning mode. A shared-layer constructor must never query a partially constructed host for the layout it needs.
- `ButtonControls` owns mode-specific button gestures when a group has cohesive policy. Chord Step uses one stateless `ChordStepModeButtons` owner for STEP, PATTERN, and pitch context, while the stateful BANK and accent sessions remain separate.
- Velocity center, velocity sensitivity, and raw-velocity blending are shared control policy when a mode maps pad input velocity through encoder-adjustable targets. Modes that use fixed velocity, generated velocity, or accent-only velocity may keep narrower mode-local policies.

### Interaction State And Routing

- `PadController` owns pad gesture interpretation and delegates musical edits to mode-specific hosts or controllers.
- `PadInteractionState` owns held-pad state, gesture consumption, recurrence-row hold state, and other pure pad-session state. Existing classes named `*PadSurface` often combine some of these responsibilities; keep them where useful, but prefer the more explicit names when making new splits.
- `Controller` and `Controls` names are allowed for smaller interaction owners when a more specific name would be forced. Prefer concrete names such as `ChordStepAccentControls` or `ChordStepBankButtonControls` over generic routers.

### Musical And Clip State

- `PatternState` owns editable/current/base/observed pattern state when a mode has a step-pattern model. Use `EditablePattern` when the model is generated events plus local overlays.
- `ClipWriter` owns writing a mode-owned pattern model to a Bitwig clip and any pending writeback patching. Use `ClipController` when the collaborator owns selected clip availability, clip creation, navigation, observation coordination, or direct clip editing workflows.
- `ObservationController` owns Bitwig note/clip observers, observed caches, delayed refreshes, and conversion into focused read models.

### Presentation

- `Presenter`, `Labels`, or `Display` helpers own reusable formatting for OLED/popup values only after equivalent display semantics have been compared across modes.

These names describe responsibilities, not a framework. Do not create empty classes just to satisfy the vocabulary, and do not force direct-edit modes into generated-pattern abstractions.

## Status

Accepted - Chord Step pilot and step-style encoder initialization rule validated; broader adoption remains incremental.

## Consequences

- New mode refactors should first identify which responsibility slice is moving, then choose the matching name from this vocabulary.
- Existing classes do not need cosmetic renames when their current names are accurate, but stale names that preserve obsolete coupling are valid refactor targets.
- Chord Step is now the reference for removing cross-mode inheritance coupling: sequencing behavior should live in the mode package that owns it.
- `ChordStepMode` is now the top-level mode implementation rather than a shell around `ChordStepSurfaceLayer`; do not copy the removed wrapper-layer pattern.
- Chord Step is also the reference for consolidation after extraction: retain deterministic models, renderers,
  stateful sessions, and Bitwig edges, but remove one-caller forwarding layers that must always be read alongside
  their delegates. Its implementation package has one public entry point and package-private collaborators.
- Nested Rhythm is the reference for generated editable patterns and clip-write pending patching.
- Melodic Step is the reference for current/base pattern state and generated-pattern clip writing; its pitch-pool,
  encoder layout, pad-surface, and selected-clip responsibilities now have named collaborators.
- Drum XOX demonstrates the vocabulary for simple direct-step editing through `DrumStepPadSurface` without forcing
  it into a generated-pattern model.
- Perform demonstrates that page state, observation state, rendering, navigation, mixing, and encoder policy can be
  separated without turning the mode vocabulary into a generic framework. Fugue and live Step Input use narrower
  collaborators where their workflows do not need the full generated-pattern shape.
- Shared helpers should emerge from repeated behavior in at least two real modes. Small behavior-specific helpers such as recurrence-row interaction are preferred over generic mode routers.
- ADR 0002 remains the Launch Control XL controller-layer separation decision. This ADR records the later Akai Fire mode-level boundary vocabulary.
