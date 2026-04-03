# Change log

This document now tracks intentional modifications made to the `bitwig-oikontrol` project.

## 1.1.0 - Akai Fire
Adds an Akai Fire control surface based on the rhbitwig fork with Oiko tweaks:
- Separate Gradle module (`modules/akai-fire`) with new UUID and name “Akai Fire by Oiko Audio” to avoid collision with the rhbitwig extension on which is it based
- Nudge: held-step micro-nudge disabled; Grid = coarse 16th shift, Shift+Grid = fine nudge on selected pad
- Euclid mode: User2 encoders = LEN/PULS/ROT/INV with gentler steps; Browser applies (Alt clears+applies); patterns tile across clip length; gate tightened to avoid insertDuration errors
- Play: Alt+Play retriggers current clip; Play toggles transport and retriggers on start
- OLED encoder info strings updated

Novation Launch control XL addition:
- User template 7: Drum Machine Mode
- User template 6: Device Remotes mode (6 pages of remotes)

## 1.0.0 — Novation Launch Control XL Oikontrol script
Mirrors the extension for the Novation Launch Control XL mk2 that ships with Bitwig, with the following additions:
- Dedicated User Template 8 incorporates Eric Ahrens' and Richie Hawtin's ARP as implemented in the rhbitwig extension
- User templates 1–7 pass raw MIDI into Bitwig so they can be mapped to plugins using MIDI CC mapping,
  or to Bitwig targets using the project MIDI mapping functionality
- LEDs indicate factory mode knob values (off, low, high)
