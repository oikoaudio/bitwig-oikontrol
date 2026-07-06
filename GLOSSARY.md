# Glossary

Durable project vocabulary and domain language.

This file defines terms that agents and humans should use consistently. It is not a PRD, task list, implementation plan, changelog, scratchpad, or index of every class name.

## Controller and workflow terms

**Akai Fire**: The main instrument-like Oikontrol surface for drum sequencing, live note input, step-sequencing, generative modes, and performance workflows.

**Launch Control XL**: The Novation Launch Control XL Mk2 extension that preserves Bitwig factory-template workflows and adds custom user-template pages for device, drum-machine, and arpeggiator control.

**Mode Family**: A top-level Akai Fire workflow selected by hardware mode buttons, currently `DRUM`, `NOTE`, `STEP`, and `PERFORM`.

**Encoder Page**: An Akai Fire encoder bank selected with `KNOB MODE`, where each mode owns the meanings of the four encoders on shared pages such as `Channel`, `Mixer`, `User 1`, and `User 2`.

**Main SELECT Encoder**: The large Akai Fire encoder used for global roles such as last-touched parameter, shuffle, tempo, note repeat, track selection, drum-grid resolution, and popup-browser navigation.

**Global Settings Overlay**: The latched `SHIFT + BROWSER` Akai Fire overlay for shared pitch, velocity feel, pad color response, clip lengths, cursor pinning, and visible-track policy.

**Popup Browser**: Bitwig's browser as opened, closed, navigated, and committed from Akai Fire hardware gestures.

**Remote Page Navigation**: Controller navigation through Bitwig remote-control pages for the active encoder target.

**Launcher Recording**: Akai Fire recording into Bitwig launcher slots from controller gestures such as `REC + pad` or `PATTERN + REC`.

**Factory Template**: A Launch Control XL template that preserves Bitwig factory-style mixer, send, track, and device behaviour.

**User Template**: A Launch Control XL user-mode template used by Oikontrol for custom workflows.

**Drum Layer**: The Launch Control XL User Template 7 workflow for Drum Machine pad volume, pad remotes, pad selection, audition, mute, and solo.

**Arp Layer**: The Launch Control XL User Template 8 workflow for controlling a focused Bitwig Arpeggiator with pitch, velocity, gate, and step-behaviour rows.

## Modes

**Drum XOX**: The default Akai Fire `DRUM` x0x-style Drum Machine step sequencer.

**Nested Rhythm**: The Akai Fire `DRUM` structural rhythm generator that creates deterministic patterns from nested divisions, tuplets, ratchets, density, clustering, and editable generated hits.

**Drum Pads**: The live Akai Fire Drum Machine playing surface with `Grid64`, `Velocity`, and `Bongos` layouts.

**NOTE Mode**: The Akai Fire live 16x4 note-input surface for melodic and harmonic playing with shared pitch state and live performance controls.

**Harmonic Input**: The scale-aware NOTE-mode lattice for entering one to three related notes per pad from shared root, scale, and octave state.

**Melodic Step**: The Akai Fire `STEP` mono phrase sequencer for generated and editable basslines, motifs, and melodic hooks.

**Chord Step**: The Akai Fire `STEP` chord-oriented sequencing workflow for builder chords, preset chord banks, and shared-pitch chord progression editing.

**Fugue**: The Akai Fire `STEP` mode that derives related MIDI lines from a source line using direction, speed, start offset, and scale-aware interval settings.

**Launcher**: The primary Akai Fire `PERFORM` page for clip launching and clip creation in vertical or horizontal track/scene orientation.

**Scene Launch**: The Akai Fire `PERFORM` page that addresses scenes from the top pad row while retaining launcher navigation and encoder behaviour.

**Mix**: The Akai Fire `PERFORM` pad page for visible-track select, solo, mute, arm, and device/layer actions.

**Device Layers**: The Akai Fire `PERFORM` page for selecting, soloing, muting, or enabling visible layers of layer-capable Bitwig devices.

**Birds-Eye**: The Akai Fire `PERFORM` overview page where each pad jumps the launcher viewport to a larger track/scene block.

## Sequencing and rhythm terms

**Step**: A time position in a step sequencer mode whose editing and clip-writing semantics are owned by that mode.

**Clip Row**: The top pad row in sequencer modes used for clip selection, creation, launch, copy/paste, or temporary mode-specific editing.

**Recurrence**: Bitwig note recurrence as edited from controller step modes using a span length and bitmask.

**Accent Mode**: A controller editing state that applies accent velocity to held or newly edited steps.

**Fine Nudge**: The Akai Fire timing gesture for moving held step material on a finer grid than the visible step grid.

**Euclidean Controls**: Drum XOX generation controls for Euclidean length, pulses, rotation, and accent density.

**Generated Hit**: A Nested Rhythm pulse event projected onto the Fire pads and written to the clip with timing, role, duration, velocity, expression, chance, recurrence, and edit overlays.

**Projected Rhythm View**: The 32-bin Nested Rhythm pad projection that shows generated timing against the controller grid rather than the literal Bitwig note grid.

**Tuplet**: A Nested Rhythm transformation that divides selected phrase spans into alternate subdivisions before ratchets are applied.

**Ratchet**: A Nested Rhythm transformation that locally subdivides selected parent regions after tuplets are applied.

**Density**: A generator control that thins or fills rhythmic or melodic material while preserving the intended phrase identity where possible.

**Cluster**: A Nested Rhythm generator control that concentrates retained hits into a smaller phrase region.

**Metric Indispensability**: The Nested Rhythm ranking idea that pulse positions in a meter have unequal structural importance.

