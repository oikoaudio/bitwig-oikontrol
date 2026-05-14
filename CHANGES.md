# Change log

This document now tracks intentional modifications made to the `bitwig-oikontrol` project.

## [2.14.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.13.0...oikontrol-v2.14.0) (2026-05-14)


### Features

* add controller navigation and clip recording controls ([#31](https://github.com/oikoaudio/bitwig-oikontrol/issues/31)) ([b15ffc6](https://github.com/oikoaudio/bitwig-oikontrol/commit/b15ffc6f19c39195d537c0c7fb5ef4495a8f888e))
* add nested rhythm clustering controls ([#36](https://github.com/oikoaudio/bitwig-oikontrol/issues/36)) ([d4d0572](https://github.com/oikoaudio/bitwig-oikontrol/commit/d4d0572fac9cef17303ee661813cdba45fa77f32))
* **fire:** add Akai Fire DRUM Grid64, Velocity, and Bongos layouts ([#35](https://github.com/oikoaudio/bitwig-oikontrol/issues/35)) ([7481a8f](https://github.com/oikoaudio/bitwig-oikontrol/commit/7481a8f3f16509bc03c54b76a81ab60a5c180ab4))
* **fire:** improve nested rhythm generation ([#38](https://github.com/oikoaudio/bitwig-oikontrol/issues/38)) ([3a99e1a](https://github.com/oikoaudio/bitwig-oikontrol/commit/3a99e1a0b1e71c939d2972ee5e77a1d5f3877ea6))
* rework nested rhythm ranking and density controls ([#40](https://github.com/oikoaudio/bitwig-oikontrol/issues/40)) ([793d3bf](https://github.com/oikoaudio/bitwig-oikontrol/commit/793d3bfc532793c2266312faed3affcecc6e07ce))


### Bug Fixes

* **fire:** track selection got stuck on hidden tracks ([#33](https://github.com/oikoaudio/bitwig-oikontrol/issues/33)) ([93fcaa1](https://github.com/oikoaudio/bitwig-oikontrol/commit/93fcaa1fe3164d38c853ac243ba0b0e00a030025))

## [2.13.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.12.0...oikontrol-v2.13.0) (2026-05-14)


### Features

* **fire:** improve nested rhythm generation ([#38](https://github.com/oikoaudio/bitwig-oikontrol/issues/38)) ([3a99e1a](https://github.com/oikoaudio/bitwig-oikontrol/commit/3a99e1a0b1e71c939d2972ee5e77a1d5f3877ea6))
* rework nested rhythm ranking and density controls ([#40](https://github.com/oikoaudio/bitwig-oikontrol/issues/40)) ([793d3bf](https://github.com/oikoaudio/bitwig-oikontrol/commit/793d3bfc532793c2266312faed3affcecc6e07ce))

## [2.12.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.11.0...oikontrol-v2.12.0) (2026-05-02)


### Features

* add nested rhythm clustering controls ([#36](https://github.com/oikoaudio/bitwig-oikontrol/issues/36)) ([d4d0572](https://github.com/oikoaudio/bitwig-oikontrol/commit/d4d0572fac9cef17303ee661813cdba45fa77f32))

## [2.11.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.10.0...oikontrol-v2.11.0) (2026-05-02)


### Features

* **fire:** add Akai Fire DRUM Grid64, Velocity, and Bongos layouts ([#35](https://github.com/oikoaudio/bitwig-oikontrol/issues/35)) ([7481a8f](https://github.com/oikoaudio/bitwig-oikontrol/commit/7481a8f3f16509bc03c54b76a81ab60a5c180ab4))


### Bug Fixes

* **fire:** track selection got stuck on hidden tracks ([#33](https://github.com/oikoaudio/bitwig-oikontrol/issues/33)) ([93fcaa1](https://github.com/oikoaudio/bitwig-oikontrol/commit/93fcaa1fe3164d38c853ac243ba0b0e00a030025))

## [2.10.0](https://github.com/oikoaudio/bitwig-oikontrol/compare/oikontrol-v2.9.0...oikontrol-v2.10.0) (2026-04-26)


### Features

* add controller navigation and clip recording controls ([#31](https://github.com/oikoaudio/bitwig-oikontrol/issues/31)) ([b15ffc6](https://github.com/oikoaudio/bitwig-oikontrol/commit/b15ffc6f19c39195d537c0c7fb5ef4495a8f888e))

## 2.9.0 - Perform scene launch page and Rec+pad recordin
- New Perform scene launch page (toggle on Perform)
- REC + pad recording
- ALT + PERFORM toggles horizontal/vertical clip launch pad view
- Renamed the controller display name (Fire Oikontrol) to fit in better with other controllers
- Distribute a single bwextension built from the two modules. If you want the individual controller extension those can still be built from source using the just-commands

## 2.8.0 - Fugue mode and generative workflow polish
- Added Akai Fire `Fugue` step mode. It uses MIDI channel 1 as a template melodic line and generates derived new lines to channels 2-4 in the same clip, using Bach-type operations (transpose/reverse/move etc).
- Added `REC + pad` in Akai Fire `PERFORM` mode to target recording directly into a visible launcher slot using the configured default clip length.
- Moved Akai Fire `PERFORM` vertical/horizontal launcher layout switching to `ALT + PERFORM`.
- Added a latched Akai Fire `PERFORM` `Scene Launch` pad page with top-row scene launch, select, copy, and delete actions.
- Swapped Nested Rhythm pattern gestures so `PATTERN DOWN` generates the current nested rhythm and `PATTERN UP` resets hit edits, to better match related gestures in other modes.
- Updated the user guide and bundled help with notes on the new Fugue workflow.

## 2.7.0 - Nested Rhythm mode and global control fixes
- Added a new Akai Fire `Nested Rhythm` drum mode for generating rhythm from nested segment divisions rather than fixed-grid step placement. The mode slices segments into symmetric and asymmetric subdivisions (tuplets and ratches) in a musically interesting way, and adds expression layer profiles for pressure, timbre, velocity, and chance that can be rotated across the generated hits. Rhythmic structures can be changed on the fly, generated hits can be embellished with per-hit edits, and recurrence can be generated or edited using the same gesture as in `Melodic Step`.
- (improvement) `PERFORM` now exits the `SHIFT + PERFORM` track-action page for easier exit
- `SHIFT + BROWSER` global-settings access is more robust, including in Drum Seq
- global root/scale/octave settings now use slower stepped encoders, clamped scale bounds, and safer scale lookup handling

## 2.6.0 - Perform track actions and encoder behavior refinement
- Added a `SHIFT + PERFORM` track-action page for `Stop`, `Solo`, `Mute`, and `Arm`
- Unified Akai Fire encoder turn and touch-reset behavior behind shared control helpers
- Added shared encoder value profiles for parameter-style controls and aligned more encoder paths with the shared behavior layer
- Softened live and step expression encoder feel for pressure, timbre, and related expression controls
- Reduced encoder touch-reset hold from `1000 ms` to `750 ms`
- Aligned live note timbre reset/default with zero-based semantics instead of centering on `64`
- Renamed active chord-step bank/state terminology from `Oikord` to `Chord`

## 2.5.0 - Harmonic note input, step routing, and control-surface polish
- Added a harmonic live `NOTE` submode with harmonic lattice layout, multi-note pad output, note-count selection, octave-span control, bass-column/full-field variants, and harmonic gliss support
- Added live-note pitch-bend with spring-return, and toggle between `5th/8v` and `ScaleDeg` gliss modes
- Moved `Chord Step` behind `STEP SEQ` so `NOTE` stays focused on live note input; pressing `STEP SEQ` now enters `Melodic Step`, then switches to `Chord Step` on the next press
- Added `ALT + NOTE` and `SHIFT + Encoder 4` as direct live-layout shortcuts for collapsing note input to scale notes or switching harmonic layout variants
- Added horizontal `Perform` orientation plus broader encoder-scaling cleanup, melodic step expression-page updates, and Bitwig 6-aligned scale naming/order
- Reworked Fire modifier routing so `SHIFT + PERFORM` latches a 4-row track-action page (`Stop`, `Solo`, `Mute`, `Arm`) and `SHIFT + BROWSER` opens a global root/scale/octave settings overlay
- Updated tests and Fire documentation to match the new harmonic input, step-routing, and live-control workflows

## 2.4.0 - Akai Fire shared pitch context and architecture rework
- Added a unified shared musical context for `Root Key`, `Scale`, and `Octave` across live `NOTE`, `Chord Step`, `Melodic Step`, and `SHIFT + PERFORM`
- Added melodic recurrence editing with held-step targeting, top-row recurrence gestures, recurrence feedback, and supporting tests
- Reworked Fire mode architecture by separating live note play from chord sequencing, extracting dedicated controllers/helpers, and simplifying duplicated framework code
- Expanded `SHIFT + PERFORM` overview editing for shared pitch controls and refreshed note/chord workflows to use the shared context consistently
- Updated `Chord Step` interpretation and melodic generator workflows, plus docs and bundled help, to reflect the new shared-pitch model

## 2.3.1 - Akai Fire selected clip sync fix
- Fixed bidirectional sync of selected clip state so shared clip selection follows the actual Bitwig `isSelected()` state instead of the controller cursor index
- Updated chord and melodic step refresh paths to trust an already selected clip before falling back to the playing clip
- Prevented passive refresh from needlessly reselecting clip slots

## 2.3.0 - Akai Fire shared sequencer clip row and step-workflow polish
- Added a shared sequencer clip row across Drum, Chord Step, and Melodic Step workflows
- Polished `Melodic Step` generation and editing behavior, including safer held-step and accent interactions
- Added reusable accent-latch and clip-row helper logic to stabilize sequencer behavior across modes
- Fixed browser-selection behavior and refreshed documentation for the updated Fire sequencing workflow

## 2.2.0 - Akai Fire shared pitch and safer performance workflows
- Added live NOTE `Pitch Gliss` with held-note retuning and a dedicated velocity response model based on `Velocity Sensitivity` and `Default Velocity`
- Added shared global `Root Key` and `Scale` across live NOTE, Chord Step, and the latched `SHIFT + PERFORM` `Settings` page
- Added Fire startup preferences for `Default Root Key` and `Default Velocity Sensitivity`, and narrowed the default note-input octave choices to `2`, `3`, and `4`
- Applied the same velocity-sensitivity model to Chord Step and made its builder default to in-key with visible-only auto-seeding
- Added clearer safety checks in Chord Step and Melodic Step for missing clips, wrong track types, hanging live notes, and explicit clip clearing
- Added relative clip halve/double gestures on `ALT + BANK LEFT/RIGHT` across Drum, Chord Step, and Melodic Step workflows
- Added a latched Fire `Settings` mode with dedicated OLED/button feedback and refreshed Perform encoder-page labeling
- Updated tests and Fire documentation to match the new shared pitch, settings, and sequencing workflow

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
- Added chord-step sequencing and performance clip launching
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
