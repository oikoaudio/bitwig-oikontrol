# ADR 0003 — Init Order and Layer Lifecycle

## Context

- The Launch Control XL sends a template-change sysex immediately after Bitwig loads the extension. If we try to bind hardware before controls or the binding manager exist, all matchers stay null and every control appears dead.
- We now have clear roles: the orchestrator (`LaunchControlXlControllerExtension`), adapters (e.g., `HardwareBindingManager`, `HostNotifications`), and mode controllers (`DrumLayerController`, `RhArpLayerController`), plus pure helpers for rendering/logic (`DrumLedRenderer`, `DrumUiState`, `AccentModeLogic`, `PadNoteMapper`).
- Future changes (new modes, different user-template slots, relaxed note filters) need to respect this lifecycle so bindings and LEDs remain intact.

## Init Order (what happens, in order)

1. Create host handles and MIDI ports (`mMidiIn`, `mMidiOut`), build preferences (`DrumSettings`), and register sysex + MIDI callbacks.
2. Initialize mode state (factory mode sysex send) and set up cursor track/device, track banks, send banks, and remote controls (project, per-track, per-pad).
3. Build hardware surface objects (knobs, sliders, buttons) and assign note/CC numbers.
4. **Construct `HardwareBindingManager` after hardware exists**, mark `mHardwareReady = true`, and call `attachHardwareMatchers()`. If a template sysex arrived earlier, `mPendingAttach` ensures we re-run attach now.
5. Create layers (main, mode layers, track control layers, device layer, drum layer), then activate main layer and initial mode.

## Template Handling and Layer Activation

- Incoming sysex is parsed via `TemplateChangeMessageParser`:
  - Factory templates (IDs 8–15) set `mFactoryTemplateActive=true`, select the matching `Mode`, enable factory layers, and call `attachHardwareMatchers()` with the factory channel.
- User template 7 (drum) enables `DrumLayerController`, rebinds matchers to drum CC/note maps, and disables factory layers. If “Auto-attach…” is enabled, it uses `DeviceLocator` to focus the first drum machine (cached lookup) and select its device on the cursor; no pinning needed because the drum layer tolerates cursor changes.
- User template 8 (arp) enables `RhArpLayerController`, pins the cursor device to the arp, and clears factory/drum bindings. With “Auto-attach…” on, it also uses `DeviceLocator` (cached) to focus the arp device; pinning keeps the cursor stable while the arp layer runs.
- If sysex arrives before hardware is ready, `attachHardwareMatchers()` defers via `mPendingAttach` and runs as soon as hardware + manager exist.

## Who Does What (map for future changes)

- **Orchestrator (`LaunchControlXlControllerExtension`)**: decides active layer based on templates, manages mode/state flags, routes incoming MIDI/sysex to drum/arp controllers, paints LEDs for factory modes.
- **Adapters**:
  - `HardwareBindingManager`: owns matcher wiring/clearing for factory vs. drum maps (CCs/notes for knobs, sliders, transport, Device/Mute/Solo/Record).
  - `HostNotifications`: wraps logging and popups.
- **Mode controllers**:
  - `DrumLayerController`: handles drum layer engagement, pad selection/audition, mute/solo modes, pad parameter control, and delegates LED colours to `DrumLedRenderer`.
  - `RhArpLayerController`: owns arp parameter control and button modes for User Template 8.
- **Pure helpers**: `DrumLedRenderer`, `DrumUiState`, `AccentModeLogic`, `PadNoteMapper` (safe to reuse/test in isolation).
- **Discovery/selection**: `DeviceLocator` finds drum/arp devices on tracks; `TemplateChangeMessageParser` parses template sysex IDs.

## Guidance for common changes

- **Move user templates (e.g., to slots 1 and 2):** update `DRUM_USER_TEMPLATE_ID` / `ARP_USER_TEMPLATE_ID`, adjust `UserModeNoteInputInstaller` masks, and ensure `attachHardwareMatchers()` channels match the new IDs. Keep the init ordering intact so bindings apply after hardware exists.
- **Allow arbitrary incoming notes on the track-focus row:** relax the note filters in `HardwareBindingManager.attachFactory/attachDrum` or add a new attach variant; ensure `handleIncomingMidi` still short-circuits to drum/arp controllers for their channels.
- **Add another mode:** extend the `Mode` enum with channel/notification, bind LEDs/knobs in `createModeLayers`, and let `selectModeFromSysex` map the new channel. Keep `HardwareBindingManager` CC mappings consistent and add LED painting if needed.

## Status

Accepted — documents the current lifecycle to avoid regressions when changing template handling or adding modes.
