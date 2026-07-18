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

In factory templates, the eight faders, knob columns, and bottom-button columns follow the eight-track Bitwig controller feedback box. Track Left/Right moves the controlled track window by eight tracks. When `Show deactivated tracks` is off, deactivated tracks are skipped by the factory track bank; when it is on, they remain available in the bank.

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
- `Exclusive Track Arm`: when on, record-arm buttons select the armed track and disarm the other visible strips; when off, record-arm buttons allow multiple armed tracks and do not change selection
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
| `NOTE` | Live note input | 16x4 melodic / harmonic note surface |
| `STEP` | Step sequencing | Enter `Poly Step`; from there press again for `Melo Gen`, then `Fugue` |
| `PERFORM` | Clip launching | 16x4 clip grid and track actions |

`DRUM`, `STEP`, and `PERFORM` remember the last page used under that button. The first entry uses the defaults above, or the configured `Startup Mode` on controller startup; after that, leaving and returning restores the last page in that family.

Shared pitch context is global across `NOTE`, `Melo Gen`, `Poly Step`, and the settings overlay. `Root Key`, `Scale`, and `Octave` changes in one of those places update the others.

Pad colors in `DRUM` and `PERFORM` follow Bitwig track, drum-lane, and clip colors. `Pad Brightness` and `Pad Saturation` control how strongly those project colors translate to the Fire LEDs.

### Shared controls

| Control | Action |
| --- | --- |
| `PLAY` | Toggle transport; when stopped, launch from the current play start |
| `ALT + PLAY` | Retrigger current clip |
| `STOP` | Stop transport; when already stopped, move play start to the beginning of the arrangement |
| `REC` | Clip launcher overdub in Drum XOX; arranger record in other modes; hold for pad-target recording in `PERFORM` |
| `ALT + REC` | Arranger overdub |
| `PATTERN` | Write automation |
| `PATTERN + REC` | Record the selected track into the next free launcher slot, regardless of mode |
| `SHIFT + PATTERN` | Metronome |
| `ALT + PATTERN` | Clip launcher overdub; when enabling, also enables automation write in touch mode |
| `KNOB MODE + PATTERN UP/DOWN` | Previous/next remote page for the active encoder page, when that page controls remotes |
| `KNOB MODE + touch encoder` | Reset that encoder's current value when the target supports reset |
| `SHIFT + DRUM` | Tap tempo |
| `SHIFT + NOTE` | Toggle record quantization, restoring the previous grid or `1/16` |
| `BROWSER` | Open or close Bitwig popup browser |
| `SHIFT + BROWSER` | Latch or close global settings overlay |
| `ALT + BROWSER` | Open browser after the current device / insertion context |
| `SHIFT + ALT + BROWSER` | Open browser before the current device / insertion context |

Plain `PLAY` does not relaunch the selected Launcher clip, so clips that were already offset keep that offset. Use `ALT + PLAY` to retrigger the selected clip from its start.

After a second `STOP` resets the arrangement start, the next `PLAY` also asks Bitwig to retrigger playing Launcher clips before launching from the play start.

When the popup browser is open, `SELECT` turn moves through results, `SELECT` press or `PLAY` commits the selected result, and `STOP` or `BROWSER` closes the browser.

When `KNOB MODE + PATTERN UP/DOWN` changes a remote page, the OLED shows the target page name and a bottom-row `N/M` count when there is more than one page. If the active encoder page has no remote target, the OLED reports `No remotes`. A single-page target reports `Page 1/1`, and page boundaries report `First page` or `Last page`. These chords are not treated as encoder-page cycles.

Hold `KNOB MODE` and tap an encoder to reset the value under that encoder. The OLED reports `No reset` or `Unmapped` when the current encoder slot cannot reset, and the `KNOB MODE` tap is not treated as an encoder-page cycle. Parameter-like controls use their existing target defaults for reset.

Use `PATTERN` for Bitwig 6's unified automation write toggle. `PATTERN` drives both arranger and launcher automation-write states together so the controller follows Bitwig 6's single Automation Write model. Use `ALT + REC` for arranger overdub, and use `ALT + PATTERN` for launcher overdub when adding data to existing Launcher clips. Turning launcher overdub on from the controller also enables automation write in touch mode; if that gesture enabled automation write, turning launcher overdub off turns automation write back off. `PATTERN + REC` is the quick launcher capture chord: it records the selected track into the next free launcher slot regardless of mode. Plain `REC` remains the arranger-record path from Perform Mix pages. Press `REC` again to stop a launcher recording started from either `PATTERN + REC` or `REC + pad`.

### Global settings overlay

Press `SHIFT + BROWSER` to latch the global settings overlay. Press `SHIFT + BROWSER` again, press `BROWSER`, or press a plain mode button from the latched overlay to close it. Press `KNOB MODE` to switch between settings pages.

