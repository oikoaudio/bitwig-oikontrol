package com.oikoaudio.fire.perform;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.SharedMusicalContext;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.EncoderTouchResetHandler;
import com.oikoaudio.fire.control.MixerEncoderProfile;
import com.oikoaudio.fire.control.PadBankRowControlBindings;
import com.oikoaudio.fire.control.ParameterEncoderBinding;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.control.TouchResetGesture;
import com.oikoaudio.fire.display.OledMeterRenderer;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.display.VuMeterFormatter;
import com.oikoaudio.fire.display.VuMeterPeakHold;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.EncoderMode;

import java.util.HashMap;
import java.util.Map;

public class PerformClipLauncherMode extends Layer {
    private enum TrackActionRow {
        SELECT(0, "Select", new RgbLigthState(0, 96, 96, true)),
        SOLO(1, "Solo", new RgbLigthState(96, 96, 0, true)),
        MUTE(2, "Mute", new RgbLigthState(110, 48, 0, true)),
        ARM(3, "Arm", new RgbLigthState(110, 0, 0, true));

        private final int rowIndex;
        private final String label;
        private final RgbLigthState color;

        TrackActionRow(final int rowIndex, final String label, final RgbLigthState color) {
            this.rowIndex = rowIndex;
            this.label = label;
            this.color = color;
        }

        static TrackActionRow fromPadIndex(final int padIndex) {
            final int row = padIndex / PerformLayout.PAD_COLUMNS;
            for (final TrackActionRow actionRow : values()) {
                if (actionRow.rowIndex == row) {
                    return actionRow;
                }
            }
            return null;
        }
    }

    /**
     * Carries the three track coordinate systems used by the perform grid.
     * <p>
     * Visible indices are pad columns/rows after hiding deactivated tracks. Source indices are the
     * raw slots in Bitwig's track bank. Absolute indices are source indices plus the bank scroll
     * offset, and are stable enough to remember selected or recording targets while the page is
     * still visible. Keeping them together avoids mixing coordinate systems at call sites.
     */
    private record TrackAddress(int visibleIndex, int sourceIndex, int absoluteIndex, Track track) {
    }

    private static final long PARAMETER_RESET_TOUCH_HOLD_MS = 750;
    private static final long PARAMETER_RESET_RECENT_ADJUSTMENT_SUPPRESS_MS = 300;
    private static final int PARAMETER_RESET_TOLERATED_ADJUSTMENT_UNITS = 2;
    private static final int MAX_TRACKS = 16;
    private static final int MAX_SCENES = 16;
    private static final int MIX_DEVICE_ROWS = 4;
    private static final int MIX_DEVICE_PAGE_COUNT = 2;
    private static final int MIX_DEVICE_SLOTS = MIX_DEVICE_ROWS * MIX_DEVICE_PAGE_COUNT;
    private static final int SCENE_ROW = 0;
    private static final double MIN_DUPLICATE_CLIP_LENGTH = 1.0;
    private static final double MAX_DUPLICATE_CLIP_LENGTH = 256.0;
    private static final int METER_REFRESH_TICKS = 1;
    private static final long METER_DISPLAY_SUPPRESS_MS = 3000;
    private static final long METER_MODE_INFO_SUPPRESS_MS = 1200;
    private static final String SELECTED_TRACK_METER_LEGEND = "Peak        | RMS";
    private static final String MIXER_ENCODER_FOOTER = "Vol  Pan  S1  S2";
    private static final String BLANK_TEXT_ROW = "                    ";
    private static final RgbLigthState SETTINGS_LOGO_ON = new RgbLigthState(127, 20, 0, true);
    private static final RgbLigthState SETTINGS_LOGO_OFF = RgbLigthState.OFF;
    private static final boolean[][] SETTINGS_LOGO = {
            {true, true, true, false, true, true, true, false, true, true, true, false, true, true, true, true},
            {true, false, false, false, false, true, false, false, true, false, true, false, true, false, false, false},
            {true, true, false, false, false, true, false, false, true, true, false, false, true, true, true, false},
            {true, false, false, false, true, true, true, false, true, false, true, false, true, true, true, true}
    };

    private final AkaiFireOikontrolExtension driver;
    private final SharedMusicalContext sharedMusicalContext;
    private final OledDisplay oled;
    private final TrackBank trackBank;
    private final CursorTrack cursorTrack;
    private final CursorTrack remoteCursorTrack;
    private final CursorRemoteControlsPage projectRemoteControls;
    private final CursorRemoteControlsPage trackRemoteControls;
    private final CursorRemoteControlsPage deviceRemoteControls;
    private final PinnableCursorDevice remoteCursorDevice;
    private final Project project;
    private final PinnableCursorClip performCursorClip;

    private final Layer channelLayer;
    private final Layer mixerLayer;
    private final Layer user1Layer;
    private final Layer user2Layer;
    private final Map<EncoderMode, Layer> modeMapping = new HashMap<>();

    private final RgbLigthState[] slotColors = new RgbLigthState[MAX_TRACKS * MAX_SCENES];
    private final RgbLigthState[] sceneColors = new RgbLigthState[MAX_SCENES];
    private final RgbLigthState[] trackColors = new RgbLigthState[MAX_TRACKS];
    private final String[] trackNames = new String[MAX_TRACKS];
    private final DeviceBank[] trackDeviceBanks = new DeviceBank[MAX_TRACKS];
    private final String[][] trackDeviceNames = new String[MAX_TRACKS][MIX_DEVICE_SLOTS];
    private final Map<Integer, Integer> rememberedDeviceByTrack = new HashMap<>();
    private final String[] sceneNames = new String[MAX_SCENES];
    private final int[] trackPeakMeters = new int[MAX_TRACKS];
    private final int[] trackRmsMeters = new int[MAX_TRACKS];
    private final VuMeterPeakHold trackPeakHoldMeters = new VuMeterPeakHold(MAX_TRACKS);
    private final boolean[] selectedVisibleTracks = new boolean[MAX_TRACKS];
    private final BooleanValueObject selectHeld = new BooleanValueObject();
    private final BooleanValueObject copyHeld = new BooleanValueObject();
    private final BooleanValueObject deleteHeld = new BooleanValueObject();
    private final EncoderTouchResetHandler parameterResetHandler;
    private Layer currentEncoderLayer;
    private EncoderMode encoderMode = EncoderMode.CHANNEL;
    private EncoderMode encoderModeBeforeMixDeviceMode;
    private boolean duplicateHeld;
    private int blinkState;
    private int totalTrackCount = MAX_TRACKS;
    private int totalSceneCount = MAX_SCENES;
    private int selectedTrackIndex = -1;
    private int selectedSceneIndex = -1;
    private int selectedMeterSourceIndex = -1;
    private int selectedMeterAbsoluteIndex = -1;
    private int selectedRemoteTrackIndex = -1;
    private int selectedRemoteDeviceIndex = -1;
    private int mixDevicePageIndex = 0;
    private int selectedTrackRmsMax = 0;
    private int selectedTrackPeakMax = 0;
    private int lastMeterDisplayBlink = Integer.MIN_VALUE;
    private int selectedSceneActionIndex = -1;
    private int pendingSceneLaunchIndex = -1;
    private int manualRecordingTrackIndex = -1;
    private int manualRecordingSceneIndex = -1;
    private boolean mainEncoderPressConsumed = false;
    private boolean manualRecordingPending = false;
    private boolean manualRecordingWasRecording = false;
    private boolean manualRecordingShouldRound = false;
    private boolean trackActionMode = false;
    private boolean mixDeviceToggleMode = false;
    private boolean sceneActionMode = false;
    private boolean active = false;
    private boolean mixMeterDisplayActive = false;
    private boolean selectedTrackMeterTextInitialized = false;
    private boolean mixerEncoderFooterVisible = false;
    private long mixMeterSuppressedUntilMs = 0;
    private PerformLayout layout = PerformLayout.vertical();

    public PerformClipLauncherMode(final AkaiFireOikontrolExtension driver) {
        super(driver.getLayers(), "PERFORM_CLIP_LAUNCHER");
        this.driver = driver;
        this.sharedMusicalContext = driver.getSharedMusicalContext();
        this.layout = FireControlPreferences.PERFORM_LAYOUT_HORIZONTAL.equals(driver.getPerformClipLauncherLayoutPreference())
                ? PerformLayout.horizontal()
                : PerformLayout.vertical();
        final ControllerHost host = driver.getHost();
        this.oled = driver.getOled();
        this.trackBank = host.createTrackBank(MAX_TRACKS, 0, MAX_SCENES, true);
        this.cursorTrack = driver.getViewControl().getCursorTrack();
        this.remoteCursorTrack = host.createCursorTrack("PERFORM_REMOTE_TRACK", "Perform Remote Track", 8, 0, true);
        this.project = host.getProject();
        this.project.hasSoloedTracks().markInterested();
        this.project.hasMutedTracks().markInterested();
        this.projectRemoteControls = project.getRootTrackGroup().createCursorRemoteControlsPage(8);
        this.remoteCursorTrack.position().markInterested();
        this.trackRemoteControls = remoteCursorTrack.createCursorRemoteControlsPage(8);
        this.remoteCursorDevice = remoteCursorTrack.createCursorDevice("PERFORM_REMOTE_DEVICE", "Perform Remote Device",
                8, CursorDeviceFollowMode.FOLLOW_SELECTION);
        this.remoteCursorDevice.exists().markInterested();
        this.remoteCursorDevice.position().markInterested();
        this.remoteCursorTrack.position().addValueObserver(position -> selectedRemoteTrackIndex = position);
        this.remoteCursorDevice.position().addValueObserver(position -> selectedRemoteDeviceIndex = position);
        this.remoteCursorDevice.exists().addValueObserver(exists -> {
            if (!exists) {
                selectedRemoteDeviceIndex = -1;
            }
        });
        this.selectedRemoteTrackIndex = this.remoteCursorTrack.position().get();
        this.selectedRemoteDeviceIndex = this.remoteCursorDevice.exists().get()
                ? this.remoteCursorDevice.position().get()
                : -1;
        this.deviceRemoteControls = this.remoteCursorDevice.createCursorRemoteControlsPage(8);
        this.performCursorClip = cursorTrack.createLauncherCursorClip("PERFORM_CURSOR", "PERFORM_CURSOR", 64, 128);

        this.channelLayer = new Layer(driver.getLayers(), "PERFORM_ENC_CHANNEL");
        this.mixerLayer = new Layer(driver.getLayers(), "PERFORM_ENC_MIXER");
        this.user1Layer = new Layer(driver.getLayers(), "PERFORM_ENC_USER1");
        this.user2Layer = new Layer(driver.getLayers(), "PERFORM_ENC_USER2");
        this.currentEncoderLayer = channelLayer;
        this.parameterResetHandler = new EncoderTouchResetHandler(
                new TouchResetGesture(4, PARAMETER_RESET_TOUCH_HOLD_MS, PARAMETER_RESET_RECENT_ADJUSTMENT_SUPPRESS_MS,
                        PARAMETER_RESET_TOLERATED_ADJUSTMENT_UNITS),
                driver::isEncoderTouchResetEnabled,
                (task, delayMs) -> driver.getHost().scheduleTask(task, delayMs),
                PARAMETER_RESET_TOUCH_HOLD_MS,
                oled::clearScreenDelayed);

        trackBank.channelCount().markInterested();
        trackBank.channelCount().addValueObserver(count -> totalTrackCount = count);
        trackBank.scrollPosition().markInterested();
        trackBank.sceneBank().itemCount().markInterested();
        trackBank.sceneBank().itemCount().addValueObserver(count -> totalSceneCount = count);
        trackBank.sceneBank().scrollPosition().markInterested();
        performCursorClip.getLoopLength().markInterested();

        initGrid();
        initEncoderPages();
        bindMainEncoder();
        initButtons();
    }

