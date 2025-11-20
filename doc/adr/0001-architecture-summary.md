# ADR 0001 — Architecture Summary for v1.0

## Context

- We start from Bitwig’s Launch Control XL Mk2 sources (MIT licence) because the factory modes expose flexible mixer/device layers (track remotes, track focus, etc.) that we want to preserve while adding user-mode capabilities. We copy Bitwig’s `framework`, `controllers/novation/common`, and `util` packages into this repo so the build is self-contained.
- rhbitwig’s Richie Hawtin arp (MIT licence) provides the behaviour for User Template 8.
- Hosts must run Bitwig Studio 6.0+ (API 25). Bitwig 6 itself runs on JDK 21, so we build against Java 21 to stay aligned with the shipping runtime while keeping an LTS toolchain.
- The Launch Control XL exposes eight factory templates (MIDI channels 8–15) and eight user templates (channels 0–7). Bitwig’s stock script consumes all channels, preventing user mappings.

## Decision

- Keep factory templates fully intact: when a factory template (ID 8–15) is active, the classic Bitwig layers are enabled and matcher assignments are attached.
- Unlock user templates 1–7 for raw MIDI pass-through by creating a non-consuming NoteInput (channels 0–6 masks) and clearing all hardware matchers when a user template is selected.
- Dedicate User Template 8 (channel 7) to the arp workflow:
  - Clear factory layers but keep the NoteInput filtered (channel 7 excluded) so arp events never reach track inputs.
  - Instantiate `RhArpLayerController` to drive the Bitwig Arpeggiator device, route all knobs/sliders/buttons to the corresponding parameters, and mirror rhbitwig’s LED scheme.
  - Track Device/Mute/Solo/Record buttons as layer toggles (timing, pattern, velocity/gate, quantize). LED colours indicate which layer is active.
- Maintain state via Sysex template-change messages: factory channels use the stock Mode enumeration; user templates update our current slot, enabling/disabling the proper layers.

## Status

Accepted – implemented in v1.0.0.

## Consequences

- Users retain all factory functionality while gaining user-mode mappings and a fully integrated arp template.
- The project must continue shipping the MIT-licensed Bitwig helpers and rhbitwig-derived classes, so the repo bundles the relevant source trees and carries a combined MIT licence.
- Future enhancements (e.g., alternative user template workflows) should add new ADRs referencing this baseline.