| Page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Pitch` | Shared root key | Shared scale | Shared octave | Note OLED notes/chords: pads only or pads plus DAW |
| `Input` | Global velocity sensitivity | Global velocity center | Pad brightness | Pad saturation |
| `Clip` | `Create`: default empty-clip length | `Record`: launcher record length | -- | -- |
| `Pins` | Pin track | Pin device | Pin clip | -- |

On the `Pins` page, turn an encoder right for `On` and left for `Off`; the pin controls stop at those two states and do not wrap. The `Input` velocity settings are shared by live `NOTE`, `Drum Pads`, and `Poly Step` input. The global settings screen also shows whether launcher and mixer track views are using all tracks or only active tracks. Press the bottom-right pad from the overlay to toggle `Show deactivated tracks`; the same persistent option is available in the controller preferences and defaults to off.

The `Screen Message Hold` hardware preference controls how long transient OLED messages stay visible before persistent screens return: `Short` is 750 ms, `Normal` is 1.5 s, and `Long` is 3 s. While playback is running, idle OLED pages can return to mode-specific meters. When playback stops, those meters keep refreshing briefly so levels can ring out, then the idle OLED falls back to the selected track name instead of silent meter displays. The `Idle Perf & Drum OLED` hardware preference defaults to `Context`; set it to `Meters` to prefer decorative VU-style idle displays on Launcher/Mix and Drum XOX pages that already maintain visible-track or visible-pad meter data. The `Note OLED Notes/Chords` hardware preference defaults to `Pads`: live Note pads show notes/chords on the OLED, while selected-track DAW playback notes do not take over the idle display. Set it to `Pads + DAW` if you also want playing clip notes/chords to appear during playback.

Where the active mode has a four-encoder page, the OLED can keep a compact legend for the current encoder assignments. This is used by live Note and the shared step-sequencer encoder pages, including Drum XOX, Melo Gen, Poly Step, Nested Rhythm, and Fugue. The `Encoder Legend Position` hardware preference defaults to `Bottom`; set it to `Top` if the top row is easier to read in your controller setup.

### Baked Note Variation

Sequencer and generator modes can write stable, clip-wide variation into continuous per-note values. This is **Baked Note Variation**: values are written once into the notes and do not change again merely because the clip loops.

On an eligible note-parameter encoder:

- `SHIFT + ALT + turn` sets that parameter's session-local variation amount from 0% to 100%.
- `SHIFT + ALT + touch` writes a fresh variation to every note-on event in the active clip loop, across all pitches and MIDI channels.
- Turn the amount down to 0%, then use `SHIFT + ALT + touch`, to reset every targeted note to the mode's insertion baseline or expression centre.
- `KNOB MODE + touch` still resets the ordinary encoder target and takes priority over variation.

The first variation policy, `Scatter`, favors values near the current default and gives notes at the same onset the same value, so chords and layered unisons move together. Pitch expression stays within 12 semitones of its default. Held steps, selected notes, the visible page, and the selected drum pad do not narrow the operation. If the controller cannot observe the complete loop it reports `Clip too large` and changes nothing.

| Mode | Eligible mapped values |
| --- | --- |
| Drum XOX | Velocity, Pressure, Timbre, Pitch expression, Chance, Velocity spread, Note Gain, Note Pan |
| Poly Step | Velocity, Pressure, Timbre, Pitch expression, Chance, Velocity spread, Note Gain, Note Pan |
| Melo Gen | Velocity, Pressure, Timbre, Pitch expression, Chance, Velocity spread, Note Gain, Note Pan |
| Nested Rhythm | Velocity, Pressure, Timbre, Pitch expression, Chance |
| Fugue | Velocity and Chance |

In Drum XOX, Poly Step, and Melo Gen, go to `User 1` and press `ALT + KNOB MODE` to latch the secondary expression page. Its legend is `Velo / Gain / Pan / PExp`; press `ALT + KNOB MODE` again to return to `Velo / Pres / Timb / PExp`. Note Gain and Note Pan use ordinary edit/default/reset behavior on that latched page, so their `SHIFT + ALT` variation gesture needs no extra held button.

Applying variation again produces a new stable result. A later deliberate generator action in Melo Gen, Fugue, or Nested Rhythm may rewrite those values. Variation amounts are shared by parameter across compatible modes for the current extension session, but are not stored in controller preferences or project metadata.

### Main SELECT encoder

Tap `SELECT` to swap between `Last Touched Parameter` and the current alternate role. Press `SHIFT + SELECT` to cycle the alternate role.
Press `ALT + SELECT` to open or close the selected device window.

Global `SELECT` turn chords:

| Control | Action |
| --- | --- |
| Hold `SHIFT` + turn `SELECT` | Move the playback start by the current arranger grid resolution |
| Hold `SHIFT + PATTERN` + turn `SELECT` | Move the playback start by fine 1/16-beat steps |
| Hold `PATTERN` + turn `SELECT` | Jump to previous/next cue marker |
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

`Drum XOX` is the default sequencer-oriented workflow for a Drum Machine. Press `DRUM` again to cycle through `Multiclip Seq`, `Nested Rhythm`, and `Drum Pads`. If `Drum Mode Pinning` is `Auto-select First Drum Machine`, Drum XOX focuses and pins the first Drum Machine it finds. If it is `Follow Selection`, Drum XOX follows the selected drum context.

When Drum XOX is idle, the OLED shows the selected pad name with the current encoder legend when `Idle Perf & Drum OLED` is `Context`. Set `Idle Perf & Drum OLED` to `Meters` to show vertical RMS meters for the 16 visible Drum Machine pad chains with the same legend. On the `Mixer` encoder page, it instead shows selected-pad maximum peak/RMS on the first large row, current peak/RMS on the second large row, and the compact encoder legend.

| Pad row | Role |
| --- | --- |
| Row 1 | Clip slots |
| Row 2 | Visible drum slots |
| Rows 3-4 | 32 visible steps for the selected lane |

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Note length | Chance | Velocity spread | Repeats |
| `Mixer` | Selected pad volume | Selected pad pan | Selected pad send 1 | Selected pad send 2 |
| `User 1` | Velocity or default velocity | Pressure or default pressure | Timbre or default timbre | Pitch |
| `User 2` | Euclid length | Euclid pulses | Euclid rotation | Accent density |

| Control | Action |
| --- | --- |
| `STEP` | Enter `Poly Step` |
| `SHIFT + STEP` | Latch accent mode |
| Hold step pad(s) + `STEP` | Toggle accent on the held steps |
| `ALT + STEP` | Fill |
| `PATTERN UP/DOWN` | Page visible steps |
| `ALT + PATTERN UP/DOWN` | Scroll the visible Drum Machine pad window |
| `BANK LEFT/RIGHT` | Move or rotate pattern |
| `SHIFT + BANK LEFT/RIGHT` | Fine nudge |
| `ALT + BANK LEFT/RIGHT` | Halve / double clip length |
| `MUTE_1` | Select |
| `SHIFT + MUTE_1` | Toggle Mute pad mode |
| `MUTE_2` | Last Step |
| `SHIFT + MUTE_2` | Toggle Solo pad mode |
| `MUTE_3` | Copy / paste |
| `MUTE_4` | Delete / reset |

Hold one or more step pads, then use the timing gestures to move those held notes directly. Fine-nudged notes stay attached to the held target during the gesture.

Hold `MUTE_3` and press a clip slot, drum pad, or step to paste from the selected item of the same type. Clip-row paste falls back to the playing clip on that track if no clip was explicitly selected.

In `Drum Pads`, `Grid64` plays a 64-pad Bitwig Drum Machine window. The starting page puts C1 on the lower-left pad, then pads run left-to-right from the bottom row. LEDs use explicit Drum Machine pad colors when set; pads with a sound but no explicit color use the track color, and empty pads stay dark. Pressing a pad shows the Bitwig pad name. `PATTERN UP/DOWN` scrolls the pad window by 16 pads; `BANK LEFT/RIGHT` reminds you to use Pattern for this, while `ALT + BANK LEFT/RIGHT` still undo/redo Bitwig project history. On the `Channel` page, encoder 1 selects layouts (`Grid64`, `Velocity`, and `Bongos`) and encoder 2 controls velocity sensitivity / `SHIFT`: velocity center. In `Velocity` and `Bongos`, the left 4x4 block selects the sound. `Velocity` uses the remaining 12x4 pads as fixed velocity zones; `Bongos` leaves separator columns between the selector and two 5x4 bongo surfaces, uses hit velocity for note velocity, and maps surface position to per-note pressure.

### Multiclip Seq mode

`Multiclip Seq` is the second `DRUM` surface. It sequences up to sixteen full child tracks rather than sixteen Drum Machine pad channels, making independent clip lengths and play starts available for polyrhythms. Four Track Lanes are visible, but only the active Lane Clip is observed and editable at a time.

Set up the Bitwig project as follows:

- Put a Drum Machine device on a group track.
- Add one to sixteen direct child instrument or hybrid tracks and route their MIDI to that group.
- Put each pattern's child-track clips in the same launcher scene.
- Open the Detail Editor in Hybrid view and set `Edit only selected layers` to `Off` (recommended).

Direct child order is the lane contract. Positions 1-16 map to Lane 1-16, API MIDI channels 0-15, and Drum Machine notes 36-51. Track and clip names do not affect mapping; reordering tracks intentionally changes it.

| Pad row | Role |
| --- | --- |
| Rows 1-4 | Four child-track lane selectors; the active row shows and edits sixteen steps |
| Row 1 while `ALT` is held first | Momentary sixteen-scene selector |

| Control | Action |
| --- | --- |
| `DRUM` | Cycle to the next Drum surface |
| `PATTERN UP/DOWN` | Page Track Lanes by four |
| `BANK LEFT/RIGHT` | Page the shared time window by sixteen steps |
| Hold step(s) + `BANK LEFT/RIGHT` | Fine-nudge only the held notes by 1/64 |
| `ALT + BANK LEFT/RIGHT` | Move the active Lane Clip play start (lane rotation) |
| `SHIFT + ALT + BANK LEFT/RIGHT` | Fine-nudge all notes in the active Lane Clip by 1/64 |
| Channel encoder 1 | Change the active Lane Clip length in whole sequencer steps |
| `MUTE_1`-`MUTE_4` | Toggle the aligned child Track mute; bright means audible, dim means muted |
| `SHIFT + MUTE_1`-`MUTE_4` | Toggle the aligned child Track solo |
| `ALT + MUTE_1`-`MUTE_4` | Select the aligned child track without changing mute or solo |
| Hold `ALT`, then scene pad | Create missing child-track clips and make that scene the edit target without launching |
| Hold `ALT + SHIFT`, then scene pad | Create missing child-track clips, launch only those child clips, and edit the scene |
| `PATTERN UP/DOWN` while Scene Overlay is visible | Page scenes by sixteen |

The active lane is always the active Bitwig child track. Selecting a child clip from Bitwig reveals its lane page. Touching a sequencer pad on another row selects that child clip, waits for its cursor to settle, and then applies that initiating edit. Additional pad presses are blocked during the switch, and held pads on one row block input from other rows. `ALT + MUTE_n` selects without editing. Rows without an eligible child track remain off.

Selecting a Multiclip Scene creates a `Default Clip Length` clip in that exact visible slot on every eligible child track that does not already have one, waits until those child slots exist, then retargets only the active Lane Clip without launching. Add `SHIFT` to launch only the eligible child clips. Cursor retargeting temporarily blocks step writes, so a pad press cannot fall through to the previously selected scene. If a Lane Clip is independently removed later, its first inserted step recreates only that exact child slot and writes the step after the new clip cursor is confirmed.

Bitwig automatically displays a group-track **sub scene** when its child tracks contain Launcher clips in that scene. This is an alias and visual summary of the child clips, not an additional group-track clip created by Multiclip Seq. Scene Overlay LEDs ignore that group-level alias and derive their population, color, queued, and playing feedback only from eligible child-track clip slots.

Each child clip retains its own loop length and play start, while Fire displays the playhead and notes only for the active row. Existing notes on unexpected MIDI channels are preserved and can still be removed or nudged; the positional MIDI channel is the convention for newly inserted notes.

Multiclip Seq never selects the group track's scene or sub scene as an editing target. It creates exact child slots without selecting them, selects only the active child clip, and shows that Lane Clip in Bitwig's Detail Editor. It never walks through the other rows or depends on additive GUI clip selection. `Edit only selected layers: On` is not a supported Multiclip Seq template setting.

### Nested Rhythm mode

`Nested Rhythm` is the third `DRUM` surface. It generates rhythm from nested segment divisions, writes exact timing to a hidden fine clip grid, and projects the generated hits back to the Fire. It is suited to tuplets, ratchets, asymmetric subdivisions, and layered rhythmic structures that are awkward on a coarse step grid.

Entering the mode never overwrites an existing clip. If there is no selected clip, the OLED prompts you to select or create one. Creating a clip from the Nested Rhythm clip row generates a starter pattern. Existing clips are protected from encoder-driven generator changes until you explicitly press `PATTERN DOWN` to overwrite and claim the clip for Nested Rhythm editing.

| Pad row | Role |
| --- | --- |
| Row 1 | Clip row; becomes recurrence mask while a hit is held |
| Rows 2-3 | 32-bin projected rhythm view |
| Row 4 | First 16 generated hits in hit order, shown by velocity with playhead highlight |

If clip play start is shifted, the nearest visible projected-rhythm column is tinted blue across rows 2-3.

The `Channel` encoder page is the primary Nested Rhythm surface: it changes the generator structure. `User 1` and `User 2` shape generated or held-hit expression, while `Mixer` remains the shared track-control page.

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Density / `ALT`: density direction / `SHIFT`: recurrence | Tuplet / `ALT`: Tup.Div / `SHIFT`: target phase | Ratchet / `ALT`: Rat.Div / `SHIFT`: target phase | Cluster / `ALT`: play start / `SHIFT`: rate |
| `Mixer` | Volume | Pan | Send 1 | Send 2 |
| `User 1` | Velocity spread or held-hit velocity / `ALT`: center / `SHIFT`: rotate | Pressure spread or held-hit pressure / `ALT`: center / `SHIFT`: rotate | Timbre spread or held-hit timbre / `ALT`: center / `SHIFT`: rotate | Chance / `ALT`: baseline / `SHIFT`: rotate |
| `User 2` | Linear pitch | Pitch Expr spread or held-hit pitch expr / `ALT`: center / `SHIFT`: rotate | Clip length / `ALT`: play start / `SHIFT`: rate | Reset hit edits / `ALT`: ratchet mode |

| Control | Action |
| --- | --- |
| `DRUM` while in `XOX Drum mode` | Enter `Multiclip Seq` |
| `DRUM` while in `Multiclip Seq` | Enter `Nested Rhythm mode` |
| `DRUM` while in `Nested Rhythm` | Enter `Drum Pads` |
| `STEP` | Enter `Poly Step` |
| `ALT + STEP` | Fill |
| `PATTERN UP` or `ALT + MUTE_4` | Reset hit edits for the selected clip |
| `PATTERN DOWN` | Generate current nested rhythm into the selected clip |
| `BANK LEFT/RIGHT` | Move clip play start |
| `SHIFT + BANK LEFT/RIGHT` | Fine move clip play start |
| `SHIFT + both BANK buttons` | Snap clip play start back to the nearest coarse grid position |
| `ALT + BANK LEFT/RIGHT` | Halve / double clip length |
| `MUTE_1` | Select clip-row pads without launching |
| Hold `MUTE_2` + projected rhythm pad | Set last step within the 32-step edit view |
| `MUTE_3` | Copy / paste clip-row content |
| `MUTE_4` | Delete clip or hit target while held |
| Hold projected rhythm pad | Hold the nearest generated hit for recurrence or expression editing |
| Tap bottom-row hit pad | Toggle that hit |
| Hold bottom-row hit pad + expression encoder | Edit that hit directly |
| `SHIFT` + hit pad | Reset that hit's local edits |

Nested Rhythm reads the selected clip loop length from Bitwig when the clip is selected or the mode is activated. `Clip length` adjusts from that DAW value, and `ALT + BANK LEFT/RIGHT` halves or doubles it.

`Density` thins the generated phrase without stretching retained notes. `ALT + Density` toggles whether density prefers structurally strong hits or weaker decorative hits. `Cluster` packs retained hits into a smaller phrase region; `ALT + Cluster` adjusts clip play start.

`Tuplet` sets how many phrase spans are transformed, and `Tup.Div` sets how many divisions each selected span receives. `Ratchet` sets how many parent cells are subdivided, and `Rat.Div` sets the subdivision count. Tuplet and ratchet divisions support every integer from `x2` through `x16`; cells that are too short to split cleanly are skipped.

`Rate` scales the structural grid before tuplets and ratchets. Compound eighth-note meters such as `6/8`, `9/8`, and `12/8` use dotted-quarter anchors at `1x`; `3x` brings eighth-level detail back as optional material.

`Chance` is play probability. `User 1 / Encoder 4` adjusts chance, `ALT` sets its baseline, and `SHIFT` rotates the chance contour. Timing is not directly editable from the Fire in this mode; the pads are a projection of generated clip notes, not the literal Bitwig note grid.

### NOTE mode

`NOTE` is a 16x4 playing surface with melodic and harmonic input modes. The melodic input mode can be chromatic or in-key. The shared note octave is initialized from `Default Note Input Octave`.

| Area / Control | Role |
| --- | --- |
| Pad matrix | 16x4 note grid |
| Pad LEDs | Root, in-scale, and out-of-scale feedback |
| `NOTE` | Toggle between melodic note input and harmonic note input |
| `ALT + NOTE` | Toggle the current layout variant: chromatic / in-key in melodic and Poly Step builder input, bass columns / full field in harmonic input |
| `STEP` | Enter `Poly Step`; press again for `Melo Gen` |
| `SHIFT + STEP` | Toggle Bitwig Step Input helper for the selected clip |
| `BANK LEFT/RIGHT` | Shared octave down / up |
| `ALT + BANK LEFT/RIGHT` | Undo / redo Bitwig project history |
| `PATTERN DOWN/UP` | Next / previous shared scale |
| `ALT + PATTERN DOWN/UP` | Lower / higher shared root key |
| `MUTE_1` | Sustain |
| `MUTE_2` | Sostenuto |
| Tap `MUTE_3` | Toggle Note Repeat |
| Hold `MUTE_3` + turn `SELECT` | Choose the repeat division without changing whether Note Repeat is on or off |
| `MUTE_4` | Hold only the pad notes already down when Hold turns on; held-note pads blink and release when pressed again or Hold turns off |
| `KNOB MODE` | Cycle live-note encoder pages |

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Mod | Pitch bend | Pitch Gliss / `ALT`: gliss mode | Timbre |
| `Mixer` | Track volume | Track pan | Send 1 | Send 2 |
| `User 1` | Global velocity sensitivity / `SHIFT`: velocity center | Aftertouch | Breath | Pitch expression |
| `User 2` | Selected device remote 1 | Device remote 2 | Device remote 3 | Device remote 4 |

Note Repeat divisions run from fastest to slowest as `1/32T`, `1/32`, `1/16T`, `1/16`, `1/8T`, `1/8`, `1/4T`, and `1/4`; the default is `1/16`.

When live `NOTE` is idle, the OLED returns to useful selected-track context after transient encoder values. Playing Fire pads show the current note or detected chord on the OLED and hold that display briefly across short gaps so repeated notes do not flash the screen. On the `Mixer` page during playback or meter ring-out, it shows selected-track maximum Peak/RMS and current Peak/RMS with the compact encoder legend. On `Channel`, `User 1`, and `User 2`, it falls back to the selected track name with the current encoder-page legend. Device remote labels use mapped parameter names when Bitwig exposes them, otherwise the legend falls back to `D1`-style labels.

`SHIFT + STEP` opens the selected launcher clip in Bitwig's Detail Editor, selects Bitwig's Step Input tool, and tries to move the editor position to the first item. The OLED shows `Step Input`, an estimated one-based `Step N/M` position, and the current encoder footer. Fire pad note/chord gestures advance the estimate once per released note-entry gesture. While the helper is active, `BANK RIGHT` sends Bitwig's right-arrow command for rest/advance or held-note extension, and `BANK LEFT` sends left-arrow for back. Press `SHIFT + STEP` again to return Bitwig to the Pointer tool.

#### Harmonic input

Harmonic input is a scale-aware lattice built from the shared root, scale, and octave. Press `NOTE` while already in Note mode to toggle between melodic note input and harmonic input.

The harmonic field walks through every other scale degree, so neighboring pads tend to produce harmonically related stacks rather than adjacent scale steps. Rows move upward by octave and are offset two columns to the right, which keeps the same harmonic anchor recurring diagonally across the grid.

When bass columns are enabled, the first two columns are a single-note bass grid. The rest of the pads play harmonic stacks. When bass columns are disabled, all 16 columns become the harmonic field. `ALT + NOTE` toggles this directly, and the same setting is available on the harmonic `Mixer` page.

While harmonic input is active, the `Mixer` encoder page changes from track mixing to harmonic layout controls:

| Harmonic `Mixer` page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Mixer` | Notes per pad: 1, 2, or 3 | Octave span: 1-3 | Bass Grid on / off | Voicing: Drop 2, Close, or Open |

