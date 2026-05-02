# Bitwig Oikontrol User Guide

Bitwig Oikontrol holds controller extensions for Bitwig Studio, currently for:

- Novation Launch Control XL Mk2
- Akai Fire

This guide is the user-facing reference for setup, modes, preferences, and common workflows. The bundled Help entry in each extension should open the same guide content.

## Installation and setup

1. Build or install the `.bwextension` for the controller you want to use.
2. In Bitwig Studio, open **Settings > Controllers**.
3. Add the Oiko controller entry for your hardware:
   - `LCXL Oikontrol`
   - `Fire Oikontrol`
4. Use the extension preferences if you want to change launch, pinning, or controller-specific behavior.

## Launch Control XL

The Launch Control XL extension keeps the Bitwig factory templates intact while adding deeper user-template workflows for:

- device remote pages
- drum machine control
- arpeggiator performance

### Controller assumptions

For the custom user-mode workflows, the factory-default user template mappings are assumed.

- All controls send on the MIDI channel that matches the selected template number.
- All buttons are configured as momentary.

User template note and CC assumptions:

- Top row knobs: `CC 13-20`
- Middle row knobs: `CC 29-36`
- Bottom row knobs: `CC 49-56`
- Faders: `CC 77-84`
- Send Up/Down: `CC 104/105`
- Track Left/Right: `CC 106/107`
- Right-side mode buttons: `105-108`

### Factory templates

Factory templates keep the same general role as the Bitwig factory script:

- Template 1: 2 sends and selected device controls
- Template 2: 2 sends and per-track device page 1
- Template 3: 2 sends and project remotes
- Template 4: 3 sends
- Template 5: 1 send and 2 per-track device controls
- Template 6: 3 per-track device controls
- Template 7: not supported
- Template 8: 3 track remote controls

### User Template 6: Device pages

User Template 6 turns the controller into a fixed page remote-control surface.

- Each hardware row maps to a fixed page of the selected device's preset remotes.
- This is useful when one patch exposes several remote pages and you want a stable one-template layout.

Quick start:

1. Select a device in Bitwig.
2. Switch the controller to User Template 6.
3. Adjust the rows, faders, and buttons as the current device pages require.

### User Template 7: Drum machine

User Template 7 is the drum workflow.

- Faders control chain volume.
- Knobs map to the first device on each visible drum pad chain.
- The upper pad row selects and can audition drum pads.
- The lower pad row controls pad remote 4 by default, or mute/solo depending on the active sub-mode.

Preferences that matter:

- `Auto-attach to first Drum Machine and Arpeggiator`
- `Audition on drum pad select`
- `Drum accent buttons momentary`

Quick start:

1. Load a Drum Machine on a track.
2. Switch the controller to User Template 7.
3. If auto-attach is enabled, the script will focus the first Drum Machine it finds.
4. Use Track Left/Right to move across pad banks.
5. Use the upper pad row to select pads and the knobs/faders to shape each sound.

### User Template 8: Arpeggiator

User Template 8 adapts the Richie Hawtin / Eric Ahrens rhbitwig arp workflow.

- The arp layer controls the focused Bitwig Arpeggiator.
- Pitch offset, velocity, gate, and step behavior are spread across the rows and mode buttons.
- Auto-attach can focus the first Arpeggiator in the project when enabled.

Quick start:

1. Add an Arpeggiator device in Bitwig.
2. Switch the controller to User Template 8.
3. Use the row controls for pitch, velocity, and gate shaping.
4. Use the side mode buttons to access the arp sub-modes inherited from rhbitwig.

## Akai Fire

The Akai Fire extension is organized around four top-level workflows.

| Button | Workflow | Role |
| --- | --- | --- |
| `DRUM` | Drum workflows | `Drum XOX`; press again for `Nested Rhythm`, then `Drum Pads` |
| `NOTE` | Live note input | 16x4 note surface; press again for harmonic layout |
| `STEP` | Step sequencing | Enters `Melodic Step`; press again for `Chord Step`, then `Fugue` |
| `PERFORM` | Clip launching | 16x4 clip grid and track actions |

