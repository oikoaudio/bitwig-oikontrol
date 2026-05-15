package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.note.ChordInversion;
import com.oikoaudio.fire.note.LiveVelocityLogic;
import com.oikoaudio.fire.note.NoteGridLayout;

import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.MusicalScale;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.Midi;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.EncoderTouchResetHandler;
import com.oikoaudio.fire.control.EncoderValueProfile;
import com.oikoaudio.fire.control.EncoderStepAccumulator;
import com.oikoaudio.fire.control.MixerEncoderProfile;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.control.TouchResetGesture;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.music.SharedPitchContextController;
import com.oikoaudio.fire.sequence.EncoderBank;
import com.oikoaudio.fire.sequence.EncoderBankLayout;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.sequence.EncoderSlotBinding;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;
import com.oikoaudio.fire.sequence.NoteStepAccess;
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
import com.oikoaudio.fire.utils.PatternButtons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ChordStepSurfaceLayer extends Layer implements StepSequencerHost, SeqClipRowHost {
    private static final int CLIP_ROW_PAD_COUNT = 16;
    private static final int CHORD_SOURCE_PAD_OFFSET = 16;
    private static final int CHORD_SOURCE_PAD_COUNT = 16;
    private static final int MIN_OCTAVE = 0;
    private static final int MAX_OCTAVE = 7;
    private static final int MIN_TRANSPOSE = 0;
    private static final int MAX_TRANSPOSE = MAX_OCTAVE * 12 + 11;
    private static final int HELD_NOTE_VELOCITY = 100;
    private static final int STEP_PAD_OFFSET = 32;
    private static final int STEP_COUNT = 32;
    private static final int MAX_CHORD_STEPS = 2048;
    private static final double STEP_LENGTH = 0.25;
    private static final int FINE_STEPS_PER_STEP = 16;
    private static final int OBSERVED_FINE_STEP_CAPACITY = MAX_CHORD_STEPS * FINE_STEPS_PER_STEP;
    private static final double FINE_STEP_LENGTH = STEP_LENGTH / FINE_STEPS_PER_STEP;
    private static final double MIN_GATE_RATIO = 0.25;
    private static final double MAX_GATE_RATIO = 1.0;
    private static final int AUDITION_VELOCITY = 96;
    private static final int LIVE_NOTE_ENCODER_THRESHOLD = 4;
    private static final int LIVE_LAYOUT_ENCODER_THRESHOLD = 6;
    private static final int CHORD_ROOT_ENCODER_THRESHOLD = 16;
    private static final int CHORD_OCTAVE_ENCODER_THRESHOLD = 8;
    private static final int CHORD_FAMILY_ENCODER_THRESHOLD = 8;
    private static final int LIVE_VELOCITY_ENCODER_THRESHOLD = 1;
    private static final int LIVE_PITCH_OFFSET_ENCODER_THRESHOLD = 6;
    private static final int MIN_CHORD_ROOT_OFFSET = -24;
    private static final int MAX_CHORD_ROOT_OFFSET = 24;
    private static final int MIN_MIDI_VALUE = 0;
    private static final int MAX_MIDI_VALUE = 127;
    private static final int MIN_VELOCITY = 1;
    private static final int DEFAULT_LIVE_VELOCITY = 100;
    private static final int DEFAULT_CHORD_STANDARD_VELOCITY = ChordStepAccentEditor.STANDARD_VELOCITY;
    private static final int DEFAULT_CHORD_ACCENTED_VELOCITY = ChordStepAccentEditor.ACCENTED_VELOCITY;
    private static final int DEFAULT_DRUM_MACHINE_LOW_NOTE = 36;
    private static final long TOUCH_RESET_HOLD_MS = 750L;
    private static final long TOUCH_RESET_RECENT_ADJUSTMENT_SUPPRESS_MS = 300L;
    private static final int TOUCH_RESET_TOLERATED_ADJUSTMENT_UNITS = 2;
    private static final int DEFAULT_LIVE_PITCH_BEND = 64;
    private static final int DEFAULT_LIVE_PITCH_EXPRESSION = 64;
    private static final int LIVE_PITCH_BEND_RETURN_STEP = 6;
    private static final long LIVE_PITCH_BEND_RETURN_DELAY_MS = 15L;
    private static final long LIVE_PITCH_BEND_INACTIVITY_RETURN_MS = 120L;
    private static final int MIDI_CC_MOD = 1;
    private static final int MIDI_CC_SUSTAIN = 64;
    private static final int MIDI_CC_SOSTENUTO = 66;
    private static final int MIDI_CC_TIMBRE = 74;
    private static final int[] LIVE_PITCH_OFFSETS = {-24, -19, -12, -7, 0, 7, 12, 19, 24};
    private static final int DEFAULT_LIVE_PITCH_OFFSET_INDEX = 4;
    private static final int DRUM_MACHINE_SCROLL_COARSE_STEPS = 16;
    private static final int MIN_SCALE_DEGREE_GLISS = -14;
    private static final int MAX_SCALE_DEGREE_GLISS = 14;
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

    private final AkaiFireOikontrolExtension driver;
    private final OledDisplay oled;
    private final NoteInput noteInput;
    private final PatternButtons patternButtons;
    private final NoteRepeatHandler noteRepeatHandler;
    private final SharedPitchContextController pitchContext;
    private final Integer[] noteTranslationTable = new Integer[128];
    private final ChordStepPadSurface chordStepPadSurface = new ChordStepPadSurface();
    private final ChordStepAccentControls chordStepAccentControls;
    private final ChordStepChordSelection chordSelection = new ChordStepChordSelection();
    private final ChordStepBuilderController chordBuilder;
    private final Set<Integer> auditioningNotes = new HashSet<>();
    private final Map<Integer, Set<Integer>> clipNotesByStep = new HashMap<>();
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
    private final CursorTrack cursorTrack;
    private final CursorTrack chordStepCursorTrack;
    private final StepSequencerEncoderHandler stepEncoderLayer;
    private final EncoderBankLayout stepEncoderBankLayout;
    private final EncoderTouchResetHandler encoderTouchResetHandler;
    private final ChordStepEditControls chordStepEditControls;
    private final BooleanValueObject lengthDisplay = new BooleanValueObject();

    private boolean inKey = false;
    private boolean noteStepActive = false;
    private boolean mainEncoderPressConsumed = false;
    private final boolean drumPadsOnly;
    private NoteStepSubMode currentStepSubMode = NoteStepSubMode.CHORD_STEP;
    private Integer selectedPresetStepIndex = null;
    private int liveVelocity = DEFAULT_LIVE_VELOCITY;
    private int liveVelocitySensitivity = 100;
    private int chordVelocitySensitivity = 100;
    private int defaultChordVelocity = DEFAULT_CHORD_STANDARD_VELOCITY;
    private int playingStep = -1;
    private int livePitchOffsetIndex = DEFAULT_LIVE_PITCH_OFFSET_INDEX;
    private int liveScaleDegreeGlissOffset = 0;
    private int harmonicNoteCountIndex = 2;
    private int harmonicOctaveSpan = 1;
    private boolean harmonicBassColumns = true;
    private int drumMachineScrollPosition = 0;
    private boolean drumMachineDefaultPageApplied = false;
    private final boolean[][] heldBongoPads = new boolean[2][NoteGridLayout.PAD_COUNT];
    private final int[] heldBongoPadCounts = new int[2];
    private int livePitchBend = DEFAULT_LIVE_PITCH_BEND;
    private boolean livePitchBendTouched = false;
    private int livePitchBendReturnGeneration = 0;
    private int livePitchBendInactivityGeneration = 0;
    private RgbLigthState chordStepBaseColor = OCCUPIED_STEP;
    private final EncoderStepAccumulator liveVelocityEncoder = new EncoderStepAccumulator(LIVE_VELOCITY_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator livePitchOffsetEncoder = new EncoderStepAccumulator(LIVE_PITCH_OFFSET_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator liveScaleEncoder = new EncoderStepAccumulator(LIVE_NOTE_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator liveOctaveEncoder = new EncoderStepAccumulator(LIVE_NOTE_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator liveLayoutEncoder = new EncoderStepAccumulator(LIVE_LAYOUT_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator chordRootEncoder = new EncoderStepAccumulator(CHORD_ROOT_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator chordOctaveEncoder = new EncoderStepAccumulator(CHORD_OCTAVE_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator chordFamilyEncoder = new EncoderStepAccumulator(CHORD_FAMILY_ENCODER_THRESHOLD);
    private final TouchResetGesture touchResetGesture =
            new TouchResetGesture(4, TOUCH_RESET_HOLD_MS, TOUCH_RESET_RECENT_ADJUSTMENT_SUPPRESS_MS,
                    TOUCH_RESET_TOLERATED_ADJUSTMENT_UNITS);
    private enum NoteStepSubMode {
        CHORD_STEP("Chord Step", BiColorLightState.GREEN_HALF, BiColorLightState.GREEN_FULL),
        CLIP_STEP_RECORD("Clip Step Record", BiColorLightState.AMBER_HALF, BiColorLightState.AMBER_FULL);

        private final String displayName;
        private final BiColorLightState idleLight;
        private final BiColorLightState activeLight;

        NoteStepSubMode(final String displayName, final BiColorLightState idleLight,
                        final BiColorLightState activeLight) {
            this.displayName = displayName;
            this.idleLight = idleLight;
            this.activeLight = activeLight;
        }

        public String displayName() {
            return displayName;
        }

        public BiColorLightState idleLight() {
            return idleLight;
        }

        public BiColorLightState activeLight() {
            return activeLight;
        }

        public NoteStepSubMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public ChordStepSurfaceLayer(final AkaiFireOikontrolExtension driver,
                                  final NoteRepeatHandler noteRepeatHandler,
                                  final String layerName) {
        this(driver, noteRepeatHandler, layerName, false);
    }

    public ChordStepSurfaceLayer(final AkaiFireOikontrolExtension driver,
                                  final NoteRepeatHandler noteRepeatHandler,
                                  final String layerName,
                                  final boolean drumPadsOnly) {
        super(driver.getLayers(), layerName);
        this.driver = driver;
        this.pitchContext = driver.getSharedPitchContextController();
        this.drumPadsOnly = false;
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
        this.noteInput = driver.getNoteInput();
        this.patternButtons = driver.getPatternButtons();
        this.noteRepeatHandler = noteRepeatHandler;
        this.encoderTouchResetHandler = new EncoderTouchResetHandler(
                touchResetGesture,
                driver::isEncoderTouchResetEnabled,
                (task, delayMs) -> driver.getHost().scheduleTask(task, delayMs),
                TOUCH_RESET_HOLD_MS,
                oled::clearScreenDelayed);
        final ControllerHost host = driver.getHost();
        this.chordStepCursorTrack = host.createCursorTrack("CHORD_STEP_MODE", "Chord Step", 8, CLIP_ROW_PAD_COUNT, true);
        this.chordStepCursorTrack.name().markInterested();
        this.chordStepCursorTrack.color().markInterested();
        this.chordStepCursorTrack.canHoldNoteData().markInterested();
        this.chordStepCursorTrack.color().addValueObserver((r, g, b) -> chordStepBaseColor = ColorLookup.getColor(r, g, b));
        chordStepBaseColor = ColorLookup.getColor(this.chordStepCursorTrack.color().get());
        this.cursorTrack = chordStepCursorTrack;
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
        noteInput.assignPolyphonicAftertouchToExpression(0, NoteInput.NoteExpression.TIMBRE_UP, 1);
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
        this.stepEncoderBankLayout = createStepEncoderBankLayout();
        this.stepEncoderLayer = new StepSequencerEncoderHandler(this, driver);

        for (int i = 0; i < noteTranslationTable.length; i++) {
            noteTranslationTable[i] = -1;
        }

        bindPads();
        bindButtons();
        bindEncoders();
    }

    public void notifyBlink(final int blinkTicks) {
        clipHandler.notifyBlink(blinkTicks);
    }

    private void bindPads() {
        final RgbButton[] pads = driver.getRgbButtons();
        for (int index = 0; index < pads.length; index++) {
            final int padIndex = index;
            pads[index].bindPressedVelocity(this, velocity -> handlePadPress(padIndex, true, velocity),
                    () -> handlePadPress(padIndex, false, 0), () -> getPadLight(padIndex));
        }
    }

    private void bindButtons() {
        driver.getButton(NoteAssign.STEP_SEQ).bindPressed(this, this::handleStepSeqPressed, this::getStepSeqLightState);
        driver.getButton(NoteAssign.BANK_L).bindPressed(this, pressed -> handleBankButton(pressed, -1),
                this::getBankLightState);
        driver.getButton(NoteAssign.BANK_R).bindPressed(this, pressed -> handleBankButton(pressed, 1),
                this::getBankLightState);
        driver.getButton(NoteAssign.MUTE_1).bindPressed(this, this::handleMute1Button, this::getMute1LightState);
        driver.getButton(NoteAssign.MUTE_2).bindPressed(this, this::handleMute2Button, this::getMute2LightState);
        driver.getButton(NoteAssign.MUTE_3).bindPressed(this, this::handleMute3Button, this::getMute3LightState);
        driver.getButton(NoteAssign.MUTE_4).bindPressed(this, this::handleMute4Button, this::getMute4LightState);
    }

    private void bindEncoders() {
        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.bindEncoder(this, this::handleMainEncoder);
        mainEncoder.bindTouched(this, this::handleMainEncoderPress);
    }

    private void syncEncoderLayers() {
        stepEncoderLayer.activate();
    }

    private void scrollDrumMachineWindow(final int amount) {
    }

    private void retuneLivePads(final Runnable stateChange) {
        stateChange.run();
    }

    private int bongoZoneForPad(final int padIndex) {
        return -1;
    }

    private void showDrumMachineWindowInfo() {
    }

    private void clearHeldBongoPads() {
    }

    private boolean isChordStepModeActive() {
        return noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP;
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
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
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
                return ChordStepSurfaceLayer.this.isBuilderFamily();
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
                ChordStepSurfaceLayer.this.startAuditionSelectedChord(velocity);
            }

            @Override
            public void startAuditionSelectedChord() {
                ChordStepSurfaceLayer.this.startAuditionSelectedChord();
            }

            @Override
            public void stopAuditionNotes() {
                ChordStepSurfaceLayer.this.stopAuditionNotes();
            }

            @Override
            public int currentChordVelocity(final int velocity) {
                return ChordStepSurfaceLayer.this.currentChordVelocity(velocity);
            }

            @Override
            public void assignSelectedChordToHeldSteps(final int velocity) {
                ChordStepSurfaceLayer.this.assignSelectedChordToHeldSteps(velocity);
            }

            @Override
            public boolean assignSelectedChordToSteps(final Set<Integer> stepIndexes, final int velocity) {
                return ChordStepSurfaceLayer.this.assignSelectedChordToSteps(stepIndexes, velocity);
            }

            @Override
            public void showCurrentChord() {
                ChordStepSurfaceLayer.this.showCurrentChord();
            }

            @Override
            public void toggleBuilderNoteOffset(final int sourcePadIndex) {
                ChordStepSurfaceLayer.this.toggleBuilderNoteOffset(sourcePadIndex);
            }

            @Override
            public void applyBuilderToHeldSteps() {
                ChordStepSurfaceLayer.this.applyBuilderToHeldSteps();
            }

            @Override
            public List<NoteStep> heldNotes() {
                return ChordStepSurfaceLayer.this.getHeldNotes();
            }

            @Override
            public void applyChordStepRecurrence(final List<NoteStep> targets,
                                                 final java.util.function.UnaryOperator<RecurrencePattern> updater) {
                ChordStepSurfaceLayer.this.applyChordStepRecurrence(targets, updater);
            }

            @Override
            public boolean ensureSelectedNoteClipSlot() {
                return ChordStepSurfaceLayer.this.ensureSelectedNoteClipSlot();
            }

            @Override
            public boolean isAccentGestureActive() {
                return chordStepAccentControls.isGestureActive();
            }

            @Override
            public void toggleAccentForStep(final int stepIndex) {
                ChordStepSurfaceLayer.this.toggleChordAccentForStep(stepIndex);
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
                ChordStepSurfaceLayer.this.setLastStep(stepIndex);
            }

            @Override
            public void pasteCurrentChordToStep(final int stepIndex) {
                ChordStepSurfaceLayer.this.pasteCurrentChordToStep(stepIndex);
            }

            @Override
            public void clearChordStep(final int stepIndex) {
                chordStepClipEditor.clearChordStep(stepIndex);
            }

            @Override
            public boolean canExtendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
                return ChordStepSurfaceLayer.this.canExtendHeldChordRange(anchorStepIndex, targetStepIndex);
            }

            @Override
            public boolean extendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
                return ChordStepSurfaceLayer.this.extendHeldChordRange(anchorStepIndex, targetStepIndex);
            }

            @Override
            public void showExtendedStepInfo(final int anchorStepIndex, final int targetStepIndex) {
                ChordStepSurfaceLayer.this.showExtendedStepInfo(anchorStepIndex, targetStepIndex);
            }

            @Override
            public void showBlockedStepInfo() {
                ChordStepSurfaceLayer.this.showBlockedStepInfo();
            }

            @Override
            public boolean hasStepStartNote(final int stepIndex) {
                return ChordStepSurfaceLayer.this.hasStepStartNote(stepIndex);
            }

            @Override
            public void loadBuilderFromStep(final int stepIndex) {
                ChordStepSurfaceLayer.this.loadBuilderFromStep(stepIndex);
            }

            @Override
            public void showHeldStepInfo(final int stepIndex) {
                ChordStepSurfaceLayer.this.showHeldStepInfo(stepIndex);
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
                ChordStepSurfaceLayer.this.toggleChordAccentForHeldSteps();
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
                chordStepClipNavigation.adjustLength(amount, ChordStepSurfaceLayer.this::ensureSelectedNoteClipSlot);
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
                chordStepClipNavigation.adjustPlayStart(amount, fine, ChordStepSurfaceLayer.this::ensureSelectedNoteClipSlot);
            }

            @Override
            public boolean completePendingFineNudge() {
                return fineNudgeController.completePendingNudge();
            }

            @Override
            public void clearPendingBankAction() {
                ChordStepSurfaceLayer.this.clearPendingBankAction();
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
        if (x < 0 || x >= STEP_COUNT) {
            return;
        }
        updateStepCache(clipNotesByStep, x, y, state);
    }

    private void handleObservedStepData(final int x, final int y, final int state) {
        chordStepEventIndex.handleObservedStepData(x, y, state);
    }

    private void updateStepCache(final Map<Integer, Set<Integer>> cache, final int x, final int y, final int state) {
        final Set<Integer> stepNotes = cache.computeIfAbsent(x, ignored -> new HashSet<>());
        if (state == NoteStep.State.Empty.ordinal()) {
            stepNotes.remove(y);
            if (stepNotes.isEmpty()) {
                cache.remove(x);
            }
            return;
        }
        stepNotes.add(y);
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
        if (currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            refreshChordStepObservation();
            ensureBuilderSeededIfEmpty();
            if (chordPageCount() > 1) {
                showChordPageInfo();
            } else {
                showCurrentChord();
            }
            return;
        }
        oled.valueInfo("Step Mode", "Clip Step Record");
    }

    private void applyLiveEncoderStepSizes(final EncoderMode mode) {
        final TouchEncoder[] encoders = driver.getEncoders();
        switch (mode) {
            case CHANNEL -> {
                encoders[0].setStepSize(MixerEncoderProfile.STEP_SIZE);
                encoders[1].setStepSize(MixerEncoderProfile.STEP_SIZE);
                encoders[2].setStepSize(MixerEncoderProfile.STEP_SIZE);
                encoders[3].setStepSize(MixerEncoderProfile.STEP_SIZE);
            }
            case MIXER, USER_1, USER_2 -> {
                for (final TouchEncoder encoder : encoders) {
                    encoder.setStepSize(MixerEncoderProfile.STEP_SIZE);
                }
            }
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
        stopAuditionNotes();
        for (final int midiNote : notes) {
            noteInput.sendRawMidiEvent(Midi.NOTE_ON, midiNote, velocity);
            auditioningNotes.add(midiNote);
        }
        oled.valueInfo(currentChordFamilyLabel(), currentChordName());
    }

    private void stopAuditionNotes() {
        if (auditioningNotes.isEmpty()) {
            return;
        }
        for (final int midiNote : auditioningNotes) {
            noteInput.sendRawMidiEvent(Midi.NOTE_OFF, midiNote, 0);
        }
        auditioningNotes.clear();
    }

    private void handleBankButton(final boolean pressed, final int amount) {
        if (noteStepActive) {
            chordStepSurface.handleBankButton(pressed, amount,
                    currentStepSubMode == NoteStepSubMode.CHORD_STEP);
            return;
        }
        if (!pressed) {
            return;
        }
        if (isDrumMachineLiveMode()) {
            oled.valueInfo("Pad Window", "Use Pattern");
            return;
        }
        adjustOctave(amount);
    }

    private BiColorLightState getBankLightState() {
        if (noteStepActive) {
            return BiColorLightState.HALF;
        }
        return BiColorLightState.HALF;
    }

    private BiColorLightState getPitchContextLightState(final int amount, final boolean root) {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            return chordStepSurface.pitchContextLight(amount, root);
        }
        if (root) {
            return BiColorLightState.AMBER_HALF;
        }
        return amount < 0
                ? (getOctave() > MIN_OCTAVE ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF)
                : (getOctave() < MAX_OCTAVE ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF);
    }

    private BiColorLightState getStepSeqLightState() {
        return chordStepSurface.stepButtonLight(
                noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP);
    }

    private int currentChordVelocity(final int rawVelocity) {
        if (chordStepAccentControls.isActive()) {
            return DEFAULT_CHORD_ACCENTED_VELOCITY;
        }
        return LiveVelocityLogic.resolveVelocity(defaultChordVelocity, chordVelocitySensitivity, rawVelocity);
    }

    private void toggleChordAccentForStep(final int stepIndex) {
        final Map<Integer, NoteStep> notesAtStep = chordStepEventIndex.noteStepsAt(stepIndex);
        if (notesAtStep.isEmpty()) {
            return;
        }
        final boolean targetAccent = chordStepAccentControls.toggleAccent(notesAtStep.values(), defaultChordVelocity);
        chordStepAccentControls.markModified();
        oled.valueInfo("Accent", targetAccent ? "Accented" : "Normal");
    }

    private void toggleChordAccentForHeldSteps() {
        final List<NoteStep> heldNotes = getHeldNotes();
        if (heldNotes.isEmpty()) {
            oled.valueInfo("Accent", "No Step");
            return;
        }
        final boolean targetAccent = chordStepAccentControls.toggleAccent(heldNotes, defaultChordVelocity);
        chordStepPadSurface.markModifiedNotes(heldNotes);
        oled.valueInfo("Accent", targetAccent ? "Accented" : "Normal");
        driver.notifyPopup("Accent", targetAccent ? "Accented" : "Normal");
    }

    private boolean isChordStepAccented(final int stepIndex) {
        return chordStepAccentControls.isStepAccented(chordStepEventIndex.noteStepsAt(stepIndex), defaultChordVelocity);
    }

    private boolean isHarmonicLiveMode() {
        return false;
    }

    private boolean isDrumMachineLiveMode() {
        return false;
    }

    public String currentNoteSubModeLabel() {
        return "Chord Step";
    }

    public boolean isHarmonicNoteSubMode() {
        return false;
    }

    public void resetNoteSubMode() {
    }

    public void cycleNoteSubMode() {
        toggleSurfaceVariant();
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

    private void adjustScale(final int amount) {
        if (amount == 0) {
            return;
        }
        final int minScale = liveScaleMinIndex();
        final int nextScale = pitchContext.getScaleIndex() + amount;
        if (nextScale < minScale || nextScale >= pitchContext.getScaleCount()) {
            return;
        }
        applyLayoutChange(() -> driver.setSharedScaleIndex(nextScale));
        showState("Scale");
    }

    public void adjustSharedScaleFromOverview(final int amount) {
        driver.adjustSharedScaleIndex(amount, liveScaleMinIndex());
    }

    private void adjustOctave(final int amount) {
        if (amount == 0) {
            return;
        }
        if (isDrumMachineLiveMode()) {
            scrollDrumMachineWindow(amount * DRUM_MACHINE_SCROLL_COARSE_STEPS);
            return;
        }
        final int nextOctave = Math.max(MIN_OCTAVE, Math.min(MAX_OCTAVE, getOctave() + amount));
        if (nextOctave == getOctave()) {
            return;
        }
        applyLayoutChange(() -> driver.setSharedOctave(nextOctave));
        showState("Octave");
    }

    private static String formatMidiNoteName(final int midiNote) {
        final int octave = midiNote / 12 - 2;
        return NoteGridLayout.noteName(midiNote) + octave;
    }

    private void showDrumMachineLayoutInfo() {
        oled.valueInfo("Drum Layout", "--");
    }

    private void adjustTransposeSemitone(final int amount) {
        if (amount == 0) {
            return;
        }
        applyLayoutChange(() -> driver.adjustSharedRootNote(amount));
        showState("Root");
    }

    private void handleMainEncoder(final int inc) {
        if (driver.isPopupBrowserActive()) {
            driver.routeBrowserMainEncoder(inc);
            return;
        }
        driver.markMainEncoderTurned();
        if (driver.handleMainEncoderGlobalChord(inc)) {
            return;
        }
        if (!noteStepActive && noteRepeatHandler.getNoteRepeatActive().get()) {
            // While transient live note repeat is active, keep SELECT dedicated to repeat-rate edits.
            // Turning through the minimum rate should not drop straight into the underlying encoder role.
            noteRepeatHandler.handleMainEncoder(inc, driver.isGlobalAltHeld(), false);
            return;
        }
        final boolean fine = driver.isGlobalShiftHeld();
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            noteRepeatHandler.handleMainEncoder(inc, driver.isGlobalAltHeld());
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TEMPO_ROLE.equals(mainEncoderRole)) {
            driver.adjustTempo(inc, fine);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_SHUFFLE_ROLE.equals(mainEncoderRole)) {
            driver.adjustGrooveShuffleAmount(inc, fine);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TRACK_SELECT_ROLE.equals(mainEncoderRole)) {
            driver.adjustSelectedTrack(inc, driver.isMainEncoderPressed());
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_DRUM_GRID_ROLE.equals(mainEncoderRole)) {
            oled.valueInfo("Drum Grid", "Drum only");
        } else {
            driver.adjustMainCursorParameter(inc, fine);
        }
    }

    private void handleMainEncoderPress(final boolean pressed) {
        if (driver.isPopupBrowserActive()) {
            driver.routeBrowserMainEncoderPress(pressed);
            return;
        }
        driver.setMainEncoderPressed(pressed);
        if (pressed && driver.isGlobalAltHeld()) {
            mainEncoderPressConsumed = true;
            driver.toggleCurrentDeviceWindow();
            return;
        }
        if (!pressed && mainEncoderPressConsumed) {
            mainEncoderPressConsumed = false;
            return;
        }
        if (!noteStepActive && noteRepeatHandler.getNoteRepeatActive().get()) {
            noteRepeatHandler.handlePressed(pressed);
            return;
        }
        if (pressed && driver.isGlobalShiftHeld()) {
            mainEncoderPressConsumed = true;
            driver.cycleMainEncoderRolePreference();
            return;
        }
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (!pressed && !driver.wasMainEncoderTurnedWhilePressed()) {
            driver.toggleMainEncoderRolePreference();
            return;
        }
        if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            noteRepeatHandler.handlePressed(pressed);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TEMPO_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showTempoInfo();
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_SHUFFLE_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showGrooveShuffleInfo();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TRACK_SELECT_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showSelectedTrackInfo(false);
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_DRUM_GRID_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                oled.valueInfo("Drum Grid", "Drum only");
            } else {
                oled.clearScreenDelayed();
            }
        } else if (pressed) {
            driver.showMainCursorParameterInfo();
        }
    }

    private void applyLayout() {
        for (int i = 0; i < noteTranslationTable.length; i++) {
            noteTranslationTable[i] = -1;
        }
        noteInput.setKeyTranslationTable(noteTranslationTable);
    }

    private void applyLayoutChange(final Runnable stateChange) {
        if (noteStepActive) {
            stateChange.run();
            showContextInfo();
            return;
        }
        retuneLivePads(stateChange);
    }

    private void showLiveVelocityInfo() {
        if (driver.isGlobalShiftHeld()) {
            oled.valueInfo("Default Velocity", Integer.toString(liveVelocity));
            return;
        }
        oled.valueInfo("Velocity", "Sens %d%% / Def %d".formatted(liveVelocitySensitivity, liveVelocity));
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
            if (isDrumMachineLiveMode()) {
                showDrumMachineWindowInfo();
                return;
            }
            oled.valueInfo("Octave", Integer.toString(getOctave()));
            return;
        }
        if ("Layout".equals(focus)) {
            if (isHarmonicLiveMode()) {
                oled.valueInfo("Layout", harmonicBassColumns ? "Bass Columns" : "Full Field");
            } else if (isDrumMachineLiveMode()) {
                showDrumMachineLayoutInfo();
            } else {
                oled.valueInfo("Layout", inKey ? "In Key" : "Chromatic");
            }
            return;
        }
        if ("Interpretation".equals(focus) && noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            oled.valueInfo("Chord Step Mode", chordSelection.interpretationDisplayName());
            return;
        }
        final String liveModeDetail = inKey ? "In Key" : "Chromatic";
        oled.lineInfo("Root %s%d".formatted(NoteGridLayout.noteName(getRootNote()), getOctave()),
                noteStepActive
                        ? "Step: %s\n%s".formatted(currentStepSubMode.displayName(),
                        currentStepSubMode == NoteStepSubMode.CHORD_STEP ? currentChordDisplay() : "Deferred")
                        : "Scale: %s\n%s".formatted(getScaleDisplayName(), liveModeDetail));
    }

    private void showContextInfo() {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            showCurrentChord();
            return;
        }
        if (noteStepActive) {
            oled.valueInfo("Step Mode", currentStepSubMode.displayName());
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

    private void showChordFamilyInfo() {
        if (isBuilderFamily()) {
            oled.valueInfo("Chord Family", ChordStepChordSelection.BUILDER_FAMILY_LABEL);
            return;
        }
        oled.valueInfo("Chord Family", chordSelection.rawFamilyName());
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

    public MusicalScale getCurrentScale() {
        return getScale();
    }

    private String getScaleDisplayName() {
        return pitchContext.getShortScaleDisplayName();
    }

    public String getCurrentScaleDisplayName() {
        return getScaleDisplayName();
    }

    private int getRootNote() {
        return pitchContext.getRootNote();
    }

    public int getCurrentRootNoteClass() {
        return getRootNote();
    }

    private int getOctave() {
        return pitchContext.getOctave();
    }

    public int getCurrentOctave() {
        return getOctave();
    }

    public int getCurrentBaseMidiNote() {
        return pitchContext.getBaseMidiNote();
    }

    private int liveScaleMinIndex() {
        return 1;
    }

    private int getBuilderFirstVisibleMidiNote() {
        final int midiNote = getChordRootMidi();
        return midiNote >= 0 && midiNote <= 127 ? midiNote : -1;
    }

    private NoteGridLayout createLayout() {
        return new NoteGridLayout(getScale(), getRootNote(), getOctave(), false);
    }

    private int applyLivePitchOffset(final int midiNote) {
        return midiNote >= 0 && midiNote <= 127 ? midiNote : -1;
    }

    private void clearTranslation() {
        chordStepPadSurface.setHeldStepAnchor(null);
        for (int i = 0; i < noteTranslationTable.length; i++) {
            noteTranslationTable[i] = -1;
        }
        noteInput.setKeyTranslationTable(noteTranslationTable);
    }

    private void handlePatternUp(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            chordStepSurface.handlePatternUp(true);
            return;
        }
        adjustOctave(1);
    }

    private void handlePatternDown(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            chordStepSurface.handlePatternDown(true);
            return;
        }
        adjustOctave(-1);
    }

    private BiColorLightState getPatternUpLight() {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            return chordStepSurface.patternUpLight();
        }
        return BiColorLightState.GREEN_HALF;
    }

    private BiColorLightState getPatternDownLight() {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
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
        return "%s <%d>".formatted(currentStepSubMode.displayName(), heldNotes.size());
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
        return currentStepSubMode.displayName();
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

    private void adjustMixerParameter(final Parameter parameter, final String fallbackLabel, final int inc) {
        EncoderValueProfile.LARGE_RANGE.adjustParameter(parameter, driver.isGlobalShiftHeld(), inc);
        oled.valueInfo(fallbackLabel, parameter.displayedValue().get());
    }

    @Override
    public EncoderBankLayout getEncoderBankLayout() {
        return stepEncoderBankLayout;
    }

    private EncoderBankLayout createStepEncoderBankLayout() {
        final Map<EncoderMode, EncoderBank> banks = new EnumMap<>(EncoderMode.class);
        banks.put(EncoderMode.CHANNEL, new EncoderBank(
                "1: Octave/Root\n2: Velocity\n3: Chord Family\n4: Interpret/Invert",
                new EncoderSlotBinding[]{
                        chordPitchContextSlot(),
                        chordBuildVelocitySlot(),
                        chordSlot(2, chordFamilyEncoder,
                                amount -> {
                                    if (driver.isGlobalAltHeld()) {
                                        adjustChordPage(amount);
                                    } else {
                                        adjustChordFamily(amount);
                                    }
                                },
                                this::showCurrentChord, this::resetChordFamilySelection),
                        interpretSlot()
                }));
        banks.put(EncoderMode.MIXER, new EncoderBank(
                "1: Volume\n2: Pan\n3: Send 1\n4: Send 2",
                new EncoderSlotBinding[]{
                        chordMixerSlot(0, "Volume"),
                        chordMixerSlot(1, "Pan"),
                        chordMixerSlot(2, "Send 1"),
                        chordMixerSlot(3, "Send 2")
                }));
        banks.put(EncoderMode.USER_1, new EncoderBank(
                "1: Velocity\n2: Pressure\n3: Timbre\n4: Pitch Expr",
                new EncoderSlotBinding[]{
                        noteAccessSlot(NoteStepAccess.VELOCITY),
                        noteAccessSlot(NoteStepAccess.PRESSURE),
                        noteAccessSlot(NoteStepAccess.TIMBRE),
                        noteAccessSlot(NoteStepAccess.PITCH)
                }));
        banks.put(EncoderMode.USER_2, new EncoderBank(
                "1: Note Length\n2: Chance\n3: Vel Spread\n4: Repeat",
                new EncoderSlotBinding[]{
                        noteAccessSlot(NoteStepAccess.DURATION),
                        noteAccessSlot(NoteStepAccess.CHANCE),
                        noteAccessSlot(NoteStepAccess.VELOCITY_SPREAD),
                        noteAccessSlot(NoteStepAccess.REPEATS)
                }));
        return new EncoderBankLayout(banks);
    }

    private EncoderSlotBinding noteAccessSlot(final NoteStepAccess access) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return access.getResolution();
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                handler.bindNoteAccess(layer, encoder, slotIndex, access);
            }
        };
    }

    private EncoderSlotBinding chordPitchContextSlot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return 0.25;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    final boolean rootContext = driver.isGlobalAltHeld();
                    final EncoderStepAccumulator accumulator = rootContext ? chordRootEncoder : chordOctaveEncoder;
                    final int amount = accumulator.consume(inc);
                    if (amount == 0) {
                        return;
                    }
                    handler.recordTouchAdjustment(slotIndex, Math.abs(amount));
                    encoderTouchResetHandler.markAdjusted(rootContext ? 1 : 0);
                    if (rootContext) {
                        adjustChordRoot(amount);
                    } else {
                        adjustChordOctave(amount);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        final boolean rootContext = driver.isGlobalAltHeld();
                        handler.beginTouchReset(slotIndex, () -> {
                            (rootContext ? chordRootEncoder : chordOctaveEncoder).reset();
                            if (rootContext) {
                                resetChordRoot();
                                showChordRootInfo();
                            } else {
                                resetChordOctave();
                                showChordOctaveInfo();
                            }
                        });
                        if (rootContext) {
                            showChordRootInfo();
                        } else {
                            showChordOctaveInfo();
                        }
                        return;
                    }
                    handler.endTouchReset(slotIndex);
                    oled.clearScreenDelayed();
                });
            }
        };
    }

    private EncoderSlotBinding chordBuildVelocitySlot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    handler.recordTouchAdjustment(slotIndex, Math.abs(inc));
                    if (driver.isGlobalShiftHeld()) {
                        adjustDefaultChordVelocity(inc);
                    } else {
                        adjustChordVelocitySensitivity(inc);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        handler.beginTouchReset(slotIndex, () -> {
                            resetChordVelocityDefaults();
                            showChordVelocityInfo();
                        });
                        showChordVelocityInfo();
                        return;
                    }
                    handler.endTouchReset(slotIndex);
                    oled.clearScreenDelayed();
                });
            }
        };
    }

    private void adjustDefaultChordVelocity(final int inc) {
        final int nextVelocity = Math.max(MIN_VELOCITY,
                Math.min(DEFAULT_CHORD_ACCENTED_VELOCITY - 1, defaultChordVelocity + inc));
        if (nextVelocity == defaultChordVelocity) {
            return;
        }
        defaultChordVelocity = nextVelocity;
        oled.paramInfo("Default Velocity", defaultChordVelocity, "Chord Step", MIN_VELOCITY,
                DEFAULT_CHORD_ACCENTED_VELOCITY - 1);
    }

    private void adjustChordVelocitySensitivity(final int inc) {
        final int nextSensitivity = LiveVelocityLogic.clampSensitivity(chordVelocitySensitivity + inc);
        if (nextSensitivity == chordVelocitySensitivity) {
            return;
        }
        chordVelocitySensitivity = nextSensitivity;
        oled.paramInfo("Velocity Sens", chordVelocitySensitivity, "Chord Step", 0, 100);
    }

    private void resetChordVelocityDefaults() {
        defaultChordVelocity = DEFAULT_CHORD_STANDARD_VELOCITY;
        chordVelocitySensitivity = 100;
    }

    private void showChordVelocityInfo() {
        if (driver.isGlobalShiftHeld()) {
            oled.valueInfo("Default Velocity", Integer.toString(defaultChordVelocity));
            return;
        }
        oled.valueInfo("Velocity", "Sens %d%% / Def %d".formatted(chordVelocitySensitivity, defaultChordVelocity));
    }

    private EncoderSlotBinding emptySlot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> { });
                encoder.bindTouched(layer, touched -> {
                    if (!touched) {
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private EncoderSlotBinding chordMixerSlot(final int index, final String label) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                final com.bitwig.extension.controller.api.Parameter parameter = switch (index) {
                    case 0 -> cursorTrack.volume();
                    case 1 -> cursorTrack.pan();
                    case 2 -> cursorTrack.sendBank().getItemAt(0);
                    default -> cursorTrack.sendBank().getItemAt(1);
                };
                parameter.name().markInterested();
                parameter.displayedValue().markInterested();
                parameter.value().markInterested();
                encoder.bindContinuousEncoder(layer, driver::isGlobalShiftHeld,
                        com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile.STRONG,
                        inc -> adjustMixerParameter(parameter, label, inc));
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        oled.valueInfo(label, parameter.displayedValue().get());
                    } else {
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
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
        return clipNotesByStep.containsKey(stepIndex) || chordStepEventIndex.hasVisibleStepContent(stepIndex);
    }

    private boolean hasStepStartNote(final int stepIndex) {
        return chordStepEventIndex.hasStepStart(stepIndex);
    }

    private Set<Integer> bestAvailableStepNotes(final int stepIndex) {
        final Set<Integer> visibleNotes = clipNotesByStep.get(stepIndex);
        if (visibleNotes != null && !visibleNotes.isEmpty()) {
            return new HashSet<>(visibleNotes);
        }
        return chordStepEventIndex.notesAtStep(stepIndex);
    }

    @FunctionalInterface
    private interface ChordAdjuster {
        void adjust(int amount);
    }

    private EncoderSlotBinding chordSlot(final int slotIndex, final EncoderStepAccumulator accumulator,
                                          final ChordAdjuster adjuster, final Runnable showInfo,
                                          final Runnable resetAction) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return 0.25;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int boundSlotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    final int amount = accumulator.consume(inc);
                    if (amount != 0) {
                        handler.recordTouchAdjustment(boundSlotIndex, Math.abs(amount));
                        encoderTouchResetHandler.markAdjusted(slotIndex);
                        adjuster.adjust(amount);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        handler.beginTouchReset(boundSlotIndex, () -> {
                            accumulator.reset();
                            resetAction.run();
                            showInfo.run();
                        });
                        showInfo.run();
                        return;
                    }
                    handler.endTouchReset(boundSlotIndex);
                    oled.clearScreenDelayed();
                });
            }
        };
    }

    private EncoderSlotBinding interpretSlot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return 1.0;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    if (inc != 0) {
                        handler.recordTouchAdjustment(slotIndex, Math.abs(inc));
                        if (driver.isGlobalShiftHeld()) {
                            adjustChordSharedScale(inc);
                        } else if (driver.isGlobalAltHeld()) {
                            invertCurrentChord(inc > 0 ? 1 : -1);
                        } else {
                            adjustChordInterpretation(inc);
                        }
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        if (driver.isGlobalShiftHeld()) {
                            handler.beginTouchReset(slotIndex, () -> { });
                            oled.valueInfo("Scale", getScaleDisplayName());
                            return;
                        }
                        if (driver.isGlobalAltHeld()) {
                            handler.beginTouchReset(slotIndex, () -> { });
                            oled.valueInfo("Invert", "Turn encoder");
                            return;
                        }
                        handler.beginTouchReset(slotIndex, () -> {
                            chordSelection.resetInterpretation();
                            oled.valueInfo("Interpret", chordSelection.interpretationDisplayName());
                        });
                        oled.valueInfo("Interpret", chordSelection.interpretationDisplayName());
                        return;
                    }
                    handler.endTouchReset(slotIndex);
                    oled.clearScreenDelayed();
                });
            }
        };
    }

    private void applyLiveVelocity() {
        final Integer[] velocityTable = new Integer[128];
        for (int i = 0; i < velocityTable.length; i++) {
            velocityTable[i] = liveVelocity;
        }
        noteInput.setVelocityTranslationTable(velocityTable);
        noteRepeatHandler.setNoteInputVelocity(liveVelocity);
    }

    @Override
    protected void onActivate() {
        noteStepActive = true;
        currentStepSubMode = NoteStepSubMode.CHORD_STEP;
        chordSelection.resetToBuilder();
        chordStepPadSurface.clearStepTracking();
        selectedPresetStepIndex = null;
        chordStepClips.position().setPage(0);
        patternButtons.setUpCallback(this::handlePatternUp, this::getPatternUpLight);
        patternButtons.setDownCallback(this::handlePatternDown, this::getPatternDownLight);
        stepEncoderLayer.deactivate();
        enterCurrentStepSubMode();
    }

    @Override
    protected void onDeactivate() {
        patternButtons.setUpCallback(pressed -> { }, () -> BiColorLightState.OFF);
        patternButtons.setDownCallback(pressed -> { }, () -> BiColorLightState.OFF);
        noteStepActive = false;
        clearHeldBongoPads();
        stopAuditionNotes();
        chordSelection.resetToBuilder();
        chordStepPadSurface.clearStepTracking();
        selectedPresetStepIndex = null;
        stepEncoderLayer.deactivate();
        clearTranslation();
    }

}
