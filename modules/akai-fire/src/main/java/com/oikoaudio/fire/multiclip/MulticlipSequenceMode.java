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

/** Four-row sequencer whose lanes are direct child tracks of a Drum Machine group. */
public final class MulticlipSequenceMode extends Layer {
    private static final int DEVICE_SCAN_SIZE = 16;
    private static final int SCENE_BANK_SIZE = 16;
    private static final int VISIBLE_LANES = MulticlipClipController.VISIBLE_LANES;
    private static final double DEFAULT_GATE = 0.12;

    private final AkaiFireOikontrolExtension driver;
    private final ControllerHost host;
    private final Layer padLayer;
    private final CursorTrack groupCursor;
    private final TrackBank laneBank;
    private final SceneBank sceneBank;
    private final RgbLightState[] laneColors = new RgbLightState[TrackLaneMapping.MAX_LANES];
    private final boolean[] laneExists = new boolean[TrackLaneMapping.MAX_LANES];
    private final boolean[] laneCanHoldNotes = new boolean[TrackLaneMapping.MAX_LANES];
    private final String[] laneNames = new String[TrackLaneMapping.MAX_LANES];
    private final boolean[] groupDeviceIsDrumMachine = new boolean[DEVICE_SCAN_SIZE];
    private final MulticlipPadInteractionState padInteraction = new MulticlipPadInteractionState();
    private final MulticlipClipController clipController;
    private final MulticlipTimingController timingController;
    private final PendingStepCreation[] pendingCreations = new PendingStepCreation[VISIBLE_LANES];
    private final MulticlipSceneOverlayState sceneOverlay = new MulticlipSceneOverlayState();
    private final boolean[] sceneSelectedInEditor = new boolean[SCENE_BANK_SIZE];

    private MulticlipPageState pageState = MulticlipPageState.initial(0);
    private int activeScene;
    private long targetGeneration;
    private long sceneActivationGeneration;
    private boolean active;
    private EncoderMode encoderMode = EncoderMode.CHANNEL;

