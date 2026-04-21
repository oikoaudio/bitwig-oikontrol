# Bitwig Oikontrol

Bitwig Oikontrol is a pair of controller extensions for Bitwig Studio v5.3+:

- Novation Launch Control XL Mk2
- Akai Fire

> [!NB]
> These controller scripts are under active development. Behavior, mappings, and mode workflows may change between versions, and some functions are still in flux.

Semantic versioning:
`major`.`minor`.`tick`

`major` - new controller extension, or overhaul of core functionality

`minor` - new mode or changes to a mode within a controller

`tick` - bug fixes and minor additions

## For users

- User guide source: [doc/user-guide.md](doc/user-guide.md)
- Inside Bitwig, press the `?` symbol to open the bundled HTML documentation.

High-level controller summary:

- **Launch Control XL (Mk2)** keeps the Bitwig factory-template workflow intact, while adding user-templates for drum machine control, the Richie Hawtin / Eric Ahrens arp workflow from `rhbitwig` and a mode targeting 6-7 remote pages in one view.

- **Akai Fire** provides OLED and optional on-screen feedback for most parameters, shared root/scale/octave control, step micro-timing, configurable device pinning, and a broader multi-workflow surface built around the Fire’s mode buttons:

`DRUM` - standard x0x drum sequencing, plus a `Nested Rhythm` generative sequencer for layered rhythmic structures

`NOTE` - two live note input modes with isomorphic and harmonic layouts

`STEP SEQ` - `Melodic Step` and `Chord Step` workflows for generated melodic sequencing and chord-based step entry

`PERFORM` - clip launching, clip management, and a `SHIFT + PERFORM` track-action page

`SHIFT + BROWSER` - a held global settings overlay for shared `Root Key`, `Scale`, and `Octave`

## Documentation

- `doc/user-guide.md`: canonical user guide source for both controllers
- `CHANGES.md`: release notes and running changelog

## Quick install

1. Download a prebuilt `.bwextension` artifact from the releases section
2. Copy the resulting file into Bitwig’s user extensions folder:
   - Linux: `~/Bitwig Studio/Extensions/`
   - macOS: `~/Documents/Bitwig Studio/Extensions/`
   - Windows: `~/Documents/Bitwig Studio/Extensions/`
3. Open Bitwig `Settings` -> `Controllers`, add the controller, and select `Oikontrol Fire` or `OikontrolLCXL`.

## For developers

### Requirements

- **Java:** built and tested with JDK 21.
- **Bitwig API:** targets `extension-api:24`, which ships with **Bitwig Studio 5.3**. The extension should also work in 6.0 (tested beta 12)
- **Controllers:** Novation Launch Control XL Mk2, Akai Fire

### Building & testing

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
- `modules/akai-fire/build/libs/OikontrolFire.bwextension`
- `modules/launchcontrol/build/libs/OikontrolLCXL.bwextension`

### Developer documentation

- Architectural Decision Records under `doc/adr/`, for example `0000-use-adrs.md` and `0001-architecture-summary.md`
- `vscode-debug.md`: how to attach VS Code’s debugger to Bitwig (JDWP) for inspection while running the extension

## Attribution

- Based on Bitwig’s original Launch Control XL controller script (MIT License) from
  [bitwig-extensions](https://github.com/bitwig/bitwig-extensions). The helper packages
  (`com.bitwig.extensions.framework`, `…controllers.novation.common`, `…util`) remain under Bitwig’s MIT terms.

- The LCXL Arp workflow on User Template 8 is adapted from Richie Hawtin and Eric Ahrens’ [rhbitwig](https://github.com/ericahrens/rhbitwig) project (also MIT).

- The Akai Fire extension started as fork of the drum sequencer in [rhbitwig](https://github.com/ericahrens/rhbitwig) by Richie Hawtin and Eric Ahrens (MIT license).

- Fine-grid step nudging is based on Wim Van den Borre's Akai Fire Nudger work [https://github.com/wimvandenborre/AkaiFireNudger/], adapted and developed further here for the sequencer workflows.

Please keep these acknowledgements if you redistribute or build on this project.
