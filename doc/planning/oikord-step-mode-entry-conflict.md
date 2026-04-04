# Oikord Step Mode Entry Conflict

## Problem

The current route into `Oikord Step` conflicts with established Drum behavior.

In Drum mode:

- `STEP SEQ` is already used for Accent
- `SHIFT + STEP SEQ` is already used for Fill

That makes `STEP SEQ` a poor long-term entry path for `Oikord Step`, because
the button is already carrying important Drum-specific behavior.

## Current Implementation

The code currently splits `NOTE` and `STEP SEQ` responsibilities like this:

- `NOTE` in top-level Note mode:
  - cycles live note layout when already in live Note
  - exits step mode back to live Note when `Oikord Step` or `Clip Step Record`
    is active
- `STEP SEQ` in Note mode:
  - enters the current note-step sub-mode from live Note
  - `SHIFT + STEP SEQ` cycles the note-step sub-mode and enters it
  - re-press in `Oikord Step` toggles `As Is` / `Cast`

So `NOTE` is already the "leave step mode" button, but `STEP SEQ` still owns
entry, sub-mode selection, and one Oikord-specific action.

## Why This Matters

- mode entry should be reachable from any top-level mode without colliding with
  the primary function of the current mode
- `Oikord Step` is conceptually closer to `NOTE` than to Drum Accent / Fill
- as more modes are added, button ownership needs to remain legible

## Recommended Cleanup Target

Treat `NOTE` as the owner of the whole note-family surface:

- live note play
- note layout switching
- note-step mode entry
- note-step mode exit

Treat `STEP SEQ` as Drum-owned again:

- Drum: Accent
- `SHIFT + STEP SEQ`: Fill
- Note family: no longer required for `Oikord Step` access

## Recommended Staged Plan

Use a low-risk cleanup first, then decide later whether a full `NOTE`-cycle is
worth the extra complexity.

### Phase 1: Make `NOTE` Own The Two Note-Family Surfaces

Treat the near-term Note family as two surfaces, each with a two-state variant:

- live note input
  - `chromatic`
  - `in-key`
- `Oikord Step`
  - `As Is`
  - `Cast`

Proposed behavior:

- plain `NOTE`
  - from Drum / Perform: enter live Note
  - from live Note: enter `Oikord Step`
  - from `Oikord Step`: return to live Note
- `ALT + NOTE`
  - in live Note: toggle `chromatic` / `in-key`
  - in `Oikord Step`: toggle `As Is` / `Cast`

This gives `NOTE` one stable job, switching between the two note-family
surfaces, while `ALT + NOTE` changes the current surface's interpretation.

### Phase 2: Revisit Full `NOTE` Cycling Later

After the remaining `PERFORM` / global navigation cleanup, reevaluate whether
plain repeated `NOTE` presses should cycle the whole note family:

- live chromatic
- live in-key
- `Oikord Step`
- `Clip Step Record`

That remains a separate design decision and would only make sense if
`Clip Step Record` becomes important enough to justify a third surface.

## Why This Staged Approach Is Better Than Immediate Full Cycling

- it gives plain `NOTE` one coherent surface-switching job
- it gives `ALT + NOTE` one coherent variant-switching job
- it removes the Drum collision immediately
- it makes the two two-state pairings line up naturally:
  `chromatic` / `in-key` and `As Is` / `Cast`
- it leaves `SHIFT + NOTE` available for a labeled utility such as `SNAP`
- it leaves room for a later expansion only if a third note-family surface is
  really needed

## Button Ownership After Cleanup

- `STEP SEQ`
  - Drum Accent / Fill only
- `NOTE`
  - switches between live Note and `Oikord Step`
- `ALT + NOTE`
  - switches the current surface's two-state variant
- `SHIFT + NOTE`
  - remains available for `SNAP` or another labeled utility if needed

## Implementation Slices

### 1. Extract Note-Family Navigation Out of `STEP SEQ`

Add explicit Note-mode methods for:

- toggle between live Note and `Oikord Step`
- toggle live-note layout
- toggle Oikord interpretation
- return to live Note

The top-level extension should call those from `NOTE` / `ALT + NOTE`
instead of letting `NoteMode` keep `STEP SEQ` as the owner.

### 2. Remove Note-Mode `STEP SEQ` Dependence

Once `NOTE` combinations work:

- stop binding Note-mode entry behavior to `STEP SEQ`
- either leave `STEP SEQ` inert in Note mode or reserve it for a future
  note-step-specific action only if there is a strong reason
- do not reuse it casually, because the point of this cleanup is to restore
  clean button ownership

### 3. Update OLED / Light Feedback

Adjust Note-family messaging so the surface explains the new button grammar:

- entering `Oikord Step` should show that surface clearly
- returning to live Note should show Note mode clearly
- `ALT + NOTE` in live Note should show layout state clearly
- toggling `As Is` / `Cast` should keep the current clear Oikord mode message
- `NOTE` light behavior should continue to reflect Note-mode ownership cleanly

### 4. Update Docs Together

When the code moves, update these docs in the same pass:

- `doc/features/fire_note_step_and_oikord_mode.feature`
- `doc/planning/align-drum-and-note-mode-encoders.md`
- `doc/planning/detailed plan for align-drum-and-note-mode-encoders.md`

Those notes currently still assume `STEP SEQ` owns note-step entry.

## Collision Audit

### Collisions Resolved By This Cleanup

- Drum keeps exclusive ownership of `STEP SEQ`
- `Oikord Step` no longer depends on a Drum-owned button for entry
- the current split where `NOTE` exits step mode but `STEP SEQ` enters it goes
  away

### Collisions Avoided By Deferring

- no need to combine encoder cleanup with mode-entry cleanup
- no need to decide global `PERFORM` navigation in the same branch

### Things To Verify Before Implementing

- `ALT + NOTE` should stay narrowly defined as "switch the current
  Note-surface variant"
- `SHIFT + NOTE` should only be given a labeled utility role if there is a
  strong reason, for example `SNAP` in live Note
- if `Clip Step Record` remains planned, it should be explicitly deferred rather
  than left half-integrated into the button grammar

## Test Plan

- `STEP SEQ` in Drum still controls Accent
- `SHIFT + STEP SEQ` in Drum still controls Fill
- plain `NOTE` enters live Note from Drum and Perform
- plain `NOTE` switches between live Note and `Oikord Step`
- `ALT + NOTE` toggles `chromatic` / `in-key` in live Note
- `ALT + NOTE` toggles `As Is` / `Cast` in `Oikord Step`
- `Oikord Step` entry no longer depends on `STEP SEQ`
- OLED text matches the new entry and cycling behavior

## Deferred Decision

Whether plain repeated `NOTE` presses should eventually cycle the full
note-family state remains open. The immediate cleanup should only remove the
`STEP SEQ` ownership conflict and keep the implementation small. `Clip Step
Record` should stay explicitly deferred unless a clean third-surface grammar is
chosen.
