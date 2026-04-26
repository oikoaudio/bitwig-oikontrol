package com.oikoaudio.fire.note;

import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteOccurrence;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.MusicalScale;
import com.bitwig.extensions.framework.MusicalScaleLibrary;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.Midi;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.chordstep.ChordStepClipController;
import com.oikoaudio.fire.chordstep.ChordStepController;
import com.oikoaudio.fire.chordstep.ChordStepEditControls;
import com.oikoaudio.fire.chordstep.ChordStepObservationController;
import com.oikoaudio.fire.chordstep.ChordStepObservedState;
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
import com.oikoaudio.fire.sequence.EncoderBank;
import com.oikoaudio.fire.sequence.EncoderBankLayout;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.sequence.EncoderSlotBinding;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;
import com.oikoaudio.fire.sequence.NoteStepAccess;
import com.oikoaudio.fire.sequence.NoteClipAvailability;
import com.oikoaudio.fire.sequence.NoteClipCursorRefresher;
import com.oikoaudio.fire.sequence.SelectedClipSlotObserver;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;
import com.oikoaudio.fire.sequence.ClipSlotSelectionResolver;
import com.oikoaudio.fire.sequence.ClipRowHandler;
import com.oikoaudio.fire.sequence.SeqClipRowHost;
import com.oikoaudio.fire.sequence.AccentLatchState;
import com.oikoaudio.fire.sequence.StepSequencerEncoderHandler;
import com.oikoaudio.fire.sequence.StepPadLightHelper;
import com.oikoaudio.fire.sequence.StepSequencerHost;
import com.oikoaudio.fire.utils.PatternButtons;
import com.oikoaudio.fire.values.StepViewPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class PitchedSurfaceLayer extends Layer implements StepSequencerHost, SeqClipRowHost {
    protected enum SurfaceRole {
        NOTE_PLAY,
        CHORD_STEP
    }
    private static final int CLIP_ROW_PAD_COUNT = 16;
    private static final int CHORD_SOURCE_PAD_OFFSET = 16;
    private static final int CHORD_SOURCE_PAD_COUNT = 16;
    private static final int BUILDER_FAMILY_INDEX = 0;
    private static final String BUILDER_FAMILY_LABEL = "Builder";
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
    private static final int MIN_CHORD_OCTAVE_OFFSET = -3;
    private static final int MAX_CHORD_OCTAVE_OFFSET = 3;
    private static final int MIN_MIDI_VALUE = 0;
    private static final int MAX_MIDI_VALUE = 127;
    private static final int MIN_VELOCITY = 1;
    private static final int DEFAULT_LIVE_VELOCITY = 100;
    private static final int DEFAULT_CHORD_STANDARD_VELOCITY = 100;
    private static final int DEFAULT_CHORD_ACCENTED_VELOCITY = 127;
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
    private static final RgbLigthState SELECTED_CHORD = new RgbLigthState(110, 24, 118, true);
    private static final RgbLigthState SELECTED_BUILDER_NOTE = new RgbLigthState(88, 18, 127, true);
    private static final RgbLigthState DEFERRED_TOP = new RgbLigthState(110, 38, 0, true);
    private static final RgbLigthState DEFERRED_BOTTOM = new RgbLigthState(36, 16, 0, true);

    private final AkaiFireOikontrolExtension driver;
    private final SurfaceRole surfaceRole;
    private final OledDisplay oled;
    private final NoteInput noteInput;
    private final PatternButtons patternButtons;
    private final NoteRepeatHandler noteRepeatHandler;
    private final MusicalScaleLibrary scaleLibrary = MusicalScaleLibrary.getInstance();
    private final Integer[] noteTranslationTable = new Integer[128];
    private final Set<Integer> heldStepPads = new HashSet<>();
    private final Set<Integer> addedStepPads = new HashSet<>();
    private final Set<Integer> modifiedStepPads = new HashSet<>();
    private final Set<Integer> auditioningNotes = new HashSet<>();
    private final Set<Integer> builderSelectedNotes = new HashSet<>();
    private final Set<Integer> modifierHandledStepPads = new HashSet<>();
    private final Map<Integer, Set<Integer>> clipNotesByStep = new HashMap<>();
    private final ChordStepObservedState observedChordStepState = new ChordStepObservedState();
    private final Map<Integer, Map<Integer, Integer>> heldBankFineStarts = new HashMap<>();
    private final Map<Integer, ChordEvent> heldBankChordEvents = new HashMap<>();
    private final Set<Integer> shiftBankTargetSteps = new HashSet<>();
    private final Map<Integer, Map<Integer, Integer>> shiftBankFineStarts = new HashMap<>();
    private final Map<Integer, ChordEvent> shiftBankChordEvents = new HashMap<>();
    private final Set<Integer> pendingBankTargetSteps = new HashSet<>();
    private final Map<Integer, Map<Integer, Integer>> pendingBankFineStarts = new HashMap<>();
    private final Map<Integer, ChordEvent> pendingBankChordEvents = new HashMap<>();
    private final ChordBank chordBank = new ChordBank();
    private final ChordStepClipController chordStepClipController;
    private final ChordStepObservationController chordStepObservationController;
    private final NotePlayController notePlayController;
    private final ChordStepController chordStepController;
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
    private final Map<Integer, Map<Integer, NoteStep>> noteStepsByPosition = new HashMap<>();
    private final Map<String, NoteStepMoveSnapshot> pendingMovedNotes = new HashMap<>();

    private enum LiveNoteSubMode {
        MELODIC("Note"),
        HARMONIC("Harmonic");

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
    private final AccentLatchState chordAccentState = new AccentLatchState();
    private boolean mainEncoderPressConsumed = false;
    private boolean builderInKey = true;
    private LiveNoteSubMode liveNoteSubMode = LiveNoteSubMode.MELODIC;
    private NoteStepSubMode currentStepSubMode = NoteStepSubMode.CHORD_STEP;
    private ChordInterpretation chordInterpretation = ChordInterpretation.AS_IS;
    private int selectedChordFamily = 0;
    private int chordPage = 0;
    private int selectedChordSlot = 0;
    private Integer selectedPresetStepIndex = null;
    private int chordOctaveOffset = 0;
    private Integer heldStepAnchor = null;
    private int liveVelocity = DEFAULT_LIVE_VELOCITY;
    private int liveVelocitySensitivity = 100;
    private int chordVelocitySensitivity = 100;
    private int defaultChordVelocity = DEFAULT_CHORD_STANDARD_VELOCITY;
    private int playingStep = -1;
    private int pendingBankDir = 0;
    private int livePitchOffsetIndex = DEFAULT_LIVE_PITCH_OFFSET_INDEX;
    private int liveScaleDegreeGlissOffset = 0;
    private LivePitchGlissMode livePitchGlissMode = LivePitchGlissMode.FIFTH_OCTAVE;
    private int harmonicNoteCountIndex = 2;
    private int harmonicOctaveSpan = 1;
    private boolean harmonicBassColumns = true;
    private int livePitchBend = DEFAULT_LIVE_PITCH_BEND;
    private boolean livePitchBendTouched = false;
    private int livePitchBendReturnGeneration = 0;
    private int livePitchBendInactivityGeneration = 0;
    private boolean pendingBankFineMove = false;
    private boolean pendingBankLengthAdjust = false;
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

    private enum ChordInterpretation {
        AS_IS("As Is"),
        IN_SCALE("In Scale");

        private final String displayName;

        ChordInterpretation(final String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        public ChordInterpretation next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    protected PitchedSurfaceLayer(final AkaiFireOikontrolExtension driver,
                                  final NoteRepeatHandler noteRepeatHandler,
                                  final String layerName,
                                  final SurfaceRole surfaceRole) {
        super(driver.getLayers(), layerName);
        this.driver = driver;
        this.surfaceRole = surfaceRole;
        this.oled = driver.getOled();
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
        this.liveRemoteControlsPage = liveCursorDevice.createCursorRemoteControlsPage(8);
        this.noteStepClip = cursorTrack.createLauncherCursorClip("NOTE_STEP", "NOTE_STEP", STEP_COUNT, 128);
        this.observedNoteClip = host.createLauncherCursorClip(OBSERVED_FINE_STEP_CAPACITY, 128);
        this.chordStepPosition = new StepViewPosition(noteStepClip, STEP_COUNT, "CHORD");
        this.noteClipSlotBank = cursorTrack.clipLauncherSlotBank();
        this.noteClipSlotBank.cursorIndex().markInterested();
        this.noteStepClip.scrollToKey(0);
        this.observedNoteClip.scrollToKey(0);
        this.observedNoteClip.setStepSize(FINE_STEP_LENGTH);
        this.noteStepClip.addStepDataObserver(this::handleStepData);
        this.noteStepClip.addNoteStepObserver(this::handleNoteStepObject);
        this.observedNoteClip.addStepDataObserver(this::handleObservedStepData);
        this.noteStepClip.playingStep().addValueObserver(this::handlePlayingStep);
        this.chordStepClipController = new ChordStepClipController(
                () -> cursorTrack.canHoldNoteData().get(),
                this::hasLoadedNoteClipContent,
                this::queueChordObservationResync,
                this::showClipAvailabilityFailure);
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
                },
                this::getLivePadMidiNotes,
                (configuredVelocity, rawVelocity) -> resolveLivePadVelocity(configuredVelocity, rawVelocity));
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

    private void applyDefaultLayoutPreference() {
        inKey = false;
        if (driver.getSharedScaleIndex() < 1) {
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
                    liveControls.markEncoderAdjusted(0);
                    adjustLiveModulation(inc);
                });
        encoders[0].bindTouched(liveChannelLayer, touched -> liveControls.handleResettableTouch(0, touched,
                () -> oled.valueInfo("Mod", Integer.toString(liveExpressionControls.modulation())),
                this::resetLiveModulation));

        encoders[1].bindContinuousEncoder(liveChannelLayer, driver::isGlobalShiftHeld,
                com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile.SOFT, inc -> {
                    if (inc == 0) {
                        return;
                    }
                    liveControls.markEncoderAdjusted(1);
                    cancelLivePitchBendReturn();
                    adjustLivePitchBend(inc);
                });
        encoders[1].bindTouched(liveChannelLayer, this::handleLivePitchBendTouch);

        encoders[2].bindEncoder(liveChannelLayer, this::handleLivePitchGlissEncoder);
        encoders[2].bindTouched(liveChannelLayer, touched -> liveControls.handleResettableTouch(2, touched,
                () -> oled.valueInfo(driver.isGlobalAltHeld() ? "Gliss Mode" : "Pitch Gliss",
                        driver.isGlobalAltHeld() ? livePitchGlissMode.displayName() : formatLivePitchOffsetDisplay()),
                () -> {
                    if (!driver.isGlobalAltHeld()) {
                        livePitchOffsetEncoder.reset();
                    }
                }));

        encoders[3].bindEncoder(liveChannelLayer, this::handleEncoder1);
        encoders[3].bindTouched(liveChannelLayer, touched -> liveControls.handleResettableTouch(3, touched,
                () -> showState(driver.isGlobalShiftHeld() ? "Layout" : driver.isGlobalAltHeld() ? "Root" : "Scale"),
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

    private void handleEncoder1(final int inc) {
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
            if (driver.isGlobalShiftHeld() || chordAccentState.isHeld()) {
                handleChordAccentPressed(pressed);
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
            notePlayController.handlePadPress(padIndex, pressed, velocity, liveVelocity);
            return;
        }
        if (currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            handleChordStepPadPress(padIndex, pressed, velocity);
            return;
        }
        handleClipStepRecordPadPress(padIndex, pressed);
    }

    private void handleChordStepPadPress(final int padIndex, final boolean pressed, final int velocity) {
        if (padIndex < CLIP_ROW_PAD_COUNT) {
            clipHandler.handlePadPress(padIndex, pressed);
            return;
        }
        if (padIndex < STEP_PAD_OFFSET) {
            final int sourcePadIndex = padIndex - CHORD_SOURCE_PAD_OFFSET;
            if (isBuilderFamily()) {
                handleBuilderSourcePadPress(sourcePadIndex, pressed);
                return;
            }
            if (!chordBank.hasSlot(currentPresetFamilyIndex(), chordPage, sourcePadIndex)) {
                return;
            }
            if (pressed) {
                selectedChordSlot = sourcePadIndex;
                final boolean hasHeldSteps = !heldStepPads.isEmpty();
                final boolean auditionEnabled = driver.isStepSeqPadAuditionEnabled();
                final boolean transportStopped = !driver.isTransportPlaying();
                if (auditionEnabled && (!hasHeldSteps || transportStopped)) {
                    startAuditionSelectedChord(currentChordVelocity(velocity));
                }
                if (hasHeldSteps) {
                    assignSelectedChordToHeldSteps(velocity);
                } else if (!auditionEnabled) {
                    showCurrentChord();
                }
            } else {
                stopAuditionNotes();
            }
            return;
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
        final boolean accentGesture = chordAccentState.isHeld() || chordAccentState.isActive();
        if (pressed && !ensureSelectedNoteClipSlot()) {
            return;
        }
        if (accentGesture) {
            if (pressed) {
                toggleChordAccentForStep(stepIndex);
            }
            return;
        }
        if (!pressed && modifierHandledStepPads.remove(stepIndex)) {
            return;
        }
        if (pressed) {
            if (chordStepController.isSelectHeld()) {
                if (isBuilderFamily()) {
                    loadBuilderFromStep(stepIndex);
                } else {
                    selectPresetStep(stepIndex);
                }
                modifierHandledStepPads.add(stepIndex);
                return;
            }
            if (chordStepController.isFixedLengthHeld()) {
                setLastStep(stepIndex);
                modifierHandledStepPads.add(stepIndex);
                return;
            }
            if (chordStepController.isCopyHeld()) {
                pasteCurrentChordToStep(stepIndex);
                modifierHandledStepPads.add(stepIndex);
                return;
            }
            if (chordStepController.isDeleteHeld()) {
                clearChordStep(stepIndex);
                modifierHandledStepPads.add(stepIndex);
                return;
            }
            if (heldStepAnchor != null
                    && heldStepAnchor != stepIndex
                    && heldStepPads.contains(heldStepAnchor)
                    && canExtendHeldChordRange(heldStepAnchor, stepIndex)
                    && extendHeldChordRange(heldStepAnchor, stepIndex)) {
                heldStepPads.add(stepIndex);
                modifiedStepPads.add(heldStepAnchor);
                modifiedStepPads.add(stepIndex);
                showExtendedStepInfo(heldStepAnchor, stepIndex);
                return;
            }
            if (heldStepAnchor != null
                    && heldStepAnchor != stepIndex
                    && heldStepPads.contains(heldStepAnchor)
                    && !canExtendHeldChordRange(heldStepAnchor, stepIndex)) {
                showBlockedStepInfo();
                return;
            }
            heldStepPads.add(stepIndex);
            if (heldStepAnchor == null) {
                heldStepAnchor = stepIndex;
            }
            if (!hasStepStartNote(stepIndex)) {
                final boolean assigned = assignSelectedChordToSteps(Collections.singleton(stepIndex), velocity);
                if (!assigned) {
                    heldStepPads.remove(stepIndex);
                    refreshHeldStepAnchor(stepIndex);
                    return;
                }
                addedStepPads.add(stepIndex);
            } else if (isBuilderFamily()) {
                loadBuilderFromStep(stepIndex);
            }
            showHeldStepInfo(stepIndex);
        } else {
            heldStepPads.remove(stepIndex);
            heldBankFineStarts.remove(stepIndex);
            refreshHeldStepAnchor(stepIndex);
            if (modifiedStepPads.contains(stepIndex)) {
                modifiedStepPads.remove(stepIndex);
            } else if (addedStepPads.contains(stepIndex)) {
                addedStepPads.remove(stepIndex);
            } else if (hasStepStartNote(stepIndex)) {
                clearChordStep(stepIndex);
            }
        }
    }

    private void handleBuilderSourcePadPress(final int padIndex, final boolean pressed) {
        if (!pressed) {
            stopAuditionNotes();
            return;
        }
        toggleBuilderNoteOffset(padIndex);
        applyBuilderToHeldSteps();
        final boolean auditionEnabled = driver.isStepSeqPadAuditionEnabled();
        if (auditionEnabled) {
            startAuditionSelectedChord();
        } else {
            showCurrentChord();
        }
    }

    private void loadBuilderFromStep(final int stepIndex) {
        final Map<Integer, NoteStep> notesAtStep = noteStepsByPosition.get(stepIndex);
        final Set<Integer> fallbackNotes = bestAvailableStepNotes(stepIndex);
        if ((notesAtStep == null || notesAtStep.isEmpty()) && fallbackNotes.isEmpty()) {
            oled.valueInfo("Select", "Empty step");
            return;
        }
        selectedChordFamily = BUILDER_FAMILY_INDEX;
        chordPage = 0;
        selectedChordSlot = 0;
        builderSelectedNotes.clear();
        int loaded = 0;
        if (notesAtStep != null && !notesAtStep.isEmpty()) {
            for (final NoteStep note : notesAtStep.values()) {
                if (note.state() != NoteStep.State.NoteOn) {
                    continue;
                }
                builderSelectedNotes.add(note.y());
                loaded++;
            }
        } else {
            for (final int midiNote : fallbackNotes) {
                builderSelectedNotes.add(midiNote);
                loaded++;
            }
        }
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
        writeChordAtStep(stepIndex, notes, currentChordVelocity(127), STEP_LENGTH);
        modifiedStepPads.add(stepIndex);
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
        observedChordStepState.handleObservedStepData(x, y, state, FINE_STEPS_PER_STEP);
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
        final int x = noteStep.x();
        final int y = noteStep.y();
        final Map<Integer, NoteStep> notesAtStep = noteStepsByPosition.computeIfAbsent(x, ignored -> new HashMap<>());
        if (noteStep.state() == NoteStep.State.Empty) {
            notesAtStep.remove(y);
            if (notesAtStep.isEmpty()) {
                noteStepsByPosition.remove(x);
            }
            return;
        }
        notesAtStep.put(y, noteStep);
        final NoteStepMoveSnapshot pending = pendingMovedNotes.remove(moveKey(x, y));
        if (pending != null) {
            applySnapshot(noteStep, pending);
        }
    }

    private void handlePlayingStep(final int playingStep) {
        final int localPlayingStep = playingStep - chordStepOffset();
        if (localPlayingStep < 0 || localPlayingStep >= STEP_COUNT) {
            this.playingStep = -1;
            return;
        }
        this.playingStep = localPlayingStep;
    }

    private String moveKey(final int x, final int y) {
        return x + ":" + y;
    }

    private void applySnapshot(final NoteStep dest, final NoteStepMoveSnapshot snapshot) {
        dest.setChance(snapshot.chance);
        dest.setPressure(snapshot.pressure);
        dest.setTimbre(snapshot.timbre);
        dest.setVelocitySpread(snapshot.velocitySpread);
        dest.setRepeatCount(snapshot.repeatCount);
        dest.setRepeatCurve(snapshot.repeatCurve);
        dest.setRepeatVelocityCurve(snapshot.repeatVelocityCurve);
        dest.setRepeatVelocityEnd(snapshot.repeatVelocityEnd);
        dest.setPan(snapshot.pan);
        dest.setRecurrence(snapshot.recurrenceLength, snapshot.recurrenceMask);
        dest.setOccurrence(snapshot.occurrence);
    }

    private void moveStepContent(final int amount) {
        if (!ensureChordClipWithinObservedCapacity()) {
            return;
        }
        final Set<Integer> selectedSteps = heldStepPads.isEmpty() ? getVisibleStartedSteps() : heldStepPads;
        if (selectedSteps.isEmpty()) {
            return;
        }
        final Map<Integer, ChordEvent> coarseMoveSnapshot = new HashMap<>();
        selectedSteps.forEach(step -> {
            final ChordEvent heldEvent = heldBankChordEvents.get(step);
            if (heldEvent != null) {
                coarseMoveSnapshot.put(step, heldEvent);
                return;
            }
            final ChordEvent shiftEvent = shiftBankChordEvents.get(step);
            if (shiftEvent != null) {
                coarseMoveSnapshot.put(step, shiftEvent);
            }
        });
        if (!heldStepPads.isEmpty()) {
            modifiedStepPads.addAll(heldStepPads);
        }
        final List<ChordEvent> eventsToMove = coarseMoveSnapshot.isEmpty()
                ? chordEventsForSteps(selectedSteps, Map.of(), amount)
                : selectedSteps.stream()
                .map(coarseMoveSnapshot::get)
                .filter(java.util.Objects::nonNull)
                .sorted(amount > 0
                        ? Comparator.comparingInt(ChordEvent::anchorFineStart).reversed()
                        : Comparator.comparingInt(ChordEvent::anchorFineStart))
                .toList();
        if (eventsToMove.isEmpty()) {
            return;
        }
        clearAllBankFineNudgeSessions();
        final int loopSteps = chordLoopSteps();
        final int loopFineSteps = chordLoopFineSteps();
        final List<ChordEventNoteMove> noteMoves = new ArrayList<>();
        boolean wrappedMoveApplied = false;
        for (final ChordEvent event : eventsToMove) {
            final int currentAnchorGlobalStep = Math.floorDiv(event.anchorFineStart(), FINE_STEPS_PER_STEP);
            final int targetAnchorGlobalStep = Math.floorMod(currentAnchorGlobalStep + amount, loopSteps);
            final int targetAnchorFineStart = targetAnchorGlobalStep * FINE_STEPS_PER_STEP;
            if (targetAnchorGlobalStep != currentAnchorGlobalStep + amount) {
                wrappedMoveApplied = true;
            }
            noteMoves.addAll(createNoteMovesForEvent(event, targetAnchorFineStart, loopFineSteps));
        }
        if (noteMoves.isEmpty()) {
            return;
        }
        rewriteChordEventMoves(noteMoves);
        for (final ChordEventNoteMove move : noteMoves) {
            if (heldStepPads.contains(move.localStep())) {
                heldBankFineStarts.computeIfAbsent(move.localStep(), ignored -> new HashMap<>())
                        .put(move.midiNote(), move.targetFineStart());
            }
        }
        if (wrappedMoveApplied) {
            refreshChordStepObservation();
        }
        oled.valueInfo("Move", amount > 0 ? "Right" : "Left");
    }

    private void invertCurrentChord(final int direction) {
        final int[] currentNotes = renderSelectedChord();
        if (currentNotes.length == 0) {
            oled.valueInfo("Invert", "Empty");
            return;
        }
        selectedChordFamily = BUILDER_FAMILY_INDEX;
        chordPage = 0;
        selectedChordSlot = 0;
        final int[] inverted = ChordInversion.rotate(currentNotes, direction);
        builderSelectedNotes.clear();
        for (final int midiNote : inverted) {
            builderSelectedNotes.add(midiNote);
        }
        applyBuilderToHeldSteps();
        oled.valueInfo("Invert", direction > 0 ? "Up" : "Down");
    }

    private void nudgeHeldNotes(final int amount, final Set<Integer> targetSteps,
                                final Map<Integer, ChordEvent> chordEventSnapshot) {
        if (!ensureChordClipWithinObservedCapacity()) {
            return;
        }
        if (targetSteps.isEmpty() || chordEventSnapshot.isEmpty()) {
            return;
        }
        modifiedStepPads.addAll(targetSteps);
        final boolean heldOnly = !heldStepPads.isEmpty();
        final List<ChordEvent> eventsToNudge = targetSteps.stream()
                .map(chordEventSnapshot::get)
                .filter(java.util.Objects::nonNull)
                .sorted(amount > 0
                        ? Comparator.comparingInt(ChordEvent::anchorFineStart).reversed()
                        : Comparator.comparingInt(ChordEvent::anchorFineStart))
                .toList();
        if (eventsToNudge.isEmpty()) {
            return;
        }
        if (heldOnly) {
            heldBankFineStarts.keySet().retainAll(targetSteps);
            targetSteps.forEach(step -> heldBankFineStarts.put(step, new HashMap<>()));
            heldBankChordEvents.keySet().retainAll(targetSteps);
        } else if (driver.isGlobalShiftHeld()) {
            shiftBankTargetSteps.clear();
            shiftBankTargetSteps.addAll(targetSteps);
            shiftBankFineStarts.keySet().retainAll(targetSteps);
            targetSteps.forEach(step -> shiftBankFineStarts.put(step, new HashMap<>()));
            shiftBankChordEvents.keySet().retainAll(targetSteps);
        } else {
            clearShiftBankFineNudgeSession();
        }
        final int loopFineSteps = chordLoopFineSteps();
        final List<ChordEventNoteMove> noteMoves = new ArrayList<>();
        final Map<Integer, ChordEvent> movedEvents = new HashMap<>();
        for (final ChordEvent event : eventsToNudge) {
            final int targetAnchorFineStart = Math.floorMod(event.anchorFineStart() + amount, loopFineSteps);
            noteMoves.addAll(createNoteMovesForEvent(event, targetAnchorFineStart, loopFineSteps));
            movedEvents.put(event.localStep(), moveChordEvent(event, targetAnchorFineStart, loopFineSteps));
        }
        if (noteMoves.isEmpty()) {
            return;
        }
        rewriteChordEventMoves(noteMoves);
        for (final ChordEventNoteMove move : noteMoves) {
            if (heldOnly) {
                heldBankFineStarts.computeIfAbsent(move.localStep(), ignored -> new HashMap<>())
                        .put(move.midiNote(), move.targetFineStart());
            } else if (driver.isGlobalShiftHeld()) {
                shiftBankFineStarts.computeIfAbsent(move.localStep(), ignored -> new HashMap<>())
                        .put(move.midiNote(), move.targetFineStart());
            }
        }
        movedEvents.forEach((step, event) -> {
            if (heldOnly) {
                heldBankChordEvents.put(step, event);
            } else if (driver.isGlobalShiftHeld()) {
                shiftBankChordEvents.put(step, event);
            }
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
        final Set<Integer> occupiedSteps = new HashSet<>(noteStepsByPosition.keySet());
        occupiedSteps.addAll(observedChordStepState.visibleOccupiedSteps(this::isVisibleGlobalStep, this::globalToLocalStep));
        return occupiedSteps;
    }

    private Set<Integer> getVisibleStartedSteps() {
        final Set<Integer> startedSteps =
                observedChordStepState.visibleStartedSteps(this::isVisibleGlobalStep, this::globalToLocalStep);
        noteStepsByPosition.entrySet().stream()
                .filter(entry -> entry.getValue().values().stream().anyMatch(note -> note.state() == NoteStep.State.NoteOn))
                .map(Map.Entry::getKey)
                .forEach(startedSteps::add);
        return startedSteps;
    }

    private Map<Integer, Integer> snapshotFineStartsForStep(final int localStep, final boolean heldOnly) {
        final Map<Integer, Integer> persisted = heldOnly ? heldBankFineStarts.get(localStep) : null;
        if (persisted != null && !persisted.isEmpty()) {
            return new HashMap<>(persisted);
        }
        final Map<Integer, Integer> starts = chordEventsForStep(localStep, Map.of()).stream()
                .flatMap(event -> event.notes().stream())
                .collect(Collectors.toMap(ChordEventNote::midiNote, ChordEventNote::fineStart, (a, b) -> a,
                        HashMap::new));
        if (heldOnly && !starts.isEmpty()) {
            heldBankFineStarts.put(localStep, new HashMap<>(starts));
        }
        return starts;
    }

    private ChordEvent snapshotChordEventForStep(final int localStep, final boolean heldOnly) {
        final ChordEvent persisted = heldOnly ? heldBankChordEvents.get(localStep) : null;
        if (persisted != null) {
            return persisted;
        }
        return chordEventsForStep(localStep, Map.of()).stream().findFirst().orElse(null);
    }

    private void updateObservedFineStart(final int oldFineStart, final int newFineStart, final int midiNote,
                                         final double duration) {
        observedChordStepState.moveFineStart(
                oldFineStart,
                newFineStart,
                midiNote,
                duration,
                FINE_STEPS_PER_STEP,
                FINE_STEP_LENGTH);
    }

    private List<ObservedChordNote> notesForMove(final int localStep) {
        return chordEventsForStep(localStep, Map.of()).stream()
                .flatMap(event -> event.notes().stream()
                        .map(note -> new ObservedChordNote(localStep, Math.floorDiv(note.fineStart(), FINE_STEPS_PER_STEP),
                                note.fineStart(), note.midiNote(), note.velocity(), note.duration(), note.visibleStep())))
                .toList();
    }

    private List<ChordEvent> chordEventsForSteps(final Set<Integer> localSteps,
                                                 final Map<Integer, Map<Integer, Integer>> startOverrides,
                                                 final int amount) {
        return localSteps.stream()
                .distinct()
                .map(step -> chordEventsForStep(step, startOverrides.getOrDefault(step, Map.of())))
                .flatMap(List::stream)
                .sorted(amount > 0
                        ? Comparator.comparingInt(ChordEvent::anchorFineStart).reversed()
                        : Comparator.comparingInt(ChordEvent::anchorFineStart))
                .toList();
    }

    private List<ChordEvent> chordEventsForStep(final int localStep, final Map<Integer, Integer> startOverrides) {
        final Map<Integer, NoteStep> visibleNotes = noteStepsByPosition.get(localStep);
        final int globalStep = localToGlobalStep(localStep);
        final Map<Integer, Integer> noteStarts = new HashMap<>();
        noteStarts.putAll(observedChordStepState.noteStartsForStep(globalStep));
        if (!startOverrides.isEmpty()) {
            noteStarts.putAll(startOverrides);
        }
        if (visibleNotes != null) {
            visibleNotes.values().stream()
                    .filter(note -> note.state() == NoteStep.State.NoteOn)
                    .forEach(note -> noteStarts.putIfAbsent(note.y(), fineStartFor(globalStep, note.y(), note.x())));
        }
        if (noteStarts.isEmpty()) {
            return List.of();
        }
        final int anchorFineStart = noteStarts.values().stream().min(Integer::compareTo).orElse(globalStep * FINE_STEPS_PER_STEP);
        final boolean normalizeToSharedAnchor = noteStarts.size() > 1
                && noteStarts.values().stream().distinct().count() == 1;
        final List<ChordEventNote> notes = noteStarts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> {
                    final int midiNote = entry.getKey();
                    final int fineStart = entry.getValue();
                    final int effectiveFineStart = normalizeToSharedAnchor ? anchorFineStart : fineStart;
                    final NoteStep visibleNote = visibleNotes == null ? null : visibleNotes.get(midiNote);
                    final int velocity = visibleNote == null
                            ? currentChordVelocity(127)
                            : (int) Math.round(visibleNote.velocity() * 127);
                    final double duration = visibleNote != null
                            ? visibleNote.duration()
                            : durationForObservedNote(fineStart, midiNote, STEP_LENGTH);
                    return new ChordEventNote(
                            midiNote,
                            effectiveFineStart,
                            effectiveFineStart - anchorFineStart,
                            velocity,
                            duration,
                            visibleNote);
                })
                .toList();
        return List.of(new ChordEvent(localStep, globalStep, anchorFineStart, notes));
    }

    private int fineStartFor(final int globalStep, final int midiNote, final int fallbackLocalStep) {
        return observedChordStepState.fineStartFor(
                globalStep,
                midiNote,
                localToGlobalStep(fallbackLocalStep) * FINE_STEPS_PER_STEP);
    }

    private double durationForObservedNote(final int fineStart, final int midiNote, final double fallbackDuration) {
        int occupiedFineSteps = 0;
        int currentFineStep = fineStart;
        while (isObservedFineStepOccupied(currentFineStep, midiNote)) {
            occupiedFineSteps++;
            currentFineStep++;
        }
        if (occupiedFineSteps == 0) {
            return fallbackDuration;
        }
        return occupiedFineSteps * FINE_STEP_LENGTH;
    }

    private boolean isObservedFineStepOccupied(final int fineStep, final int midiNote) {
        return observedChordStepState.isFineStepOccupied(fineStep, midiNote, FINE_STEPS_PER_STEP);
    }

    private List<ChordEventNoteMove> createNoteMovesForEvent(final ChordEvent event, final int targetAnchorFineStart,
                                                             final int loopFineSteps) {
        return event.notes().stream()
                .map(note -> new ChordEventNoteMove(
                        event.localStep(),
                        note,
                        Math.floorMod(targetAnchorFineStart + note.startOffset(), loopFineSteps)))
                .filter(move -> move.sourceFineStart() != move.targetFineStart())
                .toList();
    }

    private ChordEvent moveChordEvent(final ChordEvent event, final int targetAnchorFineStart, final int loopFineSteps) {
        final List<ChordEventNote> movedNotes = event.notes().stream()
                .map(note -> new ChordEventNote(
                        note.midiNote(),
                        Math.floorMod(targetAnchorFineStart + note.startOffset(), loopFineSteps),
                        note.startOffset(),
                        note.velocity(),
                        note.duration(),
                        note.visibleStep()))
                .toList();
        return new ChordEvent(
                event.localStep(),
                Math.floorDiv(targetAnchorFineStart, FINE_STEPS_PER_STEP),
                targetAnchorFineStart,
                movedNotes);
    }

    private void rewriteChordEventMoves(final List<ChordEventNoteMove> noteMoves) {
        for (final ChordEventNoteMove move : noteMoves) {
            final int targetGlobalStep = Math.floorDiv(move.targetFineStart(), FINE_STEPS_PER_STEP);
            if (move.visibleStep() != null && isVisibleGlobalStep(targetGlobalStep)) {
                pendingMovedNotes.put(moveKey(globalToLocalStep(targetGlobalStep), move.midiNote()),
                        NoteStepMoveSnapshot.capture(move.visibleStep()));
            }
        }
        for (final ChordEventNoteMove move : noteMoves) {
            observedNoteClip.clearStep(move.sourceFineStart(), move.midiNote());
        }
        for (final ChordEventNoteMove move : noteMoves) {
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
        pendingBankDir = 0;
        pendingBankFineMove = false;
        pendingBankLengthAdjust = false;
        pendingBankTargetSteps.clear();
        pendingBankFineStarts.clear();
        pendingBankChordEvents.clear();
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
            final Map<Integer, NoteStep> anchorNotes = noteStepsByPosition.getOrDefault(anchorStepIndex, Map.of());
            if (!anchorNotes.isEmpty()) {
                anchorNotes.values().forEach(note -> note.setDuration(duration));
                return true;
            }
            if (hasVisibleStepContent(anchorStepIndex) || addedStepPads.contains(anchorStepIndex)) {
                writeSelectedChordAtStep(anchorStepIndex, duration);
                return true;
            }
            return false;
        }

        final Map<Integer, NoteStep> anchorNotes = noteStepsByPosition.getOrDefault(anchorStepIndex, Map.of());
        if (!anchorNotes.isEmpty()) {
            for (final NoteStep note : anchorNotes.values()) {
                pendingMovedNotes.put(moveKey(startStep, note.y()), NoteStepMoveSnapshot.capture(note));
                setChordStep(startStep, note.y(), (int) Math.round(note.velocity() * 127), duration);
                clearChordNote(note.x(), note.y());
            }
            return true;
        }
        if (hasVisibleStepContent(anchorStepIndex) || addedStepPads.contains(anchorStepIndex)) {
            clearChordStep(anchorStepIndex);
            writeSelectedChordAtStep(startStep, duration);
            return true;
        }
        return false;
    }

    private void enterCurrentStepSubMode() {
        releaseHeldLiveNotes();
        heldStepPads.clear();
        addedStepPads.clear();
        modifiedStepPads.clear();
        heldStepAnchor = null;
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
        heldStepPads.clear();
        addedStepPads.clear();
        modifiedStepPads.clear();
        heldStepAnchor = null;
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
        modifiedStepPads.addAll(heldStepPads);
        assignSelectedChordToSteps(heldStepPads, velocity);
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
            writeChordAtStep(stepIndex, notes, appliedVelocity, STEP_LENGTH);
        }
        oled.valueInfo(currentChordFamilyLabel(), currentChordName());
        return true;
    }

    private void writeSelectedChordAtStep(final int stepIndex, final double duration) {
        final int[] notes = renderSelectedChord();
        writeChordAtStep(stepIndex, notes, currentChordVelocity(127), duration);
    }

    private void writeChordAtStep(final int stepIndex, final int[] notes, final int velocity, final double duration) {
        clearChordStep(stepIndex);
        for (final int midiNote : notes) {
            setChordStep(stepIndex, midiNote, velocity, duration);
        }
        invalidateObservedChordStep(stepIndex);
        queueChordObservationResync();
    }

    private void clearChordStep(final int stepIndex) {
        final int fineStart = localToGlobalFineStep(stepIndex);
        for (int offset = 0; offset < FINE_STEPS_PER_STEP; offset++) {
            observedNoteClip.clearStepsAtX(0, fineStart + offset);
        }
    }

    private void setChordStep(final int stepIndex, final int midiNote, final int velocity, final double duration) {
        observedNoteClip.setStep(localToGlobalFineStep(stepIndex), midiNote, velocity, duration);
    }

    private void clearChordNote(final int stepIndex, final int midiNote) {
        final Map<Integer, Integer> observedStarts =
                observedChordStepState.noteStartsForStep(localToGlobalStep(stepIndex));
        final int fineStart = observedStarts.getOrDefault(midiNote, localToGlobalFineStep(stepIndex));
        observedNoteClip.clearStep(fineStart, midiNote);
        invalidateObservedChordStep(stepIndex);
        queueChordObservationResync();
    }

    private void invalidateObservedChordStep(final int stepIndex) {
        final int globalStep = localToGlobalStep(stepIndex);
        observedChordStepState.invalidateStep(globalStep);
        heldBankFineStarts.remove(stepIndex);
        heldBankChordEvents.remove(stepIndex);
        shiftBankFineStarts.remove(stepIndex);
        shiftBankChordEvents.remove(stepIndex);
        pendingBankFineStarts.remove(stepIndex);
        pendingBankChordEvents.remove(stepIndex);
        pendingBankTargetSteps.remove(stepIndex);
    }

    private void applyBuilderToHeldSteps() {
        if (!isBuilderFamily() || heldStepPads.isEmpty()) {
            return;
        }
        modifiedStepPads.addAll(heldStepPads);
        for (final int stepIndex : heldStepPads) {
            writeSelectedChordAtStep(stepIndex, getStepDuration(stepIndex));
        }
    }

    private double getStepDuration(final int stepIndex) {
        return noteStepsByPosition.getOrDefault(stepIndex, Map.of()).values().stream()
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .mapToDouble(NoteStep::duration)
                .max()
                .orElse(STEP_LENGTH);
    }

    private void adjustChordPage(final int amount) {
        if (amount == 0) {
            return;
        }
        if (isBuilderFamily()) {
            return;
        }
        final int nextPage = Math.max(0, Math.min(currentChordPageCount() - 1, chordPage + amount));
        if (nextPage == chordPage) {
            return;
        }
        chordPage = nextPage;
        ensureSelectedChordSlotValid();
        showCurrentChord();
    }

    private void adjustChordFamily(final int amount) {
        if (amount == 0) {
            return;
        }
        final int familyCount = chordFamilyCount();
        final int nextFamily = Math.max(0, Math.min(familyCount - 1, selectedChordFamily + amount));
        if (nextFamily == selectedChordFamily) {
            return;
        }
        selectedChordFamily = nextFamily;
        chordPage = 0;
        selectedChordSlot = 0;
        ensureSelectedChordSlotValid();
        showCurrentChord();
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
        if (amount == 0) {
            return;
        }
        final int nextOffset = Math.max(MIN_CHORD_OCTAVE_OFFSET,
                Math.min(MAX_CHORD_OCTAVE_OFFSET, chordOctaveOffset + amount));
        if (nextOffset == chordOctaveOffset) {
            return;
        }
        chordOctaveOffset = nextOffset;
        showChordOctaveInfo();
    }

    private void resetChordOctave() {
        chordOctaveOffset = 0;
    }

    private void toggleChordInterpretation() {
        chordInterpretation = chordInterpretation == ChordInterpretation.AS_IS
                ? ChordInterpretation.IN_SCALE
                : ChordInterpretation.AS_IS;
        oled.valueInfo("Chord Step Mode", chordInterpretation.displayName());
        driver.notifyPopup("Chord Step Mode", chordInterpretation.displayName());
    }

    private void adjustChordInterpretation(final int amount) {
        if (amount == 0) {
            return;
        }
        if (amount > 0 && chordInterpretation == ChordInterpretation.AS_IS) {
            toggleChordInterpretation();
            return;
        }
        if (amount < 0 && chordInterpretation == ChordInterpretation.IN_SCALE) {
            toggleChordInterpretation();
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
        if (!driver.isGlobalShiftHeld()) {
            clearShiftBankFineNudgeSession();
        }
        if (noteStepActive) {
            if (currentStepSubMode == NoteStepSubMode.CHORD_STEP && driver.isGlobalAltHeld()) {
                if (pressed) {
                    pendingBankLengthAdjust = true;
                    adjustChordClipLength(amount);
                } else if (pendingBankLengthAdjust) {
                    pendingBankLengthAdjust = false;
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
                pendingBankDir = amount;
                pendingBankFineMove = driver.isGlobalShiftHeld() || !heldStepPads.isEmpty();
                pendingBankTargetSteps.clear();
                pendingBankFineStarts.clear();
                pendingBankChordEvents.clear();
                if (pendingBankFineMove) {
                    final boolean heldOnly = !heldStepPads.isEmpty();
                    final Set<Integer> targetSteps = heldOnly
                            ? new HashSet<>(heldStepPads)
                            : shiftBankTargetSteps.isEmpty()
                            ? getVisibleStartedSteps()
                            : new HashSet<>(shiftBankTargetSteps);
                    pendingBankTargetSteps.addAll(targetSteps);
                    for (final int stepIndex : targetSteps) {
                        final ChordEvent event = heldOnly
                                ? snapshotChordEventForStep(stepIndex, true)
                                : shiftBankChordEvents.containsKey(stepIndex)
                                ? shiftBankChordEvents.get(stepIndex)
                                : snapshotChordEventForStep(stepIndex, false);
                        if (event != null) {
                            pendingBankChordEvents.put(stepIndex, event);
                            final Map<Integer, Integer> starts = event.notes().stream()
                                    .collect(Collectors.toMap(ChordEventNote::midiNote, ChordEventNote::fineStart,
                                            (a, b) -> a, HashMap::new));
                            pendingBankFineStarts.put(stepIndex, starts);
                        }
                    }
                }
                return;
            }
            if (pendingBankFineMove) {
                nudgeHeldNotes(pendingBankDir, new HashSet<>(pendingBankTargetSteps), new HashMap<>(pendingBankChordEvents));
                clearPendingBankAction();
                return;
            }
            if (!pressed) {
                oled.valueInfo("Coarse Nudge", "Disabled");
                driver.notifyPopup("Coarse Nudge", "Use fine nudge in chord step mode");
                clearPendingBankAction();
                return;
            }
        }
        if (!pressed) {
            return;
        }
        adjustOctave(amount);
    }

    private void adjustChordClipLength(final int direction) {
        if (!ensureSelectedNoteClipSlot()) {
            return;
        }
        final double currentLength = noteStepClip.getLoopLength().get();
        if (direction < 0) {
            if (currentLength <= STEP_LENGTH) {
                oled.valueInfo("Clip Length", "Min");
                return;
            }
            final double newLength = Math.max(STEP_LENGTH, currentLength / 2.0);
            noteStepClip.getLoopLength().set(newLength);
            oled.valueInfo("Clip Length", formatBars(newLength));
            return;
        }
        final double newLength = Math.min(MAX_CHORD_STEPS * STEP_LENGTH, currentLength * 2.0);
        if (newLength <= currentLength) {
            oled.valueInfo("Clip Length", "Max");
            return;
        }
        noteStepClip.duplicateContent();
        noteStepClip.getLoopLength().set(newLength);
        oled.valueInfo("Clip Length", formatBars(newLength));
    }

    private String formatBars(final double beatLength) {
        final double bars = beatLength / 4.0;
        if (Math.rint(bars) == bars) {
            return Integer.toString((int) bars) + " Bars";
        }
        return String.format("%.2f Bars", bars);
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
                    ? (chordOctaveOffset > MIN_CHORD_OCTAVE_OFFSET ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF)
                    : (chordOctaveOffset < MAX_CHORD_OCTAVE_OFFSET ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF);
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
            return chordAccentState.isActive() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
        }
        if (driver.isGlobalAltHeld()) {
            return driver.getStepFillLightState();
        }
        return chordAccentState.isActive() ? BiColorLightState.AMBER_FULL : BiColorLightState.OFF;
    }

    private void handleChordAccentPressed(final boolean pressed) {
        final AccentLatchState.Transition transition = chordAccentState.handlePressed(pressed);
        if (transition == AccentLatchState.Transition.PRESSED) {
            oled.valueInfo("Accent", chordAccentState.isActive() ? "On" : "Off");
            return;
        }
        if (transition == AccentLatchState.Transition.TOGGLED_ON_RELEASE) {
            oled.valueInfo("Accent", chordAccentState.isActive() ? "On" : "Off");
            return;
        }
        oled.clearScreenDelayed();
    }

    private int currentChordVelocity(final int rawVelocity) {
        if (chordAccentState.isActive()) {
            return DEFAULT_CHORD_ACCENTED_VELOCITY;
        }
        return LiveVelocityLogic.resolveVelocity(defaultChordVelocity, chordVelocitySensitivity, rawVelocity);
    }

    private void toggleChordAccentForStep(final int stepIndex) {
        final Map<Integer, NoteStep> notesAtStep = noteStepsByPosition.get(stepIndex);
        if (notesAtStep == null || notesAtStep.isEmpty()) {
            return;
        }
        final boolean accented = notesAtStep.values().stream().allMatch(this::isChordAccented);
        final double targetVelocity = (accented ? defaultChordVelocity : DEFAULT_CHORD_ACCENTED_VELOCITY) / 127.0;
        notesAtStep.values().forEach(note -> note.setVelocity(targetVelocity));
        chordAccentState.markModified();
        oled.valueInfo("Accent", accented ? "Normal" : "Accented");
    }

    private boolean isChordAccented(final NoteStep noteStep) {
        final int velocity = (int) Math.round(noteStep.velocity() * 127);
        final int distanceToAccent = Math.abs(velocity - DEFAULT_CHORD_ACCENTED_VELOCITY);
        final int distanceToStandard = Math.abs(velocity - DEFAULT_CHORD_STANDARD_VELOCITY);
        return distanceToAccent <= distanceToStandard;
    }

    private boolean isChordStepAccented(final int stepIndex) {
        final Map<Integer, NoteStep> notesAtStep = noteStepsByPosition.get(stepIndex);
        return notesAtStep != null
                && !notesAtStep.isEmpty()
                && notesAtStep.values().stream()
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .allMatch(this::isChordAccented);
    }

    private boolean isHarmonicLiveMode() {
        return !noteStepActive && surfaceRole == SurfaceRole.NOTE_PLAY && liveNoteSubMode == LiveNoteSubMode.HARMONIC;
    }

    public String currentNoteSubModeLabel() {
        return liveNoteSubMode.displayName();
    }

    public boolean isHarmonicNoteSubMode() {
        return liveNoteSubMode == LiveNoteSubMode.HARMONIC;
    }

    public void resetNoteSubMode() {
        if (isChordStepSurface() || liveNoteSubMode == LiveNoteSubMode.MELODIC) {
            return;
        }
        applyLayoutChange(() -> liveNoteSubMode = LiveNoteSubMode.MELODIC);
    }

    public void cycleNoteSubMode() {
        if (isChordStepSurface()) {
            toggleSurfaceVariant();
            return;
        }
        final LiveNoteSubMode next = liveNoteSubMode.next();
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
        if (amount > 0 && !inKey) {
            toggleLayout();
            return;
        }
        if (amount < 0 && inKey) {
            toggleLayout();
        }
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
        builderInKey = !builderInKey;
        oled.valueInfo("Builder Layout", builderInKey ? "In Key" : "Chromatic");
        driver.notifyPopup("Builder Layout", builderInKey ? "In Key" : "Chromatic");
    }

    public BiColorLightState getModeButtonLightState() {
        return isChordStepSurface() ? BiColorLightState.AMBER_FULL : BiColorLightState.RED_FULL;
    }

    public boolean isChordStepSurface() {
        return surfaceRole == SurfaceRole.CHORD_STEP;
    }

    private void adjustScale(final int amount) {
        if (amount == 0) {
            return;
        }
        final int minScale = liveScaleMinIndex();
        final int nextScale = driver.getSharedScaleIndex() + amount;
        if (nextScale < minScale || nextScale >= scaleLibrary.getMusicalScalesCount()) {
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
        final int nextOctave = Math.max(MIN_OCTAVE, Math.min(MAX_OCTAVE, getOctave() + amount));
        if (nextOctave == getOctave()) {
            return;
        }
        applyLayoutChange(() -> driver.setSharedOctave(nextOctave));
        showState("Octave");
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
    }

    private void retuneLivePads(final Runnable stateChange) {
        livePadPerformer.retuneHeldPads(() -> {
            stateChange.run();
            applyLayout();
        }, liveVelocity);
    }

    private int[] getLivePadMidiNotes(final int padIndex) {
        final int[] layoutNotes = createLayout().notesForPad(padIndex);
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

    private int resolveLivePadVelocity(final int configuredVelocity, final int rawVelocity) {
        return LiveVelocityLogic.resolveVelocity(configuredVelocity, liveVelocitySensitivity, rawVelocity);
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
        if (padIndex < CLIP_ROW_PAD_COUNT) {
            return clipHandler.getPadLight(padIndex);
        }
        if (padIndex < STEP_PAD_OFFSET) {
            final int sourcePadIndex = padIndex - CHORD_SOURCE_PAD_OFFSET;
            if (isBuilderFamily()) {
                return getBuilderSourcePadLight(sourcePadIndex);
            }
            if (!chordBank.hasSlot(currentPresetFamilyIndex(), chordPage, sourcePadIndex)) {
                return RgbLigthState.OFF;
            }
            final ChordBank.Slot slot = chordBank.slot(currentPresetFamilyIndex(), chordPage, sourcePadIndex);
            final int groupIndex = sourcePadIndex / 8;
            final RgbLigthState grouped = getFamilyGroupColor(slot.family(), groupIndex, chordPage,
                    currentChordPageCount());
            return sourcePadIndex == selectedChordSlot ? SELECTED_CHORD : grouped.getDimmed();
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
        if (!StepPadLightHelper.isStepWithinVisibleLoop(stepIndex, chordStepPosition.getAvailableSteps())) {
            return RgbLigthState.OFF;
        }
        final boolean occupied = hasVisibleStepContent(stepIndex);
        final boolean accented = occupied && isChordStepAccented(stepIndex);
        final boolean sustained = !occupied && isChordStepSustained(stepIndex);
        final RgbLigthState occupiedStepColor = getChordOccupiedStepColor();
        final RgbLigthState sustainedStepColor = getChordSustainedStepColor();
        if (heldStepPads.contains(stepIndex)) {
            return HELD_STEP.getBrightest();
        }
        if (occupied) {
            if (stepIndex == playingStep) {
                return StepPadLightHelper.renderPlayheadHighlight(
                        accented ? occupiedStepColor.getBrightend() : occupiedStepColor);
            }
            return StepPadLightHelper.renderOccupiedStep(occupiedStepColor, accented, false);
        }
        if (sustained) {
            return stepIndex == playingStep
                    ? StepPadLightHelper.renderPlayheadHighlight(sustainedStepColor)
                    : sustainedStepColor;
        }
        return StepPadLightHelper.renderEmptyStep(stepIndex, playingStep);
    }

    private RgbLigthState getBuilderSourcePadLight(final int padIndex) {
        final int midiNote = getBuilderRenderedNoteMidiForPad(padIndex);
        if (midiNote < 0) {
            return RgbLigthState.OFF;
        }
        if (builderSelectedNotes.contains(midiNote)) {
            return SELECTED_BUILDER_NOTE;
        }
        return getBuilderBasePadLight(padIndex);
    }

    private RgbLigthState getChordOccupiedStepColor() {
        if (chordStepController.color() != null) {
            return chordStepController.color();
        }
        return chordStepBaseColor != null ? chordStepBaseColor : OCCUPIED_STEP;
    }

    private RgbLigthState getChordSustainedStepColor() {
        return getChordOccupiedStepColor().getVeryDimmed();
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
        if (heldStepPads.contains(stepIndex)) {
            return HELD_STEP.getBrightest();
        }
        return hasVisibleStepContent(stepIndex) ? OCCUPIED_STEP : DEFERRED_BOTTOM;
    }

    private RgbLigthState getFamilyColor(final String family) {
        return switch (family) {
            case "Barker" -> new RgbLigthState(120, 70, 0, true);
            case "Audible" -> new RgbLigthState(0, 90, 110, true);
            case "Sus Motion" -> new RgbLigthState(12, 100, 58, true);
            case "Quartal" -> new RgbLigthState(0, 58, 120, true);
            case "Cluster" -> new RgbLigthState(70, 0, 110, true);
            case "Minor Drift" -> new RgbLigthState(110, 20, 36, true);
            case "Dorian Lift" -> new RgbLigthState(30, 90, 18, true);
            default -> new RgbLigthState(88, 64, 0, true);
        };
    }

    private RgbLigthState getFamilyGroupColor(final String family, final int groupIndex, final int pageIndex,
                                              final int pageCount) {
        final boolean alternatePageVariant = pageCount > 1 && (pageIndex % 2 == 1);
        return switch (groupIndex % 4) {
            case 0 -> alternatePageVariant ? getAlternateFamilyColor(family).getBrightend()
                    : getFamilyColor(family).getBrightend();
            case 1 -> alternatePageVariant ? getFamilyColor(family) : getAlternateFamilyColor(family);
            case 2 -> alternatePageVariant ? getAlternateFamilyColor(family).getDimmed()
                    : getFamilyColor(family).getDimmed();
            default -> alternatePageVariant ? getFamilyColor(family).getDimmed()
                    : getAlternateFamilyColor(family).getDimmed();
        };
    }

    private RgbLigthState getAlternateFamilyColor(final String family) {
        return switch (family) {
            case "Barker" -> new RgbLigthState(24, 100, 34, true);
            case "Audible" -> new RgbLigthState(110, 72, 0, true);
            case "Sus Motion" -> new RgbLigthState(0, 66, 122, true);
            case "Quartal" -> new RgbLigthState(112, 22, 70, true);
            case "Cluster" -> new RgbLigthState(24, 108, 44, true);
            case "Minor Drift" -> new RgbLigthState(0, 94, 110, true);
            case "Dorian Lift" -> new RgbLigthState(114, 64, 0, true);
            default -> new RgbLigthState(44, 86, 24, true);
        };
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
        if ("Layout".equals(focus)) {
            if (isHarmonicLiveMode()) {
                oled.valueInfo("Layout", harmonicBassColumns ? "Bass Columns" : "Full Field");
            } else {
                oled.valueInfo("Layout", inKey ? "In Key" : "Chromatic");
            }
            return;
        }
        if ("Interpretation".equals(focus) && noteStepActive && currentStepSubMode == NoteStepSubMode.CHORD_STEP) {
            oled.valueInfo("Chord Step Mode", chordInterpretation.displayName());
            return;
        }
        oled.lineInfo("Root %s%d".formatted(NoteGridLayout.noteName(getRootNote()), getOctave()),
                noteStepActive
                        ? "Step: %s\n%s".formatted(currentStepSubMode.displayName(),
                        currentStepSubMode == NoteStepSubMode.CHORD_STEP ? currentChordDisplay() : "Deferred")
                        : "Scale: %s\n%s".formatted(getScaleDisplayName(),
                        isHarmonicLiveMode()
                                ? "Harmonic %s".formatted(harmonicBassColumns ? "Bass" : "Full")
                                : inKey ? "In Key" : "Chromatic"));
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
        oled.valueInfo("%s %d/%d".formatted(currentChordFamilyLabel(), chordPage + 1,
                        currentChordPageCount()),
                "%s %s".formatted(currentChordName(), chordInterpretationSuffix()));
    }

    private String currentChordDisplay() {
        return "%s %s".formatted(currentChordName(), chordInterpretationSuffix());
    }

    private String oledChordName(final ChordBank.Slot slot) {
        if ("Barker".equals(slot.family())) {
            return switch (slot.formulaIndex()) {
                case 0 -> "Q stack";
                case 1 -> "Q b7/9";
                case 2 -> "Q Maj7";
                case 3 -> "Q add6";
                case 4 -> "5th+Q";
                case 5 -> "Q +11";
                case 6 -> "Q oct";
                case 7 -> "Q 6/10";
                default -> "Barker";
            };
        }
        if ("Root Drone".equals(slot.family())) {
            return switch (slot.formulaIndex()) {
                case 0 -> "9/11";
                case 1 -> "sus";
                case 2 -> "6/9";
                case 3 -> "#11";
                case 4 -> "5/10";
                case 5 -> "add9";
                case 6 -> "sus69";
                case 7 -> "Maj7";
                default -> "Drone";
            };
        }
        return switch (slot.name()) {
            case "Fully diminished" -> "Fully dim";
            case "Half-diminished" -> "Half dim";
            case "Dominant 7th" -> "Dom 7th";
            case "Dominant 7th (b9)" -> "Dom 7b9";
            case "10th (Spread maj7)" -> "10th sprd";
            default -> slot.name();
        };
    }

    private String oledFamilyLabel(final String family) {
        return switch (family) {
            case BUILDER_FAMILY_LABEL -> BUILDER_FAMILY_LABEL;
            case "Sus Motion" -> "SusMot";
            case "Minor Drift" -> "MinDrft";
            case "Dorian Lift" -> "DorLift";
            case "Root Drone" -> "RootDrn";
            default -> family;
        };
    }

    private String chordInterpretationSuffix() {
        return "F%d %s K%s O%s".formatted(selectedChordFamily + 1,
                chordInterpretation == ChordInterpretation.AS_IS ? "Raw" : "InKey",
                NoteGridLayout.noteName(getRootNote()),
                formatSignedValue(chordOctaveOffset));
    }

    private ChordBank.Slot currentChordSlot() {
        if (isBuilderFamily()) {
            throw new IllegalStateException("Builder source has no preset slot");
        }
        ensureSelectedChordSlotValid();
        return chordBank.slot(currentPresetFamilyIndex(), chordPage, selectedChordSlot);
    }

    private void ensureSelectedChordSlotValid() {
        if (isBuilderFamily()) {
            chordPage = 0;
            selectedChordSlot = 0;
            return;
        }
        if (chordBank.hasSlot(currentPresetFamilyIndex(), chordPage, selectedChordSlot)) {
            return;
        }
        final int pageStart = chordPage * ChordBank.PAGE_SIZE;
        final int familySlotCount = chordBank.family(currentPresetFamilyIndex()).slots().size();
        if (pageStart >= familySlotCount) {
            chordPage = Math.max(0, currentChordPageCount() - 1);
        }
        selectedChordSlot = 0;
        while (selectedChordSlot < ChordBank.PAGE_SIZE
                && !chordBank.hasSlot(currentPresetFamilyIndex(), chordPage, selectedChordSlot)) {
            selectedChordSlot++;
        }
        if (selectedChordSlot >= ChordBank.PAGE_SIZE) {
            selectedChordSlot = 0;
        }
    }

    private int[] renderSelectedChord() {
        if (isBuilderFamily()) {
            return renderBuilderChord();
        }
        final ChordBank.Slot slot = currentChordSlot();
        if (chordInterpretation == ChordInterpretation.IN_SCALE) {
            final int shiftedRoot = getRootNote();
            final int castRoot = Math.floorMod(shiftedRoot, 12);
            final int castOctaveOffset = chordOctaveOffset;
            return transpose(chordBank.renderCast(currentPresetFamilyIndex(), chordPage, selectedChordSlot, getScale(), castRoot),
                    castOctaveOffset * 12);
        }
        return chordBank.renderAsIs(currentPresetFamilyIndex(), chordPage, selectedChordSlot,
                getChordRootMidi() + chordOctaveOffset * 12);
    }

    private int[] renderBuilderChord() {
        return builderSelectedNotes.stream()
                .sorted()
                .mapToInt(Integer::intValue)
                .toArray();
    }

    private void ensureBuilderSeededIfEmpty() {
        if (!isBuilderFamily() || !builderSelectedNotes.isEmpty()) {
            return;
        }
        final int firstVisibleNote = getBuilderFirstVisibleMidiNote();
        if (firstVisibleNote < 0) {
            return;
        }
        final int builderRoot = Math.floorMod(firstVisibleNote, 12);
        for (int padIndex = 0; padIndex < CHORD_SOURCE_PAD_COUNT; padIndex++) {
            final int midiNote = getBuilderRenderedNoteMidiForPad(padIndex);
            if (midiNote >= 0 && getScale().isRootMidiNote(builderRoot, midiNote)) {
                builderSelectedNotes.add(midiNote);
                return;
            }
        }
        final int firstPadNote = getBuilderRenderedNoteMidiForPad(0);
        if (firstPadNote >= 0) {
            builderSelectedNotes.add(firstPadNote);
        }
    }

    private int getChordRootMidi() {
        return (ChordBank.MID_REGISTER_OCTAVE + 1) * 12 + getRootNote();
    }

    private int[] transpose(final int[] notes, final int semitones) {
        if (semitones == 0) {
            return notes;
        }
        final int[] transposed = new int[notes.length];
        for (int i = 0; i < notes.length; i++) {
            transposed[i] = notes[i] + semitones;
        }
        return transposed;
    }

    private void showChordRootInfo() {
        oled.valueInfo("Chord Root", NoteGridLayout.noteName(getRootNote()));
    }

    private boolean isBuilderFamily() {
        return selectedChordFamily == BUILDER_FAMILY_INDEX;
    }

    private int currentPresetFamilyIndex() {
        return selectedChordFamily - 1;
    }

    private int chordFamilyCount() {
        return chordBank.families().size() + 1;
    }

    private int currentChordPageCount() {
        return isBuilderFamily() ? 1 : chordBank.pageCount(currentPresetFamilyIndex());
    }

    private String currentChordFamilyLabel() {
        return isBuilderFamily()
                ? BUILDER_FAMILY_LABEL
                : oledFamilyLabel(chordBank.family(currentPresetFamilyIndex()).family());
    }

    private String currentChordName() {
        if (isBuilderFamily()) {
            return builderSelectedNotes.isEmpty() ? "Empty" : builderSelectionSummary();
        }
        return oledChordName(currentChordSlot());
    }

    private void toggleBuilderNoteOffset(final int padIndex) {
        final int midiNote = getBuilderRenderedNoteMidiForPad(padIndex);
        if (midiNote < 0) {
            return;
        }
        if (builderSelectedNotes.contains(midiNote)) {
            builderSelectedNotes.remove(midiNote);
        } else {
            builderSelectedNotes.add(midiNote);
        }
    }

    private int getBuilderNoteMidiForPad(final int padIndex) {
        if (padIndex < 0 || padIndex >= CHORD_SOURCE_PAD_COUNT) {
            return -1;
        }
        final int firstVisibleNote = getBuilderFirstVisibleMidiNote();
        if (firstVisibleNote < 0) {
            return -1;
        }
        if (!builderInKey) {
            final int note = firstVisibleNote + padIndex;
            return note >= 0 && note <= 127 ? note : -1;
        }
        final int builderRoot = Math.floorMod(firstVisibleNote, 12);
        int note = firstVisibleNote;
        for (int i = 0; i < padIndex; i++) {
            note = nextBuilderScaleNote(note, builderRoot);
            if (note < 0) {
                return -1;
            }
        }
        return note;
    }

    private int getBuilderRenderedNoteMidiForPad(final int padIndex) {
        return getBuilderNoteMidiForPad(padIndex);
    }

    private String builderSelectionSummary() {
        final List<Integer> renderedNotes = builderSelectedNotes.stream()
                .sorted()
                .toList();
        final List<String> noteNames = renderedNotes.stream()
                .limit(4)
                .map(midiNote -> NoteGridLayout.noteName(Math.floorMod(midiNote, 12)))
                .toList();
        final String suffix = renderedNotes.size() > 4 ? " +" + (renderedNotes.size() - 4) : "";
        return "%d notes %s%s".formatted(renderedNotes.size(), String.join(" ", noteNames), suffix).trim();
    }

    private RgbLigthState getBuilderBasePadLight(final int padIndex) {
        final int midiNote = getBuilderNoteMidiForPad(padIndex);
        final int firstVisibleNote = getBuilderFirstVisibleMidiNote();
        final int builderRoot = firstVisibleNote >= 0
                ? Math.floorMod(firstVisibleNote, 12)
                : Math.floorMod(getRootNote(), 12);
        final RgbLigthState base;
        if (midiNote < 0) {
            base = RgbLigthState.OFF;
        } else {
            final NoteGridLayout.PadRole role;
            if (getScale().isRootMidiNote(builderRoot, midiNote)) {
                role = NoteGridLayout.PadRole.ROOT;
            } else if (getScale().isMidiNoteInScale(builderRoot, midiNote)) {
                role = NoteGridLayout.PadRole.IN_SCALE;
            } else {
                role = NoteGridLayout.PadRole.OUT_OF_SCALE;
            }
            base = switch (role) {
                case ROOT -> ROOT_COLOR;
                case IN_SCALE -> IN_SCALE_COLOR;
                case OUT_OF_SCALE -> OUT_OF_SCALE_COLOR;
                case UNAVAILABLE -> RgbLigthState.OFF;
            };
        }
        return livePadPerformer.isPadHeld(padIndex) ? base.getBrightest() : base;
    }

    private void showChordOctaveInfo() {
        oled.valueInfo("Chord Oct", formatSignedValue(chordOctaveOffset));
    }

    private void showChordFamilyInfo() {
        if (isBuilderFamily()) {
            oled.valueInfo("Chord Family", BUILDER_FAMILY_LABEL);
            return;
        }
        final ChordBank.Family family = chordBank.family(currentPresetFamilyIndex());
        oled.valueInfo("Chord Family", family.family());
    }

    private void resetChordFamilySelection() {
        selectedChordFamily = BUILDER_FAMILY_INDEX;
        chordPage = 0;
        selectedChordSlot = 0;
        ensureSelectedChordSlotValid();
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
        if (heldStepAnchor == null || heldStepAnchor != releasedStepIndex) {
            return;
        }
        heldStepAnchor = heldStepPads.stream().findFirst().orElse(null);
    }

    private MusicalScale getScale() {
        final int scaleCount = scaleLibrary.getMusicalScalesCount();
        final int safeIndex = Math.max(0, Math.min(scaleCount - 1, driver.getSharedScaleIndex()));
        return scaleLibrary.getMusicalScale(safeIndex);
    }

    public MusicalScale getCurrentScale() {
        return getScale();
    }

    private String getScaleDisplayName() {
        return switch (getScale().getName()) {
            case "Major" -> "Major";
            case "Minor" -> "Minor";
            case "Phrygian Dominant" -> "Phryg Dom";
            case "Double Harmonic Major" -> "DH Maj";
            case "Double Harmonic Minor" -> "DH Min";
            case "Harmonic Major" -> "Harm Maj";
            case "Harmonic Minor" -> "Harm Min";
            case "Jazz Minor" -> "Jazz Min";
            case "Overtone Scale" -> "Overtone";
            case "Hungarian Minor" -> "Hung Min";
            case "Ukranian Dorian" -> "Ukr Dor";
            case "Super Locrian" -> "Sup Loc";
            case "Half-diminished" -> "Half Dim";
            case "Diminished WH" -> "Dim WH";
            case "Diminished HW" -> "Dim HW";
            case "Major Pentatonic" -> "Maj Pent";
            case "Minor Pentatonic" -> "Min Pent";
            case "Blues Major" -> "Bl Maj";
            case "Blues Minor" -> "Bl Min";
            case "Whole Tone" -> "Whole";
            case "Major Triad" -> "Maj Tri";
            case "Minor Triad" -> "Min Tri";
            case "Bebop Major" -> "Bebop Maj";
            case "Bebop Dorian" -> "Bebop Dor";
            case "Bebop Mixolydian" -> "Bebop Mix";
            case "Bebop Minor" -> "Bebop Min";
            default -> getScale().getName();
        };
    }

    public String getCurrentScaleDisplayName() {
        return getScaleDisplayName();
    }

    private int getRootNote() {
        return driver.getSharedRootNote();
    }

    public int getCurrentRootNoteClass() {
        return getRootNote();
    }

    private int getOctave() {
        return driver.getSharedOctave();
    }

    public int getCurrentOctave() {
        return getOctave();
    }

    public int getCurrentBaseMidiNote() {
        return driver.getSharedBaseMidiNote();
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
        return new NoteGridLayout(getScale(), getRootNote(), getOctave(), inKey);
    }

    private int getBuilderFirstVisibleMidiNote() {
        final int firstVisible = applyLivePitchOffset(getChordRootMidi() + chordOctaveOffset * 12);
        return firstVisible >= 0 && firstVisible <= 127 ? firstVisible : -1;
    }

    private int nextBuilderScaleNote(final int currentNote, final int builderRoot) {
        int note = currentNote + 1;
        while (note <= 127) {
            if (getScale().isMidiNoteInScale(builderRoot, note)) {
                return note;
            }
            note++;
        }
        return -1;
    }

    private int applyLivePitchOffset(final int midiNote) {
        if (midiNote < 0) {
            return -1;
        }
        if (isHarmonicLiveMode()) {
            return midiNote;
        }
        if (livePitchGlissMode == LivePitchGlissMode.SCALE_DEGREE) {
            return transposeByScaleDegrees(midiNote, liveScaleDegreeGlissOffset);
        }
        final int shifted = midiNote + getLivePitchOffset();
        return shifted >= 0 && shifted <= 127 ? shifted : -1;
    }

    private int transposeByScaleDegrees(final int midiNote, final int scaleDegrees) {
        if (scaleDegrees == 0) {
            return midiNote;
        }
        int note = midiNote;
        int remaining = Math.abs(scaleDegrees);
        final int direction = scaleDegrees > 0 ? 1 : -1;
        while (remaining > 0) {
            note += direction;
            while (note >= 0 && note <= 127 && !getScale().isMidiNoteInScale(getRootNote(), note)) {
                note += direction;
            }
            if (note < 0 || note > 127) {
                return -1;
            }
            remaining--;
        }
        return note;
    }

    private void clearTranslation() {
        livePadPerformer.releaseHeldNotes();
        heldStepAnchor = null;
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
        return noteStepsByPosition.values().stream()
                .flatMap(step -> step.values().stream())
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .collect(Collectors.toList());
    }

    @Override
    public List<NoteStep> getHeldNotes() {
        return heldStepPads.stream()
                .flatMap(step -> noteStepsByPosition.getOrDefault(step, Map.of()).values().stream())
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .collect(Collectors.toList());
    }

    @Override
    public List<NoteStep> getFocusedNotes() {
        final List<NoteStep> heldNotes = getHeldNotes();
        if (!heldNotes.isEmpty()) {
            return heldNotes;
        }
        if (selectedPresetStepIndex == null) {
            return List.of();
        }
        return noteStepsByPosition.getOrDefault(selectedPresetStepIndex, Map.of()).values().stream()
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .collect(Collectors.toList());
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
        return Math.max(1, chordStepPosition.getPages());
    }

    private void pageChordSteps(final int direction) {
        final int previousPage = chordStepPosition.getCurrentPage();
        if (direction < 0) {
            chordStepPosition.scrollLeft();
        } else if (direction > 0) {
            chordStepPosition.scrollRight();
        }
        if (chordStepPosition.getCurrentPage() == previousPage) {
            return;
        }
        clearAllBankFineNudgeSessions();
        refreshChordStepObservation();
        showChordPageInfo();
    }

    private void clearHeldBankFineNudgeSession() {
        heldBankFineStarts.clear();
        heldBankChordEvents.clear();
    }

    private void clearShiftBankFineNudgeSession() {
        shiftBankTargetSteps.clear();
        shiftBankFineStarts.clear();
        shiftBankChordEvents.clear();
    }

    private void clearAllBankFineNudgeSessions() {
        clearHeldBankFineNudgeSession();
        clearShiftBankFineNudgeSession();
    }

    private void showChordPageInfo() {
        oled.valueInfo("Chord %d/%d".formatted(chordStepPosition.getCurrentPage() + 1, chordPageCount()),
                currentChordDisplay());
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
        notes.forEach(note -> modifiedStepPads.add(note.x()));
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
        observedChordStepState.clear();
        pendingMovedNotes.clear();
    }

    private record ObservedChordNote(int localStep, int globalStep, int fineStart, int midiNote, int velocity,
                                     double duration, NoteStep visibleStep) {
    }

    private record ChordEvent(int localStep, int globalStep, int anchorFineStart, List<ChordEventNote> notes) {
    }

    private record ChordEventNote(int midiNote, int fineStart, int startOffset, int velocity, double duration,
                                  NoteStep visibleStep) {
    }

    private record ChordEventNoteMove(int localStep, ChordEventNote note, int targetFineStart) {
        private int sourceFineStart() {
            return note.fineStart();
        }

        private int midiNote() {
            return note.midiNote();
        }

        private int velocity() {
            return note.velocity();
        }

        private double duration() {
            return note.duration();
        }

        private NoteStep visibleStep() {
            return note.visibleStep();
        }
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
        return observedChordStepState.hasAnyObservedNotes()
                || noteStepsByPosition.values().stream()
                .flatMap(step -> step.values().stream())
                .anyMatch(note -> note.state() == NoteStep.State.NoteOn);
    }

    private boolean hasVisibleStepContent(final int stepIndex) {
        final int globalStep = localToGlobalStep(stepIndex);
        if (observedChordStepState.hasStepContent(globalStep)) {
            return true;
        }
        if (clipNotesByStep.containsKey(stepIndex)) {
            return true;
        }
        return noteStepsByPosition.getOrDefault(stepIndex, Map.of()).values().stream()
                .anyMatch(note -> note.state() != NoteStep.State.Empty);
    }

    private boolean hasStepStartNote(final int stepIndex) {
        final int globalStep = localToGlobalStep(stepIndex);
        if (observedChordStepState.hasStepStart(globalStep)) {
            return true;
        }
        return noteStepsByPosition.getOrDefault(stepIndex, Map.of()).values().stream()
                .anyMatch(note -> note.state() == NoteStep.State.NoteOn);
    }

    private Set<Integer> bestAvailableStepNotes(final int stepIndex) {
        final Set<Integer> visibleNotes = clipNotesByStep.get(stepIndex);
        if (visibleNotes != null && !visibleNotes.isEmpty()) {
            return new HashSet<>(visibleNotes);
        }
        final Set<Integer> observedNotes = observedChordStepState.notesAtStep(localToGlobalStep(stepIndex));
        if (observedNotes != null && !observedNotes.isEmpty()) {
            return new HashSet<>(observedNotes);
        }
        return Set.of();
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
                            chordInterpretation = ChordInterpretation.AS_IS;
                            oled.valueInfo("Interpret", chordInterpretation.displayName());
                        });
                        oled.valueInfo("Interpret", chordInterpretation.displayName());
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
        builderSelectedNotes.clear();
        heldStepPads.clear();
        heldStepAnchor = null;
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
        notePlayController.deactivate(this::releaseHeldLiveNotes);
        builderSelectedNotes.clear();
        heldStepPads.clear();
        heldStepAnchor = null;
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
