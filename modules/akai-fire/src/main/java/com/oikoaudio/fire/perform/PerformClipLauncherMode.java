package com.oikoaudio.fire.perform;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.MixerEncoderProfile;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.control.TouchResetGesture;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.EncoderMode;

import java.util.HashMap;
import java.util.Map;

public class PerformClipLauncherMode extends Layer {
    private static final long PARAMETER_RESET_TOUCH_HOLD_MS = 1000;
    private static final long PARAMETER_RESET_RECENT_ADJUSTMENT_SUPPRESS_MS = 300;
    private static final int PARAMETER_RESET_TOLERATED_ADJUSTMENT_UNITS = 2;
    private static final int TRACKS = 16;
    private static final int SCENES = 4;
    private static final int DEFAULT_CLIP_LENGTH = 4;
    private static final double MIN_DUPLICATE_CLIP_LENGTH = 1.0;
    private static final double MAX_DUPLICATE_CLIP_LENGTH = 256.0;

    private final AkaiFireOikontrolExtension driver;
    private final OledDisplay oled;
    private final TrackBank trackBank;
    private final CursorTrack cursorTrack;
    private final CursorRemoteControlsPage projectRemoteControls;
    private final CursorRemoteControlsPage trackRemoteControls;
    private final CursorRemoteControlsPage deviceRemoteControls;
    private final Project project;
    private final PinnableCursorClip performCursorClip;

    private final Layer channelLayer;
    private final Layer mixerLayer;
    private final Layer user1Layer;
    private final Layer user2Layer;
    private final Map<EncoderMode, Layer> modeMapping = new HashMap<>();

    private final RgbLigthState[] slotColors = new RgbLigthState[TRACKS * SCENES];
    private final String[] trackNames = new String[TRACKS];
    private final String[] sceneNames = new String[SCENES];
    private final BooleanValueObject selectHeld = new BooleanValueObject();
    private final BooleanValueObject copyHeld = new BooleanValueObject();
    private final BooleanValueObject deleteHeld = new BooleanValueObject();
    private final TouchResetGesture parameterResetGesture =
            new TouchResetGesture(4, PARAMETER_RESET_TOUCH_HOLD_MS, PARAMETER_RESET_RECENT_ADJUSTMENT_SUPPRESS_MS,
                    PARAMETER_RESET_TOLERATED_ADJUSTMENT_UNITS);
    private Layer currentEncoderLayer;
    private EncoderMode encoderMode = EncoderMode.CHANNEL;
    private int blinkState;
    private int totalTrackCount = TRACKS;
    private int totalSceneCount = SCENES;
    private int selectedTrackIndex = -1;
    private int selectedSceneIndex = -1;

    public PerformClipLauncherMode(final AkaiFireOikontrolExtension driver) {
        super(driver.getLayers(), "PERFORM_CLIP_LAUNCHER");
        this.driver = driver;
        final ControllerHost host = driver.getHost();
        this.oled = driver.getOled();
        this.trackBank = host.createTrackBank(TRACKS, 0, SCENES);
        this.cursorTrack = driver.getViewControl().getCursorTrack();
        this.project = host.getProject();
        this.projectRemoteControls = project.getRootTrackGroup().createCursorRemoteControlsPage(8);
        this.trackRemoteControls = cursorTrack.createCursorRemoteControlsPage(8);
        final PinnableCursorDevice primaryDevice = driver.getViewControl().getPrimaryDevice();
        this.deviceRemoteControls = primaryDevice.createCursorRemoteControlsPage(8);
        this.performCursorClip = cursorTrack.createLauncherCursorClip("PERFORM_CURSOR", "PERFORM_CURSOR", 64, 128);

        this.channelLayer = new Layer(driver.getLayers(), "PERFORM_ENC_CHANNEL");
        this.mixerLayer = new Layer(driver.getLayers(), "PERFORM_ENC_MIXER");
        this.user1Layer = new Layer(driver.getLayers(), "PERFORM_ENC_USER1");
        this.user2Layer = new Layer(driver.getLayers(), "PERFORM_ENC_USER2");
        this.currentEncoderLayer = channelLayer;

        trackBank.channelCount().markInterested();
        trackBank.channelCount().addValueObserver(count -> totalTrackCount = count);
        trackBank.scrollPosition().markInterested();
        trackBank.sceneBank().itemCount().markInterested();
        trackBank.sceneBank().itemCount().addValueObserver(count -> totalSceneCount = count);
        trackBank.sceneBank().scrollPosition().markInterested();
        performCursorClip.getLoopLength().markInterested();

        initGrid();
        initEncoderPages();
        initButtons();
    }

