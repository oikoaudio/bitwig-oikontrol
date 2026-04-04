# Bitwig Oikontrol

These are controller extensions for Bitwig Studio v5.3+. The repo currently contains one extension for the Novation Launch Control XL Mk2 and one for the Akai Fire.

Bitwig Oikontrol currently provides two controller extensions:
**Novation Launch Control XL (Mk2):**
* Retains the factory templates in the extension that ships with Bitwig, on which this extension is based
* User templates 1-5 pass raw MIDI into Bitwig, enabling project MIDI mapping. LEDs under each knob mirror values (off, low, high).
* User Template 6: Device Remotes mode. Each row of controls maps to the 1st, 2nd.. 6th page of Preset Remote controls for the selected device. This is useful for in depth control of one patch. In development this was used to control a DFAM grid patch.
* User Template 7 is Drum Machine mode. The faders control chain volume, knobs map to the remotes on the first device in each chain, and the bottom button row triggers drum pads (top row) and toggles the fourth remote on each channel (bottom row)
* User Template 8 is the Richie Hawtin / Eric Ahrens arp workflow taken from
[https://github.com/ericahrens/rhbitwig/]

**Akai Fire:** The Fire script uses three top-level modes: `DRUM`, `NOTE`, and `PERFORM`.
* `DRUM` uses a fixed 4-row layout: row 1 = clips, row 2 = drum slots, rows 3-4 = 32 visible steps.
* Drum mode uses `STEP SEQ` for Accent, `SHIFT + STEP SEQ` for Fill, and the `GRID` left/right buttons for pattern shift and fine nudge. `ALT + GRID` left/right changes grid resolution.
* Drum step encoder pages use `Channel`, `Mixer`, `User 1`, and `User 2`, with Euclid controls on Drum `User 2`.
* Drum mode pinning is configured in preferences: it can either follow the current Bitwig selection or auto-pin to the first `Drum Machine` in the project when `DRUM` mode is entered.
* `NOTE` provides a 16x4 isomorphic note grid with `Chromatic` and `In Key` layouts, local root note / scale / octave controls, and OLED / LED note feedback.
* In `NOTE`, `STEP SEQ` enters the current note-step sub-mode and `SHIFT + STEP SEQ` cycles the selected sub-mode. Current note-step sub-modes are `Oikord Step` and the deferred `Clip Step Record` placeholder.
* `Oikord Step` uses the upper two pad rows for 32 curated Oikord slots and the lower two rows for 32 visible steps. It uses the same shared `Channel`, `Mixer`, and `User 1` step pages as Drum, with Oikord-specific controls on `User 2`.
* In `Oikord Step`, `MUTE_1..4` set chord octave/root offsets, `PATTERN` pages the active Oikord family, and `BANK_L/BANK_R` move or fine-nudge step content.
* `PERFORM` provides a global `16x4` clip launcher. Pads launch clips, create a new `4`-bar clip on empty slots, and show clip color plus queued / playing / recording state.
* In `PERFORM`, hold `MUTE_1` and press a pad to select a clip without launching it, hold `MUTE_3` and press a pad to copy from the selected visible clip, and hold `MUTE_4` and press a pad to delete a clip.
* In `PERFORM`, `MUTE_2` changes selected visible clip length: plain press duplicates content and doubles the loop length up to `256` bars, while `SHIFT + MUTE_2` halves loop length down to `1` bar.
* In `PERFORM`, `BANK_L/BANK_R` scroll tracks, `PATTERN` up/down scroll scenes, and holding `SHIFT` makes those controls move by `1` instead of paging.
* `PERFORM` encoder pages are `Channel = project remotes`, `Mixer = selected track volume/pan/send1/send2`, `User 1 = selected track remotes`, and `User 2 = selected device remotes`. Touching an encoder resets that parameter to its default.
* Clip launch behavior is configured in preferences: launch mode is `Synced` or `From Start`, and clip launch quantization is also selected there.
* The main encoder role is configurable between `Last Touched Parameter`, `Shuffle`, and `Note Repeat`.
* `PATTERN` defaults to Clip Launcher Automation Write, with `SHIFT + PATTERN` fixed to metronome.
* `ALT + PLAY` retriggers the current clip; plain `PLAY` toggles transport and retriggers on start.
* Fine-grid step nudging is based on Wim Van den Borre's Akai Fire Nudger work [https://github.com/wimvandenborre/AkaiFireNudger/], adapted here for the current Fire sequencer workflow.

## Requirements

- **Java:** built and tested with JDK 21.
- **Bitwig API:** targets `extension-api:24`, which ships with **Bitwig Studio 5.3**. The extension should also work in 6.0 (tested beta 12)
- **Controllers:** Novation Launch Control XL Mk2, Akai Fire

## Building & testing

All sources, including the Bitwig framework helpers, live inside this repository. 

If you use [`just`](https://github.com/casey/just), the common tasks are:

```bash
just fire-compile
just fire-test
just fire-extension
just launchcontrol-build
just launchcontrol-test
just launchcontrol-extension
just artifacts
```

The `Justfile` defaults `GRADLE_USER_HOME` to `/tmp/gradle-home`, which works well in WSL and avoids polluting the home directory during wrapper downloads.

```bash
cd /path/to/bitwig-oikontrol
./gradlew clean build --no-daemon \
  -Dorg.gradle.java.home="$JAVA_HOME"
```

The built controller artifacts are placed under each module’s `build/libs` directory, for example `modules/akai-fire/build/libs`. Unit tests are located in `src/test/java`. `NoteInputConfiguratorTest` is an example of using Mockito to (thinly) mock Bitwig APIs.

Module-specific Gradle commands:

```bash
GRADLE_USER_HOME=/tmp/gradle-home ./gradlew :modules:akai-fire:build --no-daemon
GRADLE_USER_HOME=/tmp/gradle-home ./gradlew :modules:akai-fire:jar --no-daemon

GRADLE_USER_HOME=/tmp/gradle-home ./gradlew :modules:launchcontrol:build --no-daemon
GRADLE_USER_HOME=/tmp/gradle-home ./gradlew :modules:launchcontrol:jar --no-daemon
```

Current artifact names:
- `modules/akai-fire/build/libs/AkaiFireOiko-2.0.0.bwextension`
- `modules/launchcontrol/build/libs/LaunchControlXlOikontrol-2.0.0.bwextension`

## Documentation
- Architecural Decision Record (ADR) under `doc/adr/` e.g. `0000-use-adrs.md` (process) and `0001-architecture-summary.md` (factory/user modes, Template 8 arp, Java/API targets).
- `CHANGES.md`: running changelog of code edits.
- `vscode-debug.md`: how to attach VS Code’s debugger to Bitwig (JDWP).

## Attribution

- Based on Bitwig’s original Launch Control XL controller script (MIT License) from
  [bitwig-extensions](https://github.com/bitwig/bitwig-extensions). The helper packages
  (`com.bitwig.extensions.framework`, `…controllers.novation.common`, `…util`) remain under Bitwig’s MIT terms.

- The arp workflow on User Template 8 is adapted from Eric Ahrens’ [rhbitwig](https://github.com/ericahrens/rhbitwig) project (also MIT).

- The Akai Fire extension is a fork of [rhbitwig](https://github.com/ericahrens/rhbitwig) by Eric Ahrens (MIT license)

Please keep these acknowledgements if you redistribute or build on this project.

## User Mode controller configuration

For the User Modes (7 for Drum Machine and 8 for Arpeggiator) the following (factory default) configuration is assumed on the controller

* All controls send to the MIDI channel corresponding to the Template number, e.g. user Template 8 send on MIDI channel 8.
* All buttons set to **momentary**

Knobs/Faders
------------
Top row knobs: CC 13–20
Middle row knobs: CC 29–36
Bottom row knobs: CC 49–56
Faders: CC 77–84

Navigation/buttons:
Send Up/Down: CC 104/105
Track Left/Right: CC 106/107

Right-side mode buttons:
Device: note 105 (A6)
Mute: note 106 (A#6)
Solo: note 107 (B6)
Record Arm: note 108 (C7)

Track focus row: **notes** 41, 42, 43, 44, 57, 58, 59, 60 //(F1-G#1, A2-C3)
Track control row: **notes** 73, 74, 75, 76, 89, 90, 91, 92 //(C#4-E4, F5-G#5)

User mode 7 (drum machine) expects a different set of notes sent from the Track focus row:
Track focus row: **notes** 36, 37, 38, 39, 40, 41, 42, 43 //(C1-G1) momentary values
Track control row: **notes** 73, 74, 75, 76, 89, 90, 91, 92 //(C#4-E4, F5-G#5)

## Device Remotes, Drum & Arp User Modes

**User Mode 6 (Device pages)**
To activate it, select user template 6 on the LCXL.
Each row of knobs, faders and buttons corresponds to the remote pages of the selected device
Ideal for detailed control of a complex device such as a DFAM clone Bitwig Grid sequencer

**User Mode 7 (Drum Machine)**
To activate it, select user template 7 on the LCXL. Optionally tick “Auto-attach to first Drum Machine and Arpeggiator”
to have the controller automatically find and pin the first Drum Machine device in the project, so you don't have to
select it before controlling it.

Each vertical column of knobs/slider/buttons controls the sound from one drum pad. Track select shifts the focus over
the the next set of 8 pads.

- Knobs target the 1st, 2nd and 3rd Remote of the first device on each drum pad.
- Slider: Pad volume
- Track focus row: selects pads and can audition them (preference: “Audition on drum pad select”).
- Track control row:
  - Default: controls pad remote 4; behavior can be momentary or toggle (preference: “Drum accent buttons momentary”).
  - Mute mode: pads mute/unmute (bright green = unmuted, dim green = muted).
  - Solo mode: pads solo/unsolo (yellow).
- Right-side buttons: Mute toggles drum mute mode; Solo toggles solo mode; Device/Record are unused in drum mode.
- Navigation: Track Left/Right scroll the pad bank; Send Up/Down unused (LEDs still indicate scroll availability).
- LED hints: top row bright yellow = selected pad; bottom row reflects mute/solo/accent state.

**User Mode 8 (Arpeggiator)**
- Select user template 8 (default factory mapping). Optional: auto-attach to first arp (same preference as above).
- Other arp mappings follow the bundled arp layer (see rhbitwig for a complete overview)

# Known issues:
- The Arp "custom scale mode" filter that removes notes of a given pitch does not work as expected yet. It does work
well with the Global Key/scales in Bitwig 6 however. 


## Factory modes

These are the same as in the Bitwig factory script

Factory template 1: Two sends and device mode
The first and second knobs rows control the sends. You can scroll the send window using the send select buttons. The third knob row controls the remote controls of the currently selected device.

Factory template 2: Two sends and device mode
The first and second knobs rows control the sends. You can scroll the send window using the send select buttons. The third knob row controls the first remote controls of track's device.

Factory template 3: Two sends and project remotes
Same as above except that the third row controls the project's remotes.

Factory template 4: Three sends mode
Same as above except that the third row is an additional send control.

Factory template 5: One send and Two channel device controls mode
The first row of knobs controls the send. The second and third rows of knobs controls the two first remote controls of each track's selected device.

Factory template 6: Three channel device controls mode
The first, second and third rows of knobs controls the three first remote controls of each track's selected device.

Factory Template 7: Not supported

Factory template 8: Three track remote controls mode
The first, second and third rows of knobs controls the three first remote controls of each track.
