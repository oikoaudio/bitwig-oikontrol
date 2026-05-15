package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;

import java.util.Map;
import java.util.Set;

/**
 * Aggregates the chord-step surface collaborators while the legacy pitched-surface adapter is being removed.
 */
public final class ChordStepSurfaceController {
    private final ChordStepPadSurface padSurface;
    private final ChordStepPadControls padControls;
    private final ChordStepAccentControls accentControls;
    private final ChordStepStepButtonControls stepButtonControls;
    private final ChordStepBankButtonControls bankButtonControls;
    private final ChordStepPatternButtonControls patternButtonControls;
    private final ChordStepPitchContextControls pitchContextControls;
    private final ChordStepChordSelection chordSelection;
    private final ChordStepBuilderController builder;
    private final ChordStepFineNudgeState<ChordStepEventIndex.Event> fineNudgeState;
    private final ChordStepFineNudgeController<ChordStepEventIndex.Event> fineNudgeController;
    private final ChordStepFineNudgeWriter fineNudgeWriter;
    private final ChordStepClipController clipController;
    private final ChordStepClipEditor<ChordStepEventIndex.Event> clipEditor;
    private final ChordStepClipNavigation clipNavigation;
    private final ChordStepObservationController observationController;
    private final ChordStepController controller;
    private final ChordStepClipResources clips;
    private final ChordStepEventIndex eventIndex;

    public ChordStepSurfaceController(
            final ChordStepPadSurface padSurface,
            final ChordStepPadControls padControls,
            final ChordStepAccentControls accentControls,
            final ChordStepStepButtonControls stepButtonControls,
            final ChordStepBankButtonControls bankButtonControls,
            final ChordStepPatternButtonControls patternButtonControls,
            final ChordStepPitchContextControls pitchContextControls,
            final ChordStepChordSelection chordSelection,
            final ChordStepBuilderController builder,
            final ChordStepFineNudgeState<ChordStepEventIndex.Event> fineNudgeState,
            final ChordStepFineNudgeController<ChordStepEventIndex.Event> fineNudgeController,
            final ChordStepFineNudgeWriter fineNudgeWriter,
            final ChordStepClipController clipController,
            final ChordStepClipEditor<ChordStepEventIndex.Event> clipEditor,
            final ChordStepClipNavigation clipNavigation,
            final ChordStepObservationController observationController,
            final ChordStepController controller,
            final ChordStepClipResources clips,
            final ChordStepEventIndex eventIndex) {
        this.padSurface = padSurface;
        this.padControls = padControls;
        this.accentControls = accentControls;
        this.stepButtonControls = stepButtonControls;
        this.bankButtonControls = bankButtonControls;
        this.patternButtonControls = patternButtonControls;
        this.pitchContextControls = pitchContextControls;
        this.chordSelection = chordSelection;
        this.builder = builder;
        this.fineNudgeState = fineNudgeState;
        this.fineNudgeController = fineNudgeController;
        this.fineNudgeWriter = fineNudgeWriter;
        this.clipController = clipController;
        this.clipEditor = clipEditor;
        this.clipNavigation = clipNavigation;
        this.observationController = observationController;
        this.controller = controller;
        this.clips = clips;
        this.eventIndex = eventIndex;
    }

    public void handlePadPress(final int padIndex, final boolean pressed, final int velocity) {
        padControls.handlePadPress(padIndex, pressed, velocity);
    }

    public RgbLigthState padLight(final int padIndex) {
        return padControls.padLight(padIndex);
    }

    public void handleStepButton(final boolean pressed) {
        stepButtonControls.handlePressed(pressed);
    }

    public BiColorLightState stepButtonLight(final boolean active) {
        return stepButtonControls.lightState(active);
    }

    public void handleBankButton(final boolean pressed, final int amount, final boolean lengthAdjustEnabled) {
        bankButtonControls.handlePressed(pressed, amount, lengthAdjustEnabled);
    }

    public void handlePatternUp(final boolean pressed) {
        patternButtonControls.handleUpPressed(pressed);
    }

    public void handlePatternDown(final boolean pressed) {
        patternButtonControls.handleDownPressed(pressed);
    }

    public BiColorLightState patternUpLight() {
        return patternButtonControls.upLight();
    }

    public BiColorLightState patternDownLight() {
        return patternButtonControls.downLight();
    }

    public void handlePitchContextButton(final boolean pressed, final int amount, final boolean root) {
        pitchContextControls.handlePressed(pressed, amount, root);
    }

    public BiColorLightState pitchContextLight(final int amount, final boolean root) {
        return pitchContextControls.lightState(amount, root);
    }

    public boolean nudgeHeldNotes(final int amount,
                                  final Set<Integer> targetSteps,
                                  final Map<Integer, ChordStepEventIndex.Event> chordEventSnapshot) {
        return fineNudgeWriter.nudgeHeldNotes(amount, targetSteps, chordEventSnapshot);
    }

    public boolean isFineNudgeMoveInFlight() {
        return fineNudgeWriter.isMoveInFlight();
    }

    public ChordStepPadSurface padSurface() {
        return padSurface;
    }

    public ChordStepAccentControls accentControls() {
        return accentControls;
    }

    public ChordStepChordSelection chordSelection() {
        return chordSelection;
    }

    public ChordStepBuilderController builder() {
        return builder;
    }

    public ChordStepFineNudgeState<ChordStepEventIndex.Event> fineNudgeState() {
        return fineNudgeState;
    }

    public ChordStepFineNudgeController<ChordStepEventIndex.Event> fineNudgeController() {
        return fineNudgeController;
    }

    public ChordStepClipController clipController() {
        return clipController;
    }

    public ChordStepClipEditor<ChordStepEventIndex.Event> clipEditor() {
        return clipEditor;
    }

    public ChordStepClipNavigation clipNavigation() {
        return clipNavigation;
    }

    public ChordStepObservationController observationController() {
        return observationController;
    }

    public ChordStepController controller() {
        return controller;
    }

    public ChordStepClipResources clips() {
        return clips;
    }

    public ChordStepEventIndex eventIndex() {
        return eventIndex;
    }
}
