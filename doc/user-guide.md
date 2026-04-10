# Bitwig Oikontrol User Guide

Bitwig Oikontrol holds controller extensions for Bitwig Studio, currently for:

- Novation Launch Control XL Mk2
- Akai Fire

This guide is the user-facing reference for setup, modes, preferences, and common workflows. The bundled Help entry in each extension should open the same guide content.

## Installation and setup

1. Build or install the `.bwextension` for the controller you want to use.
2. In Bitwig Studio, open **Settings > Controllers**.
3. Add the Oiko controller entry for your hardware:
   - `Oikontrol LCXL`
   - `Oikontrol Fire`
4. Use the extension preferences if you want to change launch, pinning, or controller-specific behavior.

## Launch Control XL

The Launch Control XL extension keeps the Bitwig factory templates intact while adding deeper user-template workflows for:

- device remote pages
- drum machine control
- arpeggiator performance

### Controller assumptions

For the custom user-mode workflows, the factory-default user template mappings are assumed.

- All controls send on the MIDI channel that matches the selected template number.
- All buttons are configured as momentary.

User template note and CC assumptions:

- Top row knobs: `CC 13-20`
- Middle row knobs: `CC 29-36`
- Bottom row knobs: `CC 49-56`
- Faders: `CC 77-84`
- Send Up/Down: `CC 104/105`
- Track Left/Right: `CC 106/107`
- Right-side mode buttons: `105-108`

### Factory templates

Factory templates keep the same general role as the Bitwig factory script:

- Template 1: 2 sends and selected device controls
- Template 2: 2 sends and per-track device page 1
- Template 3: 2 sends and project remotes
- Template 4: 3 sends
- Template 5: 1 send and 2 per-track device controls
- Template 6: 3 per-track device controls
- Template 7: not supported
- Template 8: 3 track remote controls

### User Template 6: Device pages

User Template 6 turns the controller into a fixed page remote-control surface.

- Each hardware row maps to a fixed page of the selected device's preset remotes.
- This is useful when one patch exposes several remote pages and you want a stable one-template layout.

Quick start:

1. Select a device in Bitwig.
2. Switch the controller to User Template 6.
3. Adjust the rows, faders, and buttons as the current device pages require.

### User Template 7: Drum machine

User Template 7 is the drum workflow.

- Faders control chain volume.
- Knobs map to the first device on each visible drum pad chain.
- The upper pad row selects and can audition drum pads.
- The lower pad row controls pad remote 4 by default, or mute/solo depending on the active sub-mode.

Preferences that matter:

- `Auto-attach to first Drum Machine and Arpeggiator`
- `Audition on drum pad select`
- `Drum accent buttons momentary`

Quick start:

1. Load a Drum Machine on a track.
2. Switch the controller to User Template 7.
3. If auto-attach is enabled, the script will focus the first Drum Machine it finds.
4. Use Track Left/Right to move across pad banks.
5. Use the upper pad row to select pads and the knobs/faders to shape each sound.

### User Template 8: Arpeggiator

User Template 8 adapts the Richie Hawtin / Eric Ahrens rhbitwig arp workflow.

- The arp layer controls the focused Bitwig Arpeggiator.
- Pitch offset, velocity, gate, and step behavior are spread across the rows and mode buttons.
- Auto-attach can focus the first Arpeggiator in the project when enabled.

Quick start:

1. Add an Arpeggiator device in Bitwig.
2. Switch the controller to User Template 8.
3. Use the row controls for pitch, velocity, and gate shaping.
4. Use the side mode buttons to access the arp sub-modes inherited from rhbitwig.

## Akai Fire

The Akai Fire extension is a clip, note, and sequencer workflow built around four top-level modes:

- `STEP`
- `DRUM`
- `NOTE`
- `PERFORM`
- `SETTINGS` (not a full mode)

### DRUM mode

`DRUM` is the default sequencer-oriented workflow.

Pad colors in `DRUM` follow the Bitwig track or drum-lane color context. The brightness and saturation preferences affect how strongly those project colors translate to the Fire LEDs, so perceived intensity will vary with the chosen track color.

- Row 1: clip slots
- Row 2: visible drum slots
- Rows 3-4: 32 visible steps for the selected lane

Main gestures:

- `STEP SEQ`: enter `Melodic Step`
- `SHIFT + STEP SEQ`: accent entry and accent editing
- `ALT + STEP SEQ`: Fill
- `BANK LEFT/RIGHT`: pattern shift and movement
- `SHIFT + BANK LEFT/RIGHT`: fine nudge
- `ALT + BANK LEFT/RIGHT`: halve / double clip length

In `DRUM`, hold `Paste` and press a clip slot, drum pad, or step. The currently selected item of the same type is used as the source to copy from:

- clip-row paste uses the currently selected clip slot as the source to copy from
- drum-pad paste uses the currently selected drum pad as the source to copy from
- step paste uses the currently selected step as the source to copy from

Encoder pages:

- `Channel`: shared step expression editing
- `Mixer`: volume, pan, and sends
- `User 1`: step behavior page
- `User 2`: Euclid controls