    public MulticlipSequenceMode(final AkaiFireOikontrolExtension driver) {
        super(driver.getLayers(), "MULTICLIP_SEQUENCE");
        this.driver = driver;
        this.host = driver.getHost();
        this.clipController = new MulticlipClipController(host);
        this.timingController =
                new MulticlipTimingController(
                        clipController,
                        padInteraction,
                        driver.getOled(),
                        new MulticlipTimingController.Context() {
                            @Override
                            public int activeRow() {
                                return pageState.activeRow();
                            }

                            @Override
                            public int firstVisibleStep() {
                                return pageState.firstVisibleStep();
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
        this.padLayer = new Layer(driver.getLayers(), "MULTICLIP_SEQUENCE_PADS");
        this.groupCursor =
                host.createCursorTrack(
                        "MULTICLIP_GROUP", "Multiclip Group", 0, SCENE_BANK_SIZE, true);
        groupCursor.exists().markInterested();
        groupCursor.isGroup().markInterested();
        groupCursor.isPinned().markInterested();
        this.laneBank =
                groupCursor.createTrackBank(TrackLaneMapping.MAX_LANES, 0, SCENE_BANK_SIZE, false);
        this.sceneBank = laneBank.sceneBank();
        laneBank.channelCount().markInterested();
        laneBank.channelCount().addValueObserver(count -> refreshLaneCount());
        observeScenes();
        observeGroupDevices();
        observeTrackLanes();
        new MulticlipControlBindings(
                driver,
                padLayer,
                new MulticlipControlBindings.Host() {
                    @Override
                    public void padPress(
                            final int padIndex, final boolean pressed, final int velocity) {
                        if (sceneOverlay.isActive()
                                && padIndex < MulticlipPageState.STEPS_PER_PAGE) {
                            if (pressed) {
                                handleScenePad(padIndex);
                            }
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

                    @Override
                    public RgbLightState padLight(final int padIndex) {
                        return laneIdentityLight(padIndex);
                    }

                    @Override
                    public void gridButton(final int direction) {
                        handleGridButton(direction);
                    }

                    @Override
                    public BiColorLightState gridLight(final int direction) {
                        return direction < 0 && !pageState.canPageTime(-1)
                                ? BiColorLightState.OFF
                                : BiColorLightState.HALF;
                    }

                    @Override
                    public void altButton(final boolean pressed) {
                        if (pressed) {
                            sceneOverlay.altPressed(padInteraction.hasHeldPads());
                            if (sceneOverlay.isActive()) {
                                driver.getOled().valueInfo("Select Scene", "ALT overlay");
                            }
                        } else {
                            sceneOverlay.altReleased();
                        }
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

    private void handleRowButton(final int row) {
        final int childPosition = childPositionForRow(row);
        if (childPosition < 0
                || childPosition >= pageState.laneCount()
                || !isEligibleLane(childPosition)) {
            return;
        }
        final Track track = laneBank.getItemAt(childPosition);
        switch (MulticlipRowButtonAction.resolve(
                driver.isGlobalAltHeld(), driver.isGlobalShiftHeld())) {
            case SELECT -> {
                pageState = pageState.withActiveChildPosition(childPosition);
                track.selectInMixer();
                track.selectInEditor();
                if (clipController.isReady(row) && clipController.exists(row)) {
                    clipController.showInEditor(row);
                }
                driver.getOled().valueInfo("Lane " + (childPosition + 1), laneName(childPosition));
            }
            case SOLO -> {
                final boolean enabled = !track.solo().get();
                track.solo().set(enabled);
                driver.getOled()
                        .valueInfo(enabled ? "Solo On" : "Solo Off", laneName(childPosition));
            }
            case MUTE -> {
                final boolean enabled = !track.mute().get();
                track.mute().set(enabled);
                driver.getOled()
                        .valueInfo(enabled ? "Mute On" : "Mute Off", laneName(childPosition));
            }
        }
    }

    private BiColorLightState rowButtonLight(final int row) {
        final int childPosition = childPositionForRow(row);
        if (childPosition < 0
                || childPosition >= pageState.laneCount()
                || !isEligibleLane(childPosition)) {
            return BiColorLightState.OFF;
        }
        final Track track = laneBank.getItemAt(childPosition);
        return MulticlipRowButtonRenderer.render(
                true,
                childPosition == pageState.activeChildPosition(),
                track.mute().get(),
                track.solo().get());
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
            final int visibleScene = index;
            scene.addIsSelectedInEditorObserver(
                    selected -> sceneSelectedInEditor[visibleScene] = selected);
        }
    }

    private void handleScenePad(final int visibleScene) {
        final int absoluteScene = sceneBank.scrollPosition().get() + visibleScene;
        activeScene = absoluteScene;
        populateSelectedScene(visibleScene, absoluteScene);
        final long activation = ++sceneActivationGeneration;
        scheduleSceneActivation(visibleScene, absoluteScene, activation, 0);
        driver.getOled().valueInfo("Scene " + (activeScene + 1), "Preparing child clips");
    }

    private void scheduleSceneActivation(
            final int visibleScene,
            final int absoluteScene,
            final long activation,
            final int attempt) {
        host.scheduleTask(
                () -> {
                    if (!active
                            || sceneActivationGeneration != activation
                            || activeScene != absoluteScene) {
                        return;
                    }
                    if (!allEligibleLaneClipsExist(visibleScene)) {
                        if (attempt < 7) {
                            scheduleSceneActivation(
                                    visibleScene, absoluteScene, activation, attempt + 1);
                        } else {
                            driver.getOled().valueInfo("Scene not ready", "Try scene again");
                        }
                        return;
                    }
                    sceneBank.getScene(visibleScene).launch();
                    retargetVisibleClipCursors();
                    selectActiveTrack();
                    scheduleEditorFallback(visibleScene, currentTarget(pageState.activeRow()));
                    driver.getOled().valueInfo("Scene " + (absoluteScene + 1), "Playing + Editing");
                },
                50);
    }

    private boolean allEligibleLaneClipsExist(final int visibleScene) {
        for (int childPosition = 0; childPosition < pageState.laneCount(); childPosition++) {
            if (isEligibleLane(childPosition)
                    && !laneBank.getItemAt(childPosition)
                            .clipLauncherSlotBank()
                            .getItemAt(visibleScene)
                            .hasContent()
                            .get()) {
                return false;
            }
        }
        return true;
    }

    private void populateSelectedScene(final int visibleScene, final int absoluteScene) {
        final int laneCount = pageState.laneCount();
        final boolean[] eligible = new boolean[laneCount];
        final boolean[] hasContent = new boolean[laneCount];
        for (int childPosition = 0; childPosition < laneCount; childPosition++) {
            eligible[childPosition] = isEligibleLane(childPosition);
            if (eligible[childPosition]) {
                hasContent[childPosition] =
                        laneBank.getItemAt(childPosition)
                                .clipLauncherSlotBank()
                                .getItemAt(visibleScene)
                                .hasContent()
                                .get();
            }
        }
        for (final int childPosition :
                MulticlipSceneSelectionPlan.missingEligibleLanes(eligible, hasContent)) {
            laneBank.getItemAt(childPosition)
                    .createNewLauncherClip(absoluteScene, driver.getDefaultClipLengthBeats());
        }
    }

    private void scheduleEditorFallback(
            final int visibleScene, final MulticlipTargetIdentity target) {
        host.scheduleTask(
                () -> {
                    final int row = pageState.activeRow();
                    if (!active || row < 0 || !target.equals(currentTarget(row))) {
                        return;
                    }
                    final EditorPresentationPlan plan =
                            EditorPresentationPlan.choose(
                                    groupCursor.exists().get(),
                                    sceneBank.getScene(visibleScene).exists().get(),
                                    sceneSelectedInEditor[visibleScene]);
                    if (plan == EditorPresentationPlan.ACTIVE_CLIP && clipController.exists(row)) {
                        clipController.showInEditor(row);
                    }
                    selectActiveTrack();
                },
                50);
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
                                        "Scene Page"),
                0);
    }

    private void handleGridButton(final int direction) {
        switch (MulticlipGridGesture.resolve(
                driver.isGlobalShiftHeld(),
                driver.isGlobalAltHeld(),
                padInteraction.hasHeldPads())) {
            case TIME_PAGE -> pageTime(direction);
            case HELD_STEP_NUDGE -> timingController.fineNudge(direction, true);
            case PLAY_START -> timingController.movePlayStart(direction);
            case WHOLE_LANE_NUDGE -> timingController.fineNudge(direction, false);
        }
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
            driver.getOled().detailInfo("Multiclip Channel", "1 Lane Length");
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
                        if (sceneOverlay.isActive()) {
                            pageScenes(-1);
                        } else {
                            pageLanes(-1);
                        }
                    }
                },
                () ->
                        (sceneOverlay.isActive()
                                        ? sceneBank.canScrollBackwards().get()
                                        : pageState.canPageLanes(-1))
                                ? BiColorLightState.HALF
                                : BiColorLightState.OFF);
        buttons.setDownCallback(
                pressed -> {
                    if (pressed) {
                        if (sceneOverlay.isActive()) {
                            pageScenes(1);
                        } else {
                            pageLanes(1);
                        }
                    }
                },
                () ->
                        (sceneOverlay.isActive()
                                        ? sceneBank.canScrollForwards().get()
                                        : pageState.canPageLanes(1))
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

    private void pageLanes(final int direction) {
        if (!pageState.canPageLanes(direction)) {
            return;
        }
        pageState = pageState.pageLanes(direction);
        retargetVisibleClipCursors();
        selectActiveTrack();
        driver.getOled()
                .valueInfo(
                        "Lanes "
                                + (pageState.lanePage() * VISIBLE_LANES + 1)
                                + "-"
                                + Math.min(
                                        pageState.laneCount(),
                                        (pageState.lanePage() + 1) * VISIBLE_LANES),
                        "Multiclip Seq");
    }

    private void pageTime(final int direction) {
        if (!pageState.canPageTime(direction)) {
            return;
        }
        pageState = pageState.pageTime(direction);
        retargetVisibleClipCursors();
        driver.getOled()
                .valueInfo(
                        "Steps "
                                + (pageState.firstVisibleStep() + 1)
                                + "-"
                                + (pageState.firstVisibleStep()
                                        + MulticlipPageState.STEPS_PER_PAGE),
                        "Time Page");
    }

    private void selectActiveTrack() {
        final int childPosition = pageState.activeChildPosition();
        if (!isEligibleLane(childPosition)) {
            return;
        }
        final Track track = laneBank.getItemAt(childPosition);
        track.selectInMixer();
        track.selectInEditor();
    }

    private void retargetVisibleClipCursors() {
        targetGeneration++;
        for (int row = 0; row < VISIBLE_LANES; row++) {
            final int childPosition = childPositionForRow(row);
            if (childPosition < 0
                    || childPosition >= pageState.laneCount()
                    || !isEligibleLane(childPosition)) {
                clipController.clearRow(row);
                continue;
            }
            final TrackLaneMapping mapping = TrackLaneMapping.fromChildPosition(childPosition);
            clipController.retarget(
                    row,
                    laneBank.getItemAt(childPosition),
                    mapping,
                    pageState.firstVisibleStep(),
                    activeScene);
        }
    }

    private int childPositionForRow(final int row) {
        return pageState.lanePage() * TrackLaneMapping.LANES_PER_PAGE + row;
    }

    private MulticlipTargetIdentity currentTarget(final int row) {
        return new MulticlipTargetIdentity(targetGeneration, childPositionForRow(row), activeScene);
    }

    private void handleStepPress(final int padIndex, final int velocity) {
        selectLaneForPad(padIndex);
        if (!isValidContext()) {
            return;
        }
        final int row = padIndex / MulticlipPageState.STEPS_PER_PAGE;
        final int step = padIndex % MulticlipPageState.STEPS_PER_PAGE;
        final int childPosition = childPositionForRow(row);
        if (childPosition < 0
                || childPosition >= pageState.laneCount()
                || !isEligibleLane(childPosition)) {
            return;
        }
        if (!clipController.isReady(row)) {
            driver.getOled().valueInfo("Lane loading", "Try step again");
            return;
        }
        final Track track = laneBank.getItemAt(childPosition);
        padInteraction.captureOccupied(padIndex, clipController.isOccupied(row, step));
        if (!clipController.exists(row)) {
            createClipAndAddFirstStep(row, step, velocity, track);
            return;
        }
        final int channel = TrackLaneMapping.fromChildPosition(childPosition).midiChannel();
        if (!padInteraction.wasOccupied(padIndex)) {
            clipController.setStep(row, channel, step, velocity, DEFAULT_GATE);
        }
    }

    private void handleStepRelease(final int padIndex) {
        if (!padInteraction.wasOccupied(padIndex) || padInteraction.isConsumed(padIndex)) {
            return;
        }
        final int row = padIndex / MulticlipPageState.STEPS_PER_PAGE;
        final int step = padIndex % MulticlipPageState.STEPS_PER_PAGE;
        final int childPosition = childPositionForRow(row);
        if (childPosition < 0 || childPosition >= pageState.laneCount()) {
            return;
        }
        for (final int channel : clipController.channelsAt(row, step)) {
            clipController.clearStep(row, channel, step);
        }
    }

    private boolean activeLaneHasClip() {
        final int row = pageState.activeRow();
        final int childPosition = pageState.activeChildPosition();
        return row >= 0
                && childPosition >= 0
                && childPosition < pageState.laneCount()
                && clipController.isReady(row)
                && clipController.exists(row);
    }

    private String activeLaneName() {
        final int childPosition = pageState.activeChildPosition();
        return laneName(childPosition);
    }

    private String laneName(final int childPosition) {
        if (childPosition < 0 || childPosition >= laneNames.length) {
            return "Lane";
        }
        final String name = laneNames[childPosition];
        return name == null || name.isBlank() ? "Lane " + (childPosition + 1) : name;
    }

    private void createClipAndAddFirstStep(
            final int row, final int step, final int velocity, final Track track) {
        final PendingStepCreation request =
                new PendingStepCreation(currentTarget(row), row, step, velocity);
        pendingCreations[row] = request;
        track.createNewLauncherClip(activeScene, driver.getDefaultClipLengthBeats());
        clipController.selectSlot(row, activeScene);
        driver.getOled().valueInfo("Creating clip", "Lane " + (childPositionForRow(row) + 1));
        schedulePendingCreation(request, 0);
    }

    private void schedulePendingCreation(final PendingStepCreation request, final int attempt) {
        host.scheduleTask(
                () -> {
                    final int row = request.row();
                    if (!active
                            || pendingCreations[row] != request
                            || !request.matches(currentTarget(row))) {
                        return;
                    }
                    final int childPosition = childPositionForRow(row);
                    if (!clipController.isReady(row) || !clipController.exists(row)) {
                        if (attempt < 7) {
                            schedulePendingCreation(request, attempt + 1);
                        } else {
                            pendingCreations[row] = null;
                            driver.getOled().valueInfo("Clip not ready", "Try step again");
                        }
                        return;
                    }
                    pendingCreations[row] = null;
                    final int channel =
                            TrackLaneMapping.fromChildPosition(childPosition).midiChannel();
                    clipController.setStep(
                            row, channel, request.step(), request.velocity(), DEFAULT_GATE);
                },
                50);
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
        for (int childPosition = 0; childPosition < TrackLaneMapping.MAX_LANES; childPosition++) {
            final int position = childPosition;
            final Track track = laneBank.getItemAt(position);
            track.exists().markInterested();
            track.name().markInterested();
            track.color().markInterested();
            track.canHoldNoteData().markInterested();
            track.mute().markInterested();
            track.solo().markInterested();
            track.exists()
                    .addValueObserver(
                            exists -> {
                                laneExists[position] = exists;
                                refreshLaneCount();
                            });
            track.canHoldNoteData()
                    .addValueObserver(
                            canHoldNotes -> {
                                laneCanHoldNotes[position] = canHoldNotes;
                                if (active) {
                                    ensureActiveLane();
                                    retargetVisibleClipCursors();
                                }
                            });
            track.name().addValueObserver(name -> laneNames[position] = name);
            track.color()
                    .addValueObserver(
                            (red, green, blue) ->
                                    laneColors[position] = ColorLookup.getColor(red, green, blue));
            track.addIsSelectedInMixerObserver(
                    selected -> handleExternalSelection(position, selected));
            track.addIsSelectedInEditorObserver(
                    selected -> handleExternalSelection(position, selected));
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
                                selected -> {
                                    if (!selected || !active) {
                                        return;
                                    }
                                    final int selectedScene =
                                            sceneBank.scrollPosition().get() + visibleScene;
                                    if (selectedScene != activeScene) {
                                        activeScene = selectedScene;
                                        retargetVisibleClipCursors();
                                    }
                                });
            }
            laneExists[position] = track.exists().get();
            laneCanHoldNotes[position] = track.canHoldNoteData().get();
            laneNames[position] = track.name().get();
            laneColors[position] = ColorLookup.getColor(track.color().get());
        }
        refreshLaneCount();
    }

    private void refreshLaneCount() {
        final int count =
                Math.max(0, Math.min(TrackLaneMapping.MAX_LANES, laneBank.channelCount().get()));
        final int activePosition = pageState.activeChildPosition();
        pageState = MulticlipPageState.initial(count);
        if (activePosition >= 0 && activePosition < count) {
            pageState = pageState.withActiveChildPosition(activePosition);
        }
        if (active) {
            retargetVisibleClipCursors();
        }
    }

    private void handleExternalSelection(final int childPosition, final boolean selected) {
        if (!selected || !isEligibleLane(childPosition)) {
            return;
        }
        final int oldPage = pageState.lanePage();
        pageState = pageState.withActiveChildPosition(childPosition);
        if (oldPage != pageState.lanePage()) {
            retargetVisibleClipCursors();
        }
        if (active) {
            showLaneInfo();
        }
    }

    private void ensureActiveLane() {
        if (isEligibleLane(pageState.activeChildPosition())) {
            return;
        }
        for (int childPosition = 0; childPosition < pageState.laneCount(); childPosition++) {
            if (isEligibleLane(childPosition)) {
                pageState = pageState.withActiveChildPosition(childPosition);
                return;
            }
        }
    }

    private void selectLaneForPad(final int padIndex) {
        if (!isValidContext()) {
            showInvalidContext();
            return;
        }
        final int row = padIndex / MulticlipPageState.STEPS_PER_PAGE;
        final int childPosition = pageState.lanePage() * TrackLaneMapping.LANES_PER_PAGE + row;
        if (childPosition < 0
                || childPosition >= pageState.laneCount()
                || !isEligibleLane(childPosition)) {
            return;
        }
        pageState = pageState.withActiveChildPosition(childPosition);
        final Track track = laneBank.getItemAt(childPosition);
        track.selectInMixer();
        track.selectInEditor();
        final int activeRow = pageState.activeRow();
        if (activeRow >= 0
                && clipController.isReady(activeRow)
                && clipController.exists(activeRow)) {
            clipController.showInEditor(activeRow);
        }
        showLaneInfo();
    }

    private RgbLightState laneIdentityLight(final int padIndex) {
        final int row = padIndex / MulticlipPageState.STEPS_PER_PAGE;
        final int step = padIndex % MulticlipPageState.STEPS_PER_PAGE;
        if (sceneOverlay.isActive() && row == 0) {
            return sceneLight(step);
        }
        final int childPosition = pageState.lanePage() * TrackLaneMapping.LANES_PER_PAGE + row;
        if (!isValidContext()
                || childPosition >= pageState.laneCount()
                || !isEligibleLane(childPosition)) {
            return RgbLightState.OFF;
        }
        final RgbLightState color =
                laneColors[childPosition] == null ? RgbLightState.WHITE : laneColors[childPosition];
        if (padInteraction.isHeld(padIndex)) {
            return padInteraction.isConsumed(padIndex)
                    ? RgbLightState.PURPLE
                    : color.getBrightest();
        }
        if (clipController.isPlaying(row, step)) {
            return RgbLightState.WHITE;
        }
        if (clipController.isOccupied(row, step)) {
            return childPosition == pageState.activeChildPosition()
                    ? color.getBrightend()
                    : color.getSoftDimmed();
        }
        return childPosition == pageState.activeChildPosition()
                ? color.getDimmed()
                : color.getVeryDimmed();
    }

    private RgbLightState sceneLight(final int visibleScene) {
        final int absoluteScene = sceneBank.scrollPosition().get() + visibleScene;
        int clips = 0;
        boolean playing = false;
        boolean queued = false;
        RgbLightState childClipColor = RgbLightState.GRAY_1;
        for (int childPosition = 0; childPosition < pageState.laneCount(); childPosition++) {
            if (!isEligibleLane(childPosition)) {
                continue;
            }
            final ClipLauncherSlot slot =
                    laneBank.getItemAt(childPosition)
                            .clipLauncherSlotBank()
                            .getItemAt(visibleScene);
            if (slot.hasContent().get()) {
                clips++;
                if (clips == 1) {
                    childClipColor = ColorLookup.getColor(slot.color().get());
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
            return childClipColor.getBrightend();
        }
        return switch (ScenePopulation.ofChildClips(clips, eligibleLaneCount())) {
            case NEW -> RgbLightState.GRAY_1;
            case PARTIAL -> childClipColor.getDimmed();
            case POPULATED -> childClipColor.getSoftDimmed();
        };
    }

    private boolean isValidContext() {
        if (!groupCursor.exists().get()
                || !groupCursor.isGroup().get()
                || pageState.laneCount() == 0
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
                && childPosition < pageState.laneCount()
                && laneExists[childPosition]
                && laneCanHoldNotes[childPosition];
    }

    private boolean hasEligibleLane() {
        return eligibleLaneCount() > 0;
    }

    private int eligibleLaneCount() {
        int count = 0;
        for (int childPosition = 0; childPosition < pageState.laneCount(); childPosition++) {
            if (isEligibleLane(childPosition)) {
                count++;
            }
        }
        return count;
    }

    private void showInvalidContext() {
        driver.getOled()
                .valueInfo(
                        pageState.laneCount() == 0 ? "No child tracks" : "Select Drum group",
                        "Multiclip Seq");
    }

    private void showLaneInfo() {
        final int position = pageState.activeChildPosition();
        if (position < 0 || position >= pageState.laneCount()) {
            showInvalidContext();
            return;
        }
        driver.getOled()
                .detailInfo(
                        "Multiclip Seq",
                        activeLaneName()
                                + "  Lane "
                                + (position + 1)
                                + "\nLanes "
                                + (pageState.lanePage() * VISIBLE_LANES + 1)
                                + "-"
                                + Math.min(
                                        pageState.laneCount(),
                                        (pageState.lanePage() + 1) * VISIBLE_LANES)
                                + "  Scene "
                                + (activeScene + 1)
                                + "\n"
                                + (clipController.exists(pageState.activeRow())
                                        ? "Clip ready"
                                        : "Empty lane"));
    }

    @Override
    protected void onActivate() {
        active = true;
        groupCursor.isPinned().set(false);
        if (groupCursor.exists().get() && !groupCursor.isGroup().get()) {
            groupCursor.selectParent();
        }
        host.scheduleTask(
                () -> {
                    if (!active) {
                        return;
                    }
                    groupCursor.isPinned().set(true);
                    ensureActiveLane();
                    retargetVisibleClipCursors();
                    if (isValidContext()) {
                        showLaneInfo();
                    } else {
                        showInvalidContext();
                    }
                },
                0);
        padLayer.activate();
        bindPatternButtons();
        showEncoderMode();
    }

    @Override
    protected void onDeactivate() {
        active = false;
        padLayer.deactivate();
        clipController.setPinned(false);
        clearPatternButtons();
        driver.getOled().setFooterLegend(null);
        groupCursor.isPinned().set(false);
        sceneOverlay.altReleased();
        targetGeneration++;
        for (int row = 0; row < VISIBLE_LANES; row++) {
            pendingCreations[row] = null;
        }
        padInteraction.clear();
    }
}