Harmonic input starts with 3 notes per harmonic pad, a 1-octave span, the `Drop 2` voicing, and the bass grid enabled. `Notes per pad` controls how many notes each harmonic pad produces before octave expansion. `Voicing` changes the register of that initial group, then `Octave span` adds octave copies above it. `Close` packs the notes above the pad's harmonic root; `Open` widens that close shape; and `Drop 2` preserves the original lattice voicing. Bass-grid pads always stay single-note, even when notes-per-pad or voicing changes. Hold several pads to play the union of their notes; overlapping notes sound once and remain active until the last pad containing them is released. `Pitch Gliss` remains available on Channel encoder 3.

### Melo Gen mode

Press `STEP` from `Poly Step` to enter `Melo Gen`; press `STEP` again to enter `Fugue`.

`Melo Gen` is a generative and editable mono phrase sequencer for basslines, motifs, and melodic hooks. It edits a 2-bar / 32-step window and keeps generated phrases constrained to the current pitch pool.

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
| `SHIFT + STEP` | Latch accent mode |
| Hold step pad(s) + `STEP` | Toggle accent on the held steps |
| `ALT + STEP` | Fill |
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
| `Channel` | Engine / `ALT`: subtype | Density / `ALT`: thin-fill current phrase | Pitch-pool octave / `ALT`: shared root / `SHIFT`: shared scale | Mutation type / `ALT`: strength |
| `Mixer` | Length / `ALT`: channel shape | Swivel / Mirror x2 / `ALT`: tension | Reverse / `ALT`: legato | Invert down / up / `ALT`: recurrence info |
| `User 1` | Velocity | Pressure | Timbre | Pitch |
| `User 2` | Note length | Chance | Velocity spread | Repeats |

