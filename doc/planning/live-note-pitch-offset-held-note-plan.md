# Live NOTE Pitch Offset Held-Note Plan

## Summary

Live `NOTE` mode now has a useful musical `Pitch Offset` control, but the
current implementation still applies it through Bitwig's key-translation table.
That is fine for new note mapping, but it is not good enough for held-note
behavior:

- `New Notes Only` does not actually preserve already-held notes
- changing the offset while holding pads can cause sounding notes to drop out
- `Retune Held Notes` cannot be trusted until live-note state is tracked
  explicitly rather than inferred from the current translation table

For now, the `Live Pitch Offset` preference should stay hidden from user
settings, and live `Pitch Offset` should be treated as "new notes use the new
mapping" without promising reliable held-note preservation.

## Goal

Rework live `NOTE` playback so pitch-offset changes operate on explicit
per-pad note state rather than on the current key-translation snapshot.

That should make both intended behaviors reliable:

- `New Notes Only`
- `Retune Held Notes`

## Problem

Current live-note playback mixes two different models:

- pad-to-note routing uses `noteInput.setKeyTranslationTable(...)`
- held-note state is inferred from `heldPads` plus the current layout

That means once the layout or pitch offset changes, the code no longer has a
stable record of what note each held pad originally emitted. The result is:

- note-off may target the wrong pitch
- old notes may be dropped when the translation table changes
- retuning held notes cannot be done deterministically

There is already a hint of the correct direction in `NoteMode.java`
(`modules/akai-fire/src/main/java/com/oikoaudio/fire/note/NoteMode.java`):

- `soundingLiveNotesByPad` exists, but it is not yet the source of truth

## Proposed Implementation

### 1. Make Live NOTE Explicitly Note-Driven

Stop relying on the translation table as the authority for live pad playback.

Instead:

- on pad press, compute the emitted note for that pad from the current layout,
  root, octave, scale mode, and live pitch offset
- send `NOTE_ON` directly
- store the emitted note in `soundingLiveNotesByPad`
- on pad release, send `NOTE_OFF` for the stored emitted note

The translation table can then be reduced or removed for live note triggering,
depending on what Bitwig requires for velocity and expression handling.

### 2. Treat `soundingLiveNotesByPad` as the Source of Truth

`soundingLiveNotesByPad` should record, for every currently held pad:

- pad index
- emitted midi note
- velocity used for note-on

Optional later additions:

- channel / expression snapshot if live expression recording evolves

This map must survive layout and pitch-offset changes.

### 3. Define Reliable Behavior for `New Notes Only`

When pitch offset changes and the behavior is `New Notes Only`:

- do not touch currently sounding notes
- do not emit note-off/note-on for held pads
- only update the mapping used for future pad presses

Result:

- held notes continue sounding at their original pitch
- new notes use the new offset

### 4. Define Reliable Behavior for `Retune Held Notes`

When pitch offset changes and the behavior is `Retune Held Notes`:

- iterate currently held pads
- read each pad's old emitted note from `soundingLiveNotesByPad`
- compute the new emitted note for the same pad under the new offset
- send `NOTE_OFF` for the old note
- send `NOTE_ON` for the new note
- update `soundingLiveNotesByPad`

Velocity should be preserved from the note's stored launch velocity.

This is the intended glissando / reharmonizing performance mode.

### 5. Keep Layout Changes Consistent

Once live playback is explicit, the same state model should be used for:

- root changes
- octave changes
- scale changes
- chromatic / in-key layout toggles

These should follow one of two clear policies:

- preserve held notes, new notes use new mapping
- or intentionally retune held notes

The policy can stay conservative at first, but it should be explicit and
consistent.

## UI / Preference Plan

The `Live Pitch Offset` preference is intentionally hidden for now because the
underlying behavior is not reliable enough yet.

Once the explicit per-pad note model is implemented, reintroduce:

- `Functionalities -> Live Pitch Offset`
  - `New Notes Only`
  - `Retune Held Notes`

## Test Plan

- holding a live note and changing `Pitch Offset` with `New Notes Only` keeps
  the held note sounding unchanged
- holding a live note and playing a new pad after changing `Pitch Offset`
  produces the new interval mapping
- holding multiple pads and changing `Pitch Offset` with `Retune Held Notes`
  re-emits the held notes at the expected new pitches
- releasing held pads after a retune sends note-off for the retuned pitch, not
  the original pitch
- changing root / octave / layout while holding notes follows the chosen held
  note policy consistently
- no stuck notes occur when switching between live play and step modes
- no stuck notes occur when leaving `NOTE` mode entirely

## Notes

- The current hidden preference plumbing can stay in code as a reminder and a
  re-entry point.
- This should be treated as a live-note playback refactor, not as a small UI
  tweak.
