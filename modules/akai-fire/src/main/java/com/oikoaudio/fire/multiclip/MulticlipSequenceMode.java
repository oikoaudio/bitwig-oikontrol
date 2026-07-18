package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.display.EncoderFooterLegend;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.utils.PatternButtons;
import java.util.ArrayList;
import java.util.List;

/** Scene, child-track, and 32-step sequencer for clips feeding a Drum Machine group. */
public final class MulticlipSequenceMode extends Layer {
    private static final int DEVICE_SCAN_SIZE = 16;
    private static final int SCENE_BANK_SIZE = MulticlipXoxLayout.SCENE_COUNT;
    private static final int MAX_LANES = MulticlipXoxLayout.LANE_COUNT;
    private static final int MAX_CREATION_ATTEMPTS = 20;
    private static final int MAX_TIME_START =
            MulticlipTiming.MAX_LOOP_STEPS - MulticlipXoxLayout.PATTERN_COUNT;
    private static final double DEFAULT_GATE = 0.12;

    private final AkaiFireOikontrolExtension driver;
    private final ControllerHost host;
    private final Layer padLayer;
    private final CursorTrack groupCursor;
    private final MulticlipGroupCursorController groupCursorController;
    private final TrackBank laneBank;
    private final SceneBank sceneBank;
    private final MulticlipSceneCreator sceneCreator;
    private final MulticlipClipController clipController;
    private final MulticlipPadInteractionState padInteraction = new MulticlipPadInteractionState();
    private final MulticlipTimingController timingController;

    private final RgbLightState[] laneColors = new RgbLightState[MAX_LANES];
    private final boolean[] laneExists = new boolean[MAX_LANES];
    private final boolean[] laneCanHoldNotes = new boolean[MAX_LANES];
    private final String[] laneNames = new String[MAX_LANES];
    private final boolean[] groupDeviceIsDrumMachine = new boolean[DEVICE_SCAN_SIZE];

    private int laneCount;
    private int activeChildPosition = -1;
    private int activeScene;
    private int firstVisibleStep;
    private long targetGeneration;
    private long creationGeneration;
    private boolean active;
    private boolean groupContextReady;
    private boolean cursorRetargetInProgress;
    private EncoderMode encoderMode = EncoderMode.CHANNEL;

    public MulticlipSequenceMode(final AkaiFireOikontrolExtension driver) {
        super(driver.getLayers(), "MULTICLIP_SEQUENCE");
        this.driver = driver;
        host = driver.getHost();
        padLayer = new Layer(driver.getLayers(), "MULTICLIP_SEQUENCE_PADS");

        groupCursor =
                host.createCursorTrack(
                        "MULTICLIP_GROUP", "Multiclip Group", 0, SCENE_BANK_SIZE, true);
        groupCursor.exists().markInterested();
        groupCursor.isGroup().markInterested();
        groupCursor.isPinned().markInterested();
        groupCursorController = new MulticlipGroupCursorController(host, groupCursor);
        laneBank = groupCursor.createTrackBank(MAX_LANES, 0, SCENE_BANK_SIZE, false);
        sceneBank = laneBank.sceneBank();
        laneBank.channelCount().markInterested();
        laneBank.channelCount().addValueObserver(ignored -> refreshLaneCount());
        observeScenes();
        observeGroupDevices();
        observeTrackLanes();

        sceneCreator =
                new MulticlipSceneCreator(
                        host, host.getProject(), () -> sceneBank.itemCount().get());
        clipController =
                new MulticlipClipController(host, driver.getViewControl().getCursorTrack());
        timingController =
                new MulticlipTimingController(
                        clipController,
                        padInteraction,
                        driver.getOled(),
                        new MulticlipTimingController.Context() {
                            @Override
                            public int firstVisibleStep() {
                                return MulticlipSequenceMode.this.firstVisibleStep;
                            }

                            @Override
                            public boolean activeLaneHasClip() {
                                return MulticlipSequenceMode.this.activeLaneHasClip();
                            }

                            @Override
                            public String activeLaneName() {
                                return MulticlipSequenceMode.this.activeLaneName();
                            }
                        });
        bindControls();
    }