    private void initGrid() {
        for (int sceneIndex = 0; sceneIndex < MAX_SCENES; sceneIndex++) {
            final Scene scene = trackBank.sceneBank().getScene(sceneIndex);
            scene.exists().markInterested();
            scene.name().markInterested();
            scene.color().markInterested();
            final int row = sceneIndex;
            scene.name().addValueObserver(name -> sceneNames[row] = name);
            scene.color().addValueObserver((r, g, b) -> sceneColors[row] = ColorLookup.getColor(r, g, b));
            sceneNames[row] = scene.name().get();
            sceneColors[row] = ColorLookup.getColor(scene.color().get());
        }

        for (int trackIndex = 0; trackIndex < MAX_TRACKS; trackIndex++) {
            final Track track = trackBank.getItemAt(trackIndex);
            track.exists().markInterested();
            track.name().markInterested();
            track.color().markInterested();
            track.arm().markInterested();
            track.mute().markInterested();
            track.solo().markInterested();
            track.isMutedBySolo().markInterested();
            track.isActivated().markInterested();
            track.isStopped().markInterested();
            track.isQueuedForStop().markInterested();
            final int column = trackIndex;
            track.name().addValueObserver(name -> trackNames[column] = name);
            track.color().addValueObserver((r, g, b) -> trackColors[column] = ColorLookup.getColor(r, g, b));
            trackNames[column] = track.name().get();
            trackColors[column] = ColorLookup.getColor(track.color().get());
            track.addIsSelectedInMixerObserver(selected -> {
                selectedVisibleTracks[column] = selected;
                if (selected) {
                    selectedTrackIndex = trackBank.scrollPosition().get() + column;
                    selectMeterTrack(column, false);
                }
            });
            track.addIsSelectedInEditorObserver(selected -> {
                if (selected) {
                    selectedVisibleTracks[column] = true;
                    selectedTrackIndex = trackBank.scrollPosition().get() + column;
                    selectMeterTrack(column, false);
                }
            });
            track.addVuMeterObserver(VuMeterFormatter.RANGE, -1, true, value -> handlePeakMeterChanged(column, value));
            track.addVuMeterObserver(VuMeterFormatter.RANGE, -1, false, value -> handleRmsMeterChanged(column, value));
            final DeviceBank deviceBank = track.createDeviceBank(MIX_DEVICE_SLOTS);
            trackDeviceBanks[column] = deviceBank;
            for (int deviceIndex = 0; deviceIndex < MIX_DEVICE_SLOTS; deviceIndex++) {
                final Device device = deviceBank.getItemAt(deviceIndex);
                final int row = deviceIndex;
                device.exists().markInterested();
                device.name().markInterested();
                device.isEnabled().markInterested();
                device.name().addValueObserver(name -> trackDeviceNames[column][row] = name);
                trackDeviceNames[column][row] = device.name().get();
            }

            for (int sceneIndex = 0; sceneIndex < MAX_SCENES; sceneIndex++) {
                final int slotIndex = toSlotIndex(trackIndex, sceneIndex);
                final int row = sceneIndex;
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
                slot.exists().markInterested();
                slot.hasContent().markInterested();
                slot.isSelected().markInterested();
                slot.isPlaying().markInterested();
                slot.isPlaybackQueued().markInterested();
                slot.isRecording().markInterested();
                slot.isRecordingQueued().markInterested();
                slot.isStopQueued().markInterested();
                slot.color().markInterested();
                slot.color().addValueObserver((r, g, b) -> slotColors[slotIndex] = ColorLookup.getColor(r, g, b));
                slot.isSelected().addValueObserver(selected -> {
                    if (selected) {
                        selectedTrackIndex = trackBank.scrollPosition().get() + column;
                        selectedSceneIndex = trackBank.sceneBank().scrollPosition().get() + row;
                    }
                });
                slot.isRecording().addValueObserver(recording -> handleSlotRecordingChanged(column, row, recording));
                slotColors[slotIndex] = ColorLookup.getColor(slot.color().get());
            }
        }
    }

    private void initEncoderPages() {
        modeMapping.put(EncoderMode.CHANNEL, channelLayer);
        modeMapping.put(EncoderMode.MIXER, mixerLayer);
        modeMapping.put(EncoderMode.USER_1, user1Layer);
        modeMapping.put(EncoderMode.USER_2, user2Layer);

        markRemotePageInterested(projectRemoteControls);
        markRemotePageInterested(trackRemoteControls);
        markRemotePageInterested(deviceRemoteControls);

        bindChannelPage(channelLayer, projectRemoteControls, "Global");
        bindMixerPage(mixerLayer);
        bindRemotePage(user1Layer, trackRemoteControls, "Track");
        bindRemotePage(user2Layer, deviceRemoteControls, "Device");
    }

    private void bindMainEncoder() {
        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.bindEncoder(this, this::handleMainEncoder);
        mainEncoder.bindTouched(this, this::handleMainEncoderPress);
    }

    private void showOverview() {
        showTransientDetailInfo("Settings",
                "1: Root %s\n2: Scale %s\n3: Oct %d".formatted(
                        com.oikoaudio.fire.note.NoteGridLayout.noteName(sharedMusicalContext.getRootNote()),
                        sharedMusicalContext.getScaleDisplayName(),
                        sharedMusicalContext.getOctave()));
        clearTransientDisplayDelayed();
    }

    public boolean isSettingsMode() {
        return false;
    }

    public boolean isTrackActionMode() {
        return trackActionMode;
    }

    public void setTrackActionMode(final boolean enabled) {
        if (!enabled) {
            leaveMixDeviceMode();
        } else {
            encoderModeBeforeMixDeviceMode = null;
        }
        trackActionMode = enabled;
        if (enabled) {
            sceneActionMode = false;
            clearClipModifierButtons();
        }
    }

    public void toggleTrackActionMode() {
        final boolean enabled = !trackActionMode;
        if (!enabled) {
            leaveMixDeviceMode();
        } else {
            encoderModeBeforeMixDeviceMode = null;
        }
        trackActionMode = enabled;
        if (trackActionMode) {
            sceneActionMode = false;
            clearClipModifierButtons();
        }
        if (trackActionMode) {
            showTrackActionInfo();
        } else {
            showCurrentModeInfo();
        }
        oled.clearScreenDelayed();
    }

    public boolean isSceneActionMode() {
        return sceneActionMode;
    }

    public void toggleSceneActionMode() {
        sceneActionMode = !sceneActionMode;
        if (sceneActionMode) {
            trackActionMode = false;
        }
        showCurrentModeInfo();
        oled.clearScreenDelayed();
    }

    public void toggleOrientation() {
        layout = layout.toggle();
        showCurrentModeInfo();
        oled.clearScreenDelayed();
    }

    public String modeLabel() {
        return layout.label();
    }

    public String activePageLabel() {
        if (trackActionMode) {
            return mixDeviceToggleMode ? mixDevicePageTitle() : "Mix";
        }
        if (sceneActionMode) {
            return "Scene Launch";
        }
        return layout.label();
    }

    private void bindChannelPage(final Layer layer, final CursorRemoteControlsPage page, final String fallbackPrefix) {
        final TouchEncoder[] encoders = driver.getEncoders();
        markPageParametersInterested(page);
        for (int i = 0; i < encoders.length; i++) {
            final int index = i;
            encoders[i].bindContinuousEncoder(layer, this::isShiftHeld, inc -> {
                if (isSettingsHeld()) {
                    adjustOverviewPitch(index, inc);
                    return;
                }
                final int parameterIndex = remoteParameterIndex(index, isAltHeld());
                final Parameter parameter = page.getParameter(parameterIndex);
                adjustRemoteParameter(index, parameter, fallbackPrefix + " " + (parameterIndex + 1), inc);
            });
            encoders[i].bindTouched(layer, touched -> {
                if (isSettingsHeld()) {
                    handleOverviewTouch(index, touched);
                    return;
                }
                final int parameterIndex = remoteParameterIndex(index, isAltHeld());
                final Parameter parameter = page.getParameter(parameterIndex);
                handleRemoteParameterTouch(index, parameter, fallbackPrefix + " " + (parameterIndex + 1), touched);
            });
        }
    }

    private void bindRemotePage(final Layer layer, final CursorRemoteControlsPage page, final String fallbackPrefix) {
        final TouchEncoder[] encoders = driver.getEncoders();
        markPageParametersInterested(page);
        for (int i = 0; i < encoders.length; i++) {
            final int index = i;
            encoders[i].bindContinuousEncoder(layer, this::isShiftHeld,
                    inc -> {
                        final int parameterIndex = remoteParameterIndex(index, isAltHeld());
                        final Parameter parameter = page.getParameter(parameterIndex);
                        adjustRemoteParameter(index, parameter, fallbackPrefix + " " + (parameterIndex + 1), inc);
                    });
            encoders[i].bindTouched(layer, touched -> {
                final int parameterIndex = remoteParameterIndex(index, isAltHeld());
                final Parameter parameter = page.getParameter(parameterIndex);
                handleRemoteParameterTouch(index, parameter, fallbackPrefix + " " + (parameterIndex + 1), touched);
            });
        }
    }

    private void adjustOverviewPitch(final int encoderIndex, final int inc) {
        if (inc == 0) {
            return;
        }
        if (encoderIndex == 0) {
            sharedMusicalContext.adjustRootNote(inc);
            showValueInfo("Root", com.oikoaudio.fire.note.NoteGridLayout.noteName(sharedMusicalContext.getRootNote()));
            return;
        }
        if (encoderIndex == 1) {
            sharedMusicalContext.adjustScaleIndex(inc, -1);
            showValueInfo("Scale", sharedMusicalContext.getScaleDisplayName());
            return;
        }
        if (encoderIndex == 2) {
            sharedMusicalContext.adjustOctave(inc);
            showValueInfo("Octave", Integer.toString(sharedMusicalContext.getOctave()));
            return;
        }
        showOverview();
    }

    private void handleOverviewTouch(final int encoderIndex, final boolean touched) {
        if (!touched) {
            oled.clearScreenDelayed();
            return;
        }
        if (encoderIndex == 0) {
            showValueInfo("Root", com.oikoaudio.fire.note.NoteGridLayout.noteName(sharedMusicalContext.getRootNote()));
            return;
        }
        if (encoderIndex == 1) {
            showValueInfo("Scale", sharedMusicalContext.getScaleDisplayName());
            return;
        }
        if (encoderIndex == 2) {
            showValueInfo("Octave", Integer.toString(sharedMusicalContext.getOctave()));
            return;
        }
        showOverview();
    }

    private void bindMixerPage(final Layer layer) {
        final TouchEncoder[] encoders = driver.getEncoders();
        final Parameter[] parameters = {
                cursorTrack.volume(),
                cursorTrack.pan(),
                cursorTrack.sendBank().getItemAt(0),
                cursorTrack.sendBank().getItemAt(1)
        };
        final String[] fallbackLabels = {"Volume", "Pan", "Send 1", "Send 2"};
        for (int i = 0; i < encoders.length; i++) {
            final int index = i;
            final Parameter parameter = parameters[i];
            final String label = fallbackLabels[i];
            ParameterEncoderBinding.bind(encoders[i], layer, index, parameter, label, this::isShiftHeld,
                    ParameterEncoderBinding.TouchResetControl.of(parameterResetHandler), mixerResetPolicy(index),
                    this::showTransientValueInfo, this::clearTransientDisplayDelayed);
        }
    }