Preferences that matter:

- `Drum Mode Pinning`
- `Step Seq Pad Audition`
- `Clip Launch Mode`
- `Clip Launch Quantization`
- `Euclid Scope`

Quick start:

1. Load a Drum Machine on a Bitwig track.
2. Enter `DRUM` mode.
3. If `Drum Mode Pinning` is set to `Auto-select First Drum Machine`, the script will focus and pin the first Drum Machine it finds.
4. If `Drum Mode Pinning` is set to `Follow Selection`, the drum sequencer follows the currently selected drum track/device context.
5. Select a lane on row 2.
6. Program steps on the lower two rows.
7. Hold `STEP SEQ` to accent notes or add accented hits quickly.

### NOTE mode

`NOTE` provides a 16x4 isomorphic playing surface.

- `Chromatic` and `In Key` layouts
- shared root key and scale, with local octave and layout controls
- LED and OLED note feedback
- `Pitch Gliss` on the Channel encoder page
- `Velocity Sensitivity` with `SHIFT + Encoder 3` for `Default Velocity`

Useful live-note controls:

- `PATTERN UP/DOWN`: octave
- `SHIFT + PATTERN UP/DOWN`: root
- `MUTE_1`: sustain
- `MUTE_2`: sostenuto

The current note-step sub-modes are:

- `Melodic Step`
- `Chord Step`
- `Clip Step Record` placeholder

Quick start:

1. Enter `NOTE`.
2. Play notes directly on the pad grid.
3. Press `NOTE` again to move between the primary note-family surfaces.
4. Use `PATTERN UP/DOWN` for octave, `SHIFT + PATTERN UP/DOWN` for shared root, and the Channel page for `Scale`, `Pitch Gliss`, and live velocity response.

### Chord Step workflow
To get to this mode from the Note live input mode, press the `NOTE` controller button. From any other mode, press the `NOTE` button twice to cycle past the Note input mode.

`Chord Step` is the chord-oriented note-step workflow.

- Upper two rows: chord definition or curated chord slots
- Lower two rows: 16 visible steps
- the builder defaults to in-key view and uses the same shared root/scale as live NOTE
- In chord definition mode, first pick which notes that should be in the chord on the top two rows, then place it on steps on the bottom two rows.
- If you select one of the pre-defined chord families (using the `PATTERN` buttons), the workflow mirrors Drum sequencing: pick a chord, then place or remove it on as many steps as you like
- Holding one or more step pads while pressing a chord pad rewrites those held steps with the chosen chord

Important gestures:

- `MUTE_1..4`: chord octave and root offsets
- `PATTERN DOWN/UP`: next/previous chord family
- `ALT + PATTERN DOWN/UP`: next/previous page within the current family
- `STEP SEQ`: enter `Melodic Step`
- `SHIFT + STEP SEQ`: accent toggle/edit
- `ALT + STEP SEQ`: Fill
- tap an empty step pad: place the selected chord
- tap a lit step pad: remove the chord from that step
- `BANK LEFT/RIGHT`: move written step content left or right
- `SHIFT + BANK LEFT/RIGHT`: adjust held chord-step note duration
- `ALT + BANK LEFT/RIGHT`: halve / double clip length
- `SHIFT + ALT + PATTERN DOWN`: clear the selected clip contents

In `Chord Step`, hold `Paste` (Mute_2) and press a step. The currently selected step is used as the source to copy from.

### Melodic STEP mode

`STEP` is a generative and editable mono phrase sequencer for basslines, motifs, and melodic hooks.

- Upper two rows: collapsed in-scale pitch pool
- Lower two rows: 16 visible steps
- generated phrases are constrained to the current pitch pool
- different generator modes provide different phrase grammars such as `Acid`, `Motif`, `Call/Resp`, `Euclid`, `Rolling`, and `Octave`

Main ideas:

- `Pattern Up` works on the pitch pool
- `Pattern Down` works on the phrase
- if you manually edit the pitch pool, it is treated as user-owned and is not auto-replaced on mode switch
- if the pool was auto-generated, first generation in a different mode can rebuild it for that mode

Important gestures:

- tap a pitch pad: add or remove that note from the pool
- tap a step pad: place, clear, or load that step depending on state
- hold a step pad and turn encoders: edit that held step directly
- `SHIFT + STEP SEQ`: hold and tap a step to toggle accent
- `PATTERN UP`: generate a new pitch pool for the current mode
- `ALT + PATTERN UP`: mutate the current pitch pool
- `PATTERN DOWN`: generate a new phrase from the current mode
- `ALT + PATTERN DOWN`: mutate the current phrase
- `SHIFT + PATTERN UP` and `SHIFT + PATTERN DOWN`: cycle the current view between `Notes`, `Expression`, and `Process`
- `BANK LEFT/RIGHT`: rotate the phrase
- `ALT + BANK LEFT/RIGHT`: halve or double the clip length
- `SHIFT + ALT + PATTERN DOWN`: clear the selected clip contents

Encoder pages:

- `Channel`: generator, density, shape, mutation type
- `Mixer`: track volume, pan, send 1, send 2
- `User 1`: tension, Euclid pulses, Euclid rotation, mutation amount
- `User 2`: selected or held step octave, gate, velocity, articulation

Notes on generation:

- `Acid` focuses on bassline-style phrase families
- `Motif` focuses on short melodic cells with repetition and variation
- `Call/Resp` creates a call phrase and an answering phrase
- `Rolling` targets denser rolling bassline motion
- the OLED generation message shows the current mode family, for example `Acd.RootAnswer`


### PERFORM mode

`PERFORM` is the clip-launch and performance surface.

Pad colors in `PERFORM` also follow Bitwig track and clip color context rather than a fixed Oikontrol palette.

- pads show clip color and launch state
- empty slots can create a new clip using the current `Default Clip Length` preference
- quick select, copy, delete, and clip-length gestures live on the `MUTE` buttons

Important gestures:

- hold `MUTE_1` + pad: select without launching
- hold `MUTE_3` + pad: paste the currently selected clip to the target slot
- hold `MUTE_4` + pad: delete
- `MUTE_2`: double visible clip length
- `SHIFT + MUTE_2`: halve visible clip length
- `BANK LEFT/RIGHT`: scroll tracks
- `PATTERN` up/down: scroll scenes
- hold `SHIFT` while scrolling for single-step movement

Encoder pages:

- `Channel`: global remotes 1-4
- `Mixer`: volume, pan, send 1, send 2
- `User 1`: track remotes 1-4
- `User 2`: master / cue controls

`SHIFT + PERFORM` opens a latched `Settings` page. From there:

- Encoder 1 adjusts the shared `Root Key`
- Encoder 2 adjusts the shared `Scale`
- the pad grid becomes a non-performance settings display
- press `PERFORM` again to leave `Settings`

### Main encoder and transport

Tap `SELECT` to swap the `SELECT` encoder between `Last Touched Parameter` and the currently selected alternate role.

Press `SHIFT + SELECT` to choose that alternate role by cycling through the full role list:

- `Last Touched Parameter`
- `Shuffle`
- `Tempo`
- `Note Repeat`
- `Track Select`
- `Drum Grid`

When `Note Repeat` is active:

- the first encoder position is `Off`
- turning `SELECT` past `Off` enables repeat at the chosen division

When `Track Select` is active:

- Turn `SELECT` to move to the previous or next track
- Hold `SELECT` while turning to jump by track pages
- In `DRUM` mode with automatic drum pinning enabled, the encoder falls back to `Drum Grid`

When `Drum Grid` is active:

- turn `SELECT` to change Drum Step grid resolution
- this role is only meaningful in `DRUM` mode

When `Shuffle` is active:

- turning `SELECT` down to `0` switches shuffle `Off`
- turning it up from `0` enables shuffle again

Shared transport behavior:

- `PLAY`: transport toggle with retrigger-on-start behavior
- `ALT + PLAY`: retrigger the current clip
- `REC`: clip recording in `DRUM`, arranger record in `NOTE` and `PERFORM`
- `ALT + REC`: arranger automation write
- `PATTERN`: clip launcher automation write
- `SHIFT + PATTERN`: metronome
- `ALT + PATTERN`: clip launcher overdub

## Preferences

### Launch Control XL preferences

- `Auto-attach to first Drum Machine and Arpeggiator`
- `Audition on drum pad select`
- `Drum accent buttons momentary`

### Akai Fire preferences

- `Clip Launch Mode`
- `Clip Launch Quantization`
- `Default Clip Length`
- `SELECT Encoder Startup`
- `Default Root Key`
- `Default Scale`
- `Default Note Input Octave`
- `Default Velocity Sensitivity`
- `Pad Brightness`
- `Pad Saturation`
- `Encoder touch reset`
- `SELECT Encoder`
- `Euclid Scope`
- `Drum Mode Pinning`
- `Step Seq Pad Audition`
- `On-screen action notifications`

`Pad Brightness` and `Pad Saturation` interact with the Bitwig track colors used by `DRUM` and `PERFORM`, so the same settings can read differently across different project palettes.

## Troubleshooting

### The wrong device is being controlled

- Check the relevant auto-attach or pinning preferences.
- In Fire `DRUM` mode, use `Follow Selection` to work on the currently selected drum track/device, or `Auto-select First Drum Machine` for a dedicated first-drum-machine workflow.
- Re-select the target track or device in Bitwig.
- Re-enter the relevant mode or template after changing focus.

### LEDs or mode state do not match what you expect

- Re-open the matching mode or user template.
- Power-cycle the controller if Bitwig started with another script attached first.
- Make sure the controller is using the expected template or default mapping.

## Attribution

- `rhbitwig` by Richie Hawtin and Eric Ahrens provided the Akai Fire drum sequencer which has been adapted for use here, as well as the arp workflow used on the LCXL User Template 8.

- The fine-grid step nudging is based on Wim Van den Borre's `AkaiFireNudger` fork of `rhbitwig`, and developed further here.

- All LCXL factory modes were taken from Bitwig's original `bitwig-extensions` for the Launch Control XL controller 
