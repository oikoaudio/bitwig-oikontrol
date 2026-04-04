package com.oikoaudio.fire;

import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.note.NoteMode;
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

public class AkaiFireDrumSeqExtension extends ControllerExtension {
    private static final double MAIN_ENCODER_STEP = 0.01;
    private static final double MAIN_ENCODER_FINE_STEP = 0.0025;
    public static final String MAIN_ENCODER_LAST_TOUCHED_ROLE = FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED;
    public static final String MAIN_ENCODER_SHUFFLE_ROLE = FireControlPreferences.MAIN_ENCODER_SHUFFLE;
    public static final String MAIN_ENCODER_NOTE_REPEAT_ROLE = FireControlPreferences.MAIN_ENCODER_NOTE_REPEAT;

    private static AkaiFireDrumSeqExtension instance;
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
    private SettableEnumValue patternActionPref;
    private SettableEnumValue mainEncoderRolePref;
    private SettableEnumValue euclidScopePref;
    private SettableBooleanValue auditionOnDrumSelectPref;
    private SettableBooleanValue auditionOikordsPref;

    private PatternButtons patternButtons;
    private NoteMode noteMode;
    private Layer performModeLayer;
    private NoteRepeatHandler noteRepeatHandler;
    private TopLevelMode activeMode = TopLevelMode.NOTE;
    private DrumSubMode activeDrumSubMode = DrumSubMode.STANDARD;

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

    protected AkaiFireDrumSeqExtension(final AkaiFireDrumSeqDefinition definition, final ControllerHost host) {
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
        performModeLayer = new Layer(layers, "PERFORM_MODE_LAYER");
        midiIn = host.getMidiInPort(0);
        midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
        midiIn.setSysexCallback(this::onSysEx);
        midiOut = host.getMidiOutPort(0);
        transport = host.createTransport();
        surface = host.createHardwareSurface();
        noteInput = midiIn.createNoteInput("MIDI", "80????", "90????", "A0????", "D0????");
        noteInput.setShouldConsumeEvents(false);
        viewControl = new ViewCursorControl(host, 16);

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
        oled.setIdleAction(this::showIdleOledInfo);
        midiOut.sendSysex(DEV_INQ);

        oled.showLogo();
        mainLayer.activate();
        switchActiveMode();
        host.scheduleTask(this::handlePing, 100);
        getHost().showPopupNotification("Init Akai Fire: Note Mode");

    }

    private void handlePing() {
        // sections.forEach(section -> section.notifyBlink(blinkTicks));
        blinkTicks++;
        oled.notifyBlink(blinkTicks);
        drumSequenceMode.notifyBlink(blinkTicks);
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

        patternActionPref = preferences.getEnumSetting("Pattern Button",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.PATTERN_ACTIONS,
                FireControlPreferences.PATTERN_ACTION_AUTOMATION_WRITE);
        patternActionPref.markInterested();

        mainEncoderRolePref = preferences.getEnumSetting("Main Encoder",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.MAIN_ENCODER_ROLES,
                FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED);
        mainEncoderRolePref.markInterested();

        euclidScopePref = preferences.getEnumSetting("Euclid Scope",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.EUCLID_SCOPES,
                FireControlPreferences.EUCLID_SCOPE_FULL_CLIP);
        euclidScopePref.markInterested();

        auditionOnDrumSelectPref = preferences.getBooleanSetting("Audition on drum slot select",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                true);
        auditionOnDrumSelectPref.markInterested();

        auditionOikordsPref = preferences.getBooleanSetting("Audition Oikords",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                true);
        auditionOikordsPref.markInterested();
    }

    private void setUpTransportControl() {
        transport.isPlaying().markInterested();
        transport.tempo().markInterested();
        transport.playPosition().markInterested();
        transport.isClipLauncherOverdubEnabled().markInterested();
        transport.isMetronomeEnabled().markInterested();
        transport.isClipLauncherAutomationWriteEnabled().markInterested();
        transport.isFillModeActive().markInterested();
        transport.defaultLaunchQuantization().markInterested();
        final BiColorButton playButton = addButton(NoteAssign.PLAY);
        playButton.bindPressed(mainLayer, this::togglePlay, this::getPlayState);
        final BiColorButton recButton = addButton(NoteAssign.REC);
        recButton.bindPressed(mainLayer, this::toggleRec, this::getOverdubState);
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

    private BiColorLightState getOverdubState() {
        return transport.isClipLauncherOverdubEnabled().get() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF;
    }

    private BiColorLightState getClipLauncherAutomationWriteEnabledState() {
        return transport.isClipLauncherAutomationWriteEnabled().get() ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF;
    }

    private BiColorLightState getPatternState() {
        if (patternActionPref == null) {
            return BiColorLightState.OFF;
        }
        if (getButton(NoteAssign.SHIFT).isPressed()) {
            return transport.isMetronomeEnabled().get() ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
        }
        if (FireControlPreferences.PATTERN_ACTION_AUTOMATION_WRITE.equals(patternActionPref.get())) {
            return getClipLauncherAutomationWriteEnabledState();
        }
        return BiColorLightState.OFF;
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
        transport.isClipLauncherOverdubEnabled().toggle();
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
            oled.valueInfo("Metronome", transport.isMetronomeEnabled().get() ? "On" : "Off");
            return;
        }
        if (FireControlPreferences.PATTERN_ACTION_AUTOMATION_WRITE.equals(patternActionPref.get())) {
            toggleClipLauncherAutomationWriteEnabled(true);
            oled.valueInfo("Pattern", transport.isClipLauncherAutomationWriteEnabled().get() ? "Automation Write On" : "Automation Write Off");
            return;
        }
        oled.valueInfo("Pattern", "Disabled");
    }