    private void initButtons() {
        new PadBankRowControlBindings(driver, this, performControlBindingsHost(),
                new PadBankRowControlBindings.ExtraButtonBinding(NoteAssign.KNOB_MODE,
                        this::handleModeAdvance, this::modeLightState)).bind();
        bindMixStatusLights();

        final BiColorButton patternUp = driver.getButton(NoteAssign.PATTERN_UP);
        patternUp.bindPressed(this, pressed -> handlePatternSceneScroll(pressed, -1),
                () -> patternSceneNavigationLightState(trackActionMode, mixDeviceToggleMode, mixDevicePageIndex,
                        isAltHeld(), deviceRemoteControls.selectedPageIndex().get(),
                        deviceRemoteControls.pageCount().getAsInt(), -1, canScrollScenes(-1)));

        final BiColorButton patternDown = driver.getButton(NoteAssign.PATTERN_DOWN);
        patternDown.bindPressed(this, pressed -> handlePatternSceneScroll(pressed, 1),
                () -> patternSceneNavigationLightState(trackActionMode, mixDeviceToggleMode, mixDevicePageIndex,
                        isAltHeld(), deviceRemoteControls.selectedPageIndex().get(),
                        deviceRemoteControls.pageCount().getAsInt(), 1, canScrollScenes(1)));
    }

    private void bindMixStatusLights() {
        final MultiStateHardwareLight[] stateLights = driver.getStateLights();
        for (int index = 0; index < stateLights.length; index++) {
            final int lightIndex = index;
            bindLightState(() -> mixStatusLightState(trackActionMode,
                    project.hasSoloedTracks().get(),
                    project.hasMutedTracks().get(),
                    lightIndex), stateLights[lightIndex]);
        }
    }

    private PadBankRowControlBindings.Host performControlBindingsHost() {
        return new PadBankRowControlBindings.Host() {
            @Override
            public void handlePadPress(final int padIndex, final boolean pressed) {
                PerformClipLauncherMode.this.handlePadPressed(padIndex, pressed);
            }

            @Override
            public RgbLigthState padLight(final int padIndex) {
                return PerformClipLauncherMode.this.getPadState(padIndex);
            }

            @Override
            public void handleBankButton(final boolean pressed, final int amount) {
                PerformClipLauncherMode.this.handleTrackScroll(pressed, amount);
            }

            @Override
            public BiColorLightState bankLightState() {
                return canScrollBankLeftRight(-1) || canScrollBankLeftRight(1)
                        ? BiColorLightState.AMBER_HALF
                        : BiColorLightState.OFF;
            }

            @Override
            public BiColorLightState bankLightState(final int amount) {
                return canScrollBankLeftRight(amount) ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF;
            }

            @Override
            public void handleRowButton(final int index, final boolean pressed) {
                handlePadActionButton(index, pressed);
            }

            @Override
            public BiColorLightState rowLightState(final int index) {
                return padActionLightState(index);
            }
        };
    }

    private void handlePadActionButton(final int index, final boolean pressed) {
        suppressMixMeterDisplay();
        if (trackActionMode) {
            if (mixDeviceToggleMode && isAltHeld()) {
                handleMixDeviceRowToggle(index, pressed);
                return;
            }
            handleMixFunctionButton(index, pressed);
            return;
        }
        switch (index) {
            case 0 -> handleModifierButton(selectHeld, "Select", "Pad select", pressed);
            case 1 -> {
                duplicateHeld = pressed;
                handleDuplicatePressed(pressed);
            }
            case 2 -> handleCopyPressed(pressed);
            case 3 -> handleModifierButton(deleteHeld, "Delete", "Pad delete", pressed);
            default -> throw new IllegalArgumentException("Unsupported pad action button index: " + index);
        }
    }

    private void handleMixFunctionButton(final int index, final boolean pressed) {
        if (!pressed) {
            oled.clearScreenDelayed();
            return;
        }
        switch (index) {
            case 0 -> driver.goToArrangementStartOrLoopStart();
            case 1 -> {
                if (project.hasSoloedTracks().get()) {
                    project.unsoloAll();
                    showValueInfo("Mix Solo", "Cleared");
                } else {
                    showValueInfo("Mix Solo", "None");
                }
            }
            case 2 -> {
                if (project.hasMutedTracks().get()) {
                    project.unmuteAll();
                    showValueInfo("Mix Mute", "Cleared");
                } else {
                    showValueInfo("Mix Mute", "None");
                }
            }
            case 3 -> driver.goToArrangementEndOrLoopEnd();
            default -> throw new IllegalArgumentException("Unsupported mix function button index: " + index);
        }
    }

    private void handleMixDeviceRowToggle(final int rowIndex, final boolean pressed) {
        if (!pressed) {
            oled.clearScreenDelayed();
            return;
        }
        final int deviceIndex = mixDeviceIndexForRow(rowIndex, mixDevicePageIndex);
        if (deviceIndex < 0) {
            return;
        }

        boolean anyDevices = false;
        boolean anyEnabled = false;
        for (int visibleTrackIndex = 0; visibleTrackIndex < visibleTrackCount(); visibleTrackIndex++) {
            final TrackAddress trackAddress = trackAddressForVisibleTrack(visibleTrackIndex);
            if (trackAddress == null) {
                continue;
            }
            final Device device = mixDevice(trackAddress.sourceIndex(), deviceIndex);
            if (device != null && device.exists().get()) {
                anyDevices = true;
                anyEnabled |= device.isEnabled().get();
            }
        }

        if (!anyDevices) {
            showValueInfo("Device Row", "No devices");
            return;
        }

        final boolean targetEnabled = rowWideDeviceToggleTarget(anyEnabled);
        for (int visibleTrackIndex = 0; visibleTrackIndex < visibleTrackCount(); visibleTrackIndex++) {
            final TrackAddress trackAddress = trackAddressForVisibleTrack(visibleTrackIndex);
            if (trackAddress == null) {
                continue;
            }
            final Device device = mixDevice(trackAddress.sourceIndex(), deviceIndex);
            if (device != null && device.exists().get()) {
                device.isEnabled().set(targetEnabled);
            }
        }
        showValueInfo(rowWideDeviceToggleTitle(targetEnabled), "Device " + (deviceIndex + 1));
    }

    private BiColorLightState padActionLightState(final int index) {
        if (trackActionMode) {
            return mixStatusLightState(true, project.hasSoloedTracks().get(), project.hasMutedTracks().get(), index);
        }
        return switch (index) {
            case 0 -> selectHeld.get() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF;
            case 1 -> duplicateHeld ? BiColorLightState.AMBER_FULL : BiColorLightState.OFF;
            case 2 -> copyHeld.get() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF;
            case 3 -> deleteHeld.get() ? BiColorLightState.RED_FULL : BiColorLightState.OFF;
            default -> throw new IllegalArgumentException("Unsupported pad action button index: " + index);
        };
    }

    private void clearClipModifierButtons() {
        selectHeld.set(false);
        copyHeld.set(false);
        deleteHeld.set(false);
        duplicateHeld = false;
    }

    private void handleModifierButton(final BooleanValueObject heldState, final String functionName,
                                      final String detail, final boolean pressed) {
        heldState.set(pressed);
        if (pressed) {
            showValueInfo(functionName, detail);
        } else {
            oled.clearScreenDelayed();
        }
    }

    private void handleCopyPressed(final boolean pressed) {
        copyHeld.set(pressed);
        if (!pressed) {
            oled.clearScreenDelayed();
            return;
        }
        final ClipLauncherSlot source = getSelectedVisibleSlot();
        if (source == null || !source.exists().get() || !source.hasContent().get()) {
            showValueInfo("Copy Clip", "Select source first");
            return;
        }
        showValueInfo("Paste sel", "Pad target");
    }

    private void handlePadPressed(final int padIndex, final boolean pressed) {
        if (pressed) {
            suppressMixMeterDisplay();
        }
        if (trackActionMode) {
            handleTrackActionPadPressed(padIndex, pressed);
            return;
        }
        if (sceneActionMode) {
            handleSceneActionPadPressed(padIndex, pressed);
            return;
        }
        final int visibleTrackIndex = visibleTrackIndexForPad(padIndex);
        final int visibleSceneIndex = visibleSceneIndexForPad(padIndex);
        if (visibleTrackIndex < 0 || visibleSceneIndex < 0
                || visibleTrackIndex >= visibleTrackCount()
                || visibleSceneIndex >= visibleSceneCount()) {
            return;
        }
        final TrackAddress trackAddress = trackAddressForVisibleTrack(visibleTrackIndex);
        if (trackAddress == null) {
            return;
        }
        final ClipLauncherSlot slot = trackAddress.track().clipLauncherSlotBank().getItemAt(visibleSceneIndex);
        handleSlotPressed(trackAddress, slot, visibleSceneIndex, pressed);
    }

    private void handleTrackActionPadPressed(final int padIndex, final boolean pressed) {
        if (mixDeviceToggleMode) {
            handleMixDevicePadPressed(padIndex, pressed);
            return;
        }
        if (!pressed) {
            return;
        }
        final TrackActionRow actionRow = TrackActionRow.fromPadIndex(padIndex);
        if (actionRow == null) {
            return;
        }
        final int visibleTrackIndex = padIndex % PerformLayout.PAD_COLUMNS;
        final TrackAddress trackAddress = trackAddressForVisibleTrack(visibleTrackIndex);
        if (trackAddress == null) {
            return;
        }
        final Track track = trackAddress.track();
        final String trackLabel = trackLabel(trackAddress);
        selectMeterTrack(trackAddress.sourceIndex(), true);
        track.selectInMixer();
        switch (actionRow) {
            case SELECT -> {
                if (isAltHeld()) {
                    track.stop();
                    showValueInfo("Mix Stop", trackLabel);
                    return;
                }
                track.selectInEditor();
                restoreRememberedMixDevice(trackAddress);
                showValueInfo("Mix Select", trackLabel);
            }
            case SOLO -> {
                track.solo().toggle(false);
                showValueInfo(track.solo().get() ? "Mix Solo" : "Mix Unsolo", trackLabel);
            }
            case MUTE -> {
                track.mute().toggle();
                showValueInfo(track.mute().get() ? "Mix Mute" : "Mix Unmute", trackLabel);
            }
            case ARM -> {
                track.arm().toggle();
                showValueInfo(track.arm().get() ? "Mix Arm" : "Mix Disarm", trackLabel);
            }
        }
    }

    private void handleMixDevicePadPressed(final int padIndex, final boolean pressed) {
        if (!pressed) {
            return;
        }
        final int deviceIndex = mixDeviceIndexForPad(padIndex, mixDevicePageIndex);
        if (deviceIndex < 0) {
            return;
        }
        final int visibleTrackIndex = padIndex % PerformLayout.PAD_COLUMNS;
        final TrackAddress trackAddress = trackAddressForVisibleTrack(visibleTrackIndex);
        if (trackAddress == null) {
            return;
        }
        final Device device = mixDevice(trackAddress.sourceIndex(), deviceIndex);
        if (device == null || !device.exists().get()) {
            showValueInfo("Mix Device", "No device");
            return;
        }
        selectMeterTrack(trackAddress.sourceIndex(), true);
        if (isAltHeld()) {
            final boolean enabled = !device.isEnabled().get();
            device.isEnabled().set(enabled);
            showValueInfo(mixDeviceActionTitle(true, enabled), mixDeviceName(trackAddress, deviceIndex));
            return;
        }
        trackAddress.track().selectInMixer();
        trackAddress.track().selectInEditor();
        remoteCursorDevice.selectDevice(device);
        selectedRemoteTrackIndex = trackAddress.absoluteIndex();
        selectedRemoteDeviceIndex = deviceIndex;
        rememberMixDeviceSelection(rememberedDeviceByTrack, trackAddress.absoluteIndex(), deviceIndex);
        device.selectInEditor();
        showValueInfo(mixDeviceActionTitle(false, device.isEnabled().get()), mixDeviceName(trackAddress, deviceIndex));
    }

    private void restoreRememberedMixDevice(final TrackAddress trackAddress) {
        final int deviceIndex = rememberedMixDeviceSelection(rememberedDeviceByTrack, trackAddress.absoluteIndex());
        if (deviceIndex < 0) {
            return;
        }
        final Device device = mixDevice(trackAddress.sourceIndex(), deviceIndex);
        if (device == null || !device.exists().get()) {
            rememberedDeviceByTrack.remove(trackAddress.absoluteIndex());
            return;
        }
        remoteCursorDevice.selectDevice(device);
        selectedRemoteTrackIndex = trackAddress.absoluteIndex();
        selectedRemoteDeviceIndex = deviceIndex;
        device.selectInEditor();
    }

