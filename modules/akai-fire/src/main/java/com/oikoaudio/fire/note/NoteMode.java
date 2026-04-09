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
import com.bitwig.extensions.framework.values.StepViewPosition;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
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
import com.oikoaudio.fire.sequence.StepSequencerEncoderHandler;
import com.oikoaudio.fire.sequence.StepPadLightHelper;
import com.oikoaudio.fire.sequence.StepSequencerHost;
import com.oikoaudio.fire.utils.PatternButtons;

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

public class NoteMode extends Layer implements StepSequencerHost {
    private static final int BUILDER_FAMILY_INDEX = 0;
    private static final String BUILDER_FAMILY_LABEL = "Builder";
    private static final int PIANO_HIGHLIGHT_INDEX = -1;
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
    private static final int OIKORD_ROOT_ENCODER_THRESHOLD = 8;
    private static final int OIKORD_OCTAVE_ENCODER_THRESHOLD = 6;
    private static final int OIKORD_FAMILY_ENCODER_THRESHOLD = 8;
    private static final int LIVE_VELOCITY_ENCODER_THRESHOLD = 1;
    private static final int LIVE_PITCH_OFFSET_ENCODER_THRESHOLD = 5;
    private static final int MIN_OIKORD_ROOT_OFFSET = -24;
    private static final int MAX_OIKORD_ROOT_OFFSET = 24;
    private static final int MIN_OIKORD_OCTAVE_OFFSET = -3;
    private static final int MAX_OIKORD_OCTAVE_OFFSET = 3;
    private static final int MIN_MIDI_VALUE = 0;
    private static final int MAX_MIDI_VALUE = 127;
    private static final int MIN_VELOCITY = 1;
    private static final int DEFAULT_LIVE_VELOCITY = 100;
    private static final int DEFAULT_OIKORD_STANDARD_VELOCITY = 100;
    private static final int DEFAULT_OIKORD_ACCENTED_VELOCITY = 127;
    private static final long TOUCH_RESET_HOLD_MS = 1000L;
    private static final long TOUCH_RESET_RECENT_ADJUSTMENT_SUPPRESS_MS = 300L;
    private static final int TOUCH_RESET_TOLERATED_ADJUSTMENT_UNITS = 2;
    private static final int DEFAULT_TIMBRE = 64;
    private static final int DEFAULT_LIVE_PITCH_EXPRESSION = 64;
    private static final int MIDI_CC_MOD = 1;
    private static final int MIDI_CC_SUSTAIN = 64;
    private static final int MIDI_CC_SOSTENUTO = 66;
    private static final int MIDI_CC_TIMBRE = 74;
    private static final int[] LIVE_PITCH_OFFSETS = {-24, -19, -12, -7, 0, 7, 12, 19, 24};
    private static final int DEFAULT_LIVE_PITCH_OFFSET_INDEX = 4;
    private static final RgbLigthState ROOT_COLOR = new RgbLigthState(120, 64, 0, true);
    private static final RgbLigthState IN_SCALE_COLOR = new RgbLigthState(0, 72, 110, true);
    private static final RgbLigthState PIANO_BLACK_KEY_COLOR = new RgbLigthState(0, 56, 120, true);
    private static final RgbLigthState PIANO_WHITE_KEY_COLOR = RgbLigthState.GRAY_2;
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
    private final OledDisplay oled;
    private final NoteInput noteInput;
    private final PatternButtons patternButtons;
    private final NoteRepeatHandler noteRepeatHandler;
    private final MusicalScaleLibrary scaleLibrary = MusicalScaleLibrary.getInstance();
    private final Integer[] noteTranslationTable = new Integer[128];
    private final Set<Integer> heldPads = new HashSet<>();
    private final Map<Integer, Integer> soundingLiveNotesByPad = new HashMap<>();
    private final Set<Integer> heldStepPads = new HashSet<>();
    private final Set<Integer> addedStepPads = new HashSet<>();
    private final Set<Integer> modifiedStepPads = new HashSet<>();
    private final Set<Integer> auditioningNotes = new HashSet<>();
    private final Set<Integer> builderSelectedNotes = new HashSet<>();
    private final Set<Integer> modifierHandledStepPads = new HashSet<>();
    private final Map<Integer, Set<Integer>> clipNotesByStep = new HashMap<>();
    private final Map<Integer, Set<Integer>> observedClipNotesByStep = new HashMap<>();
    private final Map<Integer, Map<Integer, Set<Integer>>> observedFineOccupancyByStep = new HashMap<>();
    private final Map<Integer, Map<Integer, Integer>> observedFineNoteStartsByStep = new HashMap<>();
    private final Map<Integer, Map<Integer, Integer>> heldBankFineStarts = new HashMap<>();
    private final Map<Integer, ChordEvent> heldBankChordEvents = new HashMap<>();
    private final Set<Integer> shiftBankTargetSteps = new HashSet<>();
    private final Map<Integer, Map<Integer, Integer>> shiftBankFineStarts = new HashMap<>();
    private final Map<Integer, ChordEvent> shiftBankChordEvents = new HashMap<>();
    private final Set<Integer> pendingBankTargetSteps = new HashSet<>();
    private final Map<Integer, Map<Integer, Integer>> pendingBankFineStarts = new HashMap<>();
    private final Map<Integer, ChordEvent> pendingBankChordEvents = new HashMap<>();
    private final OikordBank oikordBank = new OikordBank();
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
    private final BooleanValueObject deleteHeld = new BooleanValueObject();
    private final BooleanValueObject selectHeld = new BooleanValueObject();
    private final BooleanValueObject copyHeld = new BooleanValueObject();
    private final BooleanValueObject fixedLengthHeld = new BooleanValueObject();
    private final BooleanValueObject invertHeld = new BooleanValueObject();
    private final BooleanValueObject lengthDisplay = new BooleanValueObject();
    private final Map<Integer, Map<Integer, NoteStep>> noteStepsByPosition = new HashMap<>();
    private final Map<String, NoteStepMoveSnapshot> pendingMovedNotes = new HashMap<>();

