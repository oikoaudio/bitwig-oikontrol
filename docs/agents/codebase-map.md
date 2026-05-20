# Codebase map

## Top-level modules

- `modules/akai-fire/` is the main active development area for Akai Fire modes.
- `modules/launchcontrol/` is the Launch Control XL extension; do not apply Akai Fire assumptions there unless explicitly intended.
- `modules/common/` contains copied/shared Bitwig framework helpers. Avoid broad changes there unless required by both controllers.
- `modules/oikontrol/` contains package/output resources for the Oikontrol extension build.

## Shared Akai Fire areas

- `modules/akai-fire/src/main/java/com/oikoaudio/fire/control/` owns shared physical control helpers: encoder behaviour, touch reset, value profiles, RGB/bi-color button wrappers, and interaction policies used by multiple modes.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/music/` owns shared pitch and musical context.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/sequence/` owns drum and step-sequencer behaviour.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/nestedrhythm/` owns nested rhythm generation and playback logic.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/melodic/` owns melodic step generation.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/note/` owns live pitched and harmonic note input.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/chordstep/` owns chord-step sequencing.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/perform/` owns clip launcher / perform mode.

## Nested Rhythm mode

- `NestedRhythmMode.java` owns controller interaction, encoder pages, pad projection, clip writes, and editable generated pulses.
- `NestedRhythmGenerator.java` owns deterministic rhythm generation, density thinning, clustering, timing, durations, priorities, and generated velocities.
- `NestedRhythmContourShaper.java` owns generated pressure/timbre/pitch/chance/recurrence shaping.
- `NestedRhythmPattern.java` is the generated event model and role vocabulary.
- `NestedRhythmLoopLength.java` and `NestedRhythmPlayStart.java` own clip length and play-start helpers.
- Tests live under `modules/akai-fire/src/test/java/com/oikoaudio/fire/nestedrhythm/`; start with `NestedRhythmGeneratorTest` for generated musical behaviour.

## Drum Step mode

- `DrumSequenceMode.java` is the main step sequencer mode.
- `StepSequencerEncoderHandler.java`, `DrumPadHandler.java`, `StepPadLightHelper.java`, and `RecurrenceEditor.java` handle shared sequencer controls, pads, lights, and recurrence.

## Melodic Step mode

- `MelodicStepMode.java` is the main mode.
- `MelodicGenerator` implementations such as `MotifGenerator`, `AcidGenerator`, `EuclideanPhraseGenerator`, `RollingBassGenerator`, and `SparseGenerator` own generated phrase styles.
- `MelodicRenderer.java`, `MelodicMutator.java`, `MelodicClipAdapter.java`, and `MelodicPattern.java` own rendering, mutation, clip access, and pattern data.

## Live Note and Drum Pad play modes

- `NotePlayMode.java` and `DrumPadPlayMode.java` are the mode entry points.
- `PitchedSurfaceLayer.java` holds shared pitched-surface behaviour.
- `NoteLivePadPerformer.java`, `NoteLivePerformanceControls.java`, `NoteLiveExpressionControls.java`, and `NoteLiveEncoderModeControls.java` own live play behaviour and controls.
- Layouts live in `NoteGridLayout.java`, `HarmonicLatticeLayout.java`, and `DrumMachinePadLayout.java`.

## Chord Step mode

- `ChordStepMode.java` is the mode entry point.
- `ChordStepController.java`, `ChordStepEditControls.java`, `ChordStepClipController.java`, and observation classes own edit behaviour, clip writes, and observed state.

## Perform mode

- `PerformClipLauncherMode.java` is the clip launcher mode.
- `PerformLayout.java` owns layout constants.

## Shared control helpers

- `modules/akai-fire/src/main/java/com/oikoaudio/fire/control/` owns encoder scaling/acceleration, touch reset, RGB/bi-color button wrappers, and physical-control policies used by multiple modes.
