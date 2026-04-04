# Align Fire Drum and Note-Step Mappings

## Summary

Make Fire step modes share the same button and encoder grammar, with Drum Sequence as the behavioral baseline, while explicitly **not** forcing a shared clip-row layout yet.

This pass is primarily a **remap/reuse** pass:

- `Channel` = shared step-expression page
- `Mixer` = shared mixer page
- `User 1` = shared note-behavior/randomness page
- `User 2` = mode-specific page
- top-right left/right arrows (`BANK_L/BANK_R`, the grid arrows) = shared move / fine-nudge buttons in all step sequencers
- live `NOTE` input and `Oikord Step` share the same pitch-context buttons
- clip-grid unification is **deferred** to a future global `PERFORM` mode

Navigation assumption for this plan:

- plain `NOTE` switches between live Note and `Oikord Step`
- `ALT + NOTE` switches the current surface's two-state variant
- `STEP SEQ` is no longer part of `Oikord Step` entry

## Key Changes

### 1. Shared Encoder Pages Across Step Sequencers

Standardize the first three encoder pages across Drum, `Oikord Step`, and future note/bass/melody step modes.

Shared pages:

- `Channel`
  - `Encoder 1`: `Velocity`
  - `Encoder 2`: `Pressure`
  - `Encoder 3`: `Timbre`
  - `Encoder 4`: `Pitch Expression`
- `Mixer`
  - `Encoder 1`: `Volume`
  - `Encoder 2`: `Pan`
  - `Encoder 3`: `Send 1`
  - `Encoder 4`: `Send 2`
- `User 1`
  - `Encoder 1`: `Note Length`
  - `Encoder 2`: `Chance`
  - `Encoder 3`: `Velocity Spread`
  - `Encoder 4`: `Repeat`

Implementation notes:

- Reuse the existing Drum step-edit functions; this pass mostly rehomes them.
- `SequencEncoderHandler` becomes the shared step-page handler rather than a Drum-only page system.
- `EncoderMode` is updated to the new page meanings and reused by note-step modes.

### 2. Held-Step Editing in `Oikord Step`

When one or more steps are held in `Oikord Step`, shared pages edit the literal notes already written into the clip:

- `Channel` edits `Velocity`, `Pressure`, `Timbre`, `Pitch Expression`
- `User 1` edits `Note Length`, `Chance`, `Velocity Spread`, `Repeat`

Important simplification:

- remove the separate Oikord-only `gate` parameter
- treat shared `Note Length` as the authoritative way to edit how long written chord notes are
- keep only truly chord-specific controls on `Oikord Step`’s mode-specific page

### 3. Shared Step-Movement Buttons

Standardize the top-right left/right arrow buttons (`BANK_L/BANK_R`) across all step sequencers:

- plain `BANK_L/BANK_R`: move step content left/right by the coarse grid
- `SHIFT + BANK_L/BANK_R`: fine nudge held/selected notes
- existing Drum behavior is the source of truth and should be extracted/reused, not re-invented

This applies to:

- Drum Sequence
- `Oikord Step`
- future acid/bass/melody step sequencers

Once standardized, these buttons no longer carry pitch/range duties inside step sequencers.

### 4. Shared Pitch/Range Buttons for Live Note and Chord Step

Move live note and chord-step pitch/range context onto the right-side edit buttons in `NOTE` top-level mode so live note play and chord entry share the same muscle memory.

Inside `NOTE` top-level mode:

- `MUTE_1`: octave down
- `MUTE_2`: octave up
- `MUTE_3`: root down
- `MUTE_4`: root up

In `Oikord Step`, these become:

- `MUTE_1`: chord octave offset down
- `MUTE_2`: chord octave offset up
- `MUTE_3`: chord root offset down
- `MUTE_4`: chord root offset up

This frees the grid arrows for shared step movement.

### 5. Mode-Specific `User 2`

Keep `User 2` mode-specific.

Drum `User 2` becomes:

- `Encoder 1`: Euclid length
- `Encoder 2`: Euclid density/pulses
- `Encoder 3`: `Occurrence`
- `Encoder 4`: `Recurrence Length`

`Rotation` and `Invert` are removed from the default Drum `User 2` page.

Recurrence detail remains handled by the existing recurrence editor:
- `Encoder 4` sets recurrence length
- the existing pad-based recurrence mask editor remains the place where the active bars are chosen

`Oikord Step` `User 2` becomes:

