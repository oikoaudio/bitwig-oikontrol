# Bitwig Oikontrol

Bitwig Oikontrol is a pair of controller extensions for Bitwig Studio v5.3+:

- Novation Launch Control XL Mk2
- Akai Fire

> [!NB]
> These controller scripts are under active development. Behavior, mappings, and mode workflows may change between versions, and some functions are still in flux.

Semantic versioning:
major.minor.tick

major - new controller extension, or overhaul of core functionality
minor - new mode or changes to a mode within a controller
tick - bug fixes and minor additions

## User guide

- Canonical guide source: `doc/user-guide.md`
- Bundled help inside Bitwig:
  - `Oikontrol LCXL` opens `index.html`
  - `Oikontrol Fire` opens `index.html`

High-level controller summary:

- **Launch Control XL (Mk2)** keeps the factory-template workflow intact while adding user-template support for device pages, drum machine control, and the Richie Hawtin / Eric Ahrens arp workflow from `rhbitwig`.
- **Akai Fire** provides three top-level modes: `DRUM`, `NOTE`, and `PERFORM`, with clip sequencing, note play, Oikord note-step workflows, clip launching, OLED feedback, and configurable pinning and encoder roles.

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
- `modules/akai-fire/build/libs/OikontrolFire.bwextension`
- `modules/launchcontrol/build/libs/OikontrolLCXL.bwextension`

## Documentation
- `doc/user-guide.md`: canonical user guide source for both controllers.
- Architecural Decision Record (ADR) under `doc/adr/` e.g. `0000-use-adrs.md` (process) and `0001-architecture-summary.md` (factory/user modes, Template 8 arp, Java/API targets).
- `CHANGES.md`: running changelog of code edits.
- `vscode-debug.md`: how to attach VS Code’s debugger to Bitwig (JDWP).

## Attribution

- Based on Bitwig’s original Launch Control XL controller script (MIT License) from
  [bitwig-extensions](https://github.com/bitwig/bitwig-extensions). The helper packages
  (`com.bitwig.extensions.framework`, `…controllers.novation.common`, `…util`) remain under Bitwig’s MIT terms.

- The LCXL Arp workflow on User Template 8 is adapted from Richie Hawtin and Eric Ahrens’ [rhbitwig](https://github.com/ericahrens/rhbitwig) project (also MIT).

- The Akai Fire extension started as fork of the drum sequencer in [rhbitwig](https://github.com/ericahrens/rhbitwig) by Richie Hawtin and Eric Ahrens (MIT license).

- Fine-grid step nudging is based on Wim Van den Borre's Akai Fire Nudger work [https://github.com/wimvandenborre/AkaiFireNudger/], adapted and developed further here for the sequencer workflows.

Please keep these acknowledgements if you redistribute or build on this project.
