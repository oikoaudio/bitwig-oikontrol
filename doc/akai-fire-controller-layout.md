# Akai Fire Controller Layout

This document is the current implementation reference for the Akai Fire layout in the Oikontrol script.

It replaces the older "target layout" notes that drifted away from the code. Where behavior is still intentionally incomplete, that is called out explicitly.

## Top-Level Modes

The Fire exposes four top-level workflows. Three are selected by the large mode buttons, and `STEP` is entered with `STEP SEQ` from the current note or drum context.

| Button | Role | Notes |
| --- | --- | --- |
| `DRUM` | Drum sequencing | Default sequencing workflow |
| `NOTE` | Live note input | Includes entry to `Chord Step` on second press |
| `STEP` | Step sequencing | Enters `Melodic Step` mode |
| `PERFORM` | Clip launcher and performance mode | `16x4` clip grid |

## Shared Transport And Utility Controls

These mappings apply across the Fire modes unless a mode temporarily takes over the control:

| Control | Action |
| --- | --- |
| `PLAY` | Toggle transport, with retrigger-on-start behavior |
| `ALT + PLAY` | Retrigger current clip |
| `REC` | Clip record in `DRUM`, arranger record in `NOTE` and `PERFORM` |
| `ALT + REC` | Arranger automation write |
| `PATTERN` | Clip launcher automation write |
| `SHIFT + PATTERN` | Metronome |
| `ALT + PATTERN` | Clip launcher overdub |
| `BROWSER` | Open or close Bitwig popup browser |

## Main Select Encoder

The large `SELECT` encoder is a global utility encoder.

### Role Switching

| Action | Result |
| --- | --- |
| Tap `SELECT` | Swap between `Last Touched Parameter` and the current alternate role |
| `SHIFT + SELECT` press | Cycle the alternate role |

Available alternate roles:

- `Shuffle`
- `Tempo`
- `Note Repeat`
- `Track Select`

### Encoder Roles

| Role | Turn | Press / hold behavior |
| --- | --- | --- |
| `Last Touched Parameter` | Adjust last touched Bitwig parameter | Reset parameter to default |
| `Shuffle` | Adjust groove shuffle | Turning to `0` disables shuffle |
| `Tempo` | Adjust project tempo | No special note beyond normal encoder interaction |
| `Note Repeat` | Select repeat division; `Off` disables repeat | Works as the active repeat control |
| `Track Select` | Move to previous/next track | Hold while turning to jump by visible pages |

## Popup Browser

When the popup browser is open, the main `SELECT` encoder is temporarily remapped:

| Control | Browser action |
| --- | --- |
| `SELECT` turn | Move through popup browser results |
| `SELECT` press | Commit selected result |
| `BROWSER` | Close popup browser |

Browser insert mode follows modifiers when opening:

| Action | Browser open mode |
| --- | --- |
| `BROWSER` | Replace / add in current context |
| `SHIFT + BROWSER` | Insert before |
| `ALT + BROWSER` | Insert after |

## DRUM Mode

`DRUM` is the fixed drum-sequencer surface.

### Pad Layout

| Pad row | Role |
| --- | --- |
| Row 1 | Clip slots |
| Row 2 | Visible drum slots |
| Rows 3-4 | 32-step sequencer for the selected lane |

### Left-Side Edit Buttons

| Button | Role |
| --- | --- |
| `MUTE_1` | Select |
| `MUTE_2` | Last Step |
| `MUTE_3` | Copy |
| `MUTE_4` | Delete / Reset |

### Step And Timing Controls

| Action | Result |
| --- | --- |
| `STEP SEQ` | Enter `Melodic Step` |
| `SHIFT + STEP SEQ` | Accent mode |
| `ALT + STEP SEQ` | Toggle Fill |
| `BANK LEFT/RIGHT` | Move or rotate pattern |
| `SHIFT + BANK LEFT/RIGHT` | Fine nudge |
| `ALT + BANK LEFT/RIGHT` | Grid resolution down / up |

Held-step nudge behavior:

- Hold one or more step pads, then use `BANK LEFT/RIGHT` timing gestures to move those held notes directly.
- Fine-nudged notes stay attached to the held note target during that gesture rather than being re-picked by nearest-grid lookup mid-action.

### Encoders In DRUM

| Encoder page | Role |
| --- | --- |
| `Channel` | Shared step-expression editing |
| `Mixer` | Track volume, pan, send 1, send 2 |
| `User 1` | Step behavior page |
| `User 2` | Euclid controls |

Current `User 2` Euclid assignments:

| Encoder | Function |
| --- | --- |
| 1 | Euclid length |
| 2 | Euclid pulses |
| 3 | Euclid rotation |
| 4 | Accent density |

## NOTE Mode

`NOTE` is the live note-input surface, with note-step workflows behind `STEP SEQ`.

### Live Note Layout

| Area / Control | Role |
| --- | --- |
| Pad matrix | `16x4` isomorphic note grid |
| Pad LEDs | Root, in-scale, and out-of-scale feedback |
| `NOTE` | Cycle note layout family |
| `STEP SEQ` | Enter current note-step sub-mode |
| `SHIFT + STEP SEQ` | Cycle note-step sub-mode and enter it |
| `BANK LEFT/RIGHT` | Octave down / up |
| `PATTERN UP/DOWN` | Octave up / down |
| `SHIFT + PATTERN UP/DOWN` | Root up / down |
| `MUTE_1` | Sustain |
| `MUTE_2` | Sostenuto |
| `MUTE_3` | Note Repeat toggle |
| `KNOB MODE` | Cycle live-note encoder pages |

The default note octave is initialized from the `Default Note Input Octave` preference.

### Live Note Encoder Pages

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Pitch offset | Live velocity | Scale | Layout / note family |
| `Mixer` | Track volume | Track pan | Send 1 | Send 2 |
| `User 1` | Mod | Pressure | Timbre | Pitch expression |
| `User 2` | Selected device remote 1 | Remote 2 | Remote 3 | Remote 4 |

## Note-Step Sub-Modes

The current note-step sub-modes are:

| Sub-mode | Status | Notes |
| --- | --- | --- |
| `Chord Step` | Implemented | Main note-step mode under `NOTE` |
| `Clip Step Record` | Placeholder | Still deferred |

`Melodic Step` is not a NOTE sub-mode. It is entered directly from `DRUM` via `STEP SEQ`.

## Chord Step

`Chord Step` repurposes the `NOTE` surface into a chord-and-step editor.

### Pad Layout

| Pad row | Role |
| --- | --- |
| Upper two rows | Curated chord slots |
| Lower two rows | 32 visible steps |

### Main Chord Step Gestures

| Action | Result |
| --- | --- |
| Tap empty step | Place selected chord |
| Tap lit step | Remove chord from that step |
| Hold step pad(s) + tap chord pad | Rewrite held steps with that chord |
| Tap chord pad with no held step | Audition chord, if enabled |
| `STEP SEQ` | Toggle `As Is` / `Cast` rendering |
| `SHIFT + STEP SEQ` | Cycle note-step sub-mode |
| `PATTERN UP/DOWN` | Page the visible chord-step window |
| Hold step(s) + `BANK LEFT/RIGHT` | Fine-nudge held chord material |
| `SHIFT + BANK LEFT/RIGHT` | Fine-nudge visible chord material |

### Chord Step Left-Side Buttons

| Button | Role |
| --- | --- |
| `MUTE_1` | Select / load step |
| `MUTE_2` | Paste to target step |
| `MUTE_3` | Last Step target mode |
| `MUTE_4` | Invert selected chord (`ALT` inverts the other direction) |

Chord root and octave offsets still exist in `Chord Step`, but they are adjusted from the pitch-context controls rather than the `MUTE` buttons. Plain coarse nudge is currently disabled in this mode.

## Melodic Step

`Melodic Step` is a generated and editable mono phrase sequencer for basslines and melodic hooks.

### Pad Layout