    private void handleSceneActionPadPressed(final int padIndex, final boolean pressed) {
        if (!pressed || padIndex / PerformLayout.PAD_COLUMNS != SCENE_ROW) {
            return;
        }
        final int visibleSceneIndex = padIndex % PerformLayout.PAD_COLUMNS;
        final int absoluteSceneIndex = trackBank.sceneBank().scrollPosition().get() + visibleSceneIndex;
        if (visibleSceneIndex >= MAX_SCENES || absoluteSceneIndex >= totalSceneCount) {
            return;
        }
        final Scene scene = trackBank.sceneBank().getScene(visibleSceneIndex);
        if (scene == null || !scene.exists().get()) {
            return;
        }

        if (deleteHeld.get()) {
            scene.deleteObject();
            showValueInfo("Delete Scene", sceneLabel(absoluteSceneIndex, visibleSceneIndex));
            return;
        }

        if (copyHeld.get()) {
            final int sourceVisibleSceneIndex = resolveSceneCopySource();
            if (sourceVisibleSceneIndex >= 0 && sourceVisibleSceneIndex != visibleSceneIndex) {
                final Scene source = trackBank.sceneBank().getScene(sourceVisibleSceneIndex);
                scene.replaceInsertionPoint().copySlotsOrScenes(source);
                final String sourceLabel = sceneLabel(trackBank.sceneBank().scrollPosition().get() + sourceVisibleSceneIndex,
                        sourceVisibleSceneIndex);
                final String destinationLabel = sceneLabel(absoluteSceneIndex, visibleSceneIndex);
                showValueInfo("Copy Scene", "Select target");
                driver.notifyPopup("Copy Scene", sourceLabel + " -> " + destinationLabel);
            } else {
                showValueInfo("Copy Scene", "Select source first");
            }
            return;
        }

        if (selectHeld.get()) {
            selectedSceneActionIndex = absoluteSceneIndex;
            showValueInfo("Select Scene", sceneLabel(absoluteSceneIndex, visibleSceneIndex));
            return;
        }

        scene.launch();
        pendingSceneLaunchIndex = absoluteSceneIndex;
        showValueInfo("Launch Scene", sceneLabel(absoluteSceneIndex, visibleSceneIndex));
    }

    private void handleSlotPressed(final TrackAddress trackAddress, final ClipLauncherSlot slot,
                                   final int visibleSceneIndex, final boolean pressed) {
        final Track track = trackAddress.track();
        if (!pressed || !track.exists().get() || !slot.exists().get()) {
            return;
        }
        if (isSettingsHeld()) {
            showValueInfo("Settings", "Encoders adjust globals");
            return;
        }

        final int absoluteTrackIndex = trackAddress.absoluteIndex();
        final int absoluteSceneIndex = trackBank.sceneBank().scrollPosition().get() + visibleSceneIndex;
        final boolean hasContent = slot.hasContent().get();

        if (driver.isPerformRecordTargetingHeld()) {
            recordIntoSlot(track, slot, absoluteTrackIndex, absoluteSceneIndex);
            return;
        }

        if (deleteHeld.get()) {
            if (hasContent) {
                slot.deleteObject();
                showValueInfo("Delete Clip", slotLabel(absoluteTrackIndex, absoluteSceneIndex));
            }
            return;
        }

        if (copyHeld.get()) {
            final ClipLauncherSlot source = getSelectedVisibleSlot();
            if (source != null && (selectedTrackIndex != absoluteTrackIndex || selectedSceneIndex != absoluteSceneIndex)) {
                slot.replaceInsertionPoint().copySlotsOrScenes(source);
                final String sourceLabel = selectedSlotLabel();
                final String destinationLabel = slotLabel(absoluteTrackIndex, absoluteSceneIndex);
                showValueInfo("Copy Clip", "Select target");
                driver.notifyPopup("Copy Clip", sourceLabel + " -> " + destinationLabel);
            } else {
                showValueInfo("Copy Clip", "Select source first");
            }
            return;
        }

        track.selectInMixer();
        slot.select();

        if (selectHeld.get()) {
            showValueInfo("Select Clip", slotLabel(absoluteTrackIndex, absoluteSceneIndex));
            return;
        }

        if (slot.isRecording().get()) {
            slot.launch();
            showValueInfo("Clip Record", slotLabel(absoluteTrackIndex, absoluteSceneIndex));
            return;
        }

        if (hasContent) {
            slot.launch();
            showValueInfo("Launch Clip", slotLabel(absoluteTrackIndex, absoluteSceneIndex));
            return;
        }

        slot.createEmptyClip(driver.getDefaultClipLengthBeats());
        slot.launch();
        showValueInfo("Create Clip", slotLabel(absoluteTrackIndex, absoluteSceneIndex));
    }

    private void recordIntoSlot(final Track track, final ClipLauncherSlot slot, final int absoluteTrackIndex,
                                final int absoluteSceneIndex) {
        driver.consumePerformRecordPadGesture();
        track.selectInMixer();
        slot.select();
        if (driver.shouldDisableLauncherPostRecordingAction() || driver.shouldRoundLauncherRecordingToNearestBar()) {
            armManualRecording(absoluteTrackIndex, absoluteSceneIndex, driver.shouldRoundLauncherRecordingToNearestBar());
        }
        driver.prepareLauncherRecording();
        slot.record();
        showValueInfo("Record Clip", slotLabel(absoluteTrackIndex, absoluteSceneIndex));
    }

    private void armManualRecording(final int absoluteTrackIndex, final int absoluteSceneIndex, final boolean shouldRound) {
        manualRecordingPending = true;
        manualRecordingWasRecording = false;
        manualRecordingShouldRound = shouldRound;
        manualRecordingTrackIndex = absoluteTrackIndex;
        manualRecordingSceneIndex = absoluteSceneIndex;
    }

    public boolean stopManualLauncherRecordingIfAny() {
        if (!manualRecordingPending) {
            return false;
        }
        final TrackAddress trackAddress = trackAddressForAbsoluteTrack(manualRecordingTrackIndex);
        final int visibleSceneIndex = manualRecordingSceneIndex - trackBank.sceneBank().scrollPosition().get();
        if (trackAddress == null || visibleSceneIndex < 0 || visibleSceneIndex >= visibleSceneCount()) {
            showValueInfo("Clip Record", "Target off page");
            return true;
        }
        final Track track = trackAddress.track();
        final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(visibleSceneIndex);
        track.selectInMixer();
        slot.select();
        slot.launch();
        showValueInfo("Clip Record", slotLabel(manualRecordingTrackIndex, manualRecordingSceneIndex));
        return true;
    }

    private void handleSlotRecordingChanged(final int sourceTrackIndex, final int visibleSceneIndex,
                                            final boolean recording) {
        if (!manualRecordingPending) {
            return;
        }
        final int absoluteTrackIndex = trackBank.scrollPosition().get() + sourceTrackIndex;
        final int absoluteSceneIndex = trackBank.sceneBank().scrollPosition().get() + visibleSceneIndex;
        if (absoluteTrackIndex != manualRecordingTrackIndex || absoluteSceneIndex != manualRecordingSceneIndex) {
            return;
        }
        if (recording) {
            manualRecordingWasRecording = true;
            return;
        }
        if (!manualRecordingWasRecording) {
            return;
        }

        manualRecordingPending = false;
        manualRecordingWasRecording = false;
        if (manualRecordingShouldRound) {
            roundRecordedClipLength(absoluteTrackIndex, absoluteSceneIndex);
        }
    }

    private void roundRecordedClipLength(final int absoluteTrackIndex, final int absoluteSceneIndex) {
        driver.getHost().scheduleTask(() -> {
            final TrackAddress trackAddress = trackAddressForAbsoluteTrack(absoluteTrackIndex);
            final int visibleSceneIndex = absoluteSceneIndex - trackBank.sceneBank().scrollPosition().get();
            if (trackAddress == null || visibleSceneIndex < 0 || visibleSceneIndex >= visibleSceneCount()) {
                showValueInfo("Round Clip", "Target off page");
                return;
            }
            final Track track = trackAddress.track();
            final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(visibleSceneIndex);
            track.selectInMixer();
            slot.select();
            driver.getHost().scheduleTask(() -> {
                final double currentLength = performCursorClip.getLoopLength().get();
                final double roundedLength = roundToNearestBar(
                        currentLength,
                        driver.getTransportTimeSignatureNumerator(),
                        driver.getTransportTimeSignatureDenominator());
                performCursorClip.getLoopLength().set(roundedLength);
                showValueInfo("Round Clip", formatBars(roundedLength));
            }, 1);
        }, 50);
    }

    private void handleDuplicatePressed(final boolean pressed) {
        if (!pressed) {
            oled.clearScreenDelayed();
            return;
        }
        if (sceneActionMode) {
            showValueInfo("Scene Launch", "MUTE_2 unused");
            return;
        }
        final ClipLauncherSlot slot = getSelectedVisibleSlot();
        if (slot == null || !slot.exists().get()) {
            showValueInfo("Duplicate Clip", "Select visible clip");
            return;
        }

        final TrackAddress trackAddress = trackAddressForAbsoluteTrack(selectedTrackIndex);
        final int visibleSceneIndex = selectedSceneIndex - trackBank.sceneBank().scrollPosition().get();
        if (trackAddress == null || visibleSceneIndex < 0 || visibleSceneIndex >= visibleSceneCount()) {
            showValueInfo("Duplicate Clip", "Selected clip off page");
            return;
        }

        final Track track = trackAddress.track();
        track.selectInMixer();
        slot.select();
        driver.getHost().scheduleTask(() -> {
            final double currentLength = performCursorClip.getLoopLength().get();
            if (currentLength <= 0) {
                showValueInfo("Duplicate Clip", "No clip length");
                return;
            }
            if (isShiftHeld()) {
                if (currentLength <= MIN_DUPLICATE_CLIP_LENGTH) {
                    showValueInfo("Clip Length", formatBars(MIN_DUPLICATE_CLIP_LENGTH));
                    return;
                }
                final double newLength = Math.max(currentLength / 2.0, MIN_DUPLICATE_CLIP_LENGTH);
                performCursorClip.getLoopLength().set(newLength);
                showValueInfo("Clip Length", formatBars(newLength));
                return;
            }
            if (currentLength >= MAX_DUPLICATE_CLIP_LENGTH) {
                showValueInfo("Clip Length", formatBars(MAX_DUPLICATE_CLIP_LENGTH));
                return;
            }
            final double newLength = Math.min(currentLength * 2.0, MAX_DUPLICATE_CLIP_LENGTH);
            if (slot.hasContent().get()) {
                performCursorClip.duplicateContent();
            }
            performCursorClip.getLoopLength().set(newLength);
            showValueInfo("Clip Length", formatBars(newLength));
        }, 1);
    }

    private void handleTrackScroll(final boolean pressed, final int direction) {
        if (driver.handleGlobalUndoRedoBankButton(pressed, direction)) {
            return;
        }
        if (pressed) {
            suppressMixMeterDisplay();
        }
        if (sceneActionMode) {
            handleSceneScroll(pressed, direction);
            return;
        }
        if (!pressed || !canScrollTracks(direction)) {
            return;
        }
        final int increment = trackScrollAmount();
        final int current = trackBank.scrollPosition().get();
        final int next = clamp(current + (direction * increment), 0, maxTrackOffset());
        if (next != current) {
            trackBank.scrollPosition().set(next);
            showValueInfo("Launcher Tracks", offsetLabel(next, totalTrackCount, visibleTrackCount()));
        }
    }