- `Encoder 1`: root offset
- `Encoder 2`: octave offset
- `Encoder 3`: family select
- `Encoder 4`: reserved / no-op for now

Existing Oikord-specific button behavior remains:

- `ALT + NOTE` in `Oikord Step` toggles `As Is` / `Cast`
- `PATTERN` pages within the current Oikord family

### 6. Clip Grid and Copying Are Explicitly Deferred

Do **not** unify the Drum top-row clip workflow into note/chord step modes in this pass.

Decision:

- Drum keeps its current “clips on top row” convenience
- `Oikord Step` keeps its upper-half pad area for chord selection
- no shared top-row clip/copy partition is introduced in note/chord step modes yet

Rationale:

- this plan is about shared step-edit grammar, not pad partition
- taking a top row away from `Oikord Step` would cut visible chord vocabulary from `32` to `16`
- a shared clip grid belongs more naturally to a future global `PERFORM` mode than to any one sequencer mode

Future direction:

- `PERFORM` should eventually own the global 16x4 clip-grid view
- after `PERFORM` exists, an optional note/chord layout variant with top-row clips can be reconsidered
- that optional variant should not be the default in this pass

### 7. Deferred Oikord Running-Position Indicator

Add a Drum-style running-position indicator to the lower 32 step pads in
`Oikord Step`, so the currently playing step is visible during playback.

Decision:

- this is feasible and should be implemented later
- it is explicitly deferred until after `PERFORM` mode is finished

Implementation note:

- the likely approach is to mirror Drum Sequence’s `playingStep()` observer and
  local-step pad-light overlay behavior inside `NoteMode`

## Important Interface / Behavior Changes

- `EncoderMode` page meanings change:
  - `Channel`, `Mixer`, `User 1` become shared across step modes
  - `User 2` stays mode-specific
- `SequencEncoderHandler` is reused in note-step modes, not only Drum
- `BANK_L/BANK_R` become shared step-move buttons in all step sequencers
- `MUTE_1..4` in `NOTE` top-level mode become shared pitch/range controls
- plain `NOTE` switches between live Note and `Oikord Step`
- `ALT + NOTE` switches the active surface variant
- `SHIFT + NOTE` remains available for `SNAP` or another labeled utility
- Oikord-specific `gate` control is removed
- Drum `User 2` Euclid page shrinks from 4 Euclid controls to 2 Euclid controls plus `Occurrence` / `Recurrence Length`
- clip/copy pad partition remains different between Drum and note/chord step modes in this pass
- Drum-style running-position display in `Oikord Step` is deferred to a later pass

## Test Plan

- `KNOB_MODE` cycles the same page order in Drum and `Oikord Step`
- `Channel` edits the same four properties in Drum and `Oikord Step`
- holding Oikord steps and turning `Channel` / `User 1` edits the held clip notes directly
- `User 1` edits note length, chance, velocity spread, and repeat in Drum and `Oikord Step`
- Drum `User 2` uses Euclid length/density plus occurrence/recurrence length
- recurrence mask editing in Drum still works after the remap
- `Oikord Step` `User 2` edits root offset, octave offset, and family select
- `BANK_L/BANK_R` move/nudge behavior matches Drum across Drum and `Oikord Step`
- `MUTE_1..4` change octave/root in live `NOTE` and root/octave offsets in `Oikord Step`
- plain `NOTE` switches between live `NOTE` and `Oikord Step`
- `ALT + NOTE` toggles `chromatic` / `in-key` in live `NOTE`
- `ALT + NOTE` toggles `As Is` / `Cast` inside `Oikord Step`
- `PATTERN` still pages Oikord families correctly
- Drum top-row clip workflow remains unchanged in this pass
- note/chord step modes do not gain shared clip-row behavior in this pass
- `Oikord Step` does not yet show a Drum-style running-position indicator in this pass

## Assumptions

- Drum Sequence behavior is the baseline; note/chord step modes are aligned to Drum, not vice versa.
- `Mixer` remains a mixer page in this pass; no held-note retargeting is added there yet.
- `Occurrence` / `Recurrence` live on Drum `User 2`, with the existing recurrence editor retained for mask selection.
- `Oikord Step` `User 2` leaves one encoder intentionally unused/reserved rather than inventing a fourth chord-specific control prematurely.
- `Clip Step Record` is not part of the near-term Note-family navigation grammar and remains deferred.
- shared clip-grid behavior is deferred to future `PERFORM`, not folded into this consistency pass.
- `Oikord Step` playhead indication is deferred even though the same general approach used in Drum Sequence should work there.
