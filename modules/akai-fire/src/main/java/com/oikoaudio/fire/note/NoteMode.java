package com.oikoaudio.fire.note;

import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
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
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.EncoderStepAccumulator;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;
import com.oikoaudio.fire.sequence.NoteStepAccess;
import com.oikoaudio.fire.sequence.SequencEncoderHandler;
import com.oikoaudio.fire.sequence.StepSequencerHost;
import com.oikoaudio.fire.utils.PatternButtons;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NoteMode extends Layer implements StepSequencerHost {
    private static final int PIANO_HIGHLIGHT_INDEX = -1;
    private static final int MIN_OCTAVE = 0;
    private static final int MAX_OCTAVE = 7;
    private static final int MIN_TRANSPOSE = 0;
    private static final int MAX_TRANSPOSE = MAX_OCTAVE * 12 + 11;
    private static final int HELD_NOTE_VELOCITY = 100;
    private static final int STEP_PAD_OFFSET = 32;
    private static final int STEP_COUNT = 32;
    private static final double STEP_LENGTH = 0.25;
    private static final double MIN_GATE_RATIO = 0.25;
    private static final double MAX_GATE_RATIO = 1.0;
    private static final int AUDITION_VELOCITY = 96;
    private static final int LIVE_NOTE_ENCODER_THRESHOLD = 2;
    private static final int LIVE_LAYOUT_ENCODER_THRESHOLD = 4;
    private static final int OIKORD_ROOT_ENCODER_THRESHOLD = 5;
    private static final int OIKORD_OCTAVE_ENCODER_THRESHOLD = 4;
    private static final int OIKORD_FAMILY_ENCODER_THRESHOLD = 5;
    private static final int LIVE_VELOCITY_ENCODER_THRESHOLD = 2;
    private static final int LIVE_PITCH_OFFSET_ENCODER_THRESHOLD = 3;
    private static final int MIN_OIKORD_ROOT_OFFSET = -24;
    private static final int MAX_OIKORD_ROOT_OFFSET = 24;
    private static final int MIN_OIKORD_OCTAVE_OFFSET = -3;
    private static final int MAX_OIKORD_OCTAVE_OFFSET = 3;
    private static final double MIXER_INC = 0.025;
    private static final double MIXER_FINE_INC = 0.001;
    private static final int MIN_MIDI_VALUE = 0;
    private static final int MAX_MIDI_VALUE = 127;
    private static final int MIN_VELOCITY = 1;
    private static final int DEFAULT_LIVE_VELOCITY = 100;
    private static final int DEFAULT_TIMBRE = 64;
    private static final int MIDI_CC_MOD = 1;
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
    private final Set<Integer> auditioningNotes = new HashSet<>();
    private final Map<Integer, Set<Integer>> clipNotesByStep = new HashMap<>();
    private final OikordBank oikordBank = new OikordBank();
    private final CursorTrack cursorTrack;
    private final PinnableCursorClip noteStepClip;
    private final PinnableCursorDevice liveCursorDevice;
    private final CursorRemoteControlsPage liveRemoteControlsPage;
    private final Layer liveModeControlLayer;
    private final Layer liveChannelLayer;
    private final Layer liveMixerLayer;
    private final Layer liveUser1Layer;
    private final Layer liveUser2Layer;
    private final SequencEncoderHandler stepEncoderLayer;
    private final BooleanValueObject deleteHeld = new BooleanValueObject();
    private final BooleanValueObject selectHeld = new BooleanValueObject();
    private final BooleanValueObject lengthDisplay = new BooleanValueObject();
    private final Map<Integer, Map<Integer, NoteStep>> noteStepsByPosition = new HashMap<>();
    private final Map<String, NoteStepMoveSnapshot> pendingMovedNotes = new HashMap<>();

    private int scaleIndex = PIANO_HIGHLIGHT_INDEX;
    private int transposeBase = 36;
    private boolean inKey = false;
    private boolean noteStepActive = false;
    private NoteStepSubMode currentStepSubMode = NoteStepSubMode.OIKORD_STEP;
    private OikordInterpretation oikordInterpretation = OikordInterpretation.AS_IS;
    private int selectedOikordFamily = 0;
    private int oikordPage = 0;
    private int selectedOikordSlot = 0;
    private int oikordRootOffset = 0;
    private int oikordOctaveOffset = 0;
    private int liveVelocity = DEFAULT_LIVE_VELOCITY;
    private int livePressure = MIN_MIDI_VALUE;
    private int liveTimbre = DEFAULT_TIMBRE;
    private int liveModulation = MIN_MIDI_VALUE;
    private int livePitchOffsetIndex = DEFAULT_LIVE_PITCH_OFFSET_INDEX;
    private EncoderMode liveEncoderMode = EncoderMode.CHANNEL;
    private Layer currentLiveEncoderLayer;
    private final EncoderStepAccumulator liveVelocityEncoder = new EncoderStepAccumulator(LIVE_VELOCITY_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator livePitchOffsetEncoder = new EncoderStepAccumulator(LIVE_PITCH_OFFSET_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator liveScaleEncoder = new EncoderStepAccumulator(LIVE_NOTE_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator liveOctaveEncoder = new EncoderStepAccumulator(LIVE_NOTE_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator liveLayoutEncoder = new EncoderStepAccumulator(LIVE_LAYOUT_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator oikordRootEncoder = new EncoderStepAccumulator(OIKORD_ROOT_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator oikordOctaveEncoder = new EncoderStepAccumulator(OIKORD_OCTAVE_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator oikordFamilyEncoder = new EncoderStepAccumulator(OIKORD_FAMILY_ENCODER_THRESHOLD);

    private enum NoteStepSubMode {
        OIKORD_STEP("Oikord Step", BiColorLightState.GREEN_HALF, BiColorLightState.GREEN_FULL),
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

        this.cursorTrack = driver.getViewControl().getCursorTrack();
        this.liveCursorDevice = cursorTrack.createCursorDevice("NOTE_LIVE_DEVICE", "Note Live Device", 8,
                CursorDeviceFollowMode.FOLLOW_SELECTION);
        this.liveRemoteControlsPage = liveCursorDevice.createCursorRemoteControlsPage(8);
        this.noteStepClip = cursorTrack.createLauncherCursorClip("NOTE_STEP", "NOTE_STEP", STEP_COUNT, 128);
        this.noteStepClip.scrollToKey(0);
        this.noteStepClip.addStepDataObserver(this::handleStepData);
        this.noteStepClip.addNoteStepObserver(this::handleNoteStepObject);
        this.stepEncoderLayer = new SequencEncoderHandler(this, driver);
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

        final BiColorButton octaveUpButton = driver.getButton(NoteAssign.MUTE_1);
        octaveUpButton.bindPressed(this, pressed -> handlePitchContextButton(pressed, 1, false),
                () -> getPitchContextLightState(1, false));

        final BiColorButton octaveDownButton = driver.getButton(NoteAssign.MUTE_2);
        octaveDownButton.bindPressed(this, pressed -> handlePitchContextButton(pressed, -1, false),
                () -> getPitchContextLightState(-1, false));

        final BiColorButton rootUpButton = driver.getButton(NoteAssign.MUTE_3);
        rootUpButton.bindPressed(this, pressed -> handlePitchContextButton(pressed, 1, true),
                () -> getPitchContextLightState(1, true));

        final BiColorButton rootDownButton = driver.getButton(NoteAssign.MUTE_4);
        rootDownButton.bindPressed(this, pressed -> handlePitchContextButton(pressed, -1, true),
                () -> getPitchContextLightState(-1, true));

        final BiColorButton knobModeButton = driver.getButton(NoteAssign.KNOB_MODE);
        knobModeButton.bindPressed(liveModeControlLayer, this::handleLiveModeAdvance, this::getLiveModeLightState);
    }

    private void bindEncoders() {
        final TouchEncoder[] encoders = driver.getEncoders();
        bindLiveChannelEncoders(encoders);
        bindMixerPage(null, liveMixerLayer, encoders);
        bindLiveExpressionEncoders(encoders);
        bindLiveRemoteEncoders(encoders);

        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.bindEncoder(this, this::handleMainEncoder);
        mainEncoder.bindTouched(this, this::handleMainEncoderPress);
    }

    private void bindLiveChannelEncoders(final TouchEncoder[] encoders) {
        encoders[0].bindEncoder(liveChannelLayer, this::handleLiveVelocityEncoder);
        encoders[0].bindTouched(liveChannelLayer, this::handleLiveVelocityTouch);

        encoders[1].bindEncoder(liveChannelLayer, this::handleEncoder1);
        encoders[1].bindTouched(liveChannelLayer, this::handleEncoder1Touch);

        encoders[2].bindEncoder(liveChannelLayer, this::handleEncoder2);
        encoders[2].bindTouched(liveChannelLayer, this::handleEncoder2Touch);

        encoders[3].bindEncoder(liveChannelLayer, this::handleEncoder3);
        encoders[3].bindTouched(liveChannelLayer, this::handleEncoder3Touch);
    }

    private void bindLiveExpressionEncoders(final TouchEncoder[] encoders) {
        encoders[0].bindEncoder(liveUser1Layer, this::adjustLivePitchOffset);
        encoders[0].bindTouched(liveUser1Layer, this::handleLivePitchOffsetTouch);
        bindLiveMidiEncoder(encoders[1], liveUser1Layer, "Pressure",
                this::adjustLivePressure, () -> livePressure);
        bindLiveMidiEncoder(encoders[2], liveUser1Layer, "Timbre",
                this::adjustLiveTimbre, () -> liveTimbre);
        bindLiveMidiEncoder(encoders[3], liveUser1Layer, "Mod",
                this::adjustLiveModulation, () -> liveModulation);
    }

    private void bindLiveRemoteEncoders(final TouchEncoder[] encoders) {
        for (int i = 0; i < encoders.length; i++) {
            final int index = i;
            final Parameter parameter = liveRemoteControlsPage.getParameter(index);
            encoders[i].bindEncoder(liveUser2Layer, inc -> adjustMixerParameter(parameter.value(), inc));
            encoders[i].bindTouched(liveUser2Layer, touched -> {
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
        final int nextVelocity = Math.max(MIN_VELOCITY, Math.min(MAX_MIDI_VALUE, liveVelocity + steps));
        if (nextVelocity == liveVelocity) {
            return;
        }
        liveVelocity = nextVelocity;
        applyLiveVelocity();
        oled.paramInfo("Velocity", liveVelocity, "Live Note", MIN_VELOCITY, MAX_MIDI_VALUE);
    }

    private void handleEncoder1(final int inc) {
        adjustScale(liveScaleEncoder.consume(inc));
    }

    private void handleEncoder2(final int inc) {
        adjustOctave(liveOctaveEncoder.consume(inc));
    }

    private void handleEncoder3(final int inc) {
        if (liveLayoutEncoder.consume(inc) != 0) {
            toggleLayout();
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
        oled.valueInfo("Pitch Offset", formatSignedValue(getLivePitchOffset()));
    }

    private void handleLivePitchOffsetTouch(final boolean touched) {
        if (!touched) {
            livePitchOffsetEncoder.reset();
            oled.clearScreenDelayed();
            return;
        }
        oled.valueInfo("Pitch Offset", formatSignedValue(getLivePitchOffset()));
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
        if (!pressed) {
            liveVelocityEncoder.reset();
            oled.clearScreenDelayed();
            return;
        }
        oled.valueInfo("Velocity", Integer.toString(liveVelocity));
    }

    private void handleEncoder1Touch(final boolean pressed) {
        if (!pressed) {
            liveScaleEncoder.reset();
        }
        showTouchedState(pressed, "Scale");
    }

    private void handleEncoder2Touch(final boolean pressed) {
        if (!pressed) {
            liveOctaveEncoder.reset();
        }
        showTouchedState(pressed, "Octave");
    }

    private void handleEncoder3Touch(final boolean pressed) {
        if (!pressed) {
            liveLayoutEncoder.reset();
        }
        showTouchedState(pressed, "Layout");
    }

    private void handleStepSeqPressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (driver.isGlobalShiftHeld()) {
            currentStepSubMode = currentStepSubMode.next();
            noteStepActive = true;
            enterCurrentStepSubMode();
            oled.valueInfo("Step Mode", currentStepSubMode.displayName());
            return;
        }
        if (noteStepActive) {
            if (currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
                toggleOikordInterpretation();
            } else {
                oled.valueInfo("Step Mode", currentStepSubMode.displayName());
            }
        } else {
            noteStepActive = true;
            enterCurrentStepSubMode();
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
            if (!oikordBank.hasSlot(selectedOikordFamily, oikordPage, padIndex)) {
                return;
            }
            if (pressed) {
                selectedOikordSlot = padIndex;
                final boolean hasHeldSteps = !heldStepPads.isEmpty();
                final boolean auditionEnabled = driver.isAuditionOikordsEnabled();
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
        if (pressed) {
            heldStepPads.add(stepIndex);
            showHeldStepInfo(stepIndex);
        } else {
            heldStepPads.remove(stepIndex);
        }
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
        final Set<Integer> stepNotes = clipNotesByStep.computeIfAbsent(x, ignored -> new HashSet<>());
        if (state == NoteStep.State.Empty.ordinal()) {
            stepNotes.remove(y);
            if (stepNotes.isEmpty()) {
                clipNotesByStep.remove(x);
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
        final Set<Integer> selectedSteps = heldStepPads.isEmpty() ? noteStepsByPosition.keySet() : heldStepPads;
        if (selectedSteps.isEmpty()) {
            return;
        }
        final List<NoteStep> notesToMove = selectedSteps.stream()
                .distinct()
                .flatMap(step -> noteStepsByPosition.getOrDefault(step, Map.of()).values().stream())
                .sorted(amount > 0 ? Comparator.comparingInt(NoteStep::x).reversed() : Comparator.comparingInt(NoteStep::x))
                .toList();
        if (notesToMove.isEmpty()) {
            return;
        }
        for (final NoteStep note : notesToMove) {
            final int targetX = note.x() + amount;
            if (targetX >= 0 && targetX < STEP_COUNT) {
                noteStepClip.moveStep(note.x(), note.y(), amount, 0);
                continue;
            }
            final int wrappedX = Math.floorMod(targetX, STEP_COUNT);
            pendingMovedNotes.put(moveKey(wrappedX, note.y()), NoteStepMoveSnapshot.capture(note));
            noteStepClip.setStep(wrappedX, note.y(), (int) Math.round(note.velocity() * 127), note.duration());
            noteStepClip.clearStep(note.x(), note.y());
        }
        oled.valueInfo("Move", amount > 0 ? "Right" : "Left");
    }

    private void nudgeHeldNotes(final int amount) {
        final List<NoteStep> heldNotes = getHeldNotes();
        if (heldNotes.isEmpty()) {
            return;
        }
        for (final NoteStep note : heldNotes) {
            final double nextDuration = Math.max(MIN_GATE_RATIO * STEP_LENGTH,
                    Math.min(MAX_GATE_RATIO * STEP_LENGTH, note.duration() + amount * (STEP_LENGTH / 16.0)));
            note.setDuration(nextDuration);
        }
        oled.valueInfo("Nudge", amount > 0 ? "+Fine" : "-Fine");
    }

    private void enterCurrentStepSubMode() {
        releaseHeldLiveNotes();
        heldStepPads.clear();
        clearTranslation();
        syncEncoderLayers();
        if (currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            showCurrentOikord();
            return;
        }
        oled.valueInfo("Step Mode", "Clip Step Record");
    }

    private void returnToLivePlay() {
        noteStepActive = false;
        heldStepPads.clear();
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
        currentLiveEncoderLayer = liveLayerForMode(liveEncoderMode);
        currentLiveEncoderLayer.activate();
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
        final OikordBank.Slot slot = currentOikordSlot();
        final int[] notes = renderSelectedOikord(slot);
        for (final int stepIndex : heldStepPads) {
            noteStepClip.clearStepsAtX(0, stepIndex);
            for (final int midiNote : notes) {
                noteStepClip.setStep(stepIndex, midiNote, HELD_NOTE_VELOCITY, STEP_LENGTH);
            }
        }
        oled.valueInfo(oledFamilyLabel(slot.family()), "%s | %s".formatted(slot.name(), oikordInterpretation.displayName()));
    }

    private void adjustOikordPage(final int amount) {
        if (amount == 0) {
            return;
        }
        final int nextPage = Math.max(0, Math.min(oikordBank.pageCount(selectedOikordFamily) - 1, oikordPage + amount));
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
        final int familyCount = oikordBank.families().size();
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

    private void toggleOikordInterpretation() {
        oikordInterpretation = oikordInterpretation.next();
        oled.valueInfo("Oikord Mode", oikordInterpretation.displayName());
    }

    private void startAuditionSelectedOikord() {
        final OikordBank.Slot slot = currentOikordSlot();
        final int[] notes = renderSelectedOikord(slot);
        stopAuditionNotes();
        for (final int midiNote : notes) {
            noteInput.sendRawMidiEvent(Midi.NOTE_ON, midiNote, AUDITION_VELOCITY);
            auditioningNotes.add(midiNote);
        }
        oled.valueInfo(oledFamilyLabel(slot.family()), "%s | %s".formatted(slot.name(), oikordInterpretation.displayName()));
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
        if (!pressed) {
            return;
        }
        if (noteStepActive) {
            if (driver.isGlobalShiftHeld()) {
                nudgeHeldNotes(amount);
            } else {
                moveStepContent(amount);
            }
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
        return noteStepActive ? currentStepSubMode.activeLight() : currentStepSubMode.idleLight();
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

    public void cycleLayout() {
        toggleLayout();
    }

    public boolean isNoteStepActive() {
        return noteStepActive;
    }

    public void returnToLivePlayFromStepMode() {
        if (noteStepActive) {
            returnToLivePlay();
        }
    }

    public BiColorLightState getModeButtonLightState() {
        return inKey ? BiColorLightState.RED_FULL : BiColorLightState.AMBER_FULL;
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
        final boolean fine = driver.isGlobalShiftHeld();
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            noteRepeatHandler.handleMainEncoder(inc, driver.isGlobalAltHeld());
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_SHUFFLE_ROLE.equals(mainEncoderRole)) {
            driver.adjustGrooveShuffleAmount(inc, fine);
        } else {
            driver.adjustMainCursorParameter(inc, fine);
        }
    }

    private void handleMainEncoderPress(final boolean pressed) {
        if (driver.isPopupBrowserActive()) {
            return;
        }
        if (pressed && driver.isGlobalShiftHeld()) {
            driver.cycleMainEncoderRolePreference();
            return;
        }
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            noteRepeatHandler.handlePressed(pressed);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_SHUFFLE_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showGrooveShuffleInfo();
            } else {
                driver.toggleGrooveEnabled();
            }
        } else if (pressed) {
            driver.showMainCursorParameterInfo();
        } else {
            driver.resetMainCursorParameter();
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
            if (!oikordBank.hasSlot(selectedOikordFamily, oikordPage, padIndex)) {
                return RgbLigthState.OFF;
            }
            final OikordBank.Slot slot = oikordBank.slot(selectedOikordFamily, oikordPage, padIndex);
            final int groupIndex = padIndex / 8;
            final RgbLigthState grouped = getFamilyGroupColor(slot.family(), groupIndex);
            return padIndex == selectedOikordSlot ? grouped.getBrightest() : grouped;
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
        final RgbLigthState base = heldStepPads.contains(stepIndex)
                ? HELD_STEP
                : clipNotesByStep.containsKey(stepIndex)
                ? OCCUPIED_STEP
                : (stepIndex / 4) % 2 == 0 ? EMPTY_STEP_A : EMPTY_STEP_B;
        return heldStepPads.contains(stepIndex) ? base.getBrightest() : base;
    }

    private RgbLigthState getClipStepRecordPadLight(final int padIndex) {
        if (padIndex < STEP_PAD_OFFSET) {
            return DEFERRED_TOP;
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
        if (heldStepPads.contains(stepIndex)) {
            return HELD_STEP.getBrightest();
        }
        return clipNotesByStep.containsKey(stepIndex) ? OCCUPIED_STEP : DEFERRED_BOTTOM;
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

    private RgbLigthState getFamilyGroupColor(final String family, final int groupIndex) {
        return switch (groupIndex % 4) {
            case 0 -> getFamilyColor(family).getBrightend();
            case 1 -> getAlternateFamilyColor(family);
            case 2 -> getFamilyColor(family).getDimmed();
            default -> getAlternateFamilyColor(family).getDimmed();
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
            oled.valueInfo("Oikord Mode", oikordInterpretation.displayName());
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
        final OikordBank.Slot slot = currentOikordSlot();
        oled.valueInfo("%s %d/%d".formatted(oledFamilyLabel(slot.family()), oikordPage + 1,
                        oikordBank.pageCount(selectedOikordFamily)),
                "%s %s".formatted(slot.name(), oikordInterpretationSuffix()));
    }

    private String currentOikordDisplay() {
        final OikordBank.Slot slot = currentOikordSlot();
        return "%s %s".formatted(slot.shortLabel(), oikordInterpretationSuffix());
    }

    private String oledFamilyLabel(final String family) {
        return switch (family) {
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
        ensureSelectedOikordSlotValid();
        return oikordBank.slot(selectedOikordFamily, oikordPage, selectedOikordSlot);
    }

    private void ensureSelectedOikordSlotValid() {
        if (oikordBank.hasSlot(selectedOikordFamily, oikordPage, selectedOikordSlot)) {
            return;
        }
        final int pageStart = oikordPage * OikordBank.PAGE_SIZE;
        final int familySlotCount = oikordBank.family(selectedOikordFamily).slots().size();
        if (pageStart >= familySlotCount) {
            oikordPage = Math.max(0, oikordBank.pageCount(selectedOikordFamily) - 1);
        }
        selectedOikordSlot = 0;
        while (selectedOikordSlot < OikordBank.PAGE_SIZE
                && !oikordBank.hasSlot(selectedOikordFamily, oikordPage, selectedOikordSlot)) {
            selectedOikordSlot++;
        }
        if (selectedOikordSlot >= OikordBank.PAGE_SIZE) {
            selectedOikordSlot = 0;
        }
    }

    private int[] renderSelectedOikord(final OikordBank.Slot slot) {
        if (oikordInterpretation == OikordInterpretation.CAST) {
            final int shiftedRoot = getRootNote() + oikordRootOffset;
            final int castRoot = Math.floorMod(shiftedRoot, 12);
            final int castOctaveOffset = Math.floorDiv(shiftedRoot, 12) + oikordOctaveOffset;
            return transpose(oikordBank.renderCast(selectedOikordFamily, oikordPage, selectedOikordSlot, getScale(), castRoot),
                    castOctaveOffset * 12);
        }
        return oikordBank.renderAsIs(selectedOikordFamily, oikordPage, selectedOikordSlot,
                getOikordRootMidi() + oikordRootOffset + oikordOctaveOffset * 12);
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
        oled.valueInfo("Oik Root", "%s %s".formatted(NoteGridLayout.noteName(noteClass), formatSignedValue(oikordRootOffset)));
    }

    private void showOikordOctaveInfo() {
        oled.valueInfo("Oik Oct", formatSignedValue(oikordOctaveOffset));
    }

    private void showOikordFamilyInfo() {
        final OikordBank.Family family = oikordBank.family(selectedOikordFamily);
        oled.valueInfo("Oik Family", family.family());
    }

    private String formatSignedValue(final int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private void showHeldStepInfo(final int stepIndex) {
        oled.valueInfo("Step", Integer.toString(stepIndex + 1));
    }

    private MusicalScale getScale() {
        final int effectiveScaleIndex = scaleIndex == PIANO_HIGHLIGHT_INDEX ? 1 : scaleIndex;
        return scaleLibrary.getMusicalScale(effectiveScaleIndex);
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

    private int getOctave() {
        return transposeBase / 12;
    }

    private NoteGridLayout createLayout() {
        return new NoteGridLayout(getScale(), getRootNote(), getOctave(), inKey);
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
            adjustOikordPage(1);
        }
    }

    private void handlePatternDown(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            adjustOikordPage(-1);
        }
    }

    private BiColorLightState getPatternButtonLight() {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            return BiColorLightState.GREEN_HALF;
        }
        return BiColorLightState.OFF;
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
    public String getDetails(final List<NoteStep> heldNotes) {
        return "%s <%d>".formatted(currentStepSubMode.displayName(), heldNotes.size());
    }

    @Override
    public double getGridResolution() {
        return STEP_LENGTH;
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
    }

    @Override
    public void bindMixerPage(final SequencEncoderHandler handler, final Layer layer, final TouchEncoder[] encoders) {
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
            encoders[i].bindEncoder(layer, inc -> adjustMixerParameter(parameter.value(), inc));
            encoders[i].bindTouched(layer, touched -> {
                if (touched) {
                    oled.valueInfo(parameter.name().get(), parameter.displayedValue().get());
                } else {
                    oled.clearScreenDelayed();
                }
            });
        }
    }

    private void adjustMixerParameter(final SettableRangedValue value, final int inc) {
        final double amount = inc * (driver.isGlobalShiftHeld() ? MIXER_FINE_INC : MIXER_INC);
        value.setImmediately(Math.max(0, Math.min(1, value.get() + amount)));
    }

    @Override
    public void bindUser2Page(final SequencEncoderHandler handler, final Layer layer, final TouchEncoder[] encoders) {
        encoders[0].bindEncoder(layer, inc -> adjustOikordRoot(oikordRootEncoder.consume(inc)));
        encoders[0].bindTouched(layer, touched -> {
            if (!touched) {
                oikordRootEncoder.reset();
                oled.clearScreenDelayed();
            } else {
                showOikordRootInfo();
            }
        });
        encoders[1].bindEncoder(layer, inc -> adjustOikordOctave(oikordOctaveEncoder.consume(inc)));
        encoders[1].bindTouched(layer, touched -> {
            if (!touched) {
                oikordOctaveEncoder.reset();
                oled.clearScreenDelayed();
            } else {
                showOikordOctaveInfo();
            }
        });
        encoders[2].bindEncoder(layer, inc -> {
            final int steps = oikordFamilyEncoder.consume(inc);
            if (steps != 0) {
                adjustOikordFamily(steps);
            }
        });
        encoders[2].bindTouched(layer, touched -> {
            if (!touched) {
                oikordFamilyEncoder.reset();
                oled.clearScreenDelayed();
            } else {
                showOikordFamilyInfo();
            }
        });
        encoders[3].bindEncoder(layer, inc -> { });
        encoders[3].bindTouched(layer, touched -> {
            if (touched) {
                oled.valueInfo("User 2-4", "Reserved");
            } else {
                oled.clearScreenDelayed();
            }
        });
    }

    @Override
    public String getModeInfo(final EncoderMode mode) {
        if (mode == EncoderMode.USER_2) {
            return "1: Root Offset\n2: Octave Offset\n3: Family\n4: Reserved";
        }
        return StepSequencerHost.super.getModeInfo(mode);
    }

    private String getLiveModeInfo(final EncoderMode mode) {
        return switch (mode) {
            case CHANNEL -> "1: Velocity\n2: Scale\n3: Octave\n4: Layout";
            case MIXER -> "1: Volume\n2: Pan\n3: Send 1\n4: Send 2";
            case USER_1 -> "1: Pitch Offset\n2: Pressure\n3: Timbre\n4: Mod";
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
        heldStepPads.clear();
        patternButtons.setUpCallback(this::handlePatternUp, this::getPatternButtonLight);
        patternButtons.setDownCallback(this::handlePatternDown, this::getPatternButtonLight);
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
        heldStepPads.clear();
        stopAuditionNotes();
        deactivateLiveEncoderLayers();
        liveModeControlLayer.deactivate();
        stepEncoderLayer.deactivate();
        clearTranslation();
    }
}
