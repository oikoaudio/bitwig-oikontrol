package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.note.ChordInversion;
import com.oikoaudio.fire.note.NoteGridLayout;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.MusicalScale;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.control.VelocitySettings;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.music.SharedPitchContextController;
import com.oikoaudio.fire.sequence.EncoderBankLayout;
import com.oikoaudio.fire.sequence.NoteClipAvailability;
import com.oikoaudio.fire.sequence.NoteClipCursorRefresher;
import com.oikoaudio.fire.sequence.RecurrencePattern;
import com.oikoaudio.fire.sequence.SelectedClipSlotObserver;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;
import com.oikoaudio.fire.sequence.ClipSlotSelectionResolver;
import com.oikoaudio.fire.sequence.ClipRowHandler;
import com.oikoaudio.fire.sequence.SeqClipRowHost;
import com.oikoaudio.fire.sequence.StepSequencerEncoderHandler;
import com.oikoaudio.fire.sequence.StepPadLightHelper;
import com.oikoaudio.fire.sequence.StepSequencerHost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ChordStepMode extends Layer implements StepSequencerHost, SeqClipRowHost {
    private static final int CLIP_ROW_PAD_COUNT = 16;
    private static final int CHORD_SOURCE_PAD_OFFSET = 16;
    private static final int CHORD_SOURCE_PAD_COUNT = 16;
    private static final int MIN_OCTAVE = 0;
    private static final int MAX_OCTAVE = 7;
    private static final int STEP_PAD_OFFSET = 32;
    private static final int STEP_COUNT = 32;
    private static final int MAX_CHORD_STEPS = 2048;
    private static final double STEP_LENGTH = 0.25;
    private static final int FINE_STEPS_PER_STEP = 16;
    private static final int OBSERVED_FINE_STEP_CAPACITY = MAX_CHORD_STEPS * FINE_STEPS_PER_STEP;
    private static final double FINE_STEP_LENGTH = STEP_LENGTH / FINE_STEPS_PER_STEP;
    private static final int AUDITION_VELOCITY = 96;
    private static final int MIN_MIDI_VALUE = 0;
    private static final int MAX_MIDI_VALUE = 127;
    private static final int MIN_VELOCITY = 1;
    private static final int STANDARD_CHORD_VELOCITY = ChordStepAccentEditor.STANDARD_VELOCITY;
    private static final int ACCENTED_CHORD_VELOCITY = ChordStepAccentEditor.ACCENTED_VELOCITY;
    private static final RgbLigthState ROOT_COLOR = new RgbLigthState(120, 64, 0, true);
    private static final RgbLigthState IN_SCALE_COLOR = new RgbLigthState(0, 72, 110, true);
    private static final RgbLigthState HARMONIC_BRIGHT_COLOR = new RgbLigthState(0, 72, 122, true);
    private static final RgbLigthState HARMONIC_MINOR_COLOR = new RgbLigthState(18, 48, 104, true);
    private static final RgbLigthState HARMONIC_TENSE_COLOR = new RgbLigthState(68, 48, 116, true);
    private static final RgbLigthState HARMONIC_EXOTIC_COLOR = new RgbLigthState(108, 28, 72, true);
    private static final RgbLigthState HARMONIC_SYMMETRIC_COLOR = new RgbLigthState(46, 92, 42, true);
    private static final RgbLigthState OUT_OF_SCALE_COLOR = RgbLigthState.GRAY_1;
    private static final RgbLigthState EMPTY_STEP_A = RgbLigthState.GRAY_1;
    private static final RgbLigthState EMPTY_STEP_B = RgbLigthState.GRAY_2;
    private static final RgbLigthState OCCUPIED_STEP = new RgbLigthState(0, 90, 38, true);
    private static final RgbLigthState HELD_STEP = new RgbLigthState(120, 88, 0, true);
    private static final RgbLigthState DEFERRED_TOP = new RgbLigthState(110, 38, 0, true);
    private static final RgbLigthState DEFERRED_BOTTOM = new RgbLigthState(36, 16, 0, true);
    private static final String MODE_NAME = "Chord Step";

    private final AkaiFireOikontrolExtension driver;
    private final OledDisplay oled;
    private final SharedPitchContextController pitchContext;
    private final ChordStepPadSurface chordStepPadSurface = new ChordStepPadSurface();
    private final ChordStepAccentControls chordStepAccentControls;
    private final ChordStepChordSelection chordSelection = new ChordStepChordSelection();
    private final ChordStepBuilderController chordBuilder;
    private final ChordStepAuditionController chordStepAudition;
    private final VelocitySettings chordStepVelocity = new VelocitySettings(STANDARD_CHORD_VELOCITY, MIN_VELOCITY,
            ACCENTED_CHORD_VELOCITY - 1, 100);
    private final ChordStepVisibleClipCache visibleClipCache = new ChordStepVisibleClipCache(STEP_COUNT);
    private final ChordStepFineNudgeState<ChordStepEventIndex.Event> fineNudgeState = new ChordStepFineNudgeState<>();
    private final ChordStepFineNudgeController<ChordStepEventIndex.Event> fineNudgeController;
    private final ChordStepFineNudgeWriter chordStepFineNudgeWriter;
    private final ChordStepClipController chordStepClipController;
    private final ChordStepClipEditor<ChordStepEventIndex.Event> chordStepClipEditor;
    private final ChordStepClipNavigation chordStepClipNavigation;
    private final ChordStepObservationController chordStepObservationController;
    private final ChordStepController chordStepController;
    private final ChordStepClipResources chordStepClips;
    private final ChordStepEventIndex chordStepEventIndex;
    private final ChordStepSurfaceController chordStepSurface;
    private final ClipRowHandler clipHandler;
    private final CursorTrack chordStepCursorTrack;
    private final ChordStepControlBindings chordStepControlBindings;
    private final ChordStepEncoderControls chordStepEncoderControls;
    private final StepSequencerEncoderHandler stepEncoderLayer;
    private final ChordStepEditControls chordStepEditControls;
    private final BooleanValueObject lengthDisplay = new BooleanValueObject();

    private boolean noteStepActive = false;
    private Integer selectedPresetStepIndex = null;
    private int playingStep = -1;
    private RgbLigthState chordStepBaseColor = OCCUPIED_STEP;
    public ChordStepMode(final AkaiFireOikontrolExtension driver) {
        super(driver.getLayers(), "CHORD_STEP_MODE");
        this.driver = driver;
        this.pitchContext = driver.getSharedPitchContextController();
        this.oled = driver.getOled();
        this.chordStepAccentControls = new ChordStepAccentControls(oled);
        final ChordStepStepButtonControls chordStepStepButtonControls = new ChordStepStepButtonControls(
                chordStepAccentControls, chordStepStepButtonHost());
        final ChordStepBankButtonControls chordStepBankButtonControls =
                new ChordStepBankButtonControls(chordStepBankButtonHost());
        final ChordStepPatternButtonControls chordStepPatternButtonControls =
                new ChordStepPatternButtonControls(chordStepPatternButtonHost());
        final ChordStepPitchContextControls chordStepPitchContextControls =
                new ChordStepPitchContextControls(chordStepPitchContextHost());
        this.chordBuilder = new ChordStepBuilderController(chordSelection, pitchContext,
                this::getBuilderFirstVisibleMidiNote, CHORD_SOURCE_PAD_COUNT);
        final ChordStepPadController chordStepPadController = new ChordStepPadController(chordStepPadSurface,
                CLIP_ROW_PAD_COUNT, CHORD_SOURCE_PAD_OFFSET, STEP_PAD_OFFSET, chordStepPadHost());
        this.chordStepAudition = new ChordStepAuditionController(driver.getNoteInput());
        final ControllerHost host = driver.getHost();
        this.chordStepCursorTrack = host.createCursorTrack("CHORD_STEP_MODE", "Chord Step", 8, CLIP_ROW_PAD_COUNT, true);
        this.chordStepCursorTrack.name().markInterested();
        this.chordStepCursorTrack.color().markInterested();
        this.chordStepCursorTrack.canHoldNoteData().markInterested();
        this.chordStepCursorTrack.color().addValueObserver((r, g, b) -> chordStepBaseColor = ColorLookup.getColor(r, g, b));
        chordStepBaseColor = ColorLookup.getColor(this.chordStepCursorTrack.color().get());
        this.chordStepClips = new ChordStepClipResources(host, chordStepCursorTrack, STEP_COUNT,
                OBSERVED_FINE_STEP_CAPACITY, FINE_STEP_LENGTH);
        this.chordStepEventIndex = new ChordStepEventIndex(
                this::localToGlobalStep,
                this::globalToLocalStep,
                this::isVisibleGlobalStep,
                this::currentChordVelocity,
                FINE_STEPS_PER_STEP,
                FINE_STEP_LENGTH,
                STEP_LENGTH);
        this.chordStepClips.observe(this::handleStepData, this::handleNoteStepObject,
                this::handleObservedStepData, this::handlePlayingStep);
        this.chordStepClipController = new ChordStepClipController(
                () -> chordStepCursorTrack.canHoldNoteData().get(),
                this::hasLoadedNoteClipContent,
                this::queueChordObservationResync,
                this::showClipAvailabilityFailure);
        this.chordStepClipEditor = new ChordStepClipEditor<>(
                chordStepClips.observedClip(),
                chordStepEventIndex.observedState(),
                fineNudgeState,
                this::localToGlobalStep,
                this::localToGlobalFineStep,
                this::queueChordObservationResync,
                FINE_STEPS_PER_STEP);
        this.chordStepFineNudgeWriter = new ChordStepFineNudgeWriter(
                chordStepClips.observedClip(),
                chordStepEventIndex,
                fineNudgeState,
                chordStepPadSurface::markModifiedSteps,
                this::chordLoopFineSteps,
                this::isVisibleGlobalStep,
                this::globalToLocalStep,
                (task, delayTicks) -> driver.getHost().scheduleTask(task, delayTicks),
                this::refreshChordStepObservation,
                FINE_STEPS_PER_STEP);
        this.fineNudgeController = new ChordStepFineNudgeController<>(
                fineNudgeState,
                stepIndex -> snapshotChordEventForStep(stepIndex, true),
                this::nudgeHeldNotes);
        this.chordStepClipNavigation = new ChordStepClipNavigation(
                chordStepClips.noteClip(),
                chordStepClips.position(),
                oled,
                driver::notifyPopup,
                this::clearAllBankFineNudgeSessions,
                this::refreshChordStepObservation,
                MAX_CHORD_STEPS,
                FINE_STEPS_PER_STEP,
                FINE_STEP_LENGTH);
        this.chordStepObservationController = new ChordStepObservationController(
                (task, delayTicks) -> driver.getHost().scheduleTask(task, delayTicks),
                chordStepClips.clipSlotBank(),
                () -> driver.getViewControl().getSelectedClipSlotIndex(),
                () -> chordStepBaseColor != null ? chordStepBaseColor : OCCUPIED_STEP,
                chordStepClipController,
                this::clearObservedChordCaches,
                chordStepClips::scrollNoteClipToKeyStart,
                chordStepClips::scrollObservedClipToKeyStart,
                () -> chordStepClips.scrollNoteClipToStep(chordStepOffset()),
                chordStepClips::scrollObservedClipToStepStart);
        this.chordStepEditControls = new ChordStepEditControls(oled::valueInfo, oled::clearScreenDelayed);
        chordStepAudition.configureExpression();
        this.chordStepController = new ChordStepController(chordStepEditControls, chordStepClipController, chordStepObservationController);
        chordStepController.observeSelectedClip();
        this.clipHandler = new ClipRowHandler(this);
        final ChordStepPadLightRenderer chordStepPadLightRenderer = new ChordStepPadLightRenderer(
                chordStepPadSurface,
                chordBuilder,
                chordSelection,
                clipHandler::getPadLight,
                this::getHeldNotes,
                this::getChordOccupiedStepColor,
                chordStepClips.position()::getAvailableSteps,
                () -> playingStep,
                this::hasVisibleStepContent,
                stepIndex -> hasVisibleStepContent(stepIndex) && isChordStepAccented(stepIndex),
                this::isChordStepSustained);
        final ChordStepPadControls chordStepPadControls = new ChordStepPadControls(
                chordStepPadController,
                chordStepPadLightRenderer,
                CLIP_ROW_PAD_COUNT,
                CHORD_SOURCE_PAD_OFFSET,
                STEP_PAD_OFFSET);
        this.chordStepSurface = new ChordStepSurfaceController(
                chordStepPadSurface,
                chordStepPadControls,
                chordStepAccentControls,
                chordStepStepButtonControls,
                chordStepBankButtonControls,
                chordStepPatternButtonControls,
                chordStepPitchContextControls,
                chordSelection,
                chordBuilder,
                fineNudgeState,
                fineNudgeController,
                chordStepFineNudgeWriter,
                chordStepClipController,
                chordStepClipEditor,
                chordStepClipNavigation,
                chordStepObservationController,
                chordStepController,
                chordStepClips,
                chordStepEventIndex);
        this.chordStepEncoderControls =
                new ChordStepEncoderControls(driver, oled, chordStepCursorTrack, chordStepEncoderHost());
        this.stepEncoderLayer = new StepSequencerEncoderHandler(this, driver);

        this.chordStepControlBindings = new ChordStepControlBindings(driver, this, chordStepControlBindingsHost());
        chordStepControlBindings.bind();
        chordStepEncoderControls.bindMainEncoder(this);
    }

    public void notifyBlink(final int blinkTicks) {
        clipHandler.notifyBlink(blinkTicks);
    }

    private void syncEncoderLayers() {
        stepEncoderLayer.activate();
    }

    private ChordStepControlBindings.Host chordStepControlBindingsHost() {
        return new ChordStepControlBindings.Host() {
            @Override
            public void handlePadPress(final int padIndex, final boolean pressed, final int velocity) {
                ChordStepMode.this.handlePadPress(padIndex, pressed, velocity);
            }

            @Override
            public RgbLigthState padLight(final int padIndex) {
                return ChordStepMode.this.getPadLight(padIndex);
            }

            @Override
            public void handleStepSeqPressed(final boolean pressed) {
                ChordStepMode.this.handleStepSeqPressed(pressed);
            }

            @Override
            public BiColorLightState stepSeqLightState() {
                return ChordStepMode.this.getStepSeqLightState();
            }

            @Override
            public void handleBankButton(final boolean pressed, final int amount) {
                ChordStepMode.this.handleBankButton(pressed, amount);
            }

            @Override
            public BiColorLightState bankLightState() {
                return ChordStepMode.this.getBankLightState();
            }

            @Override
            public void handleMute1Button(final boolean pressed) {
                ChordStepMode.this.handleMute1Button(pressed);
            }

            @Override
            public BiColorLightState mute1LightState() {
                return ChordStepMode.this.getMute1LightState();
            }

            @Override
            public void handleMute2Button(final boolean pressed) {
                ChordStepMode.this.handleMute2Button(pressed);
            }

            @Override
            public BiColorLightState mute2LightState() {
                return ChordStepMode.this.getMute2LightState();
            }

            @Override
            public void handleMute3Button(final boolean pressed) {
                ChordStepMode.this.handleMute3Button(pressed);
            }

            @Override
            public BiColorLightState mute3LightState() {
                return ChordStepMode.this.getMute3LightState();
            }

            @Override
            public void handleMute4Button(final boolean pressed) {
                ChordStepMode.this.handleMute4Button(pressed);
            }

            @Override
            public BiColorLightState mute4LightState() {
                return ChordStepMode.this.getMute4LightState();
            }

            @Override
            public void handlePatternUp(final boolean pressed) {
                ChordStepMode.this.handlePatternUp(pressed);
            }

            @Override
            public BiColorLightState patternUpLight() {
                return ChordStepMode.this.getPatternUpLight();
            }

            @Override
            public void handlePatternDown(final boolean pressed) {
                ChordStepMode.this.handlePatternDown(pressed);
            }

            @Override
            public BiColorLightState patternDownLight() {
                return ChordStepMode.this.getPatternDownLight();
            }
        };
    }

    private boolean isChordStepModeActive() {
        return noteStepActive;
    }

    private void handleMute1Button(final boolean pressed) {
        if (isChordStepModeActive()) {
            chordStepController.handleMute1(pressed);
        }
    }

    private void handleMute2Button(final boolean pressed) {
        if (isChordStepModeActive()) {
            chordStepController.handleMute2(pressed);
        }
    }

    private void handleMute3Button(final boolean pressed) {
        if (isChordStepModeActive()) {
            chordStepController.handleMute3(pressed);
        }
    }

    private void handleMute4Button(final boolean pressed) {
        if (isChordStepModeActive()) {
            chordStepController.handleMute4(pressed);
            return;
        }
    }

    private BiColorLightState getMute1LightState() {
        if (isChordStepModeActive()) {
            return chordStepController.mute1LightState();
        }
        return BiColorLightState.OFF;
    }

    private BiColorLightState getMute2LightState() {
        if (isChordStepModeActive()) {
            return chordStepController.mute2LightState();
        }
        return BiColorLightState.OFF;
    }

    private BiColorLightState getMute3LightState() {
        if (isChordStepModeActive()) {
            return chordStepController.mute3LightState();
        }
        return BiColorLightState.OFF;
    }

    private BiColorLightState getMute4LightState() {
        if (isChordStepModeActive()) {
            return chordStepController.mute4LightState();
        }
        return BiColorLightState.OFF;
    }

    private void handleStepSeqPressed(final boolean pressed) {
        if (noteStepActive) {
            chordStepSurface.handleStepButton(pressed);
            return;
        }
        if (pressed) {
            driver.enterMelodicStepMode();
        }
    }

    private void handlePadPress(final int padIndex, final boolean pressed, final int velocity) {
        chordStepSurface.handlePadPress(padIndex, pressed, velocity);
    }

    private ChordStepPadController.Host chordStepPadHost() {
        return new ChordStepPadController.Host() {
            @Override
            public void handleClipRowPad(final int padIndex, final boolean pressed) {
                clipHandler.handlePadPress(padIndex, pressed);
            }

            @Override
            public boolean isBuilderFamily() {
                return ChordStepMode.this.isBuilderFamily();
            }

            @Override
            public boolean hasChordSlot(final int sourcePadIndex) {
                return chordSelection.hasSlot(sourcePadIndex);
            }

            @Override
            public void selectChordSlot(final int sourcePadIndex) {
                chordSelection.selectSlot(sourcePadIndex);
            }

            @Override
            public boolean isStepAuditionEnabled() {
                return driver.isStepSeqPadAuditionEnabled();
            }

            @Override
            public boolean isTransportPlaying() {
                return driver.isTransportPlaying();
            }

            @Override
            public void startAuditionSelectedChord(final int velocity) {
                ChordStepMode.this.startAuditionSelectedChord(velocity);
            }

            @Override
            public void startAuditionSelectedChord() {
                ChordStepMode.this.startAuditionSelectedChord();
            }

            @Override
            public void stopAuditionNotes() {
                ChordStepMode.this.stopAuditionNotes();
            }

            @Override
            public int currentChordVelocity(final int velocity) {
                return ChordStepMode.this.currentChordVelocity(velocity);
            }

            @Override
            public void assignSelectedChordToHeldSteps(final int velocity) {
                ChordStepMode.this.assignSelectedChordToHeldSteps(velocity);
            }

            @Override
            public boolean assignSelectedChordToSteps(final Set<Integer> stepIndexes, final int velocity) {
                return ChordStepMode.this.assignSelectedChordToSteps(stepIndexes, velocity);
            }

            @Override
            public void showCurrentChord() {
                ChordStepMode.this.showCurrentChord();
            }

            @Override
            public void toggleBuilderNoteOffset(final int sourcePadIndex) {
                ChordStepMode.this.toggleBuilderNoteOffset(sourcePadIndex);
            }

            @Override
            public void applyBuilderToHeldSteps() {
                ChordStepMode.this.applyBuilderToHeldSteps();
            }

            @Override
            public List<NoteStep> heldNotes() {
                return ChordStepMode.this.getHeldNotes();
            }

            @Override
            public void applyChordStepRecurrence(final List<NoteStep> targets,
                                                 final java.util.function.UnaryOperator<RecurrencePattern> updater) {
                ChordStepMode.this.applyChordStepRecurrence(targets, updater);
            }

            @Override
            public boolean ensureSelectedNoteClipSlot() {
                return ChordStepMode.this.ensureSelectedNoteClipSlot();
            }

            @Override
            public boolean isAccentGestureActive() {
                return chordStepAccentControls.isGestureActive();
            }

            @Override
            public void toggleAccentForStep(final int stepIndex) {
                ChordStepMode.this.toggleChordAccentForStep(stepIndex);
            }

            @Override
            public boolean isSelectHeld() {
                return chordStepController.isSelectHeld();
            }

            @Override
            public boolean isFixedLengthHeld() {
                return chordStepController.isFixedLengthHeld();
            }

            @Override
            public boolean isCopyHeld() {
                return chordStepController.isCopyHeld();
            }

            @Override
            public boolean isDeleteHeld() {
                return chordStepController.isDeleteHeld();
            }

            @Override
            public void selectStep(final int stepIndex) {
                if (isBuilderFamily()) {
                    loadBuilderFromStep(stepIndex);
                } else {
                    selectPresetStep(stepIndex);
                }
            }

            @Override
            public void setLastStep(final int stepIndex) {
                ChordStepMode.this.setLastStep(stepIndex);
            }

            @Override
            public void pasteCurrentChordToStep(final int stepIndex) {
                ChordStepMode.this.pasteCurrentChordToStep(stepIndex);
            }

            @Override
            public void clearChordStep(final int stepIndex) {
                chordStepClipEditor.clearChordStep(stepIndex);
            }

            @Override
            public boolean canExtendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
                return ChordStepMode.this.canExtendHeldChordRange(anchorStepIndex, targetStepIndex);
            }

            @Override
            public boolean extendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
                return ChordStepMode.this.extendHeldChordRange(anchorStepIndex, targetStepIndex);
            }

            @Override
            public void showExtendedStepInfo(final int anchorStepIndex, final int targetStepIndex) {
                ChordStepMode.this.showExtendedStepInfo(anchorStepIndex, targetStepIndex);
            }

            @Override
            public void showBlockedStepInfo() {
                ChordStepMode.this.showBlockedStepInfo();
            }

            @Override
            public boolean hasStepStartNote(final int stepIndex) {
                return ChordStepMode.this.hasStepStartNote(stepIndex);
            }

            @Override
            public void loadBuilderFromStep(final int stepIndex) {
                ChordStepMode.this.loadBuilderFromStep(stepIndex);
            }

            @Override
            public void showHeldStepInfo(final int stepIndex) {
                ChordStepMode.this.showHeldStepInfo(stepIndex);
            }

            @Override
            public void removeHeldBankFineStart(final int stepIndex) {
                fineNudgeState.invalidateStep(stepIndex);
            }
        };
    }

    private ChordStepStepButtonControls.Host chordStepStepButtonHost() {
        return new ChordStepStepButtonControls.Host() {
            @Override
            public boolean hasHeldSteps() {
                return chordStepPadSurface.hasHeldSteps();
            }

            @Override
            public void toggleAccentForHeldSteps() {
                ChordStepMode.this.toggleChordAccentForHeldSteps();
            }

            @Override
            public boolean isShiftHeld() {
                return driver.isGlobalShiftHeld();
            }

            @Override
            public boolean isAltHeld() {
                return driver.isGlobalAltHeld();
            }

            @Override
            public boolean isStandaloneChordStepSurface() {
                return isChordStepSurface();
            }

            @Override
            public void enterFugueStepMode() {
                driver.enterFugueStepMode();
            }

            @Override
            public void enterMelodicStepMode() {
                driver.enterMelodicStepMode();
            }

            @Override
            public void toggleFillMode() {
                driver.toggleFillMode();
            }

            @Override
            public boolean isFillModeActive() {
                return driver.isFillModeActive();
            }

            @Override
            public void showValueInfo(final String title, final String value) {
                oled.valueInfo(title, value);
            }

            @Override
            public BiColorLightState stepFillLightState() {
                return driver.getStepFillLightState();
            }

            @Override
            public BiColorLightState modeButtonLightState() {
                return getModeButtonLightState();
            }
        };
    }

    private ChordStepBankButtonControls.Host chordStepBankButtonHost() {
        return new ChordStepBankButtonControls.Host() {
            @Override
            public boolean isAltHeld() {
                return driver.isGlobalAltHeld();
            }

            @Override
            public boolean isShiftHeld() {
                return driver.isGlobalShiftHeld();
            }

            @Override
            public void setPendingLengthAdjust(final boolean pending) {
                fineNudgeState.setPendingLengthAdjust(pending);
            }

            @Override
            public boolean isPendingLengthAdjust() {
                return fineNudgeState.isPendingLengthAdjust();
            }

            @Override
            public void adjustLength(final int amount) {
                chordStepClipNavigation.adjustLength(amount, ChordStepMode.this::ensureSelectedNoteClipSlot);
            }

            @Override
            public boolean isFineNudgeMoveInFlight() {
                return chordStepFineNudgeWriter.isMoveInFlight();
            }

            @Override
            public Set<Integer> heldStepSnapshot() {
                return chordStepPadSurface.heldStepSnapshot();
            }

            @Override
            public void beginHeldFineNudge(final int amount, final Set<Integer> heldSteps) {
                fineNudgeController.beginHeldNudge(amount, heldSteps);
            }

            @Override
            public void adjustPlayStart(final int amount, final boolean fine) {
                chordStepClipNavigation.adjustPlayStart(amount, fine, ChordStepMode.this::ensureSelectedNoteClipSlot);
            }

            @Override
            public boolean completePendingFineNudge() {
                return fineNudgeController.completePendingNudge();
            }

            @Override
            public void clearPendingBankAction() {
                ChordStepMode.this.clearPendingBankAction();
            }
        };
    }

    private ChordStepPatternButtonControls.Host chordStepPatternButtonHost() {
        return new ChordStepPatternButtonControls.Host() {
            @Override
            public boolean isAltHeld() {
                return driver.isGlobalAltHeld();
            }

            @Override
            public void page(final int direction) {
                pageChordSteps(direction);
            }

            @Override
            public boolean canPageLeft() {
                return chordStepClips.position().canScrollLeft().get();
            }

            @Override
            public boolean canPageRight() {
                return chordStepClips.position().canScrollRight().get();
            }
        };
    }

    private ChordStepPitchContextControls.Host chordStepPitchContextHost() {
        return new ChordStepPitchContextControls.Host() {
            @Override
            public void adjustRoot(final int amount) {
                adjustChordRoot(amount);
            }

            @Override
            public void adjustOctave(final int amount) {
                adjustChordOctave(amount);
            }

            @Override
            public boolean canLowerOctave() {
                return chordSelection.canLowerOctave();
            }

            @Override
            public boolean canRaiseOctave() {
                return chordSelection.canRaiseOctave();
            }
        };
    }

    private ChordStepEncoderControls.Host chordStepEncoderHost() {
        return new ChordStepEncoderControls.Host() {
            @Override
            public void adjustChordRoot(final int amount) {
                ChordStepMode.this.adjustChordRoot(amount);
            }

            @Override
            public void resetChordRoot() {
                ChordStepMode.this.resetChordRoot();
            }

            @Override
            public void showChordRootInfo() {
                ChordStepMode.this.showChordRootInfo();
            }

            @Override
            public void adjustChordOctave(final int amount) {
                ChordStepMode.this.adjustChordOctave(amount);
            }

            @Override
            public void resetChordOctave() {
                ChordStepMode.this.resetChordOctave();
            }

            @Override
            public void showChordOctaveInfo() {
                ChordStepMode.this.showChordOctaveInfo();
            }

            @Override
            public void adjustChordVelocityCenter(final int inc) {
                ChordStepMode.this.adjustChordVelocityCenter(inc);
            }

            @Override
            public void adjustChordVelocitySensitivity(final int inc) {
                ChordStepMode.this.adjustChordVelocitySensitivity(inc);
            }

            @Override
            public void resetChordVelocityTargets() {
                ChordStepMode.this.resetChordVelocityTargets();
            }

            @Override
            public void showChordVelocityInfo() {
                ChordStepMode.this.showChordVelocityInfo();
            }

            @Override
            public void adjustChordPage(final int amount) {
                ChordStepMode.this.adjustChordPage(amount);
            }

            @Override
            public void adjustChordFamily(final int amount) {
                ChordStepMode.this.adjustChordFamily(amount);
            }

            @Override
            public void showCurrentChord() {
                ChordStepMode.this.showCurrentChord();
            }

            @Override
            public void resetChordFamilySelection() {
                ChordStepMode.this.resetChordFamilySelection();
            }

            @Override
            public void adjustChordSharedScale(final int amount) {
                ChordStepMode.this.adjustChordSharedScale(amount);
            }

            @Override
            public String getScaleDisplayName() {
                return ChordStepMode.this.getScaleDisplayName();
            }

            @Override
            public void invertCurrentChord(final int direction) {
                ChordStepMode.this.invertCurrentChord(direction);
            }

            @Override
            public void adjustChordInterpretation(final int amount) {
                ChordStepMode.this.adjustChordInterpretation(amount);
            }

            @Override
            public void resetChordInterpretation() {
                chordSelection.resetInterpretation();
            }

            @Override
            public void showChordInterpretationInfo() {
                oled.valueInfo("Interpret", chordSelection.interpretationDisplayName());
            }
        };
    }

    private void applyChordStepRecurrence(final List<NoteStep> targets,
                                          final java.util.function.UnaryOperator<RecurrencePattern> updater) {
        if (targets.isEmpty()) {
            return;
        }
        final RecurrencePattern updated = updater.apply(
                RecurrencePattern.of(targets.get(0).recurrenceLength(), targets.get(0).recurrenceMask()));
        for (final NoteStep note : targets) {
            note.setRecurrence(updated.bitwigLength(), updated.bitwigMask());
            chordStepPadSurface.markModifiedStep(note.x());
        }
        oled.valueInfo("Recurrence", updated.summary());
        driver.notifyPopup("Recurrence", updated.summary());
    }

    private void loadBuilderFromStep(final int stepIndex) {
        final Map<Integer, NoteStep> notesAtStep = chordStepEventIndex.noteStepsAt(stepIndex);
        final Set<Integer> fallbackNotes = bestAvailableStepNotes(stepIndex);
        if (notesAtStep.isEmpty() && fallbackNotes.isEmpty()) {
            oled.valueInfo("Select", "Empty step");
            return;
        }
        final Set<Integer> loadedNotes = new HashSet<>();
        int loaded = 0;
        if (!notesAtStep.isEmpty()) {
            for (final NoteStep note : notesAtStep.values()) {
                if (note.state() != NoteStep.State.NoteOn) {
                    continue;
                }
                loadedNotes.add(note.y());
                loaded++;
            }
        } else {
            for (final int midiNote : fallbackNotes) {
                loadedNotes.add(midiNote);
                loaded++;
            }
        }
        chordSelection.replaceBuilderNotes(loadedNotes);
        oled.valueInfo("Select Step", loaded > 0 ? "Step " + (stepIndex + 1) : "Empty");
    }

    private void selectPresetStep(final int stepIndex) {
        selectedPresetStepIndex = stepIndex;
        oled.valueInfo("Step " + (stepIndex + 1), "selected");
    }

    private void pasteCurrentChordToStep(final int stepIndex) {
        if (!ensureSelectedNoteClip()) {
            return;
        }
        final int[] notes = renderSelectedChord();
        if (notes.length == 0) {
            oled.valueInfo("Paste", "Empty chord");
            return;
        }
        chordStepClipEditor.writeChordAtStep(stepIndex, notes, currentChordVelocity(127), STEP_LENGTH);
        chordStepPadSurface.markModifiedStep(stepIndex);
        oled.valueInfo("Paste", "Step " + (stepIndex + 1));
    }

    private void setLastStep(final int stepIndex) {
        final double newLength = (stepIndex + 1) * STEP_LENGTH;
        chordStepClips.noteClip().getLoopLength().set(newLength);
        oled.valueInfo("Last Step", Integer.toString(stepIndex + 1));
    }

    private void handleClipStepRecordPadPress(final int padIndex, final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (padIndex < STEP_PAD_OFFSET) {
            oled.valueInfo("Clip Step Rec", "Deferred");
        } else {
            oled.valueInfo("Clip Step Rec", "Step " + (padIndex - STEP_PAD_OFFSET + 1));
        }
    }

    private void handleStepData(final int x, final int y, final int state) {
        visibleClipCache.handleStepData(x, y, state);
    }

    private void handleObservedStepData(final int x, final int y, final int state) {
        chordStepEventIndex.handleObservedStepData(x, y, state);
    }

    private void handleNoteStepObject(final NoteStep noteStep) {
        chordStepEventIndex.handleNoteStepObject(noteStep);
    }

    private void handlePlayingStep(final int playingStep) {
        final int localPlayingStep = playingStep - chordStepOffset();
        if (localPlayingStep < 0 || localPlayingStep >= STEP_COUNT) {
            this.playingStep = -1;
            return;
        }
        this.playingStep = localPlayingStep;
    }

    private void invertCurrentChord(final int direction) {
        final int[] currentNotes = renderSelectedChord();
        if (currentNotes.length == 0) {
            oled.valueInfo("Invert", "Empty");
            return;
        }
        final int[] inverted = ChordInversion.rotate(currentNotes, direction);
        chordSelection.replaceBuilderNotes(Arrays.stream(inverted).boxed().collect(Collectors.toSet()));
        applyBuilderToHeldSteps();
        oled.valueInfo("Invert", direction > 0 ? "Up" : "Down");
    }

    private void nudgeHeldNotes(final int amount, final Set<Integer> targetSteps,
                                final Map<Integer, ChordStepEventIndex.Event> chordEventSnapshot) {
        if (!ensureChordClipWithinObservedCapacity()) {
            return;
        }
        if (chordStepSurface.nudgeHeldNotes(amount, targetSteps, chordEventSnapshot)) {
            oled.valueInfo("Nudge", amount > 0 ? "+Fine" : "-Fine");
        }
    }

    private int chordLoopFineSteps() {
        return Math.max(FINE_STEPS_PER_STEP, chordStepClips.position().getSteps() * FINE_STEPS_PER_STEP);
    }

    private int chordLoopSteps() {
        return Math.max(1, chordStepClips.position().getSteps());
    }

    private boolean ensureChordClipWithinObservedCapacity() {
        if (chordStepClips.position().getSteps() <= MAX_CHORD_STEPS) {
            return true;
        }
        oled.valueInfo("Clip too long", Integer.toString(chordStepClips.position().getSteps()));
        driver.notifyPopup("Clip too long", "Chord nudging supports up to %d steps".formatted(MAX_CHORD_STEPS));
        return false;
    }

    private Set<Integer> getVisibleOccupiedSteps() {
        return chordStepEventIndex.visibleOccupiedSteps();
    }

    private Set<Integer> getVisibleStartedSteps() {
        return chordStepEventIndex.visibleStartedSteps();
    }

    private Map<Integer, Integer> snapshotFineStartsForStep(final int localStep, final boolean heldOnly) {
        final Map<Integer, Integer> persisted = fineNudgeState.fineStartsForStep(localStep, heldOnly);
        if (persisted != null && !persisted.isEmpty()) {
            return new HashMap<>(persisted);
        }
        final Map<Integer, Integer> starts = chordStepEventIndex.eventsForStep(localStep, Map.of()).stream()
                .flatMap(event -> event.notes().stream())
                .collect(Collectors.toMap(ChordStepEventIndex.EventNote::midiNote,
                        ChordStepEventIndex.EventNote::fineStart, (a, b) -> a,
                        HashMap::new));
        if (heldOnly && !starts.isEmpty()) {
            fineNudgeState.rememberHeldFineStarts(localStep, starts);
        }
        return starts;
    }

    private ChordStepEventIndex.Event snapshotChordEventForStep(final int localStep, final boolean heldOnly) {
        final ChordStepEventIndex.Event persisted = heldOnly ? fineNudgeState.heldEvent(localStep) : null;
        if (persisted != null) {
            return persisted;
        }
        return chordStepEventIndex.eventForStep(localStep, Map.of());
    }

    private void clearPendingBankAction() {
        fineNudgeController.cancelPending();
    }

    private boolean isVisibleGlobalStep(final int globalStep) {
        return globalStep >= chordStepOffset() && globalStep < chordStepOffset() + STEP_COUNT;
    }

    private int globalToLocalStep(final int globalStep) {
        return globalStep - chordStepOffset();
    }

    private boolean canExtendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
        final int startStep = Math.min(anchorStepIndex, targetStepIndex);
        final int endStep = Math.max(anchorStepIndex, targetStepIndex);
        for (int stepIndex = startStep; stepIndex <= endStep; stepIndex++) {
            if (stepIndex == anchorStepIndex) {
                continue;
            }
            if (hasStepStartNote(stepIndex)) {
                return false;
            }
        }
        return true;
    }

    private boolean extendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
        final int startStep = Math.min(anchorStepIndex, targetStepIndex);
        final int endStep = Math.max(anchorStepIndex, targetStepIndex);
        final double duration = (endStep - startStep + 1) * STEP_LENGTH;

        if (anchorStepIndex == startStep) {
            final Map<Integer, NoteStep> anchorNotes = chordStepEventIndex.noteStepsAt(anchorStepIndex);
            if (!anchorNotes.isEmpty()) {
                anchorNotes.values().forEach(note -> note.setDuration(duration));
                return true;
            }
            if (hasVisibleStepContent(anchorStepIndex) || chordStepPadSurface.hasAddedStep(anchorStepIndex)) {
                writeSelectedChordAtStep(anchorStepIndex, duration);
                return true;
            }
            return false;
        }

        final Map<Integer, NoteStep> anchorNotes = chordStepEventIndex.noteStepsAt(anchorStepIndex);
        if (!anchorNotes.isEmpty()) {
            for (final NoteStep note : anchorNotes.values()) {
                chordStepEventIndex.addPendingMoveSnapshot(startStep, note.y(), note);
                chordStepClipEditor.setChordStep(startStep, note.y(), (int) Math.round(note.velocity() * 127), duration);
                chordStepClipEditor.clearChordNote(note.x(), note.y());
            }
            return true;
        }
        if (hasVisibleStepContent(anchorStepIndex) || chordStepPadSurface.hasAddedStep(anchorStepIndex)) {
            chordStepClipEditor.clearChordStep(anchorStepIndex);
            writeSelectedChordAtStep(startStep, duration);
            return true;
        }
        return false;
    }

    private void enterCurrentStepSubMode() {
        stopAuditionNotes();
        chordStepPadSurface.clearStepTracking();
        selectedPresetStepIndex = null;
        chordStepClips.position().setPage(0);
        clearTranslation();
        syncEncoderLayers();
        refreshChordStepObservation();
        ensureBuilderSeededIfEmpty();
        if (chordPageCount() > 1) {
            showChordPageInfo();
        } else {
            showCurrentChord();
        }
    }

    private void assignSelectedChordToHeldSteps(final int velocity) {
        final Set<Integer> heldSteps = chordStepPadSurface.heldStepSnapshot();
        chordStepPadSurface.markModifiedSteps(heldSteps);
        assignSelectedChordToSteps(heldSteps, velocity);
    }

    private boolean assignSelectedChordToSteps(final Set<Integer> stepIndexes, final int velocity) {
        if (stepIndexes.isEmpty()) {
            return false;
        }
        if (!ensureSelectedNoteClip()) {
            return false;
        }
        final int[] notes = renderSelectedChord();
        if (notes.length == 0) {
            oled.valueInfo("Select", "Notes 1st");
            return false;
        }
        final int appliedVelocity = currentChordVelocity(velocity);
        for (final int stepIndex : stepIndexes) {
            chordStepClipEditor.writeChordAtStep(stepIndex, notes, appliedVelocity, STEP_LENGTH);
        }
        oled.valueInfo(currentChordFamilyLabel(), currentChordName());
        return true;
    }

    private void writeSelectedChordAtStep(final int stepIndex, final double duration) {
        final int[] notes = renderSelectedChord();
        chordStepClipEditor.writeChordAtStep(stepIndex, notes, currentChordVelocity(127), duration);
    }

    private void applyBuilderToHeldSteps() {
        if (!isBuilderFamily() || !chordStepPadSurface.hasHeldSteps()) {
            return;
        }
        final Set<Integer> heldSteps = chordStepPadSurface.heldStepSnapshot();
        chordStepPadSurface.markModifiedSteps(heldSteps);
        for (final int stepIndex : heldSteps) {
            writeSelectedChordAtStep(stepIndex, getStepDuration(stepIndex));
        }
    }

    private double getStepDuration(final int stepIndex) {
        return chordStepEventIndex.noteStepsAt(stepIndex).values().stream()
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .mapToDouble(NoteStep::duration)
                .max()
                .orElse(STEP_LENGTH);
    }

    private void adjustChordPage(final int amount) {
        if (chordSelection.adjustPage(amount)) {
            showCurrentChord();
        }
    }

    private void adjustChordFamily(final int amount) {
        if (chordSelection.adjustFamily(amount)) {
            showCurrentChord();
        }
    }

    private void adjustChordRoot(final int amount) {
        if (amount == 0) {
            return;
        }
        driver.adjustSharedRootNote(amount);
        showChordRootInfo();
    }

    private void resetChordRoot() {
        driver.setSharedRootNote(0);
    }

    private void adjustChordOctave(final int amount) {
        if (chordSelection.adjustOctave(amount)) {
            showChordOctaveInfo();
        }
    }

    private void resetChordOctave() {
        chordSelection.resetOctave();
    }

    private void toggleChordInterpretation() {
        chordSelection.toggleInterpretation();
        oled.valueInfo("Chord Step Mode", chordSelection.interpretationDisplayName());
        driver.notifyPopup("Chord Step Mode", chordSelection.interpretationDisplayName());
    }

    private void adjustChordInterpretation(final int amount) {
        if (chordSelection.adjustInterpretation(amount)) {
            oled.valueInfo("Chord Step Mode", chordSelection.interpretationDisplayName());
            driver.notifyPopup("Chord Step Mode", chordSelection.interpretationDisplayName());
        }
    }

    private void adjustChordSharedScale(final int amount) {
        if (amount == 0) {
            return;
        }
        applyLayoutChange(() -> driver.adjustSharedScaleIndex(amount, 1));
        oled.valueInfo("Scale", getScaleDisplayName());
        driver.notifyPopup("Scale", getScaleDisplayName());
    }

    private void startAuditionSelectedChord() {
        startAuditionSelectedChord(AUDITION_VELOCITY);
    }

    private void startAuditionSelectedChord(final int velocity) {
        final int[] notes = renderSelectedChord();
        chordStepAudition.startAudition(notes, velocity);
        oled.valueInfo(currentChordFamilyLabel(), currentChordName());
    }

    private void stopAuditionNotes() {
        chordStepAudition.stopAudition();
    }

    private void handleBankButton(final boolean pressed, final int amount) {
        if (noteStepActive) {
            chordStepSurface.handleBankButton(pressed, amount, true);
        }
    }

    private BiColorLightState getBankLightState() {
        if (noteStepActive) {
            return BiColorLightState.HALF;
        }
        return BiColorLightState.HALF;
    }

    private BiColorLightState getStepSeqLightState() {
        return chordStepSurface.stepButtonLight(noteStepActive);
    }

    private int currentChordVelocity(final int rawVelocity) {
        if (chordStepAccentControls.isActive()) {
            return ACCENTED_CHORD_VELOCITY;
        }
        return chordStepVelocity.resolveVelocity(rawVelocity);
    }

    private void toggleChordAccentForStep(final int stepIndex) {
        final Map<Integer, NoteStep> notesAtStep = chordStepEventIndex.noteStepsAt(stepIndex);
        if (notesAtStep.isEmpty()) {
            return;
        }
        final boolean targetAccent = chordStepAccentControls.toggleAccent(notesAtStep.values(),
                chordStepVelocity.centerVelocity());
        chordStepAccentControls.markModified();
        oled.valueInfo("Accent", targetAccent ? "Accented" : "Normal");
    }

    private void toggleChordAccentForHeldSteps() {
        final List<NoteStep> heldNotes = getHeldNotes();
        if (heldNotes.isEmpty()) {
            oled.valueInfo("Accent", "No Step");
            return;
        }
        final boolean targetAccent = chordStepAccentControls.toggleAccent(heldNotes,
                chordStepVelocity.centerVelocity());
        chordStepPadSurface.markModifiedNotes(heldNotes);
        oled.valueInfo("Accent", targetAccent ? "Accented" : "Normal");
        driver.notifyPopup("Accent", targetAccent ? "Accented" : "Normal");
    }

    private boolean isChordStepAccented(final int stepIndex) {
        return chordStepAccentControls.isStepAccented(chordStepEventIndex.noteStepsAt(stepIndex),
                chordStepVelocity.centerVelocity());
    }

    public void toggleSurfaceVariant() {
        toggleBuilderLayout();
    }

    private void toggleBuilderLayout() {
        chordBuilder.toggleLayout();
        oled.valueInfo("Builder Layout", chordBuilder.layoutDisplayName());
        driver.notifyPopup("Builder Layout", chordBuilder.layoutDisplayName());
    }

    public BiColorLightState getModeButtonLightState() {
        return isChordStepSurface() ? BiColorLightState.AMBER_FULL : BiColorLightState.RED_FULL;
    }

    private boolean isChordStepSurface() {
        return true;
    }

    private void adjustOctave(final int amount) {
        if (amount == 0) {
            return;
        }
        final int nextOctave = Math.max(MIN_OCTAVE, Math.min(MAX_OCTAVE, getOctave() + amount));
        if (nextOctave == getOctave()) {
            return;
        }
        applyLayoutChange(() -> driver.setSharedOctave(nextOctave));
        showState("Octave");
    }

    private void applyLayoutChange(final Runnable stateChange) {
        stateChange.run();
        showContextInfo();
    }

    private RgbLigthState getPadLight(final int padIndex) {
        return getChordStepPadLight(padIndex);
    }

    private RgbLigthState getChordStepPadLight(final int padIndex) {
        return chordStepSurface.padLight(padIndex);
    }

    private RgbLigthState getChordOccupiedStepColor() {
        if (chordStepController.color() != null) {
            return chordStepController.color();
        }
        return chordStepBaseColor != null ? chordStepBaseColor : OCCUPIED_STEP;
    }

    private boolean isChordStepSustained(final int stepIndex) {
        return hasVisibleStepContent(stepIndex) && !hasStepStartNote(stepIndex);
    }

    private RgbLigthState getClipStepRecordPadLight(final int padIndex) {
        if (padIndex < STEP_PAD_OFFSET) {
            return DEFERRED_TOP;
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
        if (!StepPadLightHelper.isStepWithinVisibleLoop(stepIndex, chordStepClips.position().getAvailableSteps())) {
            return RgbLigthState.OFF;
        }
        if (chordStepPadSurface.hasHeldStep(stepIndex)) {
            return HELD_STEP.getBrightest();
        }
        return hasVisibleStepContent(stepIndex) ? OCCUPIED_STEP : DEFERRED_BOTTOM;
    }

    private void showState(final String focus) {
        if ("Scale".equals(focus)) {
            oled.valueInfo("Scale", getScaleDisplayName());
            return;
        }
        if ("Root".equals(focus)) {
            oled.valueInfo("Root", "%s%d".formatted(NoteGridLayout.noteName(getRootNote()), getOctave()));
            return;
        }
        if ("Octave".equals(focus)) {
            oled.valueInfo("Octave", Integer.toString(getOctave()));
            return;
        }
        if ("Interpretation".equals(focus) && noteStepActive) {
            oled.valueInfo("Chord Step Mode", chordSelection.interpretationDisplayName());
            return;
        }
        oled.lineInfo("Root %s%d".formatted(NoteGridLayout.noteName(getRootNote()), getOctave()),
                noteStepActive
                        ? "Step: %s\n%s".formatted(MODE_NAME, currentChordDisplay())
                        : "Scale: %s".formatted(getScaleDisplayName()));
    }

    private void showContextInfo() {
        if (noteStepActive) {
            showCurrentChord();
            return;
        }
        showState("Mode");
    }

    private void showCurrentChord() {
        oled.valueInfo("%s %d/%d".formatted(currentChordFamilyLabel(), chordSelection.page() + 1,
                        currentChordPageCount()),
                "%s %s".formatted(currentChordName(), chordInterpretationSuffix()));
    }

    private String currentChordDisplay() {
        return "%s %s".formatted(currentChordName(), chordInterpretationSuffix());
    }

    private String chordInterpretationSuffix() {
        return chordSelection.interpretationSuffix(getRootNote());
    }

    private int[] renderSelectedChord() {
        return chordSelection.renderSelectedChord(getScale(), getRootNote());
    }

    private void ensureBuilderSeededIfEmpty() {
        chordBuilder.ensureSeededIfEmpty();
    }

    private int getChordRootMidi() {
        return chordSelection.chordRootMidi(getRootNote());
    }

    private void showChordRootInfo() {
        oled.valueInfo("Chord Root", NoteGridLayout.noteName(getRootNote()));
    }

    private boolean isBuilderFamily() {
        return chordSelection.isBuilderFamily();
    }

    private int currentChordPageCount() {
        return chordSelection.pageCount();
    }

    private String currentChordFamilyLabel() {
        return chordSelection.familyLabel();
    }

    private String currentChordName() {
        return chordSelection.chordName();
    }

    private void toggleBuilderNoteOffset(final int padIndex) {
        chordBuilder.toggleNoteOffset(padIndex);
    }

    private void showChordOctaveInfo() {
        oled.valueInfo("Chord Oct", formatSignedValue(chordSelection.octaveOffset()));
    }

    private void resetChordFamilySelection() {
        chordSelection.resetToBuilder();
    }

    private String formatSignedValue(final int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private void showHeldStepInfo(final int stepIndex) {
        oled.valueInfo("Step", Integer.toString(stepIndex + 1));
    }

    private void showExtendedStepInfo(final int anchorStepIndex, final int targetStepIndex) {
        final int startStep = Math.min(anchorStepIndex, targetStepIndex);
        final int endStep = Math.max(anchorStepIndex, targetStepIndex);
        oled.valueInfo("Length", (startStep + 1) + "-" + (endStep + 1));
    }

    private void showBlockedStepInfo() {
        oled.valueInfo("Length", "Blocked");
    }

    private void refreshHeldStepAnchor(final int releasedStepIndex) {
        chordStepPadSurface.refreshHeldStepAnchor(releasedStepIndex);
    }

    private MusicalScale getScale() {
        return pitchContext.getMusicalScale();
    }

    private String getScaleDisplayName() {
        return pitchContext.getShortScaleDisplayName();
    }

    private int getRootNote() {
        return pitchContext.getRootNote();
    }

    private int getOctave() {
        return pitchContext.getOctave();
    }

    private int getBuilderFirstVisibleMidiNote() {
        final int midiNote = getChordRootMidi();
        return midiNote >= MIN_MIDI_VALUE && midiNote <= MAX_MIDI_VALUE ? midiNote : -1;
    }

    private void clearTranslation() {
        chordStepPadSurface.setHeldStepAnchor(null);
        chordStepAudition.clearTranslation();
    }

    private void handlePatternUp(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (noteStepActive) {
            chordStepSurface.handlePatternUp(true);
            return;
        }
        adjustOctave(1);
    }

    private void handlePatternDown(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (noteStepActive) {
            chordStepSurface.handlePatternDown(true);
            return;
        }
        adjustOctave(-1);
    }

    private BiColorLightState getPatternUpLight() {
        if (noteStepActive) {
            return chordStepSurface.patternUpLight();
        }
        return BiColorLightState.GREEN_HALF;
    }

    private BiColorLightState getPatternDownLight() {
        if (noteStepActive) {
            return chordStepSurface.patternDownLight();
        }
        return BiColorLightState.GREEN_HALF;
    }

    @Override
    public boolean isSelectHeld() {
        return chordStepController.isSelectHeld();
    }

    @Override
    public CursorRemoteControlsPage getActiveRemoteControlsPage() {
        return null;
    }

    @Override
    public boolean isPadBeingHeld() {
        return false;
    }

    @Override
    public List<NoteStep> getOnNotes() {
        return chordStepEventIndex.allStartedNotes();
    }

    @Override
    public List<NoteStep> getHeldNotes() {
        return chordStepEventIndex.heldNotes(chordStepPadSurface.heldStepSnapshot());
    }

    @Override
    public List<NoteStep> getFocusedNotes() {
        final List<NoteStep> heldNotes = getHeldNotes();
        if (!heldNotes.isEmpty()) {
            return heldNotes;
        }
        return chordStepEventIndex.selectedStepNotes(selectedPresetStepIndex);
    }

    @Override
    public String getDetails(final List<NoteStep> heldNotes) {
        return "%s <%d>".formatted(MODE_NAME, heldNotes.size());
    }

    @Override
    public double getGridResolution() {
        return STEP_LENGTH;
    }

    private int chordStepOffset() {
        return chordStepClips.position().getStepOffset();
    }

    private int localToGlobalStep(final int localStep) {
        return chordStepOffset() + localStep;
    }

    private int localToGlobalFineStep(final int localStep) {
        return localToGlobalStep(localStep) * FINE_STEPS_PER_STEP;
    }

    private int chordPageCount() {
        return chordStepClipNavigation.pageCount();
    }

    private void pageChordSteps(final int direction) {
        chordStepClipNavigation.page(direction, currentChordDisplay());
    }

    private void clearAllBankFineNudgeSessions() {
        fineNudgeController.clearHeld();
    }

    private void showChordPageInfo() {
        chordStepClipNavigation.showPageInfo(currentChordDisplay());
    }

    @Override
    public BooleanValueObject getLengthDisplay() {
        return lengthDisplay;
    }

    @Override
    public BooleanValueObject getDeleteHeld() {
        return chordStepController.deleteHeldValue();
    }

    @Override
    public boolean isCopyHeld() {
        return chordStepController.isCopyHeld();
    }

    @Override
    public boolean isDeleteHeld() {
        return chordStepController.isDeleteHeld();
    }

    @Override
    public boolean isShiftHeld() {
        return driver.isGlobalShiftHeld();
    }

    @Override
    public AkaiFireOikontrolExtension getDriver() {
        return driver;
    }

    @Override
    public OledDisplay getOled() {
        return oled;
    }

    @Override
    public ClipLauncherSlotBank getClipSlotBank() {
        return chordStepClips.clipSlotBank();
    }

    @Override
    public PinnableCursorClip getClipCursor() {
        return chordStepClips.noteClip();
    }

    @Override
    public void notifyPopup(final String title, final String value) {
        driver.notifyPopup(title, value);
    }

    @Override
    public String getPadInfo() {
        return MODE_NAME;
    }

    @Override
    public void exitRecurrenceEdit() {
    }

    @Override
    public void enterRecurrenceEdit(final List<NoteStep> notes) {
    }

    @Override
    public void updateRecurrencLength(final int length) {
    }

    @Override
    public void registerModifiedSteps(final List<NoteStep> notes) {
        chordStepPadSurface.markModifiedNotes(notes);
    }

    @Override
    public EncoderBankLayout getEncoderBankLayout() {
        return chordStepEncoderControls.layout();
    }

    private void adjustChordVelocityCenter(final int inc) {
        if (!chordStepVelocity.adjustCenterVelocity(inc)) {
            return;
        }
        oled.paramInfo("Velocity Center", chordStepVelocity.centerVelocity(), "Chord Step",
                chordStepVelocity.minCenterVelocity(), chordStepVelocity.maxCenterVelocity());
    }

    private void adjustChordVelocitySensitivity(final int inc) {
        if (!chordStepVelocity.adjustSensitivity(inc)) {
            return;
        }
        oled.paramInfo("Velocity Sens", chordStepVelocity.sensitivity(), "Chord Step", 0, 100);
    }

    private void resetChordVelocityTargets() {
        chordStepVelocity.reset();
    }

    private void showChordVelocityInfo() {
        if (driver.isGlobalShiftHeld()) {
            oled.valueInfo("Velocity Center", Integer.toString(chordStepVelocity.centerVelocity()));
            return;
        }
        oled.valueInfo("Velocity", chordStepVelocity.summary());
    }

    private void observeSelectedNoteClip() {
        SelectedClipSlotObserver.observe(chordStepClips.clipSlotBank(), true, true, this::refreshSelectedNoteClipState);
    }

    private void refreshSelectedNoteClipState() {
        chordStepController.refreshSelectedClipState();
    }

    private void queueChordObservationResync() {
        chordStepController.queueObservationResync();
    }

    private void refreshChordStepObservation() {
        chordStepController.refreshObservation();
    }

    private void refreshChordStepObservationPass() {
        chordStepController.refreshObservationPass();
    }
    private void clearObservedChordCaches() {
        chordStepEventIndex.clear();
    }

    private boolean ensureSelectedNoteClip() {
        return chordStepController.ensureSelectedClip();
    }

    private boolean ensureSelectedNoteClipSlot() {
        return chordStepController.ensureSelectedClipSlot();
    }

    private void showClipAvailabilityFailure(final NoteClipAvailability.Failure failure) {
        oled.valueInfo(failure.title(), failure.oledDetail());
        driver.notifyPopup(failure.title(), failure.popupDetail());
    }

    private boolean hasLoadedNoteClipContent() {
        return chordStepEventIndex.hasAnyLoadedNoteContent();
    }

    private boolean hasVisibleStepContent(final int stepIndex) {
        return visibleClipCache.hasStepContent(stepIndex) || chordStepEventIndex.hasVisibleStepContent(stepIndex);
    }

    private boolean hasStepStartNote(final int stepIndex) {
        return chordStepEventIndex.hasStepStart(stepIndex);
    }

    private Set<Integer> bestAvailableStepNotes(final int stepIndex) {
        final Set<Integer> visibleNotes = visibleClipCache.notesAtStep(stepIndex);
        if (!visibleNotes.isEmpty()) {
            return visibleNotes;
        }
        return chordStepEventIndex.notesAtStep(stepIndex);
    }

    @Override
    protected void onActivate() {
        noteStepActive = true;
        chordSelection.resetToBuilder();
        chordStepPadSurface.clearStepTracking();
        selectedPresetStepIndex = null;
        chordStepClips.position().setPage(0);
        chordStepControlBindings.activatePatternButtons();
        stepEncoderLayer.deactivate();
        enterCurrentStepSubMode();
    }

    @Override
    protected void onDeactivate() {
        chordStepControlBindings.deactivatePatternButtons();
        noteStepActive = false;
        stopAuditionNotes();
        chordSelection.resetToBuilder();
        chordStepPadSurface.clearStepTracking();
        selectedPresetStepIndex = null;
        stepEncoderLayer.deactivate();
        clearTranslation();
    }

}
