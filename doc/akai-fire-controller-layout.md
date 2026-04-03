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
| Main select encoder | Last touched parameter | Planned preference-backed behavior, similar to DrivenByMoss |
| Main encoder press | Commit / alternate parameter action when required | Only if needed by the controlled parameter |

## Main Select Encoder

The large select encoder should default to controlling the last touched Bitwig parameter, assuming the API lookup confirms the expected access path in controller API 25.

Planned preference:

| Preference | Options |
| --- | --- |
| `Main Encoder Role` | `Last Touched Parameter`, `Note Repeat`, `Disabled` |

Expected behavior:

| Action | Result |
| --- | --- |
| Turn main encoder | Change last touched parameter |
| Press main encoder | Commit or trigger alternate action only when the parameter requires it |

If `Main Encoder Role = Note Repeat`, then:

| Action | Result |
| --- | --- |
| Press main encoder | Toggle note repeat on or off |
| Turn main encoder | Change note repeat value while active |
| OLED | Shows the current note repeat value |

The older note-repeat-centered use of the main encoder should be demoted into this optional setting.

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
| `BROWSER` | `TBD` utility button | Note repeat should be moved away from this central button |

### Grid Arrows

`BANK_L` and `BANK_R` stay as the main timing-edit pair:

| Action | Result |
| --- | --- |
| `BANK_L` / `BANK_R` | Coarse pattern shift |
| `SHIFT + BANK_L` / `SHIFT + BANK_R` | Fine nudge |
| `ALT + BANK_L` / `ALT + BANK_R` | Grid resolution down / up |

This keeps timing-related actions grouped on one pair of buttons.

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

`NOTE` becomes a proper note-input mode rather than a clip-legato toggle.

Planned Note mode behavior:

| Control area | Role |
| --- | --- |
| Pad matrix | Chromatic or scale-aware note input |
| `DRUM` | Return to Drum mode |
| `STEP SEQ` | Likely note step-input function |
| `SHIFT + STEP SEQ` | Likely alternate step-input function |

Detailed note-step behavior is intentionally left open until the Note mode interaction is designed more carefully.

## Perform Mode

`PERFORM` should become a third top-level mode dedicated to clip launching and performance actions.

Intended direction:

| Area | Role |
| --- | --- |
| Pad matrix | Clip / scene / performance view |
| Transport row | Performance-friendly transport actions |
| Mode button LEDs | Clear indication of active top-level mode |

This is intended to replace the current use of `PERFORM` as a temporary grid-resolution modifier.

## Pinning

Pinning the Fire to the current track, clip, or device context is still desirable.

Current recommendation:

| Approach | Status |
| --- | --- |
| Preference-driven auto pinning | Preferred |
| Dedicated manual pin button | Deferred |

The exact preference names are still open, but pinning should be controlled by settings before it consumes a prime hardware button.

## Note Repeat

Note repeat is no longer considered important enough to occupy the most central hardware controls.

Recommended direction:

- remove it from the default main encoder behavior
- remove it from the central `BROWSER` role
- reintroduce it only if it has a clearly useful placement
- consider making it optional, mode-specific, or preference-controlled

## Open Decisions

These points are still intentionally open:

- exact LED colors for Drum sub-modes
- exact left-side button behavior once polyrhythm mode is implemented
- final supported options for `PATTERN`
- final use of `BROWSER`
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
| `BANK_L/BANK_R` | Shift pattern |
| `SHIFT + BANK_L/BANK_R` | Fine nudge |
| `ALT + BANK_L/BANK_R` | Grid resolution |

This is the layout to refer back to while implementing the next Fire iterations.
