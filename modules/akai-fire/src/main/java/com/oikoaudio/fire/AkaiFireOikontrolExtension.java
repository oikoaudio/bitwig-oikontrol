package com.oikoaudio.fire;

import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.EncoderStepAccumulator;
import com.oikoaudio.fire.control.ModeButtonLights;
import com.oikoaudio.fire.control.ParameterEncoderBinding;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.control.UndoRedoBankButtonHandler;
import com.oikoaudio.fire.control.VelocitySettings;
import com.oikoaudio.fire.chordstep.ChordStepMode;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.fugue.FugueStepMode;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.melodic.MelodicStepMode;
import com.oikoaudio.fire.nestedrhythm.NestedRhythmMode;
import com.oikoaudio.fire.music.SharedPitchContextController;
import com.oikoaudio.fire.TopLevelModeState.Mode;
import com.bitwig.extensions.framework.MusicalScale;
import com.oikoaudio.fire.note.DrumPadPlayMode;
import com.oikoaudio.fire.note.NotePlayMode;
import com.oikoaudio.fire.perform.PerformClipLauncherMode;
import com.oikoaudio.fire.sequence.DrumSequenceMode;
import com.oikoaudio.fire.sequence.EncoderMode;
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
    enum BrowserTransportAction {
        NONE,
        COMMIT,
        CANCEL
    }

    enum PlayPressAction {
        STOP,
        RETRIGGER_LAUNCHERS_FROM_START,
        LAUNCH_FROM_PLAY_START
    }

    enum StopPressAction {
        STOP,
        GO_ARRANGEMENT_START
    }

    enum PatternReleaseAction {
        NONE,
        TOGGLE_AUTOMATION_WRITE,
        TOGGLE_METRONOME,
        TOGGLE_LAUNCHER_OVERDUB
    }

    record LauncherOverdubPressPlan(boolean launcherOverdubEnabled,
                                    boolean automationWriteEnabled,
                                    boolean touchAutomationWriteMode,
                                    boolean automationWriteAutoEnabled) {
    }

    public record RemotePageTarget(CursorRemoteControlsPage page, String label) {
    }

    private static final double MAIN_ENCODER_STEP = 0.01;
    private static final double MAIN_ENCODER_FINE_STEP = 0.0025;
    private static final double LAST_TOUCHED_ENCODER_STEP = 0.04;
    private static final double LAST_TOUCHED_ENCODER_FINE_STEP = 0.01;
    private static final int DEVICE_DISCOVERY_WIDTH = 128;
    private static final int CUE_MARKER_BANK_SIZE = 128;
    private static final int[] BROWSER_RESULTS_PRIME_DELAYS_MS = {0, 1, 10, 30};
    private static final int BROWSER_OPEN_DEFER_MS = 40;
    private static final int SETTINGS_PAD_COLUMNS = 16;
    private static final int SETTINGS_PAD_ROWS = 4;
    private static final int SETTINGS_SHOW_DEACTIVATED_TRACKS_PAD = 63;
    private static final int GLOBAL_ROOT_ENCODER_THRESHOLD = 16;
    private static final int GLOBAL_SCALE_ENCODER_THRESHOLD = 8;
    private static final int GLOBAL_OCTAVE_ENCODER_THRESHOLD = 8;
    private static final int GLOBAL_VELOCITY_CENTER_DEFAULT = 100;
    private static final int GLOBAL_VELOCITY_MIN = 1;
    private static final int GLOBAL_VELOCITY_MAX = 126;
    private static final RgbLigthState SETTINGS_LOGO_ON = new RgbLigthState(127, 20, 0, true);
    private static final RgbLigthState SETTINGS_LOGO_OFF = RgbLigthState.OFF;
    private static final RgbLigthState SETTINGS_TOGGLE_ON = new RgbLigthState(0, 96, 96, true);
    private static final RgbLigthState SETTINGS_TOGGLE_OFF = new RgbLigthState(0, 32, 32, true);
    private static final boolean[][] SETTINGS_LOGO = {
            {true, true, true, false, true, true, true, false, true, true, true, false, true, true, true, true},
            {true, false, false, false, false, true, false, false, true, false, true, false, true, false, false, false},
            {true, true, false, false, false, true, false, false, true, true, false, false, true, true, true, false},
            {true, false, false, false, true, true, true, false, true, false, true, false, true, true, true, true}
    };
    public static final String MAIN_ENCODER_LAST_TOUCHED_ROLE = FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED;
    public static final String MAIN_ENCODER_SHUFFLE_ROLE = FireControlPreferences.MAIN_ENCODER_SHUFFLE;
    public static final String MAIN_ENCODER_TEMPO_ROLE = FireControlPreferences.MAIN_ENCODER_TEMPO;
    public static final String MAIN_ENCODER_NOTE_REPEAT_ROLE = FireControlPreferences.MAIN_ENCODER_NOTE_REPEAT;
    public static final String MAIN_ENCODER_TRACK_SELECT_ROLE = FireControlPreferences.MAIN_ENCODER_TRACK_SELECT;
    public static final String MAIN_ENCODER_PLAYBACK_START_ROLE = FireControlPreferences.MAIN_ENCODER_PLAYBACK_START;
    public static final String MAIN_ENCODER_DRUM_GRID_ROLE = FireControlPreferences.MAIN_ENCODER_DRUM_GRID;
    private static final String ACTION_JUMP_TO_END_OF_ARRANGEMENT = "jump_to_end_of_arrangement";
    private static final String RECORD_QUANTIZATION_OFF = "OFF";
    private static final String RECORD_QUANTIZATION_DEFAULT_ON = "1/16";
    private static final String AUTOMATION_WRITE_MODE_TOUCH = "touch";
    private static final long STOPPED_METER_RING_OUT_MS = 2000;
    private static AkaiFireOikontrolExtension instance;
    private HardwareSurface surface;
    private Application application;
    private Transport transport;
    private Arranger arranger;
    private DetailEditor detailEditor;
    private CueMarkerBank cueMarkerBank;
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
    private Layer globalSettingsLayer;
    private final EncoderStepAccumulator[] globalSettingsAccumulators = new EncoderStepAccumulator[]{
            new EncoderStepAccumulator(GLOBAL_ROOT_ENCODER_THRESHOLD),
            new EncoderStepAccumulator(GLOBAL_SCALE_ENCODER_THRESHOLD),
            new EncoderStepAccumulator(GLOBAL_OCTAVE_ENCODER_THRESHOLD),
            new EncoderStepAccumulator(GLOBAL_SCALE_ENCODER_THRESHOLD)
    };
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
    private final boolean[] cueMarkerExists = new boolean[CUE_MARKER_BANK_SIZE];
    private final double[] cueMarkerPositions = new double[CUE_MARKER_BANK_SIZE];
    private final String[] cueMarkerNames = new String[CUE_MARKER_BANK_SIZE];

    private Preferences preferences;
    private SettableEnumValue clipLaunchModePref;
    private SettableEnumValue clipLaunchQuantizationPref;
    private SettableEnumValue performClipLauncherLayoutPref;
    private SettableEnumValue defaultClipLengthPref;
    private SettableEnumValue launcherRecordLengthPref;
    private SettableEnumValue startupModePref;
    private SettableEnumValue mainEncoderStartupPref;
    private SettableEnumValue euclidScopePref;
    private SettableEnumValue drumPinModePref;
    private SettableEnumValue defaultScalePref;
    private SettableEnumValue defaultRootKeyPref;
    private SettableEnumValue defaultNoteInputOctavePref;
    private SettableEnumValue defaultVelocitySensitivityPref;
    private SettableEnumValue melodicSeedModePref;
    private SettableEnumValue livePitchOffsetBehaviorPref;
    private SettableEnumValue screenMessageHoldPref;
    private SettableBooleanValue encoderTouchResetPref;
    private SettableBooleanValue showDeactivatedTracksPref;
    private SettableBooleanValue exclusiveTrackArmPref;
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
    private boolean knobModeGestureConsumed = false;
    private boolean patternPressed = false;
    private boolean patternGestureConsumed = false;
    private boolean patternPressShiftHeld = false;
    private boolean patternPressAltHeld = false;
    private boolean launcherOverdubAutoEnabledAutomationWrite = false;
    private boolean retriggerLaunchersOnNextPlay = false;
    private boolean recordGestureConsumed = false;
    private boolean suppressNextMelodicStepRelease = false;
    private boolean drumPinPreferenceObserved = false;
    private boolean globalSettingsOverlayActive = false;
    private final GlobalSettingsOverlayLatch globalSettingsOverlayLatch = new GlobalSettingsOverlayLatch();
    private EncoderMode globalSettingsEncoderMode = EncoderMode.CHANNEL;
    private String lastRecordQuantizationGrid = RECORD_QUANTIZATION_DEFAULT_ON;
    private int browserPressToken = 0;
    private int transportTimeSignatureNumerator = 4;
    private int transportTimeSignatureDenominator = 4;
    private double cueMarkerNavigationPosition = Double.NaN;
    private boolean performRecordPadGestureConsumed = false;
    private double padBrightness = FireControlPreferences.PAD_BRIGHTNESS_DEFAULT;
    private double padSaturation = FireControlPreferences.PAD_SATURATION_DEFAULT;
    private boolean encoderTouchResetEnabled = FireControlPreferences.ENCODER_TOUCH_RESET_DEFAULT;
    private boolean exclusiveTrackArmEnabled = FireControlPreferences.EXCLUSIVE_TRACK_ARM_DEFAULT;
    private long screenMessageHoldMs = FireControlPreferences.SCREEN_MESSAGE_HOLD_NORMAL_MS;
    private long stoppedMeterRingOutUntilMs = 0;
    private int stoppedMeterRingOutGeneration = 0;
    private final SharedPitchContextController sharedPitchContext = new SharedPitchContextController(
            new SharedMusicalContext(MusicalScaleLibrary.getInstance()),
            MusicalScaleLibrary.getInstance());
    private final VelocitySettings sharedVelocitySettings = new VelocitySettings(
            GLOBAL_VELOCITY_CENTER_DEFAULT,
            GLOBAL_VELOCITY_MIN,
            GLOBAL_VELOCITY_MAX,
            100);

    private PatternButtons patternButtons;
    private NotePlayMode notePlayMode;
    private DrumPadPlayMode drumPadPlayMode;
    private ChordStepMode chordStepMode;
    private MelodicStepMode melodicStepMode;
    private FugueStepMode fugueStepMode;
    private NestedRhythmMode nestedRhythmMode;
    private PerformClipLauncherMode performMode;
    private NoteRepeatHandler noteRepeatHandler;
    private final TopLevelModeState modeState = new TopLevelModeState();
    private DrumSubMode activeDrumSubMode = DrumSubMode.STANDARD;
    private String currentMainEncoderRole = FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED;
    private String alternateMainEncoderRole = FireControlPreferences.MAIN_ENCODER_TRACK_SELECT;

    private enum DrumSubMode {
        STANDARD(ModeButtonLights.MODE_1),
        NESTED_RHYTHM(ModeButtonLights.MODE_2),
        DRUM_PADS(ModeButtonLights.MODE_3);

        private final BiColorLightState lightState;

        DrumSubMode(final BiColorLightState lightState) {
            this.lightState = lightState;
        }

        public BiColorLightState getLightState() {
            return lightState;
        }

        public DrumSubMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public String displayName() {
            return switch (this) {
                case STANDARD -> "Drum XOX";
                case NESTED_RHYTHM -> "NestedRytm";
                case DRUM_PADS -> "Drum Pads";
            };
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
        application = host.createApplication();
        application.canUndo().markInterested();
        application.canRedo().markInterested();
        arranger = host.createArranger();
        cueMarkerBank = arranger.createCueMarkerBank(CUE_MARKER_BANK_SIZE);
        initializeCueMarkerBank();
        detailEditor = host.createDetailEditor();
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
        application.recordQuantizationGrid().markInterested();
        application.recordQuantizationGrid().addValueObserver(this::handleRecordQuantizationChanged);

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
        globalSettingsLayer = new Layer(layers, "GlobalSettings");
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
        initializeSharedVelocitySettings();

        patternButtons = new PatternButtons(this, mainLayer);
        drumSequenceMode = new DrumSequenceMode(this, noteRepeatHandler);
        notePlayMode = new NotePlayMode(this, noteRepeatHandler);
        drumPadPlayMode = new DrumPadPlayMode(this, noteRepeatHandler);
        chordStepMode = new ChordStepMode(this);
        melodicStepMode = new MelodicStepMode(this, noteRepeatHandler);
        fugueStepMode = new FugueStepMode(this);
        nestedRhythmMode = new NestedRhythmMode(this);
        performMode = new PerformClipLauncherMode(this);
        applyStartupModePreference();
        initGlobalSettingsOverlay();
        oled.setIdleAction(this::showIdleOledInfo);
        midiOut.sendSysex(DEV_INQ);

        oled.valueInfo("Oikontrol", "");
        mainLayer.activate();
        switchActiveMode();
        host.scheduleTask(this::handlePing, 100);
        notifyPopup("Fire Oikontrol", "Active");

    }

    private void handlePing() {
        // sections.forEach(section -> section.notifyBlink(blinkTicks));
        blinkTicks++;
        ensureDrumPinningStillValid();
        oled.notifyBlink(blinkTicks);
        drumSequenceMode.notifyBlink(blinkTicks);
        notePlayMode.notifyBlink(blinkTicks);
        drumPadPlayMode.notifyBlink(blinkTicks);
        chordStepMode.notifyBlink(blinkTicks);
        melodicStepMode.notifyBlink(blinkTicks);
        fugueStepMode.notifyBlink(blinkTicks);
        nestedRhythmMode.notifyBlink(blinkTicks);
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

        performClipLauncherLayoutPref = preferences.getEnumSetting("Perform Clip Launcher Layout",
                FireControlPreferences.CATEGORY_CLIP_LAUNCH,
                FireControlPreferences.PERFORM_LAYOUTS,
                FireControlPreferences.PERFORM_LAYOUT_VERTICAL);
        performClipLauncherLayoutPref.markInterested();

        defaultClipLengthPref = preferences.getEnumSetting("Default Clip Length",
                FireControlPreferences.CATEGORY_CLIP_LAUNCH,
                FireControlPreferences.DEFAULT_CLIP_LENGTHS,
                FireControlPreferences.CLIP_LENGTH_2_BARS);
        defaultClipLengthPref.markInterested();

        launcherRecordLengthPref = preferences.getEnumSetting("Launcher Record Length",
                FireControlPreferences.CATEGORY_CLIP_LAUNCH,
                FireControlPreferences.LAUNCHER_RECORD_LENGTHS,
                FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_2_BARS);
        launcherRecordLengthPref.markInterested();

        startupModePref = preferences.getEnumSetting("Startup Mode",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.STARTUP_MODES,
                FireControlPreferences.STARTUP_MODE_NOTE);
        startupModePref.markInterested();

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
                FireControlPreferences.DEFAULT_SCALE_MAJOR);
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

        screenMessageHoldPref = preferences.getEnumSetting("Screen Message Hold",
                FireControlPreferences.CATEGORY_HARDWARE,
                FireControlPreferences.SCREEN_MESSAGE_HOLDS,
                FireControlPreferences.SCREEN_MESSAGE_HOLD_NORMAL);
        screenMessageHoldPref.markInterested();
        screenMessageHoldPref.addValueObserver(this::applyScreenMessageHoldPreference);
        applyScreenMessageHoldPreference(screenMessageHoldPref.get());

        showDeactivatedTracksPref = preferences.getBooleanSetting("Show deactivated tracks",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.SHOW_DEACTIVATED_TRACKS_DEFAULT);
        showDeactivatedTracksPref.markInterested();

        exclusiveTrackArmPref = preferences.getBooleanSetting("Exclusive Track Arm",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.EXCLUSIVE_TRACK_ARM_DEFAULT);
        exclusiveTrackArmPref.markInterested();
        exclusiveTrackArmEnabled = exclusiveTrackArmPref.get();
        exclusiveTrackArmPref.addValueObserver(value -> exclusiveTrackArmEnabled = value);

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
        transport.isPlaying().addValueObserver(this::handleTransportPlayingChanged);
        transport.getPosition().markInterested();
        transport.playStartPosition().markInterested();
        transport.isArrangerLoopEnabled().markInterested();
        transport.arrangerLoopStart().markInterested();
        transport.arrangerLoopDuration().markInterested();
        arranger.getHorizontalScrollbarModel().getContentPerPixel().markInterested();
        transport.timeSignature().markInterested();
        transport.timeSignature().numerator().markInterested();
        transport.timeSignature().denominator().markInterested();
        transport.timeSignature().numerator().addValueObserver(value -> transportTimeSignatureNumerator = value);
        transport.timeSignature().denominator().addValueObserver(value -> transportTimeSignatureDenominator = value);
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
        transport.isArrangerOverdubEnabled().markInterested();
        transport.isArrangerAutomationWriteEnabled().markInterested();
        transport.isClipLauncherAutomationWriteEnabled().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        transport.automationWriteMode().markInterested();
        transport.isMetronomeEnabled().markInterested();
        transport.isFillModeActive().markInterested();
        transport.defaultLaunchQuantization().markInterested();
        transport.clipLauncherPostRecordingAction().markInterested();
        transport.getClipLauncherPostRecordingTimeOffset().markInterested();
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
        shiftActive.addValueObserver(ignored -> updateGlobalSettingsOverlayState());
        altActive.addValueObserver(ignored -> updateGlobalSettingsOverlayState());

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

    private void initializeCueMarkerBank() {
        cueMarkerBank.itemCount().markInterested();
        for (int index = 0; index < CUE_MARKER_BANK_SIZE; index++) {
            final int markerIndex = index;
            final CueMarker marker = cueMarkerBank.getItemAt(index);
            marker.exists().markInterested();
            marker.exists().addValueObserver(exists -> cueMarkerExists[markerIndex] = exists);
            marker.position().markInterested();
            marker.position().addValueObserver(position -> cueMarkerPositions[markerIndex] = position);
            marker.name().markInterested();
            marker.name().addValueObserver(name -> cueMarkerNames[markerIndex] = name);
        }
    }

    private void initGlobalSettingsOverlay() {
        for (int index = 0; index < encoders.length; index++) {
            final int encoderIndex = index;
            encoders[index].bindEncoder(globalSettingsLayer, inc -> {
                final int steps = globalSettingsAccumulators[encoderIndex].consume(inc);
                if (steps != 0) {
                    adjustGlobalSettings(encoderIndex, steps);
                }
            });
            encoders[index].bindTouched(globalSettingsLayer,
                    touched -> handleGlobalSettingsTouch(encoderIndex, touched));
        }
        for (int padIndex = 0; padIndex < rgbButtons.length; padIndex++) {
            final int currentPad = padIndex;
            rgbButtons[padIndex].bindPressed(globalSettingsLayer, pressed -> {
                handleGlobalSettingsPad(currentPad, pressed);
            }, () -> globalSettingsPadState(currentPad));
        }
        getButton(NoteAssign.KNOB_MODE).bindPressed(globalSettingsLayer,
                this::handleGlobalSettingsModeAdvance,
                this::globalSettingsModeLightState);
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

    public boolean shouldShowMeterIdleDisplay() {
        return isTransportPlaying() || System.currentTimeMillis() < stoppedMeterRingOutUntilMs;
    }

    private void handleTransportPlayingChanged(final boolean playing) {
        stoppedMeterRingOutGeneration++;
        if (playing) {
            stoppedMeterRingOutUntilMs = 0;
            return;
        }
        stoppedMeterRingOutUntilMs = System.currentTimeMillis() + STOPPED_METER_RING_OUT_MS;
        final int generation = stoppedMeterRingOutGeneration;
        host.scheduleTask(() -> {
            if (generation == stoppedMeterRingOutGeneration
                    && !isTransportPlaying()
                    && System.currentTimeMillis() >= stoppedMeterRingOutUntilMs) {
                showIdleOledInfo();
            }
        }, STOPPED_METER_RING_OUT_MS);
    }

    public int getTransportTimeSignatureNumerator() {
        return transportTimeSignatureNumerator;
    }

    public int getTransportTimeSignatureDenominator() {
        return transportTimeSignatureDenominator;
    }

    private BiColorLightState getRecordState() {
        if (isGlobalAltHeld()) {
            return transport.isArrangerOverdubEnabled().get() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
        }
        if (modeState.activeMode() == Mode.DRUM) {
            return transport.isClipLauncherOverdubEnabled().get() ? BiColorLightState.RED_FULL : BiColorLightState.OFF;
        }
        if (isPerformRecordTargetingHeld()) {
            return BiColorLightState.RED_HALF;
        }
        return transport.isArrangerRecordEnabled().get() ? BiColorLightState.RED_FULL : BiColorLightState.OFF;
    }

    private BiColorLightState getPatternState() {
        if (getButton(NoteAssign.SHIFT).isPressed()) {
            return transport.isMetronomeEnabled().get() ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
        }
        if (isGlobalAltHeld()) {
            return transport.isClipLauncherOverdubEnabled().get() ? BiColorLightState.AMBER_HALF : BiColorLightState.AMBER_FULL;
        }
        return isAutomationWriteEnabled() ? BiColorLightState.AMBER_FULL : BiColorLightState.OFF;
    }

    private BiColorLightState getDrumState() {
        return modeState.activeMode() == Mode.DRUM ? activeDrumSubMode.getLightState() : BiColorLightState.OFF;
    }

    private BiColorLightState getNoteState() {
        if (modeState.activeMode() == Mode.NOTE_PLAY && notePlayMode != null) {
            return notePlayMode.getModeButtonLightState();
        }
        return BiColorLightState.OFF;
    }

    private BiColorLightState getStepState() {
        if (modeState.activeMode() == Mode.CHORD_STEP && chordStepMode != null) {
            return chordStepMode.getModeButtonLightState();
        }
        if (modeState.activeMode() == Mode.MELODIC_STEP && melodicStepMode != null) {
            return melodicStepMode.getModeButtonLightState();
        }
        if (modeState.activeMode() == Mode.FUGUE_STEP && fugueStepMode != null) {
            return fugueStepMode.getModeButtonLightState();
        }
        if (modeState.activeMode() == Mode.NESTED_RHYTHM && nestedRhythmMode != null) {
            return nestedRhythmMode.getModeButtonLightState();
        }
        return BiColorLightState.OFF;
    }

    private BiColorLightState getPerformState() {
        if (modeState.activeMode() != Mode.PERFORM) {
            return BiColorLightState.OFF;
        }
        if (performMode != null && performMode.isTrackActionMode()) {
            return ModeButtonLights.MODE_3;
        }
        if (performMode != null && performMode.isSceneActionMode()) {
            return ModeButtonLights.MODE_2;
        }
        return ModeButtonLights.MODE_1;
    }

    private void dummyAction(final boolean pressed) {
    }

    private void stopAction(final boolean pressed) {
        if (browserTransportAction(isPopupBrowserActive(), NoteAssign.STOP, pressed) == BrowserTransportAction.CANCEL) {
            popupBrowser.cancel();
            notifyAction("Browser", "Closed");
            return;
        }
        if (!pressed) {
            return;
        }
        switch (stopPressAction(transport.isPlaying().get())) {
            case STOP -> {
                retriggerLaunchersOnNextPlay = false;
                transport.stop();
            }
            case GO_ARRANGEMENT_START -> {
                goToArrangementStart();
                retriggerLaunchersOnNextPlay = true;
            }
        }
    }

    private void toggleRec(final boolean pressed) {
        if (!pressed && recordGestureConsumed) {
            recordGestureConsumed = false;
            return;
        }
        if (isGlobalShiftHeld()) {
            return;
        }
        if (isGlobalAltHeld()) {
            if (!pressed) {
                return;
            }
            toggleArrangerOverdubEnabled();
            return;
        }
        if (pressed && performMode != null && performMode.stopManualLauncherRecordingIfAny()) {
            if (patternPressed) {
                patternGestureConsumed = true;
            }
            recordGestureConsumed = true;
            performRecordPadGestureConsumed = true;
            return;
        }
        if (pressed && patternPressed && !patternPressShiftHeld && !patternPressAltHeld) {
            patternGestureConsumed = true;
            recordGestureConsumed = true;
            recordNextFreeLauncherSlot();
            return;
        }
        if (modeState.activeMode() == Mode.PERFORM) {
            if (pressed) {
                performRecordPadGestureConsumed = false;
                notifyAction("Clip Record", "Pad target");
                return;
            }
            if (performRecordPadGestureConsumed) {
                performRecordPadGestureConsumed = false;
                oled.clearScreenDelayed();
                return;
            }
            final boolean nextState = !transport.isArrangerRecordEnabled().get();
            transport.isArrangerRecordEnabled().toggle();
            notifyAction("Record", nextState ? "On" : "Off");
            return;
        }
        if (!pressed) {
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

    private void recordNextFreeLauncherSlot() {
        if (performMode == null) {
            notifyAction("Record Clip", "No launcher");
            return;
        }
        performMode.recordNextFreeLauncherSlot(false);
    }

    private void toggleAutomationWriteEnabled() {
        launcherOverdubAutoEnabledAutomationWrite = false;
        final boolean nextState = automationWriteNextState(
                transport.isArrangerAutomationWriteEnabled().get(),
                transport.isClipLauncherAutomationWriteEnabled().get());
        setAutomationWriteEnabled(nextState);
        notifyAction("Write Automation", nextState ? "On" : "Off");
    }

    private void setAutomationWriteEnabled(final boolean enabled) {
        transport.isArrangerAutomationWriteEnabled().set(enabled);
        transport.isClipLauncherAutomationWriteEnabled().set(enabled);
    }

    private void toggleArrangerOverdubEnabled() {
        final boolean nextState = overdubNextState(transport.isArrangerOverdubEnabled().get());
        transport.isArrangerOverdubEnabled().set(nextState);
        notifyAction("Arranger Overdub", nextState ? "On" : "Off");
    }

    private boolean isAutomationWriteEnabled() {
        return transport.isArrangerAutomationWriteEnabled().get()
                || transport.isClipLauncherAutomationWriteEnabled().get();
    }

    static boolean automationWriteNextState(final boolean arrangerWriteEnabled,
                                            final boolean clipLauncherWriteEnabled) {
        return !(arrangerWriteEnabled || clipLauncherWriteEnabled);
    }

    static boolean overdubNextState(final boolean overdubEnabled) {
        return !overdubEnabled;
    }

    static LauncherOverdubPressPlan launcherOverdubPressPlan(final boolean launcherOverdubEnabled,
                                                             final boolean automationWriteEnabled,
                                                             final boolean automationWriteAutoEnabled) {
        final boolean nextLauncherOverdubEnabled = overdubNextState(launcherOverdubEnabled);
        if (nextLauncherOverdubEnabled) {
            return new LauncherOverdubPressPlan(true, true, true, !automationWriteEnabled);
        }
        return new LauncherOverdubPressPlan(false,
                automationWriteAutoEnabled ? false : automationWriteEnabled,
                false,
                false);
    }

    private void toggleLauncherOverdubEnabled() {
        final LauncherOverdubPressPlan plan = launcherOverdubPressPlan(
                transport.isClipLauncherOverdubEnabled().get(),
                isAutomationWriteEnabled(),
                launcherOverdubAutoEnabledAutomationWrite);
        transport.isClipLauncherOverdubEnabled().set(plan.launcherOverdubEnabled());
        if (plan.touchAutomationWriteMode()) {
            transport.automationWriteMode().set(AUTOMATION_WRITE_MODE_TOUCH);
        }
        setAutomationWriteEnabled(plan.automationWriteEnabled());
        launcherOverdubAutoEnabledAutomationWrite = plan.automationWriteAutoEnabled();
        notifyAction("Launcher Overdub", plan.launcherOverdubEnabled() ? "On" : "Off");
    }

    private void handlePatternPressed(final boolean pressed) {
        patternPressed = pressed;
        if (pressed) {
            patternGestureConsumed = false;
            patternPressShiftHeld = getButton(NoteAssign.SHIFT).isPressed();
            patternPressAltHeld = isGlobalAltHeld();
            return;
        }
        if (patternGestureConsumed) {
            patternGestureConsumed = false;
            return;
        }
        switch (patternReleaseAction(false, patternPressShiftHeld, patternPressAltHeld)) {
            case TOGGLE_METRONOME -> {
                final boolean nextState = !transport.isMetronomeEnabled().get();
                transport.isMetronomeEnabled().toggle();
                notifyAction("Metronome", nextState ? "On" : "Off");
            }
            case TOGGLE_LAUNCHER_OVERDUB -> toggleLauncherOverdubEnabled();
            case TOGGLE_AUTOMATION_WRITE -> toggleAutomationWriteEnabled();
            case NONE -> {
            }
        }
    }

    public void notifyAction(final String title, final String value) {
        if (!showModeAwareActionInfo(title, value)) {
            oled.valueInfo(title, value);
            suppressTransientOledOverlays();
        }
        if (screenNotificationsPref != null && screenNotificationsPref.get()) {
            host.showPopupNotification(title + ": " + value);
        }
    }

    private boolean showModeAwareActionInfo(final String title, final String value) {
        if (modeState.activeMode() == Mode.PERFORM && performMode != null
                && performMode.showGlobalActionInfo(title, value)) {
            return true;
        }
        return modeState.activeMode() == Mode.DRUM
                && activeDrumSubMode == DrumSubMode.STANDARD
                && drumSequenceMode != null
                && drumSequenceMode.showGlobalActionInfo(title, value);
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
        if (dismissGlobalSettingsOverlayForModeButton(Mode.DRUM)) {
            return;
        }
        leavePerformBirdsEyeIfActive();
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
            switchActiveMode();
            notifyAction("Mode", activeDrumSubMode.displayName());
        } else {
            modeState.activateDrum();
            switchActiveMode();
            notifyAction("Mode", activeDrumSubMode.displayName());
        }
    }

    private void handleNotePressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (dismissGlobalSettingsOverlayForModeButton(Mode.NOTE_PLAY)) {
            return;
        }
        leavePerformBirdsEyeIfActive();
        if (isGlobalShiftHeld()) {
            toggleRecordQuantization();
            return;
        }
        if (modeState.activeMode() == Mode.NOTE_PLAY && isGlobalAltHeld()) {
            notePlayMode.toggleLiveLayoutShortcut();
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

    private void handleRecordQuantizationChanged(final String value) {
        if (value != null && !RECORD_QUANTIZATION_OFF.equals(value)) {
            lastRecordQuantizationGrid = value;
        }
    }

    private void toggleRecordQuantization() {
        final SettableEnumValue recordQuantization = application.recordQuantizationGrid();
        final String current = recordQuantization.get();
        if (RECORD_QUANTIZATION_OFF.equals(current)) {
            final String next = normalizeRecordQuantizationGrid(lastRecordQuantizationGrid);
            recordQuantization.set(next);
            notifyAction("Record Quant", next);
        } else {
            handleRecordQuantizationChanged(current);
            recordQuantization.set(RECORD_QUANTIZATION_OFF);
            notifyAction("Record Quant", "Off");
        }
    }

    private String normalizeRecordQuantizationGrid(final String value) {
        return value == null || value.isBlank() || RECORD_QUANTIZATION_OFF.equals(value)
                ? RECORD_QUANTIZATION_DEFAULT_ON
                : value;
    }

    private void handleStepPressed(final boolean pressed) {
        if (pressed && dismissGlobalSettingsOverlayForStepButton()) {
            return;
        }
        if (pressed) {
            leavePerformBirdsEyeIfActive();
        }
        if (modeState.activeMode() == Mode.MELODIC_STEP) {
            if (!pressed && suppressNextMelodicStepRelease) {
                suppressNextMelodicStepRelease = false;
                return;
            }
            if (!isGlobalShiftHeld() && !isGlobalAltHeld() && melodicStepMode.hasHeldSteps()) {
                melodicStepMode.handleStepButton(pressed);
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
            if (!isGlobalShiftHeld() && !isGlobalAltHeld() && chordStepMode.hasHeldSteps()) {
                chordStepMode.handleStepButton(pressed);
                return;
            }
            if (!pressed || isGlobalAltHeld()) {
                return;
            }
            modeState.activateFugueStep();
            switchActiveMode();
            notifyAction("Mode", "Fugue");
            return;
        }
        if (modeState.activeMode() == Mode.FUGUE_STEP) {
            if (!pressed || isGlobalAltHeld()) {
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
        if (dismissGlobalSettingsOverlayForModeButton(Mode.PERFORM)) {
            return;
        }
        final boolean enteringPerform = modeState.activeMode() != Mode.PERFORM;
        if (enteringPerform) {
            modeState.activatePerform();
            switchActiveMode();
        }
        if (isGlobalShiftHeld() && isGlobalAltHeld()) {
            performMode.toggleBirdsEyeMode();
            notifyAction("Mode", performMode.activePageLabel());
            return;
        }
        if (isGlobalShiftHeld()) {
            performMode.toggleTrackActionMode();
            notifyAction("Mode", performMode.activePageLabel());
            return;
        }
        if (isGlobalAltHeld()) {
            performMode.toggleOrientation();
            notifyAction("Mode", performMode.activePageLabel());
            return;
        }
        if (!enteringPerform) {
            if (performMode.isBirdsEyeMode()) {
                performMode.leaveBirdsEyeMode();
                notifyAction("Mode", performMode.activePageLabel());
                return;
            }
            if (performMode.isTrackActionMode()) {
                performMode.toggleTrackActionMode();
                notifyAction("Mode", performMode.activePageLabel());
                return;
            }
            performMode.toggleSceneActionMode();
        }
        notifyAction("Mode", performMode.activePageLabel());
    }

    private boolean leavePerformBirdsEyeIfActive() {
        if (modeState.activeMode() != Mode.PERFORM || performMode == null || !performMode.isBirdsEyeMode()) {
            return false;
        }
        performMode.leaveBirdsEyeMode();
        return true;
    }

    private void togglePlay(final boolean pressed) {
        if (browserTransportAction(isPopupBrowserActive(), NoteAssign.PLAY, pressed) == BrowserTransportAction.COMMIT) {
            popupBrowser.commit();
            notifyAction("Browser", "Commit");
            return;
        }
        if (!pressed) {
            return;
        }
        // Alt+Play: retrigger the current clip without transport state change.
        if (isGlobalAltHeld()) {
            final boolean wasPlaying = transport.isPlaying().get();
            retriggerCurrentClip();
            if (wasPlaying) {
                notifyAction("Clip", "Retrigger");
                oled.clearScreenDelayed();
            }
            return;
        }
        // Regular behavior: toggle play/stop. Launcher retriggering is explicit: Alt+Play for
        // the selected clip, or second Stop then Play for Bitwig's global launcher retrigger.
        switch (playPressAction(transport.isPlaying().get(), retriggerLaunchersOnNextPlay)) {
            case STOP -> {
                retriggerLaunchersOnNextPlay = false;
                transport.isPlaying().set(false);
            }
            case RETRIGGER_LAUNCHERS_FROM_START -> {
                retriggerLaunchersOnNextPlay = false;
                if (!retriggerPlayingLauncherClips()) {
                    retriggerCurrentClip();
                }
                transport.launchFromPlayStartPosition();
            }
            case LAUNCH_FROM_PLAY_START -> {
                transport.launchFromPlayStartPosition();
            }
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
        return sharedPitchContext.getRootNote();
    }

    public void setSharedRootNote(final int rootNote) {
        sharedPitchContext.setRootNote(rootNote);
    }

    public void adjustSharedRootNote(final int amount) {
        sharedPitchContext.adjustRootNote(amount);
    }

    public int getSharedScaleIndex() {
        return sharedPitchContext.getScaleIndex();
    }

    public void setSharedScaleIndex(final int scaleIndex) {
        sharedPitchContext.setScaleIndex(scaleIndex);
    }

    public boolean adjustSharedScaleIndex(final int amount, final int minimumScaleIndex) {
        return sharedPitchContext.adjustScaleIndex(amount, minimumScaleIndex);
    }

    public int getSharedOctave() {
        return sharedPitchContext.getOctave();
    }

    public void setSharedOctave(final int octave) {
        sharedPitchContext.setOctave(octave);
    }

    public void adjustSharedOctave(final int amount) {
        sharedPitchContext.adjustOctave(amount);
    }

    public int getSharedBaseMidiNote() {
        return sharedPitchContext.getBaseMidiNote();
    }

    public String getSharedScaleDisplayName() {
        return sharedPitchContext.getScaleDisplayName();
    }

    public String getSharedShortScaleDisplayName() {
        return sharedPitchContext.getShortScaleDisplayName();
    }

    public MusicalScale getSharedMusicalScale() {
        return sharedPitchContext.getMusicalScale();
    }

    public SharedPitchContextController getSharedPitchContextController() {
        return sharedPitchContext;
    }

    public SharedMusicalContext getSharedMusicalContext() {
        return sharedPitchContext.context();
    }

    public VelocitySettings getSharedVelocitySettings() {
        return sharedVelocitySettings;
    }

    private void initializeSharedPitchContext() {
        sharedPitchContext.initializeFromPreferences(
                getDefaultScalePreference(),
                getDefaultRootKeyPreference(),
                getDefaultNoteInputOctavePreference());
    }

    private void initializeSharedVelocitySettings() {
        sharedVelocitySettings.setSensitivity(getDefaultVelocitySensitivityPreference());
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

    public boolean isExclusiveTrackArmEnabled() {
        return exclusiveTrackArmEnabled;
    }

    public long getScreenMessageHoldMs() {
        return screenMessageHoldMs;
    }

    private void applyScreenMessageHoldPreference(final String preferenceValue) {
        screenMessageHoldMs = FireControlPreferences.toScreenMessageHoldMillis(preferenceValue);
        oled.setClearDelayMs(screenMessageHoldMs);
    }

    private void applyStartupModePreference() {
        final String startupMode = FireControlPreferences.normalizeStartupMode(startupModePref == null
                ? FireControlPreferences.STARTUP_MODE_NOTE
                : startupModePref.get());
        if (performMode != null) {
            performMode.setTrackActionMode(FireControlPreferences.STARTUP_MODE_MIX.equals(startupMode));
        }
        switch (startupMode) {
            case FireControlPreferences.STARTUP_MODE_HARMONY -> modeState.activateChordStep();
            case FireControlPreferences.STARTUP_MODE_DRUM_XOX -> {
                activeDrumSubMode = DrumSubMode.STANDARD;
                modeState.activateDrum();
            }
            case FireControlPreferences.STARTUP_MODE_LAUNCHER, FireControlPreferences.STARTUP_MODE_MIX ->
                    modeState.activatePerform();
            default -> modeState.activateNotePlay();
        }
    }

    private void switchActiveMode() {
        releaseAutoPinnedDrumContext(true);
        drumSequenceMode.deactivate();
        notePlayMode.deactivate();
        drumPadPlayMode.deactivate();
        chordStepMode.deactivate();
        melodicStepMode.deactivate();
        fugueStepMode.deactivate();
        nestedRhythmMode.deactivate();
        performMode.deactivate();
        if (modeState.activeMode() == Mode.DRUM) {
            applyDrumPinningIfEnabled();
            if (activeDrumSubMode == DrumSubMode.NESTED_RHYTHM) {
                nestedRhythmMode.activate();
            } else if (activeDrumSubMode == DrumSubMode.DRUM_PADS) {
                drumPadPlayMode.activate();
            } else {
                drumSequenceMode.activate();
            }
            refreshSurfaceLights();
            return;
        }
        if (modeState.activeMode() == Mode.NOTE_PLAY) {
            notePlayMode.activate();
        } else if (modeState.activeMode() == Mode.CHORD_STEP) {
            chordStepMode.activate();
        } else if (modeState.activeMode() == Mode.MELODIC_STEP) {
            melodicStepMode.activate();
        } else if (modeState.activeMode() == Mode.FUGUE_STEP) {
            fugueStepMode.activate();
        } else if (modeState.activeMode() == Mode.NESTED_RHYTHM) {
            nestedRhythmMode.activate();
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

    public String getPerformClipLauncherLayoutPreference() {
        return FireControlPreferences.normalizePerformLayout(performClipLauncherLayoutPref == null
                ? FireControlPreferences.PERFORM_LAYOUT_VERTICAL
                : performClipLauncherLayoutPref.get());
    }

    public int getDefaultClipLengthBeats() {
        return (int) Math.round(defaultClipLengthPref == null
                ? FireControlPreferences.toClipLengthBeats(FireControlPreferences.CLIP_LENGTH_2_BARS)
                : FireControlPreferences.toClipLengthBeats(defaultClipLengthPref.get()));
    }

    public boolean shouldRoundLauncherRecordingToNearestBar() {
        return launcherRecordLengthPref != null
                && FireControlPreferences.isRoundLauncherRecordLength(launcherRecordLengthPref.get());
    }

    public boolean shouldDisableLauncherPostRecordingAction() {
        return launcherRecordLengthPref != null
                && FireControlPreferences.isManualLauncherRecordLength(launcherRecordLengthPref.get());
    }

    public boolean isPerformRecordTargetingHeld() {
        final BiColorButton recButton = getButton(NoteAssign.REC);
        return modeState.activeMode() == Mode.PERFORM
                && recButton != null
                && recButton.isPressed()
                && !patternPressed
                && !isGlobalShiftHeld()
                && !isGlobalAltHeld();
    }

    public void consumePerformRecordPadGesture() {
        performRecordPadGestureConsumed = true;
    }

    public void prepareLauncherRecording() {
        if (shouldDisableLauncherPostRecordingAction() || shouldRoundLauncherRecordingToNearestBar()) {
            transport.clipLauncherPostRecordingAction().set("off");
            return;
        }
        transport.clipLauncherPostRecordingAction().set("play_recorded");
        transport.getClipLauncherPostRecordingTimeOffset().set(getLauncherRecordLengthBeats());
    }

    private int getLauncherRecordLengthBeats() {
        return (int) Math.round(launcherRecordLengthPref == null
                ? FireControlPreferences.toLauncherRecordLengthBeats(FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_2_BARS)
                : FireControlPreferences.toLauncherRecordLengthBeats(launcherRecordLengthPref.get()));
    }

    public String getMainEncoderRolePreference() {
        return resolveMainEncoderRoleForActiveMode(currentMainEncoderRole);
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
        final String nextRole = FireControlPreferences.nextAlternateMainEncoderRole(cycleSource, isDrumGridRoleAvailable());
        currentMainEncoderRole = nextRole;
        alternateMainEncoderRole = nextRole;
        final String effectiveRole = getMainEncoderRolePreference();
        notifyAction("Encoder Role", mainEncoderRoleDisplayName(effectiveRole));
        return effectiveRole;
    }

    public String toggleMainEncoderRolePreference() {
        final String normalizedCurrentRole = getMainEncoderRolePreference();
        final String normalizedAlternateRole = resolveMainEncoderRoleForActiveMode(alternateMainEncoderRole);
        final String nextRole = FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED.equals(normalizedCurrentRole)
                ? (FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED.equals(normalizedAlternateRole)
                ? FireControlPreferences.MAIN_ENCODER_TRACK_SELECT
                : normalizedAlternateRole)
                : FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED;
        currentMainEncoderRole = nextRole;
        final String effectiveRole = getMainEncoderRolePreference();
        notifyAction("Encoder Role", mainEncoderRoleDisplayName(effectiveRole));
        return effectiveRole;
    }

    private boolean isDrumGridRoleAvailable() {
        return modeState.activeMode() == Mode.DRUM && activeDrumSubMode == DrumSubMode.STANDARD;
    }

    private String resolveMainEncoderRoleForActiveMode(final String role) {
        final String normalizedRole = FireControlPreferences.normalizeMainEncoderRole(role);
        if (FireControlPreferences.MAIN_ENCODER_TRACK_SELECT.equals(normalizedRole)
                && isDrumGridRoleAvailable()
                && shouldAutoPinFirstDrumMachine()) {
            return FireControlPreferences.MAIN_ENCODER_DRUM_GRID;
        }
        if (FireControlPreferences.MAIN_ENCODER_DRUM_GRID.equals(normalizedRole) && !isDrumGridRoleAvailable()) {
            return FireControlPreferences.MAIN_ENCODER_TRACK_SELECT;
        }
        return normalizedRole;
    }

    private String mainEncoderRoleDisplayName(final String role) {
        if (FireControlPreferences.MAIN_ENCODER_DRUM_GRID.equals(role)) {
            return "Grid Resolution";
        }
        return role;
    }

    public void goToArrangementStartOrLoopStart() {
        if (transport == null) {
            return;
        }
        if (hasActiveArrangerLoop()) {
            final double loopStart = Math.max(0.0, transport.arrangerLoopStart().get());
            setPlaybackStartPosition(loopStart);
            oled.valueInfo("Loop Start", transport.playStartPosition().getFormatted());
            return;
        }
        setPlaybackStartPosition(0.0);
        oled.valueInfo("Project Start", transport.playStartPosition().getFormatted());
    }

    private void goToArrangementStart() {
        if (transport == null) {
            return;
        }
        setPlaybackStartPosition(0.0);
        oled.valueInfo("Project Start", transport.playStartPosition().getFormatted());
    }

    private boolean retriggerPlayingLauncherClips() {
        if (!LauncherRetriggerActions.retriggerPlayingLauncherClips(application)) {
            notifyAction("Launcher Retrigger", "Unavailable");
            return false;
        }
        notifyAction("Launcher", "Retrigger");
        return true;
    }

    private void retriggerCurrentClip() {
        LauncherRetriggerActions.retriggerCurrentClip(viewControl == null ? null : viewControl.getSelectedClip());
    }

    public void goToArrangementEndOrLoopEnd() {
        if (transport == null) {
            return;
        }
        if (hasActiveArrangerLoop()) {
            final double loopEnd = Math.max(0.0,
                    transport.arrangerLoopStart().get() + transport.arrangerLoopDuration().get());
            setPlaybackStartPosition(loopEnd);
            oled.valueInfo("Loop End", transport.playStartPosition().getFormatted());
            return;
        }
        if (arranger != null) {
            arranger.zoomToFit();
        }
        final Action jumpToEnd = application == null ? null : application.getAction(ACTION_JUMP_TO_END_OF_ARRANGEMENT);
        if (jumpToEnd != null) {
            jumpToEnd.invoke();
            oled.valueInfo("Project End", "Last clip");
        } else {
            oled.valueInfo("Project End", "Unavailable");
        }
    }

    public void adjustPlaybackStartPositionByGrid(final int inc) {
        if (inc == 0 || transport == null) {
            return;
        }
        final double grid = currentArrangementGridResolution();
        final double current = Math.max(0.0, transport.playStartPosition().get());
        final double snapped = Math.round(current / grid) * grid;
        setPlaybackStartPosition(Math.max(0.0, snapped + inc * grid));
        oled.valueInfo("Play Start", transport.playStartPosition().getFormatted());
    }

    public void adjustPlaybackStartPositionFine(final int inc) {
        if (inc == 0 || transport == null) {
            return;
        }
        final double current = Math.max(0.0, transport.playStartPosition().get());
        setPlaybackStartPosition(Math.max(0.0, current + inc * 0.25));
        oled.valueInfo("Play Start Fine", transport.playStartPosition().getFormatted());
    }

    private boolean hasActiveArrangerLoop() {
        return transport.isArrangerLoopEnabled().get() && transport.arrangerLoopDuration().get() > 0.0;
    }

    private void setPlaybackStartPosition(final double beats) {
        final double target = Math.max(0.0, beats);
        cueMarkerNavigationPosition = Double.NaN;
        transport.playStartPosition().set(target);
        transport.getPosition().set(target);
        if (transport.isPlaying().get()) {
            transport.jumpToPlayStartPosition();
        }
    }

    private double currentArrangementGridResolution() {
        if (arranger == null || !arranger.getHorizontalScrollbarModel().isZoomable()) {
            return ArrangerGridStep.fallbackBeatStep(transportTimeSignatureDenominator);
        }
        final double contentPerPixel = arranger.getHorizontalScrollbarModel().getContentPerPixel().get();
        return ArrangerGridStep.fromContentPerPixel(
                contentPerPixel,
                transportTimeSignatureNumerator,
                transportTimeSignatureDenominator);
    }

    public boolean isStepSeqPadAuditionEnabled() {
        return stepSeqPadAuditionPref != null && stepSeqPadAuditionPref.get();
    }

    public String getDefaultScalePreference() {
        return defaultScalePref == null
                ? FireControlPreferences.DEFAULT_SCALE_MAJOR
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
            case DRUM -> activeDrumSubMode.displayName();
            case CHORD_STEP -> "Chord Step";
            case PERFORM -> "Perform";
            default -> "Note";
        });
    }

    public void enterMelodicStepMode() {
        suppressNextMelodicStepRelease = true;
        modeState.enterMelodicStepMode();
        switchActiveMode();
        notifyAction("Mode", "Melo Step");
    }

    public void enterFugueStepMode() {
        modeState.activateFugueStep();
        switchActiveMode();
        notifyAction("Mode", "Fugue");
    }

    public void enterNestedRhythmMode() {
        suppressNextMelodicStepRelease = true;
        activeDrumSubMode = DrumSubMode.NESTED_RHYTHM;
        modeState.activateDrum();
        switchActiveMode();
        notifyAction("Mode", "NestedRytm");
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

    private boolean shouldAutoPinStandardDrumMode() {
        return modeState.activeMode() == Mode.DRUM
                && activeDrumSubMode == DrumSubMode.STANDARD
                && shouldAutoPinFirstDrumMachine();
    }

    private void syncDrumPinningForActiveMode() {
        if (modeState.activeMode() == Mode.DRUM) {
            if (shouldAutoPinStandardDrumMode()) {
                applyDrumPinningIfEnabled();
            } else {
                releaseAutoPinnedDrumContext(true);
            }
            return;
        }
        releaseAutoPinnedDrumContext(true);
    }

    private void applyDrumPinningIfEnabled() {
        if (!shouldAutoPinStandardDrumMode() || drumAutoPinApplied || deviceLocator == null || viewControl == null) {
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
        if (!shouldAutoPinStandardDrumMode() || !drumAutoPinApplied
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
        final double stepSize = mainCursorParameterStep(fine);
        final double nextValue = Math.max(0.0, Math.min(1.0, value.get() + (inc * stepSize)));
        value.setImmediately(nextValue);
        oled.valueInfo(parameter.name().get(), parameter.displayedValue().get());
    }

    static double mainCursorParameterStep(final boolean fine) {
        return fine ? LAST_TOUCHED_ENCODER_FINE_STEP : LAST_TOUCHED_ENCODER_STEP;
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
        if (!isTransportPlaying() && !shouldShowMeterIdleDisplay()) {
            showIdleTrackInfo();
            return;
        }
        if (modeState.activeMode() == Mode.PERFORM && performMode != null && performMode.showIdleInfoIfNeeded()) {
            return;
        }
        if (modeState.activeMode() == Mode.NOTE_PLAY && notePlayMode != null && notePlayMode.showIdleInfoIfNeeded()) {
            return;
        }
        if (modeState.activeMode() == Mode.DRUM
                && activeDrumSubMode == DrumSubMode.STANDARD
                && drumSequenceMode != null
                && drumSequenceMode.showIdleInfoIfNeeded()) {
            return;
        }
        final Parameter parameter = getLastClickedParameter();
        if (parameter != null) {
            oled.valueInfo(parameter.name().get(), parameter.displayedValue().get());
            return;
        }
        final String modeLabel = switch (modeState.activeMode()) {
            case DRUM -> activeDrumSubMode.displayName();
            case NOTE_PLAY -> "Note";
            case CHORD_STEP -> "Chord Step";
            case MELODIC_STEP -> "Melo Step";
            case FUGUE_STEP -> "Fugue";
            case NESTED_RHYTHM -> "NestedRytm";
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

    public boolean toggleCurrentDeviceWindow() {
        final PinnableCursorDevice selectedDevice = viewControl.getSelectedDevice();
        final PinnableCursorDevice primaryDevice = viewControl.getPrimaryDevice();
        final PinnableCursorDevice targetDevice = selectedDevice != null && selectedDevice.exists().get()
                ? selectedDevice
                : primaryDevice;
        if (targetDevice == null || !targetDevice.exists().get()) {
            oled.valueInfo("Device Window", "No Device");
            return false;
        }
        final boolean wasOpen = targetDevice.isWindowOpen().get();
        targetDevice.isWindowOpen().toggle();
        oled.valueInfo("Device Window", wasOpen ? "Closed" : "Open");
        return true;
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

    private void activateGlobalSettingsOverlay() {
        if (globalSettingsOverlayActive) {
            showGlobalSettingsOverview();
            return;
        }
        globalSettingsOverlayActive = true;
        releaseAutoPinnedDrumContext(true);
        drumSequenceMode.deactivate();
        notePlayMode.deactivate();
        drumPadPlayMode.deactivate();
        chordStepMode.deactivate();
        melodicStepMode.deactivate();
        fugueStepMode.deactivate();
        nestedRhythmMode.deactivate();
        performMode.deactivate();
        globalSettingsLayer.activate();
        showGlobalSettingsOverview();
        refreshSurfaceLights();
    }

    private void deactivateGlobalSettingsOverlay() {
        if (!globalSettingsOverlayActive) {
            return;
        }
        globalSettingsOverlayActive = false;
        globalSettingsLayer.deactivate();
        switchActiveMode();
        oled.clearScreenDelayed();
        refreshSurfaceLights();
    }

    private boolean dismissGlobalSettingsOverlayForModeButton(final Mode targetMode) {
        if (!shouldDismissGlobalSettingsOverlayForPlainModeButton()) {
            return false;
        }
        globalSettingsOverlayLatch.close();
        final boolean alreadyInTargetMode = modeState.activeMode() == targetMode;
        deactivateGlobalSettingsOverlay();
        return alreadyInTargetMode;
    }

    private boolean dismissGlobalSettingsOverlayForStepButton() {
        if (!shouldDismissGlobalSettingsOverlayForPlainModeButton()) {
            return false;
        }
        globalSettingsOverlayLatch.close();
        final boolean alreadyInStepFamily = switch (modeState.activeMode()) {
            case CHORD_STEP, MELODIC_STEP, FUGUE_STEP, NESTED_RHYTHM -> true;
            default -> false;
        };
        deactivateGlobalSettingsOverlay();
        return alreadyInStepFamily;
    }

    private boolean shouldDismissGlobalSettingsOverlayForPlainModeButton() {
        return globalSettingsOverlayActive && !isGlobalShiftHeld() && !isGlobalAltHeld();
    }

    private void updateGlobalSettingsOverlayState() {
        final BiColorButton browserButton = getButton(NoteAssign.BROWSER);
        final boolean momentaryComboHeld = browserButton != null
                && browserButton.isPressed()
                && isGlobalShiftHeld()
                && !isGlobalAltHeld()
                && !isPopupBrowserActive();
        final boolean shouldShow = globalSettingsOverlayLatch.shouldBeActive(
                momentaryComboHeld,
                isPopupBrowserActive());
        if (shouldShow) {
            activateGlobalSettingsOverlay();
        } else {
            deactivateGlobalSettingsOverlay();
        }
    }

    private void showGlobalSettingsOverview() {
        if (globalSettingsEncoderMode == EncoderMode.USER_1) {
            oled.detailInfo("Global Settings",
                    "Page: %s\n1: Pin Track %s\n2: Pin Device %s\n3: Pin Clip %s\n4: --".formatted(
                            globalSettingsPageLabel(),
                            pinStateLabel(viewControl.getCursorTrack().isPinned().get()),
                            pinOverviewLabel(viewControl.getSelectedDevice().isPinned().get(),
                                    viewControl.getSelectedDevice().exists().get()),
                            pinOverviewLabel(viewControl.getSelectedClip().isPinned().get(),
                                    viewControl.getSelectedClip().exists().get())));
            return;
        }
        if (globalSettingsEncoderMode == EncoderMode.USER_2) {
            oled.detailInfo("Global Settings",
                    "Page: %s\n1: Create %s\n2: Record %s\n3: --\n4: --".formatted(
                            globalSettingsPageLabel(),
                            defaultClipLengthLabel(),
                            launcherRecordLengthLabel()));
            return;
        }
        if (globalSettingsEncoderMode == EncoderMode.MIXER) {
            oled.detailInfo("Global Settings",
                    "Page: %s\n1: Vel Sens %d%%\n2: Vel Ctr %d\n3: Pad Bright %s\n4: Pad Sat %s".formatted(
                            globalSettingsPageLabel(),
                            sharedVelocitySettings.sensitivity(),
                            sharedVelocitySettings.centerVelocity(),
                            padBrightnessLabel(),
                            padSaturationLabel()));
            return;
        }
        oled.detailInfo("Global Settings",
                "Page: %s\n1: Root %s\n2: Scale %s\n3: Oct %d\n4: --\nTracks: %s".formatted(
                        globalSettingsPageLabel(),
                        com.oikoaudio.fire.note.NoteGridLayout.noteName(sharedPitchContext.getRootNote()),
                        sharedPitchContext.getScaleDisplayName(),
                        sharedPitchContext.getOctave(),
                        showDeactivatedTracks() ? "All" : "Active"));
    }

    private void adjustGlobalSettings(final int encoderIndex, final int inc) {
        if (inc == 0) {
            return;
        }
        if (globalSettingsEncoderMode == EncoderMode.MIXER) {
            adjustGlobalInputSettings(encoderIndex, inc);
            return;
        }
        if (globalSettingsEncoderMode == EncoderMode.USER_1) {
            adjustGlobalPinSettings(encoderIndex, inc);
            return;
        }
        if (globalSettingsEncoderMode == EncoderMode.USER_2) {
            adjustGlobalClipSettings(encoderIndex, inc);
            return;
        }
        if (encoderIndex == 0) {
            final int nextRoot = Math.max(0, Math.min(11, sharedPitchContext.getRootNote() + inc));
            sharedPitchContext.setRootNote(nextRoot);
            oled.valueInfo("Root", com.oikoaudio.fire.note.NoteGridLayout.noteName(sharedPitchContext.getRootNote()));
            return;
        }
        if (encoderIndex == 1) {
            sharedPitchContext.adjustScaleIndex(inc, -1);
            oled.valueInfo("Scale", sharedPitchContext.getScaleDisplayName());
            return;
        }
        if (encoderIndex == 2) {
            sharedPitchContext.adjustOctave(inc);
            oled.valueInfo("Octave", Integer.toString(sharedPitchContext.getOctave()));
            return;
        }
        showGlobalSettingsOverview();
    }

    private void handleGlobalSettingsTouch(final int encoderIndex, final boolean touched) {
        if (!touched) {
            globalSettingsAccumulators[encoderIndex].reset();
            showGlobalSettingsOverview();
            return;
        }
        if (handleGlobalSettingsResetTouch(encoderIndex)) {
            return;
        }
        if (globalSettingsEncoderMode == EncoderMode.MIXER) {
            showGlobalInputSetting(encoderIndex);
            return;
        }
        if (globalSettingsEncoderMode == EncoderMode.USER_1) {
            showGlobalPinSetting(encoderIndex);
            return;
        }
        if (globalSettingsEncoderMode == EncoderMode.USER_2) {
            showGlobalClipSetting(encoderIndex);
            return;
        }
        if (encoderIndex == 0) {
            oled.valueInfo("Root", com.oikoaudio.fire.note.NoteGridLayout.noteName(sharedPitchContext.getRootNote()));
            return;
        }
        if (encoderIndex == 1) {
            oled.valueInfo("Scale", sharedPitchContext.getScaleDisplayName());
            return;
        }
        if (encoderIndex == 2) {
            oled.valueInfo("Octave", Integer.toString(sharedPitchContext.getOctave()));
            return;
        }
        showGlobalSettingsOverview();
    }

    private boolean handleGlobalSettingsResetTouch(final int encoderIndex) {
        return switch (globalSettingsEncoderMode) {
            case MIXER -> handleGlobalInputResetTouch(encoderIndex);
            case USER_1 -> handleGlobalPinResetTouch(encoderIndex);
            case USER_2 -> handleGlobalClipResetTouch(encoderIndex);
            default -> handleGlobalPitchResetTouch(encoderIndex);
        };
    }

    private boolean handleGlobalPitchResetTouch(final int encoderIndex) {
        return switch (encoderIndex) {
            case 0 -> handleKnobModeEncoderReset(true, true, "Root", "No reset",
                    () -> sharedPitchContext.setRootNote(getDefaultRootKeyPreference()),
                    () -> oled.valueInfo("Root", com.oikoaudio.fire.note.NoteGridLayout.noteName(sharedPitchContext.getRootNote())));
            case 1 -> handleKnobModeEncoderReset(true, true, "Scale", "No reset",
                    () -> sharedPitchContext.setScaleIndex(defaultScaleIndex()),
                    () -> oled.valueInfo("Scale", sharedPitchContext.getScaleDisplayName()));
            case 2 -> handleKnobModeEncoderReset(true, true, "Octave", "No reset",
                    () -> sharedPitchContext.setOctave(getDefaultNoteInputOctavePreference()),
                    () -> oled.valueInfo("Octave", Integer.toString(sharedPitchContext.getOctave())));
            default -> false;
        };
    }

    private int defaultScaleIndex() {
        return sharedPitchContext.resolveDefaultScaleIndex(getDefaultScalePreference());
    }

    private boolean handleGlobalInputResetTouch(final int encoderIndex) {
        return switch (encoderIndex) {
            case 0 -> handleKnobModeEncoderReset(true, true, "Velocity Sens", "No reset",
                    () -> sharedVelocitySettings.setSensitivity(getDefaultVelocitySensitivityPreference()),
                    () -> oled.valueInfo("Velocity Sens", sharedVelocitySettings.sensitivity() + "%"));
            case 1 -> handleKnobModeEncoderReset(true, true, "Velocity Ctr", "No reset",
                    () -> sharedVelocitySettings.setCenterVelocity(GLOBAL_VELOCITY_CENTER_DEFAULT),
                    () -> oled.valueInfo("Velocity Ctr", Integer.toString(sharedVelocitySettings.centerVelocity())));
            case 2 -> handleKnobModeEncoderReset(true, padBrightnessPref != null, "Pad Bright", "No reset",
                    () -> {
                        padBrightnessPref.setRaw(FireControlPreferences.PAD_BRIGHTNESS_DEFAULT);
                        padBrightness = FireControlPreferences.PAD_BRIGHTNESS_DEFAULT;
                    },
                    () -> oled.valueInfo("Pad Bright", padBrightnessLabel()));
            case 3 -> handleKnobModeEncoderReset(true, padSaturationPref != null, "Pad Sat", "No reset",
                    () -> {
                        padSaturationPref.setRaw(FireControlPreferences.PAD_SATURATION_DEFAULT);
                        padSaturation = FireControlPreferences.PAD_SATURATION_DEFAULT;
                    },
                    () -> oled.valueInfo("Pad Sat", padSaturationLabel()));
            default -> false;
        };
    }

    private boolean handleGlobalClipResetTouch(final int encoderIndex) {
        return switch (encoderIndex) {
            case 0 -> handleKnobModeEncoderReset(true, defaultClipLengthPref != null, "Create Len", "No reset",
                    () -> defaultClipLengthPref.set(FireControlPreferences.CLIP_LENGTH_2_BARS),
                    () -> oled.valueInfo("Create Len", defaultClipLengthLabel()));
            case 1 -> handleKnobModeEncoderReset(true, launcherRecordLengthPref != null, "Record Len", "No reset",
                    () -> launcherRecordLengthPref.set(FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_2_BARS),
                    () -> oled.valueInfo("Record Len", launcherRecordLengthLabel()));
            default -> handleKnobModeEncoderReset(true, false, "Clip", "No reset", () -> { },
                    this::showGlobalSettingsOverview);
        };
    }

    private boolean handleGlobalPinResetTouch(final int encoderIndex) {
        return switch (encoderIndex) {
            case 0 -> handleKnobModeEncoderReset(true, true, "Pin Track", "No reset",
                    () -> viewControl.getCursorTrack().isPinned().set(false),
                    () -> oled.valueInfo("Pin Track", pinStateLabel(viewControl.getCursorTrack().isPinned().get())));
            case 1 -> handleKnobModeEncoderReset(true, viewControl.getSelectedDevice().exists().get(),
                    "Pin Device", "No reset",
                    () -> viewControl.getSelectedDevice().isPinned().set(false),
                    () -> showPinInfo("Pin Device", viewControl.getSelectedDevice().isPinned().get(),
                            viewControl.getSelectedDevice().exists().get()));
            case 2 -> handleKnobModeEncoderReset(true, viewControl.getSelectedClip().exists().get(),
                    "Pin Clip", "No reset",
                    () -> viewControl.getSelectedClip().isPinned().set(false),
                    () -> showPinInfo("Pin Clip", viewControl.getSelectedClip().isPinned().get(),
                            viewControl.getSelectedClip().exists().get()));
            default -> handleKnobModeEncoderReset(true, false, "Pins", "No reset", () -> { }, this::showGlobalSettingsOverview);
        };
    }

    private void handleGlobalSettingsPad(final int padIndex, final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (padIndex != SETTINGS_SHOW_DEACTIVATED_TRACKS_PAD || showDeactivatedTracksPref == null) {
            return;
        }
        showDeactivatedTracksPref.set(!showDeactivatedTracks());
        oled.valueInfo("Tracks", showDeactivatedTracks() ? "All" : "Active");
        refreshSurfaceLights();
    }

    private void handleGlobalSettingsModeAdvance(final boolean pressed) {
        if (pressed) {
            return;
        }
        if (consumeKnobModeGesture()) {
            oled.clearScreenDelayed();
            return;
        }
        globalSettingsEncoderMode = globalSettingsEncoderMode == EncoderMode.CHANNEL
                ? EncoderMode.MIXER
                : globalSettingsEncoderMode == EncoderMode.MIXER
                ? EncoderMode.USER_2
                : globalSettingsEncoderMode == EncoderMode.USER_2
                ? EncoderMode.USER_1
                : EncoderMode.CHANNEL;
        for (final EncoderStepAccumulator accumulator : globalSettingsAccumulators) {
            accumulator.reset();
        }
        showGlobalSettingsOverview();
        refreshSurfaceLights();
    }

    private BiColorLightState globalSettingsModeLightState() {
        return globalSettingsEncoderMode.getState();
    }

    private String globalSettingsPageLabel() {
        return switch (globalSettingsEncoderMode) {
            case MIXER -> "Input";
            case USER_2 -> "Clip";
            case USER_1 -> "Pins";
            default -> "Pitch";
        };
    }

    private void adjustGlobalInputSettings(final int encoderIndex, final int inc) {
        if (encoderIndex == 0) {
            if (sharedVelocitySettings.adjustSensitivity(inc)) {
                oled.paramInfo("Velocity Sens", sharedVelocitySettings.sensitivity(), "Global Input", 0, 100);
            }
            return;
        }
        if (encoderIndex == 1) {
            if (sharedVelocitySettings.adjustCenterVelocity(inc)) {
                oled.paramInfo("Velocity Center", sharedVelocitySettings.centerVelocity(), "Global Input",
                        sharedVelocitySettings.minCenterVelocity(), sharedVelocitySettings.maxCenterVelocity());
            }
            return;
        }
        if (encoderIndex == 2) {
            adjustPadBrightness(inc);
            return;
        }
        if (encoderIndex == 3) {
            adjustPadSaturation(inc);
            return;
        }
        showGlobalSettingsOverview();
    }

    private void adjustGlobalPinSettings(final int encoderIndex, final int inc) {
        if (encoderIndex == 0) {
            applyPinEncoder("Pin Track", viewControl.getCursorTrack().isPinned(), true, inc);
            return;
        }
        if (encoderIndex == 1) {
            applyPinEncoder("Pin Device", viewControl.getSelectedDevice().isPinned(),
                    viewControl.getSelectedDevice().exists().get(), inc);
            return;
        }
        if (encoderIndex == 2) {
            applyPinEncoder("Pin Clip", viewControl.getSelectedClip().isPinned(),
                    viewControl.getSelectedClip().exists().get(), inc);
            return;
        }
        showGlobalSettingsOverview();
    }

    private void showGlobalPinSetting(final int encoderIndex) {
        if (encoderIndex == 0) {
            oled.valueInfo("Pin Track", pinStateLabel(viewControl.getCursorTrack().isPinned().get()));
            return;
        }
        if (encoderIndex == 1) {
            showPinInfo("Pin Device", viewControl.getSelectedDevice().isPinned().get(),
                    viewControl.getSelectedDevice().exists().get());
            return;
        }
        if (encoderIndex == 2) {
            showPinInfo("Pin Clip", viewControl.getSelectedClip().isPinned().get(),
                    viewControl.getSelectedClip().exists().get());
            return;
        }
        showGlobalSettingsOverview();
    }

    private void applyPinEncoder(final String label,
                                 final SettableBooleanValue pinValue,
                                 final boolean targetExists,
                                 final int inc) {
        if (!targetExists) {
            oled.valueInfo(label, "No Target");
            return;
        }
        final boolean targetPinned = inc > 0;
        pinValue.set(targetPinned);
        oled.valueInfo(label, pinStateLabel(targetPinned));
    }

    private void showPinInfo(final String label, final boolean pinned, final boolean targetExists) {
        oled.valueInfo(label, targetExists ? pinStateLabel(pinned) : "No Target");
    }

    private String pinOverviewLabel(final boolean pinned, final boolean targetExists) {
        return targetExists ? pinStateLabel(pinned) : "--";
    }

    private String pinStateLabel(final boolean pinned) {
        return pinned ? "On" : "Off";
    }

    private void showGlobalInputSetting(final int encoderIndex) {
        if (encoderIndex == 0) {
            oled.valueInfo("Velocity Sens", sharedVelocitySettings.sensitivity() + "%");
            return;
        }
        if (encoderIndex == 1) {
            oled.valueInfo("Velocity Center", Integer.toString(sharedVelocitySettings.centerVelocity()));
            return;
        }
        if (encoderIndex == 2) {
            oled.valueInfo("Pad Bright", padBrightnessLabel());
            return;
        }
        if (encoderIndex == 3) {
            oled.valueInfo("Pad Sat", padSaturationLabel());
            return;
        }
        showGlobalSettingsOverview();
    }

    private void adjustGlobalClipSettings(final int encoderIndex, final int inc) {
        if (encoderIndex == 0) {
            adjustDefaultClipLength(inc);
            return;
        }
        if (encoderIndex == 1) {
            adjustLauncherRecordLength(inc);
            return;
        }
        showGlobalSettingsOverview();
    }

    private void showGlobalClipSetting(final int encoderIndex) {
        if (encoderIndex == 0) {
            oled.valueInfo("Create Len", defaultClipLengthLabel());
            return;
        }
        if (encoderIndex == 1) {
            oled.valueInfo("Record Len", launcherRecordLengthLabel());
            return;
        }
        showGlobalSettingsOverview();
    }

    private void adjustPadBrightness(final int inc) {
        final double next = FireControlPreferences.normalizePadBrightness(
                (padBrightnessPref == null ? padBrightness : padBrightnessPref.getRaw())
                        + inc * FireControlPreferences.PAD_BRIGHTNESS_STEP);
        if (padBrightnessPref != null) {
            padBrightnessPref.setRaw(next);
        } else {
            padBrightness = next;
        }
        oled.valueInfo("Pad Bright", padBrightnessLabel(next));
    }

    private void adjustPadSaturation(final int inc) {
        final double next = FireControlPreferences.normalizePadSaturation(
                (padSaturationPref == null ? padSaturation : padSaturationPref.getRaw())
                        + inc * FireControlPreferences.PAD_SATURATION_STEP);
        if (padSaturationPref != null) {
            padSaturationPref.setRaw(next);
        } else {
            padSaturation = next;
        }
        oled.valueInfo("Pad Sat", padSaturationLabel(next));
    }

    private void adjustDefaultClipLength(final int inc) {
        if (defaultClipLengthPref == null || inc == 0) {
            return;
        }
        final String current = FireControlPreferences.normalizeDefaultClipLength(defaultClipLengthPref.get());
        int currentIndex = 0;
        for (int i = 0; i < FireControlPreferences.DEFAULT_CLIP_LENGTHS.length; i++) {
            if (FireControlPreferences.DEFAULT_CLIP_LENGTHS[i].equals(current)) {
                currentIndex = i;
                break;
            }
        }
        final int nextIndex = Math.max(0,
                Math.min(FireControlPreferences.DEFAULT_CLIP_LENGTHS.length - 1, currentIndex + inc));
        defaultClipLengthPref.set(FireControlPreferences.DEFAULT_CLIP_LENGTHS[nextIndex]);
        oled.valueInfo("Create Len", FireControlPreferences.DEFAULT_CLIP_LENGTHS[nextIndex]);
    }

    private void adjustLauncherRecordLength(final int inc) {
        if (launcherRecordLengthPref == null || inc == 0) {
            return;
        }
        final String current = FireControlPreferences.normalizeLauncherRecordLength(launcherRecordLengthPref.get());
        int currentIndex = 0;
        for (int i = 0; i < FireControlPreferences.LAUNCHER_RECORD_LENGTHS.length; i++) {
            if (FireControlPreferences.LAUNCHER_RECORD_LENGTHS[i].equals(current)) {
                currentIndex = i;
                break;
            }
        }
        final int nextIndex = Math.max(0,
                Math.min(FireControlPreferences.LAUNCHER_RECORD_LENGTHS.length - 1, currentIndex + inc));
        launcherRecordLengthPref.set(FireControlPreferences.LAUNCHER_RECORD_LENGTHS[nextIndex]);
        oled.valueInfo("Record Len", FireControlPreferences.LAUNCHER_RECORD_LENGTHS[nextIndex]);
    }

    private String defaultClipLengthLabel() {
        return FireControlPreferences.normalizeDefaultClipLength(defaultClipLengthPref == null
                ? FireControlPreferences.CLIP_LENGTH_2_BARS
                : defaultClipLengthPref.get());
    }

    private String launcherRecordLengthLabel() {
        return FireControlPreferences.normalizeLauncherRecordLength(launcherRecordLengthPref == null
                ? FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_2_BARS
                : launcherRecordLengthPref.get());
    }

    private String padBrightnessLabel() {
        return padBrightnessLabel(padBrightnessPref == null ? padBrightness : padBrightnessPref.getRaw());
    }

    private String padBrightnessLabel(final double value) {
        return "%.0f%%".formatted(FireControlPreferences.normalizePadBrightness(value));
    }

    private String padSaturationLabel() {
        return padSaturationLabel(padSaturationPref == null ? padSaturation : padSaturationPref.getRaw());
    }

    private String padSaturationLabel(final double value) {
        return "%.0f%%".formatted(FireControlPreferences.normalizePadSaturation(value));
    }

    private RgbLigthState globalSettingsPadState(final int padIndex) {
        if (padIndex == SETTINGS_SHOW_DEACTIVATED_TRACKS_PAD) {
            return showDeactivatedTracks() ? SETTINGS_TOGGLE_ON : SETTINGS_TOGGLE_OFF;
        }
        final int row = padIndex / SETTINGS_PAD_COLUMNS;
        final int column = padIndex % SETTINGS_PAD_COLUMNS;
        if (row < 0 || row >= SETTINGS_PAD_ROWS || column < 0 || column >= SETTINGS_PAD_COLUMNS) {
            return RgbLigthState.OFF;
        }
        return SETTINGS_LOGO[row][column] ? SETTINGS_LOGO_ON : SETTINGS_LOGO_OFF;
    }

    public boolean isPopupBrowserActive() {
        return popupBrowser != null && popupBrowser.exists().get();
    }

    public BiColorLightState getBrowserLightState() {
        if (globalSettingsOverlayActive) {
            return BiColorLightState.AMBER_FULL;
        }
        return isPopupBrowserActive() ? BiColorLightState.RED_FULL : BiColorLightState.OFF;
    }

    public boolean isGlobalSettingsOverlayActive() {
        return globalSettingsOverlayActive;
    }

    public boolean showDeactivatedTracks() {
        return showDeactivatedTracksPref != null
                ? showDeactivatedTracksPref.get()
                : FireControlPreferences.SHOW_DEACTIVATED_TRACKS_DEFAULT;
    }

    public boolean isGlobalAltHeld() {
        final BiColorButton button = getButton(NoteAssign.ALT);
        return button != null ? button.isPressed() : altActive.get();
    }

    public boolean isGlobalShiftHeld() {
        final BiColorButton button = getButton(NoteAssign.SHIFT);
        return button != null ? button.isPressed() : shiftActive.get();
    }

    public boolean handleGlobalUndoRedoBankButton(final boolean pressed, final int amount) {
        final UndoRedoBankButtonHandler.Action action =
                UndoRedoBankButtonHandler.actionFor(pressed, amount, isGlobalAltHeld());
        if (action == UndoRedoBankButtonHandler.Action.NONE) {
            return false;
        }
        if (action == UndoRedoBankButtonHandler.Action.UNDO) {
            if (application.canUndo().get()) {
                application.undo();
                notifyAction("Undo", "Project");
            } else {
                notifyAction("Undo", "None");
            }
            return true;
        }
        if (application.canRedo().get()) {
            application.redo();
            notifyAction("Redo", "Project");
        } else {
            notifyAction("Redo", "None");
        }
        return true;
    }

    public void refreshGlobalSettingsOverlayState() {
        updateGlobalSettingsOverlayState();
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

    public boolean handleMainEncoderGlobalChord(final int inc) {
        final MainEncoderGlobalChord.Action action = MainEncoderGlobalChord.resolve(
                inc,
                isPopupBrowserActive(),
                patternPressed,
                isGlobalShiftHeld(),
                isGlobalAltHeld());
        switch (action) {
            case PLAYBACK_START_GRID -> {
                adjustPlaybackStartPositionByGrid(inc);
                return true;
            }
            case PLAYBACK_START_FINE -> {
                patternGestureConsumed = true;
                adjustPlaybackStartPositionFine(inc);
                return true;
            }
            case CUE_MARKER -> {
                patternGestureConsumed = true;
                jumpToCueMarker(inc);
                return true;
            }
            case TIMELINE_ZOOM_HORIZONTAL -> {
                zoomTimelineHorizontally(inc);
                return true;
            }
            case TIMELINE_ZOOM_VERTICAL -> {
                zoomTimelineVertically(inc);
                return true;
            }
            case NONE -> {
                return false;
            }
        }
        return false;
    }

    private void jumpToCueMarker(final int inc) {
        if (inc == 0 || transport == null) {
            return;
        }
        final double reference = Double.isNaN(cueMarkerNavigationPosition)
                ? transport.playStartPosition().get()
                : cueMarkerNavigationPosition;
        final int markerIndex = cueMarkerIndexAfterTurn(reference, inc, cueMarkerExists, cueMarkerPositions,
                cueMarkerBank.itemCount().get());
        if (markerIndex < 0) {
            oled.valueInfo("Cue Marker", "None");
            return;
        }
        final double markerPosition = cueMarkerPositions[markerIndex];
        setPlaybackStartPosition(markerPosition);
        cueMarkerNavigationPosition = markerPosition;
        oled.valueInfo("Cue Marker", cueMarkerLabel(markerIndex));
    }

    private String cueMarkerLabel(final int markerIndex) {
        final String name = cueMarkerNames[markerIndex];
        if (name != null && !name.isBlank()) {
            return name;
        }
        return "Marker " + (markerIndex + 1);
    }

    static int cueMarkerIndexAfterTurn(final double reference,
                                       final int inc,
                                       final boolean[] exists,
                                       final double[] positions,
                                       final int itemCount) {
        final int observedLimit = itemCount > 0 ? itemCount : exists.length;
        final int limit = Math.min(Math.min(exists.length, positions.length), observedLimit);
        if (inc == 0 || limit == 0) {
            return -1;
        }
        final double epsilon = 0.000001;
        if (inc > 0) {
            for (int index = 0; index < limit; index++) {
                if (exists[index] && positions[index] > reference + epsilon) {
                    return index;
                }
            }
            return -1;
        }
        for (int index = limit - 1; index >= 0; index--) {
            if (exists[index] && positions[index] < reference - epsilon) {
                return index;
            }
        }
        return -1;
    }

    private void zoomTimelineHorizontally(final int inc) {
        if (application == null) {
            return;
        }
        for (int i = 0; i < Math.abs(inc); i++) {
            if (inc > 0) {
                application.zoomIn();
            } else {
                application.zoomOut();
            }
        }
        oled.valueInfo("Timeline Zoom", inc > 0 ? "In" : "Out");
    }

    private void zoomTimelineVertically(final int inc) {
        if (arranger == null || detailEditor == null) {
            return;
        }
        for (int i = 0; i < Math.abs(inc); i++) {
            if (inc > 0) {
                arranger.zoomInLaneHeightsSelected();
                detailEditor.zoomInLaneHeights();
            } else {
                arranger.zoomOutLaneHeightsSelected();
                detailEditor.zoomOutLaneHeights();
            }
        }
        oled.valueInfo("Lane Zoom", inc > 0 ? "In" : "Out");
    }

    public void adjustSelectedTrack(final int inc, final boolean pageStep) {
        if (inc == 0 || viewControl == null) {
            return;
        }
        if (shouldAutoPinStandardDrumMode()) {
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
        final int targetIndex = nextSelectableTrackIndex(trackBank, currentIndex, delta);
        if (targetIndex < 0) {
            return;
        }
        final Track targetTrack = trackBank.getItemAt(targetIndex);
        if (!isSelectableTrack(targetTrack)) {
            return;
        }
        targetTrack.selectInMixer();
        targetTrack.selectInEditor();
        cursorTrack.selectChannel(targetTrack);
        final String trackName = targetTrack.name().get();
        showSelectedTrackInfo(pageStep, trackName);
    }

    private int nextSelectableTrackIndex(final TrackBank trackBank, final int currentIndex, final int delta) {
        if (delta == 0) {
            return currentIndex;
        }
        final int direction = delta > 0 ? 1 : -1;
        int remaining = Math.abs(delta);
        int candidate = currentIndex;
        while (remaining > 0) {
            candidate += direction;
            if (candidate < 0 || candidate >= trackBank.getSizeOfBank()) {
                return -1;
            }
            if (isSelectableTrack(trackBank.getItemAt(candidate))) {
                remaining--;
            }
        }
        return candidate;
    }

    private boolean isSelectableTrack(final Track track) {
        return track != null
                && track.exists().get()
                && (showDeactivatedTracks() || track.isActivated().get());
    }

    public void showSelectedTrackInfo(final boolean pageStep) {
        if (viewControl == null) {
            return;
        }
        final String trackName = viewControl.getCursorTrack().name().get();
        showSelectedTrackInfo(pageStep, trackName);
    }

    private void showSelectedTrackInfo(final boolean pageStep, final String trackName) {
        showTrackInfo(pageStep ? "Track Page" : "Track Select", trackName);
    }

    private void showIdleTrackInfo() {
        if (viewControl == null) {
            return;
        }
        showTrackInfo("Track", viewControl.getCursorTrack().name().get());
    }

    private void showTrackInfo(final String title, final String trackName) {
        oled.valueInfo(title,
                trackName == null || trackName.isBlank() ? "Unnamed" : trackName);
    }

    private void handleBrowserPressed(final boolean pressed) {
        if (!pressed) {
            browserPressToken++;
            updateGlobalSettingsOverlayState();
            if (!globalSettingsOverlayActive) {
                oled.clearScreenDelayed();
            }
            return;
        }
        if (globalSettingsOverlayActive && !isGlobalShiftHeld()) {
            globalSettingsOverlayLatch.close();
            updateGlobalSettingsOverlayState();
            notifyAction("Settings", "Closed");
            return;
        }
        if (isPopupBrowserActive()) {
            popupBrowser.cancel();
            notifyAction("Browser", "Closed");
            return;
        }
        if (isGlobalShiftHeld() && !isGlobalAltHeld()) {
            final boolean wasLatched = globalSettingsOverlayLatch.isLatched();
            globalSettingsOverlayLatch.toggleLatch(true);
            if (wasLatched) {
                deactivateGlobalSettingsOverlay();
                notifyAction("Settings", "Closed");
                return;
            }
            updateGlobalSettingsOverlayState();
            notifyAction("Settings", "Latched");
            return;
        }
        updateGlobalSettingsOverlayState();
        final int pressToken = ++browserPressToken;
        host.scheduleTask(() -> maybeOpenPopupBrowser(pressToken), BROWSER_OPEN_DEFER_MS);
    }

    private void maybeOpenPopupBrowser(final int pressToken) {
        if (pressToken != browserPressToken) {
            return;
        }
        final BiColorButton browserButton = getButton(NoteAssign.BROWSER);
        if (browserButton == null || !browserButton.isPressed()) {
            return;
        }
        if (globalSettingsOverlayActive || isPopupBrowserActive()) {
            return;
        }
        openPopupBrowser();
    }

    private void openPopupBrowser() {
        final PinnableCursorDevice primaryDevice = viewControl.getPrimaryDevice();
        final boolean shiftHeld = getButton(NoteAssign.SHIFT).isPressed();
        final boolean altHeld = getButton(NoteAssign.ALT).isPressed();
        if (shiftHeld && altHeld) {
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

    public boolean isKnobModeHeld() {
        final BiColorButton button = getButton(NoteAssign.KNOB_MODE);
        return button != null && button.isPressed();
    }

    public boolean handleKnobModePatternRemotePage(final int direction) {
        if (!isKnobModeHeld()) {
            return false;
        }
        knobModeGestureConsumed = true;
        final RemotePageTarget target = currentRemotePageTarget();
        if (target == null || target.page() == null) {
            oled.valueInfo("Remote Page", "No remotes");
            oled.clearScreenDelayed();
            suppressTransientOledOverlays();
            return true;
        }
        showRemotePageNavigation(target, direction);
        suppressTransientOledOverlays();
        return true;
    }

    public boolean handleKnobModeEncoderReset(final boolean touched,
                                              final boolean resettable,
                                              final String fallbackLabel,
                                              final String unavailableDetail,
                                              final Runnable resetAction,
                                              final Runnable showAction) {
        final boolean handled = ParameterEncoderBinding.handleExplicitResetTouch(touched, knobModeEncoderResetControl(), resettable,
                fallbackLabel, unavailableDetail, resetAction, showAction, oled::valueInfo);
        if (handled) {
            suppressTransientOledOverlays();
        }
        return handled;
    }

    public ParameterEncoderBinding.ExplicitResetControl knobModeEncoderResetControl() {
        return new ParameterEncoderBinding.ExplicitResetControl() {
            @Override
            public boolean isHeld() {
                return isKnobModeHeld();
            }

            @Override
            public void consume() {
                knobModeGestureConsumed = true;
            }
        };
    }

    public boolean consumeKnobModeGesture() {
        if (!knobModeGestureConsumed) {
            return false;
        }
        knobModeGestureConsumed = false;
        return true;
    }

    public BiColorLightState knobModeRemotePageLightState(final int direction) {
        final RemotePageTarget target = currentRemotePageTarget();
        if (target == null || target.page() == null) {
            return BiColorLightState.OFF;
        }
        final CursorRemoteControlsPage page = target.page();
        return remotePageNavigationLightState(page.selectedPageIndex().get(), page.pageCount().getAsInt(), direction);
    }

    private void suppressTransientOledOverlays() {
        if (modeState.activeMode() == Mode.PERFORM && performMode != null) {
            performMode.suppressMixMeterDisplay();
            return;
        }
        if (modeState.activeMode() == Mode.DRUM
                && activeDrumSubMode == DrumSubMode.STANDARD
                && drumSequenceMode != null) {
            drumSequenceMode.suppressDrumMeterDisplay();
        }
    }

    private RemotePageTarget currentRemotePageTarget() {
        return switch (modeState.activeMode()) {
            case NOTE_PLAY -> notePlayMode == null ? null : notePlayMode.currentRemotePageTarget();
            case DRUM -> currentDrumRemotePageTarget();
            case PERFORM -> performMode == null ? null : performMode.currentRemotePageTarget();
            case CHORD_STEP, MELODIC_STEP, FUGUE_STEP, NESTED_RHYTHM -> null;
        };
    }

    private RemotePageTarget currentDrumRemotePageTarget() {
        if (activeDrumSubMode == DrumSubMode.DRUM_PADS && drumPadPlayMode != null) {
            return drumPadPlayMode.currentRemotePageTarget();
        }
        if (activeDrumSubMode != DrumSubMode.STANDARD || drumSequenceMode == null) {
            return null;
        }
        final CursorRemoteControlsPage page = drumSequenceMode.getActiveRemoteControlsPage();
        return page == null ? null : new RemotePageTarget(page, "Pad");
    }

    private void showRemotePageNavigation(final RemotePageTarget target, final int direction) {
        final CursorRemoteControlsPage page = target.page();
        final int pageCount = page.pageCount().getAsInt();
        final String title = target.label() + " Page";
        if (pageCount <= 1) {
            oled.valueInfo(title, "Page 1/1");
            oled.clearScreenDelayed();
            return;
        }
        final int currentPage = page.selectedPageIndex().get();
        final int nextPage = remotePageIndexAfterTurn(currentPage, pageCount, direction);
        if (nextPage == currentPage) {
            oled.valueInfo(title, direction < 0 ? "First page" : "Last page");
            oled.sendString(0, OledDisplay.TextJustification.RIGHT, 7,
                    remotePageCountLabel(currentPage, pageCount));
            oled.clearScreenDelayed();
            return;
        }
        page.selectedPageIndex().set(nextPage);
        oled.valueInfo(title, remotePageName(page, nextPage));
        oled.sendString(0, OledDisplay.TextJustification.RIGHT, 7, remotePageCountLabel(nextPage, pageCount));
        oled.clearScreenDelayed();
    }

    private String remotePageName(final CursorRemoteControlsPage page, final int pageIndex) {
        final String pageName = page.pageNames().get(pageIndex);
        if (pageName != null && !pageName.isBlank()) {
            return pageName;
        }
        final String currentName = page.getName().get();
        if (currentName != null && !currentName.isBlank()) {
            return currentName;
        }
        return "Page " + (pageIndex + 1);
    }

    static int remotePageIndexAfterTurn(final int currentPage, final int pageCount, final int direction) {
        if (pageCount <= 0) {
            return currentPage;
        }
        return Math.max(0, Math.min(pageCount - 1, currentPage + direction));
    }

    static String remotePageCountLabel(final int pageIndex, final int pageCount) {
        return pageCount > 1 ? (pageIndex + 1) + "/" + pageCount : "";
    }

    static BiColorLightState remotePageNavigationLightState(final int currentPage,
                                                            final int pageCount,
                                                            final int direction) {
        if (pageCount <= 1) {
            return BiColorLightState.OFF;
        }
        if (direction < 0) {
            return currentPage > 0 ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF;
        }
        if (direction > 0) {
            return currentPage < pageCount - 1 ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF;
        }
        return BiColorLightState.OFF;
    }

    static BrowserTransportAction browserTransportAction(final boolean browserActive,
                                                         final NoteAssign assignment,
                                                         final boolean pressed) {
        if (!browserActive || !pressed) {
            return BrowserTransportAction.NONE;
        }
        return switch (assignment) {
            case PLAY -> BrowserTransportAction.COMMIT;
            case STOP -> BrowserTransportAction.CANCEL;
            default -> BrowserTransportAction.NONE;
        };
    }

    static PlayPressAction playPressAction(final boolean playing) {
        return playPressAction(playing, false);
    }

    static PlayPressAction playPressAction(final boolean playing, final boolean retriggerLaunchersArmed) {
        if (playing) {
            return PlayPressAction.STOP;
        }
        if (retriggerLaunchersArmed) {
            return PlayPressAction.RETRIGGER_LAUNCHERS_FROM_START;
        }
        return PlayPressAction.LAUNCH_FROM_PLAY_START;
    }

    static StopPressAction stopPressAction(final boolean playing) {
        return playing ? StopPressAction.STOP : StopPressAction.GO_ARRANGEMENT_START;
    }

    static PatternReleaseAction patternReleaseAction(final boolean gestureConsumed,
                                                     final boolean shiftHeld,
                                                     final boolean altHeld) {
        if (gestureConsumed) {
            return PatternReleaseAction.NONE;
        }
        if (shiftHeld) {
            return PatternReleaseAction.TOGGLE_METRONOME;
        }
        if (altHeld) {
            return PatternReleaseAction.TOGGLE_LAUNCHER_OVERDUB;
        }
        return PatternReleaseAction.TOGGLE_AUTOMATION_WRITE;
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