Shared pitch context is global across `NOTE`, `Melodic Step`, `Chord Step`, and the settings overlay. `Root Key`, `Scale`, and `Octave` changes in one of those places update the others.

Pad colors in `DRUM` and `PERFORM` follow Bitwig track, drum-lane, and clip colors. `Pad Brightness` and `Pad Saturation` control how strongly those project colors translate to the Fire LEDs.

### Shared controls

| Control | Action |
| --- | --- |
| `PLAY` | Toggle transport, with retrigger-on-start behavior |
| `ALT + PLAY` | Retrigger current clip |
| `REC` | Clip record in `DRUM`; arranger record in `NOTE`; tap for arranger record in `PERFORM` |
| `ALT + REC` | Arranger automation write |
| `PATTERN` | Clip launcher automation write |
| `SHIFT + PATTERN` | Metronome |
| `ALT + PATTERN` | Clip launcher overdub |
| `BROWSER` | Open or close Bitwig popup browser |
| `SHIFT + BROWSER` | Hold global settings overlay |
| `ALT + BROWSER` | Open browser after the current device / insertion context |
| `SHIFT + ALT + BROWSER` | Open browser before the current device / insertion context |

When the popup browser is open, `SELECT` turn moves through results, `SELECT` press commits the selected result, and `BROWSER` closes the browser.

### Global settings overlay

Hold `SHIFT + BROWSER` to edit shared settings from the four encoders.

| Encoder | Setting |
| --- | --- |
| `Channel` | Shared root key |
| `Mixer` | Shared scale |
| `User 1` | Shared octave |
| `User 2` | `ClipRecLen`: launcher recording length |

The global settings screen also shows whether launcher and mixer track views are using all tracks or only active tracks. Press the bottom-right pad while holding `SHIFT + BROWSER` to toggle `Show deactivated tracks`; the same persistent option is available in the controller preferences and defaults to off.

### Main SELECT encoder

Tap `SELECT` to swap between `Last Touched Parameter` and the current alternate role. Press `SHIFT + SELECT` to cycle the alternate role.
Press `ALT + SELECT` to open or close the selected device window.

Global `SELECT` turn chords:

| Control | Action |
| --- | --- |
| Hold `PATTERN` + turn `SELECT` | Move the play position by the current meter's beat unit |
| Hold `SHIFT + PATTERN` + turn `SELECT` | Move the play position by fine 1/16-beat steps |
| Hold `ALT` + turn `SELECT` | Zoom the arranger/detail timeline horizontally |
| Hold `SHIFT + ALT` + turn `SELECT` | Zoom arranger/detail lanes vertically |

| Role | Turn | Press / hold behavior |
| --- | --- | --- |
| `Last Touched Parameter` | Adjust last touched Bitwig parameter | Reset parameter to default |
| `Shuffle` | Adjust groove shuffle; `0` disables shuffle | None |
| `Tempo` | Adjust project tempo | None |
| `Note Repeat` | Select repeat division; `Off` disables repeat | Active repeat control |
| `Track Select` | Previous / next track | Hold while turning to jump by visible pages |
| `Drum Grid` | Drum-step grid resolution | `DRUM` mode only |

### DRUM Mode

`Drum XOX` is the default sequencer-oriented workflow for a Drum Machine. Press `DRUM` again to cycle through `Nested Rhythm` and `Drum Pads`. If `Drum Mode Pinning` is `Auto-select First Drum Machine`, the script focuses and pins the first Drum Machine it finds. If it is `Follow Selection`, the sequencer follows the selected drum context.

| Pad row | Role |
| --- | --- |
| Row 1 | Clip slots |
| Row 2 | Visible drum slots |
| Rows 3-4 | 32 visible steps for the selected lane |

| Control | Action |
| --- | --- |
| `STEP` | Enter `Melodic Step` |
| `SHIFT + STEP` | Accent entry and editing |
| `ALT + STEP` | Fill |
| `PATTERN UP/DOWN` | Page visible steps |
| `ALT + PATTERN UP/DOWN` | Scroll the visible Drum Machine pad window |
| `BANK LEFT/RIGHT` | Move or rotate pattern |
| `SHIFT + BANK LEFT/RIGHT` | Fine nudge |
| `ALT + BANK LEFT/RIGHT` | Halve / double clip length |
| `MUTE_1` | Select |
| `MUTE_2` | Last Step |
| `MUTE_3` | Copy / paste |
| `MUTE_4` | Delete / reset |

