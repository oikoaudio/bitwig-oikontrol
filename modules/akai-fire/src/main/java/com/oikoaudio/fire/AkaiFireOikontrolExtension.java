package com.oikoaudio.fire;

import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.melodic.MelodicStepMode;
import com.oikoaudio.fire.TopLevelModeState.Mode;
import com.bitwig.extensions.framework.MusicalScale;
import com.oikoaudio.fire.note.ChordStepMode;
import com.oikoaudio.fire.note.NotePlayMode;
import com.oikoaudio.fire.perform.PerformClipLauncherMode;
import com.oikoaudio.fire.sequence.DrumSequenceMode;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;
import com.oikoaudio.fire.utils.PatternButtons;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.MusicalScaleLibrary;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.Midi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class AkaiFireOikontrolExtension extends ControllerExtension {
    private static final double MAIN_ENCODER_STEP = 0.01;
    private static final double MAIN_ENCODER_FINE_STEP = 0.0025;
    private static final int DEVICE_DISCOVERY_WIDTH = 128;
    private static final int[] BROWSER_RESULTS_PRIME_DELAYS_MS = {0, 1, 10, 30};
    public static final String MAIN_ENCODER_LAST_TOUCHED_ROLE = FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED;
    public static final String MAIN_ENCODER_SHUFFLE_ROLE = FireControlPreferences.MAIN_ENCODER_SHUFFLE;
    public static final String MAIN_ENCODER_TEMPO_ROLE = FireControlPreferences.MAIN_ENCODER_TEMPO;
    public static final String MAIN_ENCODER_NOTE_REPEAT_ROLE = FireControlPreferences.MAIN_ENCODER_NOTE_REPEAT;
    public static final String MAIN_ENCODER_TRACK_SELECT_ROLE = FireControlPreferences.MAIN_ENCODER_TRACK_SELECT;
    public static final String MAIN_ENCODER_DRUM_GRID_ROLE = FireControlPreferences.MAIN_ENCODER_DRUM_GRID;

    private static AkaiFireOikontrolExtension instance;
    private HardwareSurface surface;
    private Transport transport;
    private MidiIn midiIn;
    private MidiOut midiOut;
    private Layers layers;
    private int blinkTicks = 0;

    public final static byte SE_ST = (byte) 0xf0;
    public final static byte SE_EN = (byte) 0xf7;
    public final static byte MAN_ID_AKAI = 0x47;
    public final static byte DEVICE_ID = 0x7f;
    public final static byte PRODUCT_ID = 0x43;
    public final static byte SE_CMD_RGB = 0x65;
    public final static byte SE_OLED_RGB = 0x08;
    private final static String DEV_INQ = "F0 7E 00 06 01 F7";
    private final byte[] singleRgb = new byte[]{SE_ST, MAN_ID_AKAI, DEVICE_ID, PRODUCT_ID, SE_CMD_RGB, 0, 4, 0, 0, 0, 0, SE_EN};

    private final int[] lastCcValue = new int[128];
    private final RgbLigthState[] currentPadStates = new RgbLigthState[64];

    private Layer mainLayer;
    private final RgbButton[] rgbButtons = new RgbButton[64];
    private final TouchEncoder[] encoders = new TouchEncoder[4];
    private final MultiStateHardwareLight[] stateLights = new MultiStateHardwareLight[4];
    private final Map<NoteAssign, BiColorButton> controlButtons = new HashMap<>();
    private NoteInput noteInput;
    private DrumSequenceMode drumSequenceMode;
    private ViewCursorControl viewControl;
    private FireDeviceLocator deviceLocator;

    private final BooleanValueObject shiftActive = new BooleanValueObject();
    private OledDisplay oled;
    private ControllerHost host;
    private TouchEncoder mainEncoder;
    private LastClickedParameter lastClickedParameter;
    private Groove groove;
    private final BooleanValueObject altActive = new BooleanValueObject();
    private PopupBrowser popupBrowser;
    private BrowserResultsItem browserResultsCursor;

    private Preferences preferences;
    private SettableEnumValue clipLaunchModePref;
    private SettableEnumValue clipLaunchQuantizationPref;
    private SettableEnumValue defaultClipLengthPref;
    private SettableEnumValue mainEncoderStartupPref;
    private SettableEnumValue euclidScopePref;
    private SettableEnumValue drumPinModePref;
    private SettableEnumValue defaultScalePref;
    private SettableEnumValue defaultRootKeyPref;
    private SettableEnumValue defaultNoteInputOctavePref;
    private SettableEnumValue defaultVelocitySensitivityPref;
    private SettableEnumValue melodicSeedModePref;
    private SettableEnumValue livePitchOffsetBehaviorPref;
    private SettableBooleanValue encoderTouchResetPref;
    private SettableRangedValue padBrightnessPref;
    private SettableRangedValue padSaturationPref;
    private SettableRangedValue melodicFixedSeedPref;
    private SettableBooleanValue stepSeqPadAuditionPref;
    private SettableBooleanValue screenNotificationsPref;
    private String tempoDisplayValue = "";
    private boolean tempoDisplayPending = false;
    private boolean drumAutoPinApplied = false;
    private boolean drumTrackPinnedBeforeAutoPin = false;
    private boolean drumDevicePinnedBeforeAutoPin = false;
    private int drumTrackIndexBeforeAutoPin = -1;
    private boolean mainEncoderPressed = false;
    private boolean mainEncoderTurnedWhilePressed = false;
    private boolean suppressNextMelodicStepRelease = false;
    private boolean drumPinPreferenceObserved = false;
    private double padBrightness = FireControlPreferences.PAD_BRIGHTNESS_DEFAULT;
    private double padSaturation = FireControlPreferences.PAD_SATURATION_DEFAULT;
    private boolean encoderTouchResetEnabled = FireControlPreferences.ENCODER_TOUCH_RESET_DEFAULT;
    private final SharedMusicalContext sharedMusicalContext = new SharedMusicalContext(MusicalScaleLibrary.getInstance());

    private PatternButtons patternButtons;
    private NotePlayMode notePlayMode;
    private ChordStepMode chordStepMode;
    private MelodicStepMode melodicStepMode;
    private PerformClipLauncherMode performMode;
    private NoteRepeatHandler noteRepeatHandler;
    private final TopLevelModeState modeState = new TopLevelModeState();
    private DrumSubMode activeDrumSubMode = DrumSubMode.STANDARD;
    private String currentMainEncoderRole = FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED;
    private String alternateMainEncoderRole = FireControlPreferences.MAIN_ENCODER_TRACK_SELECT;

    private enum DrumSubMode {
        STANDARD(BiColorLightState.GREEN_FULL);

        private final BiColorLightState lightState;

        DrumSubMode(final BiColorLightState lightState) {
            this.lightState = lightState;
        }

        public BiColorLightState getLightState() {
            return lightState;
        }

        public DrumSubMode next() {
            return STANDARD;
        }
    }

    protected AkaiFireOikontrolExtension(final AkaiFireOikontrolDefinition definition, final ControllerHost host) {
        super(definition, host);
        instance = this;
    }

    @Override
    public void init() {
        host = getHost();
        Arrays.fill(lastCcValue, -1);

        lastClickedParameter = host.createLastClickedParameter("FIRE_LAST_CLICKED_PARAMETER", "Fire Last Clicked Parameter");
        final Parameter focusedParameter = lastClickedParameter.parameter();
        lastClickedParameter.isLocked().markInterested();
        focusedParameter.exists().markInterested();
        focusedParameter.name().markInterested();
        focusedParameter.displayedValue().markInterested();
        focusedParameter.value().markInterested();
        groove = host.createGroove();
        groove.getEnabled().name().markInterested();
        groove.getEnabled().displayedValue().markInterested();
        groove.getEnabled().value().markInterested();
        groove.getShuffleAmount().name().markInterested();
        groove.getShuffleAmount().displayedValue().markInterested();
        groove.getShuffleAmount().value().markInterested();
        popupBrowser = host.createPopupBrowser();
        popupBrowser.exists().markInterested();
        browserResultsCursor = popupBrowser.resultsColumn().createCursorItem();
        browserResultsCursor.exists().markInterested();
        browserResultsCursor.isSelected().markInterested();
        browserResultsCursor.name().markInterested();

        layers = new Layers(this);
        midiIn = host.getMidiInPort(0);
        midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
        midiIn.setSysexCallback(this::onSysEx);
        midiOut = host.getMidiOutPort(0);
        transport = host.createTransport();
        surface = host.createHardwareSurface();
        noteInput = midiIn.createNoteInput("MIDI", "80????", "90????", "A0????", "D0????");
        noteInput.setShouldConsumeEvents(false);
        viewControl = new ViewCursorControl(host, 16);
        deviceLocator = new FireDeviceLocator(host, DEVICE_DISCOVERY_WIDTH);

        mainLayer = new Layer(layers, "Main");
        oled = new OledDisplay(midiOut);
        noteRepeatHandler = new NoteRepeatHandler(
                noteInput,
                oled,
                () -> drumSequenceMode != null ? drumSequenceMode.getActiveRemoteControlsPage() : null,
                () -> drumSequenceMode != null ? drumSequenceMode.getAccentHandler().getCurrentVelocity() : 100);


        setUpHardware();
        setUpTransportControl();
        setUpPreferences();
        initializeSharedPitchContext();

        patternButtons = new PatternButtons(this, mainLayer);
        drumSequenceMode = new DrumSequenceMode(this, noteRepeatHandler);
        notePlayMode = new NotePlayMode(this, noteRepeatHandler);
        chordStepMode = new ChordStepMode(this, noteRepeatHandler);
        melodicStepMode = new MelodicStepMode(this, noteRepeatHandler);
        performMode = new PerformClipLauncherMode(this);
        oled.setIdleAction(this::showIdleOledInfo);
        midiOut.sendSysex(DEV_INQ);

        oled.valueInfo("Oikontrol", "");
        mainLayer.activate();
        switchActiveMode();
        host.scheduleTask(this::handlePing, 100);
        notifyPopup("Oikontrol Fire", "Active");

    }

    private void handlePing() {
        // sections.forEach(section -> section.notifyBlink(blinkTicks));
        blinkTicks++;
        ensureDrumPinningStillValid();
        oled.notifyBlink(blinkTicks);
        drumSequenceMode.notifyBlink(blinkTicks);
        notePlayMode.notifyBlink(blinkTicks);
        chordStepMode.notifyBlink(blinkTicks);
        melodicStepMode.notifyBlink(blinkTicks);
        performMode.notifyBlink(blinkTicks);
        host.scheduleTask(this::handlePing, 100);
    }

    private void onSysEx(final String msg) {
        // getHost().println("Sys Ex IN " + msg);
    }

    private void sendPadRgb(final int pad, final int r, final int g, final int b) {
        singleRgb[7] = (byte) pad;
        singleRgb[8] = (byte) r;
        singleRgb[9] = (byte) g;
        singleRgb[10] = (byte) b;
        midiOut.sendSysex(singleRgb);
    }

    private void setUpPreferences() {
        preferences = getHost().getPreferences();

        clipLaunchModePref = preferences.getEnumSetting("Clip Launch Mode",
                FireControlPreferences.CATEGORY_CLIP_LAUNCH,
                FireControlPreferences.CLIP_LAUNCH_MODES,
                FireControlPreferences.CLIP_LAUNCH_MODE_SYNCED);
        clipLaunchModePref.markInterested();

        clipLaunchQuantizationPref = preferences.getEnumSetting("Clip Launch Quantization",
                FireControlPreferences.CATEGORY_CLIP_LAUNCH,
                FireControlPreferences.CLIP_LAUNCH_QUANTIZATIONS,
                FireControlPreferences.QUANTIZATION_1);
        clipLaunchQuantizationPref.markInterested();
        clipLaunchQuantizationPref.addValueObserver(this::applyLaunchQuantizationPreference);
        applyLaunchQuantizationPreference(clipLaunchQuantizationPref.get());

        defaultClipLengthPref = preferences.getEnumSetting("Default Clip Length",
                FireControlPreferences.CATEGORY_CLIP_LAUNCH,
                FireControlPreferences.DEFAULT_CLIP_LENGTHS,
                FireControlPreferences.CLIP_LENGTH_2_BARS);
        defaultClipLengthPref.markInterested();

        mainEncoderStartupPref = preferences.getEnumSetting("SELECT Encoder Startup",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.MAIN_ENCODER_STARTUP_STATES,
                FireControlPreferences.MAIN_ENCODER_STARTUP_FUNCTION_SET);
        mainEncoderStartupPref.markInterested();
        mainEncoderStartupPref.addValueObserver(this::applyMainEncoderStartupPreference);
        applyMainEncoderStartupPreference(mainEncoderStartupPref.get());

        euclidScopePref = preferences.getEnumSetting("Euclid Scope",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.EUCLID_SCOPES,
                FireControlPreferences.EUCLID_SCOPE_FULL_CLIP);
        euclidScopePref.markInterested();

        defaultScalePref = preferences.getEnumSetting("Default Scale",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.DEFAULT_SCALES,
                FireControlPreferences.DEFAULT_SCALE_PIANO);
        defaultScalePref.markInterested();

        defaultRootKeyPref = preferences.getEnumSetting("Default Root Key",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.DEFAULT_ROOT_KEYS,
                FireControlPreferences.DEFAULT_ROOT_KEY);
        defaultRootKeyPref.markInterested();

        defaultNoteInputOctavePref = preferences.getEnumSetting("Default Note Input Octave",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.DEFAULT_NOTE_INPUT_OCTAVES,
                FireControlPreferences.DEFAULT_NOTE_INPUT_OCTAVE);
        defaultNoteInputOctavePref.markInterested();

        melodicSeedModePref = preferences.getEnumSetting("Melodic Seed Mode",
                FireControlPreferences.CATEGORY_GENERATIVE_CONTROL,
                FireControlPreferences.MELODIC_SEED_MODES,
                FireControlPreferences.MELODIC_SEED_MODE_RANDOM);
        melodicSeedModePref.markInterested();

        defaultVelocitySensitivityPref = preferences.getEnumSetting("Default Velocity Sensitivity",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.DEFAULT_VELOCITY_SENSITIVITIES,
                FireControlPreferences.DEFAULT_VELOCITY_SENSITIVITY);
        defaultVelocitySensitivityPref.markInterested();

        melodicFixedSeedPref = preferences.getNumberSetting("Melodic Fixed Seed",
                FireControlPreferences.CATEGORY_GENERATIVE_CONTROL,
                FireControlPreferences.MELODIC_FIXED_SEED_MIN,
                FireControlPreferences.MELODIC_FIXED_SEED_MAX,
                1,
                "",
                FireControlPreferences.MELODIC_FIXED_SEED_DEFAULT);
        melodicFixedSeedPref.markInterested();

        drumPinModePref = preferences.getEnumSetting("Drum Mode Pinning",
                FireControlPreferences.CATEGORY_PINNING,
                FireControlPreferences.DRUM_PIN_MODES,
                FireControlPreferences.DRUM_PIN_MODE_FIRST_DRUM_MACHINE);
        drumPinModePref.markInterested();
        drumPinModePref.addValueObserver(value -> {
            syncDrumPinningForActiveMode();
            if (!drumPinPreferenceObserved) {
                drumPinPreferenceObserved = true;
                return;
            }
            notifyAction("Drum Pin",
                    FireControlPreferences.shouldAutoPinFirstDrumMachine(value) ? "Automatic" : "Follow Selected");
        });

        padBrightnessPref = preferences.getNumberSetting("Pad Brightness",
                FireControlPreferences.CATEGORY_HARDWARE,
                FireControlPreferences.PAD_BRIGHTNESS_MIN,
                FireControlPreferences.PAD_BRIGHTNESS_MAX,
                FireControlPreferences.PAD_BRIGHTNESS_STEP,
                "%",
                FireControlPreferences.PAD_BRIGHTNESS_DEFAULT);
        padBrightnessPref.markInterested();
        padBrightnessPref.addRawValueObserver(value -> {
            padBrightness = FireControlPreferences.normalizePadBrightness(value);
            redrawRgbPads();
        });
        padBrightness = FireControlPreferences.normalizePadBrightness(padBrightnessPref.getRaw());

        padSaturationPref = preferences.getNumberSetting("Pad Saturation",
                FireControlPreferences.CATEGORY_HARDWARE,
                FireControlPreferences.PAD_SATURATION_MIN,
                FireControlPreferences.PAD_SATURATION_MAX,
                FireControlPreferences.PAD_SATURATION_STEP,
                "%",
                FireControlPreferences.PAD_SATURATION_DEFAULT);
        padSaturationPref.markInterested();
        padSaturationPref.addRawValueObserver(value -> {
            padSaturation = FireControlPreferences.normalizePadSaturation(value);
            redrawRgbPads();
        });
        padSaturation = FireControlPreferences.normalizePadSaturation(padSaturationPref.getRaw());

        encoderTouchResetPref = preferences.getBooleanSetting("Encoder touch reset",
                FireControlPreferences.CATEGORY_HARDWARE,
                FireControlPreferences.ENCODER_TOUCH_RESET_DEFAULT);
        encoderTouchResetPref.markInterested();
        encoderTouchResetEnabled = encoderTouchResetPref.get();
        encoderTouchResetPref.addValueObserver(value -> encoderTouchResetEnabled = value);

        // Deferred for now: the current live NOTE implementation still relies on
        // Bitwig key-translation updates, so "New Notes Only" does not preserve
        // already-held notes correctly when the pitch offset changes. Keep the
        // preference plumbing in code so it can be reintroduced once live notes
        // are driven by explicit per-pad note tracking instead.
        // livePitchOffsetBehaviorPref = preferences.getEnumSetting("Live Pitch Offset",
        //         FireControlPreferences.CATEGORY_FUNCTIONALITIES,
        //         FireControlPreferences.LIVE_PITCH_OFFSET_BEHAVIORS,
        //         FireControlPreferences.LIVE_PITCH_OFFSET_NEW_NOTES);
        // livePitchOffsetBehaviorPref.markInterested();

        stepSeqPadAuditionPref = preferences.getBooleanSetting("Step Seq Pad Audition",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                true);
        stepSeqPadAuditionPref.markInterested();

        screenNotificationsPref = preferences.getBooleanSetting("On-screen action notifications",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                true);
        screenNotificationsPref.markInterested();
    }

    private void setUpTransportControl() {
        transport.isPlaying().markInterested();
        transport.tempo().markInterested();
        transport.tempo().name().markInterested();
        transport.tempo().value().markInterested();
        transport.tempo().value().displayedValue().markInterested();
        transport.tempo().value().displayedValue().addValueObserver(value -> {
            tempoDisplayValue = value;
            if (tempoDisplayPending) {
                oled.valueInfo("Tempo", tempoDisplayValue);
                tempoDisplayPending = false;
            }
        });
        transport.playPosition().markInterested();
        transport.isArrangerRecordEnabled().markInterested();
        transport.isArrangerAutomationWriteEnabled().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        transport.isMetronomeEnabled().markInterested();
        transport.isClipLauncherAutomationWriteEnabled().markInterested();
        transport.isFillModeActive().markInterested();
        transport.defaultLaunchQuantization().markInterested();
        final BiColorButton playButton = addButton(NoteAssign.PLAY);
        playButton.bindPressed(mainLayer, this::togglePlay, this::getPlayState);
        final BiColorButton recButton = addButton(NoteAssign.REC);
        recButton.bindPressed(mainLayer, this::toggleRec, this::getRecordState);
        final BiColorButton stopButton = addButton(NoteAssign.STOP);
        stopButton.bindPressed(mainLayer, this::stopAction, BiColorLightState.RED_FULL);

        final BiColorButton shiftButton = addButton(NoteAssign.SHIFT);
        shiftButton.bind(mainLayer, shiftActive, BiColorLightState.RED_HALF, BiColorLightState.OFF);
        final BiColorButton altButton = addButton(NoteAssign.ALT);
        altButton.bind(mainLayer, altActive, BiColorLightState.RED_HALF, BiColorLightState.OFF);

        final BiColorButton m1Button = addButton(NoteAssign.MUTE_1);
        m1Button.bindPressed(mainLayer, this::dummyAction, BiColorLightState.RED_FULL);
        addButton(NoteAssign.MUTE_2);
        addButton(NoteAssign.MUTE_3);
        addButton(NoteAssign.MUTE_4);
        final BiColorButton stepButton = addButton(NoteAssign.STEP_SEQ);
        addButton(NoteAssign.KNOB_MODE, NoteAssign.KNOB_MODE_LIGHT.getNoteValue());
        addButton(NoteAssign.NOTE);
        //addButton(NoteAssign.DRUM);
        addButton(NoteAssign.PERFORM);
        addButton(NoteAssign.PATTERN_UP);
        addButton(NoteAssign.PATTERN_DOWN);
        addButton(NoteAssign.BANK_L);
        addButton(NoteAssign.BANK_R);
        final BiColorButton patternButton = addButton(NoteAssign.PATTERN);
        final BiColorButton drumButton = addButton(NoteAssign.DRUM);
        final BiColorButton noteButton = getButton(NoteAssign.NOTE);
        final BiColorButton performButton = getButton(NoteAssign.PERFORM);
        stepButton.bindPressed(mainLayer, this::handleStepPressed, this::getStepState);
        patternButton.bindPressed(mainLayer, this::handlePatternPressed, this::getPatternState);
        drumButton.bindPressed(mainLayer, this::handleDrumPressed, this::getDrumState);
        noteButton.bindPressed(mainLayer, this::handleNotePressed, this::getNoteState);
        performButton.bindPressed(mainLayer, this::handlePerformPressed, this::getPerformState);
        final BiColorButton browserButton = addButton(NoteAssign.BROWSER);
        browserButton.bindPressed(mainLayer, this::handleBrowserPressed, this::getBrowserLightState);
        stateLights[0] = createLight(NoteAssign.TRACK_SELECT_1);
        stateLights[1] = createLight(NoteAssign.TRACK_SELECT_2);
        stateLights[2] = createLight(NoteAssign.TRACK_SELECT_3);
        stateLights[3] = createLight(NoteAssign.TRACK_SELECT_4);
    }

    private BiColorButton addButton(final NoteAssign which, final int ccLightValue) {
        final BiColorButton button = new BiColorButton(which, this, ccLightValue);
        controlButtons.put(which, button);
        return button;
    }

    private BiColorButton addButton(final NoteAssign which) {
        final BiColorButton button = new BiColorButton(which, this);
        controlButtons.put(which, button);
        return button;
    }

    private MultiStateHardwareLight createLight(final NoteAssign assignment) {
        final MultiStateHardwareLight light = surface.createMultiStateHardwareLight(
                "BASIC_LIGHT_" + assignment.toString());
        final int ccValue = assignment.getNoteValue();
        light.state().onUpdateHardware(state -> {
            if (state instanceof BiColorLightState) {
                sendCC(ccValue, ((BiColorLightState) state).getStateValue());
            } else {
                sendCC(ccValue, 0);
            }
        });
        return light;
    }

    private BiColorLightState getPlayState() {
        return transport.isPlaying().get() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF;
    }

    public boolean isTransportPlaying() {
        return transport != null && transport.isPlaying().get();
    }

    private BiColorLightState getRecordState() {
        if (isGlobalAltHeld()) {
            return transport.isArrangerAutomationWriteEnabled().get() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
        }
        if (modeState.activeMode() == Mode.DRUM) {
            return transport.isClipLauncherOverdubEnabled().get() ? BiColorLightState.RED_FULL : BiColorLightState.OFF;
        }
        return transport.isArrangerRecordEnabled().get() ? BiColorLightState.RED_FULL : BiColorLightState.OFF;
    }

    private BiColorLightState getClipLauncherAutomationWriteEnabledState() {
        return transport.isClipLauncherAutomationWriteEnabled().get() ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF;
    }

    private BiColorLightState getArrangerAutomationWriteEnabledState() {
        return transport.isArrangerAutomationWriteEnabled().get() ? BiColorLightState.AMBER_FULL : BiColorLightState.OFF;
    }

    private BiColorLightState getPatternState() {
        if (getButton(NoteAssign.SHIFT).isPressed()) {
            return transport.isMetronomeEnabled().get() ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
        }
        if (isGlobalAltHeld()) {
            return transport.isClipLauncherOverdubEnabled().get() ? BiColorLightState.AMBER_HALF : BiColorLightState.AMBER_FULL;
        }
        return getClipLauncherAutomationWriteEnabledState();
    }

    private BiColorLightState getDrumState() {
        return modeState.activeMode() == Mode.DRUM ? activeDrumSubMode.getLightState() : BiColorLightState.OFF;
    }

    private BiColorLightState getNoteState() {
        if (modeState.activeMode() == Mode.NOTE_PLAY && notePlayMode != null) {
            return notePlayMode.getModeButtonLightState();
        }
        if (modeState.activeMode() == Mode.CHORD_STEP && chordStepMode != null) {
            return chordStepMode.getModeButtonLightState();
        }
        return BiColorLightState.OFF;
    }

    private BiColorLightState getStepState() {
        if (modeState.activeMode() == Mode.MELODIC_STEP && melodicStepMode != null) {
            return melodicStepMode.getModeButtonLightState();
        }
        return BiColorLightState.OFF;
    }

    private BiColorLightState getPerformState() {
        if (modeState.activeMode() != Mode.PERFORM) {
            return BiColorLightState.OFF;
        }
        return performMode != null && performMode.isSettingsMode()
                ? BiColorLightState.AMBER_FULL
                : BiColorLightState.GREEN_FULL;
    }

    private void dummyAction(final boolean pressed) {
    }

    private void stopAction(final boolean pressed) {
        if (!pressed) {
            return;
        }
        transport.stop();
        notifyAction("Transport", "Stop");
        oled.clearScreenDelayed();
    }

    private void toggleRec(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (isGlobalShiftHeld()) {
            return;
        }
        if (isGlobalAltHeld()) {
            final boolean nextState = !transport.isArrangerAutomationWriteEnabled().get();
            transport.isArrangerAutomationWriteEnabled().toggle();
            notifyAction("Arranger Write", nextState ? "On" : "Off");
            return;
        }
        if (modeState.activeMode() == Mode.DRUM) {
            final boolean nextState = !transport.isClipLauncherOverdubEnabled().get();
            transport.isClipLauncherOverdubEnabled().toggle();
            notifyAction("Clip Record", nextState ? "On" : "Off");
            return;
        }
        final boolean nextState = !transport.isArrangerRecordEnabled().get();
        transport.isArrangerRecordEnabled().toggle();
        notifyAction("Record", nextState ? "On" : "Off");
    }

    private void toggleClipLauncherAutomationWriteEnabled(final boolean pressed) {
        if (!pressed) {
            return;
        }
        final boolean enabled = transport.isClipLauncherAutomationWriteEnabled().get();
        transport.isClipLauncherAutomationWriteEnabled().set(!enabled);
    }

    private void handlePatternPressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (getButton(NoteAssign.SHIFT).isPressed()) {
            final boolean nextState = !transport.isMetronomeEnabled().get();
            transport.isMetronomeEnabled().toggle();
            notifyAction("Metronome", nextState ? "On" : "Off");
            return;
        }
        if (isGlobalAltHeld()) {
            final boolean nextState = !transport.isClipLauncherOverdubEnabled().get();
            transport.isClipLauncherOverdubEnabled().toggle();
            notifyAction("Launcher Overdub", nextState ? "On" : "Off");
            return;
        }
        toggleClipLauncherAutomationWriteEnabled(true);
        host.scheduleTask(
                () -> notifyAction("Clip Write",
                        transport.isClipLauncherAutomationWriteEnabled().get() ? "On" : "Off"),
                1);
    }

    public void notifyAction(final String title, final String value) {
        oled.valueInfo(title, value);
        if (screenNotificationsPref != null && screenNotificationsPref.get()) {
            host.showPopupNotification(title + ": " + value);
        }
    }

    public void notifyPopup(final String title, final String value) {
        if (screenNotificationsPref != null && screenNotificationsPref.get()) {
            host.showPopupNotification(title + ": " + value);
        }
    }

    private void handleDrumPressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (isGlobalAltHeld()) {
            return;
        }
        if (isGlobalShiftHeld()) {
            transport.tapTempo();
            tempoDisplayPending = true;
            return;
        }
        if (modeState.activeMode() == Mode.DRUM) {
            activeDrumSubMode = activeDrumSubMode.next();
        } else {
            modeState.activateDrum();
            switchActiveMode();
            notifyAction("Mode", "Drum");
        }
    }

    private void handleNotePressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (modeState.activeMode() == Mode.CHORD_STEP) {
            if (isGlobalAltHeld()) {
                chordStepMode.toggleSurfaceVariant();
                return;
            }
            modeState.activateNotePlay();
            switchActiveMode();
            notifyAction("Mode", notePlayMode.currentNoteSubModeLabel());
            return;
        }
        if (modeState.activeMode() != Mode.NOTE_PLAY) {
            modeState.activateNotePlay();
            switchActiveMode();
            notifyAction("Mode", notePlayMode.currentNoteSubModeLabel());
            return;
        }
        notePlayMode.cycleNoteSubMode();
        notifyAction("Mode", notePlayMode.currentNoteSubModeLabel());
    }

    private void handleStepPressed(final boolean pressed) {
        if (modeState.activeMode() == Mode.MELODIC_STEP) {
            if (!pressed && suppressNextMelodicStepRelease) {
                suppressNextMelodicStepRelease = false;
                return;
            }
            if (!isGlobalShiftHeld() && !isGlobalAltHeld()) {
                if (pressed) {
                    modeState.activateChordStep();
                    switchActiveMode();
                    notifyAction("Mode", "Chord Step");
                }
                return;
            }
            melodicStepMode.handleStepButton(pressed);
            return;
        }
        if (modeState.activeMode() == Mode.CHORD_STEP) {
            if (!pressed || isGlobalShiftHeld() || isGlobalAltHeld()) {
                return;
            }
            enterMelodicStepMode();
            return;
        }
        if (modeState.shouldIgnoreTopLevelStepPress(isGlobalShiftHeld(), isGlobalAltHeld())) {
            return;
        }
        if (!pressed) {
            return;
        }
        enterMelodicStepMode();
    }

    private void handlePerformPressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (isGlobalShiftHeld()) {
            if (modeState.activeMode() != Mode.PERFORM) {
                modeState.activatePerform();
                switchActiveMode();
            }
            performMode.enterOverview();
            notifyAction("Mode", "Settings");
            return;
        }
        if (modeState.activeMode() == Mode.PERFORM) {
            final boolean leavingSettings = performMode.isSettingsMode();
            if (leavingSettings) {
                performMode.exitOverview();
                notifyAction("Mode", performMode.modeLabel());
            } else {
                performMode.toggleOrientation();
                notifyAction("Mode", performMode.modeLabel());
            }
            return;
        }
        modeState.activatePerform();
        switchActiveMode();
        notifyAction("Mode", performMode.modeLabel());
    }

    private void togglePlay(final boolean pressed) {
        if (!pressed) {
            return;
        }
        // Alt+Play: retrigger the current clip without transport state change.
        if (drumSequenceMode.isAltHeld()) {
            final boolean wasPlaying = transport.isPlaying().get();
            drumSequenceMode.retrigger();
            notifyAction(wasPlaying ? "Clip" : "Transport", wasPlaying ? "Retrigger" : "Play");
            oled.clearScreenDelayed();
            return;
        }
        // Regular behavior: toggle play/stop, retrigger on start.
        if (transport.isPlaying().get()) {
            transport.isPlaying().set(false);
            notifyAction("Transport", "Stop");
            oled.clearScreenDelayed();
        } else {
            drumSequenceMode.retrigger();
            transport.restart();
            notifyAction("Transport", "Play");
            oled.clearScreenDelayed();
        }
    }

    private void setUpHardware() {
        for (int index = 0; index < 4; index++) {
            final int controlId = 16 + index;
            encoders[index] = new TouchEncoder(controlId, controlId, this);
        }
        mainEncoder = new TouchEncoder(0x76, 0x19, this);
        mainEncoder.bindEncoder(mainLayer, this::handleGlobalMainEncoder);
        mainEncoder.bindTouched(mainLayer, this::handleGlobalMainEncoderPress);

        for (int i = 0; i < rgbButtons.length; i++) {
            rgbButtons[i] = new RgbButton(i, this);
        }
    }

    public HardwareSurface getSurface() {
        return surface;
    }

    public MidiIn getMidiIn() {
        return midiIn;
    }

    public Layers getLayers() {
        return layers;
    }

    public NoteInput getNoteInput() {
        return noteInput;
    }

    public ViewCursorControl getViewControl() {
        return viewControl;
    }

    public RgbButton[] getRgbButtons() {
        return rgbButtons;
    }

    public TouchEncoder[] getEncoders() {
        return encoders;
    }

    public OledDisplay getOled() {
        return oled;
    }

    public BiColorButton getButton(final NoteAssign which) {
        return controlButtons.get(which);
    }

    public TouchEncoder getMainEncoder() {
        return mainEncoder;
    }

    public int getSharedRootNote() {
        return sharedMusicalContext.getRootNote();
    }

    public void setSharedRootNote(final int rootNote) {
        sharedMusicalContext.setRootNote(rootNote);
    }

    public void adjustSharedRootNote(final int amount) {
        sharedMusicalContext.adjustRootNote(amount);
    }

    public int getSharedScaleIndex() {
        return sharedMusicalContext.getScaleIndex();
    }

    public void setSharedScaleIndex(final int scaleIndex) {
        sharedMusicalContext.setScaleIndex(scaleIndex);
    }

    public boolean adjustSharedScaleIndex(final int amount, final int minimumScaleIndex) {
        return sharedMusicalContext.adjustScaleIndex(amount, minimumScaleIndex);
    }

    public int getSharedOctave() {
        return sharedMusicalContext.getOctave();
    }

    public void setSharedOctave(final int octave) {
        sharedMusicalContext.setOctave(octave);
    }

    public void adjustSharedOctave(final int amount) {
        sharedMusicalContext.adjustOctave(amount);
    }

    public int getSharedBaseMidiNote() {
        return sharedMusicalContext.getBaseMidiNote();
    }

    public String getSharedScaleDisplayName() {
        return sharedMusicalContext.getScaleDisplayName();
    }

    public MusicalScale getSharedMusicalScale() {
        final int effectiveScaleIndex = getSharedScaleIndex() == -1 ? 1 : getSharedScaleIndex();
        return MusicalScaleLibrary.getInstance().getMusicalScale(effectiveScaleIndex);
    }

    public SharedMusicalContext getSharedMusicalContext() {
        return sharedMusicalContext;
    }

    private void initializeSharedPitchContext() {
        setSharedScaleIndex(resolveDefaultSharedScaleIndex());
        setSharedRootNote(getDefaultRootKeyPreference());
        setSharedOctave(getDefaultNoteInputOctavePreference());
    }

    private int resolveDefaultSharedScaleIndex() {
        return switch (getDefaultScalePreference()) {
            case FireControlPreferences.DEFAULT_SCALE_MAJOR -> findScaleIndex("Major", 1);
            case FireControlPreferences.DEFAULT_SCALE_MINOR -> findScaleIndex("Minor", 2);
            case FireControlPreferences.DEFAULT_SCALE_HARMONIC_MINOR -> findScaleIndex("Harmonic Minor", 2);
            case FireControlPreferences.DEFAULT_SCALE_MELODIC_MINOR -> findScaleIndex("Jazz Minor", 2);
            case FireControlPreferences.DEFAULT_SCALE_MINOR_PENTATONIC -> findScaleIndex("Minor Pentatonic", 2);
            case FireControlPreferences.DEFAULT_SCALE_DORIAN -> findScaleIndex("Dorian", 2);
            case FireControlPreferences.DEFAULT_SCALE_MIXOLYDIAN -> findScaleIndex("Mixolydian", 1);
            default -> -1;
        };
    }

    private int findScaleIndex(final String scaleName, final int fallbackIndex) {
        final MusicalScaleLibrary scaleLibrary = MusicalScaleLibrary.getInstance();
        for (int i = 0; i < scaleLibrary.getMusicalScalesCount(); i++) {
            if (scaleLibrary.getMusicalScale(i).getName().equals(scaleName)) {
                return i;
            }
        }
        return fallbackIndex;
    }

    private void onMidi0(final ShortMidiMessage msg) {
        getHost().println("MIDI " + msg.getStatusByte() + " " + msg.getData1() + " " + msg.getData2());
    }

    @Override
    public void exit() {
        notifyAction("Exit", "Akai Fire");
    }

    @Override
    public void flush() {
        surface.updateHardware();
    }

    public void sendCC(final int ccNr, final int value) {
        if (lastCcValue[ccNr] == -1 || lastCcValue[ccNr] != value) {
            midiOut.sendMidi(Midi.CC, ccNr, value);
            lastCcValue[ccNr] = value;
        }
    }

    public void updateRgbPad(final int index, final RgbLigthState state) {
        currentPadStates[index] = state;
        sendScaledPadRgb(index, state);
    }

    private void redrawRgbPads() {
        for (int index = 0; index < currentPadStates.length; index++) {
            final RgbLigthState state = currentPadStates[index];
            if (state != null) {
                sendScaledPadRgb(index, state);
            }
        }
    }

    private void sendScaledPadRgb(final int index, final RgbLigthState state) {
        final int red = state.getRed() & 0xFF;
        final int green = state.getGreen() & 0xFF;
        final int blue = state.getBlue() & 0xFF;
        sendPadRgb(index,
                FireControlPreferences.scalePadColorComponent(red, red, green, blue, padBrightness, padSaturation),
                FireControlPreferences.scalePadColorComponent(green, red, green, blue, padBrightness, padSaturation),
                FireControlPreferences.scalePadColorComponent(blue, red, green, blue, padBrightness, padSaturation));
    }

    public MultiStateHardwareLight[] getStateLights() {
        return stateLights;
    }

    public boolean isEncoderTouchResetEnabled() {
        return encoderTouchResetEnabled;
    }

    private void switchActiveMode() {
        releaseAutoPinnedDrumContext(true);
        drumSequenceMode.deactivate();
        notePlayMode.deactivate();
        chordStepMode.deactivate();
        melodicStepMode.deactivate();
        performMode.deactivate();
        if (modeState.activeMode() == Mode.DRUM) {
            applyDrumPinningIfEnabled();
            drumSequenceMode.activate();
            refreshSurfaceLights();
            return;
        }
        if (modeState.activeMode() == Mode.NOTE_PLAY) {
            notePlayMode.activate();
        } else if (modeState.activeMode() == Mode.CHORD_STEP) {
            chordStepMode.activate();
        } else if (modeState.activeMode() == Mode.MELODIC_STEP) {
            melodicStepMode.activate();
        } else {
            performMode.activate();
        }
        refreshSurfaceLights();
    }

    private void refreshSurfaceLights() {
        flush();
        host.scheduleTask(this::flush, 0);
        host.scheduleTask(this::flush, 8);
    }

    private void applyLaunchQuantizationPreference(final String preferenceValue) {
        transport.defaultLaunchQuantization().set(FireControlPreferences.toLaunchQuantizationValue(preferenceValue));
    }

    public void toggleFillMode() {
        transport.isFillModeActive().toggle();
        notifyAction("Fill", transport.isFillModeActive().get() ? "On" : "Off");
    }

    public boolean isFillModeActive() {
        return transport != null && transport.isFillModeActive().get();
    }

    public BiColorLightState getFillLightState() {
        return transport.isFillModeActive().get() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
    }

    public BiColorLightState getStepFillLightState() {
        return isFillModeActive() ? BiColorLightState.AMBER_HALF : BiColorLightState.AMBER_FULL;
    }

    public String getClipLaunchModePreference() {
        return clipLaunchModePref == null
                ? FireControlPreferences.CLIP_LAUNCH_MODE_SYNCED
                : clipLaunchModePref.get();
    }

    public int getDefaultClipLengthBeats() {
        return (int) Math.round(defaultClipLengthPref == null
                ? FireControlPreferences.toClipLengthBeats(FireControlPreferences.CLIP_LENGTH_2_BARS)
                : FireControlPreferences.toClipLengthBeats(defaultClipLengthPref.get()));
    }

    public String getMainEncoderRolePreference() {
        return FireControlPreferences.normalizeMainEncoderRole(currentMainEncoderRole);
    }

    private void applyMainEncoderStartupPreference(final String preferenceValue) {
        final String startupState = FireControlPreferences.normalizeMainEncoderStartupState(preferenceValue);
        currentMainEncoderRole = FireControlPreferences.MAIN_ENCODER_STARTUP_LAST_TOUCHED.equals(startupState)
                ? FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED
                : FireControlPreferences.normalizeMainEncoderRole(alternateMainEncoderRole);
    }

    public String cycleMainEncoderRolePreference() {
        final String cycleSource = FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED.equals(getMainEncoderRolePreference())
                ? alternateMainEncoderRole
                : getMainEncoderRolePreference();
        final String nextRole = FireControlPreferences.nextAlternateMainEncoderRole(cycleSource);
        currentMainEncoderRole = nextRole;
        alternateMainEncoderRole = nextRole;
        notifyAction("Encoder Role", nextRole);
        return nextRole;
    }

    public String toggleMainEncoderRolePreference() {
        final String normalizedCurrentRole = getMainEncoderRolePreference();
        final String normalizedAlternateRole = FireControlPreferences.normalizeMainEncoderRole(alternateMainEncoderRole);
        final String nextRole = FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED.equals(normalizedCurrentRole)
                ? (FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED.equals(normalizedAlternateRole)
                ? FireControlPreferences.MAIN_ENCODER_TRACK_SELECT
                : normalizedAlternateRole)
                : FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED;
        currentMainEncoderRole = nextRole;
        notifyAction("Encoder Role", nextRole);
        return nextRole;
    }

    public boolean isStepSeqPadAuditionEnabled() {
        return stepSeqPadAuditionPref != null && stepSeqPadAuditionPref.get();
    }

    public String getDefaultScalePreference() {
        return defaultScalePref == null
                ? FireControlPreferences.DEFAULT_SCALE_PIANO
                : FireControlPreferences.normalizeDefaultScale(defaultScalePref.get());
    }

    public int getDefaultRootKeyPreference() {
        return defaultRootKeyPref == null
                ? FireControlPreferences.toDefaultRootKey(FireControlPreferences.DEFAULT_ROOT_KEY)
                : FireControlPreferences.toDefaultRootKey(defaultRootKeyPref.get());
    }

    public int getDefaultNoteInputOctavePreference() {
        return defaultNoteInputOctavePref == null
                ? FireControlPreferences.toDefaultNoteInputOctave(FireControlPreferences.DEFAULT_NOTE_INPUT_OCTAVE)
                : FireControlPreferences.toDefaultNoteInputOctave(defaultNoteInputOctavePref.get());
    }

    public int getDefaultVelocitySensitivityPreference() {
        return defaultVelocitySensitivityPref == null
                ? FireControlPreferences.toDefaultVelocitySensitivity(
                FireControlPreferences.DEFAULT_VELOCITY_SENSITIVITY)
                : FireControlPreferences.toDefaultVelocitySensitivity(defaultVelocitySensitivityPref.get());
    }

    public long initialMelodicSeed() {
        final String seedMode = melodicSeedModePref == null
                ? FireControlPreferences.MELODIC_SEED_MODE_RANDOM
                : FireControlPreferences.normalizeMelodicSeedMode(melodicSeedModePref.get());
        if (FireControlPreferences.MELODIC_SEED_MODE_FIXED.equals(seedMode)) {
            final long fixedSeed = melodicFixedSeedPref == null
                    ? FireControlPreferences.MELODIC_FIXED_SEED_DEFAULT
                    : Math.round(melodicFixedSeedPref.getRaw());
            return Math.max(FireControlPreferences.MELODIC_FIXED_SEED_MIN,
                    Math.min(FireControlPreferences.MELODIC_FIXED_SEED_MAX, fixedSeed));
        }
        return ThreadLocalRandom.current().nextLong(
                FireControlPreferences.MELODIC_FIXED_SEED_MIN,
                FireControlPreferences.MELODIC_FIXED_SEED_MAX + 1);
    }

    public void exitMelodicStepMode() {
        final Mode activeMode = modeState.exitMelodicStepMode();
        switchActiveMode();
        notifyAction("Mode", switch (activeMode) {
            case DRUM -> "Drum";
            case CHORD_STEP -> "Chord Step";
            case PERFORM -> "Perform";
            default -> "Note";
        });
    }

    public void enterMelodicStepMode() {
        suppressNextMelodicStepRelease = true;
        modeState.enterMelodicStepMode();
        switchActiveMode();
        notifyAction("Mode", "Step");
    }

    public boolean isEuclidFullClipEnabled() {
        return euclidScopePref != null
                && FireControlPreferences.EUCLID_SCOPE_FULL_CLIP.equals(
                FireControlPreferences.normalizeEuclidScope(euclidScopePref.get()));
    }

    public boolean shouldRetuneHeldLiveNotes() {
        return livePitchOffsetBehaviorPref != null
                && FireControlPreferences.LIVE_PITCH_OFFSET_RETUNE_HELD.equals(
                FireControlPreferences.normalizeLivePitchOffsetBehavior(livePitchOffsetBehaviorPref.get()));
    }

    public boolean shouldAutoPinFirstDrumMachine() {
        return drumPinModePref != null
                && FireControlPreferences.shouldAutoPinFirstDrumMachine(drumPinModePref.get());
    }

    private void syncDrumPinningForActiveMode() {
        if (modeState.activeMode() == Mode.DRUM) {
            if (shouldAutoPinFirstDrumMachine()) {
                applyDrumPinningIfEnabled();
            } else {
                releaseAutoPinnedDrumContext(false);
            }
            return;
        }
        releaseAutoPinnedDrumContext(true);
    }

    private void applyDrumPinningIfEnabled() {
        if (!shouldAutoPinFirstDrumMachine() || drumAutoPinApplied || deviceLocator == null || viewControl == null) {
            return;
        }

        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final PinnableCursorDevice primaryDevice = viewControl.getPrimaryDevice();
        drumTrackPinnedBeforeAutoPin = cursorTrack.isPinned().get();
        drumDevicePinnedBeforeAutoPin = primaryDevice.isPinned().get();
        drumTrackIndexBeforeAutoPin = cursorTrack.position().get();

        if (!deviceLocator.focusFirstDrumMachine(viewControl)) {
            return;
        }

        cursorTrack.isPinned().set(true);
        primaryDevice.isPinned().set(true);
        drumAutoPinApplied = true;
    }

    private void ensureDrumPinningStillValid() {
        if (modeState.activeMode() != Mode.DRUM || !shouldAutoPinFirstDrumMachine() || !drumAutoPinApplied
                || viewControl == null || deviceLocator == null) {
            return;
        }

        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final PinnableCursorDevice primaryDevice = viewControl.getPrimaryDevice();
        final boolean invalidTrackPin = !cursorTrack.isPinned().get();
        final boolean invalidDevicePin = !primaryDevice.isPinned().get();
        final boolean invalidDrumContext = !primaryDevice.exists().get() || !primaryDevice.hasDrumPads().get();
        if (!invalidTrackPin && !invalidDevicePin && !invalidDrumContext) {
            return;
        }

        if (deviceLocator.focusFirstDrumMachine(viewControl)) {
            cursorTrack.isPinned().set(true);
            primaryDevice.isPinned().set(true);
        }
    }

    private void releaseAutoPinnedDrumContext(final boolean restorePreviousState) {
        if (!drumAutoPinApplied || viewControl == null) {
            return;
        }

        if (restorePreviousState) {
            viewControl.getCursorTrack().isPinned().set(drumTrackPinnedBeforeAutoPin);
            viewControl.getPrimaryDevice().isPinned().set(drumDevicePinnedBeforeAutoPin);
            restoreTrackSelection(drumTrackIndexBeforeAutoPin);
        } else {
            viewControl.getCursorTrack().isPinned().set(false);
            viewControl.getPrimaryDevice().isPinned().set(false);
        }
        drumAutoPinApplied = false;
    }

    private void restoreTrackSelection(final int trackIndex) {
        if (viewControl == null || trackIndex < 0) {
            return;
        }

        final TrackBank trackBank = viewControl.getTrackBank();
        if (trackBank == null || trackIndex >= trackBank.getSizeOfBank()) {
            return;
        }

        final Track track = trackBank.getItemAt(trackIndex);
        if (track == null || !track.exists().get()) {
            return;
        }

        track.selectInMixer();
        track.selectInEditor();
    }

    public void adjustMainCursorParameter(final int inc, final boolean fine) {
        final Parameter parameter = getLastClickedParameter();
        if (parameter == null) {
            oled.valueInfo("Last Touched", "No Target");
            return;
        }
        final SettableRangedValue value = parameter.value();
        final double stepSize = fine ? MAIN_ENCODER_FINE_STEP : MAIN_ENCODER_STEP;
        final double nextValue = Math.max(0.0, Math.min(1.0, value.get() + (inc * stepSize)));
        value.setImmediately(nextValue);
        oled.valueInfo(parameter.name().get(), parameter.displayedValue().get());
    }

    public void showMainCursorParameterInfo() {
        final Parameter parameter = getLastClickedParameter();
        if (parameter == null) {
            oled.valueInfo("Last Touched", "No Target");
            return;
        }
        oled.valueInfo(parameter.name().get(), parameter.displayedValue().get());
    }

    private void showIdleOledInfo() {
        final Parameter parameter = getLastClickedParameter();
        if (parameter != null) {
            oled.valueInfo(parameter.name().get(), parameter.displayedValue().get());
            return;
        }
        final String modeLabel = switch (modeState.activeMode()) {
            case DRUM -> "Drum";
            case NOTE_PLAY -> "Note";
            case CHORD_STEP -> "Chord Step";
            case MELODIC_STEP -> "Melodic Step";
            case PERFORM -> "Perform";
        };
        oled.valueInfo("Mode", modeLabel);
    }

    public void resetMainCursorParameter() {
        final Parameter parameter = getLastClickedParameter();
        if (parameter == null) {
            return;
        }
        parameter.reset();
        oled.valueInfo(parameter.name().get(), parameter.displayedValue().get());
    }

    private Parameter getLastClickedParameter() {
        if (lastClickedParameter == null) {
            return null;
        }
        final Parameter parameter = lastClickedParameter.parameter();
        if (parameter == null || !parameter.exists().get()) {
            return null;
        }
        final String name = parameter.name().get();
        return name == null || name.isBlank() ? null : parameter;
    }

    public void adjustGrooveShuffleAmount(final int inc, final boolean fine) {
        if (groove == null) {
            return;
        }
        final Parameter shuffleAmount = groove.getShuffleAmount();
        final SettableRangedValue value = shuffleAmount.value();
        final SettableRangedValue enabledValue = groove.getEnabled().value();
        final double stepSize = fine ? MAIN_ENCODER_FINE_STEP : MAIN_ENCODER_STEP;
        final double nextValue = Math.max(0.0, Math.min(1.0, value.get() + (inc * stepSize)));
        value.setImmediately(nextValue);
        final boolean shuffleEnabled = nextValue > 0.0;
        enabledValue.setImmediately(shuffleEnabled ? 1.0 : 0.0);
        oled.valueInfo("Shuffle", shuffleEnabled ? shuffleAmount.displayedValue().get() : "Off");
    }

    public void adjustTempo(final int inc, final boolean fine) {
        if (inc == 0) {
            return;
        }
        tempoDisplayPending = true;
        transport.tempo().incRaw(inc * (fine ? 0.1 : 1.0));
    }

    public void showTempoInfo() {
        oled.valueInfo("Tempo", tempoDisplayValue);
    }

    public void showGrooveShuffleInfo() {
        if (groove == null) {
            return;
        }
        oled.valueInfo("Shuffle", groove.getShuffleAmount().displayedValue().get());
    }

    public boolean isPopupBrowserActive() {
        return popupBrowser != null && popupBrowser.exists().get();
    }

    public BiColorLightState getBrowserLightState() {
        return isPopupBrowserActive() ? BiColorLightState.RED_FULL : BiColorLightState.OFF;
    }

    public boolean isGlobalAltHeld() {
        return altActive.get();
    }

    public boolean isGlobalShiftHeld() {
        return shiftActive.get();
    }

    public void setMainEncoderPressed(final boolean pressed) {
        mainEncoderPressed = pressed;
        if (pressed) {
            mainEncoderTurnedWhilePressed = false;
        }
    }

    public boolean isMainEncoderPressed() {
        return mainEncoderPressed;
    }

    public void markMainEncoderTurned() {
        if (mainEncoderPressed) {
            mainEncoderTurnedWhilePressed = true;
        }
    }

    public boolean wasMainEncoderTurnedWhilePressed() {
        return mainEncoderTurnedWhilePressed;
    }

    public void adjustSelectedTrack(final int inc, final boolean pageStep) {
        if (inc == 0 || viewControl == null) {
            return;
        }
        if (modeState.activeMode() == Mode.DRUM && shouldAutoPinFirstDrumMachine()) {
            oled.valueInfo("Track Sel.", "Pinned");
            notifyPopup("Track Sel.", "Pinned");
            return;
        }
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final TrackBank trackBank = viewControl.getTrackBank();
        if (trackBank == null) {
            return;
        }
        final int currentIndex = viewControl.getSelectedTrackIndex();
        final int delta = inc * (pageStep ? 8 : 1);
        final int targetIndex = Math.max(0, Math.min(trackBank.getSizeOfBank() - 1, currentIndex + delta));
        final Track targetTrack = trackBank.getItemAt(targetIndex);
        if (targetTrack == null || !targetTrack.exists().get()) {
            return;
        }
        targetTrack.selectInMixer();
        targetTrack.selectInEditor();
        cursorTrack.selectChannel(targetTrack);
        final String trackName = targetTrack.name().get();
        showSelectedTrackInfo(pageStep, trackName);
    }

    public void showSelectedTrackInfo(final boolean pageStep) {
        if (viewControl == null) {
            return;
        }
        final String trackName = viewControl.getCursorTrack().name().get();
        showSelectedTrackInfo(pageStep, trackName);
    }

    private void showSelectedTrackInfo(final boolean pageStep, final String trackName) {
        oled.valueInfo(pageStep ? "Track Page" : "Track Select",
                trackName == null || trackName.isBlank() ? "Unnamed" : trackName);
    }

    private void handleBrowserPressed(final boolean pressed) {
        if (!pressed) {
            oled.clearScreenDelayed();
            return;
        }
        if (isPopupBrowserActive()) {
            popupBrowser.cancel();
            notifyAction("Browser", "Closed");
            return;
        }
        openPopupBrowser();
    }

    private void openPopupBrowser() {
        final PinnableCursorDevice primaryDevice = viewControl.getPrimaryDevice();
        final boolean shiftHeld = getButton(NoteAssign.SHIFT).isPressed();
        final boolean altHeld = getButton(NoteAssign.ALT).isPressed();
        if (shiftHeld) {
            if (primaryDevice.exists().get()) {
                primaryDevice.beforeDeviceInsertionPoint().browse();
            } else {
                viewControl.getCursorTrack().startOfDeviceChainInsertionPoint().browse();
            }
            scheduleBrowserResultsSelectionPrime();
            notifyAction("Browser", "Before");
            return;
        }
        if (altHeld) {
            if (primaryDevice.exists().get()) {
                primaryDevice.afterDeviceInsertionPoint().browse();
            } else {
                viewControl.getCursorTrack().endOfDeviceChainInsertionPoint().browse();
            }
            scheduleBrowserResultsSelectionPrime();
            notifyAction("Browser", "After");
            return;
        }
        if (primaryDevice.exists().get()) {
            primaryDevice.replaceDeviceInsertionPoint().browse();
            scheduleBrowserResultsSelectionPrime();
            notifyAction("Browser", "Replace");
        } else {
            viewControl.getCursorTrack().endOfDeviceChainInsertionPoint().browse();
            scheduleBrowserResultsSelectionPrime();
            notifyAction("Browser", "Add");
        }
    }

    private void scheduleBrowserResultsSelectionPrime() {
        for (final int delayMs : BROWSER_RESULTS_PRIME_DELAYS_MS) {
            host.scheduleTask(this::primeBrowserResultsSelection, delayMs);
        }
    }

    private void primeBrowserResultsSelection() {
        if (!isPopupBrowserActive()) {
            return;
        }
        if (browserResultsCursor.exists().get()) {
            if (!browserResultsCursor.isSelected().get()) {
                browserResultsCursor.isSelected().set(true);
            }
            return;
        }
        popupBrowser.selectFirstFile();
        if (browserResultsCursor.exists().get()) {
            browserResultsCursor.isSelected().set(true);
        }
    }

    private void handleGlobalMainEncoder(final int inc) {
        if (!isPopupBrowserActive()) {
            return;
        }
        if (inc > 0) {
            for (int i = 0; i < inc; i++) {
                popupBrowser.selectNextFile();
            }
        } else if (inc < 0) {
            for (int i = 0; i < -inc; i++) {
                popupBrowser.selectPreviousFile();
            }
        }
        oled.valueInfo("Browser", browserResultsCursor.exists().get() ? browserResultsCursor.name().get() : "No Results");
    }

    public void routeBrowserMainEncoder(final int inc) {
        handleGlobalMainEncoder(inc);
    }

    private void handleGlobalMainEncoderPress(final boolean press) {
        if (!isPopupBrowserActive()) {
            return;
        }
        if (press) {
            popupBrowser.commit();
            notifyAction("Browser", "Commit");
        }
    }

    public void routeBrowserMainEncoderPress(final boolean press) {
        handleGlobalMainEncoderPress(press);
    }

    public static AkaiFireOikontrolExtension getInstance() {
        return instance;
    }

    public static ControllerHost getGlobalHost() {
        return instance.getHost();
    }

    public PatternButtons getPatternButtons() {
        return patternButtons;
    }
}