    private void bindControls() {
        new MulticlipControlBindings(
                driver,
                padLayer,
                new MulticlipControlBindings.Host() {
                    @Override
                    public void padPress(
                            final int padIndex, final boolean pressed, final int velocity) {
                        handlePad(padIndex, pressed, velocity);
                    }

                    @Override
                    public RgbLightState padLight(final int padIndex) {
                        return MulticlipSequenceMode.this.padLight(padIndex);
                    }

                    @Override
                    public void gridButton(final int direction) {
                        handleGridButton(direction);
                    }

                    @Override
                    public BiColorLightState gridLight(final int direction) {
                        return direction < 0 && firstVisibleStep == 0
                                ? BiColorLightState.OFF
                                : direction > 0 && firstVisibleStep >= MAX_TIME_START
                                        ? BiColorLightState.OFF
                                        : BiColorLightState.HALF;
                    }

                    @Override
                    public void altButton(final boolean pressed) {
                        // ALT modifies the grid buttons; scenes always remain visible on row one.
                    }

                    @Override
                    public BiColorLightState altLight() {
                        return driver.isGlobalAltHeld()
                                ? BiColorLightState.HALF
                                : BiColorLightState.OFF;
                    }

                    @Override
                    public void rowButton(final int row, final boolean pressed) {
                        if (pressed) {
                            handleRowButton(row);
                        }
                    }

                    @Override
                    public BiColorLightState rowLight(final int row) {
                        return rowButtonLight(row);
                    }

                    @Override
                    public void knobModeButton(final boolean pressed) {
                        handleKnobModeButton(pressed);
                    }

                    @Override
                    public BiColorLightState knobModeLight() {
                        return encoderMode.getState();
                    }

                    @Override
                    public void encoderTurn(final int encoderIndex, final int increment) {
                        if (encoderMode == EncoderMode.CHANNEL && encoderIndex == 0) {
                            timingController.adjustLoopLength(increment);
                        }
                    }
                });
    }

    private void handlePad(final int padIndex, final boolean pressed, final int velocity) {
        if (MulticlipXoxLayout.isScenePad(padIndex)) {
            if (pressed) {
                selectScene(MulticlipXoxLayout.sceneInPage(padIndex));
            }
            return;
        }
        if (MulticlipXoxLayout.isLanePad(padIndex)) {
            if (pressed) {
                selectLane(MulticlipXoxLayout.childPosition(padIndex));
            }
            return;
        }
        if (!MulticlipXoxLayout.isPatternPad(padIndex)) {
            return;
        }
        if (pressed) {
            padInteraction.press(padIndex);
            handleStepPress(padIndex, velocity);
        } else {
            handleStepRelease(padIndex);
            padInteraction.release(padIndex);
        }
    }

    private void selectScene(final int visibleScene) {
        if (!groupContextReady) {
            showInvalidContext();
            return;
        }
        final int absoluteScene = sceneBank.scrollPosition().get() + visibleScene;
        final boolean launch = driver.isGlobalShiftHeld();
        creationGeneration++;
        sceneCreator.ensureExists(
                absoluteScene,
                created -> {
                    if (!active || !created) {
                        return;
                    }
                    activeScene = absoluteScene;
                    focusActiveTarget(null);
                    if (launch) {
                        final int currentVisible = activeScene - sceneBank.scrollPosition().get();
                        if (currentVisible >= 0 && currentVisible < SCENE_BANK_SIZE) {
                            MulticlipChildSceneLauncher.launch(eligibleChildSlots(currentVisible));
                        }
                    }
                    driver.getOled()
                            .valueInfo(
                                    "Scene " + (activeScene + 1),
                                    launch ? "Playing + Editing" : "Editing");
                });
    }

    private void selectLane(final int childPosition) {
        if (!isEligibleLane(childPosition)) {
            return;
        }
        activeChildPosition = childPosition;
        creationGeneration++;
        focusActiveTarget(null);
        showLaneInfo();
    }

