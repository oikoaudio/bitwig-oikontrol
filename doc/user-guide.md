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

- clip-row paste uses the currently selected clip slot as the source to copy from, or falls back to the playing clip on that track if no clip was explicitly selected
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
- shared root key, scale, and octave center, with local layout controls
- LED and OLED note feedback
- `Pitch Gliss` on the Channel encoder page
- `Velocity Sensitivity` with `SHIFT + Encoder 3` for `Default Velocity`

Useful live-note controls:

- `PATTERN UP/DOWN`: shared octave
- `ALT + NOTE`: toggle the local live-note layout shortcut
- `SHIFT + Encoder 4`: local layout (`Chromatic` / `In Key` in melodic note mode)
- `Encoder 4`: shared scale
- `ALT + Encoder 4`: shared root
- `MUTE_1`: sustain
- `MUTE_2`: sostenuto

`Melodic Step` and `Chord Step` now live behind `STEP SEQ` rather than under `NOTE`.

Quick start:

1. Enter `NOTE`.
2. Play notes directly on the pad grid.
3. Press `NOTE` again to move between the primary note-family surfaces.
4. Use `ALT + NOTE` or `SHIFT + Encoder 4` for the local layout, `PATTERN UP/DOWN` for shared octave, `Encoder 4` for shared scale, `ALT + Encoder 4` for shared root, and the Channel page for `Pitch Gliss` and live velocity response.

The shared `Root Key`, `Scale`, and `Octave` are global across `NOTE`, `Chord Step`, `Melodic Step`, and the held `SHIFT + BROWSER` settings overlay.

### Chord Step workflow
Press `STEP SEQ` once to enter `Melodic Step`, then press `STEP SEQ` again to switch to `Chord Step`. Press `NOTE` to return to live note input.

`Chord Step` is the chord-oriented note-step workflow.

- Row 1: clip row
- Row 2: chord definition or curated chord slots
- Rows 3-4: 32 visible steps
- the builder defaults to in-key view and uses the same shared root/scale as live NOTE
- In chord definition mode, first pick which notes should be in the chord on row 2, then place it on steps on the lower two rows.
- If you select one of the pre-defined chord families, the workflow mirrors Drum sequencing: pick a chord, then place or remove it on as many steps as you like
- Holding one or more step pads while pressing a chord pad rewrites those held steps with the chosen chord

Important gestures:

- top row clip pads: launch, select, create, and paste clips; `Delete` clears clip contents and `Shift + Delete` removes the clip object
- `PATTERN DOWN/UP`: previous/next visible step page
- `STEP SEQ`: enter `Melodic Step`
- `SHIFT + STEP SEQ`: accent toggle/edit
- `ALT + STEP SEQ`: Fill
- tap an empty step pad: place the selected chord
- tap a lit step pad: remove the chord from that step
- `BANK LEFT/RIGHT`: move written step content left or right
- `SHIFT + BANK LEFT/RIGHT`: experimental micro-timing nudge for chord material; behavior is currently temperamental
- `ALT + BANK LEFT/RIGHT`: halve / double clip length
- `MUTE_1`: select / load step
- `MUTE_2`: last-step target mode
- `MUTE_3`: paste to target step or clip slot
- `MUTE_4`: delete target step or clip
- `ALT + MUTE_4`: invert chord
- `SHIFT + ALT + MUTE_4`: invert chord in the opposite direction
- `Encoder 3`: chord family
- `ALT + Encoder 3`: chord family page
- `Encoder 4`: interpretation (`As Is` / `In Scale`)
- `SHIFT + Encoder 4`: shared scale
- `ALT + Encoder 4`: shared root

Chord banks are static libraries of chord formulas and voicing variants. Changing shared `Root Key` or `Scale` does not switch to a different bank or slot; it only changes how the selected slot is rendered. In `As Is`, the stored chord shape is transposed from the current root. In `In Scale`, the same slot is rebuilt from the current shared scale and root, so changing scale can reharmonize the result.

Timing note:

- coarse nudge is intentionally disabled in `Chord Step`
- chord-step micro-timing is currently temperamental and should be treated as experimental

### Melodic STEP mode

`STEP` is a generative and editable mono phrase sequencer for basslines, motifs, and melodic hooks.
It edits a 2-bar / 32-step window and does not expand beyond that range from within the mode.

