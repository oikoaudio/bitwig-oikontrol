# Change log

This document now tracks intentional modifications made to the `bitwig-oikontrol` project.

## 2.1.0 - Akai Fire melodic generation and workflow pass
- Added `Melodic Step` mode under `STEP` with phrase engines for `Acid`, `Motif`, `Call/Resp`, `Euclid`, `Rolling`, and `Octave`
- Added editable melodic pitch-pool generation, mutation, note audition, and cleaner preservation of manual pool edits
- Reworked Fire encoder banks and step-sequencer controls to align behavior across drum, note, chord, and melodic workflows
- Added step paging plus fine-nudging for chord sequencing, while leaving unreliable rotation behavior disabled for now
- Added default note-input octave control, stronger OLED and pad feedback, and generator family feedback during melodic generation
- Updated docs, tests, and Fire workflow preferences to match the 2.1 sequencing pass

## 2.0.0 - Akai Fire and multi-controller repo
Adds a full Akai Fire extension and promotes `bitwig-oikontrol` into a multi-controller repository:
- Added a full Akai Fire extension under `modules/akai-fire`
- Added Fire `DRUM`, `NOTE`, and `PERFORM` workflows
- Added drum step sequencing with fine nudge, accent/fill handling, and Euclid controls
- Added note mode with isomorphic layout, scale/root/octave controls, and note-step access
- Added Oikord chord-step sequencing and performance clip launching
- Added Fire OLED feedback, encoder pages, transport/tempo control, and preferences
- Restructured the repository into a multi-module `bitwig-oikontrol` project for Akai Fire and Launch Control XL
- Continued Launch Control XL improvements, including device remote pages and drum-mode refinements
- Updated docs, tests, and build tooling

## 1.0.0 — Novation Launch Control XL Oikontrol script
Mirrors the extension for the Novation Launch Control XL mk2 that ships with Bitwig, with the following additions:
- Dedicated User Template 8 incorporates Eric Ahrens' and Richie Hawtin's ARP as implemented in the rhbitwig extension
- User templates 1–7 pass raw MIDI into Bitwig so they can be mapped to plugins using MIDI CC mapping,
  or to Bitwig targets using the project MIDI mapping functionality
- LEDs indicate factory mode knob values (off, low, high)
