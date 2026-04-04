# Akai Fire Controller Layout

This document is the working reference for the intended Akai Fire control layout in the Oikontrol script.

It describes the target layout to build toward. Some items are already implemented, some are planned, and a few remain intentionally provisional until the surrounding mode design is complete.

## Top-Level Modes

The three large mode buttons on the Fire become the primary mode selectors:

| Button | Role | Notes |
| --- | --- | --- |
| `DRUM` | Main sequencer mode | Repeated press cycles Drum sub-modes |
| `NOTE` | Note input mode | Scale or chromatic note playing |
| `PERFORM` | Clip launcher / performance mode | Inspired by the DrivenByMoss performance workflow |

## Drum Sub-Modes

`DRUM` is a top-level mode and also the entry point for Drum sub-modes.

| Action | Result |
| --- | --- |
| Press `DRUM` from another top-level mode | Enter standard Drum mode |
| Press `DRUM` again while already in Drum mode | Advance to next Drum sub-mode |

Planned Drum sub-modes:

| Sub-mode | Intended LED color | Purpose |
| --- | --- | --- |
| Standard Drum | `TBD` | Main drum sequencer workflow |
| Polyrhythm Drum | `TBD` | One row per lane with independent lengths and per-row generation |

Exact colors are still to be chosen based on what the Fire LEDs can represent clearly.

## Global Buttons

These mappings should remain consistent across the top-level modes unless noted otherwise.

| Control | Target role | Notes |
| --- | --- | --- |
| `PLAY` | Toggle transport | Existing behavior |
| `ALT + PLAY` | Retrigger current clip | Already implemented and worth keeping |
| `STOP` | Stop transport | Existing behavior |
| `REC` | Toggle clip launcher overdub | Existing behavior |
| `SHIFT` | Global modifier | Used for alternate actions |
| `ALT` | Global modifier | Used for alternate actions |
| `BROWSER` | Global popup browser | Available in all top-level modes |
| Main select encoder | Preference-backed main encoder | `Last Touched Parameter`, `Shuffle`, or `Note Repeat` |
| Main encoder press | Role-specific action | Reset last-touched parameter, toggle groove, toggle note repeat, or commit browser selection |

## Main Select Encoder

The large select encoder defaults to controlling the real last clicked/touched Bitwig parameter via `LastClickedParameter`.

Preference:

| Preference | Options |
| --- | --- |
| `Main Encoder Role` | `Last Touched Parameter`, `Shuffle`, `Note Repeat` |

Behavior:

| Action | Result |
| --- | --- |
| Turn main encoder | Change last touched parameter |
| Press main encoder | Reset the current last touched parameter to its default value |

Additional encoder-role behavior:

| Role | Turn | Press | OLED |
| --- | --- | --- | --- |
| `Shuffle` | Adjust global groove shuffle amount | Toggle groove on/off | Shuffle amount or on/off |
| `Note Repeat` | Change note repeat value while active | Toggle note repeat on/off | Current note repeat value |
Role management:

| Action | Result |
| --- | --- |
| `SHIFT + press SELECT` | Cycle the persistent main encoder role |
| `SHIFT + turn SELECT` | Fine adjustment for continuous roles |
| Accent hold | Temporarily overrides the encoder for accent velocity editing |

## Drum Mode Layout

Standard Drum mode is built around a fixed 4-row pad layout:

| Pad row | Role |
| --- | --- |
| Row 1 | Clip slots |
| Row 2 | Drum slots / visible drum pads |
| Rows 3-4 | 32-step sequencer for the selected drum lane |

This replaces the current preference-driven choice between clip-row and mute-row behavior.

## Drum Mode Buttons

### Left Side Buttons

| Button | Primary role | Shift role |
| --- | --- | --- |
| `MUTE_1` | Select / row-target action | Mute mode or alternate row action |
| `MUTE_2` | Last step / fixed length | Solo or alternate row action if retained |
| `MUTE_3` | Copy | None planned |
| `MUTE_4` | Delete / reset | None planned |

These may be refined further once the row-oriented polyrhythm mode is implemented.

### Bottom Row Buttons

| Button | Role in Drum mode | Notes |
| --- | --- | --- |
| Hold `STEP SEQ` | Accent gesture mode | Hold and tap steps to add or toggle accented notes |
| `SHIFT + STEP SEQ` | Fill | Secondary action while Accent gets the prime gesture |
| `PATTERN` | Configurable utility | Default should be Clip Launcher Automation Write |
| `SHIFT + PATTERN` | Metronome | Fixed default matching the printed controller label |
| `BROWSER` | Global popup browser | Plain = replace/current context, `SHIFT` = insert before, `ALT` = insert after |

