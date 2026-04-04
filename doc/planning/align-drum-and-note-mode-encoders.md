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

While one or more note steps are held in `Oikord Step`, the shared `Channel`
page should retarget those held notes directly, just as Drum mode already
edits held step content. In practice that means held Oikord steps should become
editable for shared literal-note properties such as:

- `Note Length`
- `Pressure`
- `Timbre`
- `Velocity`

This held-step retargeting is not implemented yet in `Oikord Step` and remains
part of the deferred encoder-page unification work.

Likely candidates:

- root offset
- octave offset
- `As Is` / `Cast`
- later chord-native transforms if they prove useful

## Deferred Items

- Oikord audition preference, analogous to drum audition
- any Drum-mode encoder remap work on the Oikord landing branch
- Drum-style running-position indication on the lower 32 pads in `Oikord Step`
  should be added later, but is deferred until after `PERFORM` mode is
  finished

# My current idea:
The step of making the modes behave consistently. that means moving some functions to other buttons, and maybe some shared functionality

1. if we use a shared Channel layout that is the same for all modes so that becomes muscle memory in a way, then mode specific things go to user1/user2.

2. moving steps left and right with nudging/fine nuding should really behave the same whether we sequence drums, chords or a possible future acid line/bass/melody note sequencer

3. buttons that move the note range up and down in octaves and note ranges should be shared between the note input and the chord sequencing mode if possible. perhaps we CAN at some point introduce pitching of drums per step? many of the drum sequencing modules do support not only note input of course but also tuning. then we have to maybe use note expression (similar to pressure/timbre) rather than note info, or some special drum machine setup with sounds on separate channels to avoid triggering other sounds, or one drum sound per track which ties it to a template which isn't ideal.. 

4. encoder functions: how far can we take shared function like

Channel: Velocity, Pressure, Timbre, Pitch expression
Mixer: Volume, panning, send 1, send 2
User 1:  Chance, Velocity spread, Repeat, Occurence ("randomness")
user 2: mode specific functions. euclidean for drums.. i mean euclidean COULD also be used for chord seq mode with len, density, note randomness, and encoder 4 note offset.. not sure how useful currently.

Note on mixer: if a note step is held, the values should be updated for those notes in the clip if encoder values are changed. perhaps  Mixer: Volume become "gain expression", pan "pan expression", send 1 and 2 become.. ??

can we improve the plan for button/knob mapping consistency along these lines? what collisions do you see? right now Channel is used for the controls for chord seq mode but those could be moved to user 2 or we rely more on buttons that aren't taken to shift chord family, octave and note offset.