Hold one or more step pads, then use the timing gestures to move those held notes directly. Fine-nudged notes stay attached to the held target during the gesture.

Hold `MUTE_3` and press a clip slot, drum pad, or step to paste from the selected item of the same type. Clip-row paste falls back to the playing clip on that track if no clip was explicitly selected.

In `Drum Pads`, `Grid64` plays a 64-pad Bitwig Drum Machine window. The starting page puts C1 on the lower-left pad, then pads run left-to-right from the bottom row. LEDs use explicit Drum Machine pad colors when set; pads with a sound but no explicit color use the track color, and empty pads stay dark. Pressing a pad shows the Bitwig pad name. `PATTERN UP/DOWN` scrolls the pad window by 16 pads; the OLED shows the lower-left note as `Pad Low`, for example `C1`. On the `Channel` page, encoder 1 selects layouts (`Grid64`, `Velocity`, and `Bongos`) and encoder 2 controls velocity sensitivity / `SHIFT`: default velocity. In `Velocity` and `Bongos`, the left 4x4 block selects the sound. `Velocity` uses the remaining 12x4 pads as fixed velocity zones; `Bongos` leaves separator columns between the selector and two 5x4 bongo surfaces, uses hit velocity for note velocity, and maps surface position to per-note pressure.

| Encoder page | Encoders |
| --- | --- |
| `Channel` | Shared step-expression editing |
| `Mixer` | Track volume, pan, send 1, send 2 |
| `User 1` | Step behavior page |
| `User 2` | Euclid length, pulses, rotation, accent density |

### Nested Rhythm mode

`Nested Rhythm` is the second `DRUM` surface. It generates rhythm from nested segment divisions, writes exact timing to a hidden fine clip grid, and projects the generated hits back to the Fire. It is suited to tuplets, ratchets, asymmetric subdivisions, and layered rhythmic structures that are awkward on a coarse step grid.

If the selected clip slot is empty when you enter the mode, Nested Rhythm generates its default starter pattern once.

| Pad row | Role |
| --- | --- |
| Row 1 | Clip row; becomes recurrence mask while a hit is held |
| Rows 2-3 | 32-bin projected rhythm view |
| Row 4 | First 16 generated hits in hit order, shown by velocity with playhead highlight |

| Control | Action |
| --- | --- |
| `DRUM` while in `XOX Drum mode` | Enter `Nested Rhythm mode` |
| `DRUM` while in `Nested Rhythm` | Enter `Drum Pads` |
| `STEP` | Enter `Melodic Step mode` |
| `PATTERN UP` or `ALT + MUTE_4` | Reset hit edits for the selected clip |
| `PATTERN DOWN` | Generate current nested rhythm into the selected clip |
| `ALT + BANK LEFT/RIGHT` | Halve / double clip length |
| Hold `MUTE_2` + projected rhythm pad | Set last step within the 32-step edit view |
| Hold projected rhythm pad | Target nearest generated hit |
| Tap bottom-row hit pad | Toggle that hit |
| Hold bottom-row hit pad + expression encoder | Edit that hit directly |
| `SHIFT` + hit pad | Reset that hit's local edits |

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Density / `ALT`: chance / `SHIFT`: recurrence | Tuplet / `ALT`: Tup.Div / `SHIFT`: target phase | Ratchet / `ALT`: Rat.Div / `SHIFT`: target phase | Cluster / `ALT`: play start |
| `Mixer` | Volume | Pan | Send 1 | Send 2 |
| `User 1` | Velocity spread or held-hit velocity / `ALT`: center / `SHIFT`: rotate | Pressure spread or held-hit pressure / `ALT`: center / `SHIFT`: rotate | Timbre spread or held-hit timbre / `ALT`: center / `SHIFT`: rotate | Chance / `ALT`: baseline / `SHIFT`: rotate |
| `User 2` | Linear pitch | Pitch Expr spread or held-hit pitch expr / `ALT`: center / `SHIFT`: rotate | Clip length / `ALT`: play start | Reset hit edits |