Current generator modes are `Acid`, `Motif`, `Call/Resp`, `Rolling`, and `Octave`. If you manually edit the pitch pool, it becomes user-owned and is not auto-replaced on mode switch.

For recurrence editing, hold one or more active melodic steps. Row 1 becomes an 8-pad recurrence editor; tap pads to toggle recurrence hits, or hold the first pad as a span anchor and tap another pad to set the recurrence span.

### Poly Step mode

Press `STEP` from normal performance modes to enter `Poly Step`. Press `STEP` again to enter `Melo Gen`. Press `NOTE` to return to live note input.

`Poly Step` is the polyphonic note-step workflow for single notes or chords. The builder starts blank in chromatic view and uses the same shared root and scale as live `NOTE`.

| Pad row | Role |
| --- | --- |
| Row 1 | Clip row |
| Row 2 | Curated chord slots or builder notes |
| Rows 3-4 | 32 visible steps |

| Control | Action |
| --- | --- |
| Tap empty step | Place selected chord |
| Tap lit step | Remove chord |
| Hold source pitch/chord pad(s) + tap lit step | Replace its pitches while preserving timing, duration, velocity, probability, expression, and conditions |
| Hold step pad(s) + chord pad | Rewrite held steps with that chord |
| Tap chord pad with no held step | Audition chord, if enabled |
| `STEP` | Enter `Melo Gen` |
| `SHIFT + STEP` | Latch accent mode |
| Hold step pad(s) + `STEP` | Toggle accent on the held steps |
| `ALT + STEP` | Fill |
| `PATTERN UP/DOWN` | Page visible chord-step window |
| `SHIFT + PATTERN DOWN/UP` | Set builder latch off / on |
| `BANK LEFT/RIGHT` | Move clip start |
| `SHIFT + BANK LEFT/RIGHT` with no held steps | Fine move clip start |
| `SHIFT + both BANK buttons` with no held steps | Snap clip start back to the nearest coarse grid position |
| `ALT + BANK LEFT/RIGHT` | Halve / double clip length |
| Hold step(s) + `BANK LEFT/RIGHT` | Experimental micro-timing nudge for held chord material |

