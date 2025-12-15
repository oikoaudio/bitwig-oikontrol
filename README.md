# Bitwig Oikontrol

Bitwig Oikontrol bundles two controllers under one roof:
- **Novation Launch Control XL (Mk2):** retains factory templates, frees user templates for Bitwig mappings, and dedicates User Template 8 to the Richie Hawtin / Eric Ahrens arp workflow while templates 1–7 pass raw MIDI into Bitwig. LEDs under each knob mirror values (off, low, high).
- **Akai Fire:** Oiko-tuned fork of rhbitwig with Euclid mode on User 2 and per-pad mixer on Mixer mode.

## Requirements

- **Java:** built and tested with JDK 21.
- **Bitwig API:** targets `extension-api:24`, which ships with **Bitwig Studio 5.3**. The extension should also work in 6.0 (tested beta 6)
- **Controller:** Novation Launch Control XL Mk2.

## Building & testing

All sources, including the Bitwig framework helpers, live inside this repository. 

```bash
cd /path/to/bitwig-oikontrol
./gradlew clean build --no-daemon \
  -Dorg.gradle.java.home="$JAVA_HOME"
```

The resulting `.bwextension` artifact is placed under `bitwig-oikontrol/build/libs`. Unit tests are located in `src/test/java`; `NoteInputConfiguratorTest` shows how Mockito is used to (thinly) mock Bitwig APIs.

## Documentation

- Architecural Decision Record (ADR) under `doc/adr/` e.g. `0000-use-adrs.md` (process) and `0001-architecture-summary.md` (factory/user modes, Template 8 arp, Java/API targets).
- `CHANGES.md`: running changelog of code edits.
- `BUILD.md`: more detail on local builds and installation.
- `vscode-debug.md`: how to attach VS Code’s debugger to Bitwig (JDWP).

## Attribution

- Based on Bitwig’s original Launch Control XL controller script (MIT License) from
  [bitwig-extensions](https://github.com/bitwig/bitwig-extensions). The helper packages
  (`com.bitwig.extensions.framework`, `…controllers.novation.common`, `…util`) remain under Bitwig’s MIT terms.

- The arp workflow on User Template 8 is adapted from Eric Ahrens’ [rhbitwig](https://github.com/ericahrens/rhbitwig) project (also MIT).

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