    private void focusActiveTarget(final Runnable afterReady) {
        if (!active || !isValidContext() || !isEligibleLane(activeChildPosition)) {
            return;
        }
        final int visibleScene = activeScene - sceneBank.scrollPosition().get();
        if (visibleScene < 0 || visibleScene >= SCENE_BANK_SIZE) {
            driver.getOled().valueInfo("Scene off page", "Select scene again");
            return;
        }
        final long generation = ++targetGeneration;
        cursorRetargetInProgress = true;
        clipController.clear();
        final Track track = laneBank.getItemAt(activeChildPosition);
        final ClipLauncherSlot targetSlot = track.clipLauncherSlotBank().getItemAt(visibleScene);
        track.selectInMixer();
        track.selectInEditor();
        clipController.retarget(
                track,
                targetSlot,
                TrackLaneMapping.fromChildPosition(activeChildPosition),
                firstVisibleStep,
                activeScene,
                targetSlot.hasContent().get(),
                ready -> {
                    if (!active || targetGeneration != generation) {
                        return;
                    }
                    cursorRetargetInProgress = false;
                    if (!ready) {
                        driver.getOled().valueInfo("Clip not ready", "Select lane again");
                        return;
                    }
                    if (clipController.exists()) {
                        clipController.showInEditor();
                    }
                    if (afterReady != null) {
                        afterReady.run();
                    }
                });
    }

    private void handleStepPress(final int padIndex, final int velocity) {
        if (!isValidContext()) {
            showInvalidContext();
            return;
        }
        if (cursorRetargetInProgress || !clipController.isReady()) {
            driver.getOled().valueInfo("Clip loading", "Pad input blocked");
            return;
        }
        final int step = MulticlipXoxLayout.visibleStep(padIndex);
        padInteraction.captureOccupied(padIndex, clipController.isOccupied(step));
        if (!clipController.exists()) {
            createClipAndAddFirstStep(step, velocity);
            return;
        }
        if (!padInteraction.wasOccupied(padIndex)) {
            clipController.setStep(activeMapping().midiChannel(), step, velocity, DEFAULT_GATE);
        }
    }

    private void handleStepRelease(final int padIndex) {
        if (!padInteraction.wasOccupied(padIndex) || padInteraction.isConsumed(padIndex)) {
            return;
        }
        final int step = MulticlipXoxLayout.visibleStep(padIndex);
        for (final int channel : clipController.channelsAt(step)) {
            clipController.clearStep(channel, step);
        }
    }

    private void createClipAndAddFirstStep(final int step, final int velocity) {
        final int childPosition = activeChildPosition;
        final int scene = activeScene;
        final int visibleScene = scene - sceneBank.scrollPosition().get();
        if (visibleScene < 0 || visibleScene >= SCENE_BANK_SIZE) {
            return;
        }
        final long creation = ++creationGeneration;
        final Track track = laneBank.getItemAt(childPosition);
        final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(visibleScene);
        MulticlipChildClipCreator.create(
                track.clipLauncherSlotBank(), visibleScene, driver.getDefaultClipLengthBeats());
        cursorRetargetInProgress = true;
        driver.getOled().valueInfo("Creating child clip", activeLaneName());
        awaitCreatedClip(creation, childPosition, scene, slot, step, velocity, 0);
    }

    private void awaitCreatedClip(
            final long creation,
            final int childPosition,
            final int scene,
            final ClipLauncherSlot slot,
            final int step,
            final int velocity,
            final int attempt) {
        host.scheduleTask(
                () -> {
                    if (!active
                            || creationGeneration != creation
                            || activeChildPosition != childPosition
                            || activeScene != scene) {
                        return;
                    }
                    if (!slot.hasContent().get()) {
                        if (attempt < MAX_CREATION_ATTEMPTS) {
                            awaitCreatedClip(
                                    creation,
                                    childPosition,
                                    scene,
                                    slot,
                                    step,
                                    velocity,
                                    attempt + 1);
                        } else {
                            cursorRetargetInProgress = false;
                            driver.getOled().valueInfo("Clip not created", "Try step again");
                        }
                        return;
                    }
                    focusActiveTarget(
                            () -> {
                                if (creationGeneration == creation
                                        && activeChildPosition == childPosition
                                        && activeScene == scene) {
                                    clipController.setStep(
                                            TrackLaneMapping.fromChildPosition(childPosition)
                                                    .midiChannel(),
                                            step,
                                            velocity,
                                            DEFAULT_GATE);
                                }
                            });
                },
                50);
    }