Nested Rhythm reads the selected clip loop length from Bitwig when the clip is selected or the mode is activated. The `Clip length` encoder then adjusts that length from the current DAW value instead of always starting from the mode default. `ALT + BANK LEFT/RIGHT` halves or doubles the current clip length from that same DAW-derived value.

`Chance` is play probability. It starts at 100%; turn it down to make generated hits less likely to play, with weaker/interior hits reduced first. The displayed value is the lowest generated play chance in the current phrase when no hit is held. `ALT + Chance` sets the baseline that the generated chance contour works down from.

Timing is not directly editable from the Fire in this mode. The Fire pads are a projection of the generated rhythm, not the literal Bitwig note grid.
Generated velocities combine local hard/soft accents with broader ramps across ratchet and tuplet spans, so interior subdivisions can rise toward the next structural hit without overtaking it. The default velocity depth is `1.75x`; turn it down for flatter phrases or up slightly for more contrast.
Lowering `Density` removes generated hits without stretching the retained notes, including when clustering is active; note lengths continue to follow the full-density rhythmic structure and are capped again after clustered timing compensation. The displayed range is `20%` to `100%`: `20%` is the sparse minimum where required anchors may still remain, and `100%` keeps the full generated structure. Density thinning prefers compact pairs and short gestures across active ratchet, tuplet, and nested-ratchet material, rather than letting one generated layer consume the whole density budget. Thinning is monotonic, so hits drop out in a stable order instead of disappearing and reappearing as you keep turning in one direction.
`Cluster` pushes retained hits toward a contiguous phrase region at the end of the clip; at `50%`, the retained phrase is constrained to the back half, and at `100%` it is constrained to the final quarter. `ALT + Cluster` adjusts clip play start. Clustered anchors are compensated onto the sixteenth-note grid; other clustered hits prefer the same coarser grid when the phrase has room, then fall back to a thirty-second-note approximation when crowded. When clustering is active, tuplets add material into the compressed phrase instead of clearing the covered span first. At `0%` cluster, anchors remain protected; as clustering rises, anchors join the density selection so the whole phrase can concentrate when there are other hits to trade against. Velocity contour positions stay attached to the full generated structure.

Tuplet is a half-bar transform: `Tuplet` sets how many half-bar targets are affected, while `Tup.Div` sets how many divisions each selected half-bar is split into. Target priority starts with the second half-bar, then the first, then the last lead-in half-bar, then the remaining interior half-bars. `Target Phase` rotates through that target order. Ratchet comes after tuplets: `Ratchet` sets how many parent regions are affected, while `Rat.Div` sets how many divisions each selected parent region is split into. Parent regions are the beat cells plus any tuplet cells created inside the selected half-bars, using the same target-priority idea. In clips longer than one bar, ratchet target order is derived from the whole clip as a single phrase, spreading early targets through phrase positions such as the first-quarter point, midpoint, phrase start, and final lead-in instead of treating the clip as repeated one-bar copies. Tuplet divisions are meter-aware; in `4/4` they include `3 / 5 / 7`, while `5/4` can expose divisions such as `3 / 4 / 6 / 7`. Ratchet divisions support `2 / 3 / 4 / 5 / 6 / 7 / 8`.

### NOTE mode

`NOTE` is a 16x4 playing surface with chromatic, in-key, and harmonic layouts. The shared note octave is initialized from `Default Note Input Octave`.

