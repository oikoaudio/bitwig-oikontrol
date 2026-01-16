# ADR 0002 — Separation of Concerns for Controller Layers

## Context

- The main extension class wires hardware matchers, manages template changes, orchestrates drum/arp layers, and paints LEDs. This makes reasoning and testing difficult.
- The arpeggiator folder (`controllers/novation/launch_control_xl/arp`) from rhbitwig demonstrates a cleaner split: a small controller orchestrates behaviour while helpers (button modes, state containers) keep logic pure and testable.
- New drum features (mute/solo modes, accent buttons, pad auditioning, LED painting) and auto-attach behaviour for drum/arp need clearer ownership boundaries to avoid regressions.
- We want an architecture that allows reusing pure behaviour with different hardware by swapping adapters (e.g., another controller like Akai Fire) without duplicating domain logic.

## Decision

Refactor the existing codebase as follows:

- **Orchestration layer:** `LaunchControlXlControllerExtension` stays the conductor. It listens to template changes, decides which layer is active (factory/drum/arp), and hands control to the appropriate mode controller. It does not compute colours or MIDI wiring details.
- **Adapters:** `HardwareBindingManager` owns matcher wiring/clearing for factory vs. drum modes (CC/note bindings for knobs/sliders/buttons). `HostNotifications` wraps popups/logging. These are the host/hardware edges that can be swapped if hardware changes.
- **Settings:** `DrumSettings` encapsulates drum preferences (audition on select, accent momentary) so controllers receive explicit configuration instead of reaching into Host prefs. Future `ArpSettings` can mirror this.
- **Pure render/logic helpers:** (`DrumLedRenderer`, `DrumUiState`, `PadNoteMapper`, `AccentModeLogic`) compute colours, note offsets, and accent behaviour without host dependencies; covered by unit tests.
- **State snapshots:** Controllers hold only minimal live state (selected pad, mode flags) needed to react to input; rendering receives immutable snapshots (`DrumUiState`) so view logic remains pure.
- Keep layer activation and template handling in `LaunchControlXlControllerExtension`, but push mode-specific behaviour into dedicated controllers (drum/arp) modelled after the arp package structure.

## Status

Accepted — refactor in progress and partially implemented (binding manager, drum settings, pure helpers, LED rendering).

## Consequences

- **Modularity/Testability:** Core behaviours (LED colours, pad note mapping, accent handling) can be unit-tested off-host; adding new modes reuses the same patterns.
- **Clear ownership:** Hardware wiring, settings, and UI rendering live in dedicated classes; the main extension focuses on template switches and layer activation.
- **Easier evolution:** Additional settings (e.g., future arp/drum options) can join `DrumSettings`/future `ArpSettings` without touching orchestration code; new LED schemes drop into renderers.
- **Adapter reuse:** Pure helpers are hardware-agnostic; another controller could reuse them by providing its own binding manager and note/CC maps while keeping the orchestrator/controller shape. Full hardware support would still live in a separate extension module, but the domain/rendering pieces can be shared.
- **Migration effort:** Some factory-only logic still lives in the main class; follow-up refactors should extract remaining binding/paint concerns similarly to keep symmetry with the arp module.