Each note or chord is owned by the step pad nearest to its note-on position. Fine nudge keeps that
ownership until the note-on crosses the midpoint to the neighboring step, then transfers it as one
event. A long note still lights, selects, and clears only from its owning pad; its duration may extend
across later steps without making those pads occupied.

| Left-side button | Action |
| --- | --- |
| `MUTE_1` | Select / load step |
| `MUTE_2` | Last Step target mode |
| `MUTE_3` | Paste to target step or clip slot |
| `MUTE_4` | Delete step or clip |

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Chord octave / `ALT`: shared root / `SHIFT`: shared scale | Global velocity sensitivity / `SHIFT`: velocity center | Pitch/chord set / `ALT`: set page | Interpretation / `ALT`: invert chord / `SHIFT`: builder layout |
| `Mixer` | Track volume | Track pan | Send 1 | Send 2 |
| `User 1` | Note velocity | Pressure | Timbre | Pitch |
| `User 2` | Note length | Chance | Velocity spread | Repeats |

On the `User 1` and `User 2` pages, an encoder edits held or selected notes when they are available. With no note
target, it instead changes the session-local default for the next note or chord inserted in `Poly Step`; touching
the encoder shows that default. Hold `KNOB MODE` and touch the encoder to restore its original default. These
defaults cover velocity, pressure, timbre, pitch expression, note length, chance, velocity spread, and repeats.

Chord banks are static libraries of chord formulas and voicing variants. Press several chord-set pads together to
combine their rendered pitches into one grip; the combined grip remains selected after release and can be auditioned,
written to empty steps, or used for source-first pitch replacement just like a multi-pad Builder grip. Changing
shared `Root Key` or `Scale` does not switch bank, page, or slot; it only changes how the selected slot or combined
grip is rendered. `As Is` transposes the stored chord shapes from the current root. `In Scale` rebuilds them from the
current shared scale and root.

Coarse nudge is intentionally disabled in `Poly Step`; micro-timing should still be smoke-tested after
changes to clip observation or note editing.

### Fugue mode

Press `STEP` from `Melo Gen` to enter `Fugue`. `Fugue` treats MIDI channel 1 as the source line and generates related lines on channels 2-4.