| Area / Control | Role |
| --- | --- |
| Pad matrix | 16x4 note grid |
| Pad LEDs | Root, in-scale, and out-of-scale feedback |
| `NOTE` | Cycle note layout family |
| `ALT + NOTE` | Toggle live-note layout shortcut |
| `STEP SEQ` | Enter `Melodic Step`; press again for `Chord Step` |
| `BANK LEFT/RIGHT` or `PATTERN UP/DOWN` | Shared octave down / up |
| `SHIFT + PATTERN UP/DOWN` | Shared root down / up |
| `MUTE_1` | Sustain |
| `MUTE_2` | Sostenuto |
| `MUTE_3` | Note Repeat toggle |
| `KNOB MODE` | Cycle live-note encoder pages |

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Mod | Pitch Gliss | Velocity sensitivity / `SHIFT`: Default velocity | Shared scale / `ALT`: Shared root key / `SHIFT`: local layout |
| `Mixer` | Track volume | Track pan | Send 1 | Send 2 |
| `User 1` | Aftertouch | Pressure | Timbre | Pitch expression |
| `User 2` | Selected device remote 1 | Remote 2 | Remote 3 | Remote 4 |

### Melodic Step mode

`Melodic Step` is a generative and editable mono phrase sequencer for basslines, motifs, and melodic hooks. It edits a 2-bar / 32-step window and keeps generated phrases constrained to the current pitch pool.

| Pad row | Role |
| --- | --- |
| Row 1 | Clip row; becomes recurrence editor while active steps are held |
| Row 2 | 16-note pitch pool |
| Rows 3-4 | 32-step melodic phrase |

| Control | Action |
| --- | --- |
| Tap pitch-pool pad | Add / remove that pitch |
| Hold step + pitch-pool pad | Assign that pitch to the held step |
| Tap step | Toggle, place, clear, or select step depending on state |
| Hold step + encoder | Edit held step directly |
| `SHIFT + STEP SEQ` hold | Accent gesture for melodic steps |
| `PATTERN UP` / `ALT + PATTERN UP` | Generate / mutate pitch pool |
| `PATTERN DOWN` / `ALT + PATTERN DOWN` | Generate / mutate phrase |
| `SHIFT + PATTERN UP/DOWN` | Cycle `Notes`, `Expression`, and `Process` views |
| `BANK LEFT/RIGHT` | Rotate phrase |
| `ALT + BANK LEFT/RIGHT` | Halve / double clip length |

| Left-side button | Action |
| --- | --- |
| `MUTE_1` | Select clip without launch |
| `MUTE_2` | Last Step target mode |
| `MUTE_3` | Paste to clip slot |
| `MUTE_4` | Clear step or clip contents |
| `SHIFT + MUTE_4` on clip pad | Remove clip object |

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Engine / `ALT`: subtype | Density | Pitch-pool octave / `ALT`: shared root | Mutation type / `ALT`: strength |
| `Mixer` | Halve / double length | Swivel / mirror-double | Reverse | Invert down / up |
| `User 1` | Engine macro | Tension | Legato | Recurrence span helper |
| `User 2` | Selected or held step octave | Gate | Velocity | Articulation |

Current generator modes are `Acid`, `Motif`, `Call/Resp`, `Rolling`, and `Octave`. If you manually edit the pitch pool, it becomes user-owned and is not auto-replaced on mode switch.

For recurrence editing, hold one or more active melodic steps. Row 1 becomes an 8-pad recurrence editor; tap pads to toggle recurrence hits, or hold the first pad as a span anchor and tap another pad to set the recurrence span.

### Chord Step mode

Press `STEP SEQ` from `Melodic Step` to enter `Chord Step`. Press `NOTE` to return to live note input.

`Chord Step` is the chord-oriented note-step workflow. The builder defaults to in-key view and uses the same shared root and scale as live `NOTE`.

| Pad row | Role |
| --- | --- |
| Row 1 | Clip row |
| Row 2 | Curated chord slots or builder notes |
| Rows 3-4 | 32 visible steps |

| Control | Action |
| --- | --- |
| Tap empty step | Place selected chord |
| Tap lit step | Remove chord |
| Hold step pad(s) + chord pad | Rewrite held steps with that chord |
| Tap chord pad with no held step | Audition chord, if enabled |
| `STEP SEQ` | Return to `Melodic Step` |
| `PATTERN UP/DOWN` | Page visible chord-step window |
| `ALT + BANK LEFT/RIGHT` | Halve / double clip length |
| Hold step(s) + `BANK LEFT/RIGHT` | Experimental micro-timing nudge for held chord material |
| `SHIFT + BANK LEFT/RIGHT` | Experimental micro-timing nudge for visible chord material |