### Grid Arrows

The two top-right buttons with `GRID` printed between them and left/right arrows on the button caps stay as the main timing-edit pair. In code these are `BANK_L` and `BANK_R`.

| Action | Result |
| --- | --- |
| `GRID` left / right arrows | Coarse pattern shift |
| `SHIFT + GRID` left / right arrows | Fine nudge |
| `ALT + GRID` left / right arrows | Grid resolution down / up |

This keeps timing-related actions grouped on one pair of buttons.

Implementation note:

- Holding one or more step pads while pressing the `GRID` left / right arrows fine-nudges those held notes directly, without needing `SHIFT`.
- While the pad remains held, repeated nudges stay attached to that same note start rather than re-targeting by nearest grid match.
- After release, ownership returns to the currently visible coarse grid. A note nudged earlier can therefore appear to belong to the previous step until the grid resolution is increased.

## Popup Browser Controls

When the popup browser is open in any top-level mode, the main select encoder takes over popup-browser result navigation:

| Control | Browser action |
| --- | --- |
| `SELECT` turn | Move through the popup browser results |
| `SELECT` press | Commit the selected result |
| `BROWSER` | Close the popup browser |

Behavior notes:

- The OLED shows the currently selected browser result while turning `SELECT`.
- `PATTERN` and the `GRID` arrows keep their normal Drum-mode functions; they no longer add extra popup-browser navigation state.
- The Browser button and popup result browsing are global controller functions, not Drum-mode-only behavior.

## Euclid Controls

Euclid is intended as a simplified Drum-mode generator, not a separate commit workflow.

| Control | Role |
| --- | --- |
| Euclid length encoder | Change pattern length |
| Euclid density encoder | Change pulses / density |

Design notes:

- Remove `ROT`
- Remove `INV`
- Apply immediately when length or pulse changes
- Clear and rewrite only when the effective Euclid state changes
- Keep grid-button shifting as the practical replacement for rotation

## Note Mode

`NOTE` is now split into live note play plus note-step sub-modes.

### Live Note Play

Default `NOTE` behavior:

| Control area | Role |
| --- | --- |
| Pad matrix | 16x4 isomorphic note grid |
| Pad LEDs | Root, in-scale, and out-of-scale note highlighting |
| `DRUM` | Return to Drum mode |
| `PERFORM` | Switch to Perform mode |
| `NOTE` | Toggle `Chromatic` / `In Key` layout |
| `STEP SEQ` | Enter the current note-step sub-mode |
| `SHIFT + STEP SEQ` | Cycle note-step sub-mode and enter it |
| `PATTERN` up / down | Root note up / down |
| `BANK` left / right | Octave down / up |
| `MUTE_1` / `MUTE_2` | Previous / next scale |
| User encoders 1-4 | Root, scale, octave, layout toggle |

OLED feedback shows the current layout, root, octave, and scale whenever one of those values changes.

### Note Step Sub-Modes

Available sub-modes:

| Sub-mode | Status | Purpose |
| --- | --- | --- |
| `Oikord Step` | Implemented first pass | Curated harmonic step assignment |
| `Clip Step Record` | Deferred placeholder | Future ordinary note-into-clip step mode |

`STEP SEQ` enters the current note-step sub-mode from live note play.
While a note-step sub-mode is active, `NOTE` returns to live note play.

### Oikord Step

`Oikord Step` repurposes the pad grid and several nearby controls:

| Control | Role |
| --- | --- |
| Upper two pad rows | 32 visible curated Oikord slots |
| Lower two pad rows | 32 visible steps |
| Hold lower-row step(s) + tap upper-row Oikord | Assign selected Oikord to those steps |
| Tap upper-row Oikord with no held steps | Audition the Oikord if the preference is enabled |
| `STEP SEQ` while already in `Oikord Step` | Toggle `As Is` / `Cast` rendering |
| `SHIFT + STEP SEQ` | Cycle note-step sub-mode |
| `PATTERN` up / down | Page within the active Oikord family across two pages |
| `MUTE_1` / `MUTE_2` | Oikord octave offset down / up |
| `MUTE_3` / `MUTE_4` | Oikord root offset down / up |
| `GRID` left / right (`BANK_L` / `BANK_R`) | Move step content left / right |
| `SHIFT + GRID` left / right | Fine nudge held or selected notes |
| Encoder 1 | Oikord root offset |
| Encoder 2 | Oikord octave offset |
| Encoder 3 | Select active Oikord family |
| Encoder 4 | Reserved / no-op for now |