| Pad row | Role |
| --- | --- |
| Upper two rows | Pitch pool |
| Lower two rows | 32-step melodic phrase |

### Generator Modes

Current generators:

- `Acid`
- `Motif`
- `Call/Resp`
- `Euclid`
- `Rolling`
- `Octave`

### Main Melodic Step Gestures

| Action | Result |
| --- | --- |
| Tap pitch-pool pad | Add or remove that pitch from the pool |
| Hold step + tap pitch-pool pad | Assign that pitch to the held step |
| Tap step | Toggle or select step |
| `SHIFT + STEP SEQ` hold | Accent gesture for melodic steps |
| `BANK LEFT/RIGHT` | Rotate phrase left / right |
| `PATTERN UP` | Generate new pitch pool |
| `ALT + PATTERN UP` | Mutate pitch pool |
| `PATTERN DOWN` | Generate new phrase |
| `ALT + PATTERN DOWN` | Mutate phrase |
| `SHIFT + PATTERN UP/DOWN` | Cycle view between `Notes`, `Expression`, and `Process` |

If `Step Seq Pad Audition` is enabled, pressing a pitch-pool pad also auditions that note.

### Melodic Step Left-Side Buttons

| Button | Primary role | Alt / Shift variant |
| --- | --- | --- |
| `MUTE_1` | Repeat / double | `SHIFT`: halve, `ALT`: mirror-double |
| `MUTE_2` | Reverse | `ALT`: swivel halves |
| `MUTE_3` | Invert up | `ALT`: invert down |
| `MUTE_4` | Last Step target mode | None |

### Melodic Step Encoders

`Melodic Step` uses the shared step-encoder infrastructure with mode-specific pages for generation and phrase editing. The exact labels shown on OLED depend on the active view and the held-step state.

## PERFORM Mode

`PERFORM` is the `16x4` clip-launch and performance surface.

### Pad Layout

| Area | Role |
| --- | --- |
| Pad matrix | `16x4` clip grid |
| Filled slot | Select and launch |
| Empty slot | Create a new clip using `Default Clip Length`, then launch |

Pad LEDs follow Bitwig clip and track colors plus launch state.

### Left-Side Buttons

| Button | Role |
| --- | --- |
| `MUTE_1` | Hold for select-without-launch |
| `MUTE_2` | Double selected visible clip length |
| `SHIFT + MUTE_2` | Halve selected visible clip length |
| `MUTE_3` | Hold for copy / paste selected clip |
| `MUTE_4` | Hold for delete |

### Navigation

| Action | Result |
| --- | --- |
| `BANK LEFT/RIGHT` | Scroll tracks by visible page |
| `SHIFT + BANK LEFT/RIGHT` | Scroll tracks by one |
| `PATTERN UP/DOWN` | Scroll scenes by visible page |
| `SHIFT + PATTERN UP/DOWN` | Scroll scenes by one |
| `KNOB MODE` | Cycle Perform encoder pages |

### Perform Encoder Pages

| Page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Project Remote 1 | Project Remote 2 | Project Remote 3 | Project Remote 4 |
| `Mixer` | Selected track volume | Selected track pan | Selected track send 1 | Selected track send 2 |
| `User 1` | Selected track remote 1 | Remote 2 | Remote 3 | Remote 4 |
| `User 2` | Selected device remote 1 | Remote 2 | Remote 3 | Remote 4 |

## Preferences That Affect Layout

These preferences materially change how the Fire feels in use:

- `Clip Launch Mode`
- `Clip Launch Quantization`
- `Default Clip Length`
- `Default Note Input Octave`
- `SELECT Encoder Startup`
- `SELECT Encoder`
- `Drum Mode Pinning`
- `Step Seq Pad Audition`
- `Euclid Scope`
- `Pad Brightness`
- `Pad Saturation`

## Known Gaps

These areas are still intentionally incomplete or provisional:

- `Clip Step Record` remains a placeholder.
- `Chord Step` works best with simpler, grid-aligned chord material.
- `Melodic Step` encoder documentation is still lighter here than the implementation and should be expanded once that mode stabilizes further.