    private void handlePatternSceneScroll(final boolean pressed, final int direction) {
        if (trackActionMode) {
            if (pressed) {
                if (mixDeviceToggleMode && isAltHeld()) {
                    handleRemotePageNavigation(direction);
                    return;
                }
                if (direction > 0) {
                    if (!mixDeviceToggleMode) {
                        setMixDeviceMode(true);
                    } else if (mixDevicePageIndex < MIX_DEVICE_PAGE_COUNT - 1) {
                        setMixDevicePage(mixDevicePageIndex + 1);
                    }
                } else if (direction < 0 && mixDeviceToggleMode) {
                    if (mixDevicePageIndex > 0) {
                        setMixDevicePage(mixDevicePageIndex - 1);
                    } else {
                        setMixDeviceMode(false);
                    }
                }
            }
            return;
        }
        handleSceneScroll(pressed, direction);
    }

    private void setMixDeviceMode(final boolean enabled) {
        if (enabled == mixDeviceToggleMode) {
            return;
        }
        suppressMixMeterDisplay();
        if (enabled) {
            mixDevicePageIndex = 0;
            encoderModeBeforeMixDeviceMode = encoderMode;
            switchEncoderMode(EncoderMode.USER_2, false);
        } else {
            leaveMixDeviceMode();
        }
        mixDeviceToggleMode = enabled;
        showTrackActionInfo();
        oled.clearScreenDelayed();
    }

    private void leaveMixDeviceMode() {
        if (encoderModeBeforeMixDeviceMode != null) {
            switchEncoderMode(encoderModeBeforeMixDeviceMode, false);
            encoderModeBeforeMixDeviceMode = null;
        }
        mixDevicePageIndex = 0;
        mixDeviceToggleMode = false;
    }

    private void setMixDevicePage(final int pageIndex) {
        final int nextPage = clamp(pageIndex, 0, MIX_DEVICE_PAGE_COUNT - 1);
        if (nextPage == mixDevicePageIndex) {
            return;
        }
        suppressMixMeterDisplay();
        mixDevicePageIndex = nextPage;
        showTrackActionInfo();
        oled.clearScreenDelayed();
    }

    private void handleSceneScroll(final boolean pressed, final int direction) {
        if (pressed) {
            suppressMixMeterDisplay();
        }
        if (!pressed || !canScrollScenes(direction)) {
            return;
        }
        final int increment = sceneScrollAmount();
        final int current = trackBank.sceneBank().scrollPosition().get();
        final int next = clamp(current + (direction * increment), 0, maxSceneOffset());
        if (next != current) {
            trackBank.sceneBank().scrollPosition().set(next);
            showValueInfo("Launcher Scenes", offsetLabel(next, totalSceneCount, visibleSceneCount()));
        }
    }

    private void handleModeAdvance(final boolean pressed) {
        if (!pressed) {
            oled.clearScreenDelayed();
            return;
        }
        if (selectHeld.get()) {
            showCurrentModeInfo();
            return;
        }
        if (isSettingsHeld()) {
            showOverview();
            return;
        }
        switchMode(nextMode());
    }

    private void handleMainEncoder(final int inc) {
        if (driver.isPopupBrowserActive()) {
            driver.routeBrowserMainEncoder(inc);
            return;
        }
        driver.markMainEncoderTurned();
        suppressMixMeterDisplay();
        if (driver.handleMainEncoderGlobalChord(inc)) {
            return;
        }
        if (isAltHeld()) {
            handleRemotePageNavigation(inc);
            return;
        }

        final boolean fine = isShiftHeld();
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (AkaiFireOikontrolExtension.MAIN_ENCODER_TEMPO_ROLE.equals(mainEncoderRole)) {
            driver.adjustTempo(inc, fine);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_SHUFFLE_ROLE.equals(mainEncoderRole)) {
            driver.adjustGrooveShuffleAmount(inc, fine);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TRACK_SELECT_ROLE.equals(mainEncoderRole)) {
            driver.adjustSelectedTrack(inc, driver.isMainEncoderPressed());
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_PLAYBACK_START_ROLE.equals(mainEncoderRole)) {
            driver.adjustPlaybackStartPositionByGrid(inc);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            showValueInfo("Note Repeat", "Unavailable");
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_DRUM_GRID_ROLE.equals(mainEncoderRole)) {
            showValueInfo("Drum Grid", "Drum only");
        } else {
            driver.adjustMainCursorParameter(inc, fine);
        }
    }

    private void handleMainEncoderPress(final boolean pressed) {
        if (driver.isPopupBrowserActive()) {
            driver.routeBrowserMainEncoderPress(pressed);
            return;
        }
        suppressMixMeterDisplay();
        driver.setMainEncoderPressed(pressed);
        if (pressed && isAltHeld()) {
            mainEncoderPressConsumed = true;
            driver.toggleCurrentDeviceWindow();
            return;
        }
        if (pressed && isShiftHeld()) {
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
        if (AkaiFireOikontrolExtension.MAIN_ENCODER_TEMPO_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showTempoInfo();
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_SHUFFLE_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showGrooveShuffleInfo();
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TRACK_SELECT_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showSelectedTrackInfo(false);
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_PLAYBACK_START_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                showValueInfo("Play Start", "Grid step");
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                showValueInfo("Note Repeat", "Unavailable");
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_DRUM_GRID_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                showValueInfo("Drum Grid", "Drum only");
            } else {
                oled.clearScreenDelayed();
            }
        } else if (pressed) {
            driver.showMainCursorParameterInfo();
        }
    }

    private void handleRemotePageNavigation(final int inc) {
        final CursorRemoteControlsPage page = remotePageForCurrentEncoderMode();
        final String label = remotePageLabelForCurrentEncoderMode();
        if (page == null || inc == 0) {
            showValueInfo("Remote Page", "No page");
            return;
        }

        final int pageCount = page.pageCount().getAsInt();
        if (pageCount <= 0) {
            showValueInfo(label + " Page", "No Pages");
            return;
        }
        final int currentPage = page.selectedPageIndex().get();
        final int nextPage = remotePageIndexAfterTurn(currentPage, pageCount, inc);
        if (nextPage != currentPage) {
            page.selectedPageIndex().set(nextPage);
        }
        showValueInfo(label + " Page", remotePageName(page, nextPage));
    }

    private CursorRemoteControlsPage remotePageForCurrentEncoderMode() {
        return switch (encoderMode) {
            case CHANNEL -> projectRemoteControls;
            case USER_1 -> trackRemoteControls;
            case USER_2 -> deviceRemoteControls;
            case MIXER -> null;
        };
    }