    private int scaleIndex = PIANO_HIGHLIGHT_INDEX;
    private int transposeBase = 48;
    private boolean inKey = false;
    private boolean noteStepActive = false;
    private boolean oikordAccentActive = false;
    private boolean oikordAccentButtonHeld = false;
    private boolean oikordAccentModified = false;
    private boolean mainEncoderPressConsumed = false;
    private boolean builderInKey = false;
    private NoteStepSubMode currentStepSubMode = NoteStepSubMode.OIKORD_STEP;
    private OikordInterpretation oikordInterpretation = OikordInterpretation.AS_IS;
    private int selectedOikordFamily = 0;
    private int oikordPage = 0;
    private int selectedOikordSlot = 0;
    private Integer selectedPresetStepIndex = null;
    private int oikordRootOffset = 0;
    private int oikordOctaveOffset = 0;
    private Integer heldStepAnchor = null;
    private int liveVelocity = DEFAULT_LIVE_VELOCITY;
    private int livePressure = MIN_MIDI_VALUE;
    private int liveTimbre = DEFAULT_TIMBRE;
    private int liveModulation = MIN_MIDI_VALUE;
    private int livePitchExpression = DEFAULT_LIVE_PITCH_EXPRESSION;
    private int defaultOikordVelocity = DEFAULT_OIKORD_STANDARD_VELOCITY;
    private int playingStep = -1;
    private int pendingBankDir = 0;
    private int livePitchOffsetIndex = DEFAULT_LIVE_PITCH_OFFSET_INDEX;
    private EncoderMode liveEncoderMode = EncoderMode.CHANNEL;
    private Layer currentLiveEncoderLayer;
    private boolean sustainActive = false;
    private boolean sostenutoActive = false;
    private boolean selectedNoteClipHasContent = false;
    private int selectedNoteClipSlotIndex = -1;
    private boolean chordObservationResyncQueued = false;
    private boolean pendingBankFineMove = false;
    private boolean bankMoveInFlight = false;
    private int bankMoveGeneration = 0;
    private RgbLigthState oikordStepBaseColor = OCCUPIED_STEP;
    private RgbLigthState selectedNoteClipColor = OCCUPIED_STEP;
    private final EncoderStepAccumulator liveVelocityEncoder = new EncoderStepAccumulator(LIVE_VELOCITY_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator livePitchOffsetEncoder = new EncoderStepAccumulator(LIVE_PITCH_OFFSET_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator liveScaleEncoder = new EncoderStepAccumulator(LIVE_NOTE_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator liveOctaveEncoder = new EncoderStepAccumulator(LIVE_NOTE_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator liveLayoutEncoder = new EncoderStepAccumulator(LIVE_LAYOUT_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator oikordRootEncoder = new EncoderStepAccumulator(OIKORD_ROOT_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator oikordOctaveEncoder = new EncoderStepAccumulator(OIKORD_OCTAVE_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator oikordFamilyEncoder = new EncoderStepAccumulator(OIKORD_FAMILY_ENCODER_THRESHOLD);
    private final TouchResetGesture touchResetGesture =
            new TouchResetGesture(4, TOUCH_RESET_HOLD_MS, TOUCH_RESET_RECENT_ADJUSTMENT_SUPPRESS_MS,
                    TOUCH_RESET_TOLERATED_ADJUSTMENT_UNITS);
    private enum NoteStepSubMode {
        OIKORD_STEP("Chord Step", BiColorLightState.GREEN_HALF, BiColorLightState.GREEN_FULL),
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

    private enum OikordInterpretation {
        AS_IS("As Is"),
        CAST("Cast");

        private final String displayName;

        OikordInterpretation(final String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        public OikordInterpretation next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public NoteMode(final AkaiFireOikontrolExtension driver, final NoteRepeatHandler noteRepeatHandler) {
        super(driver.getLayers(), "NOTE_MODE_LAYER");
        this.driver = driver;
        this.oled = driver.getOled();
        this.noteInput = driver.getNoteInput();
        this.patternButtons = driver.getPatternButtons();
        this.noteRepeatHandler = noteRepeatHandler;
        this.liveModeControlLayer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_PAGE_CONTROLS");
        this.liveChannelLayer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_CHANNEL");
        this.liveMixerLayer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_MIXER");
        this.liveUser1Layer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_USER1");
        this.liveUser2Layer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_USER2");

        final ControllerHost host = driver.getHost();
        this.cursorTrack = host.createCursorTrack("NOTE_VIEW", "Note View", 8, 8, true);
        this.cursorTrack.name().markInterested();
        this.cursorTrack.color().markInterested();
        this.cursorTrack.color().addValueObserver((r, g, b) -> oikordStepBaseColor = ColorLookup.getColor(r, g, b));
        oikordStepBaseColor = ColorLookup.getColor(this.cursorTrack.color().get());
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
        observeSelectedNoteClip();
        this.stepEncoderBankLayout = createStepEncoderBankLayout();
        this.stepEncoderLayer = new StepSequencerEncoderHandler(this, driver);
        this.currentLiveEncoderLayer = liveChannelLayer;

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
        applyDefaultScalePreference();
    }

    private void applyDefaultScalePreference() {
        final String defaultScale = driver.getDefaultScalePreference();
        switch (defaultScale) {
            case FireControlPreferences.DEFAULT_SCALE_MINOR -> {
                scaleIndex = findScaleIndex("Aeolian (Minor)", 2);
                inKey = true;
            }
            case FireControlPreferences.DEFAULT_SCALE_MAJOR -> {
                scaleIndex = findScaleIndex("Ionan (Major)", 1);
                inKey = true;
            }
            default -> {
                scaleIndex = PIANO_HIGHLIGHT_INDEX;
                inKey = false;
            }
        }
    }

    private int findScaleIndex(final String scaleName, final int fallbackIndex) {
        for (int i = 0; i < scaleLibrary.getMusicalScalesCount(); i++) {
            if (scaleLibrary.getMusicalScale(i).getName().equals(scaleName)) {
                return i;
            }
        }
        return fallbackIndex;
    }

    private void bindPads() {
        final RgbButton[] pads = driver.getRgbButtons();
        for (int index = 0; index < pads.length; index++) {
            final int padIndex = index;
            pads[index].bindPressed(this, pressed -> handlePadPress(padIndex, pressed), () -> getPadLight(padIndex));
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
        encoders[0].bindEncoder(liveChannelLayer, this::adjustLivePitchOffset);
        encoders[0].bindTouched(liveChannelLayer, this::handleLivePitchOffsetTouch);

        encoders[1].bindEncoder(liveChannelLayer, this::handleLiveVelocityEncoder);
        encoders[1].bindTouched(liveChannelLayer, this::handleLiveVelocityTouch);

        encoders[2].bindEncoder(liveChannelLayer, this::handleEncoder1);
        encoders[2].bindTouched(liveChannelLayer, this::handleEncoder1Touch);

        encoders[3].bindEncoder(liveChannelLayer, this::handleEncoder3);
        encoders[3].bindTouched(liveChannelLayer, this::handleEncoder3Touch);
    }

    private void bindLiveExpressionEncoders(final TouchEncoder[] encoders) {
        bindLiveMidiEncoder(encoders[0], liveUser1Layer, "Mod",
                this::adjustLiveModulation, () -> liveModulation);
        bindLiveMidiEncoder(encoders[1], liveUser1Layer, "Pressure",
                this::adjustLivePressure, () -> livePressure);
        bindLiveMidiEncoder(encoders[2], liveUser1Layer, "Timbre",
                this::adjustLiveTimbre, () -> liveTimbre);
        bindLiveMidiEncoder(encoders[3], liveUser1Layer, "Pitch Expr",
                this::adjustLivePitchExpression, () -> livePitchExpression);
    }

    private void bindLiveRemoteEncoders(final TouchEncoder[] encoders) {
        for (int i = 0; i < encoders.length; i++) {
            final int index = i;
            final Parameter parameter = liveRemoteControlsPage.getParameter(index);
            encoders[i].bindEncoder(liveUser2Layer, inc -> adjustMixerParameter(parameter, parameter.name().get(), inc));
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
            final com.bitwig.extension.controller.api.Parameter parameter = params.get(i);
            final String fallbackLabel = switch (i) {
                case 0 -> "Volume";
                case 1 -> "Pan";
                case 2 -> "Send 1";
                default -> "Send 2";
            };
            encoders[i].bindEncoder(liveMixerLayer, inc -> adjustMixerParameter(parameter, fallbackLabel, inc));
            encoders[i].bindTouched(liveMixerLayer, touched -> {
                if (touched) {
                    oled.valueInfo(parameter.name().get(), parameter.displayedValue().get());
                } else {
                    oled.clearScreenDelayed();
                }
            });
        }
    }

    private void bindLiveMidiEncoder(final TouchEncoder encoder, final Layer layer, final String label,
                                     final java.util.function.IntConsumer adjuster,
                                     final java.util.function.IntSupplier valueSupplier) {
        encoder.bindEncoder(layer, adjuster::accept);
        encoder.bindTouched(layer, touched -> handleLiveExpressionTouch(touched, label,
                Integer.toString(valueSupplier.getAsInt())));
    }

    private void handleLiveVelocityEncoder(final int inc) {
        final int steps = liveVelocityEncoder.consume(inc);
        if (steps == 0) {
            return;
        }
        markEncoderAdjusted(1);
        final int nextVelocity = Math.max(MIN_VELOCITY, Math.min(MAX_MIDI_VALUE, liveVelocity + steps));
        if (nextVelocity == liveVelocity) {
            return;
        }
        liveVelocity = nextVelocity;
        applyLiveVelocity();
        oled.paramInfo("Velocity", liveVelocity, "Live Note", MIN_VELOCITY, MAX_MIDI_VALUE);
    }

    private void handleEncoder1(final int inc) {
        final int steps = liveScaleEncoder.consume(inc);
        if (steps != 0) {
            markEncoderAdjusted(2);
            adjustScale(steps);
        }
    }

    private void handleEncoder2(final int inc) {
        final int steps = liveOctaveEncoder.consume(inc);
        if (steps != 0) {
            markEncoderAdjusted(2);
            adjustOctave(steps);
        }
    }

    private void handleEncoder3(final int inc) {
        final int steps = liveLayoutEncoder.consume(inc);
        if (steps != 0) {
            markEncoderAdjusted(3);
            adjustLayout(steps);
        }
    }

    private void adjustLivePressure(final int inc) {
        adjustLiveMidiValue(inc, "Pressure", value -> {
            livePressure = value;
            noteInput.sendRawMidiEvent(Midi.CHANNEL_AT, livePressure, 0);
        }, livePressure);
    }

    private void adjustLiveTimbre(final int inc) {
        adjustLiveMidiValue(inc, "Timbre", value -> {
            liveTimbre = value;
            noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_TIMBRE, liveTimbre);
        }, liveTimbre);
    }

    private void adjustLiveModulation(final int inc) {
        adjustLiveMidiValue(inc, "Mod", value -> {
            liveModulation = value;
            noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_MOD, liveModulation);
        }, liveModulation);
    }

    private void adjustLivePitchExpression(final int inc) {
        adjustLiveMidiValue(inc, "Pitch Expr", value -> {
            livePitchExpression = value;
            sendPitchExpressionValue(value);
        }, livePitchExpression);
    }

    private void adjustLiveMidiValue(final int inc, final String label,
                                     final java.util.function.IntConsumer setter, final int currentValue) {
        if (inc == 0) {
            return;
        }
        final int nextValue = Math.max(MIN_MIDI_VALUE, Math.min(MAX_MIDI_VALUE, currentValue + inc));
        if (nextValue == currentValue) {
            return;
        }
        setter.accept(nextValue);
        oled.paramInfo(label, nextValue, "Live Note", MIN_MIDI_VALUE, MAX_MIDI_VALUE);
    }

    private void adjustLivePitchOffset(final int inc) {
        final int steps = livePitchOffsetEncoder.consume(inc);
        if (steps == 0) {
            return;
        }
        final int nextIndex = Math.max(0, Math.min(LIVE_PITCH_OFFSETS.length - 1, livePitchOffsetIndex + steps));
        if (nextIndex == livePitchOffsetIndex) {
            return;
        }
        markEncoderAdjusted(0);
        final Map<Integer, Integer> oldHeldNotes = collectHeldNotes(createLayout());
        final boolean retuneHeld = driver.shouldRetuneHeldLiveNotes();
        if (retuneHeld) {
            sendHeldNotes(oldHeldNotes, false);
        }
        livePitchOffsetIndex = nextIndex;
        applyLayout();
        if (retuneHeld) {
            sendHeldNotes(collectHeldNotes(createLayout()), true);
        }
        oled.valueInfo("Pitch Offs", formatSignedValue(getLivePitchOffset()));
    }

    private void handleLivePitchOffsetTouch(final boolean touched) {
        handleResettableTouch(0, touched,
                () -> oled.valueInfo("Pitch Offs", formatSignedValue(getLivePitchOffset())),
                livePitchOffsetEncoder::reset);
    }

    private void sendPitchExpressionValue(final int value) {
        final int bend;
        if (value <= DEFAULT_LIVE_PITCH_EXPRESSION) {
            bend = (int) Math.round((value / (double) DEFAULT_LIVE_PITCH_EXPRESSION) * 8192.0);
        } else {
            final int rangeAboveCenter = MAX_MIDI_VALUE - DEFAULT_LIVE_PITCH_EXPRESSION;
            bend = 8192 + (int) Math.round(((value - DEFAULT_LIVE_PITCH_EXPRESSION) / (double) rangeAboveCenter)
                    * (16383.0 - 8192.0));
        }
        noteInput.sendRawMidiEvent(Midi.PITCH_BEND, bend & 0x7F, (bend >> 7) & 0x7F);
    }

    private void resetLivePerformanceToggles() {
        if (sustainActive) {
            sustainActive = false;
            noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_SUSTAIN, MIN_MIDI_VALUE);
        }
        if (sostenutoActive) {
            sostenutoActive = false;
            noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_SOSTENUTO, MIN_MIDI_VALUE);
        }
    }

    private int getLivePitchOffset() {
        return LIVE_PITCH_OFFSETS[livePitchOffsetIndex];
    }

    private void handleLiveExpressionTouch(final boolean touched, final String label, final String value) {
        if (touched) {
            oled.valueInfo(label, value);
        } else {
            oled.clearScreenDelayed();
        }
    }

    private void handleLiveVelocityTouch(final boolean pressed) {
        handleResettableTouch(1, pressed,
                () -> oled.valueInfo("Velocity", Integer.toString(liveVelocity)),
                liveVelocityEncoder::reset);
    }

    private boolean isChordStepModeActive() {
        return noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP;
    }

    private void handleMute1Button(final boolean pressed) {
        if (isChordStepModeActive()) {
            selectHeld.set(pressed);
            if (pressed) {
                oled.valueInfo("Select", "Load step");
            } else {
                oled.clearScreenDelayed();
            }
            return;
        }
        if (pressed) {
            sustainActive = !sustainActive;
            noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_SUSTAIN, sustainActive ? MAX_MIDI_VALUE : MIN_MIDI_VALUE);
            oled.valueInfo("Sustain", sustainActive ? "On" : "Off");
        }
    }

    private void handleMute2Button(final boolean pressed) {
        if (isChordStepModeActive()) {
            copyHeld.set(pressed);
            if (pressed) {
                oled.valueInfo("Paste", "Target step");
            } else {
                oled.clearScreenDelayed();
            }
            return;
        }
        if (pressed) {
            sostenutoActive = !sostenutoActive;
            noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_SOSTENUTO,
                    sostenutoActive ? MAX_MIDI_VALUE : MIN_MIDI_VALUE);
            oled.valueInfo("Sostenuto", sostenutoActive ? "On" : "Off");
        }
    }

    private void handleMute3Button(final boolean pressed) {
        if (isChordStepModeActive()) {
            fixedLengthHeld.set(pressed);
            if (pressed) {
                oled.valueInfo("Last Step", "Target step");
            } else {
                oled.clearScreenDelayed();
            }
            return;
        }
        if (pressed) {
            noteRepeatHandler.toggleActive();
        }
    }

    private void handleMute4Button(final boolean pressed) {
        if (isChordStepModeActive()) {
            invertHeld.set(pressed);
            if (pressed) {
                invertCurrentChord(driver.isGlobalAltHeld() ? -1 : 1);
            }
            return;
        }
    }

    private BiColorLightState getMute1LightState() {
        if (isChordStepModeActive()) {
            return selectHeld.get() ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
        }
        return sustainActive ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
    }

    private BiColorLightState getMute2LightState() {
        if (isChordStepModeActive()) {
            return copyHeld.get() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
        }
        return sostenutoActive ? BiColorLightState.AMBER_FULL : BiColorLightState.OFF;
    }

    private BiColorLightState getMute3LightState() {
        if (isChordStepModeActive()) {
            return fixedLengthHeld.get() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
        }
        return noteRepeatHandler.getNoteRepeatActive().get() ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
    }

    private BiColorLightState getMute4LightState() {
        if (isChordStepModeActive()) {
            if (invertHeld.get()) {
                return driver.isGlobalAltHeld() ? BiColorLightState.RED_FULL : BiColorLightState.RED_FULL;
            }
            return driver.isGlobalAltHeld() ? BiColorLightState.RED_HALF : BiColorLightState.RED_FULL;
        }
        return BiColorLightState.OFF;
    }

    private void handleEncoder1Touch(final boolean pressed) {
        handleResettableTouch(2, pressed, () -> showState("Scale"), liveScaleEncoder::reset);
    }

    private void handleEncoder2Touch(final boolean pressed) {
        handleResettableTouch(1, pressed, () -> oled.valueInfo("Velocity", Integer.toString(liveVelocity)),
                liveVelocityEncoder::reset);
    }

    private void handleEncoder3Touch(final boolean pressed) {
        handleResettableTouch(3, pressed, () -> showState("Layout"), liveLayoutEncoder::reset);
    }

    private void handleStepSeqPressed(final boolean pressed) {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            if (driver.isGlobalShiftHeld()) {
                if (pressed) {
                    driver.toggleFillMode();
                    oled.valueInfo("Fill", driver.getFillLightState() == BiColorLightState.AMBER_FULL ? "On" : "Off");
                }
                return;
            }
            if (driver.isGlobalAltHeld()) {
                handleOikordAccentPressed(pressed);
                return;
            }
            if (pressed) {
                driver.enterMelodicStepMode();
            }
            return;
        }
        if (pressed) {
            driver.enterMelodicStepMode();
        }
    }

    private void handlePadPress(final int padIndex, final boolean pressed) {
        if (!noteStepActive) {
            handleLivePadPress(padIndex, pressed);
            return;
        }
        if (currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            handleOikordStepPadPress(padIndex, pressed);
            return;
        }
        handleClipStepRecordPadPress(padIndex, pressed);
    }

    private void handleLivePadPress(final int padIndex, final boolean pressed) {
        if (pressed) {
            heldPads.add(padIndex);
        } else {
            heldPads.remove(padIndex);
        }
    }

    private void handleOikordStepPadPress(final int padIndex, final boolean pressed) {
        if (padIndex < STEP_PAD_OFFSET) {
            if (isBuilderFamily()) {
                handleBuilderSourcePadPress(padIndex, pressed);
                return;
            }
            if (!oikordBank.hasSlot(currentPresetFamilyIndex(), oikordPage, padIndex)) {
                return;
            }
            if (pressed) {
                selectedOikordSlot = padIndex;
                final boolean hasHeldSteps = !heldStepPads.isEmpty();
                final boolean auditionEnabled = driver.isStepSeqPadAuditionEnabled();
                final boolean transportStopped = !driver.isTransportPlaying();
                if (auditionEnabled && (!hasHeldSteps || transportStopped)) {
                    startAuditionSelectedOikord();
                }
                if (hasHeldSteps) {
                    assignSelectedOikordToHeldSteps();
                } else if (!auditionEnabled) {
                    showCurrentOikord();
                }
            } else {
                stopAuditionNotes();
            }
            return;
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
        if (oikordAccentButtonHeld) {
            if (pressed) {
                toggleOikordAccentForStep(stepIndex);
            }
            return;
        }
        if (!pressed && modifierHandledStepPads.remove(stepIndex)) {
            return;
        }
        if (pressed) {
            if (invertHeld.get()) {
                modifierHandledStepPads.add(stepIndex);
                return;
            }
            if (selectHeld.get()) {
                if (isBuilderFamily()) {
                    loadBuilderFromStep(stepIndex);
                } else {
                    selectPresetStep(stepIndex);
                }
                modifierHandledStepPads.add(stepIndex);
                return;
            }
            if (fixedLengthHeld.get()) {
                setLastStep(stepIndex);
                modifierHandledStepPads.add(stepIndex);
                return;
            }
            if (copyHeld.get()) {
                pasteCurrentChordToStep(stepIndex);
                modifierHandledStepPads.add(stepIndex);
                return;
            }
            if (heldStepAnchor != null
                    && heldStepAnchor != stepIndex
                    && heldStepPads.contains(heldStepAnchor)
                    && canExtendHeldOikordRange(heldStepAnchor, stepIndex)
                    && extendHeldOikordRange(heldStepAnchor, stepIndex)) {
                heldStepPads.add(stepIndex);
                modifiedStepPads.add(heldStepAnchor);
                modifiedStepPads.add(stepIndex);
                showExtendedStepInfo(heldStepAnchor, stepIndex);
                return;
            }
            if (heldStepAnchor != null
                    && heldStepAnchor != stepIndex
                    && heldStepPads.contains(heldStepAnchor)
                    && !canExtendHeldOikordRange(heldStepAnchor, stepIndex)) {
                showBlockedStepInfo();
                return;
            }
            heldStepPads.add(stepIndex);
            if (heldStepAnchor == null) {
                heldStepAnchor = stepIndex;
            }
            if (!hasStepStartNote(stepIndex)) {
                final boolean assigned = assignSelectedOikordToSteps(Collections.singleton(stepIndex));
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
            startAuditionSelectedOikord();
        } else {
            showCurrentOikord();
        }
    }

    private void loadBuilderFromStep(final int stepIndex) {
        final Map<Integer, NoteStep> notesAtStep = noteStepsByPosition.get(stepIndex);
        final Set<Integer> fallbackNotes = bestAvailableStepNotes(stepIndex);
        if ((notesAtStep == null || notesAtStep.isEmpty()) && fallbackNotes.isEmpty()) {
            oled.valueInfo("Select", "Empty step");
            return;
        }
        selectedOikordFamily = BUILDER_FAMILY_INDEX;
        oikordPage = 0;
        selectedOikordSlot = 0;
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
        final int[] notes = renderSelectedOikord();
        if (notes.length == 0) {
            oled.valueInfo("Paste", "Empty chord");
            return;
        }
        writeOikordAtStep(stepIndex, notes, currentOikordVelocity(), STEP_LENGTH);
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
        final int coarseStep = Math.floorDiv(x, FINE_STEPS_PER_STEP);
        final Map<Integer, Set<Integer>> notesAtStep =
                observedFineOccupancyByStep.computeIfAbsent(coarseStep, ignored -> new HashMap<>());
        final Set<Integer> occupiedFineSteps = notesAtStep.computeIfAbsent(y, ignored -> new HashSet<>());
        if (state == NoteStep.State.Empty.ordinal()) {
            occupiedFineSteps.remove(x);
            if (occupiedFineSteps.isEmpty()) {
                notesAtStep.remove(y);
            }
            if (notesAtStep.isEmpty()) {
                observedFineOccupancyByStep.remove(coarseStep);
                observedClipNotesByStep.remove(coarseStep);
            } else {
                observedClipNotesByStep.put(coarseStep, new HashSet<>(notesAtStep.keySet()));
            }
            final Map<Integer, Integer> noteStarts = observedFineNoteStartsByStep.get(coarseStep);
            if (noteStarts != null && Integer.valueOf(x).equals(noteStarts.get(y))) {
                noteStarts.remove(y);
                if (noteStarts.isEmpty()) {
                    observedFineNoteStartsByStep.remove(coarseStep);
                }
            }
            return;
        }

        occupiedFineSteps.add(x);
        observedClipNotesByStep.computeIfAbsent(coarseStep, ignored -> new HashSet<>()).add(y);
        if (state == NoteStep.State.NoteOn.ordinal()) {
            observedFineNoteStartsByStep.computeIfAbsent(coarseStep, ignored -> new HashMap<>()).put(y, x);
        }
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
        final int[] currentNotes = renderSelectedOikord();
        if (currentNotes.length == 0) {
            oled.valueInfo("Invert", "Empty");
            return;
        }
        selectedOikordFamily = BUILDER_FAMILY_INDEX;
        oikordPage = 0;
        selectedOikordSlot = 0;
        final int[] inverted = rotateChordInversion(currentNotes, direction);
        builderSelectedNotes.clear();
        for (final int midiNote : inverted) {
            builderSelectedNotes.add(midiNote);
        }
        applyBuilderToHeldSteps();
        oled.valueInfo("Invert", direction > 0 ? "Up" : "Down");
    }

    private int[] rotateChordInversion(final int[] notes, final int direction) {
        final int[] sorted = java.util.Arrays.stream(notes).sorted().toArray();
        if (sorted.length <= 1) {
            return sorted;
        }
        final int[] rotated = java.util.Arrays.copyOf(sorted, sorted.length);
        if (direction >= 0) {
            final int first = rotated[0];
            System.arraycopy(rotated, 1, rotated, 0, rotated.length - 1);
            rotated[rotated.length - 1] = first + 12;
        } else {
            final int last = rotated[rotated.length - 1];
            System.arraycopy(rotated, 0, rotated, 1, rotated.length - 1);
            rotated[0] = last - 12;
        }
        java.util.Arrays.sort(rotated);
        return rotated;
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
        observedClipNotesByStep.keySet().stream()
                .filter(this::isVisibleGlobalStep)
                .map(this::globalToLocalStep)
                .forEach(occupiedSteps::add);
        return occupiedSteps;
    }

    private Set<Integer> getVisibleStartedSteps() {
        final Set<Integer> startedSteps = observedFineNoteStartsByStep.keySet().stream()
                .filter(this::isVisibleGlobalStep)
                .map(this::globalToLocalStep)
                .collect(Collectors.toCollection(HashSet::new));
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
        final int oldGlobalStep = Math.floorDiv(oldFineStart, FINE_STEPS_PER_STEP);
        final int newGlobalStep = Math.floorDiv(newFineStart, FINE_STEPS_PER_STEP);
        final int occupiedFineSteps = Math.max(1, (int) Math.round(duration / FINE_STEP_LENGTH));
        for (int offset = 0; offset < occupiedFineSteps; offset++) {
            final int oldFineStep = oldFineStart + offset;
            final int oldStep = Math.floorDiv(oldFineStep, FINE_STEPS_PER_STEP);
            final Map<Integer, Set<Integer>> oldOccupancy = observedFineOccupancyByStep.get(oldStep);
            if (oldOccupancy != null) {
                final Set<Integer> occupied = oldOccupancy.get(midiNote);
                if (occupied != null) {
                    occupied.remove(oldFineStep);
                    if (occupied.isEmpty()) {
                        oldOccupancy.remove(midiNote);
                    }
                }
                if (oldOccupancy.isEmpty()) {
                    observedFineOccupancyByStep.remove(oldStep);
                    observedClipNotesByStep.remove(oldStep);
                } else {
                    observedClipNotesByStep.put(oldStep, new HashSet<>(oldOccupancy.keySet()));
                }
            }
        }
        final Map<Integer, Integer> oldStarts = observedFineNoteStartsByStep.get(oldGlobalStep);
        if (oldStarts != null && Integer.valueOf(oldFineStart).equals(oldStarts.get(midiNote))) {
            oldStarts.remove(midiNote);
            if (oldStarts.isEmpty()) {
                observedFineNoteStartsByStep.remove(oldGlobalStep);
            }
        }
        for (int offset = 0; offset < occupiedFineSteps; offset++) {
            final int newFineStep = newFineStart + offset;
            final int newStep = Math.floorDiv(newFineStep, FINE_STEPS_PER_STEP);
            observedFineOccupancyByStep
                    .computeIfAbsent(newStep, ignored -> new HashMap<>())
                    .computeIfAbsent(midiNote, ignored -> new HashSet<>())
                    .add(newFineStep);
            observedClipNotesByStep.computeIfAbsent(newStep, ignored -> new HashSet<>()).add(midiNote);
        }
        observedFineNoteStartsByStep.computeIfAbsent(newGlobalStep, ignored -> new HashMap<>()).put(midiNote, newFineStart);
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
        noteStarts.putAll(observedFineNoteStartsByStep.getOrDefault(globalStep, Map.of()));
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
                            ? currentOikordVelocity()
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
        final Map<Integer, Integer> observedStarts = observedFineNoteStartsByStep.get(globalStep);
        if (observedStarts != null && observedStarts.containsKey(midiNote)) {
            return observedStarts.get(midiNote);
        }
        return localToGlobalStep(fallbackLocalStep) * FINE_STEPS_PER_STEP;
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
        final int coarseStep = Math.floorDiv(fineStep, FINE_STEPS_PER_STEP);
        return observedFineOccupancyByStep
                .getOrDefault(coarseStep, Map.of())
                .getOrDefault(midiNote, Set.of())
                .contains(fineStep);
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

    private boolean canExtendHeldOikordRange(final int anchorStepIndex, final int targetStepIndex) {
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

    private boolean extendHeldOikordRange(final int anchorStepIndex, final int targetStepIndex) {
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
                writeSelectedOikordAtStep(anchorStepIndex, duration);
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
            writeSelectedOikordAtStep(startStep, duration);
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
        if (currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            refreshChordStepObservation();
            ensureBuilderSeededIfEmpty();
            if (chordPageCount() > 1) {
                showChordPageInfo();
            } else {
                showCurrentOikord();
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
        final boolean useStepEncoders = noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP;
        if (useStepEncoders) {
            deactivateLiveEncoderLayers();
            liveModeControlLayer.deactivate();
            stepEncoderLayer.activate();
        } else {
            stepEncoderLayer.deactivate();
            activateCurrentLiveEncoderLayer();
            liveModeControlLayer.activate();
        }
    }

    private void deactivateLiveEncoderLayers() {
        liveChannelLayer.deactivate();
        liveMixerLayer.deactivate();
        liveUser1Layer.deactivate();
        liveUser2Layer.deactivate();
    }

    private void activateCurrentLiveEncoderLayer() {
        deactivateLiveEncoderLayers();
        applyLiveEncoderStepSizes(liveEncoderMode);
        currentLiveEncoderLayer = liveLayerForMode(liveEncoderMode);
        currentLiveEncoderLayer.activate();
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

    private Layer liveLayerForMode(final EncoderMode mode) {
        return switch (mode) {
            case CHANNEL -> liveChannelLayer;
            case MIXER -> liveMixerLayer;
            case USER_1 -> liveUser1Layer;
            case USER_2 -> liveUser2Layer;
        };
    }

    private void assignSelectedOikordToHeldSteps() {
        modifiedStepPads.addAll(heldStepPads);
        assignSelectedOikordToSteps(heldStepPads);
    }

    private boolean assignSelectedOikordToSteps(final Set<Integer> stepIndexes) {
        if (stepIndexes.isEmpty()) {
            return false;
        }
        if (!ensureSelectedNoteClip()) {
            return false;
        }
        final int[] notes = renderSelectedOikord();
        if (notes.length == 0) {
            oled.valueInfo("Select", "Notes 1st");
            return false;
        }
        for (final int stepIndex : stepIndexes) {
            writeOikordAtStep(stepIndex, notes, currentOikordVelocity(), STEP_LENGTH);
        }
        oled.valueInfo(currentOikordFamilyLabel(), currentOikordName());
        return true;
    }

    private void writeSelectedOikordAtStep(final int stepIndex, final double duration) {
        final int[] notes = renderSelectedOikord();
        writeOikordAtStep(stepIndex, notes, currentOikordVelocity(), duration);
    }

    private void writeOikordAtStep(final int stepIndex, final int[] notes, final int velocity, final double duration) {
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
                observedFineNoteStartsByStep.getOrDefault(localToGlobalStep(stepIndex), Map.of());
        final int fineStart = observedStarts.getOrDefault(midiNote, localToGlobalFineStep(stepIndex));
        observedNoteClip.clearStep(fineStart, midiNote);
        invalidateObservedChordStep(stepIndex);
        queueChordObservationResync();
    }

    private void invalidateObservedChordStep(final int stepIndex) {
        final int globalStep = localToGlobalStep(stepIndex);
        observedClipNotesByStep.remove(globalStep);
        observedFineOccupancyByStep.remove(globalStep);
        observedFineNoteStartsByStep.remove(globalStep);
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
            writeSelectedOikordAtStep(stepIndex, getStepDuration(stepIndex));
        }
    }

    private double getStepDuration(final int stepIndex) {
        return noteStepsByPosition.getOrDefault(stepIndex, Map.of()).values().stream()
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .mapToDouble(NoteStep::duration)
                .max()
                .orElse(STEP_LENGTH);
    }

    private void adjustOikordPage(final int amount) {
        if (amount == 0) {
            return;
        }
        if (isBuilderFamily()) {
            return;
        }
        final int nextPage = Math.max(0, Math.min(currentOikordPageCount() - 1, oikordPage + amount));
        if (nextPage == oikordPage) {
            return;
        }
        oikordPage = nextPage;
        ensureSelectedOikordSlotValid();
        showCurrentOikord();
    }

    private void adjustOikordFamily(final int amount) {
        if (amount == 0) {
            return;
        }
        final int familyCount = oikordFamilyCount();
        final int nextFamily = Math.max(0, Math.min(familyCount - 1, selectedOikordFamily + amount));
        if (nextFamily == selectedOikordFamily) {
            return;
        }
        selectedOikordFamily = nextFamily;
        oikordPage = 0;
        selectedOikordSlot = 0;
        ensureSelectedOikordSlotValid();
        showCurrentOikord();
    }

    private void adjustOikordRoot(final int amount) {
        if (amount == 0) {
            return;
        }
        final int nextOffset = Math.max(MIN_OIKORD_ROOT_OFFSET,
                Math.min(MAX_OIKORD_ROOT_OFFSET, oikordRootOffset + amount));
        if (nextOffset == oikordRootOffset) {
            return;
        }
        oikordRootOffset = nextOffset;
        showOikordRootInfo();
    }

    private void resetOikordRoot() {
        oikordRootOffset = 0;
    }

    private void adjustOikordOctave(final int amount) {
        if (amount == 0) {
            return;
        }
        final int nextOffset = Math.max(MIN_OIKORD_OCTAVE_OFFSET,
                Math.min(MAX_OIKORD_OCTAVE_OFFSET, oikordOctaveOffset + amount));
        if (nextOffset == oikordOctaveOffset) {
            return;
        }
        oikordOctaveOffset = nextOffset;
        showOikordOctaveInfo();
    }

    private void resetOikordOctave() {
        oikordOctaveOffset = 0;
    }

    private void toggleOikordInterpretation() {
        oikordInterpretation = oikordInterpretation == OikordInterpretation.AS_IS
                ? OikordInterpretation.CAST
                : OikordInterpretation.AS_IS;
        oled.valueInfo("Chord Step Mode", oikordInterpretation.displayName());
        driver.notifyPopup("Chord Step Mode", oikordInterpretation.displayName());
    }

    private void adjustOikordInterpretation(final int amount) {
        if (amount == 0) {
            return;
        }
        if (amount > 0 && oikordInterpretation == OikordInterpretation.AS_IS) {
            toggleOikordInterpretation();
            return;
        }
        if (amount < 0 && oikordInterpretation == OikordInterpretation.CAST) {
            toggleOikordInterpretation();
        }
    }

    private void startAuditionSelectedOikord() {
        final int[] notes = renderSelectedOikord();
        stopAuditionNotes();
        for (final int midiNote : notes) {
            noteInput.sendRawMidiEvent(Midi.NOTE_ON, midiNote, AUDITION_VELOCITY);
            auditioningNotes.add(midiNote);
        }
        oled.valueInfo(currentOikordFamilyLabel(), currentOikordName());
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

    private void handleLiveModeAdvance(final boolean pressed) {
        if (!pressed || noteStepActive) {
            return;
        }
        liveEncoderMode = switch (liveEncoderMode) {
            case CHANNEL -> EncoderMode.MIXER;
            case MIXER -> EncoderMode.USER_1;
            case USER_1 -> EncoderMode.USER_2;
            case USER_2 -> EncoderMode.CHANNEL;
        };
        activateCurrentLiveEncoderLayer();
        oled.detailInfo("Encoder Mode", getLiveModeInfo(liveEncoderMode));
        oled.clearScreenDelayed();
    }

    private BiColorLightState getLiveModeLightState() {
        return liveEncoderMode.getState();
    }

    private void handlePitchContextButton(final boolean pressed, final int amount, final boolean root) {
        if (!pressed) {
            return;
        }
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            if (root) {
                adjustOikordRoot(amount);
            } else {
                adjustOikordOctave(amount);
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

    private void handleResettableTouch(final int encoderIndex, final boolean touched,
                                       final Runnable showInfo, final Runnable resetAction) {
        if (touched) {
            if (driver.isEncoderTouchResetEnabled()) {
                touchResetGesture.onTouchStart(encoderIndex);
                driver.getHost().scheduleTask(() -> {
                    if (driver.isEncoderTouchResetEnabled() && touchResetGesture.shouldResetWhileTouched(encoderIndex)) {
                        resetAction.run();
                        showInfo.run();
                    }
                }, TOUCH_RESET_HOLD_MS);
            }
            showInfo.run();
            return;
        }
        if (driver.isEncoderTouchResetEnabled()) {
            touchResetGesture.onTouchEnd(encoderIndex);
        }
        oled.clearScreenDelayed();
    }

    private void markEncoderAdjusted(final int encoderIndex) {
        if (driver.isEncoderTouchResetEnabled()) {
            touchResetGesture.onAdjusted(encoderIndex, 1);
        }
    }

    private BiColorLightState getBankLightState() {
        if (noteStepActive) {
            return BiColorLightState.HALF;
        }
        return BiColorLightState.HALF;
    }

    private BiColorLightState getPitchContextLightState(final int amount, final boolean root) {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            if (root) {
                return amount < 0
                        ? (oikordRootOffset > MIN_OIKORD_ROOT_OFFSET ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF)
                        : (oikordRootOffset < MAX_OIKORD_ROOT_OFFSET ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF);
            }
            return amount < 0
                    ? (oikordOctaveOffset > MIN_OIKORD_OCTAVE_OFFSET ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF)
                    : (oikordOctaveOffset < MAX_OIKORD_OCTAVE_OFFSET ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF);
        }
        if (root) {
            return amount < 0
                    ? (transposeBase > MIN_TRANSPOSE ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF)
                    : (transposeBase < MAX_TRANSPOSE ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF);
        }
        return amount < 0
                ? (getOctave() > MIN_OCTAVE ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF)
                : (getOctave() < MAX_OCTAVE ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF);
    }

    private BiColorLightState getStepSeqLightState() {
        if (!(noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP)) {
            return BiColorLightState.OFF;
        }
        if (driver.isGlobalShiftHeld()) {
            return driver.getFillLightState();
        }
        if (driver.isGlobalAltHeld()) {
            return oikordAccentActive ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
        }
        return BiColorLightState.GREEN_HALF;
    }

    private void handleOikordAccentPressed(final boolean pressed) {
        oikordAccentButtonHeld = pressed;
        if (!pressed) {
            if (!oikordAccentModified) {
                oikordAccentActive = !oikordAccentActive;
                oled.valueInfo("Accent", oikordAccentActive ? "On" : "Off");
            } else {
                oled.clearScreenDelayed();
            }
            oikordAccentModified = false;
            return;
        }
        oled.valueInfo("Accent", oikordAccentActive ? "On" : "Off");
    }

    private int currentOikordVelocity() {
        return oikordAccentActive ? DEFAULT_OIKORD_ACCENTED_VELOCITY : defaultOikordVelocity;
    }

    private void toggleOikordAccentForStep(final int stepIndex) {
        final Map<Integer, NoteStep> notesAtStep = noteStepsByPosition.get(stepIndex);
        if (notesAtStep == null || notesAtStep.isEmpty()) {
            return;
        }
        final boolean accented = notesAtStep.values().stream().allMatch(this::isOikordAccented);
        final double targetVelocity = (accented ? defaultOikordVelocity : DEFAULT_OIKORD_ACCENTED_VELOCITY) / 127.0;
        notesAtStep.values().forEach(note -> note.setVelocity(targetVelocity));
        oikordAccentModified = true;
        oled.valueInfo("Accent", accented ? "Normal" : "Accented");
    }

    private boolean isOikordAccented(final NoteStep noteStep) {
        final int velocity = (int) Math.round(noteStep.velocity() * 127);
        final int distanceToAccent = Math.abs(velocity - DEFAULT_OIKORD_ACCENTED_VELOCITY);
        final int distanceToStandard = Math.abs(velocity - DEFAULT_OIKORD_STANDARD_VELOCITY);
        return distanceToAccent <= distanceToStandard;
    }

    private boolean isOikordStepAccented(final int stepIndex) {
        final Map<Integer, NoteStep> notesAtStep = noteStepsByPosition.get(stepIndex);
        return notesAtStep != null
                && !notesAtStep.isEmpty()
                && notesAtStep.values().stream()
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .allMatch(this::isOikordAccented);
    }

    private void toggleLayout() {
        applyLayoutChange(() -> {
            inKey = !inKey;
            if (inKey && scaleIndex == PIANO_HIGHLIGHT_INDEX) {
                scaleIndex = 1;
            }
        });
        showState("Layout");
    }

    private void adjustLayout(final int amount) {
        if (amount == 0) {
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

    public void togglePrimarySurface() {
        if (noteStepActive) {
            returnToLivePlay();
            driver.notifyAction("Mode", "Note");
            return;
        }
        currentStepSubMode = NoteStepSubMode.OIKORD_STEP;
        noteStepActive = true;
        enterCurrentStepSubMode();
        driver.notifyPopup("Mode", currentStepSubMode.displayName());
    }

    public void toggleCurrentSurfaceVariant() {
        if (noteStepActive) {
            if (currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
                toggleBuilderLayout();
            } else {
                driver.notifyAction("Step Mode", currentStepSubMode.displayName());
            }
            return;
        }
        toggleLayout();
    }

    private void toggleBuilderLayout() {
        builderInKey = !builderInKey;
        if (builderInKey && scaleIndex == PIANO_HIGHLIGHT_INDEX) {
            scaleIndex = 1;
        }
        oled.valueInfo("Builder Layout", builderInKey ? "In Key" : "Chromatic");
        driver.notifyPopup("Builder Layout", builderInKey ? "In Key" : "Chromatic");
    }

    public BiColorLightState getModeButtonLightState() {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            return BiColorLightState.GREEN_FULL;
        }
        return inKey ? BiColorLightState.RED_FULL : BiColorLightState.AMBER_FULL;
    }

    public boolean isStepSurfaceActive() {
        return noteStepActive;
    }

    private void adjustScale(final int amount) {
        if (amount == 0) {
            return;
        }
        final int minScale = inKey ? 1 : PIANO_HIGHLIGHT_INDEX;
        final int nextScale = scaleIndex + amount;
        if (nextScale < minScale || nextScale >= scaleLibrary.getMusicalScalesCount()) {
            return;
        }
        applyLayoutChange(() -> scaleIndex = nextScale);
        showState("Scale");
    }

    private void adjustOctave(final int amount) {
        if (amount == 0) {
            return;
        }
        final int nextOctave = Math.max(MIN_OCTAVE, Math.min(MAX_OCTAVE, getOctave() + amount));
        if (nextOctave == getOctave()) {
            return;
        }
        applyLayoutChange(() -> transposeBase = nextOctave * 12 + getRootNote());
        showState("Octave");
    }

    private void adjustTransposeSemitone(final int amount) {
        if (amount == 0) {
            return;
        }
        final int nextBase = transposeBase + amount;
        if (nextBase < MIN_TRANSPOSE || nextBase > MAX_TRANSPOSE) {
            return;
        }
        applyLayoutChange(() -> transposeBase = nextBase);
        showState("Root");
    }

    private void handleMainEncoder(final int inc) {
        if (driver.isPopupBrowserActive()) {
            return;
        }
        driver.markMainEncoderTurned();
        if (!noteStepActive && noteRepeatHandler.getNoteRepeatActive().get()) {
            noteRepeatHandler.handleMainEncoder(inc, driver.isGlobalAltHeld());
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
        } else {
            driver.adjustMainCursorParameter(inc, fine);
        }
    }

    private void handleMainEncoderPress(final boolean pressed) {
        if (driver.isPopupBrowserActive()) {
            return;
        }
        driver.setMainEncoderPressed(pressed);
        if (!noteStepActive && noteRepeatHandler.getNoteRepeatActive().get()) {
            noteRepeatHandler.handlePressed(pressed);
            return;
        }
        if (pressed && driver.isGlobalShiftHeld()) {
            mainEncoderPressConsumed = true;
            driver.cycleMainEncoderRolePreference();
            return;
        }
        if (!pressed && mainEncoderPressConsumed) {
            mainEncoderPressConsumed = false;
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
        } else if (pressed) {
            driver.showMainCursorParameterInfo();
        }
    }

    private void applyLayout() {
        final NoteGridLayout layout = createLayout();
        for (int i = 0; i < noteTranslationTable.length; i++) {
            noteTranslationTable[i] = -1;
        }
        for (int padIndex = 0; padIndex < NoteGridLayout.PAD_COUNT; padIndex++) {
            final int translatedNote = applyLivePitchOffset(layout.noteForPad(padIndex));
            noteTranslationTable[0x36 + padIndex] = translatedNote;
        }
        noteInput.setKeyTranslationTable(noteTranslationTable);
    }

    private void applyLayoutChange(final Runnable stateChange) {
        if (noteStepActive) {
            stateChange.run();
            showContextInfo();
            return;
        }
        final Map<Integer, Integer> oldHeldNotes = collectHeldNotes(createLayout());
        sendHeldNotes(oldHeldNotes, false);
        stateChange.run();
        applyLayout();
        sendHeldNotes(collectHeldNotes(createLayout()), true);
    }

    private Map<Integer, Integer> collectHeldNotes(final NoteGridLayout layout) {
        final Map<Integer, Integer> heldNotes = new HashMap<>();
        for (final int padIndex : heldPads) {
            final int midiNote = applyLivePitchOffset(layout.noteForPad(padIndex));
            if (midiNote >= 0) {
                heldNotes.merge(midiNote, 1, Integer::sum);
            }
        }
        return heldNotes;
    }

    private void sendHeldNotes(final Map<Integer, Integer> notes, final boolean noteOn) {
        final int status = noteOn ? Midi.NOTE_ON : Midi.NOTE_OFF;
        final int velocity = noteOn ? HELD_NOTE_VELOCITY : 0;
        for (final int midiNote : notes.keySet()) {
            noteInput.sendRawMidiEvent(status, midiNote, velocity);
        }
    }

    private void releaseHeldLiveNotes() {
        sendHeldNotes(collectHeldNotes(createLayout()), false);
        soundingLiveNotesByPad.clear();
        heldPads.clear();
        stopAuditionNotes();
    }

    private RgbLigthState getPadLight(final int padIndex) {
        if (!noteStepActive) {
            return getLivePadLight(padIndex);
        }
        if (currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            return getOikordStepPadLight(padIndex);
        }
        return getClipStepRecordPadLight(padIndex);
    }

    private RgbLigthState getLivePadLight(final int padIndex) {
        final NoteGridLayout layout = createLayout();
        final int midiNote = applyLivePitchOffset(layout.noteForPad(padIndex));
        final RgbLigthState base;
        if (midiNote < 0) {
            base = RgbLigthState.OFF;
        } else if (!inKey && scaleIndex == PIANO_HIGHLIGHT_INDEX) {
            if (layout.roleForPad(padIndex) == NoteGridLayout.PadRole.ROOT) {
                base = ROOT_COLOR;
            } else if (NoteGridLayout.isBlackKey(midiNote)) {
                base = PIANO_BLACK_KEY_COLOR;
            } else {
                base = PIANO_WHITE_KEY_COLOR;
            }
        } else {
            final NoteGridLayout.PadRole role = layout.roleForPad(padIndex);
            base = switch (role) {
                case ROOT -> ROOT_COLOR;
                case IN_SCALE -> IN_SCALE_COLOR;
                case OUT_OF_SCALE -> OUT_OF_SCALE_COLOR;
                case UNAVAILABLE -> RgbLigthState.OFF;
            };
        }
        return heldPads.contains(padIndex) ? base.getBrightest() : base;
    }

    private RgbLigthState getOikordStepPadLight(final int padIndex) {
        if (padIndex < STEP_PAD_OFFSET) {
            if (isBuilderFamily()) {
                return getBuilderSourcePadLight(padIndex);
            }
            if (!oikordBank.hasSlot(currentPresetFamilyIndex(), oikordPage, padIndex)) {
                return RgbLigthState.OFF;
            }
            final OikordBank.Slot slot = oikordBank.slot(currentPresetFamilyIndex(), oikordPage, padIndex);
            final int groupIndex = padIndex / 8;
            final RgbLigthState grouped = getFamilyGroupColor(slot.family(), groupIndex, oikordPage,
                    currentOikordPageCount());
            return padIndex == selectedOikordSlot ? SELECTED_CHORD : grouped.getDimmed();
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
        final boolean occupied = hasVisibleStepContent(stepIndex);
        final boolean accented = occupied && isOikordStepAccented(stepIndex);
        final boolean sustained = !occupied && isOikordStepSustained(stepIndex);
        final RgbLigthState occupiedStepColor = getOikordOccupiedStepColor();
        final RgbLigthState sustainedStepColor = getOikordSustainedStepColor();
        if (heldStepPads.contains(stepIndex)) {
            return HELD_STEP.getBrightest();
        }
        if (occupied) {
            return StepPadLightHelper.renderOccupiedStep(occupiedStepColor, accented, stepIndex == playingStep);
        }
        if (sustained) {
            return stepIndex == playingStep ? sustainedStepColor.getBrightend() : sustainedStepColor;
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

    private RgbLigthState getOikordOccupiedStepColor() {
        if (selectedNoteClipColor != null) {
            return selectedNoteClipColor;
        }
        return oikordStepBaseColor != null ? oikordStepBaseColor : OCCUPIED_STEP;
    }

    private RgbLigthState getOikordSustainedStepColor() {
        return getOikordOccupiedStepColor().getVeryDimmed();
    }

    private boolean isOikordStepSustained(final int stepIndex) {
        return hasVisibleStepContent(stepIndex) && !hasStepStartNote(stepIndex);
    }

    private RgbLigthState getClipStepRecordPadLight(final int padIndex) {
        if (padIndex < STEP_PAD_OFFSET) {
            return DEFERRED_TOP;
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
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
            oled.valueInfo("Layout", inKey ? "In Key" : "Chromatic");
            return;
        }
        if ("Interpretation".equals(focus) && noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            oled.valueInfo("Chord Step Mode", oikordInterpretation.displayName());
            return;
        }
        oled.lineInfo("Root %s%d".formatted(NoteGridLayout.noteName(getRootNote()), getOctave()),
                noteStepActive
                        ? "Step: %s\n%s".formatted(currentStepSubMode.displayName(),
                        currentStepSubMode == NoteStepSubMode.OIKORD_STEP ? currentOikordDisplay() : "Deferred")
                        : "Scale: %s\n%s".formatted(getScaleDisplayName(), inKey ? "In Key" : "Chromatic"));
    }

    private void showContextInfo() {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            showCurrentOikord();
            return;
        }
        if (noteStepActive) {
            oled.valueInfo("Step Mode", currentStepSubMode.displayName());
            return;
        }
        showState("Mode");
    }

    private void showCurrentOikord() {
        oled.valueInfo("%s %d/%d".formatted(currentOikordFamilyLabel(), oikordPage + 1,
                        currentOikordPageCount()),
                "%s %s".formatted(currentOikordName(), oikordInterpretationSuffix()));
    }

    private String currentOikordDisplay() {
        return "%s %s".formatted(currentOikordName(), oikordInterpretationSuffix());
    }

    private String oledOikordName(final OikordBank.Slot slot) {
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

    private String oikordInterpretationSuffix() {
        return "F%d %s R%s O%s".formatted(selectedOikordFamily + 1,
                oikordInterpretation == OikordInterpretation.AS_IS ? "Raw" : "Cast",
                formatSignedValue(oikordRootOffset),
                formatSignedValue(oikordOctaveOffset));
    }

    private OikordBank.Slot currentOikordSlot() {
        if (isBuilderFamily()) {
            throw new IllegalStateException("Builder source has no preset slot");
        }
        ensureSelectedOikordSlotValid();
        return oikordBank.slot(currentPresetFamilyIndex(), oikordPage, selectedOikordSlot);
    }

    private void ensureSelectedOikordSlotValid() {
        if (isBuilderFamily()) {
            oikordPage = 0;
            selectedOikordSlot = 0;
            return;
        }
        if (oikordBank.hasSlot(currentPresetFamilyIndex(), oikordPage, selectedOikordSlot)) {
            return;
        }
        final int pageStart = oikordPage * OikordBank.PAGE_SIZE;
        final int familySlotCount = oikordBank.family(currentPresetFamilyIndex()).slots().size();
        if (pageStart >= familySlotCount) {
            oikordPage = Math.max(0, currentOikordPageCount() - 1);
        }
        selectedOikordSlot = 0;
        while (selectedOikordSlot < OikordBank.PAGE_SIZE
                && !oikordBank.hasSlot(currentPresetFamilyIndex(), oikordPage, selectedOikordSlot)) {
            selectedOikordSlot++;
        }
        if (selectedOikordSlot >= OikordBank.PAGE_SIZE) {
            selectedOikordSlot = 0;
        }
    }

    private int[] renderSelectedOikord() {
        if (isBuilderFamily()) {
            return renderBuilderOikord();
        }
        final OikordBank.Slot slot = currentOikordSlot();
        if (oikordInterpretation == OikordInterpretation.CAST) {
            final int shiftedRoot = getRootNote() + oikordRootOffset;
            final int castRoot = Math.floorMod(shiftedRoot, 12);
            final int castOctaveOffset = Math.floorDiv(shiftedRoot, 12) + oikordOctaveOffset;
            return transpose(oikordBank.renderCast(currentPresetFamilyIndex(), oikordPage, selectedOikordSlot, getScale(), castRoot),
                    castOctaveOffset * 12);
        }
        return oikordBank.renderAsIs(currentPresetFamilyIndex(), oikordPage, selectedOikordSlot,
                getOikordRootMidi() + oikordRootOffset + oikordOctaveOffset * 12);
    }

    private int[] renderBuilderOikord() {
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
        final int builderRoot = firstVisibleNote >= 0
                ? Math.floorMod(firstVisibleNote, 12)
                : Math.floorMod(getRootNote() + oikordRootOffset, 12);
        for (int padIndex = 0; padIndex < STEP_PAD_OFFSET; padIndex++) {
            final int midiNote = getBuilderRenderedNoteMidiForPad(padIndex);
            if (midiNote >= 0 && getScale().isRootMidiNote(builderRoot, midiNote)) {
                builderSelectedNotes.add(midiNote);
                return;
            }
        }
        builderSelectedNotes.add(getOikordRootMidi() + oikordRootOffset + oikordOctaveOffset * 12);
    }

    private int getOikordRootMidi() {
        return (OikordBank.MID_REGISTER_OCTAVE + 1) * 12 + getRootNote();
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

    private void showOikordRootInfo() {
        final int noteClass = Math.floorMod(getRootNote() + oikordRootOffset, 12);
        oled.valueInfo("Chord Root", "%s %s".formatted(NoteGridLayout.noteName(noteClass), formatSignedValue(oikordRootOffset)));
    }

    private boolean isBuilderFamily() {
        return selectedOikordFamily == BUILDER_FAMILY_INDEX;
    }

    private int currentPresetFamilyIndex() {
        return selectedOikordFamily - 1;
    }

    private int oikordFamilyCount() {
        return oikordBank.families().size() + 1;
    }

    private int currentOikordPageCount() {
        return isBuilderFamily() ? 1 : oikordBank.pageCount(currentPresetFamilyIndex());
    }

    private String currentOikordFamilyLabel() {
        return isBuilderFamily()
                ? BUILDER_FAMILY_LABEL
                : oledFamilyLabel(oikordBank.family(currentPresetFamilyIndex()).family());
    }

    private String currentOikordName() {
        if (isBuilderFamily()) {
            return builderSelectedNotes.isEmpty() ? "Empty" : builderSelectionSummary();
        }
        return oledOikordName(currentOikordSlot());
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
        if (padIndex < 0 || padIndex >= STEP_PAD_OFFSET) {
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
                : Math.floorMod(getRootNote() + oikordRootOffset, 12);
        final RgbLigthState base;
        if (midiNote < 0) {
            base = RgbLigthState.OFF;
        } else if (!builderInKey && scaleIndex == PIANO_HIGHLIGHT_INDEX) {
            if (getScale().isRootMidiNote(builderRoot, midiNote)) {
                base = ROOT_COLOR;
            } else if (NoteGridLayout.isBlackKey(midiNote)) {
                base = PIANO_BLACK_KEY_COLOR;
            } else {
                base = PIANO_WHITE_KEY_COLOR;
            }
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
        return heldPads.contains(padIndex) ? base.getBrightest() : base;
    }

    private void showOikordOctaveInfo() {
        oled.valueInfo("Chord Oct", formatSignedValue(oikordOctaveOffset));
    }

    private void showOikordFamilyInfo() {
        if (isBuilderFamily()) {
            oled.valueInfo("Chord Family", BUILDER_FAMILY_LABEL);
            return;
        }
        final OikordBank.Family family = oikordBank.family(currentPresetFamilyIndex());
        oled.valueInfo("Chord Family", family.family());
    }

    private void resetOikordFamilySelection() {
        selectedOikordFamily = BUILDER_FAMILY_INDEX;
        oikordPage = 0;
        selectedOikordSlot = 0;
        ensureSelectedOikordSlotValid();
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
        final int effectiveScaleIndex = scaleIndex == PIANO_HIGHLIGHT_INDEX ? 1 : scaleIndex;
        return scaleLibrary.getMusicalScale(effectiveScaleIndex);
    }

    public MusicalScale getCurrentScale() {
        return getScale();
    }

    private String getScaleDisplayName() {
        if (scaleIndex == PIANO_HIGHLIGHT_INDEX) {
            return "Piano";
        }
        return switch (getScale().getName()) {
            case "Ionan (Major)" -> "Major";
            case "Aeolian (Minor)" -> "Minor";
            case "Phrygian Dominant" -> "Phryg Dom";
            case "Double Harmonic" -> "Dbl Harm";
            case "Harmonic Minor" -> "Harm Min";
            case "Melodic Minor (ascending)" -> "Mel Min";
            case "Hungarian Minor" -> "Hung Min";
            case "Ukranian Dorian" -> "Ukr Dor";
            case "Super Locrian" -> "Sup Loc";
            case "Half-Whole Diminished" -> "Half-Whole";
            case "Major Pentatonic" -> "Maj Pent";
            case "Minor Pentatonic" -> "Min Pent";
            case "Major Blues" -> "Maj Blues";
            case "Whole Tone" -> "Whole";
            case "Whole Half" -> "WholeHalf";
            case "BeBop Major" -> "Bebop Maj";
            case "BeBop Dorian" -> "Bebop Dor";
            case "BeBop Mixolydian" -> "Bebop Mix";
            case "BeBop Minor" -> "Bebop Min";
            default -> getScale().getName();
        };
    }

    private int getRootNote() {
        return Math.floorMod(transposeBase, 12);
    }

    public int getCurrentRootNoteClass() {
        return getRootNote();
    }

    private int getOctave() {
        return transposeBase / 12;
    }

    public int getCurrentOctave() {
        return getOctave();
    }

    public int getCurrentBaseMidiNote() {
        return transposeBase;
    }

    private NoteGridLayout createLayout() {
        return new NoteGridLayout(getScale(), getRootNote(), getOctave(), inKey);
    }

    private int getBuilderFirstVisibleMidiNote() {
        final int firstVisible = applyLivePitchOffset(getOikordRootMidi() + oikordRootOffset + oikordOctaveOffset * 12);
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
        final int shifted = midiNote + getLivePitchOffset();
        return shifted >= 0 && shifted <= 127 ? shifted : -1;
    }

    private void clearTranslation() {
        heldPads.clear();
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
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            if (!driver.isGlobalAltHeld()) {
                pageChordSteps(-1);
            }
            return;
        }
        handlePitchContextButton(true, 1, driver.isGlobalShiftHeld());
    }

    private void handlePatternDown(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            if (!driver.isGlobalAltHeld()) {
                pageChordSteps(1);
            }
            return;
        }
        handlePitchContextButton(true, -1, driver.isGlobalShiftHeld());
    }

    private BiColorLightState getPatternUpLight() {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            return chordStepPosition.canScrollLeft().get() ? BiColorLightState.GREEN_HALF : BiColorLightState.OFF;
        }
        return BiColorLightState.GREEN_HALF;
    }

    private BiColorLightState getPatternDownLight() {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            return chordStepPosition.canScrollRight().get() ? BiColorLightState.GREEN_HALF : BiColorLightState.OFF;
        }
        return BiColorLightState.GREEN_HALF;
    }

    @Override
    public boolean isSelectHeld() {
        return selectHeld.get();
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
                currentOikordDisplay());
    }

    @Override
    public BooleanValueObject getLengthDisplay() {
        return lengthDisplay;
    }

    @Override
    public BooleanValueObject getDeleteHeld() {
        return deleteHeld;
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
        MixerEncoderProfile.adjustParameter(parameter, driver.isGlobalShiftHeld(), inc);
        oled.valueInfo(fallbackLabel, parameter.displayedValue().get());
    }

    @Override
    public EncoderBankLayout getEncoderBankLayout() {
        return stepEncoderBankLayout;
    }

    private EncoderBankLayout createStepEncoderBankLayout() {
        final Map<EncoderMode, EncoderBank> banks = new EnumMap<>(EncoderMode.class);
        banks.put(EncoderMode.CHANNEL, new EncoderBank(
                "1: Octave Root\n2: Chord Root\n3: Chord Family\n4: Interpret",
                new EncoderSlotBinding[]{
                        oikordSlot(0, oikordOctaveEncoder, this::adjustOikordOctave,
                                this::showOikordOctaveInfo, this::resetOikordOctave),
                        oikordSlot(1, oikordRootEncoder, this::adjustOikordRoot,
                                this::showOikordRootInfo, this::resetOikordRoot),
                        oikordSlot(2, oikordFamilyEncoder,
                                amount -> {
                                    if (driver.isGlobalShiftHeld() || driver.isGlobalAltHeld()) {
                                        adjustOikordPage(amount);
                                    } else {
                                        adjustOikordFamily(amount);
                                    }
                                },
                                this::showCurrentOikord, this::resetOikordFamilySelection),
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
                        chordVelocitySlot(),
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

    private EncoderSlotBinding chordVelocitySlot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return NoteStepAccess.VELOCITY.getResolution();
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    final List<NoteStep> heldNotes = getHeldNotes();
                    handler.recordTouchAdjustment(slotIndex, Math.abs(inc));
                    if (!heldNotes.isEmpty()) {
                        handler.handleExplicitNoteAccess(inc, NoteStepAccess.VELOCITY);
                        return;
                    }
                    adjustDefaultOikordVelocity(inc);
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        final List<NoteStep> heldNotes = getHeldNotes();
                        if (!heldNotes.isEmpty()) {
                            handler.beginTouchReset(slotIndex,
                                    () -> handler.resetAccessorToDefault(NoteStepAccess.VELOCITY));
                            handler.showAccessorTouchValue(NoteStepAccess.VELOCITY);
                            return;
                        }
                        handler.beginTouchReset(slotIndex, () -> {
                            resetDefaultOikordVelocity();
                            showDefaultOikordVelocity();
                        });
                        showDefaultOikordVelocity();
                        return;
                    }
                    handler.endTouchReset(slotIndex);
                    oled.clearScreenDelayed();
                });
            }
        };
    }

    private void adjustDefaultOikordVelocity(final int inc) {
        final int nextVelocity = Math.max(MIN_VELOCITY,
                Math.min(DEFAULT_OIKORD_ACCENTED_VELOCITY - 1, defaultOikordVelocity + inc));
        if (nextVelocity == defaultOikordVelocity) {
            return;
        }
        defaultOikordVelocity = nextVelocity;
        showDefaultOikordVelocity();
    }

    private void resetDefaultOikordVelocity() {
        defaultOikordVelocity = DEFAULT_OIKORD_STANDARD_VELOCITY;
    }

    private void showDefaultOikordVelocity() {
        oled.paramInfo("Velocity", defaultOikordVelocity, "Chord Default", MIN_VELOCITY,
                DEFAULT_OIKORD_ACCENTED_VELOCITY - 1);
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
                encoder.bindEncoder(layer, inc -> adjustMixerParameter(parameter, label, inc));
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
        for (int i = 0; i < noteClipSlotBank.getSizeOfBank(); i++) {
            final ClipLauncherSlot slot = noteClipSlotBank.getItemAt(i);
            slot.exists().markInterested();
            slot.hasContent().markInterested();
            slot.isSelected().markInterested();
            slot.color().markInterested();
            slot.exists().addValueObserver(ignored -> refreshSelectedNoteClipState());
            slot.hasContent().addValueObserver(ignored -> refreshSelectedNoteClipState());
            slot.isSelected().addValueObserver(ignored -> refreshSelectedNoteClipState());
            slot.color().addValueObserver((r, g, b) -> refreshSelectedNoteClipState());
        }
        refreshSelectedNoteClipState();
    }

    private void refreshSelectedNoteClipState() {
        final int previousSlotIndex = selectedNoteClipSlotIndex;
        final boolean previousHasContent = selectedNoteClipHasContent;
        selectedNoteClipSlotIndex = -1;
        selectedNoteClipHasContent = false;
        selectedNoteClipColor = oikordStepBaseColor != null ? oikordStepBaseColor : OCCUPIED_STEP;
        for (int i = 0; i < noteClipSlotBank.getSizeOfBank(); i++) {
            final ClipLauncherSlot slot = noteClipSlotBank.getItemAt(i);
            if (slot.exists().get() && slot.isSelected().get()) {
                selectedNoteClipSlotIndex = i;
                selectedNoteClipHasContent = slot.hasContent().get();
                selectedNoteClipColor = ColorLookup.getColor(slot.color().get());
                break;
            }
        }
        if (previousSlotIndex != selectedNoteClipSlotIndex || previousHasContent != selectedNoteClipHasContent) {
            queueChordObservationResync();
        }
    }

    private void queueChordObservationResync() {
        if (chordObservationResyncQueued) {
            return;
        }
        chordObservationResyncQueued = true;
        driver.getHost().scheduleTask(() -> {
            chordObservationResyncQueued = false;
            refreshChordStepObservation();
        }, 0);
    }

    private void refreshChordStepObservation() {
        refreshSelectedNoteClipState();
        refreshChordStepObservationPass();
        driver.getHost().scheduleTask(this::refreshChordStepObservationPass, 1);
        driver.getHost().scheduleTask(this::refreshChordStepObservationPass, 6);
        driver.getHost().scheduleTask(this::refreshChordStepObservationPass, 18);
    }

    private void refreshChordStepObservationPass() {
        clearObservedChordCaches();
        final int preferredSlotIndex = driver.getViewControl().getSelectedClipSlotIndex();
        if (preferredSlotIndex >= 0 && preferredSlotIndex < noteClipSlotBank.getSizeOfBank()) {
            noteClipSlotBank.cursorIndex().set(preferredSlotIndex);
            final ClipLauncherSlot preferredSlot = noteClipSlotBank.getItemAt(preferredSlotIndex);
            if (preferredSlot.exists().get()) {
                preferredSlot.select();
            }
        } else {
            for (int i = 0; i < noteClipSlotBank.getSizeOfBank(); i++) {
                final ClipLauncherSlot slot = noteClipSlotBank.getItemAt(i);
                if (slot.exists().get() && slot.isSelected().get()) {
                    slot.select();
                    break;
                }
            }
        }
        noteStepClip.scrollToKey(0);
        observedNoteClip.scrollToKey(0);
        noteStepClip.scrollToStep(chordStepOffset());
        observedNoteClip.scrollToStep(0);
    }

    private void clearObservedChordCaches() {
        observedClipNotesByStep.clear();
        observedFineOccupancyByStep.clear();
        observedFineNoteStartsByStep.clear();
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
        if (selectedNoteClipHasContent || hasLoadedNoteClipContent()) {
            return true;
        }
        oled.valueInfo("No Clip", "Create or Select Clip");
        driver.notifyPopup("No Clip", "Create or select clip");
        return false;
    }

    private boolean hasLoadedNoteClipContent() {
        return !observedClipNotesByStep.isEmpty()
                || noteStepsByPosition.values().stream()
                .flatMap(step -> step.values().stream())
                .anyMatch(note -> note.state() == NoteStep.State.NoteOn);
    }

    private boolean hasVisibleStepContent(final int stepIndex) {
        final int globalStep = localToGlobalStep(stepIndex);
        if (observedClipNotesByStep.containsKey(globalStep)) {
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
        if (observedFineNoteStartsByStep.containsKey(globalStep)) {
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
        final Set<Integer> observedNotes = observedClipNotesByStep.get(localToGlobalStep(stepIndex));
        if (observedNotes != null && !observedNotes.isEmpty()) {
            return new HashSet<>(observedNotes);
        }
        return Set.of();
    }

    @FunctionalInterface
    private interface OikordAdjuster {
        void adjust(int amount);
    }

    private EncoderSlotBinding oikordSlot(final int slotIndex, final EncoderStepAccumulator accumulator,
                                          final OikordAdjuster adjuster, final Runnable showInfo,
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
                        markEncoderAdjusted(slotIndex);
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
                        adjustOikordInterpretation(inc);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        handler.beginTouchReset(slotIndex, () -> {
                            oikordInterpretation = OikordInterpretation.AS_IS;
                            oled.valueInfo("Interpret", oikordInterpretation.displayName());
                        });
                        oled.valueInfo("Interpret", oikordInterpretation.displayName());
                        return;
                    }
                    handler.endTouchReset(slotIndex);
                    oled.clearScreenDelayed();
                });
            }
        };
    }

    private String getLiveModeInfo(final EncoderMode mode) {
        return switch (mode) {
            case CHANNEL -> "1: Pitch Offs\n2: Velocity\n3: Scale\n4: Layout";
            case MIXER -> "1: Volume\n2: Pan\n3: Send 1\n4: Send 2";
            case USER_1 -> "1: Mod\n2: Pressure\n3: Timbre\n4: Pitch Expr";
            case USER_2 -> "1: Remote 1\n2: Remote 2\n3: Remote 3\n4: Remote 4";
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
        noteStepActive = false;
        builderSelectedNotes.clear();
        heldStepPads.clear();
        heldStepAnchor = null;
        selectedPresetStepIndex = null;
        chordStepPosition.setPage(0);
        patternButtons.setUpCallback(this::handlePatternUp, this::getPatternUpLight);
        patternButtons.setDownCallback(this::handlePatternDown, this::getPatternDownLight);
        liveEncoderMode = EncoderMode.CHANNEL;
        activateCurrentLiveEncoderLayer();
        liveModeControlLayer.activate();
        stepEncoderLayer.deactivate();
        applyLiveVelocity();
        applyLayout();
        showState("Mode");
    }

    @Override
    protected void onDeactivate() {
        patternButtons.setUpCallback(pressed -> { }, () -> BiColorLightState.OFF);
        patternButtons.setDownCallback(pressed -> { }, () -> BiColorLightState.OFF);
        noteStepActive = false;
        resetLivePerformanceToggles();
        builderSelectedNotes.clear();
        heldStepPads.clear();
        heldStepAnchor = null;
        selectedPresetStepIndex = null;
        stopAuditionNotes();
        deactivateLiveEncoderLayers();
        liveModeControlLayer.deactivate();
        stepEncoderLayer.deactivate();
        clearTranslation();
    }
}