| Left-side button | Action |
| --- | --- |
| `MUTE_1` | Select / load step |
| `MUTE_2` | Last Step target mode |
| `MUTE_3` | Paste to target step or clip slot |
| `MUTE_4` | Delete step or clip |
| `ALT + MUTE_4` | Invert chord |
| `SHIFT + ALT + MUTE_4` | Invert chord in the opposite direction |

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Chord octave / `ALT`: shared root | Velocity sensitivity / `SHIFT`: default velocity | Chord family / `ALT`: family page | Interpretation / `SHIFT`: shared scale / `ALT`: shared root |
| `Mixer` | Track volume | Track pan | Send 1 | Send 2 |
| `User 1` | Note velocity edit | Note chance edit | Recurrence-oriented note editing | Recurrence-oriented note editing |
| `User 2` | Selected device remote 1 | Remote 2 | Remote 3 | Remote 4 |

Chord banks are static libraries of chord formulas and voicing variants. Changing shared `Root Key` or `Scale` does not switch bank, page, or slot; it only changes how the selected slot is rendered. `As Is` transposes the stored chord shape from the current root. `In Scale` rebuilds that slot from the current shared scale and root.

Coarse nudge is intentionally disabled in `Chord Step`; micro-timing is currently temperamental and should be treated as experimental.

### Fugue mode

Press `STEP` from `Chord Step` to enter `Fugue`. `Fugue` treats MIDI channel 1 as the source line and generates related lines on channels 2-4.

| Control | Action |
| --- | --- |
| `PATTERN DOWN` | Reread channel 1 from the clip and rebuild derived lines |
| Encoder turn on a derived-line page | Immediately rebuild that line with the new parameter |
| Channel 1 pads and encoders | Edit the source/template line |

If you change channel 1 notes or expression directly in Bitwig, press `PATTERN DOWN` to update the generated lines from the current DAW clip state. Fugue deliberately avoids live regeneration during Bitwig note-editor drags, so you can audition an alternate source line in isolation and avoid rewriting derived notes while the DAW is still editing the source event.

For immediate derived-line feedback, change source expression from the controller instead. Controller-owned edits update the clip and regenerate from the controller's current source state.

### PERFORM mode

`PERFORM` is the 16x4 clip-launch and performance surface. Filled slots select and launch. Empty slots create a new clip using `Default Clip Length`, then launch.

`Perform Clip Launcher Layout` chooses whether the mode starts as `PerformV` or `PerformH`. `ALT + PERFORM` still toggles the layout for the current session.

| Control | Action |
| --- | --- |
| `REC` + pad | Record into the targeted launcher slot using `Default Clip Length` |
| `MUTE_1` + pad | Select without launching |
| `MUTE_2` | Double selected visible clip length |
| `SHIFT + MUTE_2` | Halve selected visible clip length |
| `MUTE_3` + pad | Paste selected clip to target slot |
| `MUTE_4` + pad | Delete |
| `BANK LEFT/RIGHT` | Scroll tracks by visible page |
| `SHIFT + BANK LEFT/RIGHT` | Scroll tracks by one |
| `PATTERN UP/DOWN` | Scroll scenes by visible page |
| `SHIFT + PATTERN UP/DOWN` | Scroll scenes by one |
| `KNOB MODE` | Cycle Perform encoder pages |
| `PERFORM` while in Perform | Toggle clip launcher / Scene Launch pad page |
| `ALT + PERFORM` | Toggle vertical/horizontal launcher layout |
| `SHIFT + PERFORM` | Toggle latched track-control pad page |

Track-control page rows are select, solo, mute, and arm for the 16 visible tracks. On the select row, hold `ALT` and press a pad to stop that track. `KNOB MODE` still cycles the Perform encoder pages while the track-control pad page is active.