    private String remotePageLabelForCurrentEncoderMode() {
        return switch (encoderMode) {
            case CHANNEL -> "Project";
            case USER_1 -> "Track";
            case USER_2 -> "Device";
            case MIXER -> "Mixer";
        };
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

    private void switchMode(final EncoderMode newMode) {
        switchEncoderMode(newMode, true);
    }

    private void switchEncoderMode(final EncoderMode newMode, final boolean showInfo) {
        encoderMode = newMode;
        resetSelectedTrackMeterText();
        currentEncoderLayer.deactivate();
        currentEncoderLayer = modeMapping.get(newMode);
        applyEncoderStepSizes();
        currentEncoderLayer.activate();
        if (showInfo) {
            if (isSettingsHeld()) {
                showOverview();
            } else {
                showCurrentModeInfo();
            }
        }
        oled.clearScreenDelayed();
    }

    private void applyEncoderStepSizes() {
        for (final TouchEncoder encoder : driver.getEncoders()) {
            encoder.setStepSize(MixerEncoderProfile.STEP_SIZE);
        }
    }

    private EncoderMode nextMode() {
        return switch (encoderMode) {
            case CHANNEL -> EncoderMode.MIXER;
            case MIXER -> EncoderMode.USER_1;
            case USER_1 -> EncoderMode.USER_2;
            case USER_2 -> EncoderMode.CHANNEL;
        };
    }

    private BiColorLightState modeLightState() {
        return encoderMode.getState();
    }

    private String modeInfo(final EncoderMode mode) {
        if (isSettingsHeld()) {
            return "1: Root Key\n2: Scale\n3: Octave\n4: Global";
        }
        return switch (mode) {
            case CHANNEL -> "1: Remote 1\n2: Remote 2\n3: Remote 3\n4: Remote 4";
            case MIXER -> "1: Volume\n2: Pan\n3: Send 1\n4: Send 2";
            case USER_1 -> "1: Track Remote 1\n2: Track Remote 2\n3: Track Remote 3\n4: Track Remote 4";
            case USER_2 -> "1: Device Remote 1\n2: Device Remote 2\n3: Device Remote 3\n4: Device Remote 4";
        };
    }

    private String modeTitle(final EncoderMode mode) {
        if (isSettingsHeld()) {
            return "Settings";
        }
        return switch (mode) {
            case CHANNEL -> "Global Remotes";
            case MIXER -> "Mixer";
            case USER_1 -> "Track Remotes";
            case USER_2 -> "Device Remotes";
        };
    }

    private void showCurrentModeInfo() {
        if (sceneActionMode) {
            showTransientDetailInfo("Scene Launch", "Top row: Launch\nM1 Select  M3 Copy\nM4 Delete",
                    METER_MODE_INFO_SUPPRESS_MS);
            return;
        }
        showTransientDetailInfo(modeTitle(encoderMode), modeInfo(encoderMode), METER_MODE_INFO_SUPPRESS_MS);
    }

    private void showTrackActionInfo() {
        if (mixDeviceToggleMode) {
            showTransientDetailInfo(mixDevicePageTitle(),
                    "Rows: %s\nPad: Select  ALT: On/Off\nPattern: Page/Mix".formatted(mixDevicePageRangeLabel()),
                    METER_MODE_INFO_SUPPRESS_MS);
            return;
        }
        showTransientDetailInfo("Mix",
                "Rows: %s/%s/%s/%s\nPattern Down: Devices\nM1 Start  M2 SoloClr\nM3 MuteClr  M4 End".formatted(
                TrackActionRow.SELECT.label,
                TrackActionRow.SOLO.label,
                TrackActionRow.MUTE.label,
                TrackActionRow.ARM.label), METER_MODE_INFO_SUPPRESS_MS);
    }

    private String mixDevicePageTitle() {
        return "Mix Devices " + mixDevicePageRangeLabel();
    }

    private String mixDevicePageRangeLabel() {
        final int first = (mixDevicePageIndex * MIX_DEVICE_ROWS) + 1;
        final int last = first + MIX_DEVICE_ROWS - 1;
        return first + "-" + last;
    }

    private void showPerformMeterDisplay() {
        if (encoderMode == EncoderMode.MIXER) {
            showSelectedTrackMeterDisplay();
        } else {
            showVisibleTrackMeterDisplay();
        }
    }

    private void showVisibleTrackMeterDisplay() {
        resetSelectedTrackMeterText();
        trackPeakHoldMeters.decay();
        oled.sendImage(OledMeterRenderer.verticalMeters(visibleTrackMeterValues(), visibleTrackPeakHoldValues(),
                visibleTrackMutedValues(), visibleTrackCount()));
    }

    private void showSelectedTrackMeterDisplay() {
        TrackAddress trackAddress = selectedMeterTrackAddress();
        if (trackAddress == null) {
            trackAddress = firstVisibleTrackAddress();
        }
        if (trackAddress == null) {
            resetSelectedTrackMeterText();
            oled.clearScreen();
            oled.detailInfo("Mixer RMS", "No visible track");
            return;
        }

        final int source = trackAddress.sourceIndex();
        final int currentRms = trackRmsMeters[source];
        final int maxRms = isSelectedMeterTrack(trackAddress) ? selectedTrackRmsMax : currentRms;
        final int currentPeak = trackPeakMeters[source];
        final int maxPeak = isSelectedMeterTrack(trackAddress) ? selectedTrackPeakMax : currentPeak;
        if (!selectedTrackMeterTextInitialized) {
            clearSelectedTrackMeterRows();
            oled.sendString(0, OledDisplay.TextJustification.LEFT, 0, SELECTED_TRACK_METER_LEGEND);
            showMixerEncoderFooter();
            selectedTrackMeterTextInitialized = true;
        }
        oled.sendString(2, OledDisplay.TextJustification.LEFT, 1,
                VuMeterFormatter.meterPairLine(maxPeak, maxRms));
        oled.sendString(2, OledDisplay.TextJustification.LEFT, 4,
                VuMeterFormatter.meterPairLine(currentPeak, currentRms));
    }

    private int[] visibleTrackMeterValues() {
        final int[] values = new int[visibleTrackCount()];
        for (int visibleTrackIndex = 0; visibleTrackIndex < visibleTrackCount(); visibleTrackIndex++) {
            final TrackAddress trackAddress = trackAddressForVisibleTrack(visibleTrackIndex);
            values[visibleTrackIndex] = trackAddress == null ? 0 : trackRmsMeters[trackAddress.sourceIndex()];
        }
        return values;
    }

    private int[] visibleTrackPeakHoldValues() {
        final int[] values = new int[visibleTrackCount()];
        for (int visibleTrackIndex = 0; visibleTrackIndex < visibleTrackCount(); visibleTrackIndex++) {
            final TrackAddress trackAddress = trackAddressForVisibleTrack(visibleTrackIndex);
            values[visibleTrackIndex] = trackAddress == null ? 0 : trackPeakHoldMeters.valueAt(trackAddress.sourceIndex());
        }
        return values;
    }

    private boolean[] visibleTrackMutedValues() {
        final boolean[] values = new boolean[visibleTrackCount()];
        for (int visibleTrackIndex = 0; visibleTrackIndex < visibleTrackCount(); visibleTrackIndex++) {
            final TrackAddress trackAddress = trackAddressForVisibleTrack(visibleTrackIndex);
            values[visibleTrackIndex] = trackAddress != null && isMutedForMeter(trackAddress.track());
        }
        return values;
    }

    private TrackAddress selectedMeterTrackAddress() {
        if (selectedMeterSourceIndex >= 0) {
            final int visibleIndex = visibleTrackIndexForSourceTrack(selectedMeterSourceIndex);
            final TrackAddress address = trackAddress(visibleIndex, selectedMeterSourceIndex);
            if (address != null && isSelectedMeterTrack(address)) {
                return address;
            }
        }
        if (selectedTrackIndex >= 0) {
            return trackAddressForAbsoluteTrack(selectedTrackIndex);
        }
        return null;
    }

    private TrackAddress firstVisibleTrackAddress() {
        for (int visibleTrackIndex = 0; visibleTrackIndex < visibleTrackCount(); visibleTrackIndex++) {
            final TrackAddress trackAddress = trackAddressForVisibleTrack(visibleTrackIndex);
            if (trackAddress != null) {
                return trackAddress;
            }
        }
        return null;
    }

    private void showValueInfo(final String title, final String value) {
        if (shouldPreserveMixerEncoderFooter()) {
            clearRowsAboveMixerEncoderFooter();
            selectedTrackMeterTextInitialized = false;
        } else {
            resetSelectedTrackMeterText();
            oled.clearScreen();
        }
        oled.valueInfoNoClear(title, value);
    }

    public boolean showGlobalActionInfo(final String title, final String value) {
        if (!active || encoderMode != EncoderMode.MIXER) {
            return false;
        }
        suppressMixMeterDisplay();
        showValueInfo(title, value);
        return true;
    }

    private boolean shouldPreserveMixerEncoderFooter() {
        return active && encoderMode == EncoderMode.MIXER && mixerEncoderFooterVisible
                && !sceneActionMode && !isSettingsHeld();
    }

    private void clearSelectedTrackMeterRows() {
        if (mixerEncoderFooterVisible) {
            clearRowsAboveMixerEncoderFooter();
            return;
        }
        oled.clearScreen();
    }

    private void clearRowsAboveMixerEncoderFooter() {
        oled.sendString(2, OledDisplay.TextJustification.LEFT, 0, BLANK_TEXT_ROW);
        oled.sendString(2, OledDisplay.TextJustification.LEFT, 1, BLANK_TEXT_ROW);
        oled.sendString(3, OledDisplay.TextJustification.LEFT, 2, BLANK_TEXT_ROW);
        oled.sendString(2, OledDisplay.TextJustification.LEFT, 4, BLANK_TEXT_ROW);
        for (int row = 0; row < 7; row++) {
            oled.sendString(0, OledDisplay.TextJustification.LEFT, row, BLANK_TEXT_ROW);
        }
    }

    private void showMixerEncoderFooter() {
        if (mixerEncoderFooterVisible) {
            return;
        }
        oled.sendString(0, OledDisplay.TextJustification.LEFT, 7, MIXER_ENCODER_FOOTER);
        mixerEncoderFooterVisible = true;
    }

    private void resetSelectedTrackMeterText() {
        selectedTrackMeterTextInitialized = false;
        mixerEncoderFooterVisible = false;
    }

    private void showTransientValueInfo(final String title, final String value) {
        suppressMixMeterDisplay();
        showValueInfo(title, value);
    }

    private void showTransientDetailInfo(final String title, final String lines) {
        showTransientDetailInfo(title, lines, METER_DISPLAY_SUPPRESS_MS);
    }

    private void showTransientDetailInfo(final String title, final String lines, final long suppressMs) {
        suppressMixMeterDisplay(suppressMs);
        resetSelectedTrackMeterText();
        oled.clearScreen();
        oled.detailInfo(title, lines);
    }

    private void clearTransientDisplayDelayed() {
        suppressMixMeterDisplay();
        oled.clearScreenDelayed();
    }

    private void suppressMixMeterDisplay() {
        suppressMixMeterDisplay(METER_DISPLAY_SUPPRESS_MS);
    }

    private void suppressMixMeterDisplay(final long suppressMs) {
        mixMeterDisplayActive = false;
        mixMeterSuppressedUntilMs = System.currentTimeMillis() + suppressMs;
    }

    private void adjustRemoteParameter(final int encoderIndex, final Parameter parameter, final String fallbackLabel,
                                       final int inc) {
        if (!ParameterEncoderBinding.isMapped(parameter)) {
            return;
        }
        parameterResetHandler.markAdjusted(encoderIndex, Math.abs(inc));
        ParameterEncoderBinding.adjustParameter(parameter, isShiftHeld(), inc);
        ParameterEncoderBinding.showValue(parameter, fallbackLabel, this::showTransientValueInfo);
    }

    private void handleRemoteParameterTouch(final int encoderIndex, final Parameter parameter,
                                            final String fallbackLabel, final boolean touched) {
        if (touched) {
            parameterResetHandler.beginTouchReset(encoderIndex, () -> {
                if (ParameterEncoderBinding.isMapped(parameter)) {
                    parameter.reset();
                    ParameterEncoderBinding.showValue(parameter, fallbackLabel, this::showTransientValueInfo);
                }
            });
            if (!ParameterEncoderBinding.isMapped(parameter)) {
                showTransientValueInfo(fallbackLabel, "Unmapped");
                return;
            }
            ParameterEncoderBinding.showValue(parameter, fallbackLabel, this::showTransientValueInfo);
            return;
        }

        parameterResetHandler.endTouchReset(encoderIndex);
        clearTransientDisplayDelayed();
    }

    private void markParameterInterested(final Parameter parameter) {
        if (parameter == null) {
            return;
        }
        ParameterEncoderBinding.markInterested(parameter);
    }

    private void markPageParametersInterested(final CursorRemoteControlsPage page) {
        for (int i = 0; i < page.getParameterCount(); i++) {
            markParameterInterested(page.getParameter(i));
        }
    }

    private void markRemotePageInterested(final CursorRemoteControlsPage page) {
        page.selectedPageIndex().markInterested();
        page.pageCount().markInterested();
        page.pageNames().markInterested();
        page.getName().markInterested();
    }

    private ParameterEncoderBinding.ResetPolicy mixerResetPolicy(final int index) {
        return index == 0
                ? ParameterEncoderBinding.ResetPolicy.NONE
                : ParameterEncoderBinding.ResetPolicy.ORIGIN;
    }

    private int trackScrollAmount() {
        return layout.trackScrollAmount(isShiftHeld());
    }

    private int sceneScrollAmount() {
        return layout.sceneScrollAmount(isShiftHeld());
    }

    private boolean isAltHeld() {
        return driver.isGlobalAltHeld();
    }

    static int remoteParameterIndex(final int encoderIndex, final boolean altHeld) {
        return (altHeld ? 4 : 0) + encoderIndex;
    }

    static double roundToNearestBar(final double beatLength, final int meterNumerator, final int meterDenominator) {
        final int numerator = Math.max(1, meterNumerator);
        final int denominator = Math.max(1, meterDenominator);
        final double barLength = numerator * (4.0 / denominator);
        if (Double.isNaN(beatLength) || Double.isInfinite(beatLength) || beatLength <= 0) {
            return barLength;
        }
        return Math.max(barLength, Math.round(beatLength / barLength) * barLength);
    }

    static int remotePageIndexAfterTurn(final int currentPage, final int pageCount, final int inc) {
        if (pageCount <= 0) {
            return currentPage;
        }
        return clamp(currentPage + inc, 0, pageCount - 1);
    }

    static String trackControlActionForPad(final int padIndex, final boolean altHeld) {
        final TrackActionRow actionRow = TrackActionRow.fromPadIndex(padIndex);
        if (actionRow == null) {
            return "";
        }
        if (actionRow == TrackActionRow.SELECT && altHeld) {
            return "Stop";
        }
        return actionRow.label;
    }

    private boolean canScrollTracks(final int direction) {
        final int current = trackBank.scrollPosition().get();
        return direction < 0 ? current > 0 : current < maxTrackOffset();
    }

    private boolean canScrollScenes(final int direction) {
        final int current = trackBank.sceneBank().scrollPosition().get();
        return direction < 0 ? current > 0 : current < maxSceneOffset();
    }

    private boolean canScrollBankLeftRight(final int direction) {
        return sceneActionMode ? canScrollScenes(direction) : canScrollTracks(direction);
    }

    private int maxTrackOffset() {
        return layout.maxTrackOffset(totalTrackCount);
    }

    private int maxSceneOffset() {
        return layout.maxSceneOffset(totalSceneCount);
    }

    private boolean isShiftHeld() {
        return driver.isGlobalShiftHeld();
    }

    private boolean isSettingsHeld() {
        return false;
    }

    private ClipLauncherSlot getSelectedVisibleSlot() {
        if (selectedTrackIndex < 0 || selectedSceneIndex < 0) {
            return null;
        }
        final int sceneOffset = trackBank.sceneBank().scrollPosition().get();
        final TrackAddress trackAddress = trackAddressForAbsoluteTrack(selectedTrackIndex);
        final int visibleSceneIndex = selectedSceneIndex - sceneOffset;
        if (trackAddress == null || visibleSceneIndex < 0 || visibleSceneIndex >= visibleSceneCount()) {
            return null;
        }
        return trackAddress.track().clipLauncherSlotBank().getItemAt(visibleSceneIndex);
    }

    private int resolveSceneCopySource() {
        final int sceneOffset = trackBank.sceneBank().scrollPosition().get();
        final int selectedVisibleSceneIndex = selectedSceneActionIndex - sceneOffset;
        if (selectedVisibleSceneIndex >= 0 && selectedVisibleSceneIndex < MAX_SCENES) {
            return selectedVisibleSceneIndex;
        }
        for (int sceneIndex = 0; sceneIndex < MAX_SCENES; sceneIndex++) {
            if (sceneHasPlayingClip(sceneIndex)) {
                return sceneIndex;
            }
        }
        for (int sceneIndex = 0; sceneIndex < MAX_SCENES; sceneIndex++) {
            if (sceneHasRecordingClip(sceneIndex)) {
                return sceneIndex;
            }
        }
        return -1;
    }

    private boolean sceneHasPlayingClip(final int visibleSceneIndex) {
        for (int trackIndex = 0; trackIndex < MAX_TRACKS; trackIndex++) {
            final Track track = trackBank.getItemAt(trackIndex);
            if (isControllableTrack(track) && track.clipLauncherSlotBank().getItemAt(visibleSceneIndex).isPlaying().get()) {
                return true;
            }
        }
        return false;
    }

    private boolean sceneHasRecordingClip(final int visibleSceneIndex) {
        for (int trackIndex = 0; trackIndex < MAX_TRACKS; trackIndex++) {
            final Track track = trackBank.getItemAt(trackIndex);
            if (isControllableTrack(track) && track.clipLauncherSlotBank().getItemAt(visibleSceneIndex).isRecording().get()) {
                return true;
            }
        }
        return false;
    }

    public void notifyBlink(final int blinkState) {
        this.blinkState = blinkState;
        clearPendingSceneLaunchIfPlaying();
        refreshMixMetersIfVisible(blinkState);
    }

    public boolean showIdleInfoIfNeeded() {
        if (!shouldShowPerformMeters()) {
            return false;
        }
        showPerformMeterDisplay();
        mixMeterDisplayActive = true;
        lastMeterDisplayBlink = blinkState;
        return true;
    }

    private void refreshMixMetersIfVisible(final int blinkState) {
        if (!shouldShowPerformMeters()) {
            mixMeterDisplayActive = false;
            return;
        }
        if (System.currentTimeMillis() < mixMeterSuppressedUntilMs) {
            return;
        }
        if (!mixMeterDisplayActive || blinkState - lastMeterDisplayBlink >= METER_REFRESH_TICKS) {
            showPerformMeterDisplay();
            mixMeterDisplayActive = true;
            lastMeterDisplayBlink = blinkState;
        }
    }

    private boolean shouldShowPerformMeters() {
        return active && !sceneActionMode && !isSettingsHeld();
    }

    private void clearPendingSceneLaunchIfPlaying() {
        if (pendingSceneLaunchIndex < 0) {
            return;
        }
        final int visibleSceneIndex = pendingSceneLaunchIndex - trackBank.sceneBank().scrollPosition().get();
        if (visibleSceneIndex < 0 || visibleSceneIndex >= MAX_SCENES) {
            pendingSceneLaunchIndex = -1;
            return;
        }
        if (sceneHasPlayingClip(visibleSceneIndex)) {
            pendingSceneLaunchIndex = -1;
        }
    }

    @Override
    protected void onActivate() {
        active = true;
        applyEncoderStepSizes();
        currentEncoderLayer.activate();
        if (isSettingsHeld()) {
            showOverview();
        } else {
            showCurrentModeInfo();
        }
    }

    @Override
    protected void onDeactivate() {
        active = false;
        mixMeterDisplayActive = false;
        resetSelectedTrackMeterText();
        currentEncoderLayer.deactivate();
    }

    private RgbLigthState getPadState(final int padIndex) {
        if (isSettingsHeld()) {
            return settingsLogoState(padIndex);
        }
        if (trackActionMode) {
            return mixDeviceToggleMode ? getMixDevicePadState(padIndex) : getTrackActionPadState(padIndex);
        }
        if (sceneActionMode) {
            return getSceneActionPadState(padIndex);
        }
        final int visibleTrackIndex = visibleTrackIndexForPad(padIndex);
        final int visibleSceneIndex = visibleSceneIndexForPad(padIndex);
        if (visibleTrackIndex < 0 || visibleSceneIndex < 0
                || visibleTrackIndex >= visibleTrackCount()
                || visibleSceneIndex >= visibleSceneCount()) {
            return RgbLigthState.OFF;
        }
        final TrackAddress trackAddress = trackAddressForVisibleTrack(visibleTrackIndex);
        if (trackAddress == null) {
            return RgbLigthState.OFF;
        }
        final ClipLauncherSlot slot = trackAddress.track().clipLauncherSlotBank().getItemAt(visibleSceneIndex);
        return getSlotState(slot, trackAddress, visibleSceneIndex);
    }

    private RgbLigthState getSceneActionPadState(final int padIndex) {
        if (padIndex / PerformLayout.PAD_COLUMNS != SCENE_ROW) {
            return RgbLigthState.OFF;
        }
        final int visibleSceneIndex = padIndex % PerformLayout.PAD_COLUMNS;
        final int absoluteSceneIndex = trackBank.sceneBank().scrollPosition().get() + visibleSceneIndex;
        if (visibleSceneIndex >= MAX_SCENES || absoluteSceneIndex >= totalSceneCount) {
            return RgbLigthState.OFF;
        }
        final Scene scene = trackBank.sceneBank().getScene(visibleSceneIndex);
        if (scene == null || !scene.exists().get()) {
            return RgbLigthState.OFF;
        }
        final RgbLigthState baseColor = sceneColor(visibleSceneIndex);
        if (absoluteSceneIndex == pendingSceneLaunchIndex) {
            return blinkFast(baseColor.getBrightest(), baseColor.getDimmed());
        }
        if (absoluteSceneIndex == selectedSceneActionIndex) {
            return baseColor.getBrightest();
        }
        if (sceneHasRecordingClip(visibleSceneIndex)) {
            return blinkFast(baseColor.getBrightest(), baseColor);
        }
        if (sceneHasPlayingClip(visibleSceneIndex)) {
            return blinkSlow(baseColor, baseColor.getDimmed());
        }
        return baseColor.getSoftDimmed();
    }

    private RgbLigthState getSlotState(final ClipLauncherSlot slot, final TrackAddress trackAddress, final int visibleSceneIndex) {
        if (isSettingsHeld()) {
            return settingsLogoState(toPadIndex(trackAddress.visibleIndex(), visibleSceneIndex));
        }
        if (!slot.exists().get()) {
            return RgbLigthState.OFF;
        }
        if (!slot.hasContent().get()) {
            return slot.isSelected().get() ? RgbLigthState.GRAY_2 : RgbLigthState.GRAY_1;
        }

        final int slotColorIndex = toSlotIndex(trackAddress.sourceIndex(), visibleSceneIndex);
        final RgbLigthState baseColor = slotColors[slotColorIndex] == null
                ? RgbLigthState.WHITE
                : slotColors[slotColorIndex];
        if (slot.isRecording().get()) {
            return blinkFast(baseColor.getBrightest(), baseColor);
        }
        if (slot.isRecordingQueued().get()) {
            return blinkFast(baseColor.getBrightest(), baseColor.getDimmed());
        }
        if (slot.isPlaybackQueued().get() || slot.isStopQueued().get()) {
            return blinkFast(baseColor.getBrightend(), baseColor.getDimmed());
        }
        if (slot.isPlaying().get()) {
            return slot.isSelected().get() ? blinkSlow(baseColor.getBrightest(), baseColor)
                    : blinkSlow(baseColor, baseColor.getDimmed());
        }
        return slot.isSelected().get() ? baseColor.getBrightend() : baseColor.getSoftDimmed();
    }

    private RgbLigthState sceneColor(final int visibleSceneIndex) {
        final RgbLigthState color = visibleSceneIndex >= 0 && visibleSceneIndex < sceneColors.length
                ? sceneColors[visibleSceneIndex]
                : null;
        return color == null || RgbLigthState.OFF.equals(color) ? RgbLigthState.PURPLE : color;
    }

    private RgbLigthState getTrackActionPadState(final int padIndex) {
        final TrackActionRow actionRow = TrackActionRow.fromPadIndex(padIndex);
        if (actionRow == null) {
            return RgbLigthState.OFF;
        }
        final int visibleTrackIndex = padIndex % PerformLayout.PAD_COLUMNS;
        final TrackAddress trackAddress = trackAddressForVisibleTrack(visibleTrackIndex);
        if (trackAddress == null) {
            return RgbLigthState.OFF;
        }
        final Track track = trackAddress.track();
        final RgbLigthState baseColor = actionRow == TrackActionRow.SELECT
                ? trackColor(trackAddress.sourceIndex())
                : actionRow.color;
        return switch (actionRow) {
            case SELECT -> {
                if (track.isQueuedForStop().get()) {
                    yield blinkFast(baseColor.getBrightest(), baseColor.getDimmed());
                }
                yield mixSelectPadColor(baseColor, isTrackSelected(trackAddress), track.isStopped().get());
            }
            case SOLO -> track.solo().get() ? baseColor.getBrightest() : baseColor.getDimmed();
            case MUTE -> track.mute().get() ? baseColor.getBrightest() : baseColor.getDimmed();
            case ARM -> track.arm().get() ? baseColor : baseColor.getDimmed();
        };
    }

    private RgbLigthState getMixDevicePadState(final int padIndex) {
        final int deviceIndex = mixDeviceIndexForPad(padIndex, mixDevicePageIndex);
        if (deviceIndex < 0) {
            return RgbLigthState.OFF;
        }
        final int visibleTrackIndex = padIndex % PerformLayout.PAD_COLUMNS;
        final TrackAddress trackAddress = trackAddressForVisibleTrack(visibleTrackIndex);
        if (trackAddress == null) {
            return RgbLigthState.OFF;
        }
        final Device device = mixDevice(trackAddress.sourceIndex(), deviceIndex);
        if (device == null || !device.exists().get()) {
            return RgbLigthState.OFF;
        }
        return mixDevicePadColor(trackColor(trackAddress.sourceIndex()), device.isEnabled().get(),
                isMixDeviceSelected(trackAddress, deviceIndex));
    }

    static RgbLigthState mixSelectPadColor(final RgbLigthState trackColor, final boolean selected,
                                           final boolean stopped) {
        if (selected) {
            return trackColor.getBrightest();
        }
        return stopped ? trackColor.getDimmed() : trackColor;
    }

    static int mixDeviceIndexForPad(final int padIndex, final int devicePageIndex) {
        final int row = padIndex / PerformLayout.PAD_COLUMNS;
        return mixDeviceIndexForRow(row, devicePageIndex);
    }

    static int mixDeviceIndexForRow(final int row, final int devicePageIndex) {
        return row >= 0 && row < MIX_DEVICE_ROWS
                ? (clamp(devicePageIndex, 0, MIX_DEVICE_PAGE_COUNT - 1) * MIX_DEVICE_ROWS) + row
                : -1;
    }

    static RgbLigthState mixDevicePadColor(final RgbLigthState trackColor, final boolean enabled,
                                           final boolean selected) {
        if (selected && enabled) {
            return trackColor.getBrightest();
        }
        if (selected) {
            return trackColor.getSoftDimmed();
        }
        return enabled ? trackColor : trackColor.getDimmed();
    }

    static String mixDeviceActionTitle(final boolean altHeld, final boolean enabled) {
        if (!altHeld) {
            return "Device Select";
        }
        return enabled ? "Device On" : "Device Off";
    }

    static boolean rowWideDeviceToggleTarget(final boolean anyEnabled) {
        return !anyEnabled;
    }

    static String rowWideDeviceToggleTitle(final boolean enabled) {
        return enabled ? "Device Row On" : "Device Row Off";
    }

    static void rememberMixDeviceSelection(final Map<Integer, Integer> rememberedDeviceByTrack,
                                           final int absoluteTrackIndex,
                                           final int deviceIndex) {
        if (absoluteTrackIndex < 0 || deviceIndex < 0 || deviceIndex >= MIX_DEVICE_SLOTS) {
            return;
        }
        rememberedDeviceByTrack.put(absoluteTrackIndex, deviceIndex);
    }

    static int rememberedMixDeviceSelection(final Map<Integer, Integer> rememberedDeviceByTrack,
                                            final int absoluteTrackIndex) {
        if (absoluteTrackIndex < 0) {
            return -1;
        }
        return rememberedDeviceByTrack.getOrDefault(absoluteTrackIndex, -1);
    }

    static BiColorLightState mixStatusLightState(final boolean trackActionMode,
                                                 final boolean hasSoloedTracks,
                                                 final boolean hasMutedTracks,
                                                 final int index) {
        return switch (index) {
            case 0, 3 -> BiColorLightState.OFF;
            case 1 -> trackActionMode && hasSoloedTracks ? BiColorLightState.AMBER_FULL : BiColorLightState.OFF;
            case 2 -> trackActionMode && hasMutedTracks ? BiColorLightState.AMBER_FULL : BiColorLightState.OFF;
            default -> throw new IllegalArgumentException("Unsupported mix status light index: " + index);
        };
    }

    static BiColorLightState patternSceneNavigationLightState(final boolean trackActionMode,
                                                              final boolean mixDeviceToggleMode,
                                                              final int mixDevicePageIndex,
                                                              final boolean altHeld,
                                                              final int remotePageIndex,
                                                              final int remotePageCount,
                                                              final int direction,
                                                              final boolean canScrollScenes) {
        if (trackActionMode) {
            if (mixDeviceToggleMode && altHeld) {
                return remotePageNavigationLightState(remotePageIndex, remotePageCount, direction);
            }
            if (direction > 0) {
                return mixDeviceToggleMode && mixDevicePageIndex >= MIX_DEVICE_PAGE_COUNT - 1
                        ? BiColorLightState.OFF
                        : BiColorLightState.AMBER_HALF;
            }
            if (direction < 0) {
                return mixDeviceToggleMode ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF;
            }
            return BiColorLightState.OFF;
        }
        return canScrollScenes ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF;
    }

    static BiColorLightState remotePageNavigationLightState(final int currentPage,
                                                            final int pageCount,
                                                            final int direction) {
        if (pageCount <= 0) {
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

    private RgbLigthState trackColor(final int sourceTrackIndex) {
        final RgbLigthState color = sourceTrackIndex >= 0 && sourceTrackIndex < trackColors.length
                ? trackColors[sourceTrackIndex]
                : null;
        return color == null || RgbLigthState.OFF.equals(color) ? TrackActionRow.SELECT.color : color;
    }

    private boolean isTrackSelected(final TrackAddress trackAddress) {
        return selectedVisibleTracks[trackAddress.sourceIndex()];
    }

    private boolean isMixDeviceSelected(final TrackAddress trackAddress, final int deviceIndex) {
        return remoteCursorDevice.exists().get()
                && selectedRemoteTrackIndex == trackAddress.absoluteIndex()
                && selectedRemoteDeviceIndex == deviceIndex;
    }

    private RgbLigthState settingsLogoState(final int padIndex) {
        final int row = padIndex / PerformLayout.PAD_COLUMNS;
        final int column = padIndex % PerformLayout.PAD_COLUMNS;
        if (row < 0 || row >= SETTINGS_LOGO.length || column < 0 || column >= PerformLayout.PAD_COLUMNS) {
            return RgbLigthState.OFF;
        }
        return SETTINGS_LOGO[row][column] ? SETTINGS_LOGO_ON : SETTINGS_LOGO_OFF;
    }

    private RgbLigthState blinkSlow(final RgbLigthState on, final RgbLigthState off) {
        return blinkState % 8 < 4 ? on : off;
    }

    private RgbLigthState blinkFast(final RgbLigthState on, final RgbLigthState off) {
        return blinkState % 2 == 0 ? on : off;
    }

    private String slotLabel(final int absoluteTrackIndex, final int absoluteSceneIndex) {
        final int visibleSceneIndex = absoluteSceneIndex - trackBank.sceneBank().scrollPosition().get();
        final TrackAddress trackAddress = trackAddressForAbsoluteTrack(absoluteTrackIndex);
        final String trackName = trackAddress != null
                ? nameOrFallback(trackNames[trackAddress.sourceIndex()], "Track " + (absoluteTrackIndex + 1))
                : "Track " + (absoluteTrackIndex + 1);
        final String sceneName = visibleSceneIndex >= 0 && visibleSceneIndex < MAX_SCENES
                ? nameOrFallback(sceneNames[visibleSceneIndex], "Scene " + (absoluteSceneIndex + 1))
                : "Scene " + (absoluteSceneIndex + 1);
        return trackName + " / " + sceneName;
    }

    private String sceneLabel(final int absoluteSceneIndex, final int visibleSceneIndex) {
        return visibleSceneIndex >= 0 && visibleSceneIndex < MAX_SCENES
                ? nameOrFallback(sceneNames[visibleSceneIndex], "Scene " + (absoluteSceneIndex + 1))
                : "Scene " + (absoluteSceneIndex + 1);
    }

    private String selectedSlotLabel() {
        if (selectedTrackIndex < 0 || selectedSceneIndex < 0) {
            return "None";
        }
        return slotLabel(selectedTrackIndex, selectedSceneIndex);
    }

    private String trackLabel(final TrackAddress trackAddress) {
        return nameOrFallback(trackNames[trackAddress.sourceIndex()], "Track " + (trackAddress.absoluteIndex() + 1));
    }

    private String mixDeviceName(final TrackAddress trackAddress, final int deviceIndex) {
        return nameOrFallback(trackDeviceNames[trackAddress.sourceIndex()][deviceIndex],
                "Device " + (deviceIndex + 1));
    }

    private Device mixDevice(final int sourceTrackIndex, final int deviceIndex) {
        if (sourceTrackIndex < 0 || sourceTrackIndex >= trackDeviceBanks.length
                || deviceIndex < 0 || deviceIndex >= MIX_DEVICE_SLOTS) {
            return null;
        }
        final DeviceBank deviceBank = trackDeviceBanks[sourceTrackIndex];
        return deviceBank == null ? null : deviceBank.getItemAt(deviceIndex);
    }

    private void handleRmsMeterChanged(final int sourceTrackIndex, final int value) {
        trackRmsMeters[sourceTrackIndex] = value;
        if (sourceTrackIndex == selectedMeterSourceIndex
                && trackBank.scrollPosition().get() + sourceTrackIndex == selectedMeterAbsoluteIndex) {
            selectedTrackRmsMax = Math.max(selectedTrackRmsMax, value);
        }
    }

    private void handlePeakMeterChanged(final int sourceTrackIndex, final int value) {
        trackPeakMeters[sourceTrackIndex] = value;
        trackPeakHoldMeters.update(sourceTrackIndex, value);
        if (sourceTrackIndex == selectedMeterSourceIndex
                && trackBank.scrollPosition().get() + sourceTrackIndex == selectedMeterAbsoluteIndex) {
            selectedTrackPeakMax = Math.max(selectedTrackPeakMax, value);
        }
    }

    private void selectMeterTrack(final int sourceTrackIndex, final boolean resetMax) {
        if (sourceTrackIndex < 0 || sourceTrackIndex >= MAX_TRACKS) {
            return;
        }
        final int absoluteTrackIndex = trackBank.scrollPosition().get() + sourceTrackIndex;
        final boolean changed = sourceTrackIndex != selectedMeterSourceIndex
                || absoluteTrackIndex != selectedMeterAbsoluteIndex;
        selectedMeterSourceIndex = sourceTrackIndex;
        selectedMeterAbsoluteIndex = absoluteTrackIndex;
        if (resetMax || changed) {
            selectedTrackRmsMax = trackRmsMeters[sourceTrackIndex];
            selectedTrackPeakMax = trackPeakMeters[sourceTrackIndex];
        }
    }

    private boolean isSelectedMeterTrack(final TrackAddress trackAddress) {
        return trackAddress.sourceIndex() == selectedMeterSourceIndex
                && trackAddress.absoluteIndex() == selectedMeterAbsoluteIndex;
    }

    private String offsetLabel(final int offset, final int total, final int visibleCount) {
        final int start = Math.min(total, offset + 1);
        final int end = Math.min(total, offset + visibleCount);
        return start + "-" + end + " / " + Math.max(total, visibleCount);
    }

    private String formatBars(final double beatLength) {
        final double bars = beatLength / 4.0;
        if (Math.rint(bars) == bars) {
            return Integer.toString((int) bars) + " Bars";
        }
        return String.format("%.2f Bars", bars);
    }

    private String nameOrFallback(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int visibleTrackCount() {
        return layout.visibleTrackCount();
    }

    private int visibleSceneCount() {
        return layout.visibleSceneCount();
    }

    private int visibleTrackIndexForPad(final int padIndex) {
        return layout.visibleTrackIndexForPad(padIndex);
    }

    private int visibleSceneIndexForPad(final int padIndex) {
        return layout.visibleSceneIndexForPad(padIndex);
    }

    private int toPadIndex(final int visibleTrackIndex, final int visibleSceneIndex) {
        return layout.toPadIndex(visibleTrackIndex, visibleSceneIndex);
    }

    private int toSlotIndex(final int trackIndex, final int sceneIndex) {
        return sceneIndex * MAX_TRACKS + trackIndex;
    }

    private TrackAddress trackAddressForVisibleTrack(final int visibleTrackIndex) {
        final int sourceTrackIndex = sourceTrackIndexForVisibleTrack(visibleTrackIndex);
        if (sourceTrackIndex < 0) {
            return null;
        }
        return trackAddress(visibleTrackIndex, sourceTrackIndex);
    }

    /**
     * Resolves a remembered absolute target back to the current visible perform grid. This returns
     * null when the target has scrolled away or is currently hidden by the deactivated-track filter;
     * those operations intentionally bail out instead of acting on the wrong visible pad.
     */
    private TrackAddress trackAddressForAbsoluteTrack(final int absoluteTrackIndex) {
        final int sourceTrackIndex = absoluteTrackIndex - trackBank.scrollPosition().get();
        final int visibleTrackIndex = visibleTrackIndexForSourceTrack(sourceTrackIndex);
        if (visibleTrackIndex < 0) {
            return null;
        }
        return trackAddress(visibleTrackIndex, sourceTrackIndex);
    }

    private TrackAddress trackAddress(final int visibleTrackIndex, final int sourceTrackIndex) {
        if (visibleTrackIndex < 0 || visibleTrackIndex >= visibleTrackCount()
                || sourceTrackIndex < 0 || sourceTrackIndex >= MAX_TRACKS) {
            return null;
        }
        final Track track = trackBank.getItemAt(sourceTrackIndex);
        if (!isControllableTrack(track)) {
            return null;
        }
        return new TrackAddress(
                visibleTrackIndex,
                sourceTrackIndex,
                trackBank.scrollPosition().get() + sourceTrackIndex,
                track);
    }

    private int sourceTrackIndexForVisibleTrack(final int visibleTrackIndex) {
        if (visibleTrackIndex < 0 || visibleTrackIndex >= visibleTrackCount()) {
            return -1;
        }
        int visible = 0;
        for (int sourceTrackIndex = 0; sourceTrackIndex < MAX_TRACKS; sourceTrackIndex++) {
            final Track track = trackBank.getItemAt(sourceTrackIndex);
            if (isControllableTrack(track)) {
                if (visible == visibleTrackIndex) {
                    return sourceTrackIndex;
                }
                visible++;
            }
        }
        return -1;
    }

    /**
     * Inverse lookup for the active-track compaction. Bitwig's TrackBank still contains deactivated
     * tracks, so the hardware grid builds its own compacted view when the controller preference says
     * to hide them.
     */
    private int visibleTrackIndexForSourceTrack(final int sourceTrackIndex) {
        if (sourceTrackIndex < 0 || sourceTrackIndex >= MAX_TRACKS) {
            return -1;
        }
        int visible = 0;
        for (int candidate = 0; candidate < MAX_TRACKS; candidate++) {
            final Track track = trackBank.getItemAt(candidate);
            if (isControllableTrack(track)) {
                if (candidate == sourceTrackIndex) {
                    return visible;
                }
                visible++;
            }
        }
        return -1;
    }

    private boolean isControllableTrack(final Track track) {
        return track != null && track.exists().get() && (driver.showDeactivatedTracks() || track.isActivated().get());
    }

    private boolean isMutedForMeter(final Track track) {
        return track.mute().get() || track.isMutedBySolo().get();
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}
