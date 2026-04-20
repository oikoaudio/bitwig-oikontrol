# Akai Fire Controller Layout

This document is the current implementation reference for the Akai Fire layout in the Oikontrol script.

It replaces the older "target layout" notes that drifted away from the code. Where behavior is still intentionally incomplete, that is called out explicitly.

## Top-Level Modes

The Fire exposes four top-level workflows. Three are selected by the large mode buttons, and `STEP` is entered with `STEP SEQ` from the current note or drum context. `DRUM` now has two main surfaces: standard drum sequencing and `Nested Rhythm`.

| Button | Role | Notes |
| --- | --- | --- |
| `DRUM` | Drum sequencing | XOX Drum sequencing |
| `NOTE` | Live note input | Cycles to `Harmonic mode` on second press |
| `STEP` | Step sequencing | Enters `Melodic Step`; press again to switch to `Chord Step` |
| `PERFORM` | Clip launcher | Cycles between a vertical and a horizontal `16x4` clip grid |

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
| `SHIFT + BROWSER` | Open global settings for scale and key (two first encoders) |

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

Opening the browser keeps Bitwig's remembered search/filter state intact. Result navigation is immediately available from the `SELECT` encoder without needing keyboard or mouse focus first.

Browser insert mode follows modifiers when opening:

| Action | Browser open mode |
| --- | --- |
| `BROWSER` | Replace / add in current context |
| `SHIFT + BROWSER` | Insert before |
| `ALT + BROWSER` | Insert after |

## DRUM Mode

`DRUM` is the xox style drum sequencer from the rhbitwig extension, with added micro-timing control and minor adjustments to controls, pad order and pad colors.

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

`NOTE` is the live note-input surface, with multiple scales and live controls for modulation wheel, pitch bend, glissando, expression and mappings to track remotes.
Pressing `NOTE` again enters a different Harmonic pad layout where each pad can emit multiple notes that represent triplet chord inversions. Combine multiple pads for richer chords.

### Live Note Layout

| Area / Control | Role |
| --- | --- |
| Pad matrix | `16x4` isomorphic note grid |
| Pad LEDs | Root, in-scale, and out-of-scale feedback |
| `NOTE` | Cycle note layout family |
| `ALT + NOTE` | Toggle live-note layout shortcut (`Chromatic` / `In Key`, or harmonic layout variant) |
| `STEP SEQ` | Enter `Melodic Step`; when already there, press again to switch to `Chord Step` |
| `DRUM` while already in `DRUM` | Toggle between standard drum sequencing and `Nested Rhythm` |
| `BANK LEFT/RIGHT` | Octave down / up |
| `PATTERN UP/DOWN` | Octave up / down |
| `SHIFT + PATTERN UP/DOWN` | Root up / down |
| `MUTE_1` | Sustain |
| `MUTE_2` | Sostenuto |
| `MUTE_3` | Note Repeat toggle |
| `KNOB MODE` | Cycle live-note encoder pages |

The shared note octave is initialized from the `Default Note Input Octave` preference.

In live NOTE mode:

- Encoder 2 is `Pitch Gliss`
- Encoder 3 adjusts `Velocity Sensitivity`
- `SHIFT + Encoder 3` adjusts `Default Velocity`
- `SHIFT + Encoder 4` toggles the local live-note layout
- Encoder 4 adjusts the shared `Scale`
- `ALT + Encoder 4` adjusts the shared `Root Key`
- `PATTERN UP/DOWN` adjusts the shared `Octave`

### Live Note Encoder Pages

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Mod | Pitch Gliss | Velocity sensitivity (`SHIFT`: Default velocity) | Shared scale (`ALT`: Shared root key) |
| `Mixer` | Track volume | Track pan | Send 1 | Send 2 |
| `User 1` | Aftertouch | Pressure | Timbre | Pitch expression |
| `User 2` | Selected device remote 1 | Remote 2 | Remote 3 | Remote 4 |

## Chord Step

`Chord Step` is the second `STEP SEQ` surface after `Melodic Step`.

It uses the shared `Root Key`, `Scale`, and `Octave` from live NOTE input and `SHIFT + PERFORM`. Changing pitch context in one of those places updates all of them.

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
| `STEP SEQ` | Return to `Melodic Step` |
| `SHIFT + STEP SEQ` | Reserved for melodic-step accent handling |
| `PATTERN UP/DOWN` | Page the visible chord-step window |
| `ALT + BANK LEFT/RIGHT` | Halve / double clip length |
| Hold step(s) + `BANK LEFT/RIGHT` | Experimental micro-timing nudge for held chord material |
| `SHIFT + BANK LEFT/RIGHT` | Experimental micro-timing nudge for visible chord material |

Chord-step timing note:

