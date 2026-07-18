package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.sequence.StepPadLightHelper;
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
    private final MulticlipDrumPadEncoderController drumPadEncoderController;
    private final MulticlipEncoderController encoderController;

    private final RgbLightState[] laneColors = new RgbLightState[MAX_LANES];
    private final boolean[] laneExists = new boolean[MAX_LANES];
    private final boolean[] laneCanHoldNotes = new boolean[MAX_LANES];
    private final String[] laneNames = new String[MAX_LANES];
    private final boolean[] groupDeviceIsDrumMachine = new boolean[DEVICE_SCAN_SIZE];

    private int laneCount;
    private int activeChildPosition = -1;
    private int activeScene;
    private int firstVisibleStep;
    private int blinkState;
    private long targetGeneration;
    private long creationGeneration;
    private boolean active;
    private boolean groupContextReady;
    private boolean cursorRetargetInProgress;
    private boolean groupSelected;
    private boolean selectHeld;
    private boolean lastStepHeld;
    private boolean copyHeld;
    private boolean deleteHeld;
    private boolean laneMuteMode;
    private boolean laneSoloMode;

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
        final PinnableCursorDevice drumMachine =
                groupCursor.createCursorDevice(
                        "MULTICLIP_DRUM_MACHINE",
                        "Multiclip Drum Machine",
                        2,
                        CursorDeviceFollowMode.FIRST_INSTRUMENT);
        drumMachine.exists().markInterested();
        drumMachine.hasDrumPads().markInterested();
        final DrumPadBank drumPads = drumMachine.createDrumPadBank(MAX_LANES);
        drumPadEncoderController =
                new MulticlipDrumPadEncoderController(drumPads, driver.getOled());
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
        encoderController =
                new MulticlipEncoderController(
                        driver,
                        clipController,
                        padInteraction,
                        drumPadEncoderController,
                        new MulticlipEncoderController.Context() {
                            @Override
                            public boolean selectHeld() {
                                return MulticlipSequenceMode.this.selectHeld;
                            }

                            @Override
                            public String activeLaneName() {
                                return MulticlipSequenceMode.this.activeLaneName();
                            }

                            @Override
                            public int activeChildPosition() {
                                return activeChildPosition;
                            }

                            @Override
                            public int activeScene() {
                                return activeScene;
                            }

                            @Override
                            public int firstVisibleStep() {
                                return firstVisibleStep;
                            }

                            @Override
                            public int midiChannel() {
                                return isEligibleLane(activeChildPosition)
                                        ? activeMapping().midiChannel()
                                        : 0;
                            }

                            @Override
                            public boolean activeClipExists() {
                                return activeLaneHasClip();
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
                            final int padIndex, final boolean pressed, final int ignoredVelocity) {
                        handlePad(padIndex, pressed);
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
                        handleRowButton(row, pressed);
                    }

                    @Override
                    public BiColorLightState rowLight(final int row) {
                        return rowButtonLight(row);
                    }
                });
    }

    private void handlePad(final int padIndex, final boolean pressed) {
        if (MulticlipXoxLayout.isScenePad(padIndex)) {
            handleScenePad(MulticlipXoxLayout.sceneInPage(padIndex), pressed);
            return;
        }
        if (MulticlipXoxLayout.isLanePad(padIndex)) {
            if (pressed) {
                handleLanePad(MulticlipXoxLayout.childPosition(padIndex));
            }
            return;
        }
        if (!MulticlipXoxLayout.isPatternPad(padIndex)) {
            return;
        }
        if (pressed) {
            padInteraction.press(padIndex);
            if (deleteHeld) {
                padInteraction.consume(padIndex);
                deleteStep(padIndex);
            } else if (lastStepHeld) {
                padInteraction.consume(padIndex);
                setLastStep(padIndex);
            } else {
                handleStepPress(padIndex);
            }
        } else {
            handleStepRelease(padIndex);
            padInteraction.release(padIndex);
        }
    }

    private void handleScenePad(final int visibleScene, final boolean pressed) {
        if (!groupContextReady) {
            showInvalidContext();
            return;
        }
        if (deleteHeld) {
            if (pressed) {
                driver.getOled().valueInfo("Delete", "Use a pattern pad");
            }
            return;
        }
        final int absoluteScene = sceneBank.scrollPosition().get() + visibleScene;
        final MulticlipSceneActionResolver.Action action =
                MulticlipSceneActionResolver.resolve(
                        pressed,
                        absoluteScene < sceneBank.itemCount().get(),
                        driver.isGlobalAltHeld(),
                        selectHeld,
                        copyHeld,
                        driver.isGlobalShiftHeld());
        switch (action) {
            case IGNORE -> {
                if (pressed) {
                    driver.getOled().valueInfo("Empty scene", "ALT + pad to edit");
                }
            }
            case LAUNCH_AND_FOLLOW -> launchSceneAndFollow(visibleScene, absoluteScene);
            case SELECT -> selectSceneForEditing(absoluteScene);
            case COPY_CLIP -> copyLaneClipToScene(absoluteScene);
            case COPY_SCENE -> copyChildSceneToScene(absoluteScene);
        }
    }

    private void launchSceneAndFollow(final int visibleScene, final int absoluteScene) {
        MulticlipChildSceneLauncher.launch(eligibleChildSlots(visibleScene));
        creationGeneration++;
        activeScene = absoluteScene;
        groupSelected = false;
        focusActiveTarget(null);
        driver.getOled().valueInfo("Launch Scene " + (activeScene + 1), "Queued + editing");
    }

    private void selectSceneForEditing(final int absoluteScene) {
        creationGeneration++;
        sceneCreator.ensureExists(
                absoluteScene,
                created -> {
                    if (!active || !created) {
                        return;
                    }
                    activeScene = absoluteScene;
                    groupSelected = false;
                    focusActiveTarget(null);
                    driver.getOled()
                            .valueInfo("Scene " + (activeScene + 1), "Selected for editing");
                });
    }

    private void copyLaneClipToScene(final int targetScene) {
        ensureCopyTarget(
                targetScene,
                () -> {
                    final ClipLauncherSlot source = slot(activeChildPosition, activeScene);
                    final ClipLauncherSlot target = slot(activeChildPosition, targetScene);
                    final boolean copied = MulticlipChildCopyController.copyClip(source, target);
                    driver.getOled()
                            .valueInfo(
                                    copied ? "Paste Lane Clip" : "Nothing to paste",
                                    "Scene " + (targetScene + 1));
                });
    }

    private void copyChildSceneToScene(final int targetScene) {
        ensureCopyTarget(
                targetScene,
                () -> {
                    final List<MulticlipChildCopyController.SlotPair> pairs = new ArrayList<>();
                    for (int child = 0; child < laneCount; child++) {
                        if (isEligibleLane(child)) {
                            pairs.add(
                                    new MulticlipChildCopyController.SlotPair(
                                            slot(child, activeScene), slot(child, targetScene)));
                        }
                    }
                    final int changed = MulticlipChildCopyController.copyScene(pairs);
                    driver.getOled()
                            .valueInfo(
                                    "Paste Child Scene",
                                    changed + " lane" + (changed == 1 ? "" : "s"));
                });
    }

    private void ensureCopyTarget(final int targetScene, final Runnable copyAction) {
        if (!isValidContext() || !isEligibleLane(activeChildPosition)) {
            showInvalidContext();
            return;
        }
        if (!isSceneVisible(activeScene)) {
            driver.getOled().valueInfo("Source off page", "Page back to source");
            return;
        }
        if (targetScene == activeScene) {
            driver.getOled().valueInfo("Same scene", "Choose destination");
            return;
        }
        creationGeneration++;
        sceneCreator.ensureExists(
                targetScene,
                created -> {
                    if (active && created && isSceneVisible(targetScene)) {
                        copyAction.run();
                    }
                });
    }

    private ClipLauncherSlot slot(final int childPosition, final int absoluteScene) {
        final int visibleScene = absoluteScene - sceneBank.scrollPosition().get();
        return laneBank.getItemAt(childPosition).clipLauncherSlotBank().getItemAt(visibleScene);
    }

    private boolean isSceneVisible(final int absoluteScene) {
        final int visibleScene = absoluteScene - sceneBank.scrollPosition().get();
        return visibleScene >= 0 && visibleScene < SCENE_BANK_SIZE;
    }

    private void handleLanePad(final int childPosition) {
        if (!isEligibleLane(childPosition)) {
            return;
        }
        if (deleteHeld) {
            driver.getOled().valueInfo("Delete", "Use a pattern pad");
            return;
        }
        final Track track = laneBank.getItemAt(childPosition);
        if (laneMuteMode) {
            final boolean muted = !track.mute().get();
            track.mute().set(muted);
            driver.getOled().valueInfo(muted ? "Mute On" : "Mute Off", laneName(childPosition));
            return;
        }
        if (laneSoloMode) {
            final boolean soloed = !track.solo().get();
            track.solo().set(soloed);
            driver.getOled().valueInfo(soloed ? "Solo On" : "Solo Off", laneName(childPosition));
            return;
        }
        selectLane(childPosition);
    }

    private void selectLane(final int childPosition) {
        if (!isEligibleLane(childPosition)) {
            return;
        }
        activeChildPosition = childPosition;
        groupSelected = false;
        drumPadEncoderController.setActivePad(childPosition);
        creationGeneration++;
        focusActiveTarget(null);
        showLaneInfo();
    }

    private void focusActiveTarget(final Runnable afterReady) {
        if (!active || !isValidContext() || !isEligibleLane(activeChildPosition)) {
            return;
        }
        groupSelected = false;
        drumPadEncoderController.setActivePad(activeChildPosition);
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

    private void handleStepPress(final int padIndex) {
        if (!isValidContext()) {
            showInvalidContext();
            return;
        }
        if (groupSelected) {
            driver.getOled().valueInfo("Group selected", "Choose a lane first");
            return;
        }
        if (cursorRetargetInProgress || !clipController.isReady()) {
            driver.getOled().valueInfo("Clip loading", "Pad input blocked");
            return;
        }
        final int step = MulticlipXoxLayout.visibleStep(padIndex);
        if (!clipController.exists()) {
            padInteraction.captureOccupied(padIndex, false);
            createClipAndAddFirstStep(step);
            return;
        }
        if (!MulticlipTiming.isVisibleStepWithinLoop(
                clipController.loopLength(), firstVisibleStep, step)) {
            padInteraction.consume(padIndex);
            driver.getOled().valueInfo("Outside clip", "Hold Last Step to extend");
            return;
        }
        padInteraction.captureOccupied(padIndex, clipController.isOccupied(step));
        if (!padInteraction.wasOccupied(padIndex)) {
            clipController.setStep(
                    activeMapping().midiChannel(),
                    step,
                    encoderController.insertionVelocity(),
                    DEFAULT_GATE,
                    encoderController.insertionDefaults());
        }
    }

    private void setLastStep(final int padIndex) {
        if (!activeLaneHasClip()) {
            driver.getOled().valueInfo("Empty lane", "Length unchanged");
            return;
        }
        final int steps = firstVisibleStep + MulticlipXoxLayout.visibleStep(padIndex) + 1;
        clipController.setLoopLength(MulticlipTiming.beatsForSteps(steps));
        driver.getOled().valueInfo("Length " + steps + " steps", activeLaneName());
    }

    private void deleteStep(final int padIndex) {
        if (cursorRetargetInProgress || !clipController.isReady() || groupSelected) {
            driver.getOled().valueInfo("Step unavailable", "Select a Lane Clip");
            return;
        }
        final int step = MulticlipXoxLayout.visibleStep(padIndex);
        for (final int channel : clipController.channelsAt(step)) {
            clipController.clearStep(channel, step);
        }
        driver.getOled()
                .valueInfo("Delete step " + (firstVisibleStep + step + 1), activeLaneName());
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

    private void createClipAndAddFirstStep(final int step) {
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
        awaitCreatedClip(creation, childPosition, scene, slot, step, 0);
    }

    private void awaitCreatedClip(
            final long creation,
            final int childPosition,
            final int scene,
            final ClipLauncherSlot slot,
            final int step,
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
                                    creation, childPosition, scene, slot, step, attempt + 1);
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
                                            encoderController.insertionVelocity(),
                                            DEFAULT_GATE,
                                            encoderController.insertionDefaults());
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

    private void handleRowButton(final int row, final boolean pressed) {
        switch (row) {
            case 0 -> handleSelectButton(pressed);
            case 1 -> handleLastStepButton(pressed);
            case 2 -> handleCopyButton(pressed);
            case 3 -> handleDeleteButton(pressed);
            default -> {
                // The Fire exposes four row buttons.
            }
        }
    }

    private void handleSelectButton(final boolean pressed) {
        if (pressed && driver.isGlobalShiftHeld()) {
            laneMuteMode = !laneMuteMode;
            laneSoloMode = false;
            selectHeld = false;
            driver.getOled().valueInfo("Lane Mute", laneMuteMode ? "On" : "Off");
            return;
        }
        if (pressed) {
            laneMuteMode = false;
        }
        selectHeld = pressed;
        showEditButtonFeedback(0, pressed);
    }

    private void handleLastStepButton(final boolean pressed) {
        if (pressed && driver.isGlobalAltHeld()) {
            lastStepHeld = false;
            selectContainingGroup();
            return;
        }
        if (pressed && driver.isGlobalShiftHeld()) {
            laneSoloMode = !laneSoloMode;
            laneMuteMode = false;
            lastStepHeld = false;
            driver.getOled().valueInfo("Lane Solo", laneSoloMode ? "On" : "Off");
            return;
        }
        if (pressed) {
            laneSoloMode = false;
        }
        lastStepHeld = pressed;
        showEditButtonFeedback(1, pressed);
    }

    private void handleCopyButton(final boolean pressed) {
        copyHeld = pressed;
        showEditButtonFeedback(2, pressed);
    }

    private void handleDeleteButton(final boolean pressed) {
        deleteHeld = pressed;
        encoderController.setDeleteHeld(pressed);
        showEditButtonFeedback(3, pressed);
    }

    private void showEditButtonFeedback(final int row, final boolean pressed) {
        if (!pressed) {
            driver.getOled().clearScreenDelayed();
            return;
        }
        final MulticlipEditButtonFeedback.Message message =
                MulticlipEditButtonFeedback.message(row, driver.isGlobalShiftHeld());
        driver.getOled().valueInfo(message.title(), message.detail());
    }

    private void selectContainingGroup() {
        if (!groupContextReady || !groupCursor.exists().get()) {
            showInvalidContext();
            return;
        }
        creationGeneration++;
        targetGeneration++;
        cursorRetargetInProgress = false;
        clipController.clear();
        groupSelected = true;
        groupCursor.selectInMixer();
        groupCursor.selectInEditor();
        driver.getOled().valueInfo("Drum group", "Device control");
    }

    private BiColorLightState rowButtonLight(final int row) {
        final boolean activeState =
                switch (row) {
                    case 0 -> selectHeld || laneMuteMode;
                    case 1 -> lastStepHeld || laneSoloMode || groupSelected;
                    case 2 -> copyHeld;
                    case 3 -> deleteHeld;
                    default -> false;
                };
        return activeState ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF;
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
        final Track track = laneBank.getItemAt(childPosition);
        return laneLightState(
                color,
                childPosition == activeChildPosition,
                laneMuteMode,
                track.mute().get(),
                laneSoloMode,
                track.solo().get());
    }

    static RgbLightState laneLightState(
            final RgbLightState color,
            final boolean activeLane,
            final boolean muteMode,
            final boolean muted,
            final boolean soloMode,
            final boolean soloed) {
        if (muteMode) {
            return muted ? color.getVeryDimmed() : color.getBrightest();
        }
        if (soloMode) {
            return soloed ? color.getBrightest() : color.getVeryDimmed();
        }
        return activeLane ? color.getBrightest() : color.getSoftDimmed();
    }

    private RgbLightState patternLight(final int padIndex) {
        if (!isValidContext() || !isEligibleLane(activeChildPosition)) {
            return RgbLightState.OFF;
        }
        final int step = MulticlipXoxLayout.visibleStep(padIndex);
        final RgbLightState color = laneColor(activeChildPosition);
        if (lastStepHeld) {
            return lastStepLight(step, color);
        }
        final boolean hasClip = activeLaneHasClip();
        final boolean withinLoop =
                !hasClip
                        || MulticlipTiming.isVisibleStepWithinLoop(
                                clipController.loopLength(), firstVisibleStep, step);
        final int shiftedPlayStart =
                hasClip
                        ? StepPadLightHelper.nearestVisibleStepForShiftedClipStart(
                                clipController.playStart(),
                                clipController.loopLength(),
                                MulticlipTiming.STEP_BEATS,
                                firstVisibleStep,
                                MulticlipXoxLayout.PATTERN_COUNT)
                        : -1;
        return MulticlipPatternLight.render(
                color,
                withinLoop,
                padInteraction.isHeld(padIndex),
                padInteraction.isConsumed(padIndex),
                clipController.isPlaying(step),
                clipController.isOccupied(step),
                step == shiftedPlayStart);
    }

    private RgbLightState lastStepLight(final int step, final RgbLightState color) {
        if (!activeLaneHasClip()) {
            return RgbLightState.OFF;
        }
        final int visibleLoopSteps =
                MulticlipTiming.visibleLoopStepCount(
                        clipController.loopLength(),
                        firstVisibleStep,
                        MulticlipXoxLayout.PATTERN_COUNT);
        if (step >= visibleLoopSteps) {
            return RgbLightState.OFF;
        }
        final int loopSteps = MulticlipTiming.stepsForBeats(clipController.loopLength());
        if (firstVisibleStep + step == loopSteps - 1) {
            return RgbLightState.WHITE;
        }
        return clipController.isOccupied(step) ? color.getBrightend() : color.getVeryDimmed();
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
        final boolean selected = absoluteScene == activeScene;
        final RgbLightState idle =
                selected
                        ? childColor.getBrightend()
                        : switch (ScenePopulation.ofChildClips(clips, eligibleLaneCount())) {
                            case NEW -> RgbLightState.GRAY_1;
                            case PARTIAL -> childColor.getDimmed();
                            case POPULATED -> childColor.getSoftDimmed();
                        };
        return MulticlipPlaybackLight.render(
                childColor, selected, playing, queued, idle, blinkState);
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
        groupSelected = false;
        drumPadEncoderController.setActivePad(childPosition);
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
        groupSelected = false;
        drumPadEncoderController.setActivePad(childPosition);
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
                drumPadEncoderController.setActivePad(child);
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
                    drumPadEncoderController.setActivePad(child);
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
        return hasDrumMachine();
    }

    private boolean hasDrumMachine() {
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
        final MulticlipContextFeedback.Message message = invalidContextMessage();
        driver.getOled().valueInfo(message.title(), message.detail());
    }

    private MulticlipContextFeedback.Message invalidContextMessage() {
        return MulticlipContextFeedback.message(
                groupContextReady, hasDrumMachine(), laneCount, eligibleLaneCount());
    }

    public boolean showIdleInfoIfNeeded() {
        if (!active || isValidContext()) {
            return false;
        }
        final MulticlipContextFeedback.Message message = invalidContextMessage();
        driver.getOled().valueInfoPersistentNoClear(message.title(), message.detail());
        return true;
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

    public CursorRemoteControlsPage getActiveRemoteControlsPage() {
        return encoderController.getActiveRemoteControlsPage();
    }

    public void notifyBlink(final int blinkTicks) {
        blinkState = blinkTicks;
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
        encoderController.activate();
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
        encoderController.deactivate();
        clearPatternButtons();
    }
}
