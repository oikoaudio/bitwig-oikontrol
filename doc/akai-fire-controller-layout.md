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
- `Drum Grid`

### Encoder Roles

| Role | Turn | Press / hold behavior |
| --- | --- | --- |
| `Last Touched Parameter` | Adjust last touched Bitwig parameter | Reset parameter to default |
| `Shuffle` | Adjust groove shuffle | Turning to `0` disables shuffle |
| `Tempo` | Adjust project tempo | No special note beyond normal encoder interaction |
| `Note Repeat` | Select repeat division; `Off` disables repeat | Works as the active repeat control |
| `Track Select` | Move to previous/next track | Hold while turning to jump by visible pages |
| `Drum Grid` | Adjust drum-step grid resolution in `DRUM` mode | `DRUM` mode only |

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
| `ALT + BANK LEFT/RIGHT` | Halve / double clip length |

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

Drum grid resolution is now adjusted from the `SELECT` encoder when its role is set to `Drum Grid`.

## NOTE Mode

`NOTE` is the live note-input surface, with note-step workflows behind `STEP SEQ`.

`NOTE`, `Chord Step`, and `SHIFT + PERFORM` now share one global pitch context for `Root Key` and `Scale`. Octave and layout remain mode-local.

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

In live NOTE mode:

- Encoder 2 is `Pitch Gliss`
- Encoder 3 adjusts `Velocity Sensitivity`
- `SHIFT + Encoder 3` adjusts `Default Velocity`
- Encoder 4 adjusts the shared `Scale`

### Live Note Encoder Pages

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Mod | Pitch Gliss | Velocity sensitivity (`SHIFT`: Default velocity) | Shared scale |
| `Mixer` | Track volume | Track pan | Send 1 | Send 2 |
| `User 1` | Aftertouch | Pressure | Timbre | Pitch expression |
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

It uses the shared `Root Key` and `Scale` from live NOTE input and `SHIFT + PERFORM`. Changing key or scale in one of those places updates all of them.

### Pad Layout

| Pad row | Role |
| --- | --- |
| Row 1 | Clip row |
| Row 2 | Curated chord slots or builder notes |
| Rows 3-4 | 32 visible steps |

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
| `SHIFT + ALT + PATTERN DOWN` | Clear current selected clip contents |
| `ALT + BANK LEFT/RIGHT` | Halve / double clip length |
| Hold step(s) + `BANK LEFT/RIGHT` | Fine-nudge held chord material |
| `SHIFT + BANK LEFT/RIGHT` | Fine-nudge visible chord material |

### Chord Step Left-Side Buttons

| Button | Role |
| --- | --- |
| `MUTE_1` | Select / load step |
| `MUTE_2` | Last Step target mode |
| `MUTE_3` | Paste to target step or clip slot |
| `MUTE_4` | Delete step or clip (`ALT`: invert chord, `SHIFT + ALT`: invert opposite direction) |

The chord builder defaults to showing in-key notes only. If it auto-seeds a note into an empty builder, that note must be visible on the current builder rows.

### Chord Step Encoders

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Chord octave (`ALT`: Shared root key) | Velocity sensitivity (`SHIFT`: Default velocity) | Chord family | Chord render / interpretation |
| `Mixer` | Track volume | Track pan | Send 1 | Send 2 |
| `User 1` | Note velocity edit | Note chance edit | Note recurrence length | Note recurrence count |
| `User 2` | Selected device remote 1 | Remote 2 | Remote 3 | Remote 4 |

`Chord Step` no longer owns a separate root/key state. `ALT + Encoder 1` updates the same shared root used by live NOTE input.

## Melodic Step

`Melodic Step` is a generated and editable mono phrase sequencer for basslines and melodic hooks.

### Pad Layout

| Pad row | Role |
| --- | --- |
| Row 1 | Clip row |
| Row 2 | 16-note pitch pool |
| Rows 3-4 | 32-step melodic phrase |

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
| `ALT + BANK LEFT/RIGHT` | Halve / double clip length |
| `PATTERN UP` | Generate new pitch pool |
| `ALT + PATTERN UP` | Mutate pitch pool |
| `PATTERN DOWN` | Generate new phrase |
| `ALT + PATTERN DOWN` | Mutate phrase |
| `SHIFT + ALT + PATTERN DOWN` | Clear current selected clip contents |
| `SHIFT + PATTERN UP/DOWN` | Cycle view between `Notes`, `Expression`, and `Process` |

If `Step Seq Pad Audition` is enabled, pressing a pitch-pool pad also auditions that note.

### Melodic Step Left-Side Buttons

| Button | Role |
| --- | --- |
| `MUTE_1` | Select clip without launch |
| `MUTE_2` | Last Step target mode |
| `MUTE_3` | Paste to clip slot |
| `MUTE_4` | Delete step or clip |

Melodic transforms moved off the left-side buttons and now live on the `Mixer` encoder page:

- Encoder 1: halve / double length
- Encoder 2: swivel / mirror-double
- Encoder 3: reverse
- Encoder 4: invert down / up

### Melodic Step Encoders

`Melodic Step` uses the shared step-encoder infrastructure with mode-specific pages for generation and phrase editing. The exact labels shown on OLED depend on the active view and the held-step state.

## PERFORM Mode

`PERFORM` is the `16x4` clip-launch and performance surface.

`SHIFT + PERFORM` opens a latched `Settings` page on the `Channel` encoder page. From there, Encoder 1 adjusts the shared `Root Key` and Encoder 2 adjusts the shared `Scale`. Press `PERFORM` again to leave `Settings`.

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

Perform OLED page titles are:

- `Channel`: `Global Remotes`
- `Mixer`: `Mixer`
- `User 1`: `Track Remotes`
- `User 2`: `Master/Cue`

While `Settings` is active in `PERFORM`:

- Encoder 1 edits shared `Root Key`
- Encoder 2 edits shared `Scale`
- Encoders 3-4 are reserved for future shared settings

## Preferences That Affect Layout

These preferences materially change how the Fire feels in use:

- `Clip Launch Mode`
- `Clip Launch Quantization`
- `Default Clip Length`
- `Default Root Key`
- `Default Note Input Octave`
- `Default Velocity Sensitivity`
- `SELECT Encoder Startup`
- `SELECT Encoder`
- `Drum Mode Pinning`
- `Step Seq Pad Audition`
- `Euclid Scope`
- `Pad Brightness`
- `Pad Saturation`

`Default Root Key`, `Default Scale`, `Default Note Input Octave`, and `Default Velocity Sensitivity` are startup defaults. They initialize the shared pitch context and live NOTE response when the script starts; changing key, scale, or velocity sensitivity from the controller does not currently write those defaults back into Bitwig preferences.

## Known Gaps

These areas are still intentionally incomplete or provisional:

- `Clip Step Record` remains a placeholder.
- `Chord Step` works best with simpler, grid-aligned chord material.
- `Melodic Step` encoder documentation is still lighter here than the implementation and should be expanded once that mode stabilizes further.