- Row 1: clip row
- Row 2: compact 16-note in-scale pitch pool
- Rows 3-4: 32 visible steps
- generated phrases are constrained to the current pitch pool
- different generator modes provide different phrase grammars such as `Acid`, `Motif`, `Call/Resp`, `Rolling`, and `Octave`

Main ideas:

- `Pattern Up` works on the pitch pool
- `Pattern Down` works on the phrase
- if you manually edit the pitch pool, it is treated as user-owned and is not auto-replaced on mode switch
- if the pool was auto-generated, first generation in a different mode can rebuild it for that mode
- changing pool octave center no longer rewrites the selected clip immediately

Important gestures:

- top row clip pads: launch, select, create, and paste clips; `Delete` clears clip contents and `Shift + Delete` removes the clip object
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

Encoder pages:

- `Channel`: engine, density, pitch-pool octave center, mutation type
- `Mixer`: melodic process transforms
- `User 1`: engine macro, tension, legato, recurrence helper
- `User 2`: selected or held step octave, gate, velocity, articulation

Channel page details:

- Encoder 1: `Engine`
- `ALT + Encoder 1`: engine subtype / family when available
- Encoder 2: `Density`
- Encoder 3: pitch-pool octave center
- `ALT + Encoder 3`: shared root key
- Encoder 4: `Mutation Type`
- `ALT + Encoder 4`: mutation strength

User 1 page details:

- Encoder 1: engine-specific macro such as motion, contour, answer, movement, or jump
- Encoder 2: tension
- Encoder 3: legato
- Encoder 4: recurrence span helper

Recurrence editing:

- hold one or more active melodic steps to enter recurrence targeting
- while steps are held, the top clip row becomes an 8-pad recurrence editor instead of clip launch
- tap pads on that top row to toggle recurrence hits within the current span
- hold the first top-row pad as a span anchor, then tap another top-row pad to set the recurrence span
- touch `User 1 / Encoder 4` to see the current recurrence summary for the held step set
- recurrence editing currently applies only to held active note steps

Melodic left-side buttons now align with the shared sequencer clip/edit workflow:

- `MUTE_1`: select clip without launch
- `MUTE_2`: last-step target mode
- `MUTE_3`: paste to clip slot
- `MUTE_4`: clear step or clear clip contents
- `SHIFT + MUTE_4` on a clip pad: remove the clip object

Melodic transform controls moved to the `Mixer` page:

- Encoder 1: halve / double length
- Encoder 2: swivel / mirror-double
- Encoder 3: reverse
- Encoder 4: invert down / up

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
- `User 2`: selected device remotes 1-4

Track-action page:

- `SHIFT + PERFORM`: toggle the latched track-action page
- row 1: stop for the 16 visible tracks
- row 2: solo for the 16 visible tracks
- row 3: mute for the 16 visible tracks
- row 4: arm for the 16 visible tracks
- row colors are blue/dark for stop, yellow for solo, orange for mute, and red for arm

Global settings overlay:

- `SHIFT + BROWSER`: hold the global `Root Key` / `Scale` / `Octave` overlay from any mode
- Encoder 1 adjusts shared root
- Encoder 2 adjusts shared scale
- Encoder 3 adjusts shared octave
- releasing `BROWSER` returns to the active mode view

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
- `BROWSER`: open or close Bitwig popup browser
- `ALT + BROWSER`: open browser after the current device / insertion context
- `SHIFT + ALT + BROWSER`: open browser before the current device / insertion context
- `SHIFT + BROWSER`: hold global pitch settings instead of opening the browser

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
- `Melodic Seed Mode`
- `Default Velocity Sensitivity`
- `Melodic Fixed Seed`
- `Pad Brightness`
- `Pad Saturation`
- `Encoder touch reset`
- `SELECT Encoder`
- `Euclid Scope`
- `Drum Mode Pinning`
- `Step Seq Pad Audition`
- `On-screen action notifications`

`Pad Brightness` and `Pad Saturation` interact with the Bitwig track colors used by `DRUM` and `PERFORM`, so the same settings can read differently across different project palettes.

`Melodic Seed Mode` controls how `Melodic Step` chooses its initial generator seed when the controller session starts. `Random` starts each session from a new seed. `Fixed` starts from the configured `Melodic Fixed Seed` value, which makes the sequence of generated melodic phrases reproducible across reconnects or reloads. Each `Generate` press still advances forward from that starting point.

The melodic seed controls are grouped into their own `Generative control` preference section so they stay together in Bitwig's settings UI.

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
