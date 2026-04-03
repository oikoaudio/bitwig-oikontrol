# Align Drum and Note Mode Encoders

This note captures a follow-up cleanup that is intentionally deferred from the
first `Oikord Step` landing. The goal is to align Drum and Note Step encoder
pages without mixing that remap into the same branch as the new note-step mode.

## Intent

- Keep the current Oikord branch focused on landing `Oikord Step`
- Revisit encoder-page harmonization in a separate branch
- Reduce mode-to-mode relearning where the underlying edit target is the same

## Proposed Shared Page

Promote `Channel` into the shared literal-note edit page across Drum and Note
Step modes.

Target `Channel` mapping:

- `Encoder 1`: `Timbre`
- `Encoder 2`: `Pressure`
- `Encoder 3`: `Note Length`
- `Encoder 4`: `Velocity`

## Proposed Drum Follow-Up

Move more esoteric drum step-behavior edits off `Channel` and onto `User 1`.

Likely `User 1` contents after remap:

- `Chance`
- `Repeats`
- `Recurrence Length`
- related repeat / recurrence behavior

Keep `User 2` as Euclid in Drum mode.

## Proposed Note Step Follow-Up

After the shared `Channel` page is in place, define a distinct `User 1` page
for `Oikord Step` based on real-world use rather than guessing too early.

Likely candidates:

- root offset
- octave offset
- `As Is` / `Cast`
- later chord-native transforms if they prove useful

## Deferred Items

- Oikord audition preference, analogous to drum audition
- any Drum-mode encoder remap work on the Oikord landing branch
