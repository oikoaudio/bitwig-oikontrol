package com.oikoaudio.fire.note;

import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
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
import com.oikoaudio.fire.chordstep.ChordStepAccentControls;
import com.oikoaudio.fire.chordstep.ChordStepAccentEditor;
import com.oikoaudio.fire.chordstep.ChordStepBuilderController;
import com.oikoaudio.fire.chordstep.ChordStepChordSelection;
import com.oikoaudio.fire.chordstep.ChordStepClipController;
import com.oikoaudio.fire.chordstep.ChordStepClipEditor;
import com.oikoaudio.fire.chordstep.ChordStepClipNavigation;
import com.oikoaudio.fire.chordstep.ChordStepController;
import com.oikoaudio.fire.chordstep.ChordStepEditControls;
import com.oikoaudio.fire.chordstep.ChordStepEventIndex;
import com.oikoaudio.fire.chordstep.ChordStepFineNudgeController;
import com.oikoaudio.fire.chordstep.ChordStepFineNudgeState;
import com.oikoaudio.fire.chordstep.ChordStepObservationController;
import com.oikoaudio.fire.chordstep.ChordStepPadLightRenderer;
import com.oikoaudio.fire.chordstep.ChordStepPadController;
import com.oikoaudio.fire.chordstep.ChordStepPadSurface;
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
import com.oikoaudio.fire.values.StepViewPosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class PitchedSurfaceLayer extends Layer implements StepSequencerHost, SeqClipRowHost {
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
    private static final int MAX_DRUM_MACHINE_SCROLL_POSITION = MAX_MIDI_VALUE - DrumMachinePadLayout.PAD_WINDOW_SIZE + 1;
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
    private final ChordStepPadController chordStepPadController;
    private final ChordStepPadLightRenderer chordStepPadLightRenderer;
    private final ChordStepAccentControls chordStepAccentControls;
    private final ChordStepChordSelection chordSelection = new ChordStepChordSelection();
    private final ChordStepBuilderController chordBuilder;
    private final Set<Integer> auditioningNotes = new HashSet<>();
    private final Map<Integer, Set<Integer>> clipNotesByStep = new HashMap<>();
    private final ChordStepFineNudgeState<ChordStepEventIndex.Event> fineNudgeState = new ChordStepFineNudgeState<>();
    private final ChordStepFineNudgeController<ChordStepEventIndex.Event> fineNudgeController;
    private final ChordStepClipController chordStepClipController;
    private final ChordStepClipEditor<ChordStepEventIndex.Event> chordStepClipEditor;
    private final ChordStepClipNavigation chordStepClipNavigation;
    private final ChordStepObservationController chordStepObservationController;
    private final NotePlayController notePlayController;
    private final ChordStepController chordStepController;
    private final ChordStepEventIndex chordStepEventIndex;
    private final NoteLiveControlSurface liveControls;
    private final NoteLivePadPerformer livePadPerformer;
    private final NoteLiveExpressionControls liveExpressionControls;
    private final ClipRowHandler clipHandler;
    private final CursorTrack cursorTrack;
    private final PinnableCursorClip noteStepClip;
    private final Clip observedNoteClip;
    private final StepViewPosition chordStepPosition;
    private final ClipLauncherSlotBank noteClipSlotBank;
    private final PinnableCursorDevice liveCursorDevice;
    private final PinnableCursorDevice liveDrumMachineDevice;
    private final DrumPadBank liveDrumPadBank;
    private final CursorRemoteControlsPage liveRemoteControlsPage;
    private final Layer liveModeControlLayer;
    private final Layer liveChannelLayer;
    private final Layer liveMixerLayer;
    private final Layer liveUser1Layer;
    private final Layer liveUser2Layer;
    private final StepSequencerEncoderHandler stepEncoderLayer;
    private final EncoderBankLayout stepEncoderBankLayout;
    private final EncoderTouchResetHandler encoderTouchResetHandler;
    private final ChordStepEditControls chordStepEditControls;
    private final BooleanValueObject lengthDisplay = new BooleanValueObject();

    private enum LiveNoteSubMode {
        MELODIC("Note"),
        HARMONIC("Harmonic"),
        DRUM_PADS("Drum Pads");

        private final String displayName;

        LiveNoteSubMode(final String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        public LiveNoteSubMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private enum LivePitchGlissMode {
        FIFTH_OCTAVE("5th/8v"),
        SCALE_DEGREE("ScaleDeg");

        private final String displayName;

        LivePitchGlissMode(final String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private boolean inKey = false;
    private boolean noteStepActive = false;
    private boolean mainEncoderPressConsumed = false;
    private final boolean drumPadsOnly;
    private LiveNoteSubMode liveNoteSubMode = LiveNoteSubMode.MELODIC;
    private NoteStepSubMode currentStepSubMode = NoteStepSubMode.CHORD_STEP;
    private Integer selectedPresetStepIndex = null;
    private int liveVelocity = DEFAULT_LIVE_VELOCITY;
    private int liveVelocitySensitivity = 100;
    private int chordVelocitySensitivity = 100;
    private int defaultChordVelocity = DEFAULT_CHORD_STANDARD_VELOCITY;
    private int playingStep = -1;
    private int livePitchOffsetIndex = DEFAULT_LIVE_PITCH_OFFSET_INDEX;
    private int liveScaleDegreeGlissOffset = 0;
    private LivePitchGlissMode livePitchGlissMode = LivePitchGlissMode.FIFTH_OCTAVE;
    private int harmonicNoteCountIndex = 2;
    private int harmonicOctaveSpan = 1;
    private boolean harmonicBassColumns = true;
    private int drumMachineScrollPosition = 0;
    private DrumMachinePadLayout.Layout drumMachineLayout = DrumMachinePadLayout.Layout.GRID64;
    private int selectedDrumPadOffset = 0;
    private int secondaryDrumPadOffset = 1;
    private boolean drumMachineDefaultPageApplied = false;
    private final boolean[][] heldBongoPads = new boolean[2][NoteGridLayout.PAD_COUNT];
    private final int[] heldBongoPadCounts = new int[2];
    private final RgbLigthState[] drumMachinePadColors = new RgbLigthState[DrumMachinePadLayout.PAD_WINDOW_SIZE];
    private final String[] drumMachinePadNames = new String[DrumMachinePadLayout.PAD_WINDOW_SIZE];
    private final boolean[] drumMachinePadExists = new boolean[DrumMachinePadLayout.PAD_WINDOW_SIZE];
    private int livePitchBend = DEFAULT_LIVE_PITCH_BEND;
    private boolean livePitchBendTouched = false;
    private int livePitchBendReturnGeneration = 0;
    private int livePitchBendInactivityGeneration = 0;
    private boolean bankMoveInFlight = false;
    private int bankMoveGeneration = 0;
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

    protected PitchedSurfaceLayer(final AkaiFireOikontrolExtension driver,
                                  final NoteRepeatHandler noteRepeatHandler,
                                  final String layerName) {
        this(driver, noteRepeatHandler, layerName, false);
    }

    protected PitchedSurfaceLayer(final AkaiFireOikontrolExtension driver,
                                  final NoteRepeatHandler noteRepeatHandler,
                                  final String layerName,
                                  final boolean drumPadsOnly) {
        super(driver.getLayers(), layerName);
        this.driver = driver;
        this.pitchContext = driver.getSharedPitchContextController();
        this.drumPadsOnly = drumPadsOnly;
        this.liveNoteSubMode = drumPadsOnly ? LiveNoteSubMode.DRUM_PADS : LiveNoteSubMode.MELODIC;
        this.oled = driver.getOled();
        this.chordStepAccentControls = new ChordStepAccentControls(oled);
        this.chordBuilder = new ChordStepBuilderController(chordSelection, pitchContext,
                this::getBuilderFirstVisibleMidiNote, CHORD_SOURCE_PAD_COUNT);
        this.chordStepPadController = new ChordStepPadController(chordStepPadSurface,
                CLIP_ROW_PAD_COUNT, CHORD_SOURCE_PAD_OFFSET, STEP_PAD_OFFSET, chordStepPadHost());
        this.noteInput = driver.getNoteInput();
        this.patternButtons = driver.getPatternButtons();
        this.noteRepeatHandler = noteRepeatHandler;
        this.liveModeControlLayer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_PAGE_CONTROLS");
        this.liveChannelLayer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_CHANNEL");
        this.liveMixerLayer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_MIXER");
        this.liveUser1Layer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_USER1");
        this.liveUser2Layer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_USER2");
        this.encoderTouchResetHandler = new EncoderTouchResetHandler(
                touchResetGesture,
                driver::isEncoderTouchResetEnabled,
                (task, delayMs) -> driver.getHost().scheduleTask(task, delayMs),
                TOUCH_RESET_HOLD_MS,
                oled::clearScreenDelayed);
        final NoteLiveEncoderModeControls liveEncoderControls = new NoteLiveEncoderModeControls(
                liveEncoderLayer(liveChannelLayer),
                liveEncoderLayer(liveMixerLayer),
                liveEncoderLayer(liveUser1Layer),
                liveEncoderLayer(liveUser2Layer),
                this::applyLiveEncoderStepSizes,
                this::liveEncoderModeInfo);

        final ControllerHost host = driver.getHost();
        this.cursorTrack = host.createCursorTrack("NOTE_VIEW", "Note View", 8, CLIP_ROW_PAD_COUNT, true);
        this.cursorTrack.name().markInterested();
        this.cursorTrack.color().markInterested();
        this.cursorTrack.canHoldNoteData().markInterested();
        this.cursorTrack.color().addValueObserver((r, g, b) -> chordStepBaseColor = ColorLookup.getColor(r, g, b));
        chordStepBaseColor = ColorLookup.getColor(this.cursorTrack.color().get());
        this.liveCursorDevice = cursorTrack.createCursorDevice("NOTE_LIVE_DEVICE", "Note Live Device", 8,
                CursorDeviceFollowMode.FOLLOW_SELECTION);
        this.liveDrumMachineDevice = drumPadsOnly
                ? driver.getViewControl().getPrimaryDevice()
                : cursorTrack.createCursorDevice("NOTE_DRUM_MACHINE", "Note Drum Machine", 8,
                CursorDeviceFollowMode.FIRST_INSTRUMENT);
        this.liveDrumMachineDevice.hasDrumPads().markInterested();
        this.liveDrumPadBank = liveDrumMachineDevice.createDrumPadBank(DrumMachinePadLayout.PAD_WINDOW_SIZE);
        observeLiveDrumPads();
        this.liveRemoteControlsPage = liveCursorDevice.createCursorRemoteControlsPage(8);
        this.noteStepClip = cursorTrack.createLauncherCursorClip("NOTE_STEP", "NOTE_STEP", STEP_COUNT, 128);
        this.observedNoteClip = host.createLauncherCursorClip(OBSERVED_FINE_STEP_CAPACITY, 128);
        this.chordStepPosition = new StepViewPosition(noteStepClip, STEP_COUNT, "CHORD");
        this.chordStepEventIndex = new ChordStepEventIndex(
                this::localToGlobalStep,
                this::globalToLocalStep,
                this::isVisibleGlobalStep,
                this::currentChordVelocity,
                FINE_STEPS_PER_STEP,
                FINE_STEP_LENGTH,
                STEP_LENGTH);
        this.noteClipSlotBank = cursorTrack.clipLauncherSlotBank();
        this.noteClipSlotBank.cursorIndex().markInterested();
        this.noteStepClip.scrollToKey(0);
        this.observedNoteClip.scrollToKey(0);
        this.observedNoteClip.setStepSize(FINE_STEP_LENGTH);
        this.noteStepClip.getPlayStart().markInterested();
        this.noteStepClip.addStepDataObserver(this::handleStepData);
        this.noteStepClip.addNoteStepObserver(this::handleNoteStepObject);
        this.observedNoteClip.addStepDataObserver(this::handleObservedStepData);
        this.noteStepClip.playingStep().addValueObserver(this::handlePlayingStep);
        this.chordStepClipController = new ChordStepClipController(
                () -> cursorTrack.canHoldNoteData().get(),
                this::hasLoadedNoteClipContent,
                this::queueChordObservationResync,
                this::showClipAvailabilityFailure);
        this.fineNudgeController = new ChordStepFineNudgeController<>(
                fineNudgeState,
                stepIndex -> snapshotChordEventForStep(stepIndex, true),
                this::nudgeHeldNotes);
        this.chordStepClipEditor = new ChordStepClipEditor<>(
                observedNoteClip,
                chordStepEventIndex.observedState(),
                fineNudgeState,
                this::localToGlobalStep,
                this::localToGlobalFineStep,
                this::queueChordObservationResync,
                FINE_STEPS_PER_STEP);
        this.chordStepClipNavigation = new ChordStepClipNavigation(
                noteStepClip,
                chordStepPosition,
                oled,
                driver::notifyPopup,
                this::clearAllBankFineNudgeSessions,
                this::refreshChordStepObservation,
                MAX_CHORD_STEPS,
                FINE_STEPS_PER_STEP,
                FINE_STEP_LENGTH);
        this.chordStepObservationController = new ChordStepObservationController(
                (task, delayTicks) -> driver.getHost().scheduleTask(task, delayTicks),
                noteClipSlotBank,
                () -> driver.getViewControl().getSelectedClipSlotIndex(),
                () -> chordStepBaseColor != null ? chordStepBaseColor : OCCUPIED_STEP,
                chordStepClipController,
                this::clearObservedChordCaches,
                () -> noteStepClip.scrollToKey(0),
                () -> observedNoteClip.scrollToKey(0),
                () -> noteStepClip.scrollToStep(chordStepOffset()),
                () -> observedNoteClip.scrollToStep(0));
        final NoteLivePerformanceControls livePerformanceControls = new NoteLivePerformanceControls(
                value -> noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_SUSTAIN, value),
                value -> noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_SOSTENUTO, value),
                noteRepeatHandler::toggleActive,
                () -> noteRepeatHandler.getNoteRepeatActive().get(),
                oled::valueInfo);
        this.chordStepEditControls = new ChordStepEditControls(oled::valueInfo, oled::clearScreenDelayed);
        this.liveControls = new NoteLiveControlSurface(livePerformanceControls, liveEncoderControls,
                encoderTouchResetHandler, oled::valueInfo, oled::detailInfo, oled::clearScreenDelayed);
        this.livePadPerformer = new NoteLivePadPerformer(
                new NoteLivePadPerformer.MidiOut() {
                    @Override
                    public void noteOn(final int midiNote, final int velocity) {
                        noteInput.sendRawMidiEvent(Midi.NOTE_ON, midiNote, velocity);
                    }

                    @Override
                    public void noteOff(final int midiNote) {
                        noteInput.sendRawMidiEvent(Midi.NOTE_OFF, midiNote, 0);
                    }

                    @Override
                    public void timbre(final int midiNote, final int value) {
                        noteInput.sendRawMidiEvent(Midi.POLY_AT, midiNote, value);
                    }
                },
                this::getLivePadMidiNotes,
                (padIndex, configuredVelocity, rawVelocity) -> resolveLivePadVelocity(padIndex, configuredVelocity, rawVelocity),
                this::resolveLivePadTimbre);
        noteInput.assignPolyphonicAftertouchToExpression(0, NoteInput.NoteExpression.TIMBRE_UP, 1);
        this.liveExpressionControls = new NoteLiveExpressionControls(new NoteLiveExpressionControls.MidiExpressionOut() {
            @Override
            public void channelAftertouch(final int value) {
                noteInput.sendRawMidiEvent(Midi.CHANNEL_AT, value, 0);
            }

            @Override
            public void modulation(final int value) {
                noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_MOD, value);
            }

            @Override
            public void timbre(final int value) {
                noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_TIMBRE, value);
            }

            @Override
            public void pitchBend(final int bend) {
                noteInput.sendRawMidiEvent(Midi.PITCH_BEND, bend & 0x7F, (bend >> 7) & 0x7F);
            }
        });
        this.notePlayController = new NotePlayController(liveControls, livePadPerformer);
        this.chordStepController = new ChordStepController(chordStepEditControls, chordStepClipController, chordStepObservationController);
        chordStepController.observeSelectedClip();
        this.clipHandler = new ClipRowHandler(this);
        this.chordStepPadLightRenderer = new ChordStepPadLightRenderer(
                chordStepPadSurface,
                chordBuilder,
                chordSelection,
                clipHandler::getPadLight,
                this::getHeldNotes,
                this::getChordOccupiedStepColor,
                chordStepPosition::getAvailableSteps,
                () -> playingStep,
                this::hasVisibleStepContent,
                stepIndex -> hasVisibleStepContent(stepIndex) && isChordStepAccented(stepIndex),
                this::isChordStepSustained);
        this.stepEncoderBankLayout = createStepEncoderBankLayout();
        this.stepEncoderLayer = new StepSequencerEncoderHandler(this, driver);

        for (int i = 0; i < liveRemoteControlsPage.getParameterCount(); i++) {
            final Parameter parameter = liveRemoteControlsPage.getParameter(i);
            parameter.name().markInterested();
            parameter.displayedValue().markInterested();
            parameter.value().markInterested();
        }

        for (int i = 0; i < noteTranslationTable.length; i++) {
            noteTranslationTable[i] = -1;
        }

        bindPads();
        bindButtons();
        bindEncoders();
        applyDefaultLayoutPreference();
        applyDefaultVelocitySensitivityPreference();
    }

    private void observeLiveDrumPads() {
        liveDrumPadBank.canScrollBackwards().markInterested();
        liveDrumPadBank.canScrollForwards().markInterested();
        liveDrumPadBank.scrollPosition().markInterested();
        liveDrumPadBank.scrollPosition().addValueObserver(position -> {
            drumMachineScrollPosition = position;
            if (isDrumMachineLiveMode()) {
                retuneLivePads(() -> { });
            }
        });
        drumMachineScrollPosition = liveDrumPadBank.scrollPosition().get();
        for (int index = 0; index < DrumMachinePadLayout.PAD_WINDOW_SIZE; index++) {
            final DrumPad pad = liveDrumPadBank.getItemAt(index);
            final int padIndex = index;
            drumMachinePadColors[padIndex] = null;
            drumMachinePadNames[padIndex] = "";
            pad.exists().markInterested();
            pad.exists().addValueObserver(exists -> drumMachinePadExists[padIndex] = exists);
            drumMachinePadExists[padIndex] = pad.exists().get();
            pad.name().markInterested();
            pad.name().addValueObserver(name -> drumMachinePadNames[padIndex] = name);
            drumMachinePadNames[padIndex] = pad.name().get();
            pad.color().markInterested();
            pad.color().addValueObserver((r, g, b) ->
                    drumMachinePadColors[padIndex] = explicitDrumMachinePadColorOrNull(pad));
            drumMachinePadColors[padIndex] = explicitDrumMachinePadColorOrNull(pad);
        }
    }

    private static RgbLigthState explicitDrumMachinePadColorOrNull(final DrumPad pad) {
        final Color color = pad.color().get();
        if (color == null || color.getAlpha255() == 0) {
            return null;
        }
        final RgbLigthState light = ColorLookup.getColor(color);
        return light == null || light.equals(RgbLigthState.OFF) ? null : light;
    }

    private void applyDefaultLayoutPreference() {
        inKey = false;
        if (pitchContext.getScaleIndex() < 1) {
            driver.setSharedScaleIndex(1);
        }
    }

    private void applyDefaultVelocitySensitivityPreference() {
        liveVelocitySensitivity = driver.getDefaultVelocitySensitivityPreference();
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
        final BiColorButton stepSeqButton = driver.getButton(NoteAssign.STEP_SEQ);
        stepSeqButton.bindPressed(this, this::handleStepSeqPressed, this::getStepSeqLightState);

        final BiColorButton bankLeftButton = driver.getButton(NoteAssign.BANK_L);
        bankLeftButton.bindPressed(this, pressed -> handleBankButton(pressed, -1), this::getBankLightState);

        final BiColorButton bankRightButton = driver.getButton(NoteAssign.BANK_R);
        bankRightButton.bindPressed(this, pressed -> handleBankButton(pressed, 1), this::getBankLightState);

        final BiColorButton mute1Button = driver.getButton(NoteAssign.MUTE_1);
        mute1Button.bindPressed(this, pressed -> handleMute1Button(pressed), this::getMute1LightState);

        final BiColorButton mute2Button = driver.getButton(NoteAssign.MUTE_2);
        mute2Button.bindPressed(this, pressed -> handleMute2Button(pressed), this::getMute2LightState);

        final BiColorButton mute3Button = driver.getButton(NoteAssign.MUTE_3);
        mute3Button.bindPressed(this, pressed -> handleMute3Button(pressed), this::getMute3LightState);

        final BiColorButton mute4Button = driver.getButton(NoteAssign.MUTE_4);
        mute4Button.bindPressed(this, pressed -> handleMute4Button(pressed), this::getMute4LightState);

        final BiColorButton knobModeButton = driver.getButton(NoteAssign.KNOB_MODE);
        knobModeButton.bindPressed(liveModeControlLayer, this::handleLiveModeAdvance, this::getLiveModeLightState);
    }

    private void bindEncoders() {
        final TouchEncoder[] encoders = driver.getEncoders();
        bindLiveChannelEncoders(encoders);
        bindLiveMixerEncoders(encoders);
        bindLiveExpressionEncoders(encoders);
        bindLiveRemoteEncoders(encoders);

        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.bindEncoder(this, this::handleMainEncoder);
        mainEncoder.bindTouched(this, this::handleMainEncoderPress);
    }

    private void bindLiveChannelEncoders(final TouchEncoder[] encoders) {
        encoders[0].bindContinuousEncoder(liveChannelLayer, driver::isGlobalShiftHeld,
                com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile.SOFT, inc -> {
                    if (inc == 0) {
                        return;
                    }
                    if (isDrumMachineLiveMode()) {
                        handleDrumMachineLayoutEncoder(0, inc);
                        return;
                    }
                    liveControls.markEncoderAdjusted(0);
                    adjustLiveModulation(inc);
                });
        encoders[0].bindTouched(liveChannelLayer, touched -> liveControls.handleResettableTouch(0, touched,
                () -> {
                    if (isDrumMachineLiveMode()) {
                        showDrumMachineLayoutInfo();
                    } else {
                        oled.valueInfo("Mod", Integer.toString(liveExpressionControls.modulation()));
                    }
                },
                () -> {
                    if (!isDrumMachineLiveMode()) {
                        resetLiveModulation();
                    }
                }));

        encoders[1].bindContinuousEncoder(liveChannelLayer, driver::isGlobalShiftHeld,
                com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile.SOFT, inc -> {
                    if (inc == 0) {
                        return;
                    }
                    if (isDrumMachineLiveMode()) {
                        handleLiveVelocityEncoder(1, inc);
                        return;
                    }
                    liveControls.markEncoderAdjusted(1);
                    cancelLivePitchBendReturn();
                    adjustLivePitchBend(inc);
                });
        encoders[1].bindTouched(liveChannelLayer, touched -> {
            if (isDrumMachineLiveMode()) {
                liveControls.handleResettableTouch(1, touched,
                        this::showLiveVelocityInfo,
                        liveVelocityEncoder::reset);
                return;
            }
            handleLivePitchBendTouch(touched);
        });

        encoders[2].bindEncoder(liveChannelLayer, this::handleLivePitchGlissEncoder);
        encoders[2].bindTouched(liveChannelLayer, touched -> liveControls.handleResettableTouch(2, touched,
                () -> {
                    if (isDrumMachineLiveMode()) {
                        oled.valueInfo("Drum Pads", "--");
                        return;
                    }
                    oled.valueInfo(driver.isGlobalAltHeld() ? "Gliss Mode" : "Pitch Gliss",
                            driver.isGlobalAltHeld() ? livePitchGlissMode.displayName() : formatLivePitchOffsetDisplay());
                },
                () -> {
                    if (!isDrumMachineLiveMode() && !driver.isGlobalAltHeld()) {
                        livePitchOffsetEncoder.reset();
                    }
                }));

        encoders[3].bindEncoder(liveChannelLayer, this::handleEncoder1);
        encoders[3].bindTouched(liveChannelLayer, touched -> liveControls.handleResettableTouch(3, touched,
                () -> {
                    if (isDrumMachineLiveMode()) {
                        oled.valueInfo("Drum Pads", "--");
                        return;
                    }
                    showState(driver.isGlobalShiftHeld() ? "Layout" : driver.isGlobalAltHeld() ? "Root" : "Scale");
                },
                liveScaleEncoder::reset));
    }

    private void bindLiveExpressionEncoders(final TouchEncoder[] encoders) {
        encoders[0].bindEncoder(liveUser1Layer, inc -> handleLiveVelocityEncoder(0, inc));
        encoders[0].bindTouched(liveUser1Layer, touched -> liveControls.handleResettableTouch(0, touched,
                this::showLiveVelocityInfo,
                liveVelocityEncoder::reset));
        bindResettableLiveMidiEncoder(encoders[1], liveUser1Layer, 1, "Aftertouch",
                this::adjustLivePressure, () -> Integer.toString(liveExpressionControls.pressure()), this::resetLivePressure);
        bindResettableLiveMidiEncoder(encoders[2], liveUser1Layer, 2, "Timbre",
                this::adjustLiveTimbre, () -> Integer.toString(liveExpressionControls.timbre()), this::resetLiveTimbre);
        bindResettableLiveMidiEncoder(encoders[3], liveUser1Layer, 3, "Pitch Expr",
                this::adjustLivePitchExpression, this::formatLivePitchExpressionDisplay, this::resetLivePitchExpression);
    }

    private void bindLiveRemoteEncoders(final TouchEncoder[] encoders) {
        for (int i = 0; i < encoders.length; i++) {
            final int index = i;
            final Parameter parameter = liveRemoteControlsPage.getParameter(index);
            encoders[i].bindContinuousEncoder(liveUser2Layer, driver::isGlobalShiftHeld,
                    com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile.STRONG,
                    inc -> adjustMixerParameter(parameter, parameter.name().get(), inc));
            encoders[i].bindTouched(liveUser2Layer, touched -> {
                if (touched) {
                    oled.valueInfo(parameter.name().get(), parameter.displayedValue().get());
                } else {
                    oled.clearScreenDelayed();
                }
            });
        }
    }

    private void bindLiveMixerEncoders(final TouchEncoder[] encoders) {
        final Track track = cursorTrack;
        final List<com.bitwig.extension.controller.api.Parameter> params = new ArrayList<>();
        params.add(track.volume());
        params.add(track.pan());
        params.add(track.sendBank().getItemAt(0));
        params.add(track.sendBank().getItemAt(1));
        for (final com.bitwig.extension.controller.api.Parameter parameter : params) {
            parameter.name().markInterested();
            parameter.displayedValue().markInterested();
            parameter.value().markInterested();
        }
        for (int i = 0; i < encoders.length; i++) {
            final int index = i;
            final com.bitwig.extension.controller.api.Parameter parameter = params.get(i);
            final String fallbackLabel = switch (i) {
                case 0 -> "Volume";
                case 1 -> "Pan";
                case 2 -> "Send 1";
                default -> "Send 2";
            };
            encoders[i].bindContinuousEncoder(liveMixerLayer, driver::isGlobalShiftHeld,
                    com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile.STRONG, inc -> {
                    if (inc == 0) {
                        return;
                    }
                    if (isHarmonicLiveMode()) {
                            handleHarmonicMixerEncoder(index, inc);
                            return;
                    }
                    adjustMixerParameter(parameter, fallbackLabel, inc);
                    });
            encoders[i].bindTouched(liveMixerLayer, touched -> {
                if (touched) {
                    if (isHarmonicLiveMode()) {
                        showHarmonicMixerInfo(index);
                    } else {
                        oled.valueInfo(parameter.name().get(), parameter.displayedValue().get());
                    }
                } else {
                    oled.clearScreenDelayed();
                }
            });
        }
    }

    private void handleHarmonicMixerEncoder(final int encoderIndex, final int inc) {
        switch (encoderIndex) {
            case 0 -> adjustHarmonicNoteCount(inc);
            case 1 -> adjustHarmonicOctaveSpan(inc);
            case 2 -> adjustHarmonicBassColumns(inc);
            case 3 -> adjustLivePitchOffset(inc);
            default -> { }
        }
    }

    private void showHarmonicMixerInfo(final int encoderIndex) {
        switch (encoderIndex) {
            case 0 -> oled.valueInfo("Notes", harmonicNoteCountDisplay());
            case 1 -> oled.valueInfo("Octaves", Integer.toString(harmonicOctaveSpan));
            case 2 -> oled.valueInfo("Bass Grid", harmonicBassColumns ? "On" : "Off");
            case 3 -> oled.valueInfo("Pitch Gliss", formatLivePitchOffsetDisplay());
            default -> oled.clearScreenDelayed();
        }
    }

    private void bindResettableLiveMidiEncoder(final TouchEncoder encoder, final Layer layer, final int encoderIndex,
                                               final String label, final java.util.function.IntConsumer adjuster,
                                               final java.util.function.Supplier<String> valueSupplier,
                                               final Runnable resetAction) {
        bindResettableLiveMidiEncoder(encoder, layer, encoderIndex, label,
                com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile.SOFT,
                adjuster, valueSupplier, resetAction);
    }

    private void bindResettableLiveMidiEncoder(final TouchEncoder encoder, final Layer layer, final int encoderIndex,
                                               final String label,
                                               final com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile profile,
                                               final java.util.function.IntConsumer adjuster,
                                               final java.util.function.Supplier<String> valueSupplier,
                                               final Runnable resetAction) {
        encoder.bindContinuousEncoder(layer, driver::isGlobalShiftHeld,
                profile, inc -> {
            if (inc == 0) {
                return;
            }
            liveControls.markEncoderAdjusted(encoderIndex);
            adjuster.accept(inc);
        });
        encoder.bindTouched(layer, touched -> liveControls.handleResettableTouch(encoderIndex, touched,
                () -> oled.valueInfo(label, valueSupplier.get()),
                resetAction));
    }

    private void bindLivePitchBendEncoder(final TouchEncoder encoder, final Layer layer, final int encoderIndex) {
        encoder.bindContinuousEncoder(layer, driver::isGlobalShiftHeld,
                com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile.SOFT, inc -> {
                    if (inc == 0) {
                        return;
                    }
                    liveControls.markEncoderAdjusted(encoderIndex);
                    cancelLivePitchBendReturn();
                    adjustLivePitchBend(inc);
                });
        encoder.bindTouched(layer, this::handleLivePitchBendTouch);
    }

    private void handleLiveVelocityEncoder(final int inc) {
        handleLiveVelocityEncoder(1, inc);
    }

    private void handleLiveVelocityEncoder(final int encoderIndex, final int inc) {
        final int steps = liveVelocityEncoder.consume(inc);
        if (steps == 0) {
            return;
        }
        liveControls.markEncoderAdjusted(encoderIndex);
        if (driver.isGlobalShiftHeld()) {
            final int nextVelocity = Math.max(MIN_VELOCITY, Math.min(MAX_MIDI_VALUE, liveVelocity + steps));
            if (nextVelocity == liveVelocity) {
                return;
            }
            liveVelocity = nextVelocity;
            applyLiveVelocity();
            oled.paramInfo("Default Velocity", liveVelocity, "Live Note", MIN_VELOCITY, MAX_MIDI_VALUE);
            return;
        }
        final int nextSensitivity = LiveVelocityLogic.clampSensitivity(liveVelocitySensitivity + steps);
        if (nextSensitivity == liveVelocitySensitivity) {
            return;
        }
        liveVelocitySensitivity = nextSensitivity;
        oled.paramInfo("Velocity Sens", liveVelocitySensitivity, "Live Note", 0, 100);
    }

    private void handleDrumMachineLayoutEncoder(final int encoderIndex, final int inc) {
        final int steps = liveScaleEncoder.consume(inc);
        if (steps == 0) {
            return;
        }
        liveControls.markEncoderAdjusted(encoderIndex);
        adjustDrumMachineLayout(steps);
    }

    private void handleEncoder1(final int inc) {
        if (isDrumMachineLiveMode()) {
            return;
        }
        final int steps = liveScaleEncoder.consume(inc);
        if (steps != 0) {
            liveControls.markEncoderAdjusted(3);
            if (driver.isGlobalShiftHeld()) {
                adjustLayout(steps);
            } else if (driver.isGlobalAltHeld()) {
                adjustTransposeSemitone(steps);
            } else {
                adjustScale(steps);
            }
        }
    }

    private void handleEncoder2(final int inc) {
        final int steps = liveOctaveEncoder.consume(inc);
        if (steps != 0) {
            liveControls.markEncoderAdjusted(2);
            adjustOctave(steps);
        }
    }

    private void handleEncoder3(final int inc) {
        final int steps = liveLayoutEncoder.consume(inc);
        if (steps != 0) {
            liveControls.markEncoderAdjusted(3);
            adjustLayout(steps);
        }
    }

    private void adjustLivePressure(final int inc) {
        if (liveExpressionControls.adjustPressure(inc)) {
            oled.paramInfo("Aftertouch", liveExpressionControls.pressure(), "Live Note", MIN_MIDI_VALUE, MAX_MIDI_VALUE);
        }
    }

    private void adjustLiveTimbre(final int inc) {
        if (liveExpressionControls.adjustTimbre(inc)) {
            oled.paramInfo("Timbre", liveExpressionControls.timbre(), "Live Note", MIN_MIDI_VALUE, MAX_MIDI_VALUE);
        }
    }

    private void adjustLiveModulation(final int inc) {
        if (liveExpressionControls.adjustModulation(inc)) {
            oled.paramInfo("Mod", liveExpressionControls.modulation(), "Live Note", MIN_MIDI_VALUE, MAX_MIDI_VALUE);
        }
    }

    private void adjustLivePitchBend(final int inc) {
        final int next = Math.max(MIN_MIDI_VALUE, Math.min(MAX_MIDI_VALUE, livePitchBend + inc));
        if (next == livePitchBend) {
            return;
        }
        livePitchBend = next;
        liveExpressionControls.setTransientPitchBendValue(livePitchBend);
        oled.valueInfo("Pitch Bend", formatSignedValue(livePitchBend - DEFAULT_LIVE_PITCH_BEND));
        armLivePitchBendInactivityReturn();
    }

    private void adjustLivePitchExpression(final int inc) {
        if (liveExpressionControls.adjustPitchExpression(inc)) {
            oled.valueInfo("Pitch Expr", formatLivePitchExpressionDisplay());
        }
    }

    private void handleLivePitchGlissEncoder(final int inc) {
        if (isDrumMachineLiveMode()) {
            return;
        }
        if (driver.isGlobalAltHeld()) {
            toggleLivePitchGlissMode(inc);
            return;
        }
        adjustLivePitchOffset(inc);
    }

    private void toggleLivePitchGlissMode(final int inc) {
        if (inc == 0) {
            return;
        }
        final LivePitchGlissMode next = inc < 0 ? LivePitchGlissMode.FIFTH_OCTAVE : LivePitchGlissMode.SCALE_DEGREE;
        if (next == livePitchGlissMode) {
            oled.valueInfo("Gliss Mode", livePitchGlissMode.displayName());
            return;
        }
        livePitchGlissMode = next;
        oled.valueInfo("Gliss Mode", livePitchGlissMode.displayName());
    }

    private void adjustLivePitchOffset(final int inc) {
        final int steps = livePitchOffsetEncoder.consume(inc);
        if (steps == 0) {
            return;
        }
        if (livePitchGlissMode == LivePitchGlissMode.FIFTH_OCTAVE) {
            final int nextIndex = Math.max(0, Math.min(LIVE_PITCH_OFFSETS.length - 1, livePitchOffsetIndex + steps));
            if (nextIndex == livePitchOffsetIndex) {
                return;
            }
            liveControls.markEncoderAdjusted(1);
            retuneLivePads(() -> livePitchOffsetIndex = nextIndex);
            oled.valueInfo("Pitch Gliss", formatLivePitchOffsetDisplay());
            return;
        }
        final int nextOffset = Math.max(MIN_SCALE_DEGREE_GLISS,
                Math.min(MAX_SCALE_DEGREE_GLISS, liveScaleDegreeGlissOffset + steps));
        if (nextOffset == liveScaleDegreeGlissOffset) {
            return;
        }
        liveControls.markEncoderAdjusted(1);
        retuneLivePads(() -> liveScaleDegreeGlissOffset = nextOffset);
        oled.valueInfo("Pitch Gliss", formatLivePitchOffsetDisplay());
    }

    private void adjustHarmonicNoteCount(final int inc) {
        final int steps = liveVelocityEncoder.consume(inc);
        if (steps == 0) {
            return;
        }
        final int next = Math.max(0, Math.min(HarmonicLatticeLayout.NOTE_COUNTS.length - 1, harmonicNoteCountIndex + steps));
        if (next == harmonicNoteCountIndex) {
            return;
        }
        liveControls.markEncoderAdjusted(0);
        retuneLivePads(() -> harmonicNoteCountIndex = next);
        oled.valueInfo("Notes", harmonicNoteCountDisplay());
    }

    private void adjustHarmonicOctaveSpan(final int inc) {
        final int steps = liveOctaveEncoder.consume(inc);
        if (steps == 0) {
            return;
        }
        final int next = Math.max(1, Math.min(3, harmonicOctaveSpan + steps));
        if (next == harmonicOctaveSpan) {
            return;
        }
        liveControls.markEncoderAdjusted(1);
        retuneLivePads(() -> harmonicOctaveSpan = next);
        oled.valueInfo("Octaves", Integer.toString(harmonicOctaveSpan));
    }

    private void adjustHarmonicBassColumns(final int inc) {
        final int steps = liveLayoutEncoder.consume(inc);
        if (steps == 0) {
            return;
        }
        final boolean next = steps > 0;
        if (next == harmonicBassColumns) {
            return;
        }
        liveControls.markEncoderAdjusted(2);
        retuneLivePads(() -> harmonicBassColumns = next);
        oled.valueInfo("Bass Grid", harmonicBassColumns ? "On" : "Off");
    }

    private String harmonicNoteCountDisplay() {
        return Integer.toString(HarmonicLatticeLayout.NOTE_COUNTS[harmonicNoteCountIndex]);
    }

    private int getHarmonicGlissStepOffset() {
        return livePitchGlissMode == LivePitchGlissMode.FIFTH_OCTAVE
                ? livePitchOffsetIndex - DEFAULT_LIVE_PITCH_OFFSET_INDEX
                : liveScaleDegreeGlissOffset;
    }

    static int displayPitchGlissValue(final boolean fifthOctaveMode,
                                      final int livePitchOffset,
                                      final int scaleDegreeGlissOffset) {
        if (!fifthOctaveMode) {
            return scaleDegreeGlissOffset;
        }
        return livePitchOffset;
    }

    private String formatLivePitchExpressionDisplay() {
        return formatSignedValue(liveExpressionControls.pitchExpression() - DEFAULT_LIVE_PITCH_EXPRESSION);
    }

    private String formatLivePitchOffsetDisplay() {
        final int value = displayPitchGlissValue(livePitchGlissMode == LivePitchGlissMode.FIFTH_OCTAVE,
                getLivePitchOffset(), liveScaleDegreeGlissOffset);
        return "%s %s".formatted(livePitchGlissMode.displayName(), formatSignedValue(value));
    }

    private String liveEncoderModeInfo(final EncoderMode mode) {
        if (isDrumMachineLiveMode() && mode == EncoderMode.CHANNEL) {
            return "1: Layout\n2: Velocity\n3: --\n4: --";
        }
        if (isHarmonicLiveMode() && mode == EncoderMode.MIXER) {
            return "1: Notes\n2: Octaves\n3: Bass Grid\n4: Pitch Gliss";
        }
        return NoteLiveEncoderModeControls.modeInfo(mode);
    }

    private void resetLivePressure() {
        liveExpressionControls.resetPressure();
    }

    private void resetLiveModulation() {
        liveExpressionControls.resetModulation();
    }

    private void resetLiveTimbre() {
        liveExpressionControls.resetTimbre();
    }

    private void resetLivePitchExpression() {
        liveExpressionControls.resetPitchExpression();
    }

    private void handleLivePitchBendTouch(final boolean touched) {
        if (isDrumMachineLiveMode()) {
            if (touched) {
                showDrumMachineLayoutInfo();
            } else {
                oled.clearScreenDelayed();
            }
            return;
        }
        livePitchBendTouched = touched;
        cancelLivePitchBendReturn();
        if (touched) {
            oled.valueInfo("Pitch Bend", formatSignedValue(livePitchBend - DEFAULT_LIVE_PITCH_BEND));
            return;
        }
        scheduleLivePitchBendReturn();
    }

    private void scheduleLivePitchBendReturn() {
        if (livePitchBend == DEFAULT_LIVE_PITCH_BEND) {
            oled.clearScreenDelayed();
            return;
        }
        final int generation = ++livePitchBendReturnGeneration;
        driver.getHost().scheduleTask(() -> continueLivePitchBendReturn(generation), LIVE_PITCH_BEND_RETURN_DELAY_MS);
    }

    private void continueLivePitchBendReturn(final int generation) {
        if (generation != livePitchBendReturnGeneration || livePitchBendTouched) {
            return;
        }
        if (livePitchBend < DEFAULT_LIVE_PITCH_BEND) {
            livePitchBend = Math.min(DEFAULT_LIVE_PITCH_BEND, livePitchBend + LIVE_PITCH_BEND_RETURN_STEP);
        } else if (livePitchBend > DEFAULT_LIVE_PITCH_BEND) {
            livePitchBend = Math.max(DEFAULT_LIVE_PITCH_BEND, livePitchBend - LIVE_PITCH_BEND_RETURN_STEP);
        }
        liveExpressionControls.setTransientPitchBendValue(livePitchBend);
        oled.valueInfo("Pitch Bend", formatSignedValue(livePitchBend - DEFAULT_LIVE_PITCH_BEND));
        if (livePitchBend == DEFAULT_LIVE_PITCH_BEND) {
            oled.clearScreenDelayed();
            return;
        }
        driver.getHost().scheduleTask(() -> continueLivePitchBendReturn(generation), LIVE_PITCH_BEND_RETURN_DELAY_MS);
    }

    private void cancelLivePitchBendReturn() {
        livePitchBendReturnGeneration++;
    }

    private void armLivePitchBendInactivityReturn() {
        final int generation = ++livePitchBendInactivityGeneration;
        driver.getHost().scheduleTask(() -> {
            if (generation != livePitchBendInactivityGeneration || livePitchBendTouched
                    || livePitchBend == DEFAULT_LIVE_PITCH_BEND) {
                return;
            }
            scheduleLivePitchBendReturn();
        }, LIVE_PITCH_BEND_INACTIVITY_RETURN_MS);
    }

    private int getLivePitchOffset() {
        return LIVE_PITCH_OFFSETS[livePitchOffsetIndex];
    }

    private boolean isChordStepModeActive() {
        return noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP;
    }

    private void handleMute1Button(final boolean pressed) {
        if (isChordStepModeActive()) {
            chordStepController.handleMute1(pressed);
            return;
        }
        notePlayController.handleMute1(pressed);
    }

    private void handleMute2Button(final boolean pressed) {
        if (isChordStepModeActive()) {
            chordStepController.handleMute2(pressed);
            return;
        }
        notePlayController.handleMute2(pressed);
    }

    private void handleMute3Button(final boolean pressed) {
        if (isChordStepModeActive()) {
            chordStepController.handleMute3(pressed);
            return;
        }
        notePlayController.handleMute3(pressed);
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
        return notePlayController.mute1LightState();
    }

    private BiColorLightState getMute2LightState() {
        if (isChordStepModeActive()) {
            return chordStepController.mute2LightState();
        }
        return notePlayController.mute2LightState();
    }

    private BiColorLightState getMute3LightState() {
        if (isChordStepModeActive()) {
            return chordStepController.mute3LightState();
        }
        return notePlayController.mute3LightState();
    }

    private BiColorLightState getMute4LightState() {
        if (isChordStepModeActive()) {
            return chordStepController.mute4LightState();
        }
        return BiColorLightState.OFF;
    }

    private void handleStepSeqPressed(final boolean pressed) {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            if (pressed && chordStepPadSurface.hasHeldSteps()) {
                toggleChordAccentForHeldSteps();
                return;
            }
            if (driver.isGlobalShiftHeld() || chordStepAccentControls.isHeld()) {
                chordStepAccentControls.handlePressed(pressed);
                return;
            }
            if (driver.isGlobalAltHeld()) {
                if (pressed) {
                    driver.toggleFillMode();
                    oled.valueInfo("Fill", driver.isFillModeActive() ? "On" : "Off");
                }
                return;
            }
            if (pressed) {
                if (isChordStepSurface()) {
                    driver.enterFugueStepMode();
                    return;
                }
                driver.enterMelodicStepMode();
            }
            return;
        }
        if (pressed) {
            driver.enterMelodicStepMode();
        }
    }

    private void handlePadPress(final int padIndex, final boolean pressed, final int velocity) {
        if (!noteStepActive) {
            if (isDrumMachineLiveMode() && drumMachineLayout == DrumMachinePadLayout.Layout.BONGOS
                    && handleBongoSurfaceGate(padIndex, pressed)) {
                return;
            }
            if (pressed && isDrumMachineLiveMode() && handleDrumMachinePadPress(padIndex)) {
                return;
            }
            notePlayController.handlePadPress(padIndex, pressed, velocity, liveVelocity);
            return;
        }
        if (currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            chordStepPadController.handlePadPress(padIndex, pressed, velocity);
            return;
        }
        handleClipStepRecordPadPress(padIndex, pressed);
    }

    private boolean handleBongoSurfaceGate(final int padIndex, final boolean pressed) {
        final int zone = bongoZoneForPad(padIndex);
        if (zone < 0) {
            return false;
        }
        if (pressed) {
            final boolean zoneAlreadyHeld = heldBongoPadCounts[zone] > 0;
            if (!heldBongoPads[zone][padIndex]) {
                heldBongoPads[zone][padIndex] = true;
                heldBongoPadCounts[zone]++;
            }
            if (zoneAlreadyHeld) {
                return true;
            }
            return false;
        }
        if (heldBongoPads[zone][padIndex]) {
            heldBongoPads[zone][padIndex] = false;
            heldBongoPadCounts[zone] = Math.max(0, heldBongoPadCounts[zone] - 1);
        }
        return false;
    }

    private int bongoZoneForPad(final int padIndex) {
        final LiveNoteLayout layout = createLayout();
        if (!(layout instanceof DrumMachinePadLayout drumMachinePadLayout)
                || drumMachinePadLayout.selectorOffsetForPad(padIndex) >= 0) {
            return -1;
        }
        final int column = padIndex % NoteGridLayout.PAD_COLUMNS;
        if (column < 5 || column == 10) {
            return -1;
        }
        return column >= 11 ? 1 : 0;
    }

    private boolean handleDrumMachinePadPress(final int padIndex) {
        final LiveNoteLayout layout = createLayout();
        if (!(layout instanceof DrumMachinePadLayout drumMachinePadLayout)) {
            return false;
        }
        final int selectorOffset = drumMachinePadLayout.selectorOffsetForPad(padIndex);
        if (selectorOffset >= 0) {
            assignDrumMachineSelector(selectorOffset);
            showDrumMachinePadInfo(padIndex);
            return false;
        }
        showDrumMachinePadInfo(padIndex);
        return false;
    }

    private void assignDrumMachineSelector(final int selectorOffset) {
        if (drumMachineLayout == DrumMachinePadLayout.Layout.BONGOS) {
            final int heldZone = heldBongoPadCounts[1] > 0 ? 1 : heldBongoPadCounts[0] > 0 ? 0 : -1;
            if (heldZone == 1) {
                secondaryDrumPadOffset = selectorOffset;
                return;
            }
            if (heldZone == 0) {
                selectedDrumPadOffset = selectorOffset;
                return;
            }
        }
        selectedDrumPadOffset = selectorOffset;
        if (drumMachineLayout == DrumMachinePadLayout.Layout.BONGOS
                && secondaryDrumPadOffset == selectedDrumPadOffset) {
            secondaryDrumPadOffset = Math.min(DrumMachinePadLayout.PAD_WINDOW_SIZE - 1, selectorOffset + 1);
        }
    }

    private void showDrumMachinePadInfo(final int padIndex) {
        final LiveNoteLayout layout = createLayout();
        if (!(layout instanceof DrumMachinePadLayout drumMachinePadLayout)) {
            return;
        }
        final int bankIndex = drumMachinePadLayout.padBankIndexForPad(padIndex);
        if (bankIndex < 0 || bankIndex >= drumMachinePadNames.length) {
            return;
        }
        final int midiNote = drumMachineScrollPosition + bankIndex;
        final String name = drumMachinePadNames[bankIndex] == null || drumMachinePadNames[bankIndex].isBlank()
                ? "MIDI " + midiNote
                : drumMachinePadNames[bankIndex];
        oled.valueInfo("Drum Pad", name);
    }

    private ChordStepPadController.Host chordStepPadHost() {
        return new ChordStepPadController.Host() {
            @Override
            public void handleClipRowPad(final int padIndex, final boolean pressed) {
                clipHandler.handlePadPress(padIndex, pressed);
            }

            @Override
            public boolean isBuilderFamily() {
                return PitchedSurfaceLayer.this.isBuilderFamily();
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
                PitchedSurfaceLayer.this.startAuditionSelectedChord(velocity);
            }

            @Override
            public void startAuditionSelectedChord() {
                PitchedSurfaceLayer.this.startAuditionSelectedChord();
            }

            @Override
            public void stopAuditionNotes() {
                PitchedSurfaceLayer.this.stopAuditionNotes();
            }

            @Override
            public int currentChordVelocity(final int velocity) {
                return PitchedSurfaceLayer.this.currentChordVelocity(velocity);
            }

            @Override
            public void assignSelectedChordToHeldSteps(final int velocity) {
                PitchedSurfaceLayer.this.assignSelectedChordToHeldSteps(velocity);
            }

            @Override
            public boolean assignSelectedChordToSteps(final Set<Integer> stepIndexes, final int velocity) {
                return PitchedSurfaceLayer.this.assignSelectedChordToSteps(stepIndexes, velocity);
            }

            @Override
            public void showCurrentChord() {
                PitchedSurfaceLayer.this.showCurrentChord();
            }

            @Override
            public void toggleBuilderNoteOffset(final int sourcePadIndex) {
                PitchedSurfaceLayer.this.toggleBuilderNoteOffset(sourcePadIndex);
            }

            @Override
            public void applyBuilderToHeldSteps() {
                PitchedSurfaceLayer.this.applyBuilderToHeldSteps();
            }

            @Override
            public List<NoteStep> heldNotes() {
                return PitchedSurfaceLayer.this.getHeldNotes();
            }

            @Override
            public void applyChordStepRecurrence(final List<NoteStep> targets,
                                                 final java.util.function.UnaryOperator<RecurrencePattern> updater) {
                PitchedSurfaceLayer.this.applyChordStepRecurrence(targets, updater);
            }

            @Override
            public boolean ensureSelectedNoteClipSlot() {
                return PitchedSurfaceLayer.this.ensureSelectedNoteClipSlot();
            }

            @Override
            public boolean isAccentGestureActive() {
                return chordStepAccentControls.isGestureActive();
            }

            @Override
            public void toggleAccentForStep(final int stepIndex) {
                PitchedSurfaceLayer.this.toggleChordAccentForStep(stepIndex);
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
                PitchedSurfaceLayer.this.setLastStep(stepIndex);
            }

            @Override
            public void pasteCurrentChordToStep(final int stepIndex) {
                PitchedSurfaceLayer.this.pasteCurrentChordToStep(stepIndex);
            }

            @Override
            public void clearChordStep(final int stepIndex) {
                chordStepClipEditor.clearChordStep(stepIndex);
            }

            @Override
            public boolean canExtendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
                return PitchedSurfaceLayer.this.canExtendHeldChordRange(anchorStepIndex, targetStepIndex);
            }

            @Override
            public boolean extendHeldChordRange(final int anchorStepIndex, final int targetStepIndex) {
                return PitchedSurfaceLayer.this.extendHeldChordRange(anchorStepIndex, targetStepIndex);
            }

            @Override
            public void showExtendedStepInfo(final int anchorStepIndex, final int targetStepIndex) {
                PitchedSurfaceLayer.this.showExtendedStepInfo(anchorStepIndex, targetStepIndex);
            }

            @Override
            public void showBlockedStepInfo() {
                PitchedSurfaceLayer.this.showBlockedStepInfo();
            }

            @Override
            public boolean hasStepStartNote(final int stepIndex) {
                return PitchedSurfaceLayer.this.hasStepStartNote(stepIndex);
            }

            @Override
            public void loadBuilderFromStep(final int stepIndex) {
                PitchedSurfaceLayer.this.loadBuilderFromStep(stepIndex);
            }

            @Override
            public void showHeldStepInfo(final int stepIndex) {
                PitchedSurfaceLayer.this.showHeldStepInfo(stepIndex);
            }

            @Override
            public void removeHeldBankFineStart(final int stepIndex) {
                fineNudgeState.invalidateStep(stepIndex);
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
        noteStepClip.getLoopLength().set(newLength);
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
        if (targetSteps.isEmpty() || chordEventSnapshot.isEmpty()) {
            return;
        }
        chordStepPadSurface.markModifiedSteps(targetSteps);
        final List<ChordStepEventIndex.Event> eventsToNudge = targetSteps.stream()
                .map(chordEventSnapshot::get)
                .filter(java.util.Objects::nonNull)
                .sorted(amount > 0
                        ? Comparator.comparingInt(ChordStepEventIndex.Event::anchorFineStart).reversed()
                        : Comparator.comparingInt(ChordStepEventIndex.Event::anchorFineStart))
                .toList();
        if (eventsToNudge.isEmpty()) {
            return;
        }
        fineNudgeState.beginHeldNudge(targetSteps);
        final int loopFineSteps = chordLoopFineSteps();
        final List<ChordStepEventIndex.EventNoteMove> noteMoves = new ArrayList<>();
        final Map<Integer, ChordStepEventIndex.Event> movedEvents = new HashMap<>();
        for (final ChordStepEventIndex.Event event : eventsToNudge) {
            final int targetAnchorFineStart = Math.floorMod(event.anchorFineStart() + amount, loopFineSteps);
            noteMoves.addAll(chordStepEventIndex.createNoteMovesForEvent(event, targetAnchorFineStart, loopFineSteps));
            movedEvents.put(event.localStep(), chordStepEventIndex.moveEvent(event, targetAnchorFineStart, loopFineSteps));
        }
        if (noteMoves.isEmpty()) {
            return;
        }
        rewriteChordEventMoves(noteMoves);
        for (final ChordStepEventIndex.EventNoteMove move : noteMoves) {
            fineNudgeState.putHeldFineStart(move.localStep(), move.midiNote(), move.targetFineStart());
        }
        movedEvents.forEach((step, event) -> {
            fineNudgeState.putHeldEvent(step, event);
        });
        oled.valueInfo("Nudge", amount > 0 ? "+Fine" : "-Fine");
    }

    private int chordLoopFineSteps() {
        return Math.max(FINE_STEPS_PER_STEP, chordStepPosition.getSteps() * FINE_STEPS_PER_STEP);
    }

    private int chordLoopSteps() {
        return Math.max(1, chordStepPosition.getSteps());
    }

    private boolean ensureChordClipWithinObservedCapacity() {
        if (chordStepPosition.getSteps() <= MAX_CHORD_STEPS) {
            return true;
        }
        oled.valueInfo("Clip too long", Integer.toString(chordStepPosition.getSteps()));
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

    private void updateObservedFineStart(final int oldFineStart, final int newFineStart, final int midiNote,
                                         final double duration) {
        chordStepEventIndex.moveFineStart(oldFineStart, newFineStart, midiNote, duration);
    }

    private void rewriteChordEventMoves(final List<ChordStepEventIndex.EventNoteMove> noteMoves) {
        for (final ChordStepEventIndex.EventNoteMove move : noteMoves) {
            final int targetGlobalStep = Math.floorDiv(move.targetFineStart(), FINE_STEPS_PER_STEP);
            if (move.visibleStep() != null && isVisibleGlobalStep(targetGlobalStep)) {
                chordStepEventIndex.addPendingMoveSnapshot(globalToLocalStep(targetGlobalStep), move.midiNote(),
                        move.visibleStep());
            }
        }
        for (final ChordStepEventIndex.EventNoteMove move : noteMoves) {
            observedNoteClip.clearStep(move.sourceFineStart(), move.midiNote());
        }
        for (final ChordStepEventIndex.EventNoteMove move : noteMoves) {
            observedNoteClip.setStep(move.targetFineStart(), move.midiNote(), move.velocity(), move.duration());
            updateObservedFineStart(move.sourceFineStart(), move.targetFineStart(), move.midiNote(), move.duration());
        }
        beginBankMoveInFlight();
        refreshChordStepObservation();
    }

    private void beginBankMoveInFlight() {
        bankMoveInFlight = true;
        final int generation = ++bankMoveGeneration;
        driver.getHost().scheduleTask(() -> {
            if (bankMoveGeneration == generation) {
                bankMoveInFlight = false;
            }
        }, 24);
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
        releaseHeldLiveNotes();
        chordStepPadSurface.clearStepTracking();
        selectedPresetStepIndex = null;
        chordStepPosition.setPage(0);
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

    private void returnToLivePlay() {
        noteStepActive = false;
        chordStepPadSurface.clearStepTracking();
        selectedPresetStepIndex = null;
        syncEncoderLayers();
        applyLayout();
        showState("Mode");
    }

    private void syncEncoderLayers() {
        final boolean useStepEncoders = noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP;
        if (useStepEncoders) {
            notePlayController.deactivateEncoders();
            liveModeControlLayer.deactivate();
            stepEncoderLayer.activate();
        } else {
            stepEncoderLayer.deactivate();
            notePlayController.activateEncoders();
            liveModeControlLayer.activate();
        }
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
            if (currentStepSubMode == NoteStepSubMode.CHORD_STEP && driver.isGlobalAltHeld()) {
                if (pressed) {
                    fineNudgeState.setPendingLengthAdjust(true);
                    chordStepClipNavigation.adjustLength(amount, this::ensureSelectedNoteClipSlot);
                } else if (fineNudgeState.isPendingLengthAdjust()) {
                    fineNudgeState.setPendingLengthAdjust(false);
                }
                clearPendingBankAction();
                return;
            }
            if (bankMoveInFlight) {
                if (!pressed) {
                    clearPendingBankAction();
                }
                return;
            }
            if (pressed) {
                final boolean heldOnly = chordStepPadSurface.hasHeldSteps();
                if (heldOnly) {
                    fineNudgeController.beginHeldNudge(amount, chordStepPadSurface.heldStepSnapshot());
                } else {
                    chordStepClipNavigation.adjustPlayStart(amount, driver.isGlobalShiftHeld(),
                            this::ensureSelectedNoteClipSlot);
                }
                return;
            }
            if (fineNudgeController.completePendingNudge()) {
                return;
            }
            if (!pressed) {
                clearPendingBankAction();
                return;
            }
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

    private void handleLiveModeAdvance(final boolean pressed) {
        notePlayController.handleModeAdvance(pressed, noteStepActive);
        if (pressed && !noteStepActive) {
            oled.clearScreenDelayed();
        }
    }

    private BiColorLightState getLiveModeLightState() {
        return notePlayController.modeLightState();
    }

    private void handlePitchContextButton(final boolean pressed, final int amount, final boolean root) {
        if (!pressed) {
            return;
        }
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            if (root) {
                adjustChordRoot(amount);
            } else {
                adjustChordOctave(amount);
            }
            return;
        }
        if (root) {
            adjustTransposeSemitone(amount);
        } else {
            adjustOctave(amount);
        }
    }

    private void showTouchedState(final boolean pressed, final String label) {
        if (pressed) {
            showState(label);
        } else {
            oled.clearScreenDelayed();
        }
    }

    private BiColorLightState getBankLightState() {
        if (noteStepActive) {
            return BiColorLightState.HALF;
        }
        return BiColorLightState.HALF;
    }

    private BiColorLightState getPitchContextLightState(final int amount, final boolean root) {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            if (root) {
                return BiColorLightState.AMBER_HALF;
            }
            return amount < 0
                    ? (chordSelection.canLowerOctave() ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF)
                    : (chordSelection.canRaiseOctave() ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF);
        }
        if (root) {
            return BiColorLightState.AMBER_HALF;
        }
        return amount < 0
                ? (getOctave() > MIN_OCTAVE ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF)
                : (getOctave() < MAX_OCTAVE ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF);
    }

    private BiColorLightState getStepSeqLightState() {
        if (!(noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP)) {
            return BiColorLightState.OFF;
        }
        if (driver.isGlobalShiftHeld()) {
            return chordStepAccentControls.isActive() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
        }
        if (driver.isGlobalAltHeld()) {
            return driver.getStepFillLightState();
        }
        return chordStepAccentControls.isActive() ? BiColorLightState.AMBER_FULL : BiColorLightState.OFF;
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
        return !noteStepActive && !isChordStepSurface() && liveNoteSubMode == LiveNoteSubMode.HARMONIC;
    }

    private boolean isDrumMachineLiveMode() {
        return !noteStepActive && !isChordStepSurface() && liveNoteSubMode == LiveNoteSubMode.DRUM_PADS;
    }

    public String currentNoteSubModeLabel() {
        if (liveNoteSubMode == LiveNoteSubMode.DRUM_PADS) {
            return "Drum Pads";
        }
        return liveNoteSubMode.displayName();
    }

    public boolean isHarmonicNoteSubMode() {
        return liveNoteSubMode == LiveNoteSubMode.HARMONIC;
    }

    public void resetNoteSubMode() {
        if (drumPadsOnly || isChordStepSurface() || liveNoteSubMode == LiveNoteSubMode.MELODIC) {
            return;
        }
        applyLayoutChange(() -> liveNoteSubMode = LiveNoteSubMode.MELODIC);
    }

    public void cycleNoteSubMode() {
        if (isChordStepSurface()) {
            toggleSurfaceVariant();
            return;
        }
        if (drumPadsOnly) {
            showDrumMachineLayoutInfo();
            return;
        }
        final LiveNoteSubMode next = liveNoteSubMode == LiveNoteSubMode.MELODIC
                ? LiveNoteSubMode.HARMONIC
                : LiveNoteSubMode.MELODIC;
        applyLayoutChange(() -> {
            liveNoteSubMode = next;
        });
    }

    private void toggleLayout() {
        if (isHarmonicLiveMode()) {
            harmonicBassColumns = !harmonicBassColumns;
            retuneLivePads(() -> { });
            showState("Layout");
            return;
        }
        if (isDrumMachineLiveMode()) {
            showDrumMachineLayoutInfo();
            return;
        }
        applyLayoutChange(() -> {
            inKey = !inKey;
        });
        showState("Layout");
    }

    private void adjustLayout(final int amount) {
        if (amount == 0) {
            return;
        }
        if (isHarmonicLiveMode()) {
            if ((amount > 0 && !harmonicBassColumns) || (amount < 0 && harmonicBassColumns)) {
                toggleLayout();
            }
            return;
        }
        if (isDrumMachineLiveMode()) {
            adjustDrumMachineLayout(amount);
            return;
        }
        if (amount > 0 && !inKey) {
            toggleLayout();
            return;
        }
        if (amount < 0 && inKey) {
            toggleLayout();
        }
    }

    private void adjustDrumMachineLayout(final int amount) {
        if (amount == 0) {
            return;
        }
        final DrumMachinePadLayout.Layout[] layouts = DrumMachinePadLayout.Layout.values();
        final int nextIndex = Math.max(0, Math.min(layouts.length - 1, drumMachineLayout.ordinal() + amount));
        if (nextIndex == drumMachineLayout.ordinal()) {
            showDrumMachineLayoutInfo();
            return;
        }
        retuneLivePads(() -> drumMachineLayout = layouts[nextIndex]);
        showDrumMachineLayoutInfo();
    }

    public void toggleSurfaceVariant() {
        if (isChordStepSurface()) {
            if (currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
                toggleBuilderLayout();
            } else {
                driver.notifyAction("Step Mode", currentStepSubMode.displayName());
            }
            return;
        }
        cycleNoteSubMode();
    }

    public void toggleLiveLayoutShortcut() {
        if (isChordStepSurface()) {
            return;
        }
        toggleLayout();
    }

    private void toggleBuilderLayout() {
        chordBuilder.toggleLayout();
        oled.valueInfo("Builder Layout", chordBuilder.layoutDisplayName());
        driver.notifyPopup("Builder Layout", chordBuilder.layoutDisplayName());
    }

    public BiColorLightState getModeButtonLightState() {
        return isChordStepSurface() ? BiColorLightState.AMBER_FULL : BiColorLightState.RED_FULL;
    }

    public boolean isChordStepSurface() {
        return false;
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

    private void scrollDrumMachineWindow(final int amount) {
        if (amount == 0) {
            return;
        }
        final int nextPosition = Math.max(0,
                Math.min(MAX_DRUM_MACHINE_SCROLL_POSITION, drumMachineScrollPosition + amount));
        if (nextPosition == drumMachineScrollPosition) {
            showDrumMachineWindowInfo();
            return;
        }
        drumMachineScrollPosition = nextPosition;
        liveDrumPadBank.scrollPosition().set(nextPosition);
        showDrumMachineWindowInfo();
    }

    private void showDrumMachineWindowInfo() {
        oled.valueInfo("Pad Low", formatDrumMachinePadWindowLowNote());
    }

    private String formatDrumMachinePadWindowLowNote() {
        return "P%d %s".formatted(relativeDrumMachinePadPage(), formatMidiNoteName(drumMachineScrollPosition));
    }

    private int relativeDrumMachinePadPage() {
        return Math.floorDiv(drumMachineScrollPosition - DEFAULT_DRUM_MACHINE_LOW_NOTE,
                DRUM_MACHINE_SCROLL_COARSE_STEPS) + 1;
    }

    private static String formatMidiNoteName(final int midiNote) {
        final int octave = midiNote / 12 - 2;
        return NoteGridLayout.noteName(midiNote) + octave;
    }

    private void showDrumMachineLayoutInfo() {
        oled.valueInfo("Drum Layout", drumMachineLayout.displayName());
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

    private void releaseHeldLiveNotes() {
        livePadPerformer.releaseHeldNotes();
        stopAuditionNotes();
        clearHeldBongoPads();
    }

    private void clearHeldBongoPads() {
        for (int zone = 0; zone < heldBongoPads.length; zone++) {
            Arrays.fill(heldBongoPads[zone], false);
            heldBongoPadCounts[zone] = 0;
        }
    }

    private void retuneLivePads(final Runnable stateChange) {
        livePadPerformer.retuneHeldPads(() -> {
            stateChange.run();
            applyLayout();
        }, liveVelocity);
    }

    private int[] getLivePadMidiNotes(final int padIndex) {
        final LiveNoteLayout layout = createLayout();
        if (layout instanceof DrumMachinePadLayout drumMachinePadLayout) {
            if (!liveDrumMachineDevice.hasDrumPads().get()) {
                return new int[0];
            }
            final int bankIndex = drumMachinePadLayout.padBankIndexForPad(padIndex);
            if (bankIndex < 0 || bankIndex >= drumMachinePadExists.length || !drumMachinePadExists[bankIndex]) {
                return new int[0];
            }
        }
        final int[] layoutNotes = layout.notesForPad(padIndex);
        final int[] shifted = new int[layoutNotes.length];
        int count = 0;
        for (final int layoutNote : layoutNotes) {
            final int shiftedNote = applyLivePitchOffset(layoutNote);
            if (shiftedNote >= 0) {
                shifted[count++] = shiftedNote;
            }
        }
        if (count == shifted.length) {
            return shifted;
        }
        final int[] compact = new int[count];
        System.arraycopy(shifted, 0, compact, 0, count);
        return compact;
    }

    private int resolveLivePadVelocity(final int padIndex, final int configuredVelocity, final int rawVelocity) {
        if (isDrumMachineLiveMode() && drumMachineLayout == DrumMachinePadLayout.Layout.VELOCITY) {
            return drumMachineFixedVelocityForPad(padIndex);
        }
        return LiveVelocityLogic.resolveVelocity(configuredVelocity, liveVelocitySensitivity, rawVelocity);
    }

    private int resolveLivePadTimbre(final int padIndex) {
        if (!isDrumMachineLiveMode() || drumMachineLayout != DrumMachinePadLayout.Layout.BONGOS
                || bongoZoneForPad(padIndex) < 0) {
            return -1;
        }
        final int column = padIndex % NoteGridLayout.PAD_COLUMNS;
        final int rowFromBottom = NoteGridLayout.PAD_ROWS - 1 - (padIndex / NoteGridLayout.PAD_COLUMNS);
        final int surfaceStartColumn = column >= 11 ? 11 : 5;
        final double x = column - surfaceStartColumn;
        final double y = rowFromBottom;
        final double dx = Math.abs(x - 2.0);
        final double dy = Math.abs(y - 1.5);
        final double distance = Math.sqrt(dx * dx + dy * dy);
        return Math.max(MIN_MIDI_VALUE, Math.min(MAX_MIDI_VALUE, (int) Math.round(distance / 2.5 * MAX_MIDI_VALUE)));
    }

    private int drumMachineFixedVelocityForPad(final int padIndex) {
        final int column = padIndex % NoteGridLayout.PAD_COLUMNS;
        final int rowFromTop = padIndex / NoteGridLayout.PAD_COLUMNS;
        final int rowFromBottom = NoteGridLayout.PAD_ROWS - 1 - rowFromTop;
        if (column < 4) {
            return liveVelocity;
        }
        final int columnTier = Math.min(3, (column - 4) / 3);
        return Math.max(MIN_VELOCITY, Math.min(MAX_MIDI_VALUE, 36 + rowFromBottom * 22 + columnTier * 8));
    }

    private void showLiveVelocityInfo() {
        if (driver.isGlobalShiftHeld()) {
            oled.valueInfo("Default Velocity", Integer.toString(liveVelocity));
            return;
        }
        oled.valueInfo("Velocity", "Sens %d%% / Def %d".formatted(liveVelocitySensitivity, liveVelocity));
    }

    private RgbLigthState getPadLight(final int padIndex) {
        if (!noteStepActive) {
            return getLivePadLight(padIndex);
        }
        if (currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            return getChordStepPadLight(padIndex);
        }
        return getClipStepRecordPadLight(padIndex);
    }

    private RgbLigthState getLivePadLight(final int padIndex) {
        final LiveNoteLayout layout = createLayout();
        if (isDrumMachineLiveMode() && layout instanceof DrumMachinePadLayout drumMachinePadLayout) {
            final RgbLigthState base = getDrumMachinePadBaseLight(padIndex, drumMachinePadLayout);
            return livePadPerformer.isPadHeld(padIndex) ? base.getBrightest() : base;
        }
        final int midiNote = applyLivePitchOffset(layout.primaryNoteForPad(padIndex));
        final RgbLigthState base;
        if (midiNote < 0) {
            base = RgbLigthState.OFF;
        } else if (isHarmonicLiveMode()) {
            base = getHarmonicLivePadBaseLight(padIndex, layout);
        } else {
            final NoteGridLayout.PadRole role = layout.roleForPad(padIndex);
            final RgbLigthState melodicFamilyColor = harmonicScaleFamilyColor();
            base = switch (role) {
                case ROOT -> ROOT_COLOR;
                case IN_SCALE -> melodicFamilyColor;
                case OUT_OF_SCALE -> OUT_OF_SCALE_COLOR;
                case UNAVAILABLE -> RgbLigthState.OFF;
            };
        }
        return livePadPerformer.isPadHeld(padIndex) ? base.getBrightest() : base;
    }

    private RgbLigthState getDrumMachinePadBaseLight(final int padIndex, final DrumMachinePadLayout layout) {
        if (!liveDrumMachineDevice.hasDrumPads().get()) {
            return RgbLigthState.OFF;
        }
        final int bankIndex = layout.padBankIndexForPad(padIndex);
        if (bankIndex < 0 || bankIndex >= drumMachinePadColors.length || !drumMachinePadExists[bankIndex]) {
            return RgbLigthState.OFF;
        }
        final RgbLigthState color = drumMachinePadColors[bankIndex];
        final RgbLigthState base = color != null ? color : trackPadColor();
        if (layout.selectorOffsetForPad(padIndex) == selectedDrumPadOffset) {
            return base.getBrightest();
        }
        if (drumMachineLayout == DrumMachinePadLayout.Layout.VELOCITY && layout.selectorOffsetForPad(padIndex) < 0) {
            return velocityRampColor(base, drumMachineFixedVelocityForPad(padIndex));
        }
        return base;
    }

    private RgbLigthState trackPadColor() {
        return chordStepBaseColor != null ? chordStepBaseColor : IN_SCALE_COLOR;
    }

    private RgbLigthState velocityRampColor(final RgbLigthState base, final int velocity) {
        if (velocity >= 112) {
            return base.getBrightest();
        }
        if (velocity >= 88) {
            return base.getBrightend();
        }
        if (velocity >= 64) {
            return base;
        }
        if (velocity >= 40) {
            return base.getSoftDimmed();
        }
        return base.getDimmed();
    }

    private RgbLigthState getHarmonicLivePadBaseLight(final int padIndex, final LiveNoteLayout layout) {
        final NoteGridLayout.PadRole role = layout.roleForPad(padIndex);
        final boolean bassColumnPad = harmonicBassColumns && (padIndex % NoteGridLayout.PAD_COLUMNS) < 2;
        if (role == NoteGridLayout.PadRole.UNAVAILABLE) {
            return RgbLigthState.OFF;
        }
        if (role == NoteGridLayout.PadRole.ROOT) {
            final int primaryMidiNote = applyLivePitchOffset(layout.primaryNoteForPad(padIndex));
            final RgbLigthState rootBase = bassColumnPad ? ROOT_COLOR.getSoftDimmed() : ROOT_COLOR;
            return livePadPerformer.isMidiNoteSounding(primaryMidiNote) ? rootBase.getBrightend() : rootBase;
        }
        final RgbLigthState familyColor = harmonicScaleFamilyColor();
        final RgbLigthState padBase = bassColumnPad ? familyColor.getSoftDimmed() : familyColor;
        final int primaryMidiNote = applyLivePitchOffset(layout.primaryNoteForPad(padIndex));
        if (livePadPerformer.isMidiNoteSounding(primaryMidiNote)) {
            return padBase.getBrightend();
        }
        return padBase;
    }

    private RgbLigthState harmonicScaleFamilyColor() {
        final String scaleName = getScale().getName().toLowerCase();
        if (scaleName.contains("ion")
                || scaleName.contains("major")
                || scaleName.contains("lydian")
                || scaleName.contains("mixolyd")) {
            return HARMONIC_BRIGHT_COLOR;
        }
        if (scaleName.contains("dorian")
                || scaleName.contains("aeolian")
                || scaleName.contains("minor")
                || scaleName.contains("melodic minor")
                || scaleName.contains("blues")) {
            return HARMONIC_MINOR_COLOR;
        }
        if (scaleName.contains("whole tone")
                || scaleName.contains("chromatic")
                || scaleName.contains("diminished")
                || scaleName.contains("octatonic")
                || scaleName.contains("augmented")) {
            return HARMONIC_SYMMETRIC_COLOR;
        }
        if (scaleName.contains("phryg")
                || scaleName.contains("locrian")
                || scaleName.contains("altered")
                || scaleName.contains("dominant")
                || scaleName.contains("double harmonic")) {
            return HARMONIC_TENSE_COLOR;
        }
        if (scaleName.contains("harmonic")
                || scaleName.contains("hungarian")
                || scaleName.contains("enigmatic")
                || scaleName.contains("persian")
                || scaleName.contains("byzantine")
                || scaleName.contains("oriental")) {
            return HARMONIC_EXOTIC_COLOR;
        }
        return HARMONIC_BRIGHT_COLOR;
    }

    private RgbLigthState getChordStepPadLight(final int padIndex) {
        return chordStepPadLightRenderer.padLight(padIndex, CLIP_ROW_PAD_COUNT, CHORD_SOURCE_PAD_OFFSET,
                STEP_PAD_OFFSET);
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
        if (!StepPadLightHelper.isStepWithinVisibleLoop(stepIndex, chordStepPosition.getAvailableSteps())) {
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
        final String liveModeDetail;
        if (isHarmonicLiveMode()) {
            liveModeDetail = "Harmonic %s".formatted(harmonicBassColumns ? "Bass" : "Full");
        } else if (isDrumMachineLiveMode()) {
            liveModeDetail = drumMachineLayout.displayName();
        } else {
            liveModeDetail = inKey ? "In Key" : "Chromatic";
        }
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

    private LiveNoteLayout createLayout() {
        if (isHarmonicLiveMode()) {
            return new HarmonicLatticeLayout(getScale(), getRootNote(), getOctave(),
                    HarmonicLatticeLayout.NOTE_COUNTS[harmonicNoteCountIndex],
                    harmonicOctaveSpan,
                    harmonicBassColumns, getHarmonicGlissStepOffset());
        }
        if (isDrumMachineLiveMode()) {
            return new DrumMachinePadLayout(drumMachineScrollPosition, drumMachineLayout,
                    selectedDrumPadOffset, secondaryDrumPadOffset);
        }
        return new NoteGridLayout(getScale(), getRootNote(), getOctave(), inKey);
    }

    private int getBuilderFirstVisibleMidiNote() {
        final int firstVisible = applyLivePitchOffset(getChordRootMidi());
        return firstVisible >= 0 && firstVisible <= 127 ? firstVisible : -1;
    }

    private int applyLivePitchOffset(final int midiNote) {
        if (midiNote < 0) {
            return -1;
        }
        if (isHarmonicLiveMode() || isDrumMachineLiveMode()) {
            return midiNote;
        }
        if (livePitchGlissMode == LivePitchGlissMode.SCALE_DEGREE) {
            return pitchContext.transposeByScaleDegrees(midiNote, liveScaleDegreeGlissOffset);
        }
        final int shifted = midiNote + getLivePitchOffset();
        return shifted >= 0 && shifted <= 127 ? shifted : -1;
    }

    private void clearTranslation() {
        livePadPerformer.releaseHeldNotes();
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
            if (!driver.isGlobalAltHeld()) {
                pageChordSteps(-1);
            }
            return;
        }
        adjustOctave(1);
    }

    private void handlePatternDown(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            if (!driver.isGlobalAltHeld()) {
                pageChordSteps(1);
            }
            return;
        }
        adjustOctave(-1);
    }

    private BiColorLightState getPatternUpLight() {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            return chordStepPosition.canScrollLeft().get() ? BiColorLightState.GREEN_HALF : BiColorLightState.OFF;
        }
        return BiColorLightState.GREEN_HALF;
    }

    private BiColorLightState getPatternDownLight() {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            return chordStepPosition.canScrollRight().get() ? BiColorLightState.GREEN_HALF : BiColorLightState.OFF;
        }
        return BiColorLightState.GREEN_HALF;
    }

    @Override
    public boolean isSelectHeld() {
        return chordStepController.isSelectHeld();
    }

    @Override
    public CursorRemoteControlsPage getActiveRemoteControlsPage() {
        return liveRemoteControlsPage;
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
        return chordStepPosition.getStepOffset();
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
        return noteClipSlotBank;
    }

    @Override
    public PinnableCursorClip getClipCursor() {
        return noteStepClip;
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
        SelectedClipSlotObserver.observe(noteClipSlotBank, true, true, this::refreshSelectedNoteClipState);
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
        noteStepActive = isChordStepSurface();
        currentStepSubMode = NoteStepSubMode.CHORD_STEP;
        chordSelection.resetToBuilder();
        chordStepPadSurface.clearStepTracking();
        selectedPresetStepIndex = null;
        chordStepPosition.setPage(0);
        patternButtons.setUpCallback(this::handlePatternUp, this::getPatternUpLight);
        patternButtons.setDownCallback(this::handlePatternDown, this::getPatternDownLight);
        if (isChordStepSurface()) {
            notePlayController.deactivate(this::releaseHeldLiveNotes);
            liveModeControlLayer.deactivate();
            stepEncoderLayer.deactivate();
            enterCurrentStepSubMode();
        } else {
            notePlayController.activate();
            liveModeControlLayer.activate();
            stepEncoderLayer.deactivate();
            if (drumPadsOnly && !drumMachineDefaultPageApplied) {
                drumMachineDefaultPageApplied = true;
                if (drumMachineScrollPosition == 0) {
                    drumMachineScrollPosition = DEFAULT_DRUM_MACHINE_LOW_NOTE;
                    liveDrumPadBank.scrollPosition().set(DEFAULT_DRUM_MACHINE_LOW_NOTE);
                }
            }
            applyLiveVelocity();
            applyLayout();
            showState("Mode");
        }
    }

    @Override
    protected void onDeactivate() {
        patternButtons.setUpCallback(pressed -> { }, () -> BiColorLightState.OFF);
        patternButtons.setDownCallback(pressed -> { }, () -> BiColorLightState.OFF);
        noteStepActive = false;
        clearHeldBongoPads();
        notePlayController.deactivate(this::releaseHeldLiveNotes);
        chordSelection.resetToBuilder();
        chordStepPadSurface.clearStepTracking();
        selectedPresetStepIndex = null;
        liveModeControlLayer.deactivate();
        stepEncoderLayer.deactivate();
        clearTranslation();
    }

    private static NoteLiveEncoderModeControls.LayerHandle liveEncoderLayer(final Layer layer) {
        return new NoteLiveEncoderModeControls.LayerHandle() {
            @Override
            public void activate() {
                layer.activate();
            }

            @Override
            public void deactivate() {
                layer.deactivate();
            }
        };
    }
}