- coarse nudge is intentionally disabled
- chord-step micro-timing is currently temperamental and should be treated as experimental

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
| `Channel` | Chord octave (`ALT`: Shared root key) | Velocity sensitivity (`SHIFT`: Default velocity) | Chord family (`ALT`: family page) | Interpretation (`SHIFT`: Shared scale, `ALT`: Shared root key) |
| `Mixer` | Track volume | Track pan | Send 1 | Send 2 |
| `User 1` | Note velocity edit | Note chance edit | Recurrence-oriented note editing | Recurrence-oriented note editing |
| `User 2` | Selected device remote 1 | Remote 2 | Remote 3 | Remote 4 |

`Chord Step` no longer owns a separate root/key state. `ALT + Encoder 1` and `ALT + Encoder 4` both update the same shared root used by live NOTE input, while `SHIFT + Encoder 4` updates the shared scale that `In Scale` interpretation uses.

Chord banks themselves are static. Changing shared `Root Key` or `Scale` does not select a different bank, page, or slot; it only changes how the current slot is rendered. `As Is` transposes the stored chord shape from the current root. `In Scale` rebuilds that same slot against the current shared scale and root, so scale changes can reharmonize the chord.

## Melodic Step

`Melodic Step` is a generated and editable mono phrase sequencer for basslines and melodic hooks.
It edits a 2-bar / 32-step window and does not expand beyond that range from within the mode.

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
| `SHIFT + PATTERN UP/DOWN` | Cycle view between `Notes`, `Expression`, and `Process` |

If `Step Seq Pad Audition` is enabled, pressing a pitch-pool pad also auditions that note.

### Melodic Step Left-Side Buttons

| Button | Role |
| --- | --- |
| `MUTE_1` | Select clip without launch |
| `MUTE_2` | Last Step target mode |
| `MUTE_3` | Paste to clip slot |
| `MUTE_4` | Clear step or clip contents |

On the top-row clip pads, `SHIFT + MUTE_4` removes the clip object.

Melodic transforms moved off the left-side buttons and now live on the `Mixer` encoder page:

- Encoder 1: halve / double length
- Encoder 2: swivel / mirror-double
- Encoder 3: reverse
- Encoder 4: invert down / up

### Melodic Step Encoders

`Melodic Step` uses the shared step-encoder infrastructure with mode-specific pages for generation and phrase editing. The exact labels shown on OLED depend on the active view and the held-step state.

Current `Channel` page:

| Slot | Function |
| --- | --- |
| 1 | Engine |
| `ALT + 1` | Engine subtype / family when available |
| 2 | Density |
| 3 | Pitch-pool octave center |
| `ALT + 3` | Shared root key |
| 4 | Mutation type |
| `ALT + 4` | Mutation strength |

Current `User 1` page:

| Slot | Function |
| --- | --- |
| 1 | Engine-specific macro (`Motion`, `Contour`, `Answer`, `Movement`, `Jump`) |
| 2 | Tension |
| 3 | Legato |
| 4 | Recurrence span helper |

Melodic recurrence editing:

- Hold one or more active melodic steps.
- While those steps are held, the top clip row switches into an 8-pad recurrence editor.
- Tap top-row pads to toggle recurrence hits within the current span.
- Hold the first top-row pad as a span anchor, then tap another top-row pad to set the recurrence span.
- `User 1 / Encoder 4` shows the current recurrence summary for the held target step(s).

## Nested Rhythm

`Nested Rhythm` is the second main `DRUM` surface. It is a generator-based drum workflow that writes exact timing to a hidden fine clip grid and projects the resulting hits back to the Fire.

If the selected clip slot is empty when you enter the mode, Nested Rhythm generates its default starter pattern automatically once.

### Pad Layout

| Pad row | Role |
| --- | --- |
| Row 1 | Clip row |
| Rows 2-3 | 32-bin projected rhythm view |
| Row 4 | First 16 generated hits in hit order, shown by velocity with playhead highlight |

### Main Nested Rhythm Gestures

| Action | Result |
| --- | --- |
| `DRUM` while already in `DRUM` | Toggle between standard drum sequencing and `Nested Rhythm` |
| `STEP SEQ` | Enter `Melodic Step` |
| `DRUM` while in `Nested Rhythm` | Return to standard drum sequencing |
| `PATTERN UP` | Generate current nested rhythm into the selected clip |
| Hold `MUTE_2`, then tap a projected rhythm pad | Set the last step within the 32-step edit view |
| `PATTERN DOWN` or `ALT` + `MUTE_4` | Reset hit edits for the current selected clip |
| Hold projected rhythm pad | Target the nearest generated hit while held |
| Tap bottom-row hit pad | Toggle that generated hit on/off |
| Hold bottom-row hit pad while turning an expression encoder | Edit that hit directly |
| Hold a hit, then use Row 1 pads 1-8 | Edit that hit's recurrence mask across up to 8 phrase revolutions |
| `SHIFT` + hit pad | Reset that hit's local edits |