Hold `REC` and press a pad to target recording directly into that visible slot. For fixed `Default Clip Length` values, the script sets Bitwig's clip launcher post-record action to play the recorded clip after that length. If `Default Clip Length` is set to `Off`, recording continues until manually stopped without post-processing. If it is set to `Round`, recording continues until manually stopped, then the recorded clip loop length is rounded to the nearest whole bar. Press `REC` again to end an `Off` or `Round` launcher recording and launch the recorded clip, even after switching to another mode. Filled MIDI clips can overdub MIDI according to Bitwig's clip launcher behavior; audio launcher clips do not support audio overdub, but clip automation can still be written with clip launcher automation write/overdub enabled.

The `Scene Launch` page keeps the same encoder and navigation controls as Perform. Its top row addresses the 16 visible scenes: press a scene pad to launch, hold `MUTE_1` and press a scene pad to select it as the scene copy source, hold `MUTE_3` and press a scene pad to copy the selected scene to that target, and hold `MUTE_4` and press a scene pad to delete it. If no scene source is selected, scene copy falls back to the first visible scene with playing clips, then the first visible scene with recording clips. `MUTE_2` is unused on this page.

On `Scene Launch`, `BANK LEFT/RIGHT` also scrolls the visible scene window, with `SHIFT + BANK LEFT/RIGHT` scrolling by one scene.

On remote encoder pages, hold `ALT` while turning an encoder to control remotes 5-8 on the same page.

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Project Remote 1 | Project Remote 2 | Project Remote 3 | Project Remote 4 |
| `Mixer` | Selected track volume | Selected track pan | Selected track send 1 | Selected track send 2 |
| `User 1` | Selected track remote 1 | Remote 2 | Remote 3 | Remote 4 |
| `User 2` | Selected device remote 1 | Remote 2 | Remote 3 | Remote 4 |

## Preferences

### Launch Control XL preferences

- `Auto-attach to first Drum Machine and Arpeggiator`
- `Audition on drum pad select`
- `Drum accent buttons momentary`

### Akai Fire preferences

- `Clip Launch Mode`
- `Clip Launch Quantization`
- `Perform Clip Launcher Layout`
- `Default Clip Length`
- `SELECT Encoder Startup`
- `Default Root Key`
- `Default Scale`
- `Default Note Input Octave`
- `Melodic Seed Mode`
- `Default Velocity Sensitivity`
- `Melodic Fixed Seed`
- `Pad Brightness`
- `Pad Saturation`
- `Encoder touch reset`
- `SELECT Encoder`
- `Euclid Scope`
- `Drum Mode Pinning`
- `Step Seq Pad Audition`
- `On-screen action notifications`

`Pad Brightness` and `Pad Saturation` interact with the Bitwig track colors used by `DRUM` and `PERFORM`, so the same settings can read differently across different project palettes.

`Melodic Seed Mode` controls how `Melodic Step` chooses its initial generator seed when the controller session starts. `Random` starts each session from a new seed. `Fixed` starts from the configured `Melodic Fixed Seed` value, which makes the sequence of generated melodic phrases reproducible across reconnects or reloads. Each `Generate` press still advances forward from that starting point.

The melodic seed controls are grouped into their own `Generative control` preference section so they stay together in Bitwig's settings UI.

## Troubleshooting

### The wrong device is being controlled

- Check the relevant auto-attach or pinning preferences.
- In Fire `DRUM` mode, use `Follow Selection` to work on the currently selected drum track/device, or `Auto-select First Drum Machine` for a dedicated first-drum-machine workflow.
- Re-select the target track or device in Bitwig.
- Re-enter the relevant mode or template after changing focus.

### LEDs or mode state do not match what you expect

- Re-open the matching mode or user template.
- Power-cycle the controller if Bitwig started with another script attached first.
- Make sure the controller is using the expected template or default mapping.

## Attribution

- `rhbitwig` by Richie Hawtin and Eric Ahrens provided the Akai Fire drum sequencer which has been adapted for use here, as well as the arp workflow used on the LCXL User Template 8.

- The fine-grid step nudging is based on Wim Van den Borre's `AkaiFireNudger` fork of `rhbitwig`, and developed further here.

- All LCXL factory modes were taken from Bitwig's original `bitwig-extensions` for the Launch Control XL controller 