| Control | Action |
| --- | --- |
| `KNOB MODE` | Cycle active line: channel 1 source, then derived lines 2-4 |
| `MUTE_1`-`MUTE_4` | Enable / turn off the corresponding line; turning off a derived line clears its MIDI channel |
| `PATTERN UP` | Cycle preset for the active derived line |
| `PATTERN DOWN` | Reread channel 1 from the clip and rebuild derived lines |
| `BANK LEFT/RIGHT` | Adjust active line start; on channel 1, adjust clip length |
| `ALT + BANK LEFT/RIGHT` | Halve / double clip length |
| `STEP` | Enter `Poly Step` |
| Encoder turn on a derived-line page | Immediately rebuild that line with the new parameter |
| Channel 1 pads and encoders | Edit the source/template line |

| Active line | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| Channel 1 source | Shared root / `ALT`: source velocity | Shared scale / `ALT`: source chance | Clip length / `ALT`: source gate | Clip play start |
| Derived lines 2-4 | Direction / `SHIFT`: preset / `ALT`: velocity | Tempo / `ALT`: chance | Start / `ALT`: gate | Interval / `ALT`: octave jump |
| Held channel 1 pad | Velocity | Chance | Gate | Pitch |

When a sequencer clip start is shifted in `Poly Step`, `Nested Rhythm`, or `Fugue`, the nearest visible pad-grid column is tinted blue. Fine shifts use the nearest coarse column.

For a source-only clip, Fugue starts with derived lines 2-4 off. Enable a line to generate it from channel 1; turn it off to clear that generated MIDI channel. If channels 2-4 already contain notes when you enter Fugue, those notes are protected and the OLED prompts for `PATTERN DOWN`. Pressing `PATTERN DOWN` explicitly replaces all three derived channels and enables them for subsequent Fugue editing.

If you change channel 1 notes or expression directly in Bitwig, press `PATTERN DOWN` to update the generated lines from the current DAW clip state. Fugue deliberately avoids live regeneration during Bitwig note-editor drags, so you can audition an alternate source line in isolation and avoid rewriting derived notes while the DAW is still editing the source event.

For immediate derived-line feedback, change source expression from the controller instead. Controller-owned edits update the clip and regenerate from the controller's current source state.

### Launcher mode (`PERFORM`)

`PERFORM` opens the 16x4 launcher surface. Filled slots select and launch from the clip start. Empty slots create a new clip using `Default Clip Length`, then launch from the clip start.

`Perform Clip Launcher Layout` chooses whether the launcher starts as `LauncherV` or `LauncherH`. `ALT + PERFORM` still toggles the launcher layout for the current session; Mix remains a 16-track page.

| Control | Action |
| --- | --- |
| `REC` + pad | Record into the targeted launcher slot using `Launcher Record Length` |
| `PATTERN + REC` | Record the selected track into the next free launcher slot |
| `MUTE_1` + pad | Select without launching |
| `MUTE_2` | Double selected visible clip length |
| `SHIFT + MUTE_2` | Halve selected visible clip length |
| `MUTE_3` + pad | Paste selected clip to target slot |
| `MUTE_4` + pad | Delete |
| `BANK LEFT/RIGHT` | Scroll tracks by visible page |
| `SHIFT + BANK LEFT/RIGHT` | Scroll tracks by one |
| `ALT + BANK LEFT/RIGHT` | Undo / redo Bitwig project history |
| `PATTERN UP/DOWN` | Scroll scenes by visible page |
| `SHIFT + PATTERN UP/DOWN` | Scroll scenes by one |
| `KNOB MODE` | Cycle Launcher encoder pages |
| `PERFORM` while in Launcher | Toggle launcher / Scene Launch pad page |
| `ALT + PERFORM` | Toggle vertical/horizontal launcher layout; Mix stays 16 tracks |
| `SHIFT + PERFORM` | Toggle latched Mix pad page |
| `SHIFT + ALT + PERFORM` | Toggle Birds-Eye launcher navigation |

The Mix page rows are select, solo, mute, and arm for the 16 visible tracks. The select row uses each track's Bitwig color. Solo and mute pads do not change the selected track. Arm pads allow multiple armed tracks and do not change selection unless `Exclusive Track Arm` is enabled; with that preference on, arming a track selects it and disarms the other visible tracks. Hold `ALT` while pressing an arm pad to invert the preference for that press: additive arm when exclusive arm is enabled, or exclusive arm when it is disabled. On the select row, hold `ALT` and press a pad to stop that track. While the Mix page is active, `MUTE_1` jumps to the loop start or project start, `MUTE_2` and its nearby status LED light when any track is soloed and clear all solos, `MUTE_3` and its nearby status LED light when any track is muted and clear all mutes, and `MUTE_4` jumps to the loop end or zooms the arranger to the full project and jumps to the project end. Press `PATTERN DOWN` on Mix to switch from track actions to device view for devices 1-4, press `PATTERN DOWN` again for devices 5-8, and press `PATTERN UP` to step back through device pages and then return to track actions. In device view, hold `KNOB MODE` and press `PATTERN UP`/`PATTERN DOWN` to move the selected device remote page. Each column remains one visible track, rows 1-4 represent the current four-device page on that track, lit pads indicate enabled devices, dim pads indicate bypassed devices, and unoccupied device slots are off. Press a device pad to select it and show its device name on the OLED; hold the main encoder while pressing a device pad to select that device and open or close its window. The last selected device slot is remembered per track, so selecting that track again from the Mix select row restores the remembered device when it still exists. Hold `ALT` and press a device pad to toggle it on or off. Hold `ALT` and press `MUTE_1`-`MUTE_4` to toggle the matching visible device row across all visible tracks: if any occupied slot in the row is enabled, the row turns off; otherwise it turns on. The selected enabled device is brightest, and the selected bypassed device is softly lit. Entering device view switches the encoders to the selected device remote page, and returning to track actions restores the previous encoder page. Tap `KNOB MODE` to cycle the Launcher encoder pages while the Mix pad page is active.

If the selected device has layers, press `PATTERN DOWN` once more from device view to open the Device Layers page. Columns address the first 16 layers of the selected Instrument Layer, FX Layer, Instrument Selector, or FX Selector. Rows select, solo, mute, and turn the layer on or off. Press `PATTERN UP` to return to device view.

The Birds-Eye page is for large launcher sets. Each pad represents one launcher viewport block in the current vertical or horizontal layout; lit pads have tracks and scenes behind them, and the bright pad is the current viewport. Press a pad to jump both the track bank and scene bank to that block. Press `PERFORM` to leave Birds-Eye and return to the normal launcher page, or press `NOTE`, `DRUM`, or `STEP` to leave Birds-Eye and switch modes.

