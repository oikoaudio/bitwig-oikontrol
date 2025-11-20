# Launch Control XL Oikontrol

Launch Control XL Oikontrol is a self-contained fork of Bitwig’s factory Launch Control XL (Mk2) script. It keeps every
factory template intact, unlocks the user templates for Bitwig mappings, and dedicates User Template 8 to the
Richie Hawtin / Eric Ahrens arp workflow while templates 1–7 pass raw MIDI through to Bitwig. This extension also uses the LEDs under each knob to indicate each value (off, low, high).

## Requirements

- **Java:** built and tested with JDK 21.
- **Bitwig API:** targets `extension-api:24`, which ships with **Bitwig Studio 5.3** and later. The extension should also work in 6.0 (tested beta 6)
- **Controller:** Novation Launch Control XL Mk2.

## Building & testing

All sources—including the Bitwig framework helpers—live inside this repository. 

```bash
cd /path/to/bitwig-launchcontrolmk2-oiko
./gradlew clean build --no-daemon \
  -Dorg.gradle.java.home="$JAVA_HOME"
```

The resulting `.bwextension` artifact is placed under `oiko-launchcontrol/build/libs`. Unit tests are located in `src/test/java`; `NoteInputConfiguratorTest` shows how Mockito is used to (thinly) mock Bitwig APIs.

## Documentation

- ADRs under `doc/adr/` – e.g., `0000-use-adrs.md` (process) and `0001-architecture-summary.md` (factory/user modes, Template 8 arp, Java/API targets).
- `CHANGES.md` – running changelog of code edits.
- `BUILD.md` – more detail on local builds and installation.
- `vscode-debug.md` – how to attach VS Code’s debugger to Bitwig (JDWP).

## Attribution

- Based on Bitwig’s original Launch Control XL controller script (MIT License) from
  [bitwig-extensions](https://github.com/bitwig/bitwig-extensions). The helper packages
  (`com.bitwig.extensions.framework`, `…controllers.novation.common`, `…util`) remain under Bitwig’s MIT terms.

- The arp workflow is adapted from Eric Ahrens’ [rhbitwig](https://github.com/rhbitwig/rhbitwig) project (also MIT).
  User Template 8 intentionally reproduces the Richie Hawtin Arp layout while the rest of the controller keeps the stock Bitwig experience.

Please keep these acknowledgements if you redistribute or build on this project.

# Known issues:
- The Arp "custom scale mode" filter that removes notes of a given pitch does not work as expected yet