### Nested Rhythm Encoders

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Density / `SHIFT`: recurrence | Tuplet count / `ALT`: cover / `SHIFT`: phase | Ratchet count / `ALT`: width / `SHIFT`: phase | Chance / `ALT`: baseline / `SHIFT`: rotate |
| `Mixer` | Volume | Pan | Send 1 | Send 2 |
| `User 1` | Velocity spread or held-hit velocity / `ALT`: center / `SHIFT`: rotate | Pressure spread or held-hit pressure / `ALT`: center / `SHIFT`: rotate | Timbre spread or held-hit timbre / `ALT`: center / `SHIFT`: rotate | Pitch Expr spread or held-hit pitch expr / `ALT`: center / `SHIFT`: rotate |
| `User 2` | Linear pitch | Clip length / `ALT`: play start | Reset hit edits | Meter readout |

Timing note:

- exact starts are written to a hidden fine grid in the Bitwig clip
- the Fire pads do not edit those raw start times directly
- the visible rhythm row is a projected overview rather than a literal note grid

Control note:

- `Density` is still a thinning control, but the mode currently opens at maximum density so newly enabled tuplets and ratchets are audible immediately
- `SHIFT + Density` generates a default recurrence pattern over up to 8 phrase revolutions; stronger hits recur more often and weaker/interior hits drop out first
- when `Density` thins a tuplet- or ratchet-owned span, it removes visible hits from that claimed span without reviving an underlying base pulse there
- `User 1` is the expression page; with no hit held, plain turn edits spread, `ALT` edits center, and `SHIFT` edits rotation; with a hit held, plain turn edits that hit directly
- `Channel / Encoder 4` writes Bitwig note chance: plain turn edits chance depth, `ALT` edits chance baseline, and `SHIFT` edits chance rotation; holding a hit turns the plain action into a direct chance edit for that hit
- while a hit is held, Row 1 temporarily becomes an 8-pad recurrence mask instead of the clip row
- `User 2` pitch is a single non-wrapping note control rather than separate root and octave knobs
- `User 2 / Encoder 2` controls generated clip length in bars, with `ALT` adjusting clip play start in meter-aware beat steps; `User 2 / Encoder 4` shows the current transport meter
- `Tuplet` stays a half-bar transform, not a per-quarter burst; `Cover` sets how many consecutive half-bars are claimed, and `Tuplet Phase` rotates that continuous claimed region across the clip
- `Ratchet Width` chooses phrase beats in deterministic priority order, and `Ratchet Phase` rotates that chosen set across the actual beat positions of the clip
- available `Tuplet` counts now depend on meter and claimed span; in `4/4` that still yields `3 / 5 / 7`, while `5/4` can expose counts such as `3 / 4 / 6 / 7`
- `Ratchet` supports even and odd burst counts including `2 / 3 / 4 / 5 / 6 / 7 / 8`

## PERFORM Mode

`PERFORM` is the `16x4` clip-launch and performance surface.

`SHIFT + PERFORM` opens a latched `Settings` page on the `Channel` encoder page. From there, Encoder 1 adjusts the shared `Root Key`, Encoder 2 adjusts the shared `Scale`, and Encoder 3 adjusts the shared `Octave`. Press `PERFORM` again to leave `Settings`.

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
- Encoder 3 edits shared `Octave`
- Encoder 4 is currently unused on the settings page

## Preferences That Affect Layout

These preferences change how the Fire feels in use:

- `Clip Launch Mode`
- `Clip Launch Quantization`
- `Default Clip Length`
- `Default Root Key`
- `Default Note Input Octave`
- `Melodic Seed Mode`
- `Default Velocity Sensitivity`
- `Melodic Fixed Seed`
- `SELECT Encoder Startup`
- `SELECT Encoder`
- `Drum Mode Pinning`
- `Step Seq Pad Audition`
- `Euclid Scope`
- `Pad Brightness`
- `Pad Saturation`

`Default Root Key`, `Default Scale`, `Default Note Input Octave`, and `Default Velocity Sensitivity` are startup defaults. They initialize the shared pitch context and live NOTE response when the script starts; changing key, scale, octave, or velocity sensitivity from the controller does not currently write those defaults back into Bitwig preferences.

`Melodic Seed Mode` controls how `Melodic Step` chooses its initial generator seed when the controller session starts. `Random` starts each session from a new seed. `Fixed` starts from the configured `Melodic Fixed Seed` value, which makes the sequence of generated melodic phrases reproducible across reconnects or reloads. Each `Generate` press still advances forward from that starting point.

The melodic seed controls are grouped into their own `Generative control` preference section so they stay together in Bitwig's settings UI.

## Known Gaps

These areas are still intentionally incomplete or provisional:

- `Clip Step Record` remains a placeholder.
- `Chord Step` works best with simpler, grid-aligned chord material.
- The ocumentation is still lighter here than the implementation and should be updated :)