When the Launcher or Mix page is idle, the OLED shows the selected track name with the current encoder legend when `Idle Perf & Drum OLED` is `Context`. Set `Idle Perf & Drum OLED` to `Meters` to show vertical RMS meters for the visible tracks with that same legend. Track selection changes coming from Bitwig or another controller briefly show the selected track name before returning to the persistent idle display. On the Mix page's `Mixer` encoder page, the OLED shows selected-track maximum peak/RMS on the first large row, current peak/RMS on the second large row, and the compact encoder legend.

Hold `REC` and press a pad to target recording directly into that visible slot. Hold `PATTERN` and tap `REC` to record into the first free slot on the selected track, regardless of the active mode. `Default Clip Length` controls empty clip creation and is always a fixed length. `Launcher Record Length` controls launcher recording: fixed values set Bitwig's clip launcher post-record action to play the recorded clip after that length, `Manual` records until stopped without post-processing, and `Round` records until stopped, then rounds the recorded clip loop length to the nearest whole bar. Press `REC` again to end a launcher recording started from the controller and launch the recorded clip, even after switching to another mode. Filled MIDI clips can overdub MIDI according to Bitwig's clip launcher behavior; audio launcher clips do not support audio overdub, but automation can still be written with `ALT + PATTERN` launcher overdub, which enables `PATTERN` automation write in touch mode when needed.

The `Scene Launch` page keeps the same encoder and navigation controls as Launcher. Its top row addresses the 16 visible scenes: press a scene pad to launch, hold `MUTE_1` and press a scene pad to select it as the scene copy source, hold `MUTE_3` and press a scene pad to copy the selected scene to that target, and hold `MUTE_4` and press a scene pad to delete it. If no scene source is selected, scene copy falls back to the first visible scene with playing clips, then the first visible scene with recording clips. `MUTE_2` is unused on this page.

On `Scene Launch`, `BANK LEFT/RIGHT` also scrolls the visible scene window, with `SHIFT + BANK LEFT/RIGHT` scrolling by one scene.

On remote encoder pages, hold `ALT` while turning an encoder to control remotes 5-8 on the same page.

| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| `Channel` | Global Remote 1 | Global Remote 2 | Global Remote 3 | Global Remote 4 |
| `Mixer` | Selected track volume | Selected track pan | Selected track send 1 | Selected track send 2 |
| `User 1` | Selected track remote 1 | Track remote 2 | Track remote 3 | Track remote 4 |
| `User 2` | Selected device remote 1 | Device remote 2 | Device remote 3 | Device remote 4 |

In `SHIFT + PERFORM`, remote encoder pages idle to the selected track name with the compact encoder legend when `Idle Perf & Drum OLED` is `Context`. Set `Idle Perf & Drum OLED` to `Meters` to restore visible-track meters with that same legend. The legend uses mapped remote parameter names where available, and scoped fallback labels such as `G1`, `T1`, or `D1` when Bitwig reports only generic remote slots.

### Generative mode background

The control tables above describe where the functions are. This section describes what the generative modes are trying to do musically.

#### Poly Step background

`Poly Step` starts as an open pitch builder. The default `Builder` family lets you choose one note or a chord directly from the pad rows. It starts blank in chromatic view, so one selected source pad behaves like melodic step input and several selected source pads form a chord. With builder latch off, tapping a source pad replaces the current builder notes, while pressing several source pads together captures that grip as the current chord after release. Hold steps while changing the builder grip to rewrite those steps immediately. Conversely, hold source pads first and tap an occupied step to replace only its pitches while preserving its other note parameters; deactivate the step first when you want a completely fresh note. Use `SHIFT + PATTERN DOWN` for latch off and `SHIFT + PATTERN UP` for latch on. With builder latch on, source-pad taps add or remove notes from the builder until you change, invert, reset, or reload it. Use `SHIFT + encoder 4` on the `Channel` page, or `ALT + NOTE`, to switch the builder row between chromatic notes and in-key notes from the shared root and scale.

The preset banks are the more opinionated side of the mode. They use interval sets designed to move harmonic content without forcing a song key or a functional progression. Many of the voicings are deliberately open, so they tolerate inversion, transposition, and scale remapping better than tightly closed block chords. Changing shared `Root Key` or `Scale` does not move to a different preset slot; it changes how the selected slot is rendered.

The preset slots are also ordered to keep neighboring chords in a similar sounding range. The voicing generator favors stable outer contours, especially the bass and top note, so stepping through a bank tends to change the internal color without making the whole part jump wildly around the keyboard.

`Raw` interpretation keeps the chord shape as semitone intervals from the current root. This is useful when you want the exact color of the stored formula: dominant, diminished, quartal, cluster, sus, or drone shapes. `InKey` interpretation casts the formula through the current Bitwig scale, so the same slot becomes a scale-aware color shape. This is good for modal writing, parallel harmonic color, and material that will later pass through Bitwig scale-aware devices.

The available families mix the open builder with preset color banks:

| Family | Character |
| --- | --- |
| `Builder` | Manual chord source built from selected notes on the pad rows |
| `Audible` | Common chord formulas with compact, synth-friendly voicing behavior |
| `Barker` | Quartal and open-fifth colors adapted from Sam Barker's Octatrack chord-chain sequence |
| `Sus Motion` | Suspended second / fourth colors for unresolved movement |
| `Quartal` | Fourth-stacked harmony and open modal shapes |
| `Cluster` | Add9, 6/9, sus, and close-color rubs |
| `Minor Drift` | Minor, m6, m7, sus, and b6 colors for darker modal movement |
| `Dorian Lift` | Minor shapes with major-sixth / Dorian lift colors |
| `Root Drone` | Pedal-root shapes for drones, bass anchors, and static tonal centers |

Use the builder when you want to define the chord yourself from the current scale. Use the preset banks when you want harmonic color that can stay on one tonal center, shift with a mode, cast into another scale, or produce suspended / quartal / cluster material without writing a functional chord progression.

#### Melo Gen background

`Melo Gen` generates mono phrases, then lets you edit them as normal steps. All engines write into the current pitch pool and shared pitch context, but each engine has a different phrase grammar.