    private void initGrid() {
        final RgbButton[] rgbButtons = driver.getRgbButtons();
        for (int sceneIndex = 0; sceneIndex < SCENES; sceneIndex++) {
            final Scene scene = trackBank.sceneBank().getScene(sceneIndex);
            scene.exists().markInterested();
            scene.name().markInterested();
            final int row = sceneIndex;
            scene.name().addValueObserver(name -> sceneNames[row] = name);
            sceneNames[row] = scene.name().get();
        }

        for (int trackIndex = 0; trackIndex < TRACKS; trackIndex++) {
            final Track track = trackBank.getItemAt(trackIndex);
            track.exists().markInterested();
            track.name().markInterested();
            final int column = trackIndex;
            track.name().addValueObserver(name -> trackNames[column] = name);
            trackNames[column] = track.name().get();

            for (int sceneIndex = 0; sceneIndex < SCENES; sceneIndex++) {
                final int padIndex = toPadIndex(trackIndex, sceneIndex);
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
                slot.color().addValueObserver((r, g, b) -> slotColors[padIndex] = ColorLookup.getColor(r, g, b));
                slot.isSelected().addValueObserver(selected -> {
                    if (selected) {
                        selectedTrackIndex = trackBank.scrollPosition().get() + column;
                        selectedSceneIndex = trackBank.sceneBank().scrollPosition().get() + row;
                    }
                });
                slotColors[padIndex] = ColorLookup.getColor(slot.color().get());
                rgbButtons[padIndex].bindPressed(this, pressed -> handleSlotPressed(track, slot, column, row, pressed),
                        () -> getSlotState(slot, padIndex));
            }
        }
    }

    private void initEncoderPages() {
        modeMapping.put(EncoderMode.CHANNEL, channelLayer);
        modeMapping.put(EncoderMode.MIXER, mixerLayer);
        modeMapping.put(EncoderMode.USER_1, user1Layer);
        modeMapping.put(EncoderMode.USER_2, user2Layer);

        bindRemotePage(channelLayer, projectRemoteControls, "Global");
        bindMixerPage(mixerLayer);
        bindRemotePage(user1Layer, trackRemoteControls, "Track");
        bindRemotePage(user2Layer, deviceRemoteControls, "Device");
    }