    private void handleGridButton(final int direction) {
        switch (MulticlipGridGesture.resolve(
                driver.isGlobalShiftHeld(), driver.isGlobalAltHeld(), heldPatternPadExists())) {
            case TIME_PAGE -> pageTime(direction);
            case HELD_STEP_NUDGE -> timingController.fineNudge(direction, true);
            case PLAY_START -> timingController.movePlayStart(direction);
            case WHOLE_LANE_NUDGE -> timingController.fineNudge(direction, false);
        }
    }

    private boolean heldPatternPadExists() {
        for (int pad = MulticlipXoxLayout.PATTERN_START; pad < 64; pad++) {
            if (padInteraction.isHeld(pad)) {
                return true;
            }
        }
        return false;
    }

    private void pageTime(final int direction) {
        final int next =
                Math.max(
                        0,
                        Math.min(
                                MAX_TIME_START,
                                firstVisibleStep + direction * MulticlipXoxLayout.PATTERN_COUNT));
        if (next == firstVisibleStep) {
            return;
        }
        firstVisibleStep = next;
        focusActiveTarget(null);
        driver.getOled()
                .valueInfo(
                        "Steps "
                                + (firstVisibleStep + 1)
                                + "-"
                                + (firstVisibleStep + MulticlipXoxLayout.PATTERN_COUNT),
                        activeLaneName());
    }

    private void pageScenes(final int direction) {
        if (direction < 0) {
            sceneBank.scrollPageBackwards();
        } else {
            sceneBank.scrollPageForwards();
        }
        host.scheduleTask(
                () ->
                        driver.getOled()
                                .valueInfo(
                                        "Scenes "
                                                + (sceneBank.scrollPosition().get() + 1)
                                                + "-"
                                                + (sceneBank.scrollPosition().get()
                                                        + SCENE_BANK_SIZE),
                                        "Multiclip Seq"),
                0);
    }

    private void handleRowButton(final int row) {
        if (row != 1 || !isEligibleLane(activeChildPosition)) {
            return;
        }
        final Track track = laneBank.getItemAt(activeChildPosition);
        switch (MulticlipRowButtonAction.resolve(
                driver.isGlobalAltHeld(), driver.isGlobalShiftHeld())) {
            case SELECT -> focusActiveTarget(null);
            case SOLO -> {
                final boolean enabled = !track.solo().get();
                track.solo().set(enabled);
                driver.getOled().valueInfo(enabled ? "Solo On" : "Solo Off", activeLaneName());
            }
            case MUTE -> {
                final boolean enabled = !track.mute().get();
                track.mute().set(enabled);
                driver.getOled().valueInfo(enabled ? "Mute On" : "Mute Off", activeLaneName());
            }
        }
    }

    private BiColorLightState rowButtonLight(final int row) {
        if (row != 1 || !isEligibleLane(activeChildPosition)) {
            return BiColorLightState.OFF;
        }
        final Track track = laneBank.getItemAt(activeChildPosition);
        return MulticlipRowButtonRenderer.render(
                true, true, track.mute().get(), track.solo().get());
    }

    private RgbLightState padLight(final int padIndex) {
        if (MulticlipXoxLayout.isScenePad(padIndex)) {
            return sceneLight(MulticlipXoxLayout.sceneInPage(padIndex));
        }
        if (MulticlipXoxLayout.isLanePad(padIndex)) {
            return laneLight(MulticlipXoxLayout.childPosition(padIndex));
        }
        if (MulticlipXoxLayout.isPatternPad(padIndex)) {
            return patternLight(padIndex);
        }
        return RgbLightState.OFF;
    }

    private RgbLightState laneLight(final int childPosition) {
        if (!isValidContext() || !isEligibleLane(childPosition)) {
            return RgbLightState.OFF;
        }
        final RgbLightState color = laneColor(childPosition);
        return childPosition == activeChildPosition ? color.getBrightest() : color.getVeryDimmed();
    }

    private RgbLightState patternLight(final int padIndex) {
        if (!isValidContext() || !isEligibleLane(activeChildPosition)) {
            return RgbLightState.OFF;
        }
        final int step = MulticlipXoxLayout.visibleStep(padIndex);
        final RgbLightState color = laneColor(activeChildPosition);
        if (padInteraction.isHeld(padIndex)) {
            return padInteraction.isConsumed(padIndex)
                    ? RgbLightState.PURPLE
                    : color.getBrightest();
        }
        if (clipController.isPlaying(step)) {
            return RgbLightState.WHITE;
        }
        if (clipController.isOccupied(step)) {
            return color.getBrightend();
        }
        return color.getVeryDimmed();
    }

