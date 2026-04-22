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
import com.oikoaudio.fire.SharedMusicalContext;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.EncoderTouchResetHandler;
import com.oikoaudio.fire.control.EncoderValueProfile;
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

    private static final long PARAMETER_RESET_TOUCH_HOLD_MS = 750;
    private static final long PARAMETER_RESET_RECENT_ADJUSTMENT_SUPPRESS_MS = 300;
    private static final int PARAMETER_RESET_TOLERATED_ADJUSTMENT_UNITS = 2;
    private static final int MAX_TRACKS = 16;
    private static final int MAX_SCENES = 16;
    private static final double MIN_DUPLICATE_CLIP_LENGTH = 1.0;
    private static final double MAX_DUPLICATE_CLIP_LENGTH = 256.0;
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

    private final RgbLigthState[] slotColors = new RgbLigthState[MAX_TRACKS * MAX_SCENES];
    private final String[] trackNames = new String[MAX_TRACKS];
    private final String[] sceneNames = new String[MAX_SCENES];
    private final boolean[] selectedVisibleTracks = new boolean[MAX_TRACKS];
    private final BooleanValueObject selectHeld = new BooleanValueObject();
    private final BooleanValueObject copyHeld = new BooleanValueObject();
    private final BooleanValueObject deleteHeld = new BooleanValueObject();
    private final EncoderTouchResetHandler parameterResetHandler;
    private Layer currentEncoderLayer;
    private EncoderMode encoderMode = EncoderMode.CHANNEL;
    private int blinkState;
    private int totalTrackCount = MAX_TRACKS;
    private int totalSceneCount = MAX_SCENES;
    private int selectedTrackIndex = -1;
    private int selectedSceneIndex = -1;
    private boolean mainEncoderPressConsumed = false;
    private boolean trackActionMode = false;
    private PerformLayout layout = PerformLayout.vertical();

    public PerformClipLauncherMode(final AkaiFireOikontrolExtension driver) {
        super(driver.getLayers(), "PERFORM_CLIP_LAUNCHER");
        this.driver = driver;
        this.sharedMusicalContext = driver.getSharedMusicalContext();
        final ControllerHost host = driver.getHost();
        this.oled = driver.getOled();
        this.trackBank = host.createTrackBank(MAX_TRACKS, 0, MAX_SCENES);
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
        final RgbButton[] rgbButtons = driver.getRgbButtons();
        for (int sceneIndex = 0; sceneIndex < MAX_SCENES; sceneIndex++) {
            final Scene scene = trackBank.sceneBank().getScene(sceneIndex);
            scene.exists().markInterested();
            scene.name().markInterested();
            final int row = sceneIndex;
            scene.name().addValueObserver(name -> sceneNames[row] = name);
            sceneNames[row] = scene.name().get();
        }

        for (int trackIndex = 0; trackIndex < MAX_TRACKS; trackIndex++) {
            final Track track = trackBank.getItemAt(trackIndex);
            track.exists().markInterested();
            track.name().markInterested();
            track.arm().markInterested();
            track.mute().markInterested();
            track.solo().markInterested();
            track.isStopped().markInterested();
            track.isQueuedForStop().markInterested();
            final int column = trackIndex;
            track.name().addValueObserver(name -> trackNames[column] = name);
            trackNames[column] = track.name().get();
            track.addIsSelectedInMixerObserver(selected -> {
                selectedVisibleTracks[column] = selected;
                if (selected) {
                    selectedTrackIndex = trackBank.scrollPosition().get() + column;
                }
            });
            track.addIsSelectedInEditorObserver(selected -> {
                if (selected) {
                    selectedVisibleTracks[column] = true;
                    selectedTrackIndex = trackBank.scrollPosition().get() + column;
                }
            });

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
                slotColors[slotIndex] = ColorLookup.getColor(slot.color().get());
            }
        }
        for (int padIndex = 0; padIndex < rgbButtons.length; padIndex++) {
            final int currentPad = padIndex;
            rgbButtons[padIndex].bindPressed(this, pressed -> handlePadPressed(currentPad, pressed),
                    () -> getPadState(currentPad));
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
        oled.detailInfo("Settings",
                "1: Root %s\n2: Scale %s\n3: Oct %d".formatted(
                        com.oikoaudio.fire.note.NoteGridLayout.noteName(sharedMusicalContext.getRootNote()),
                        sharedMusicalContext.getScaleDisplayName(),
                        sharedMusicalContext.getOctave()));
        oled.clearScreenDelayed();
    }

    public boolean isSettingsMode() {
        return false;
    }

    public boolean isTrackActionMode() {
        return trackActionMode;
    }

    public void toggleTrackActionMode() {
        trackActionMode = !trackActionMode;
        if (trackActionMode) {
            showTrackActionInfo();
        } else {
            showCurrentModeInfo();
        }
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
        return trackActionMode ? "Tracks" : layout.label();
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
                adjustParameter(index, parameter, fallbackPrefix + " " + (parameterIndex + 1), inc);
            });
            encoders[i].bindTouched(layer, touched -> {
                if (isSettingsHeld()) {
                    handleOverviewTouch(index, touched);
                    return;
                }
                final int parameterIndex = remoteParameterIndex(index, isAltHeld());
                final Parameter parameter = page.getParameter(parameterIndex);
                handleParameterTouch(index, parameter, fallbackPrefix + " " + (parameterIndex + 1), touched);
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
                        adjustParameter(index, parameter, fallbackPrefix + " " + (parameterIndex + 1), inc);
                    });
            encoders[i].bindTouched(layer, touched -> {
                final int parameterIndex = remoteParameterIndex(index, isAltHeld());
                final Parameter parameter = page.getParameter(parameterIndex);
                handleParameterTouch(index, parameter, fallbackPrefix + " " + (parameterIndex + 1), touched);
            });
        }
    }

    private void adjustOverviewPitch(final int encoderIndex, final int inc) {
        if (inc == 0) {
            return;
        }
        if (encoderIndex == 0) {
            sharedMusicalContext.adjustRootNote(inc);
            oled.valueInfo("Root", com.oikoaudio.fire.note.NoteGridLayout.noteName(sharedMusicalContext.getRootNote()));
            return;
        }
        if (encoderIndex == 1) {
            sharedMusicalContext.adjustScaleIndex(inc, -1);
            oled.valueInfo("Scale", sharedMusicalContext.getScaleDisplayName());
            return;
        }
        if (encoderIndex == 2) {
            sharedMusicalContext.adjustOctave(inc);
            oled.valueInfo("Octave", Integer.toString(sharedMusicalContext.getOctave()));
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
            oled.valueInfo("Root", com.oikoaudio.fire.note.NoteGridLayout.noteName(sharedMusicalContext.getRootNote()));
            return;
        }
        if (encoderIndex == 1) {
            oled.valueInfo("Scale", sharedMusicalContext.getScaleDisplayName());
            return;
        }
        if (encoderIndex == 2) {
            oled.valueInfo("Octave", Integer.toString(sharedMusicalContext.getOctave()));
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
            markParameterInterested(parameter);
            final String label = fallbackLabels[i];
            encoders[i].bindContinuousEncoder(layer, this::isShiftHeld,
                    inc -> adjustParameter(index, parameter, label, inc));
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

    private void handlePadPressed(final int padIndex, final boolean pressed) {
        if (trackActionMode) {
            handleTrackActionPadPressed(padIndex, pressed);
            return;
        }
        final int visibleTrackIndex = visibleTrackIndexForPad(padIndex);
        final int visibleSceneIndex = visibleSceneIndexForPad(padIndex);
        if (visibleTrackIndex < 0 || visibleSceneIndex < 0
                || visibleTrackIndex >= visibleTrackCount()
                || visibleSceneIndex >= visibleSceneCount()) {
            return;
        }
        final Track track = trackBank.getItemAt(visibleTrackIndex);
        final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(visibleSceneIndex);
        handleSlotPressed(track, slot, visibleTrackIndex, visibleSceneIndex, pressed);
    }

    private void handleTrackActionPadPressed(final int padIndex, final boolean pressed) {
        if (!pressed) {
            return;
        }
        final TrackActionRow actionRow = TrackActionRow.fromPadIndex(padIndex);
        if (actionRow == null) {
            return;
        }
        final int visibleTrackIndex = padIndex % PerformLayout.PAD_COLUMNS;
        final Track track = trackBank.getItemAt(visibleTrackIndex);
        if (track == null || !track.exists().get()) {
            return;
        }
        final int absoluteTrackIndex = trackBank.scrollPosition().get() + visibleTrackIndex;
        final String trackLabel = trackLabel(absoluteTrackIndex, visibleTrackIndex);
        track.selectInMixer();
        switch (actionRow) {
            case SELECT -> {
                if (isAltHeld()) {
                    track.stop();
                    oled.valueInfo("Track Stop", trackLabel);
                    return;
                }
                track.selectInEditor();
                oled.valueInfo("Track Select", trackLabel);
            }
            case SOLO -> {
                track.solo().toggle(false);
                oled.valueInfo(track.solo().get() ? "Track Solo" : "Track Unsolo", trackLabel);
            }
            case MUTE -> {
                track.mute().toggle();
                oled.valueInfo(track.mute().get() ? "Track Mute" : "Track Unmute", trackLabel);
            }
            case ARM -> {
                track.arm().toggle();
                oled.valueInfo(track.arm().get() ? "Track Arm" : "Track Disarm", trackLabel);
            }
        }
    }

    private void handleSlotPressed(final Track track, final ClipLauncherSlot slot, final int visibleTrackIndex,
                                   final int visibleSceneIndex, final boolean pressed) {
        if (!pressed || !track.exists().get() || !slot.exists().get()) {
            return;
        }
        if (isSettingsHeld()) {
            oled.valueInfo("Settings", "Encoders adjust globals");
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

        slot.createEmptyClip(driver.getDefaultClipLengthBeats());
        slot.launch();
        oled.valueInfo("Create Clip", slotLabel(absoluteTrackIndex, absoluteSceneIndex));
    }

    private void handleDuplicatePressed(final boolean pressed) {
        if (!pressed) {
            oled.clearScreenDelayed();
            return;
        }
        final ClipLauncherSlot slot = getSelectedVisibleSlot();
        if (slot == null || !slot.exists().get()) {
            oled.valueInfo("Duplicate Clip", "Select visible clip");
            return;
        }

        final int visibleTrackIndex = selectedTrackIndex - trackBank.scrollPosition().get();
        final int visibleSceneIndex = selectedSceneIndex - trackBank.sceneBank().scrollPosition().get();
        if (visibleTrackIndex < 0 || visibleTrackIndex >= visibleTrackCount()
                || visibleSceneIndex < 0 || visibleSceneIndex >= visibleSceneCount()) {
            oled.valueInfo("Duplicate Clip", "Selected clip off page");
            return;
        }

        final Track track = trackBank.getItemAt(visibleTrackIndex);
        track.selectInMixer();
        slot.select();
        driver.getHost().scheduleTask(() -> {
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
            if (slot.hasContent().get()) {
                performCursorClip.duplicateContent();
            }
            performCursorClip.getLoopLength().set(newLength);
            oled.valueInfo("Clip Length", formatBars(newLength));
        }, 1);
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
            oled.valueInfo("Perform Tracks", offsetLabel(next, totalTrackCount, visibleTrackCount()));
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
            oled.valueInfo("Perform Scenes", offsetLabel(next, totalSceneCount, visibleSceneCount()));
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
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            oled.valueInfo("Note Repeat", "Unavailable");
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
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                oled.valueInfo("Note Repeat", "Unavailable");
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

    private void handleRemotePageNavigation(final int inc) {
        final CursorRemoteControlsPage page = remotePageForCurrentEncoderMode();
        final String label = remotePageLabelForCurrentEncoderMode();
        if (page == null || inc == 0) {
            oled.valueInfo("Remote Page", "No page");
            return;
        }

        final int pageCount = page.pageCount().getAsInt();
        if (pageCount <= 0) {
            oled.valueInfo(label + " Page", "No Pages");
            return;
        }
        final int currentPage = page.selectedPageIndex().get();
        final int nextPage = remotePageIndexAfterTurn(currentPage, pageCount, inc);
        if (nextPage != currentPage) {
            page.selectedPageIndex().set(nextPage);
        }
        oled.valueInfo(label + " Page", remotePageName(page, nextPage));
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
        encoderMode = newMode;
        currentEncoderLayer.deactivate();
        currentEncoderLayer = modeMapping.get(newMode);
        applyEncoderStepSizes();
        currentEncoderLayer.activate();
        if (isSettingsHeld()) {
            showOverview();
        } else {
            showCurrentModeInfo();
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
        oled.detailInfo(modeTitle(encoderMode), modeInfo(encoderMode));
    }

    private void showTrackActionInfo() {
        oled.detailInfo("Track Controls", "1: %s / ALT Stop\n2: %s\n3: %s\n4: %s".formatted(
                TrackActionRow.SELECT.label,
                TrackActionRow.SOLO.label,
                TrackActionRow.MUTE.label,
                TrackActionRow.ARM.label));
    }

    private void adjustParameter(final int encoderIndex, final Parameter parameter, final String fallbackLabel,
                                 final int inc) {
        if (!isMapped(parameter)) {
            return;
        }
        parameterResetHandler.markAdjusted(encoderIndex, Math.abs(inc));
        EncoderValueProfile.LARGE_RANGE.adjustParameter(parameter, isShiftHeld(), inc);
        oled.valueInfo(labelFor(parameter, fallbackLabel), parameter.displayedValue().get());
    }

    private void handleParameterTouch(final int encoderIndex, final Parameter parameter, final String fallbackLabel,
                                      final boolean touched) {
        if (touched) {
            parameterResetHandler.beginTouchReset(encoderIndex, () -> {
                if (isMapped(parameter)) {
                    parameter.reset();
                    oled.valueInfo(labelFor(parameter, fallbackLabel), parameter.displayedValue().get());
                }
            });
            if (!isMapped(parameter)) {
                oled.valueInfo(fallbackLabel, "Unmapped");
                return;
            }
            oled.valueInfo(labelFor(parameter, fallbackLabel), parameter.displayedValue().get());
            return;
        }

        if (!isMapped(parameter)) {
            parameterResetHandler.endTouchReset(encoderIndex);
            oled.clearScreenDelayed();
            return;
        }
        parameterResetHandler.endTouchReset(encoderIndex);
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

    private boolean isMapped(final Parameter parameter) {
        return parameter != null && parameter.exists().get();
    }

    private String labelFor(final Parameter parameter, final String fallbackLabel) {
        final String name = parameter.name().get();
        return name == null || name.isBlank() ? fallbackLabel : name;
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
        final int trackOffset = trackBank.scrollPosition().get();
        final int sceneOffset = trackBank.sceneBank().scrollPosition().get();
        final int visibleTrackIndex = selectedTrackIndex - trackOffset;
        final int visibleSceneIndex = selectedSceneIndex - sceneOffset;
        if (visibleTrackIndex < 0 || visibleTrackIndex >= visibleTrackCount()
                || visibleSceneIndex < 0 || visibleSceneIndex >= visibleSceneCount()) {
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
        if (isSettingsHeld()) {
            showOverview();
        } else {
            showCurrentModeInfo();
        }
    }

    @Override
    protected void onDeactivate() {
        currentEncoderLayer.deactivate();
    }

    private RgbLigthState getPadState(final int padIndex) {
        if (isSettingsHeld()) {
            return settingsLogoState(padIndex);
        }
        if (trackActionMode) {
            return getTrackActionPadState(padIndex);
        }
        final int visibleTrackIndex = visibleTrackIndexForPad(padIndex);
        final int visibleSceneIndex = visibleSceneIndexForPad(padIndex);
        if (visibleTrackIndex < 0 || visibleSceneIndex < 0
                || visibleTrackIndex >= visibleTrackCount()
                || visibleSceneIndex >= visibleSceneCount()) {
            return RgbLigthState.OFF;
        }
        final ClipLauncherSlot slot = trackBank.getItemAt(visibleTrackIndex).clipLauncherSlotBank().getItemAt(visibleSceneIndex);
        return getSlotState(slot, visibleTrackIndex, visibleSceneIndex);
    }

    private RgbLigthState getSlotState(final ClipLauncherSlot slot, final int visibleTrackIndex, final int visibleSceneIndex) {
        if (isSettingsHeld()) {
            return settingsLogoState(toPadIndex(visibleTrackIndex, visibleSceneIndex));
        }
        if (!slot.exists().get()) {
            return RgbLigthState.OFF;
        }
        if (!slot.hasContent().get()) {
            return slot.isSelected().get() ? RgbLigthState.GRAY_2 : RgbLigthState.GRAY_1;
        }

        final RgbLigthState baseColor = slotColors[toSlotIndex(visibleTrackIndex, visibleSceneIndex)] == null
                ? RgbLigthState.WHITE
                : slotColors[toSlotIndex(visibleTrackIndex, visibleSceneIndex)];
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

    private RgbLigthState getTrackActionPadState(final int padIndex) {
        final TrackActionRow actionRow = TrackActionRow.fromPadIndex(padIndex);
        if (actionRow == null) {
            return RgbLigthState.OFF;
        }
        final int visibleTrackIndex = padIndex % PerformLayout.PAD_COLUMNS;
        final Track track = trackBank.getItemAt(visibleTrackIndex);
        if (track == null || !track.exists().get()) {
            return RgbLigthState.OFF;
        }
        final RgbLigthState baseColor = actionRow.color;
        return switch (actionRow) {
            case SELECT -> {
                if (track.isQueuedForStop().get()) {
                    yield blinkFast(baseColor.getBrightest(), baseColor.getDimmed());
                }
                yield isVisibleTrackSelected(visibleTrackIndex)
                        ? baseColor.getBrightest()
                        : track.isStopped().get() ? baseColor.getDimmed() : baseColor;
            }
            case SOLO -> track.solo().get() ? baseColor.getBrightest() : baseColor.getDimmed();
            case MUTE -> track.mute().get() ? baseColor.getBrightest() : baseColor.getDimmed();
            case ARM -> track.arm().get() ? baseColor : baseColor.getDimmed();
        };
    }

    private boolean isVisibleTrackSelected(final int visibleTrackIndex) {
        return visibleTrackIndex >= 0
                && visibleTrackIndex < selectedVisibleTracks.length
                && selectedVisibleTracks[visibleTrackIndex];
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
        final int visibleTrackIndex = absoluteTrackIndex - trackBank.scrollPosition().get();
        final int visibleSceneIndex = absoluteSceneIndex - trackBank.sceneBank().scrollPosition().get();
        final String trackName = visibleTrackIndex >= 0 && visibleTrackIndex < MAX_TRACKS
                ? nameOrFallback(trackNames[visibleTrackIndex], "Track " + (absoluteTrackIndex + 1))
                : "Track " + (absoluteTrackIndex + 1);
        final String sceneName = visibleSceneIndex >= 0 && visibleSceneIndex < MAX_SCENES
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

    private String trackLabel(final int absoluteTrackIndex, final int visibleTrackIndex) {
        return visibleTrackIndex >= 0 && visibleTrackIndex < MAX_TRACKS
                ? nameOrFallback(trackNames[visibleTrackIndex], "Track " + (absoluteTrackIndex + 1))
                : "Track " + (absoluteTrackIndex + 1);
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

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}