| Engine | Tendency |
| --- | --- |
| `Acid` | Root-oriented bass hooks with accents, slides, octave leads, and short answer figures |
| `Call/Resp` | A first-half call followed by a transformed reply: down, up, inverted-around-root, or cadential |
| `Rolling` | Repeating bass cells for driving low-end movement; useful for pocket, root drive, and late-lift patterns |
| `Octave` | Simple pulse material built around root / octave jumps; direct, forceful, and easy to steer |
| `Motif` | Small repeated phrase cells with tails, sequence replies, truncation/extension, and hook returns |

`Density` mainly changes how much of the phrase is populated. Acid spans from a three-hit metric backbone per 16 steps at `0.00` to its complete seeded rhythm skeleton at `1.00`, restoring stronger positions first. Rolling similarly grows from four characteristic family hits per 16 steps to an active note on every step, without changing the selected family as density moves. `Tension` allows wider or more colorful scale-degree movement. `Legato` encourages longer gates and slides where the engine supports them. Octave activity changes how willing the phrase is to jump registers. After generation, `ALT + PATTERN DOWN`, transform encoders, held-step editing, and pitch-pool edits are the intended way to keep an idea but bend it toward the part you need.

#### Nested Rhythm background

`Nested Rhythm` is a rhythm-structure generator. It builds nested metric divisions, ranks candidate pulses, then thins, clusters, ratchets, and shapes expression from that structure. The result is useful for tuplets, asymmetric subdivisions, layered percussion, and rhythms that would be slow to draw by hand on a fixed 16th grid.

The mode intentionally separates generated structure from local edits. `PATTERN DOWN` claims or rewrites the selected clip from the current generator state, while held-hit edits let you change recurrence and expression without abandoning the generated pattern. For more theory behind this mode, see `docs/nested-rhythm-musical-grounding.md`.

#### Fugue background

`Fugue` is based on contrapuntal transformation rather than random generation. MIDI channel 1 is the subject or template line. Channels 2-4 are generated as related voices, so the result behaves more like canon, imitation, augmentation, diminution, retrograde, and transposed answers than like a normal step sequencer.

Each derived line has its own direction, speed, start offset, and scale-aware pitch interval. Slowing a line down acts like augmentation; speeding it up acts like diminution. `Reverse` gives a retrograde version of the source, while `PingPong` reflects the line back through the phrase. Start offsets let the voices enter later, giving simple canon-like overlap. Pitch offsets move by scale degrees, so fifths, fourths, octaves, and wider answers stay connected to the shared root and scale.

The preset names are quick experiments against the current source line: `Bass /8`, `3rd 2trp`, `10th x2`, `High /2`, `Rev x2`, `5th x2`, `4th Rev`, and `8ve Ping`. They are meant to find useful relationships quickly, then you can adjust direction, speed, start, interval, velocity, chance, and gate by line.

Use `Fugue` when you already have a melodic idea and want related material around it: bass shadows, high echoes, doubled answers, reversed fragments, or fast decorative lines. It is Bach-inspired rather than a strict species-counterpoint checker; the goal is fast, playable related voices that can be muted, compared, and routed by MIDI channel in Bitwig.

## Preferences

### Launch Control XL preferences

- `Auto-attach to first Drum Machine and Arpeggiator`
- `Exclusive Track Arm`: record-arm buttons select the armed track and disarm the other visible strips when enabled; defaults off for standard multi-arm behavior
- `Audition on drum pad select`
- `Drum accent buttons momentary`
- `Show deactivated tracks`: includes deactivated tracks in Launch Control XL factory track banks; defaults off so deactivated tracks are skipped

### Akai Fire preferences

- `Clip Launch Mode`
- `Clip Launch Quantization`
- `Perform Clip Launcher Layout`
- `Default Clip Length`
- `Launcher Record Length`
- `Startup Mode`
- `SELECT Encoder Startup`
- `Default Root Key`
- `Default Scale`
- `Default Note Input Octave`
- `Melodic Seed Mode`
- `Default Velocity Sensitivity`
- `Melodic Fixed Seed`
- `Pad Brightness`
- `Pad Saturation`
- `Screen Message Hold`
- `Idle Perf & Drum OLED`: choose contextual idle text/values or visible-track/pad meters for Perform and Drum XOX
- `Encoder Legend Position`: choose whether compact OLED encoder legends appear on the bottom or top row
- `Note OLED Notes/Chords`: choose whether live Note pad notes/chords stay pad-only or also follow selected-track DAW playback notes
- `Euclid Scope`
- `Drum Mode Pinning`
- `Exclusive Track Arm`: arm pads select the armed track and disarm other visible tracks when enabled; defaults off for standard multi-arm behavior
- `Step Seq Pad Audition`
- `On-screen action notifications`

`Startup Mode` chooses the first active Akai Fire page when the extension starts. The available startup pages are `Note`, `Harmony`, `Drum XOX`, `Launcher`, and `Mix`.

`Pad Brightness` and `Pad Saturation` interact with the Bitwig track colors used by `DRUM` and `PERFORM`, so the same settings can read differently across different project palettes.

`Melodic Seed Mode` controls how `Melo Gen` chooses its initial generator seed when the controller session starts. `Random` starts each session from a new seed. `Fixed` starts from the configured `Melodic Fixed Seed` value, which makes the sequence of generated melodic phrases reproducible across reconnects or reloads. Each `Generate` press still advances forward from that starting point.

The melodic seed controls are grouped into their own `Generative control` preference section so they stay together in Bitwig's settings UI.

## Troubleshooting

### The wrong device is being controlled

- Check the relevant auto-attach or pinning preferences.
- In Fire `Drum XOX`, use `Follow Selection` to work on the currently selected drum track/device, or `Auto-select First Drum Machine` for a dedicated first-drum-machine workflow.
- Re-select the target track or device in Bitwig.
- Re-enter the relevant mode or template after changing focus.

### LEDs or mode state do not match what you expect

- Re-open the matching mode or user template.
- Power-cycle the controller if Bitwig started with another script attached first.
- Make sure the controller is using the expected template or default mapping.

## Attribution

- `rhbitwig` by Richie Hawtin and Eric Ahrens provided the Akai Fire drum sequencer which has been adapted for use here, as well as the arp workflow used on the LCXL User Template 8.

- The fine-grid step nudging is based on Wim Van den Borre's `AkaiFireNudger` fork of `rhbitwig`, and developed further here.

- Note/chord OLED detection is adapted from Wim van den Borre's `BitX`: https://github.com/wimvandenborre/BitX

- The `Barker` chord family is adapted from Sam Barker's Octatrack chord-chain MIDI/sample workflow: https://www.voltek-labs.net/octatrack

- All LCXL factory modes were taken from Bitwig's original `bitwig-extensions` for the Launch Control XL controller 