    private RgbLightState sceneLight(final int visibleScene) {
        final int absoluteScene = sceneBank.scrollPosition().get() + visibleScene;
        if (absoluteScene >= sceneBank.itemCount().get()) {
            return absoluteScene == activeScene
                    ? RgbLightState.GRAY_1.getBrightend()
                    : RgbLightState.GRAY_1;
        }
        int clips = 0;
        boolean playing = false;
        boolean queued = false;
        RgbLightState childColor = RgbLightState.GRAY_1;
        for (int child = 0; child < laneCount; child++) {
            if (!isEligibleLane(child)) {
                continue;
            }
            final ClipLauncherSlot slot =
                    laneBank.getItemAt(child).clipLauncherSlotBank().getItemAt(visibleScene);
            if (slot.hasContent().get()) {
                clips++;
                if (clips == 1) {
                    childColor = ColorLookup.getColor(slot.color().get());
                }
            }
            playing |= slot.isPlaying().get();
            queued |= slot.isPlaybackQueued().get();
        }
        if (playing) {
            return RgbLightState.WHITE;
        }
        if (queued) {
            return RgbLightState.PURPLE;
        }
        if (absoluteScene == activeScene) {
            return childColor.getBrightend();
        }
        return switch (ScenePopulation.ofChildClips(clips, eligibleLaneCount())) {
            case NEW -> RgbLightState.GRAY_1;
            case PARTIAL -> childColor.getDimmed();
            case POPULATED -> childColor.getSoftDimmed();
        };
    }

    private void observeScenes() {
        sceneBank.scrollPosition().markInterested();
        sceneBank.itemCount().markInterested();
        sceneBank.canScrollBackwards().markInterested();
        sceneBank.canScrollForwards().markInterested();
        for (int index = 0; index < SCENE_BANK_SIZE; index++) {
            final Scene scene = sceneBank.getScene(index);
            scene.exists().markInterested();
            scene.name().markInterested();
            scene.color().markInterested();
            scene.clipCount().markInterested();
            scene.sceneIndex().markInterested();
        }
    }

    private void observeGroupDevices() {
        final DeviceBank devices = groupCursor.createDeviceBank(DEVICE_SCAN_SIZE);
        for (int index = 0; index < DEVICE_SCAN_SIZE; index++) {
            final int deviceIndex = index;
            final Device device = devices.getItemAt(index);
            device.exists().markInterested();
            device.hasDrumPads().markInterested();
            device.hasDrumPads()
                    .addValueObserver(value -> groupDeviceIsDrumMachine[deviceIndex] = value);
            groupDeviceIsDrumMachine[deviceIndex] = device.hasDrumPads().get();
        }
    }

    private void observeTrackLanes() {
        for (int child = 0; child < MAX_LANES; child++) {
            final int position = child;
            final Track track = laneBank.getItemAt(position);
            track.exists().markInterested();
            track.position().markInterested();
            track.name().markInterested();
            track.color().markInterested();
            track.canHoldNoteData().markInterested();
            track.mute().markInterested();
            track.solo().markInterested();
            track.exists().addValueObserver(value -> laneExists[position] = value);
            track.canHoldNoteData().addValueObserver(value -> laneCanHoldNotes[position] = value);
            track.name().addValueObserver(value -> laneNames[position] = value);
            track.color()
                    .addValueObserver(
                            (red, green, blue) ->
                                    laneColors[position] = ColorLookup.getColor(red, green, blue));
            track.addIsSelectedInMixerObserver(
                    selected -> handleExternalTrackSelection(position, selected));
            track.addIsSelectedInEditorObserver(
                    selected -> handleExternalTrackSelection(position, selected));
            for (int scene = 0; scene < SCENE_BANK_SIZE; scene++) {
                final int visibleScene = scene;
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(scene);
                slot.exists().markInterested();
                slot.hasContent().markInterested();
                slot.isSelected().markInterested();
                slot.isPlaying().markInterested();
                slot.isPlaybackQueued().markInterested();
                slot.color().markInterested();
                slot.isSelected()
                        .addValueObserver(
                                selected ->
                                        handleExternalClipSelection(
                                                position, visibleScene, selected));
            }
            laneExists[position] = track.exists().get();
            laneCanHoldNotes[position] = track.canHoldNoteData().get();
            laneNames[position] = track.name().get();
            laneColors[position] = ColorLookup.getColor(track.color().get());
        }
        refreshLaneCount();
    }