    private void bindRemotePage(final Layer layer, final CursorRemoteControlsPage page, final String fallbackPrefix) {
        final TouchEncoder[] encoders = driver.getEncoders();
        for (int i = 0; i < encoders.length; i++) {
            final int index = i;
            final Parameter parameter = page.getParameter(i);
            markParameterInterested(parameter);
            encoders[i].bindEncoder(layer,
                    inc -> adjustParameter(index, parameter, fallbackPrefix + " " + (index + 1), inc));
            encoders[i].bindTouched(layer, touched -> handleParameterTouch(index, parameter,
                    fallbackPrefix + " " + (index + 1), touched));
        }
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
            markParameterInterested(parameter);
            final String label = fallbackLabels[i];
            encoders[i].bindEncoder(layer, inc -> adjustParameter(index, parameter, label, inc));
            encoders[i].bindTouched(layer, touched -> handleParameterTouch(index, parameter, label, touched));
        }
    }

    private void initButtons() {
        final BiColorButton knobModeButton = driver.getButton(NoteAssign.KNOB_MODE);
        knobModeButton.bindPressed(this, this::handleModeAdvance, this::modeLightState);

        final BiColorButton bankLeft = driver.getButton(NoteAssign.BANK_L);
        bankLeft.bindPressed(this, pressed -> handleTrackScroll(pressed, -1),
                () -> canScrollTracks(-1) ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF);

        final BiColorButton bankRight = driver.getButton(NoteAssign.BANK_R);
        bankRight.bindPressed(this, pressed -> handleTrackScroll(pressed, 1),
                () -> canScrollTracks(1) ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF);

        final BiColorButton patternUp = driver.getButton(NoteAssign.PATTERN_UP);
        patternUp.bindPressed(this, pressed -> handleSceneScroll(pressed, -1),
                () -> canScrollScenes(-1) ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF);

        final BiColorButton patternDown = driver.getButton(NoteAssign.PATTERN_DOWN);
        patternDown.bindPressed(this, pressed -> handleSceneScroll(pressed, 1),
                () -> canScrollScenes(1) ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF);

        final BiColorButton selectButton = driver.getButton(NoteAssign.MUTE_1);
        bindModifierButton(selectButton, selectHeld, "Select", "Pad select", BiColorLightState.GREEN_FULL);

        final BiColorButton duplicateButton = driver.getButton(NoteAssign.MUTE_2);
        duplicateButton.bindPressed(this, this::handleDuplicatePressed,
                () -> duplicateButton.isPressed() ? BiColorLightState.AMBER_FULL : BiColorLightState.OFF);

        final BiColorButton copyButton = driver.getButton(NoteAssign.MUTE_3);
        copyButton.bindPressed(this, pressed -> {
            copyHeld.set(pressed);
            if (!pressed) {
                oled.clearScreenDelayed();
                return;
            }
            final ClipLauncherSlot source = getSelectedVisibleSlot();
            if (source == null || !source.exists().get() || !source.hasContent().get()) {
                oled.valueInfo("Copy Clip", "Select source first");
                return;
            }
            oled.valueInfo("Paste sel", "Pad target");
        }, () -> copyButton.isPressed() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF);

        final BiColorButton deleteButton = driver.getButton(NoteAssign.MUTE_4);
        bindModifierButton(deleteButton, deleteHeld, "Delete", "Pad delete", BiColorLightState.RED_FULL);
    }

    private void bindModifierButton(final BiColorButton button, final BooleanValueObject heldState,
                                    final String functionName, final String detail,
                                    final BiColorLightState activeColor) {
        button.bindPressed(this, pressed -> {
            heldState.set(pressed);
            if (pressed) {
                oled.valueInfo(functionName, detail);
            } else {
                oled.clearScreenDelayed();
            }
        }, () -> button.isPressed() ? activeColor : BiColorLightState.OFF);
    }

    private void handleSlotPressed(final Track track, final ClipLauncherSlot slot, final int visibleTrackIndex,
                                   final int visibleSceneIndex, final boolean pressed) {
        if (!pressed || !track.exists().get() || !slot.exists().get()) {
            return;
        }

        final int absoluteTrackIndex = trackBank.scrollPosition().get() + visibleTrackIndex;
        final int absoluteSceneIndex = trackBank.sceneBank().scrollPosition().get() + visibleSceneIndex;
        final boolean hasContent = slot.hasContent().get();

        if (deleteHeld.get()) {
            if (hasContent) {
                slot.deleteObject();
                oled.valueInfo("Delete Clip", slotLabel(absoluteTrackIndex, absoluteSceneIndex));
            }
            return;
        }

        if (copyHeld.get()) {
            final ClipLauncherSlot source = getSelectedVisibleSlot();
            if (source != null && (selectedTrackIndex != absoluteTrackIndex || selectedSceneIndex != absoluteSceneIndex)) {
                slot.replaceInsertionPoint().copySlotsOrScenes(source);
                final String sourceLabel = selectedSlotLabel();
                final String destinationLabel = slotLabel(absoluteTrackIndex, absoluteSceneIndex);
                oled.valueInfo("Copy Clip", "Select target");
                driver.notifyPopup("Copy Clip", sourceLabel + " -> " + destinationLabel);
            } else {
                oled.valueInfo("Copy Clip", "Select source first");
            }
            return;
        }

        track.selectInMixer();
        slot.select();

        if (selectHeld.get()) {
            oled.valueInfo("Select Clip", slotLabel(absoluteTrackIndex, absoluteSceneIndex));
            return;
        }

        if (hasContent) {
            slot.launch();
            oled.valueInfo("Launch Clip", slotLabel(absoluteTrackIndex, absoluteSceneIndex));
            return;
        }

        slot.createEmptyClip(DEFAULT_CLIP_LENGTH);
        slot.launch();
        oled.valueInfo("Create Clip", slotLabel(absoluteTrackIndex, absoluteSceneIndex));
    }

    private void handleDuplicatePressed(final boolean pressed) {
        if (!pressed) {
            oled.clearScreenDelayed();
            return;
        }
        final ClipLauncherSlot slot = getSelectedVisibleSlot();
        if (slot == null || !slot.exists().get() || !slot.hasContent().get()) {
            oled.valueInfo("Duplicate Clip", "Select visible clip");
            return;
        }

        final int visibleTrackIndex = selectedTrackIndex - trackBank.scrollPosition().get();
        final int visibleSceneIndex = selectedSceneIndex - trackBank.sceneBank().scrollPosition().get();
        if (visibleTrackIndex < 0 || visibleTrackIndex >= TRACKS || visibleSceneIndex < 0 || visibleSceneIndex >= SCENES) {
            oled.valueInfo("Duplicate Clip", "Selected clip off page");
            return;
        }

        final Track track = trackBank.getItemAt(visibleTrackIndex);
        track.selectInMixer();
        slot.select();

        final double currentLength = performCursorClip.getLoopLength().get();
        if (currentLength <= 0) {
            oled.valueInfo("Duplicate Clip", "No clip length");
            return;
        }
        if (isShiftHeld()) {
            if (currentLength <= MIN_DUPLICATE_CLIP_LENGTH) {
                oled.valueInfo("Clip Length", formatBars(MIN_DUPLICATE_CLIP_LENGTH));
                return;
            }
            final double newLength = Math.max(currentLength / 2.0, MIN_DUPLICATE_CLIP_LENGTH);
            performCursorClip.getLoopLength().set(newLength);
            oled.valueInfo("Clip Length", formatBars(newLength));
            return;
        }
        if (currentLength >= MAX_DUPLICATE_CLIP_LENGTH) {
            oled.valueInfo("Clip Length", formatBars(MAX_DUPLICATE_CLIP_LENGTH));
            return;
        }
        final double newLength = Math.min(currentLength * 2.0, MAX_DUPLICATE_CLIP_LENGTH);
        performCursorClip.duplicateContent();
        performCursorClip.getLoopLength().set(newLength);
        oled.valueInfo("Clip Length", formatBars(newLength));
    }

    private void handleTrackScroll(final boolean pressed, final int direction) {
        if (!pressed || !canScrollTracks(direction)) {
            return;
        }
        final int increment = trackScrollAmount();
        final int current = trackBank.scrollPosition().get();
        final int next = clamp(current + (direction * increment), 0, maxTrackOffset());
        if (next != current) {
            trackBank.scrollPosition().set(next);
            oled.valueInfo("Perform Tracks", offsetLabel(next, totalTrackCount, TRACKS));
        }
    }

    private void handleSceneScroll(final boolean pressed, final int direction) {
        if (!pressed || !canScrollScenes(direction)) {
            return;
        }
        final int increment = sceneScrollAmount();
        final int current = trackBank.sceneBank().scrollPosition().get();
        final int next = clamp(current + (direction * increment), 0, maxSceneOffset());
        if (next != current) {
            trackBank.sceneBank().scrollPosition().set(next);
            oled.valueInfo("Perform Scenes", offsetLabel(next, totalSceneCount, SCENES));
        }
    }

    private void handleModeAdvance(final boolean pressed) {
        if (!pressed) {
            oled.clearScreenDelayed();
            return;
        }
        if (selectHeld.get()) {
            oled.detailInfo("Encoder Mode", modeInfo(encoderMode));
            return;
        }
        switchMode(nextMode());
    }

    private void switchMode(final EncoderMode newMode) {
        encoderMode = newMode;
        currentEncoderLayer.deactivate();
        currentEncoderLayer = modeMapping.get(newMode);
        applyEncoderStepSizes();
        currentEncoderLayer.activate();
        oled.detailInfo("Encoder Mode", modeInfo(newMode));
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
        return switch (mode) {
            case CHANNEL -> "1: Global 1\n2: Global 2\n3: Global 3\n4: Global 4";
            case MIXER -> "1: Volume\n2: Pan\n3: Send 1\n4: Send 2";
            case USER_1 -> "1: Track Remote 1\n2: Track Remote 2\n3: Track Remote 3\n4: Track Remote 4";
            case USER_2 -> "1: Master Vol\n2: Master Pan\n3: Cue Vol\n4: Cue Mix";
        };
    }

    private void adjustParameter(final int encoderIndex, final Parameter parameter, final String fallbackLabel,
                                 final int inc) {
        if (!isMapped(parameter)) {
            return;
        }
        if (driver.isEncoderTouchResetEnabled()) {
            parameterResetGesture.onAdjusted(encoderIndex, Math.abs(inc));
        }
        MixerEncoderProfile.adjustParameter(parameter, isShiftHeld(), inc);
        oled.valueInfo(labelFor(parameter, fallbackLabel), parameter.displayedValue().get());
    }

    private void handleParameterTouch(final int encoderIndex, final Parameter parameter, final String fallbackLabel,
                                      final boolean touched) {
        if (touched) {
            if (driver.isEncoderTouchResetEnabled()) {
                parameterResetGesture.onTouchStart(encoderIndex);
                driver.getHost().scheduleTask(() -> {
                    if (driver.isEncoderTouchResetEnabled()
                            && parameterResetGesture.shouldResetWhileTouched(encoderIndex)
                            && isMapped(parameter)) {
                        parameter.reset();
                        oled.valueInfo(labelFor(parameter, fallbackLabel), parameter.displayedValue().get());
                    }
                }, PARAMETER_RESET_TOUCH_HOLD_MS);
            }
            if (!isMapped(parameter)) {
                oled.valueInfo(fallbackLabel, "Unmapped");
                return;
            }
            oled.valueInfo(labelFor(parameter, fallbackLabel), parameter.displayedValue().get());
            return;
        }

        if (!isMapped(parameter)) {
            if (driver.isEncoderTouchResetEnabled()) {
                parameterResetGesture.onTouchEnd(encoderIndex);
            }
            oled.clearScreenDelayed();
            return;
        }
        if (driver.isEncoderTouchResetEnabled()) {
            parameterResetGesture.onTouchEnd(encoderIndex);
        }
        oled.clearScreenDelayed();
    }

    private void markParameterInterested(final Parameter parameter) {
        if (parameter == null) {
            return;
        }
        parameter.exists().markInterested();
        parameter.name().markInterested();
        parameter.displayedValue().markInterested();
        parameter.value().markInterested();
    }

    private boolean isMapped(final Parameter parameter) {
        return parameter != null && parameter.exists().get();
    }

    private String labelFor(final Parameter parameter, final String fallbackLabel) {
        final String name = parameter.name().get();
        return name == null || name.isBlank() ? fallbackLabel : name;
    }

    private int trackScrollAmount() {
        return isShiftHeld() ? 1 : TRACKS;
    }

    private int sceneScrollAmount() {
        return isShiftHeld() ? 1 : SCENES;
    }

    private boolean canScrollTracks(final int direction) {
        final int current = trackBank.scrollPosition().get();
        return direction < 0 ? current > 0 : current < maxTrackOffset();
    }

    private boolean canScrollScenes(final int direction) {
        final int current = trackBank.sceneBank().scrollPosition().get();
        return direction < 0 ? current > 0 : current < maxSceneOffset();
    }

    private int maxTrackOffset() {
        return Math.max(0, totalTrackCount - TRACKS);
    }

    private int maxSceneOffset() {
        return Math.max(0, totalSceneCount - SCENES);
    }

    private boolean isShiftHeld() {
        return driver.isGlobalShiftHeld();
    }

    private ClipLauncherSlot getSelectedVisibleSlot() {
        if (selectedTrackIndex < 0 || selectedSceneIndex < 0) {
            return null;
        }
        final int trackOffset = trackBank.scrollPosition().get();
        final int sceneOffset = trackBank.sceneBank().scrollPosition().get();
        final int visibleTrackIndex = selectedTrackIndex - trackOffset;
        final int visibleSceneIndex = selectedSceneIndex - sceneOffset;
        if (visibleTrackIndex < 0 || visibleTrackIndex >= TRACKS || visibleSceneIndex < 0 || visibleSceneIndex >= SCENES) {
            return null;
        }
        return trackBank.getItemAt(visibleTrackIndex).clipLauncherSlotBank().getItemAt(visibleSceneIndex);
    }

    public void notifyBlink(final int blinkState) {
        this.blinkState = blinkState;
    }

    @Override
    protected void onActivate() {
        applyEncoderStepSizes();
        currentEncoderLayer.activate();
    }

    @Override
    protected void onDeactivate() {
        currentEncoderLayer.deactivate();
    }

    private RgbLigthState getSlotState(final ClipLauncherSlot slot, final int padIndex) {
        if (!slot.exists().get()) {
            return RgbLigthState.OFF;
        }
        if (!slot.hasContent().get()) {
            return slot.isSelected().get() ? RgbLigthState.GRAY_2 : RgbLigthState.GRAY_1;
        }

        final RgbLigthState baseColor = slotColors[padIndex] == null ? RgbLigthState.WHITE : slotColors[padIndex];
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

    private RgbLigthState blinkSlow(final RgbLigthState on, final RgbLigthState off) {
        return blinkState % 8 < 4 ? on : off;
    }

    private RgbLigthState blinkFast(final RgbLigthState on, final RgbLigthState off) {
        return blinkState % 2 == 0 ? on : off;
    }

    private String slotLabel(final int absoluteTrackIndex, final int absoluteSceneIndex) {
        final int visibleTrackIndex = absoluteTrackIndex - trackBank.scrollPosition().get();
        final int visibleSceneIndex = absoluteSceneIndex - trackBank.sceneBank().scrollPosition().get();
        final String trackName = visibleTrackIndex >= 0 && visibleTrackIndex < TRACKS
                ? nameOrFallback(trackNames[visibleTrackIndex], "Track " + (absoluteTrackIndex + 1))
                : "Track " + (absoluteTrackIndex + 1);
        final String sceneName = visibleSceneIndex >= 0 && visibleSceneIndex < SCENES
                ? nameOrFallback(sceneNames[visibleSceneIndex], "Scene " + (absoluteSceneIndex + 1))
                : "Scene " + (absoluteSceneIndex + 1);
        return trackName + " / " + sceneName;
    }

    private String selectedSlotLabel() {
        if (selectedTrackIndex < 0 || selectedSceneIndex < 0) {
            return "None";
        }
        return slotLabel(selectedTrackIndex, selectedSceneIndex);
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

    private int toPadIndex(final int trackIndex, final int sceneIndex) {
        return sceneIndex * TRACKS + trackIndex;
    }

    private int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}
