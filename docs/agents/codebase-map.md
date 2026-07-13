# Codebase map

Akai Fire class names below are rooted under `modules/akai-fire/src/main/java/com/oikoaudio/fire/` unless a full path is shown.

## Top-level modules

- `modules/akai-fire/` is the main active development area for Akai Fire modes.
- `modules/launchcontrol/` is the Launch Control XL extension; do not apply Akai Fire assumptions there unless explicitly intended.
- `modules/common/` contains copied/shared Bitwig framework helpers. Avoid broad changes there unless required by both controllers.
- `modules/oikontrol/` contains package/output resources for the Oikontrol extension build.

## Shared Akai Fire areas

- `modules/akai-fire/src/main/java/com/oikoaudio/fire/control/` owns shared physical control helpers: encoder behaviour, touch reset, value profiles, RGB/bi-color button wrappers, and interaction policies used by multiple modes.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/music/` owns shared pitch and musical context.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/sequence/` owns drum and shared note-step sequencing behaviour, including selected-note clip coordination.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/nestedrhythm/` owns nested rhythm generation and playback logic.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/melodic/` owns melodic step generation.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/note/` owns live pitched and harmonic note input.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/chordstep/` owns chord-step sequencing.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/fugue/` owns fugue sequencing and transformation.
- `modules/akai-fire/src/main/java/com/oikoaudio/fire/perform/` owns clip launcher / perform mode.

## Akai Fire application shell

- `AkaiFireOikontrolExtension.java` is the composition root and top-level mode/layer lifecycle owner.
- `FirePreferences.java` owns Bitwig preference handles and exposes typed values and callbacks.
- `GlobalSettingsOverlayController.java`, `PopupBrowserController.java`, `GlobalMainEncoderController.java`, and
  `DrumAutoPinController.java` own their named global interaction workflows. `MainEncoderRouting.java` is the
  concrete mode-facing boundary for global main-encoder roles, modifiers, browser precedence, and reset policy.
  These helpers keep mode-local controls from treating the complete extension as a service bag.
- Global physical state and reusable encoder/button policies belong under `control/`; mode-specific musical
  semantics remain in the active mode package.

## Nested Rhythm mode

- `NestedRhythmMode.java` composes the mode lifecycle and coordinates its focused collaborators.
- `NestedRhythmGenerator.java` owns deterministic rhythm generation, density thinning, clustering, timing, durations, priorities, and generated velocities.
- `NestedRhythmContourShaper.java` owns generated pressure/timbre/pitch/chance/recurrence shaping.
- `NestedRhythmParameterState.java` owns structural and expression parameter snapshots. `NestedRhythmMode.java`
  completes its `EncoderBankLayout` before constructing `StepSequencerEncoderLayer`, and owns that physical
  layer's activation/deactivation directly.
- `NestedRhythmPadSurface.java` owns pad gestures and projection; `NestedRhythmEditablePattern.java` owns local hit
  overlays; `NestedRhythmClipWriter.java` owns clip writes and pending writeback patches.
- `SelectedNoteClipCoordinator.java` supplies the shared selected-note clip availability and refresh workflow used
  by Nested Rhythm and Melodic Step.
- `NestedRhythmPattern.java` is the generated event model and role vocabulary.
- `NestedRhythmLoopLength.java` and `NestedRhythmPlayStart.java` own clip length and play-start helpers.
- Tests live under `modules/akai-fire/src/test/java/com/oikoaudio/fire/nestedrhythm/`; start with `NestedRhythmGeneratorTest` for generated musical behaviour.

## Drum Step mode

- `DrumSequenceMode.java` is the main step sequencer mode.
- `DrumStepPadSurface.java` owns pure pad gesture/session decisions; `DrumPadHandler.java` applies those decisions to
  the Bitwig clip boundary.
- `StepSequencerEncoderLayer.java`, `StepPadLightHelper.java`, `RecurrenceEditor.java`, and the small
  `ClipRow*`/`SequencerEdit*` helpers own shared sequencer controls, lights, recurrence, and edit policies.
- Step-style modes must pass a completed `EncoderBankLayout` explicitly to `StepSequencerEncoderLayer`; the shared
  layer never queries a partially constructed host for its layout.

## Melodic Step mode

- `MelodicStepMode.java` is the main mode.
- `MelodicGenerator` implementations such as `MotifGenerator`, `AcidGenerator`, `EuclideanPhraseGenerator`, `RollingBassGenerator`, and `SparseGenerator` own generated phrase styles.
- `MelodicStepPatternState.java`, `MelodicStepClipWriter.java`, and `SelectedNoteClipCoordinator.java` own current/base
  pattern state, clip writes, and selected-note clip coordination.
- `MelodicPitchPoolController.java`, `MelodicStepEncoderLayout.java`, and `MelodicStepPadSurface.java` own pitch
  selection/audition, immutable encoder-bank mapping, and pad gestures. `MelodicStepMode` owns the shared physical
  encoder layer lifecycle.
- `MelodicRenderer.java`, `MelodicMutator.java`, `MelodicClipAdapter.java`, and `MelodicPattern.java` own rendering,
  mutation, clip access, and pattern data.

## Live Note and Drum Pad play modes

- `NotePlayMode.java` and `DrumPadPlayMode.java` are the mode entry points.
- `LivePadSurfaceLayer.java` holds shared live pad-surface behaviour.
- `NoteLivePadPerformer.java`, `NoteLivePerformanceControls.java`, `NoteLiveExpressionControls.java`, and `NoteLiveEncoderModeControls.java` own live play behaviour and controls.
- `StepInputWorkflowController.java` owns delayed Step Input activation, cancellation, navigation, display state, and
  cleanup for live note and drum-pad surfaces.
- Layouts live in `NoteGridLayout.java`, `HarmonicLatticeLayout.java`, and `DrumMachinePadLayout.java`.

## Poly Step mode (`ChordStep*` internally)

The user-facing mode is named Poly Step because it sequences single notes as well as chords. The established
package and implementation class names remain `chordstep`/`ChordStep*`.

- `ChordStepMode.java` is the sole public package entry point and explicit composition root. Its constructor is
  grouped into musical/edit state, clip observation/mutation, pads/feedback, buttons/encoders, and physical
  binding/activation phases.
- `ChordStepPadController.java`, `ChordStepPadSurface.java`, and `ChordStepPadLightRenderer.java` are the primary
  pad input, held-gesture state, and feedback path. There is no forwarding pad/surface facade.
- `ChordStepClipController.java` owns selected-clip state and availability;
  `ChordStepObservationController.java` owns resync coalescing and delayed refresh passes;
  `ChordStepClipResources.java`, `ChordStepClipEditor.java`, and `ChordStepClipNavigation.java` isolate distinct
  Bitwig resource, mutation, and navigation boundaries.
- `ChordStepFineNudgeSession.java` owns held/pending BANK gesture state while
  `ChordStepFineNudgeWriter.java` owns clip rewrites and delayed in-flight suppression.
- `ChordStepModeButtons.java` owns stateless STEP/PATTERN/pitch-context policy;
  `ChordStepBankButtonControls.java`, `ChordStepAccentControls.java`, and `ChordStepEditControls.java` retain their
  independently stateful interaction responsibilities.
- `ChordStepEncoderControls.java` owns semantic encoder gestures and produces the completed bank layout;
  `StepSequencerEncoderLayer` owns physical page binding/lifecycle. `ChordStepControlBindings.java` receives
  concrete controls, and only `ChordStepMode` references the complete extension.
- `ChordStepWorkflowScenarioTest.java` covers the principal edit/feedback, fine-nudge, selected-clip, audition,
  and cancellation paths using real responsibility owners plus focused hardware/Bitwig fakes.

## Perform mode

- `PerformClipLauncherMode.java` is the lifecycle/composition shell and Bitwig effect adapter.
- `PerformPageState.java` owns exclusive page/subpage truth; `PerformObservationState.java` owns observed launcher,
  track, device, and meter state.
- `PerformPadRenderer.java` owns pure pad-light decisions; `PerformLauncherNavigationController.java`,
  `PerformMixController.java`, `PerformDeviceLayersController.java`, and `PerformEncoderControls.java` own their named
  interaction policies.
- `PerformLayout.java` owns layout constants.

## Fugue mode

- `FugueStepMode.java` is the lifecycle and musical-write coordinator.
- `FugueControlBindings.java`, `FugueEncoderControls.java`, `FugueTemplatePadController.java`, and
  `FugueObservationController.java` own physical bindings, encoder state/presentation, template-pad edits, and
  source/playback observation.
- `FugueRenderer.java`, `MelodicLineTransformer.java`, and `ScaleAwareTransposer.java` are pure rendering and musical
  transformation helpers.

## Launch Control XL

Launch Control class names below are rooted under
`modules/launchcontrol/src/main/java/com/bitwig/extensions/controllers/novation/launch_control_xl/`.

- `LaunchControlXlControllerExtension.java` is the composition root and template/layer lifecycle orchestrator. It
  constructs Bitwig objects and hardware layers, adapts controller ports, samples immutable UI snapshots, applies
  rendered LED frames, and sends MIDI.
- `factory/FactoryLayerController.java` owns factory-template interaction state and policy;
  `factory/FactoryUiSnapshot.java` and `factory/FactoryLedRenderer.java` own immutable render input and pure factory
  LED decisions.
- `DrumLayerController.java` owns Drum template interaction and delegates pure decisions to the helpers under
  `drum/`; `arp/RhArpLayerController.java` owns Arp template interaction.
- `DevicePagesController.java` owns the Device Pages user-template workflow.
- `support/HardwareBindingManager.java` owns matcher attachment/clearing. The remaining `support/` classes adapt
  host notifications, note input, template parsing, layer state, and device discovery.