    private void handleExternalTrackSelection(final int childPosition, final boolean selected) {
        if (!active
                || !groupContextReady
                || cursorRetargetInProgress
                || !selected
                || !isEligibleLane(childPosition)) {
            return;
        }
        // Do not retarget here: a clip click selects its track before its exact slot observer
        // fires.
        activeChildPosition = childPosition;
        showLaneInfo();
    }

    private void handleExternalClipSelection(
            final int childPosition, final int visibleScene, final boolean selected) {
        if (!active || !groupContextReady || !selected || !isEligibleLane(childPosition)) {
            return;
        }
        final int selectedScene = sceneBank.scrollPosition().get() + visibleScene;
        final boolean changed =
                childPosition != activeChildPosition || selectedScene != activeScene;
        if (cursorRetargetInProgress && !changed) {
            // This is the exact slot selected by our own in-progress retarget.
            return;
        }
        activeChildPosition = childPosition;
        activeScene = selectedScene;
        creationGeneration++;
        sceneCreator.invalidate();
        if (changed) {
            focusActiveTarget(null);
        }
        showLaneInfo();
    }

    private void refreshLaneCount() {
        laneCount = Math.max(0, Math.min(MAX_LANES, laneBank.channelCount().get()));
        ensureActiveLane();
    }

    private void ensureActiveLane() {
        if (isEligibleLane(activeChildPosition)) {
            return;
        }
        activeChildPosition = -1;
        for (int child = 0; child < laneCount; child++) {
            if (isEligibleLane(child)) {
                activeChildPosition = child;
                return;
            }
        }
    }

    private void syncSelectedChildClipTarget() {
        for (int child = 0; child < laneCount; child++) {
            if (!isEligibleLane(child)) {
                continue;
            }
            for (int scene = 0; scene < SCENE_BANK_SIZE; scene++) {
                if (laneBank.getItemAt(child)
                        .clipLauncherSlotBank()
                        .getItemAt(scene)
                        .isSelected()
                        .get()) {
                    activeChildPosition = child;
                    activeScene = sceneBank.scrollPosition().get() + scene;
                    return;
                }
            }
        }
    }

    private List<ClipLauncherSlot> eligibleChildSlots(final int visibleScene) {
        final List<ClipLauncherSlot> slots = new ArrayList<>();
        for (int child = 0; child < laneCount; child++) {
            if (isEligibleLane(child)) {
                slots.add(laneBank.getItemAt(child).clipLauncherSlotBank().getItemAt(visibleScene));
            }
        }
        return List.copyOf(slots);
    }

    private boolean activeLaneHasClip() {
        return isEligibleLane(activeChildPosition)
                && clipController.isReady()
                && clipController.exists();
    }

    private TrackLaneMapping activeMapping() {
        return TrackLaneMapping.fromChildPosition(activeChildPosition);
    }

    private String activeLaneName() {
        return laneName(activeChildPosition);
    }

    private String laneName(final int childPosition) {
        if (childPosition < 0 || childPosition >= laneNames.length) {
            return "Lane";
        }
        final String name = laneNames[childPosition];
        return name == null || name.isBlank() ? "Lane " + (childPosition + 1) : name;
    }

    private RgbLightState laneColor(final int childPosition) {
        return laneColors[childPosition] == null ? RgbLightState.WHITE : laneColors[childPosition];
    }

    private boolean isValidContext() {
        if (!groupContextReady
                || !groupCursor.exists().get()
                || !groupCursor.isGroup().get()
                || laneCount == 0
                || !hasEligibleLane()) {
            return false;
        }
        for (final boolean drumMachine : groupDeviceIsDrumMachine) {
            if (drumMachine) {
                return true;
            }
        }
        return false;
    }

    private boolean isEligibleLane(final int childPosition) {
        return childPosition >= 0
                && childPosition < laneCount
                && laneExists[childPosition]
                && laneCanHoldNotes[childPosition];
    }