    private void handleDrumPressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (activeMode == TopLevelMode.DRUM) {
            activeDrumSubMode = activeDrumSubMode.next();
        } else {
            activeMode = TopLevelMode.DRUM;
            switchActiveMode();
        }
        oled.valueInfo("Mode", "Drum");
    }

    private void handleNotePressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (activeMode == TopLevelMode.NOTE) {
            if (noteMode.isNoteStepActive()) {
                noteMode.returnToLivePlayFromStepMode();
                oled.valueInfo("Mode", "Note");
                return;
            }
            noteMode.cycleLayout();
            return;
        }
        activeMode = TopLevelMode.NOTE;
        switchActiveMode();
        oled.valueInfo("Mode", "Note");
    }

    private void handlePerformPressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        activeMode = TopLevelMode.PERFORM;
        switchActiveMode();
        oled.valueInfo("Mode", "Perform");
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
        } else {
            drumSequenceMode.retrigger();
            transport.restart();
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
        getHost().showPopupNotification("Exit Akai Fire Drum Seq");
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
        drumSequenceMode.deactivate();
        noteMode.deactivate();
        performModeLayer.deactivate();
        if (activeMode == TopLevelMode.DRUM) {
            drumSequenceMode.activate();
            return;
        }
        clearPads();
        if (activeMode == TopLevelMode.NOTE) {
            noteMode.activate();
        } else {
            performModeLayer.activate();
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
        return mainEncoderRolePref == null
                ? FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED
                : FireControlPreferences.normalizeMainEncoderRole(mainEncoderRolePref.get());
    }

    public String cycleMainEncoderRolePreference() {
        final String nextRole = FireControlPreferences.nextMainEncoderRole(getMainEncoderRolePreference());
        if (mainEncoderRolePref != null) {
            mainEncoderRolePref.set(nextRole);
        }
        oled.valueInfo("Encoder Role", nextRole);
        return nextRole;
    }

    public boolean isAuditionOnDrumSelectEnabled() {
        return auditionOnDrumSelectPref != null && auditionOnDrumSelectPref.get();
    }

    public boolean isAuditionOikordsEnabled() {
        return auditionOikordsPref != null && auditionOikordsPref.get();
    }

    public boolean isEuclidFullClipEnabled() {
        return euclidScopePref != null
                && FireControlPreferences.EUCLID_SCOPE_FULL_CLIP.equals(
                FireControlPreferences.normalizeEuclidScope(euclidScopePref.get()));
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

    public void toggleGrooveEnabled() {
        if (groove == null) {
            return;
        }
        final SettableRangedValue value = groove.getEnabled().value();
        final boolean enableGroove = value.get() < 0.5;
        value.setImmediately(enableGroove ? 1.0 : 0.0);
        oled.valueInfo("Shuffle", enableGroove ? "On" : "Off");
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
            oled.valueInfo("Browser", "Closed");
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
            oled.valueInfo("Browser", "Before");
            return;
        }
        if (altHeld) {
            if (primaryDevice.exists().get()) {
                primaryDevice.afterDeviceInsertionPoint().browse();
            } else {
                viewControl.getCursorTrack().endOfDeviceChainInsertionPoint().browse();
            }
            oled.valueInfo("Browser", "After");
            return;
        }
        if (primaryDevice.exists().get()) {
            primaryDevice.replaceDeviceInsertionPoint().browse();
            oled.valueInfo("Browser", "Replace");
        } else {
            viewControl.getCursorTrack().endOfDeviceChainInsertionPoint().browse();
            oled.valueInfo("Browser", "Add");
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
            oled.valueInfo("Browser", "Commit");
        }
    }

    public static AkaiFireDrumSeqExtension getInstance() {
        return instance;
    }

    public static ControllerHost getGlobalHost() {
        return instance.getHost();
    }

    public PatternButtons getPatternButtons() {
        return patternButtons;
    }
}