Behavior notes:

- Eight curated families are exposed: `Barker`, `Audible`, `SUSMOTION`, `QUARTALCOLOR`, `CLUSTERLIGHT`, `MINORDRIFT`, `DORIANLIFT`, and `ROOTDRONE`
- Each curated family preserves its full 64-chord traversal
- The upper rows show 32 visible slots at a time, grouped visually in blocks of 8 for recognition
- Oikord assignment writes literal notes directly into the clip
- `As Is` keeps the source voicing literally transposed
- `Cast` renders the Oikord through the Fire's local note-scale state
- Oikord root and octave offsets persist until changed and affect new assignments and auditions
- Oikord audition is controlled by a preference: `Audition Oikords`
- Oikord selection OLED text shows family, chosen voicing name, page, rendering mode, and current root/octave offsets
- Shared step encoder pages now apply in `Oikord Step`: `Channel = Velocity/Pressure/Timbre/Pitch Expression`, `Mixer = Volume/Pan/Send 1/Send 2`, `User 1 = Note Length/Chance/Velocity Spread/Repeat`, `User 2 = Oikord-specific root/octave/family`

Explicitly deferred from this first pass:

- piano layout
- Bitwig host-scale follow
- full Clip Step Record behavior
- broader performance-note features

## Perform Mode

`PERFORM` is now a top-level `16x4` clip launcher and performance mode.

Current layout:

| Area / Control | Role |
| --- | --- |
| Pad matrix | Global `16x4` clip grid |
| Filled slot pad | Select and launch the clip |
| Empty slot pad | Create a new `4`-bar clip and launch it |
| Pad LEDs | Clip color plus queued / playing / recording indication |
| `MUTE_1` | Select modifier |
| `MUTE_2` | Duplicate selected visible clip and double its length |
| `MUTE_3` | Copy from the selected visible clip |
| `MUTE_4` | Delete clip |
| `BANK_L` / `BANK_R` | Scroll tracks by visible page |
| `SHIFT + BANK_L` / `SHIFT + BANK_R` | Scroll tracks by `1` |
| `PATTERN` up / down | Scroll scenes by visible page |
| `SHIFT + PATTERN` up / down | Scroll scenes by `1` |
| `KNOB_MODE` | Cycle Perform encoder pages |

Perform encoder pages:

| Page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Project Remote 1 | Project Remote 2 | Project Remote 3 | Project Remote 4 |
| `Mixer` | Selected Track Volume | Selected Track Pan | Selected Track Send 1 | Selected Track Send 2 |
| `User 1` | Selected Track Remote 1 | Selected Track Remote 2 | Selected Track Remote 3 | Selected Track Remote 4 |
| `User 2` | Selected Device Remote 1 | Selected Device Remote 2 | Selected Device Remote 3 | Selected Device Remote 4 |

Behavior notes:

- Touching any bound top encoder resets that parameter to its default value.
- `Perform` currently focuses on clip launch, slot selection, copy/delete, and fast remote access rather than scene launch, stop rows, or deeper transport workflows.
- This replaces the previous temporary use of `PERFORM` as a grid-resolution modifier.

## Pinning

Pinning the Fire to the current track, clip, or device context is still desirable.

Current recommendation:

| Approach | Status |
| --- | --- |
| Preference-driven auto pinning | Preferred |
| Dedicated manual pin button | Deferred |

The exact preference names are still open, but pinning should be controlled by settings before it consumes a prime hardware button.

## Note Repeat

Note repeat no longer occupies the `BROWSER` button. It remains available as an optional main encoder role.

## Open Decisions

These points are still intentionally open:

- exact LED colors for Drum sub-modes
- exact left-side button behavior once polyrhythm mode is implemented
- exact pin preferences
- exact Note-mode step-input behavior
- whether solo remains part of the row workflow or is dropped

## Summary

The intended high-level model is:

| Button | Meaning |
| --- | --- |
| `DRUM` | Sequencing |
| `NOTE` | Playing notes |
| `PERFORM` | Launching / performing clips |

And within Drum mode:

| Control | Meaning |
| --- | --- |
| Hold `STEP SEQ` | Accent gesture mode |
| `SHIFT + STEP SEQ` | Fill |
| `PATTERN` | Configurable utility |
| `SHIFT + PATTERN` | Metronome |
| `GRID` left / right arrows | Shift pattern |
| `SHIFT + GRID` left / right arrows | Fine nudge |
| `ALT + GRID` left / right arrows | Grid resolution |

This is the layout to refer back to while implementing the next Fire iterations.