    private boolean hasEligibleLane() {
        return eligibleLaneCount() > 0;
    }

    private int eligibleLaneCount() {
        int count = 0;
        for (int child = 0; child < laneCount; child++) {
            if (isEligibleLane(child)) {
                count++;
            }
        }
        return count;
    }

    private void showInvalidContext() {
        driver.getOled()
                .valueInfo(
                        laneCount == 0 ? "No child tracks" : "Select Drum group", "Multiclip Seq");
    }

    private void showLaneInfo() {
        if (!isEligibleLane(activeChildPosition)) {
            showInvalidContext();
            return;
        }
        driver.getOled()
                .detailInfo(
                        "Multiclip Seq",
                        activeLaneName()
                                + "  Lane "
                                + (activeChildPosition + 1)
                                + "\nScene "
                                + (activeScene + 1)
                                + "  Steps "
                                + (firstVisibleStep + 1)
                                + "-"
                                + (firstVisibleStep + MulticlipXoxLayout.PATTERN_COUNT)
                                + "\n"
                                + (clipController.exists() ? "Clip ready" : "Empty lane"));
    }

    private void handleKnobModeButton(final boolean pressed) {
        if (!pressed && !driver.consumeKnobModeGesture()) {
            encoderMode =
                    switch (encoderMode) {
                        case CHANNEL -> EncoderMode.MIXER;
                        case MIXER -> EncoderMode.USER_1;
                        case USER_1 -> EncoderMode.USER_2;
                        case USER_2 -> EncoderMode.CHANNEL;
                    };
            showEncoderMode();
        }
    }

    private void showEncoderMode() {
        if (encoderMode == EncoderMode.CHANNEL) {
            driver.getOled().detailInfo("Multiclip Channel", "1 Clip Length");
            driver.getOled().setFooterLegend(EncoderFooterLegend.of("Lgth", "", "", ""));
        } else {
            driver.getOled().detailInfo("Multiclip " + encoderMode, "Reserved");
            driver.getOled().setFooterLegend(null);
        }
    }

    private void bindPatternButtons() {
        final PatternButtons buttons = driver.getPatternButtons();
        if (buttons == null) {
            return;
        }
        buttons.setUpCallback(
                pressed -> {
                    if (pressed) {
                        pageScenes(-1);
                    }
                },
                () ->
                        sceneBank.canScrollBackwards().get()
                                ? BiColorLightState.HALF
                                : BiColorLightState.OFF);
        buttons.setDownCallback(
                pressed -> {
                    if (pressed) {
                        pageScenes(1);
                    }
                },
                () ->
                        sceneBank.canScrollForwards().get()
                                ? BiColorLightState.HALF
                                : BiColorLightState.OFF);
    }

    private void clearPatternButtons() {
        final PatternButtons buttons = driver.getPatternButtons();
        if (buttons == null) {
            return;
        }
        buttons.setUpCallback(pressed -> {}, () -> BiColorLightState.OFF);
        buttons.setDownCallback(pressed -> {}, () -> BiColorLightState.OFF);
    }

    @Override
    protected void onActivate() {
        active = true;
        groupContextReady = false;
        cursorRetargetInProgress = true;
        padLayer.activate();
        bindPatternButtons();
        groupCursorController.activate(
                ready -> {
                    if (!active) {
                        return;
                    }
                    groupContextReady = ready;
                    if (!ready) {
                        cursorRetargetInProgress = false;
                        showInvalidContext();
                        return;
                    }
                    refreshLaneCount();
                    syncSelectedChildClipTarget();
                    ensureActiveLane();
                    if (isValidContext()) {
                        focusActiveTarget(null);
                        showLaneInfo();
                    } else {
                        cursorRetargetInProgress = false;
                        showInvalidContext();
                    }
                });
        showEncoderMode();
    }

    @Override
    protected void onDeactivate() {
        active = false;
        groupContextReady = false;
        cursorRetargetInProgress = false;
        targetGeneration++;
        creationGeneration++;
        sceneCreator.invalidate();
        clipController.clear();
        groupCursorController.deactivate();
        padInteraction.clear();
        padLayer.deactivate();
        clearPatternButtons();
        driver.getOled().setFooterLegend(null);
    }
}
