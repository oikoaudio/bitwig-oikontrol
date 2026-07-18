package com.oikoaudio.fire;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.MusicalScale;
import com.bitwig.extensions.framework.MusicalScaleLibrary;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.Midi;
import com.oikoaudio.fire.TopLevelModeState.DrumMode;
import com.oikoaudio.fire.TopLevelModeState.Mode;
import com.oikoaudio.fire.TopLevelModeState.PerformMode;
import com.oikoaudio.fire.chordstep.ChordStepMode;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.ModeButtonLights;
import com.oikoaudio.fire.control.ParameterEncoderBinding;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.control.UndoRedoBankButtonHandler;
import com.oikoaudio.fire.control.VelocitySettings;
import com.oikoaudio.fire.display.EncoderLegendPosition;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.fugue.FugueStepMode;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.melodic.MelodicStepMode;
import com.oikoaudio.fire.multiclip.MulticlipSequenceMode;
import com.oikoaudio.fire.music.SharedPitchContextController;
import com.oikoaudio.fire.nestedrhythm.NestedRhythmMode;
import com.oikoaudio.fire.note.DrumPadPlayMode;
import com.oikoaudio.fire.note.NotePlayMode;
import com.oikoaudio.fire.perform.PerformClipLauncherMode;
import com.oikoaudio.fire.sequence.DrumSequenceMode;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;
import com.oikoaudio.fire.sequence.NoteVariationAmounts;
import com.oikoaudio.fire.utils.PatternButtons;
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

    record LauncherOverdubPressPlan(
            boolean launcherOverdubEnabled,
            boolean automationWriteEnabled,
            boolean touchAutomationWriteMode,
            boolean automationWriteAutoEnabled) {}

    public record RemotePageTarget(CursorRemoteControlsPage page, String label) {}

    private static final double MAIN_ENCODER_STEP = 0.01;
    private static final double MAIN_ENCODER_FINE_STEP = 0.0025;
    private static final double LAST_TOUCHED_ENCODER_STEP = 0.04;
    private static final double LAST_TOUCHED_ENCODER_FINE_STEP = 0.01;
    private static final int DEVICE_DISCOVERY_WIDTH = 128;
    private static final int CUE_MARKER_BANK_SIZE = 128;
    private static final int GLOBAL_VELOCITY_CENTER_DEFAULT = 100;
    private static final int GLOBAL_VELOCITY_MIN = 1;
    private static final int GLOBAL_VELOCITY_MAX = 126;
    public static final String MAIN_ENCODER_LAST_TOUCHED_ROLE =
            FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED;
    public static final String MAIN_ENCODER_SHUFFLE_ROLE =
            FireControlPreferences.MAIN_ENCODER_SHUFFLE;
    public static final String MAIN_ENCODER_TEMPO_ROLE = FireControlPreferences.MAIN_ENCODER_TEMPO;
    public static final String MAIN_ENCODER_NOTE_REPEAT_ROLE =
            FireControlPreferences.MAIN_ENCODER_NOTE_REPEAT;
    public static final String MAIN_ENCODER_TRACK_SELECT_ROLE =
            FireControlPreferences.MAIN_ENCODER_TRACK_SELECT;
    public static final String MAIN_ENCODER_PLAYBACK_START_ROLE =
            FireControlPreferences.MAIN_ENCODER_PLAYBACK_START;
    public static final String MAIN_ENCODER_DRUM_GRID_ROLE =
            FireControlPreferences.MAIN_ENCODER_DRUM_GRID;
    private static final String ACTION_JUMP_TO_END_OF_ARRANGEMENT = "jump_to_end_of_arrangement";
    private static final String RECORD_QUANTIZATION_OFF = "OFF";
    private static final String RECORD_QUANTIZATION_DEFAULT_ON = "1/16";
    private static final String AUTOMATION_WRITE_MODE_TOUCH = "touch";
    private static final long STOPPED_METER_RING_OUT_MS = 2000;
    private static final long STOPPED_IDLE_TRACK_REFRESH_MS = 2500;
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

    public static final byte SE_ST = (byte) 0xf0;
    public static final byte SE_EN = (byte) 0xf7;
    public static final byte MAN_ID_AKAI = 0x47;
    public static final byte DEVICE_ID = 0x7f;
    public static final byte PRODUCT_ID = 0x43;
    public static final byte SE_CMD_RGB = 0x65;
    public static final byte SE_OLED_RGB = 0x08;
    private static final String DEV_INQ = "F0 7E 00 06 01 F7";
    private final byte[] singleRgb =
            new byte[] {
                SE_ST, MAN_ID_AKAI, DEVICE_ID, PRODUCT_ID, SE_CMD_RGB, 0, 4, 0, 0, 0, 0, SE_EN
            };

    private final int[] lastCcValue = new int[128];
    private final RgbLightState[] currentPadStates = new RgbLightState[64];

    private Layer mainLayer;
    private GlobalSettingsOverlayController globalSettingsOverlay;
    private final RgbButton[] rgbButtons = new RgbButton[64];
    private final TouchEncoder[] encoders = new TouchEncoder[4];
    private final MultiStateHardwareLight[] stateLights = new MultiStateHardwareLight[4];
    private final Map<NoteAssign, BiColorButton> controlButtons = new HashMap<>();
    private NoteInput noteInput;
    private DrumSequenceMode drumSequenceMode;
    private MulticlipSequenceMode multiclipSequenceMode;
    private ViewCursorControl viewControl;
    private FireDeviceLocator deviceLocator;
    private DrumAutoPinController drumAutoPinController;

    private final BooleanValueObject shiftActive = new BooleanValueObject();
    private OledDisplay oled;
    private ControllerHost host;
    private TouchEncoder mainEncoder;
    private LastClickedParameter lastClickedParameter;
    private Groove groove;
    private final BooleanValueObject altActive = new BooleanValueObject();
    private final NoteVariationAmounts noteVariationAmounts = new NoteVariationAmounts();
    private PopupBrowserController popupBrowserController;
    private GlobalMainEncoderController globalMainEncoderController;
    private final GlobalMainEncoderController.RoleActions globalMainEncoderRoleActions =
            new GlobalMainEncoderController.RoleActions() {
                @Override
                public void adjustTempo(final int inc, final boolean fine) {
                    AkaiFireOikontrolExtension.this.adjustTempo(inc, fine);
                }

                @Override
                public void adjustShuffle(final int inc, final boolean fine) {
                    adjustGrooveShuffleAmount(inc, fine);
                }

                @Override
                public void adjustTrackSelection(final int inc, final boolean pageStep) {
                    adjustSelectedTrack(inc, pageStep);
                }

                @Override
                public void adjustPlaybackStart(final int inc) {
                    adjustPlaybackStartPositionByGrid(inc);
                }
            };
    private final GlobalMainEncoderController.GlobalChordActions globalMainEncoderChordActions =
            new GlobalMainEncoderController.GlobalChordActions() {
                @Override
                public void consumePatternGesture() {
                    patternGestureConsumed = true;
                }

                @Override
                public void adjustPlaybackStartByGrid(final int inc) {
                    adjustPlaybackStartPositionByGrid(inc);
                }

                @Override
                public void adjustPlaybackStartFine(final int inc) {
                    adjustPlaybackStartPositionFine(inc);
                }

                @Override
                public void jumpToCueMarker(final int inc) {
                    AkaiFireOikontrolExtension.this.jumpToCueMarker(inc);
                }

                @Override
                public void zoomTimelineHorizontally(final int inc) {
                    AkaiFireOikontrolExtension.this.zoomTimelineHorizontally(inc);
                }

                @Override
                public void zoomTimelineVertically(final int inc) {
                    AkaiFireOikontrolExtension.this.zoomTimelineVertically(inc);
                }
            };
    private final boolean[] cueMarkerExists = new boolean[CUE_MARKER_BANK_SIZE];
    private final double[] cueMarkerPositions = new double[CUE_MARKER_BANK_SIZE];
    private final String[] cueMarkerNames = new String[CUE_MARKER_BANK_SIZE];

    private FirePreferences firePreferences;
    private String tempoDisplayValue = "";
    private boolean tempoDisplayPending = false;
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
    private String lastRecordQuantizationGrid = RECORD_QUANTIZATION_DEFAULT_ON;
    private int transportTimeSignatureNumerator = 4;
    private int transportTimeSignatureDenominator = 4;
    private double cueMarkerNavigationPosition = Double.NaN;
    private boolean performRecordPadGestureConsumed = false;
    private long screenMessageHoldMs = FireControlPreferences.SCREEN_MESSAGE_HOLD_NORMAL_MS;
    private long stoppedMeterRingOutUntilMs = 0;
    private int stoppedMeterRingOutGeneration = 0;
    private long lastStoppedIdleTrackRefreshMs = 0;
    private final SharedPitchContextController sharedPitchContext =
            new SharedPitchContextController(
                    new SharedMusicalContext(MusicalScaleLibrary.getInstance()),
                    MusicalScaleLibrary.getInstance());
    private final VelocitySettings sharedVelocitySettings =
            new VelocitySettings(
                    GLOBAL_VELOCITY_CENTER_DEFAULT, GLOBAL_VELOCITY_MIN, GLOBAL_VELOCITY_MAX, 100);

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

    protected AkaiFireOikontrolExtension(
            final AkaiFireOikontrolDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        host = getHost();
        Arrays.fill(lastCcValue, -1);

        lastClickedParameter =
                host.createLastClickedParameter(
                        "FIRE_LAST_CLICKED_PARAMETER", "Fire Last Clicked Parameter");
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
        application.recordQuantizationGrid().markInterested();
        application
                .recordQuantizationGrid()
                .addValueObserver(this::handleRecordQuantizationChanged);

        // Host-backed resources and shared cursor state must exist before global collaborators.
        layers = new Layers(this);
        midiIn = host.getMidiInPort(0);
        midiIn.setSysexCallback(this::onSysEx);
        midiOut = host.getMidiOutPort(0);
        transport = host.createTransport();
        surface = host.createHardwareSurface();
        noteInput = midiIn.createNoteInput("MIDI", "80????", "90????", "A0????", "D0????");
        noteInput.setShouldConsumeEvents(false);
        viewControl = new ViewCursorControl(host, 16);
        deviceLocator = new FireDeviceLocator(host, DEVICE_DISCOVERY_WIDTH);
        initDrumAutoPinController();

        mainLayer = new Layer(layers, "Main");
        oled = new OledDisplay(midiOut);
        initPopupBrowserController();
        globalMainEncoderController =
                new GlobalMainEncoderController(
                        this::isDrumGridRoleAvailable, this::shouldAutoPinFirstDrumMachine);
        noteRepeatHandler =
                new NoteRepeatHandler(
                        noteInput,
                        oled,
                        () ->
                                drumSequenceMode != null
                                        ? drumSequenceMode.getActiveRemoteControlsPage()
                                        : null,
                        () ->
                                drumSequenceMode != null
                                        ? drumSequenceMode.getAccentHandler().getCurrentVelocity()
                                        : 100);

        // Hardware bindings precede live preference callbacks and shared musical-state
        // initialization.
        setUpHardware();
        setUpTransportControl();
        setUpPreferences();
        initializeSharedPitchContext();
        initializeSharedVelocitySettings();

        // Modes are composed only after all shared controls, preferences, and state owners are
        // ready.
        patternButtons = new PatternButtons(this, mainLayer);
        drumSequenceMode = new DrumSequenceMode(this, noteRepeatHandler);
        multiclipSequenceMode = new MulticlipSequenceMode(this);
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
        multiclipSequenceMode.notifyBlink(blinkTicks);
        notePlayMode.notifyBlink(blinkTicks);
        drumPadPlayMode.notifyBlink(blinkTicks);
        chordStepMode.notifyBlink(blinkTicks);
        melodicStepMode.notifyBlink(blinkTicks);
        fugueStepMode.notifyBlink(blinkTicks);
        nestedRhythmMode.notifyBlink(blinkTicks);
        performMode.notifyBlink(blinkTicks);
        refreshStoppedIdleTrackInfoIfNeeded();
        host.scheduleTask(this::handlePing, 100);
    }

    private void refreshStoppedIdleTrackInfoIfNeeded() {
        if (isTransportPlaying()
                || shouldShowMeterIdleDisplay()
                || oled.hasPendingTransientMessage()) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (now - lastStoppedIdleTrackRefreshMs >= STOPPED_IDLE_TRACK_REFRESH_MS) {
            if (showActiveModeIdleOledInfo()) {
                lastStoppedIdleTrackRefreshMs = now;
                return;
            }
            showIdleModeTrackInfo(currentModeLabel());
        }
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
        firePreferences =
                new FirePreferences(
                        getHost().getPreferences(),
                        new FirePreferences.Listener() {
                            @Override
                            public void launchQuantizationChanged(final String quantizationValue) {
                                transport.defaultLaunchQuantization().set(quantizationValue);
                            }

                            @Override
                            public void mainEncoderStartupChanged(final String startupState) {
                                applyMainEncoderStartupPreference(startupState);
                            }

                            @Override
                            public void drumPinModeChanged(final boolean autoPin) {
                                syncDrumPinningForActiveMode();
                                if (!drumPinPreferenceObserved) {
                                    drumPinPreferenceObserved = true;
                                    return;
                                }
                                notifyAction("Drum Pin", autoPin ? "Automatic" : "Follow Selected");
                            }

                            @Override
                            public void padAppearanceChanged(
                                    final double brightness, final double saturation) {
                                redrawRgbPads();
                            }

                            @Override
                            public void screenMessageHoldChanged(final long holdMillis) {
                                applyScreenMessageHoldPreference(holdMillis);
                            }

                            @Override
                            public void encoderLegendPositionChanged(final String position) {
                                applyEncoderLegendPositionPreference(position);
                            }
                        });
        redrawRgbPads();
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
        transport
                .timeSignature()
                .numerator()
                .addValueObserver(value -> transportTimeSignatureNumerator = value);
        transport
                .timeSignature()
                .denominator()
                .addValueObserver(value -> transportTimeSignatureDenominator = value);
        transport.tempo().markInterested();
        transport.tempo().name().markInterested();
        transport.tempo().value().markInterested();
        transport.tempo().value().displayedValue().markInterested();
        transport
                .tempo()
                .value()
                .displayedValue()
                .addValueObserver(
                        value -> {
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
        // addButton(NoteAssign.DRUM);
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
        browserButton.bindPressed(
                mainLayer, this::handleBrowserPressed, this::getBrowserLightState);
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
            marker.position()
                    .addValueObserver(position -> cueMarkerPositions[markerIndex] = position);
            marker.name().markInterested();
            marker.name().addValueObserver(name -> cueMarkerNames[markerIndex] = name);
        }
    }

    private void initGlobalSettingsOverlay() {
        globalSettingsOverlay =
                new GlobalSettingsOverlayController(
                        layers,
                        encoders,
                        rgbButtons,
                        getButton(NoteAssign.KNOB_MODE),
                        oled,
                        sharedPitchContext,
                        sharedVelocitySettings,
                        firePreferences,
                        viewControl,
                        new GlobalSettingsOverlayController.Host() {
                            @Override
                            public boolean browserButtonPressed() {
                                final BiColorButton button = getButton(NoteAssign.BROWSER);
                                return button != null && button.isPressed();
                            }

                            @Override
                            public boolean popupBrowserActive() {
                                return isPopupBrowserActive();
                            }

                            @Override
                            public boolean shiftHeld() {
                                return isGlobalShiftHeld();
                            }

                            @Override
                            public boolean altHeld() {
                                return isGlobalAltHeld();
                            }

                            @Override
                            public Mode activeMode() {
                                return modeState.activeMode();
                            }

                            @Override
                            public void prepareActivation() {
                                prepareGlobalSettingsOverlayActivation();
                            }

                            @Override
                            public void restoreActiveMode() {
                                switchActiveMode();
                            }

                            @Override
                            public void refreshSurfaceLights() {
                                AkaiFireOikontrolExtension.this.refreshSurfaceLights();
                            }

                            @Override
                            public boolean consumeKnobModeGesture() {
                                return AkaiFireOikontrolExtension.this.consumeKnobModeGesture();
                            }

                            @Override
                            public ParameterEncoderBinding.ExplicitResetControl
                                    explicitResetControl() {
                                return knobModeEncoderResetControl();
                            }
                        });
    }

    private void initPopupBrowserController() {
        popupBrowserController =
                PopupBrowserController.create(
                        host,
                        viewControl,
                        oled,
                        new PopupBrowserController.Host() {
                            @Override
                            public boolean browserButtonPressed() {
                                final BiColorButton button = getButton(NoteAssign.BROWSER);
                                return button != null && button.isPressed();
                            }

                            @Override
                            public boolean shiftHeld() {
                                return isGlobalShiftHeld();
                            }

                            @Override
                            public boolean altHeld() {
                                return isGlobalAltHeld();
                            }

                            @Override
                            public boolean overlayActive() {
                                return isGlobalSettingsOverlayActive();
                            }

                            @Override
                            public boolean overlayLatched() {
                                return globalSettingsOverlay != null
                                        && globalSettingsOverlay.isLatched();
                            }

                            @Override
                            public void toggleOverlayLatch() {
                                if (globalSettingsOverlay != null) {
                                    globalSettingsOverlay.toggleLatch();
                                }
                            }

                            @Override
                            public void closeOverlayLatch() {
                                if (globalSettingsOverlay != null) {
                                    globalSettingsOverlay.closeLatch();
                                }
                            }

                            @Override
                            public void deactivateOverlay() {
                                if (globalSettingsOverlay != null) {
                                    globalSettingsOverlay.deactivate();
                                }
                            }

                            @Override
                            public void refreshOverlayState() {
                                updateGlobalSettingsOverlayState();
                            }

                            @Override
                            public void notifyAction(final String title, final String value) {
                                AkaiFireOikontrolExtension.this.notifyAction(title, value);
                            }
                        });
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
        final MultiStateHardwareLight light =
                surface.createMultiStateHardwareLight("BASIC_LIGHT_" + assignment.toString());
        final int ccValue = assignment.getNoteValue();
        light.state()
                .onUpdateHardware(
                        state -> {
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

    public boolean shouldShowPlaybackNoteChordDisplay() {
        return firePreferences != null
                && FireControlPreferences.shouldShowPlaybackNoteChordDisplay(
                        firePreferences.noteChordDisplay());
    }

    private void handleTransportPlayingChanged(final boolean playing) {
        stoppedMeterRingOutGeneration++;
        if (playing) {
            stoppedMeterRingOutUntilMs = 0;
            lastStoppedIdleTrackRefreshMs = 0;
            return;
        }
        stoppedMeterRingOutUntilMs = System.currentTimeMillis() + STOPPED_METER_RING_OUT_MS;
        final int generation = stoppedMeterRingOutGeneration;
        host.scheduleTask(
                () -> {
                    if (generation == stoppedMeterRingOutGeneration
                            && !isTransportPlaying()
                            && System.currentTimeMillis() >= stoppedMeterRingOutUntilMs) {
                        showIdleOledInfo();
                    }
                },
                STOPPED_METER_RING_OUT_MS);
    }

    public int getTransportTimeSignatureNumerator() {
        return transportTimeSignatureNumerator;
    }

    public int getTransportTimeSignatureDenominator() {
        return transportTimeSignatureDenominator;
    }

    private BiColorLightState getRecordState() {
        if (isGlobalAltHeld()) {
            return transport.isArrangerOverdubEnabled().get()
                    ? BiColorLightState.AMBER_FULL
                    : BiColorLightState.AMBER_HALF;
        }
        if (modeState.activeMode() == Mode.DRUM) {
            return transport.isClipLauncherOverdubEnabled().get()
                    ? BiColorLightState.RED_FULL
                    : BiColorLightState.OFF;
        }
        if (isPerformRecordTargetingHeld()) {
            return BiColorLightState.RED_HALF;
        }
        return transport.isArrangerRecordEnabled().get()
                ? BiColorLightState.RED_FULL
                : BiColorLightState.OFF;
    }

    private BiColorLightState getPatternState() {
        if (getButton(NoteAssign.SHIFT).isPressed()) {
            return transport.isMetronomeEnabled().get()
                    ? BiColorLightState.GREEN_FULL
                    : BiColorLightState.GREEN_HALF;
        }
        if (isGlobalAltHeld()) {
            return transport.isClipLauncherOverdubEnabled().get()
                    ? BiColorLightState.AMBER_HALF
                    : BiColorLightState.AMBER_FULL;
        }
        return isAutomationWriteEnabled() ? BiColorLightState.AMBER_FULL : BiColorLightState.OFF;
    }

    private BiColorLightState getDrumState() {
        return modeState.activeMode() == Mode.DRUM
                ? drumModeLightState(modeState.activeDrumMode())
                : BiColorLightState.OFF;
    }

    private BiColorLightState drumModeLightState(final DrumMode mode) {
        return switch (mode) {
            case STANDARD -> ModeButtonLights.MODE_1;
            case MULTICLIP_SEQ -> ModeButtonLights.MODE_4;
            case NESTED_RHYTHM -> ModeButtonLights.MODE_2;
            case DRUM_PADS -> ModeButtonLights.MODE_3;
        };
    }

    private String drumModeLabel(final DrumMode mode) {
        return switch (mode) {
            case STANDARD -> "Drum XOX";
            case MULTICLIP_SEQ -> "Multiclip Seq";
            case NESTED_RHYTHM -> "NestedRytm";
            case DRUM_PADS -> "Drum Pads";
        };
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

    private void dummyAction(final boolean pressed) {}

    private void stopAction(final boolean pressed) {
        if (browserTransportAction(isPopupBrowserActive(), NoteAssign.STOP, pressed)
                == BrowserTransportAction.CANCEL) {
            popupBrowserController.cancel();
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
        final boolean nextState =
                automationWriteNextState(
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

    static boolean automationWriteNextState(
            final boolean arrangerWriteEnabled, final boolean clipLauncherWriteEnabled) {
        return !(arrangerWriteEnabled || clipLauncherWriteEnabled);
    }

    static boolean overdubNextState(final boolean overdubEnabled) {
        return !overdubEnabled;
    }

    static LauncherOverdubPressPlan launcherOverdubPressPlan(
            final boolean launcherOverdubEnabled,
            final boolean automationWriteEnabled,
            final boolean automationWriteAutoEnabled) {
        final boolean nextLauncherOverdubEnabled = overdubNextState(launcherOverdubEnabled);
        if (nextLauncherOverdubEnabled) {
            return new LauncherOverdubPressPlan(true, true, true, !automationWriteEnabled);
        }
        return new LauncherOverdubPressPlan(
                false, automationWriteAutoEnabled ? false : automationWriteEnabled, false, false);
    }

    private void toggleLauncherOverdubEnabled() {
        final LauncherOverdubPressPlan plan =
                launcherOverdubPressPlan(
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
            case NONE -> {}
        }
    }

    public void notifyAction(final String title, final String value) {
        if (!showModeAwareActionInfo(title, value)) {
            oled.valueInfo(title, value);
        }
        if (firePreferences != null && firePreferences.screenNotifications()) {
            host.showPopupNotification(title + ": " + value);
        }
    }

    private boolean showModeAwareActionInfo(final String title, final String value) {
        if (modeState.activeMode() == Mode.PERFORM
                && performMode != null
                && performMode.showGlobalActionInfo(title, value)) {
            return true;
        }
        return modeState.activeMode() == Mode.DRUM
                && modeState.activeDrumMode() == DrumMode.STANDARD
                && drumSequenceMode != null
                && drumSequenceMode.showGlobalActionInfo(title, value);
    }

    private void showModeChangeInfo(final String modeLabel) {
        showIdleModeTrackInfo(modeLabel);
        notifyPopup("Mode", modeLabel);
    }

    public void notifyPopup(final String title, final String value) {
        if (firePreferences != null && firePreferences.screenNotifications()) {
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
            modeState.cycleDrumMode();
            switchActiveMode();
            showModeChangeInfo(drumModeLabel(modeState.activeDrumMode()));
        } else {
            modeState.activateDrum();
            switchActiveMode();
            showModeChangeInfo(drumModeLabel(modeState.activeDrumMode()));
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
            showModeChangeInfo(notePlayMode.currentNoteSubModeLabel());
            return;
        }
        if (modeState.activeMode() != Mode.NOTE_PLAY) {
            modeState.activateNotePlay();
            switchActiveMode();
            showModeChangeInfo(notePlayMode.currentNoteSubModeLabel());
            return;
        }
        notePlayMode.cycleNoteSubMode();
        showModeChangeInfo(notePlayMode.currentNoteSubModeLabel());
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
                    enterPlainStepPressTarget();
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
            enterPlainStepPressTarget();
            return;
        }
        if (modeState.activeMode() == Mode.FUGUE_STEP) {
            if (!pressed || isGlobalAltHeld()) {
                return;
            }
            enterPlainStepPressTarget();
            return;
        }
        if (modeState.shouldIgnoreTopLevelStepPress(isGlobalShiftHeld(), isGlobalAltHeld())) {
            return;
        }
        if (!pressed) {
            return;
        }
        enterPlainStepPressTarget();
    }

    public void enterPlainStepPressTarget() {
        switch (modeState.plainStepPressTarget()) {
            case CHORD_STEP -> enterChordStepMode();
            case MELODIC_STEP -> enterMelodicStepMode();
            case FUGUE_STEP -> enterFugueStepMode();
            default -> enterChordStepMode();
        }
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
            rememberActivePerformMode();
            showModeChangeInfo(performMode.activePageLabel());
            return;
        }
        if (isGlobalShiftHeld()) {
            performMode.toggleTrackActionMode();
            rememberActivePerformMode();
            showModeChangeInfo(performMode.activePageLabel());
            return;
        }
        if (isGlobalAltHeld()) {
            performMode.toggleOrientation();
            showModeChangeInfo(performMode.activePageLabel());
            return;
        }
        if (!enteringPerform) {
            if (performMode.isBirdsEyeMode()) {
                performMode.leaveBirdsEyeMode();
                rememberActivePerformMode();
                showModeChangeInfo(performMode.activePageLabel());
                return;
            }
            if (performMode.isTrackActionMode()) {
                performMode.toggleTrackActionMode();
                rememberActivePerformMode();
                showModeChangeInfo(performMode.activePageLabel());
                return;
            }
            performMode.toggleSceneActionMode();
            rememberActivePerformMode();
        }
        showModeChangeInfo(performMode.activePageLabel());
    }

    private void rememberActivePerformMode() {
        modeState.activatePerform(
                performMode.isTrackActionMode() ? PerformMode.MIX : PerformMode.LAUNCHER);
    }

    private boolean leavePerformBirdsEyeIfActive() {
        if (modeState.activeMode() != Mode.PERFORM
                || performMode == null
                || !performMode.isBirdsEyeMode()) {
            return false;
        }
        performMode.leaveBirdsEyeMode();
        return true;
    }

    private void togglePlay(final boolean pressed) {
        if (browserTransportAction(isPopupBrowserActive(), NoteAssign.PLAY, pressed)
                == BrowserTransportAction.COMMIT) {
            popupBrowserController.commit();
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

    public Application getApplication() {
        return application;
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

    public NoteVariationAmounts getNoteVariationAmounts() {
        return noteVariationAmounts;
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

    public void updateRgbPad(final int index, final RgbLightState state) {
        currentPadStates[index] = state;
        sendScaledPadRgb(index, state);
    }

    private void redrawRgbPads() {
        for (int index = 0; index < currentPadStates.length; index++) {
            final RgbLightState state = currentPadStates[index];
            if (state != null) {
                sendScaledPadRgb(index, state);
            }
        }
    }

    private void sendScaledPadRgb(final int index, final RgbLightState state) {
        final int red = state.getRed() & 0xFF;
        final int green = state.getGreen() & 0xFF;
        final int blue = state.getBlue() & 0xFF;
        final double brightness =
                firePreferences == null
                        ? FireControlPreferences.PAD_BRIGHTNESS_DEFAULT
                        : firePreferences.padBrightness();
        final double saturation =
                firePreferences == null
                        ? FireControlPreferences.PAD_SATURATION_DEFAULT
                        : firePreferences.padSaturation();
        sendPadRgb(
                index,
                FireControlPreferences.scalePadColorComponent(
                        red, red, green, blue, brightness, saturation),
                FireControlPreferences.scalePadColorComponent(
                        green, red, green, blue, brightness, saturation),
                FireControlPreferences.scalePadColorComponent(
                        blue, red, green, blue, brightness, saturation));
    }

    public MultiStateHardwareLight[] getStateLights() {
        return stateLights;
    }

    public boolean isExclusiveTrackArmEnabled() {
        return firePreferences != null && firePreferences.exclusiveTrackArm();
    }

    public long getScreenMessageHoldMs() {
        return screenMessageHoldMs;
    }

    public boolean isIdleOledMetersEnabled() {
        return firePreferences != null && firePreferences.idleOledMeters();
    }

    private void applyScreenMessageHoldPreference(final long holdMillis) {
        screenMessageHoldMs = holdMillis;
        oled.setClearDelayMs(screenMessageHoldMs);
    }

    private void applyEncoderLegendPositionPreference(final String preferenceValue) {
        final String normalized =
                FireControlPreferences.normalizeEncoderLegendPosition(preferenceValue);
        oled.setFooterLegendPosition(
                FireControlPreferences.ENCODER_LEGEND_POSITION_TOP.equals(normalized)
                        ? EncoderLegendPosition.TOP
                        : EncoderLegendPosition.BOTTOM);
    }

    private void applyStartupModePreference() {
        final String startupMode =
                firePreferences == null
                        ? FireControlPreferences.STARTUP_MODE_NOTE
                        : firePreferences.startupMode();
        switch (startupMode) {
            case FireControlPreferences.STARTUP_MODE_HARMONY -> modeState.activateChordStep();
            case FireControlPreferences.STARTUP_MODE_DRUM_XOX ->
                    modeState.activateDrum(DrumMode.STANDARD);
            case FireControlPreferences.STARTUP_MODE_MIX ->
                    modeState.activatePerform(PerformMode.MIX);
            case FireControlPreferences.STARTUP_MODE_LAUNCHER ->
                    modeState.activatePerform(PerformMode.LAUNCHER);
            default -> modeState.activateNotePlay();
        }
    }

    private void switchActiveMode() {
        final boolean handOffDrumSelection =
                modeState.activeMode() == Mode.DRUM
                        && modeState.activeDrumMode().takesOverAutoPinnedDrumSelection();
        releaseAutoPinnedDrumContext(!handOffDrumSelection);
        drumSequenceMode.deactivate();
        multiclipSequenceMode.deactivate();
        notePlayMode.deactivate();
        drumPadPlayMode.deactivate();
        chordStepMode.deactivate();
        melodicStepMode.deactivate();
        fugueStepMode.deactivate();
        nestedRhythmMode.deactivate();
        performMode.deactivate();
        if (modeState.activeMode() == Mode.DRUM) {
            applyDrumPinningIfEnabled();
            if (modeState.activeDrumMode() == DrumMode.NESTED_RHYTHM) {
                nestedRhythmMode.activate();
            } else if (modeState.activeDrumMode() == DrumMode.DRUM_PADS) {
                drumPadPlayMode.activate();
            } else if (modeState.activeDrumMode() == DrumMode.MULTICLIP_SEQ) {
                multiclipSequenceMode.activate();
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
        } else {
            performMode.setTrackActionMode(modeState.activePerformMode() == PerformMode.MIX);
            performMode.activate();
        }
        refreshSurfaceLights();
    }

    private void refreshSurfaceLights() {
        flush();
        host.scheduleTask(this::flush, 0);
        host.scheduleTask(this::flush, 8);
    }

    public void toggleFillMode() {
        transport.isFillModeActive().toggle();
        notifyAction("Fill", transport.isFillModeActive().get() ? "On" : "Off");
    }

    public boolean isFillModeActive() {
        return transport != null && transport.isFillModeActive().get();
    }

    public BiColorLightState getFillLightState() {
        return transport.isFillModeActive().get()
                ? BiColorLightState.AMBER_FULL
                : BiColorLightState.AMBER_HALF;
    }

    public BiColorLightState getStepFillLightState() {
        return isFillModeActive() ? BiColorLightState.AMBER_HALF : BiColorLightState.AMBER_FULL;
    }

    public String getClipLaunchModePreference() {
        return firePreferences == null
                ? FireControlPreferences.CLIP_LAUNCH_MODE_SYNCED
                : firePreferences.clipLaunchMode();
    }

    public String getPerformClipLauncherLayoutPreference() {
        return firePreferences == null
                ? FireControlPreferences.PERFORM_LAYOUT_VERTICAL
                : firePreferences.performLayout();
    }

    public int getDefaultClipLengthBeats() {
        return firePreferences == null ? 8 : firePreferences.defaultClipLengthBeats();
    }

    public boolean shouldRoundLauncherRecordingToNearestBar() {
        return firePreferences != null && firePreferences.roundLauncherRecordLength();
    }

    public boolean shouldDisableLauncherPostRecordingAction() {
        return firePreferences != null && firePreferences.manualLauncherRecordLength();
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
        if (shouldDisableLauncherPostRecordingAction()
                || shouldRoundLauncherRecordingToNearestBar()) {
            transport.clipLauncherPostRecordingAction().set("off");
            return;
        }
        transport.clipLauncherPostRecordingAction().set("play_recorded");
        transport.getClipLauncherPostRecordingTimeOffset().set(getLauncherRecordLengthBeats());
    }

    private int getLauncherRecordLengthBeats() {
        return firePreferences == null ? 8 : firePreferences.launcherRecordLengthBeats();
    }

    public String getMainEncoderRolePreference() {
        return globalMainEncoderController.currentRole();
    }

    private void applyMainEncoderStartupPreference(final String preferenceValue) {
        globalMainEncoderController.applyStartupPreference(preferenceValue);
    }

    public String cycleMainEncoderRolePreference() {
        final String effectiveRole = globalMainEncoderController.cycleRole();
        notifyAction("Encoder Role", mainEncoderRoleDisplayName(effectiveRole));
        return effectiveRole;
    }

    public String toggleMainEncoderRolePreference() {
        final String effectiveRole = globalMainEncoderController.toggleRole();
        notifyAction("Encoder Role", mainEncoderRoleDisplayName(effectiveRole));
        return effectiveRole;
    }

    private boolean isDrumGridRoleAvailable() {
        return modeState.activeMode() == Mode.DRUM
                && modeState.activeDrumMode() == DrumMode.STANDARD;
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
        LauncherRetriggerActions.retriggerCurrentClip(
                viewControl == null ? null : viewControl.getSelectedClip());
    }

    public void goToArrangementEndOrLoopEnd() {
        if (transport == null) {
            return;
        }
        if (hasActiveArrangerLoop()) {
            final double loopEnd =
                    Math.max(
                            0.0,
                            transport.arrangerLoopStart().get()
                                    + transport.arrangerLoopDuration().get());
            setPlaybackStartPosition(loopEnd);
            oled.valueInfo("Loop End", transport.playStartPosition().getFormatted());
            return;
        }
        if (arranger != null) {
            arranger.zoomToFit();
        }
        final Action jumpToEnd =
                application == null
                        ? null
                        : application.getAction(ACTION_JUMP_TO_END_OF_ARRANGEMENT);
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
        return transport.isArrangerLoopEnabled().get()
                && transport.arrangerLoopDuration().get() > 0.0;
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
        final double contentPerPixel =
                arranger.getHorizontalScrollbarModel().getContentPerPixel().get();
        return ArrangerGridStep.fromContentPerPixel(
                contentPerPixel,
                transportTimeSignatureNumerator,
                transportTimeSignatureDenominator);
    }

    public boolean isStepSeqPadAuditionEnabled() {
        return firePreferences != null && firePreferences.stepSequencerPadAudition();
    }

    public String getDefaultScalePreference() {
        return firePreferences == null
                ? FireControlPreferences.DEFAULT_SCALE_MAJOR
                : firePreferences.defaultScale();
    }

    public int getDefaultRootKeyPreference() {
        return firePreferences == null ? 0 : firePreferences.defaultRootKey();
    }

    public int getDefaultNoteInputOctavePreference() {
        return firePreferences == null ? 3 : firePreferences.defaultNoteInputOctave();
    }

    public int getDefaultVelocitySensitivityPreference() {
        return firePreferences == null ? 80 : firePreferences.defaultVelocitySensitivity();
    }

    public long initialMelodicSeed() {
        final long randomSeed =
                ThreadLocalRandom.current()
                        .nextLong(
                                FireControlPreferences.MELODIC_FIXED_SEED_MIN,
                                FireControlPreferences.MELODIC_FIXED_SEED_MAX + 1);
        return firePreferences == null
                ? randomSeed
                : firePreferences.initialMelodicSeed(() -> randomSeed);
    }

    public void exitMelodicStepMode() {
        final Mode activeMode = modeState.exitMelodicStepMode();
        switchActiveMode();
        showModeChangeInfo(
                switch (activeMode) {
                    case DRUM -> drumModeLabel(modeState.activeDrumMode());
                    case CHORD_STEP -> "Poly Step";
                    case PERFORM -> "Perform";
                    default -> "Note";
                });
    }

    public void enterMelodicStepMode() {
        suppressNextMelodicStepRelease = true;
        modeState.enterMelodicStepMode();
        switchActiveMode();
        showModeChangeInfo("Melo Gen");
    }

    public void enterChordStepMode() {
        modeState.activateChordStep();
        switchActiveMode();
        showModeChangeInfo("Poly Step");
    }

    public void enterFugueStepMode() {
        modeState.activateFugueStep();
        switchActiveMode();
        showModeChangeInfo("Fugue");
    }

    public boolean isEuclidFullClipEnabled() {
        return firePreferences != null && firePreferences.euclidFullClip();
    }

    public boolean shouldRetuneHeldLiveNotes() {
        return false;
    }

    public boolean shouldAutoPinFirstDrumMachine() {
        return firePreferences != null && firePreferences.autoPinFirstDrumMachine();
    }

    private boolean shouldAutoPinDrumContext() {
        return modeState.activeMode() == Mode.DRUM
                && modeState.activeDrumMode().usesAutoPinnedDrumContext()
                && shouldAutoPinFirstDrumMachine();
    }

    private void syncDrumPinningForActiveMode() {
        drumAutoPinController.sync();
    }

    private void applyDrumPinningIfEnabled() {
        drumAutoPinController.applyIfEnabled();
    }

    private void ensureDrumPinningStillValid() {
        drumAutoPinController.validate();
    }

    private void releaseAutoPinnedDrumContext(final boolean restorePreviousState) {
        drumAutoPinController.release(restorePreviousState);
    }

    private void initDrumAutoPinController() {
        drumAutoPinController =
                new DrumAutoPinController(
                        this::shouldAutoPinDrumContext,
                        new DrumAutoPinController.Port() {
                            @Override
                            public boolean isTrackPinned() {
                                return viewControl.getCursorTrack().isPinned().get();
                            }

                            @Override
                            public boolean isDevicePinned() {
                                return viewControl.getPrimaryDevice().isPinned().get();
                            }

                            @Override
                            public int selectedTrackIndex() {
                                return viewControl.getCursorTrack().position().get();
                            }

                            @Override
                            public boolean focusFirstDrumMachine() {
                                return deviceLocator.focusFirstDrumMachine(viewControl);
                            }

                            @Override
                            public void setTrackPinned(final boolean pinned) {
                                viewControl.getCursorTrack().isPinned().set(pinned);
                            }

                            @Override
                            public void setDevicePinned(final boolean pinned) {
                                viewControl.getPrimaryDevice().isPinned().set(pinned);
                            }

                            @Override
                            public boolean focusedDeviceExists() {
                                return viewControl.getPrimaryDevice().exists().get();
                            }

                            @Override
                            public boolean focusedDeviceHasDrumPads() {
                                return viewControl.getPrimaryDevice().hasDrumPads().get();
                            }

                            @Override
                            public void restoreTrackSelection(final int trackIndex) {
                                AkaiFireOikontrolExtension.this.restoreTrackSelection(trackIndex);
                            }
                        });
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
        if (showActiveModeIdleOledInfo()) {
            return;
        }
        if (!isTransportPlaying() && !shouldShowMeterIdleDisplay()) {
            showIdleModeTrackInfo(currentModeLabel());
            return;
        }
        final Parameter parameter = getLastClickedParameter();
        if (parameter != null) {
            oled.valueInfo(parameter.name().get(), parameter.displayedValue().get());
            return;
        }
        showIdleModeTrackInfo(currentModeLabel());
    }

    private String currentModeLabel() {
        return switch (modeState.activeMode()) {
            case DRUM -> drumModeLabel(modeState.activeDrumMode());
            case NOTE_PLAY ->
                    notePlayMode == null ? "Note" : notePlayMode.currentNoteSubModeLabel();
            case CHORD_STEP -> "Poly Step";
            case MELODIC_STEP -> "Melo Gen";
            case FUGUE_STEP -> "Fugue";
            case PERFORM -> performMode == null ? "Perform" : performMode.activePageLabel();
        };
    }

    private boolean showActiveModeIdleOledInfo() {
        if (modeState.activeMode() == Mode.PERFORM
                && performMode != null
                && performMode.showIdleInfoIfNeeded()) {
            return true;
        }
        if (modeState.activeMode() == Mode.NOTE_PLAY
                && notePlayMode != null
                && notePlayMode.showIdleInfoIfNeeded()) {
            return true;
        }
        if (modeState.activeMode() == Mode.DRUM
                && modeState.activeDrumMode() == DrumMode.MULTICLIP_SEQ
                && multiclipSequenceMode != null
                && multiclipSequenceMode.showIdleInfoIfNeeded()) {
            return true;
        }
        if (modeState.activeMode() == Mode.DRUM
                && modeState.activeDrumMode() == DrumMode.STANDARD
                && drumSequenceMode != null
                && drumSequenceMode.showIdleInfoIfNeeded()) {
            return true;
        }
        return false;
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
        final PinnableCursorDevice targetDevice =
                selectedDevice != null && selectedDevice.exists().get()
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

    private void prepareGlobalSettingsOverlayActivation() {
        releaseAutoPinnedDrumContext(true);
        drumSequenceMode.deactivate();
        notePlayMode.deactivate();
        drumPadPlayMode.deactivate();
        chordStepMode.deactivate();
        melodicStepMode.deactivate();
        fugueStepMode.deactivate();
        nestedRhythmMode.deactivate();
        performMode.deactivate();
    }

    private boolean dismissGlobalSettingsOverlayForModeButton(final Mode targetMode) {
        return globalSettingsOverlay != null
                && globalSettingsOverlay.dismissForModeButton(targetMode);
    }

    private boolean dismissGlobalSettingsOverlayForStepButton() {
        return globalSettingsOverlay != null && globalSettingsOverlay.dismissForStepButton();
    }

    private void updateGlobalSettingsOverlayState() {
        if (globalSettingsOverlay != null) {
            globalSettingsOverlay.refreshState();
        }
    }

    public boolean isPopupBrowserActive() {
        return popupBrowserController != null && popupBrowserController.isActive();
    }

    public BiColorLightState getBrowserLightState() {
        if (isGlobalSettingsOverlayActive()) {
            return BiColorLightState.AMBER_FULL;
        }
        return isPopupBrowserActive() ? BiColorLightState.RED_FULL : BiColorLightState.OFF;
    }

    public boolean isGlobalSettingsOverlayActive() {
        return globalSettingsOverlay != null && globalSettingsOverlay.isActive();
    }

    public boolean showDeactivatedTracks() {
        return firePreferences != null
                ? firePreferences.showDeactivatedTracks()
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
        globalMainEncoderController.setPressed(pressed);
    }

    public boolean isMainEncoderPressed() {
        return globalMainEncoderController.isPressed();
    }

    public void markMainEncoderTurned() {
        globalMainEncoderController.markTurned();
    }

    public boolean wasMainEncoderTurnedWhilePressed() {
        return globalMainEncoderController.wasTurnedWhilePressed();
    }

    public boolean handleMainEncoderGlobalChord(final int inc) {
        return globalMainEncoderController.handleGlobalChord(
                inc,
                isPopupBrowserActive(),
                patternPressed,
                isGlobalShiftHeld(),
                isGlobalAltHeld(),
                globalMainEncoderChordActions);
    }

    public boolean routeGlobalMainEncoderRole(final int inc, final boolean fine) {
        return globalMainEncoderController.routeRoleTurn(inc, fine, globalMainEncoderRoleActions);
    }

    private void jumpToCueMarker(final int inc) {
        if (inc == 0 || transport == null) {
            return;
        }
        final double reference =
                Double.isNaN(cueMarkerNavigationPosition)
                        ? transport.playStartPosition().get()
                        : cueMarkerNavigationPosition;
        final int markerIndex =
                cueMarkerIndexAfterTurn(
                        reference,
                        inc,
                        cueMarkerExists,
                        cueMarkerPositions,
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

    static int cueMarkerIndexAfterTurn(
            final double reference,
            final int inc,
            final boolean[] exists,
            final double[] positions,
            final int itemCount) {
        return GlobalMainEncoderController.cueMarkerIndexAfterTurn(
                reference, inc, exists, positions, itemCount);
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
        if (shouldAutoPinDrumContext()) {
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

    private int nextSelectableTrackIndex(
            final TrackBank trackBank, final int currentIndex, final int delta) {
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

    private void showIdleModeTrackInfo(final String modeLabel) {
        if (viewControl == null) {
            oled.valueInfo("Mode", modeLabel);
            return;
        }
        lastStoppedIdleTrackRefreshMs = System.currentTimeMillis();
        oled.clearScreen();
        oled.valueInfoPersistentNoClear(
                modeLabel, normalizedTrackName(viewControl.getCursorTrack().name().get()));
    }

    private void showTrackInfo(final String title, final String trackName) {
        oled.valueInfo(title, normalizedTrackName(trackName));
    }

    private String normalizedTrackName(final String trackName) {
        return trackName == null || trackName.isBlank() ? "Unnamed" : trackName;
    }

    private void handleBrowserPressed(final boolean pressed) {
        popupBrowserController.handleBrowserPressed(pressed);
    }

    private void handleGlobalMainEncoder(final int inc) {
        popupBrowserController.adjustSelection(inc);
    }

    public void routeBrowserMainEncoder(final int inc) {
        popupBrowserController.adjustSelection(inc);
    }

    private void handleGlobalMainEncoderPress(final boolean pressed) {
        popupBrowserController.handleMainEncoderPress(pressed);
    }

    public void routeBrowserMainEncoderPress(final boolean pressed) {
        popupBrowserController.handleMainEncoderPress(pressed);
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
            return true;
        }
        showRemotePageNavigation(target, direction);
        return true;
    }

    public boolean handleKnobModeEncoderReset(
            final boolean touched,
            final boolean resettable,
            final String fallbackLabel,
            final String unavailableDetail,
            final Runnable resetAction,
            final Runnable showAction) {
        final boolean handled =
                ParameterEncoderBinding.handleExplicitResetTouch(
                        touched,
                        knobModeEncoderResetControl(),
                        resettable,
                        fallbackLabel,
                        unavailableDetail,
                        resetAction,
                        showAction,
                        oled::valueInfo);
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
        return remotePageNavigationLightState(
                page.selectedPageIndex().get(), page.pageCount().getAsInt(), direction);
    }

    private RemotePageTarget currentRemotePageTarget() {
        return switch (modeState.activeMode()) {
            case NOTE_PLAY -> notePlayMode == null ? null : notePlayMode.currentRemotePageTarget();
            case DRUM -> currentDrumRemotePageTarget();
            case PERFORM -> performMode == null ? null : performMode.currentRemotePageTarget();
            case CHORD_STEP, MELODIC_STEP, FUGUE_STEP -> null;
        };
    }

    private RemotePageTarget currentDrumRemotePageTarget() {
        if (modeState.activeDrumMode() == DrumMode.DRUM_PADS && drumPadPlayMode != null) {
            return drumPadPlayMode.currentRemotePageTarget();
        }
        if (modeState.activeDrumMode() == DrumMode.MULTICLIP_SEQ && multiclipSequenceMode != null) {
            final CursorRemoteControlsPage page =
                    multiclipSequenceMode.getActiveRemoteControlsPage();
            return page == null ? null : new RemotePageTarget(page, "Lane Pad");
        }
        if (modeState.activeDrumMode() != DrumMode.STANDARD || drumSequenceMode == null) {
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
            oled.sendString(
                    0,
                    OledDisplay.TextJustification.RIGHT,
                    7,
                    remotePageCountLabel(currentPage, pageCount));
            oled.clearScreenDelayed();
            return;
        }
        page.selectedPageIndex().set(nextPage);
        oled.valueInfo(title, remotePageName(page, nextPage));
        oled.sendString(
                0,
                OledDisplay.TextJustification.RIGHT,
                7,
                remotePageCountLabel(nextPage, pageCount));
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

    static int remotePageIndexAfterTurn(
            final int currentPage, final int pageCount, final int direction) {
        return GlobalMainEncoderController.remotePageIndexAfterTurn(
                currentPage, pageCount, direction);
    }

    static String remotePageCountLabel(final int pageIndex, final int pageCount) {
        return GlobalMainEncoderController.remotePageCountLabel(pageIndex, pageCount);
    }

    static BiColorLightState remotePageNavigationLightState(
            final int currentPage, final int pageCount, final int direction) {
        return GlobalMainEncoderController.remotePageNavigationLightState(
                currentPage, pageCount, direction);
    }

    static BrowserTransportAction browserTransportAction(
            final boolean browserActive, final NoteAssign assignment, final boolean pressed) {
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

    static PlayPressAction playPressAction(
            final boolean playing, final boolean retriggerLaunchersArmed) {
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

    static PatternReleaseAction patternReleaseAction(
            final boolean gestureConsumed, final boolean shiftHeld, final boolean altHeld) {
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

    public PatternButtons getPatternButtons() {
        return patternButtons;
    }
}
