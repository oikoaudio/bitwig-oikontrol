# Bitwig Oikontrol

Opinionated controller extensions for Bitwig Studio, focused on turning MIDI controllers into expressive composition and performance surfaces.

Oikontrol currently supports:

- **Akai Fire**
- **Novation Launch Control XL Mk2**

The Akai Fire script is the main instrument-like surface. It brings together drum sequencing, melodic generators, chord-step sequencing, harmonic note input, fugue-style transformations, clip launching, and performance controls around the Fire's pads, encoders, OLED, and mode buttons.

The Launch Control XL script adds user-template pages for drum machine control, arp performance, and remote-control access to the Bitwig factory template.

Oikontrol is maintained primarily by its author and shaped around Bitwig composition and performance workflows. It is stable enough to use, but still actively evolving as the controller designs are refined.

## Download

Download the latest prebuilt extension:

[Oikontrol.bwextension](https://github.com/oikoaudio/bitwig-oikontrol/releases/latest/download/Oikontrol.bwextension)

Older versions and release notes are available on the [releases page](https://github.com/oikoaudio/bitwig-oikontrol/releases).

## Highlights

Oikontrol includes several opinionated workflows that go beyond basic mixer, transport, and device control:

- **Expressive note performance:** scale-aware pad layouts, harmonic input, and pitch/mod-style performance controls with snapback behavior.
- **Generative sequencing:** Nested Rhythm, Melodic Step generators, Chord Step, and Fugue transformations for creating and reshaping musical material from the controller.
- **Deep drum editing:** x0x sequencing with micro-timing, recurrence, velocity, and page navigation.
- **Quick gain staging:** peak/RMS meter readouts, max-level tracking, and quick max reset help set levels directly from the hardware.
- **OLED-guided control:** mode parameters and transient action feedback are shown directly on the Akai Fire OLED.
- **Performance device control:** the Perform Mix page can select devices, toggle device rows, open device windows, and navigate remote pages from the grid.
- **Controller-first launcher recording:** quick recording shortcuts with separate clip creation and launcher record length settings.

## Quick Install

1. Download the latest prebuilt [`Oikontrol.bwextension`](https://github.com/oikoaudio/bitwig-oikontrol/releases/latest/download/Oikontrol.bwextension).
2. Copy the file into Bitwig's user extensions folder:
   - Linux: `~/Bitwig Studio/Extensions/`
   - macOS: `~/Documents/Bitwig Studio/Extensions/`
   - Windows: `~/Documents/Bitwig Studio/Extensions/`
3. Open Bitwig `Settings` -> `Controllers`, add the controller, and select `Fire Oikontrol` or `LCXL Oikontrol`.

Inside Bitwig, press the `?` symbol from the controller extension to open the bundled HTML documentation.

## Akai Fire Workflows

The Fire script is built around four mode families, each with its own pages. Shared encoder pages, OLED feedback, and global settings tie the workflows together. Many modes share a musical context, so changing the root, scale, or octave can reshape live note input, generated melodic material, and chord-oriented workflows together.

- **`DRUM` family:** `Drum XOX`, `Nested Rhythm`, and `Drum Pads`.
  `Drum XOX` is the x0x-style Drum Machine sequencer, with micro-timing, recurrence, velocity and expression editing, Euclidean controls, accent/fill gestures, per-pad mixer controls, and peak/RMS metering for quick gain staging. `Nested Rhythm` generates layered deterministic rhythm patterns for tuplets, ratchets, asymmetric subdivisions, and editable generated hits. `Drum Pads` is the live Drum Machine surface, with 64-pad grid, velocity-zone, and bongo-style layouts.
- **`NOTE` family:** melodic note input and harmonic note input.
  Melodic input gives scale-aware isomorphic playing, chromatic or in-key layouts, shared octave control, note repeat access, and pitch/mod-style performance controls with snapback behavior. Harmonic input turns the grid into a scale-aware lattice for stacked notes, bass columns, octave expansion, and quick modal chord color.
- **`STEP` family:** `Melodic Step`, `Chord Step`, and `Fugue`.
  `Melodic Step` combines generated phrase engines with hands-on step, pitch-pool, recurrence, and expression editing. `Chord Step` builds chord progressions from in-scale builder notes or preset color banks. `Fugue` treats channel 1 as a source line and generates related voices on channels 2-4 for canon-like, retrograde, transposed, augmented, and diminished variations.
- **`PERFORM` family:** `Launcher`, `Scene Launch`, `Mix`, `Mix Devices`, `Device Layers`, and `Birds-Eye`.
  Launcher pages handle vertical or horizontal clip launching, scene launching, clip creation, pad-target recording, and next-free-slot recording. Mix pages cover select/solo/mute/arm, track stop, loop navigation, device selection, device row toggles, plugin window toggles, remote page navigation, device-layer mixing, bird's-eye navigation for large launcher sets, and track peak/RMS metering.
- **`SHIFT + BROWSER` settings:** a latched global overlay for shared pitch, input velocity feel, pad color response, default clip length, launcher record length, cursor pinning, and whether deactivated tracks are shown on the controller.

## Launch Control XL Workflows

The Launch Control XL extension starts from Bitwig's original factory-template behavior and extends it with additional user-template pages.

- **Factory-style control** remains available for the familiar mixer and device workflow.
- **Drum machine control** gives direct access to common drum-machine parameters.
- **Arp performance control** adapts the Richie Hawtin / Eric Ahrens `rhbitwig` workflow.
- **Remote-page control** targets 6-7 remote pages in one view, depending on the device layout.

## Documentation

- [docs/user-guide.md](docs/user-guide.md): canonical user guide source for both controllers.
- `CHANGES.md`: release notes and running changelog.
- Bundled HTML documentation is generated from the user guide and shipped inside the extension.

## Versioning

Oikontrol uses:

`major`.`minor`.`tick`

- `major` - a new controller extension, or an overhaul of core functionality.
- `minor` - a new mode, or significant changes to a mode within a controller.
- `tick` - bug fixes, documentation updates, and minor additions.

## For Developers

### Requirements

- **Java:** built and tested with JDK 21.
- **Bitwig API:** targets `extension-api:24`, which ships with **Bitwig Studio 5.3**. The extension should also work in Bitwig Studio 6.0; beta 12 has been tested.
- **Controllers:** Novation Launch Control XL Mk2 and Akai Fire.

### Building And Testing

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

You can also build directly with Gradle:

```bash
cd /path/to/bitwig-oikontrol
./gradlew clean build --no-daemon \
  -Dorg.gradle.java.home="$JAVA_HOME"
```

The built controller artifacts are placed under each module's `build/libs` directory. Unit tests live under `src/test/java`; `NoteInputConfiguratorTest` is an example of using Mockito to thinly mock Bitwig APIs.

Module-specific Gradle commands:

```bash
GRADLE_USER_HOME=/tmp/gradle-home ./gradlew :modules:akai-fire:build --no-daemon
GRADLE_USER_HOME=/tmp/gradle-home ./gradlew :modules:akai-fire:jar --no-daemon

GRADLE_USER_HOME=/tmp/gradle-home ./gradlew :modules:launchcontrol:build --no-daemon
GRADLE_USER_HOME=/tmp/gradle-home ./gradlew :modules:launchcontrol:jar --no-daemon
```

Current artifact names:

- `modules/oikontrol/build/libs/Oikontrol.bwextension` contains both controller extensions.
- `modules/akai-fire/build/libs/FireOikontrol.bwextension`.
- `modules/launchcontrol/build/libs/LCXLOikontrol.bwextension`.

### Developer Documentation

- `CONTRIBUTING.md`: contribution workflow and Conventional Commit guidance.
- `docs/adr/`: Architectural Decision Records, including `0000-use-adrs.md` and `0001-architecture-summary.md`.
- `docs/dev/`: active implementation notes, experiments, and workflow documentation.
- `vscode-debug.md`: how to attach VS Code's debugger to Bitwig with JDWP while running the extension.

## Attribution

- Based on Bitwig's original Launch Control XL controller script, licensed under the MIT License, from [bitwig-extensions](https://github.com/bitwig/bitwig-extensions). The helper packages (`com.bitwig.extensions.framework`, `...controllers.novation.common`, `...util`) remain under Bitwig's MIT terms.

- The LCXL Arp workflow on User Template 8 is adapted from Richie Hawtin and Eric Ahrens' [rhbitwig](https://github.com/ericahrens/rhbitwig) project, also MIT licensed.

- The Akai Fire extension started as a fork of the drum sequencer in [rhbitwig](https://github.com/ericahrens/rhbitwig) by Richie Hawtin and Eric Ahrens, MIT licensed.

- Fine-grid step nudging is based on Wim Van den Borre's [Akai Fire Nudger](https://github.com/wimvandenborre/AkaiFireNudger/), adapted and developed further here for the sequencer workflows.

Please keep these acknowledgements if you redistribute or build on this project.