**Chance**: Bitwig note play probability as edited or generated by controller modes that own the relevant clip-write path.

## Musical terms

**Shared Pitch Context**: The Akai Fire shared root, scale, and octave state used by `NOTE`, Melodic Step, Chord Step, and the global settings overlay.

**Pitch Pool**: The Melodic Step row of selectable notes that constrains generated and edited mono phrases.

**Melodic Generator**: A Melodic Step phrase engine that produces a `MelodicPattern` from phrase context and generator parameters.

**Melodic Mutator**: The Melodic Step process that changes an existing generated phrase while preserving or changing selected musical aspects.

**Chord Builder**: The Chord Step manual chord source where builder pads select notes and render the current chord.

**Builder Latch**: The Chord Step builder setting that switches source pads between latch-off replacement/grip entry and latch-on gradual add/remove chord construction.

**Chord Bank**: A static library of Chord Step preset chord formulas and voicing variants.

**Chord Interpretation**: The Chord Step choice between keeping a stored semitone shape as `Raw` or casting it through the current Bitwig scale as `InKey`.

**Source Line**: In Fugue, the MIDI channel 1 template line used to regenerate derived lines.

**Derived Line**: In Fugue, a generated line on MIDI channels 2–4 derived from the source line.

**Fugue Direction**: The Fugue line direction transform, currently `Forward`, `Reverse`, or `PingPong`.

**Fugue Speed**: The Fugue line timing transform that maps source steps into slower augmentation-like or faster diminution-like derived phrases.

## Physical control terms

**Pad Matrix**: The Akai Fire 16x4 RGB pad grid that modes map to clip rows, drum slots, step rows, pitch pools, harmonic input, launcher slots, track actions, and generated-hit projections.

**Pad Brightness**: The Akai Fire setting that controls how strongly Bitwig colors translate to Fire LEDs.

**Pad Saturation**: The Akai Fire setting that controls how saturated Bitwig-derived Fire LED colors appear.

**Velocity Settings**: The shared Akai Fire input-velocity policy used by modes that map pad velocity through adjustable sensitivity and centre targets.

**Encoder Touch Reset**: The explicit Akai Fire `KNOB MODE + touch encoder` gesture for resetting the current encoder target when that target supports reset.

**OLED Feedback**: Akai Fire screen feedback for persistent mode state, meters, encoder targets, and transient action messages.

**Peak/RMS Metering**: Akai Fire OLED metering that distinguishes current peak/RMS readings from max-level tracking used for quick gain staging.

## Architecture and code ownership terms

**Mode**: In Akai Fire architecture, a mode owns lifecycle and high-level composition for a controller workflow.

**ControlBindings**: The mode-specific physical controller boundary for pads, encoders, main/select knob, mode buttons, bank buttons, mute buttons, and lights bound to a Bitwig `Layer`.

**PhysicalControls**: An ADR vocabulary near-synonym for `ControlBindings`, but not the preferred current code term where `ControlBindings` is established.

**PadControls**: The pad-specific part of a mode's physical-control boundary when the 64-pad matrix is complex enough to name separately.

**EncoderControls**: A mode-specific owner for encoder page construction and encoder turn/touch behaviour.

**ButtonControls**: A mode-specific owner for cohesive button gesture policies.

**PadController**: An interaction owner that interprets pad gestures and delegates musical edits to mode-specific hosts or controllers.

**PadInteractionState**: The pure held-pad and pad-session state for gesture consumption, recurrence-row holds, and similar pad-session facts.

**PatternState**: An owner for editable, current, base, or observed step-pattern state when a mode has a step-pattern model.

**EditablePattern**: A generated event model plus local editable overlays.

**ClipWriter**: An owner for writing a mode-owned pattern model to a Bitwig clip.

**ClipController**: An owner for selected clip availability, clip creation, navigation, observation coordination, or direct clip editing workflows.

**ObservationController**: An owner for Bitwig note/clip observers, observed caches, delayed refreshes, and conversion into focused read models.

**Orchestrator**: In Launch Control XL architecture, the main extension class that decides the active layer, manages mode flags, routes incoming MIDI/SysEx, and delegates workflow behaviour.

**Adapter**: A host or hardware edge wrapper that can be swapped or tested separately from pure behaviour.

**Pure Helper**: A helper that computes behaviour without depending on Bitwig host state.

**State Snapshot**: An immutable or focused view of live controller state passed into rendering or logic so view behaviour stays pure.

**Layer Activation**: The controller-script lifecycle behaviour that enables the correct hardware layer after controls, matchers, and binding managers exist.

**Common Module**: The `modules/common/` module containing copied or shared Bitwig framework helpers.

## Historical or deprecated terms

**ChordStepSurfaceLayer**: A historical Chord Step extraction artifact from the transition away from live-note-surface inheritance.

**SurfaceLayer**: A historical or transitional architecture term for an internal mode-owned hardware/control surface.

## Aliases to avoid

**Context file**: Use **Glossary** for durable project vocabulary and **PRD** or **Issue** for feature intent and task execution.

**Agent-ready**: Use **Ready for agent** when following the project issue-state vocabulary.

**PhysicalControls**: Prefer **ControlBindings** when referring to current mode-owned binding classes.

## Flagged ambiguities

**Step**: Use **Sequencer step** for musical timing positions and **Slice issue** for implementation work.

**Mode**: Use **Mode Family** for top-level hardware-button workflows and **Mode** for a concrete controller workflow owner.

**Clip Row**: Clarify the active mode when discussing clip-row behaviour because the same physical row owns different actions in different modes.

**SurfaceLayer**: Treat as historical unless discussing older architecture or an explicit Bitwig `Layer` activation scope.
