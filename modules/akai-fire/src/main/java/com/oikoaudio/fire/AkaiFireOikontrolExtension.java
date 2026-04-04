package com.oikoaudio.fire;

import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.note.NoteMode;
import com.oikoaudio.fire.perform.PerformClipLauncherMode;
import com.oikoaudio.fire.sequence.DrumSequenceMode;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;
import com.oikoaudio.fire.utils.PatternButtons;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.Midi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AkaiFireOikontrolExtension extends ControllerExtension {
    private static final double MAIN_ENCODER_STEP = 0.01;
    private static final double MAIN_ENCODER_FINE_STEP = 0.0025;
    private static final int DEVICE_DISCOVERY_WIDTH = 128;
    public static final String MAIN_ENCODER_LAST_TOUCHED_ROLE = FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED;
    public static final String MAIN_ENCODER_SHUFFLE_ROLE = FireControlPreferences.MAIN_ENCODER_SHUFFLE;
    public static final String MAIN_ENCODER_TEMPO_ROLE = FireControlPreferences.MAIN_ENCODER_TEMPO;
    public static final String MAIN_ENCODER_NOTE_REPEAT_ROLE = FireControlPreferences.MAIN_ENCODER_NOTE_REPEAT;

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
    private SettableEnumValue euclidScopePref;
    private SettableEnumValue drumPinModePref;
    private SettableEnumValue livePitchOffsetBehaviorPref;
    private SettableBooleanValue stepSeqPadAuditionPref;
    private SettableBooleanValue screenNotificationsPref;
    private String tempoDisplayValue = "";
    private boolean tempoDisplayPending = false;
    private boolean drumAutoPinApplied = false;
    private boolean drumTrackPinnedBeforeAutoPin = false;
    private boolean drumDevicePinnedBeforeAutoPin = false;

    private PatternButtons patternButtons;
    private NoteMode noteMode;
    private PerformClipLauncherMode performMode;
    private NoteRepeatHandler noteRepeatHandler;
    private TopLevelMode activeMode = TopLevelMode.NOTE;
    private DrumSubMode activeDrumSubMode = DrumSubMode.STANDARD;
    private String currentMainEncoderRole = FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED;

    private enum TopLevelMode {
        DRUM,
        NOTE,
        PERFORM
    }

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
                () -> drumSequenceMode != null ? drumSequenceMode.getAccentHandler().getCurrenVel() : 100);


        setUpHardware();
        setUpTransportControl();
        setUpPreferences();

        patternButtons = new PatternButtons(this, mainLayer);
        drumSequenceMode = new DrumSequenceMode(this, noteRepeatHandler);
        noteMode = new NoteMode(this, noteRepeatHandler);
        performMode = new PerformClipLauncherMode(this);
        oled.setIdleAction(this::showIdleOledInfo);
        midiOut.sendSysex(DEV_INQ);

        oled.showLogo();
        mainLayer.activate();
        switchActiveMode();
        host.scheduleTask(this::handlePing, 100);
        notifyAction("Init", "Note Mode");

    }

    private void handlePing() {
        // sections.forEach(section -> section.notifyBlink(blinkTicks));
        blinkTicks++;
        ensureDrumPinningStillValid();
        oled.notifyBlink(blinkTicks);
        drumSequenceMode.notifyBlink(blinkTicks);
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

        euclidScopePref = preferences.getEnumSetting("Euclid Scope",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.EUCLID_SCOPES,
                FireControlPreferences.EUCLID_SCOPE_FULL_CLIP);
        euclidScopePref.markInterested();

        drumPinModePref = preferences.getEnumSetting("Drum Mode Pinning",
                FireControlPreferences.CATEGORY_PINNING,
                FireControlPreferences.DRUM_PIN_MODES,
                FireControlPreferences.DRUM_PIN_MODE_FOLLOW_SELECTION);
        drumPinModePref.markInterested();
        drumPinModePref.addValueObserver(value -> {
            syncDrumPinningForActiveMode();
            notifyAction("Drum Pin",
                    FireControlPreferences.shouldAutoPinFirstDrumMachine(value) ? "Automatic" : "Follow Selected");
        });

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
        addButton(NoteAssign.STEP_SEQ);
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
        if (activeMode == TopLevelMode.DRUM) {
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
            return transport.isClipLauncherOverdubEnabled().get() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
        }
        return getClipLauncherAutomationWriteEnabledState();
    }

    private BiColorLightState getDrumState() {
        return activeMode == TopLevelMode.DRUM ? activeDrumSubMode.getLightState() : BiColorLightState.OFF;
    }

    private BiColorLightState getNoteState() {
        if (activeMode != TopLevelMode.NOTE || noteMode == null) {
            return BiColorLightState.OFF;
        }
        return noteMode.getModeButtonLightState();
    }

    private BiColorLightState getPerformState() {
        return activeMode == TopLevelMode.PERFORM ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF;
    }

    private void dummyAction(final boolean pressed) {
    }

    private void stopAction(final boolean pressed) {
        if (!pressed) {
            return;
        }
        transport.stop();
    }

    private void toggleRec(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (isGlobalAltHeld()) {
            transport.isArrangerAutomationWriteEnabled().toggle();
            notifyAction("Arranger Write", transport.isArrangerAutomationWriteEnabled().get() ? "On" : "Off");
            return;
        }
        if (activeMode == TopLevelMode.DRUM) {
            transport.isClipLauncherOverdubEnabled().toggle();
            notifyAction("Clip Record", transport.isClipLauncherOverdubEnabled().get() ? "On" : "Off");
            return;
        }
        transport.isArrangerRecordEnabled().toggle();
        notifyAction("Record", transport.isArrangerRecordEnabled().get() ? "On" : "Off");
    }

    private void toggleClipLauncherAutomationWriteEnabled(final boolean pressed) {
        if (!pressed) {
            return;
        }
        transport.isClipLauncherAutomationWriteEnabled().toggle();
    }

    private void handlePatternPressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (getButton(NoteAssign.SHIFT).isPressed()) {
            transport.isMetronomeEnabled().toggle();
            notifyAction("Metronome", transport.isMetronomeEnabled().get() ? "On" : "Off");
            return;
        }
        if (isGlobalAltHeld()) {
            transport.isClipLauncherOverdubEnabled().toggle();
            notifyAction("Launcher Overdub", transport.isClipLauncherOverdubEnabled().get() ? "On" : "Off");
            return;
        }
        toggleClipLauncherAutomationWriteEnabled(true);
        notifyAction("Clip Write", transport.isClipLauncherAutomationWriteEnabled().get() ? "On" : "Off");
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
            if (shouldAutoPinFirstDrumMachine()) {
                return;
            }
            if (activeMode != TopLevelMode.DRUM) {
                activeMode = TopLevelMode.DRUM;
                switchActiveMode();
                notifyAction("Mode", "Drum");
            }
            pinDrumContextNow();
            return;
        }
        if (isGlobalShiftHeld()) {
            transport.tapTempo();
            tempoDisplayPending = true;
            return;
        }
        if (activeMode == TopLevelMode.DRUM) {
            activeDrumSubMode = activeDrumSubMode.next();
        } else {
            activeMode = TopLevelMode.DRUM;
            switchActiveMode();
            notifyAction("Mode", "Drum");
        }
    }

    private void handleNotePressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (activeMode == TopLevelMode.NOTE) {
            if (isGlobalAltHeld()) {
                noteMode.toggleCurrentSurfaceVariant();
            } else {
                noteMode.togglePrimarySurface();
            }
            return;
        }
        activeMode = TopLevelMode.NOTE;
        switchActiveMode();
        notifyAction("Mode", "Note");
    }

    private void handlePerformPressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (activeMode == TopLevelMode.PERFORM) {
            return;
        }
        activeMode = TopLevelMode.PERFORM;
        switchActiveMode();
        notifyAction("Mode", "Perform");
    }

    private void togglePlay(final boolean pressed) {
        if (!pressed) {
            return;
        }
        // Alt+Play: retrigger the current clip without transport state change.
        if (drumSequenceMode.isAltHeld()) {
            drumSequenceMode.retrigger();
            return;
        }
        // Regular behavior: toggle play/stop, retrigger on start.
        if (transport.isPlaying().get()) {
            transport.isPlaying().set(false);
            notifyAction("Transport", "Stop");
        } else {
            drumSequenceMode.retrigger();
            transport.restart();
            notifyAction("Transport", "Play");
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
        sendPadRgb(index, state.getRed(), state.getGreen(), state.getBlue());
    }

    public MultiStateHardwareLight[] getStateLights() {
        return stateLights;
    }

    private void switchActiveMode() {
        releaseAutoPinnedDrumContext(true);
        drumSequenceMode.deactivate();
        noteMode.deactivate();
        performMode.deactivate();
        if (activeMode == TopLevelMode.DRUM) {
            applyDrumPinningIfEnabled();
            drumSequenceMode.activate();
            return;
        }
        clearPads();
        if (activeMode == TopLevelMode.NOTE) {
            noteMode.activate();
        } else {
            performMode.activate();
        }
    }

    private void clearPads() {
        for (int index = 0; index < rgbButtons.length; index++) {
            updateRgbPad(index, RgbLigthState.OFF);
        }
    }

    private void applyLaunchQuantizationPreference(final String preferenceValue) {
        transport.defaultLaunchQuantization().set(FireControlPreferences.toLaunchQuantizationValue(preferenceValue));
    }

    public void toggleFillMode() {
        transport.isFillModeActive().toggle();
        notifyAction("Fill", transport.isFillModeActive().get() ? "On" : "Off");
    }

    public BiColorLightState getFillLightState() {
        return transport.isFillModeActive().get() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
    }

    public String getClipLaunchModePreference() {
        return clipLaunchModePref == null
                ? FireControlPreferences.CLIP_LAUNCH_MODE_SYNCED
                : clipLaunchModePref.get();
    }

    public String getMainEncoderRolePreference() {
        return FireControlPreferences.normalizeMainEncoderRole(currentMainEncoderRole);
    }

    public String cycleMainEncoderRolePreference() {
        final String nextRole = FireControlPreferences.nextMainEncoderRole(getMainEncoderRolePreference());
        currentMainEncoderRole = nextRole;
        notifyAction("Encoder Role", nextRole);
        return nextRole;
    }

    public boolean isStepSeqPadAuditionEnabled() {
        return stepSeqPadAuditionPref != null && stepSeqPadAuditionPref.get();
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
        if (activeMode == TopLevelMode.DRUM) {
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

        if (!deviceLocator.focusFirstDrumMachine(viewControl)) {
            return;
        }

        cursorTrack.isPinned().set(true);
        primaryDevice.isPinned().set(true);
        drumAutoPinApplied = true;
    }

    private void pinDrumContextNow() {
        if (viewControl == null || deviceLocator == null) {
            return;
        }

        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final PinnableCursorDevice primaryDevice = viewControl.getPrimaryDevice();
        if (!primaryDevice.exists().get() || !primaryDevice.hasDrumPads().get()) {
            notifyAction("Drum Pin", "No Selected Drum Machine");
            return;
        }

        cursorTrack.isPinned().set(true);
        primaryDevice.isPinned().set(true);
        notifyAction("Drum Pin", "Pinned");
    }

    private void ensureDrumPinningStillValid() {
        if (activeMode != TopLevelMode.DRUM || !shouldAutoPinFirstDrumMachine() || !drumAutoPinApplied
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
        } else {
            viewControl.getCursorTrack().isPinned().set(false);
            viewControl.getPrimaryDevice().isPinned().set(false);
        }
        drumAutoPinApplied = false;
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
        final String modeLabel = switch (activeMode) {
            case DRUM -> "Drum";
            case NOTE -> "Note";
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
        final double stepSize = fine ? MAIN_ENCODER_FINE_STEP : MAIN_ENCODER_STEP;
        final double nextValue = Math.max(0.0, Math.min(1.0, value.get() + (inc * stepSize)));
        value.setImmediately(nextValue);
        oled.valueInfo("Shuffle", shuffleAmount.displayedValue().get());
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

    public void toggleGrooveEnabled() {
        if (groove == null) {
            return;
        }
        final SettableRangedValue value = groove.getEnabled().value();
        final boolean enableGroove = value.get() < 0.5;
        value.setImmediately(enableGroove ? 1.0 : 0.0);
        notifyAction("Shuffle", enableGroove ? "On" : "Off");
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
            notifyAction("Browser", "Before");
            return;
        }
        if (altHeld) {
            if (primaryDevice.exists().get()) {
                primaryDevice.afterDeviceInsertionPoint().browse();
            } else {
                viewControl.getCursorTrack().endOfDeviceChainInsertionPoint().browse();
            }
            notifyAction("Browser", "After");
            return;
        }
        if (primaryDevice.exists().get()) {
            primaryDevice.replaceDeviceInsertionPoint().browse();
            notifyAction("Browser", "Replace");
        } else {
            viewControl.getCursorTrack().endOfDeviceChainInsertionPoint().browse();
            notifyAction("Browser", "Add");
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

    private void handleGlobalMainEncoderPress(final boolean press) {
        if (!isPopupBrowserActive()) {
            return;
        }
        if (press) {
            popupBrowser.commit();
            notifyAction("Browser", "Commit");
        }
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
