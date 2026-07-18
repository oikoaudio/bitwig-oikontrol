package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.PadMatrixBindings;
import com.oikoaudio.fire.display.EncoderFooterLegend;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.utils.PatternButtons;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Four-row sequencer whose lanes are direct child tracks of a Drum Machine group. */
public final class MulticlipSequenceMode extends Layer {
    private static final int DEVICE_SCAN_SIZE = 16;
    private static final int SCENE_BANK_SIZE = 16;
    private static final int VISIBLE_LANES = 4;
    private static final double STEP_SIZE = 0.25;
    private static final double DEFAULT_GATE = 0.12;
    private static final int FINE_STEPS_PER_COARSE_STEP = 16;
    private static final int FINE_OBSERVATION_STEPS =
            MulticlipTiming.MAX_LOOP_STEPS * FINE_STEPS_PER_COARSE_STEP;

    private final AkaiFireOikontrolExtension driver;
    private final ControllerHost host;
    private final Layer padLayer;
    private final CursorTrack groupCursor;
    private final TrackBank laneBank;
    private final RgbLightState[] laneColors = new RgbLightState[TrackLaneMapping.MAX_LANES];
    private final boolean[] laneExists = new boolean[TrackLaneMapping.MAX_LANES];
    private final String[] laneNames = new String[TrackLaneMapping.MAX_LANES];
    private final boolean[] groupDeviceIsDrumMachine = new boolean[DEVICE_SCAN_SIZE];
    private final boolean[] heldPads = new boolean[64];
    private final boolean[] pressWasOccupied = new boolean[64];
    private final boolean[] heldGestureConsumed = new boolean[64];
    private final CursorTrack[] laneCursors = new CursorTrack[VISIBLE_LANES];
    private final PinnableCursorClip[] laneClips = new PinnableCursorClip[VISIBLE_LANES];
    private final PinnableCursorClip[] fineLaneClips = new PinnableCursorClip[VISIBLE_LANES];
    @SuppressWarnings("unchecked")
    private final Map<Integer, Set<Integer>>[] fineNotes = new Map[VISIBLE_LANES];
    private final MulticlipLaneState laneState = new MulticlipLaneState();
    private final PendingStepCreation[] pendingCreations =
            new PendingStepCreation[VISIBLE_LANES];

    private MulticlipPageState pageState = MulticlipPageState.initial(0);
    private int activeScene;
    private long targetGeneration;
    private boolean active;
    private EncoderMode encoderMode = EncoderMode.CHANNEL;

    public MulticlipSequenceMode(final AkaiFireOikontrolExtension driver) {
        super(driver.getLayers(), "MULTICLIP_SEQUENCE");
        this.driver = driver;
        this.host = driver.getHost();
        this.padLayer = new Layer(driver.getLayers(), "MULTICLIP_SEQUENCE_PADS");
        this.groupCursor =
                host.createCursorTrack(
                        "MULTICLIP_GROUP", "Multiclip Group", 0, SCENE_BANK_SIZE, true);
        groupCursor.exists().markInterested();
        groupCursor.isGroup().markInterested();
        groupCursor.isPinned().markInterested();
        this.laneBank =
                groupCursor.createTrackBank(
                        TrackLaneMapping.MAX_LANES, 0, SCENE_BANK_SIZE, false);
        laneBank.channelCount().markInterested();
        observeGroupDevices();
        observeTrackLanes();
        createVisibleClipCursors();
        PadMatrixBindings.bindPressedVelocity(
                padLayer,
                driver.getRgbButtons(),
                new PadMatrixBindings.Host() {
                    @Override
                    public void handlePadPress(
                            final int padIndex, final boolean pressed, final int velocity) {
                        if (pressed) {
                            heldPads[padIndex] = true;
                            handleStepPress(padIndex, velocity);
                        } else {
                            handleStepRelease(padIndex);
                            heldPads[padIndex] = false;
                            pressWasOccupied[padIndex] = false;
                            heldGestureConsumed[padIndex] = false;
                        }
                    }

                    @Override
                    public RgbLightState padLight(final int padIndex) {
                        return laneIdentityLight(padIndex);
                    }
                });
        bindNavigationButtons();
        bindEncoderControls();
    }

    private void bindNavigationButtons() {
        driver.getButton(NoteAssign.BANK_L)
                .bindPressed(
                        padLayer,
                        pressed -> {
                            if (pressed) {
                                handleGridButton(-1);
                            }
                        },
                        () ->
                                pageState.canPageTime(-1)
                                        ? BiColorLightState.HALF
                                        : BiColorLightState.OFF);
        driver.getButton(NoteAssign.BANK_R)
                .bindPressed(
                        padLayer,
                        pressed -> {
                            if (pressed) {
                                handleGridButton(1);
                            }
                        },
                        () -> BiColorLightState.HALF);
    }

    private void handleGridButton(final int direction) {
        switch (MulticlipGridGesture.resolve(
                driver.isGlobalShiftHeld(), driver.isGlobalAltHeld(), hasHeldPads())) {
            case TIME_PAGE -> pageTime(direction);
            case HELD_STEP_NUDGE -> fineNudge(direction, true);
            case PLAY_START -> movePlayStart(direction);
            case WHOLE_LANE_NUDGE -> fineNudge(direction, false);
        }
    }

    private void bindEncoderControls() {
        driver.getButton(NoteAssign.KNOB_MODE)
                .bindPressed(
                        padLayer,
                        pressed -> {
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
                        },
                        () -> encoderMode.getState());
        for (int index = 0; index < driver.getEncoders().length; index++) {
            final int encoderIndex = index;
            driver.getEncoders()[index]
                    .bindThresholdedEncoder(
                            padLayer,
                            5,
                            10,
                            driver::isGlobalShiftHeld,
                            increment -> {
                                if (encoderMode == EncoderMode.CHANNEL && encoderIndex == 0) {
                                    adjustActiveLoopLength(increment);
                                }
                            });
        }
    }

    private void showEncoderMode() {
        if (encoderMode == EncoderMode.CHANNEL) {
            driver.getOled().detailInfo("Multiclip Channel", "1 Lane Length");
            driver.getOled()
                    .setFooterLegend(EncoderFooterLegend.of("Lgth", "", "", ""));
        } else {
            driver.getOled().detailInfo("Multiclip " + encoderMode, "Reserved");
            driver.getOled().setFooterLegend(null);
        }
    }

    private boolean hasHeldPads() {
        for (final boolean held : heldPads) {
            if (held) {
                return true;
            }
        }
        return false;
    }

    private void bindPatternButtons() {
        final PatternButtons buttons = driver.getPatternButtons();
        if (buttons == null) {
            return;
        }
        buttons.setUpCallback(
                pressed -> {
                    if (pressed) {
                        pageLanes(-1);
                    }
                },
                () ->
                        pageState.canPageLanes(-1)
                                ? BiColorLightState.HALF
                                : BiColorLightState.OFF);
        buttons.setDownCallback(
                pressed -> {
                    if (pressed) {
                        pageLanes(1);
                    }
                },
                () ->
                        pageState.canPageLanes(1)
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
        if (childPosition < 0 || childPosition >= pageState.laneCount()) {
            return;
        }
        final Track track = laneBank.getItemAt(childPosition);
        track.selectInMixer();
        track.selectInEditor();
    }

    private void createVisibleClipCursors() {
        for (int row = 0; row < VISIBLE_LANES; row++) {
            final int laneRow = row;
            final CursorTrack cursor =
                    host.createCursorTrack(
                            "MULTICLIP_LANE_" + (row + 1),
                            "Multiclip Lane " + (row + 1),
                            0,
                            SCENE_BANK_SIZE,
                            false);
            cursor.isPinned().markInterested();
            cursor.isPinned().set(true);
            final PinnableCursorClip clip =
                    cursor.createLauncherCursorClip(
                            "MULTICLIP_CLIP_" + (row + 1),
                            "Multiclip Clip " + (row + 1),
                            MulticlipPageState.STEPS_PER_PAGE,
                            1);
            clip.setStepSize(STEP_SIZE);
            clip.addNoteStepObserver(note -> handleObservedStep(laneRow, note));
            clip.playingStep()
                    .addValueObserver(
                            step -> laneState.setPlayingStep(laneRow, visiblePlayingStep(step)));
            clip.getLoopLength().markInterested();
            clip.getPlayStart().markInterested();
            final PinnableCursorClip fineClip =
                    cursor.createLauncherCursorClip(
                            "MULTICLIP_FINE_" + (row + 1),
                            "Multiclip Fine " + (row + 1),
                            FINE_OBSERVATION_STEPS,
                            1);
            fineClip.setStepSize(MulticlipTiming.FINE_STEP_BEATS);
            fineNotes[row] = new HashMap<>();
            fineClip.addNoteStepObserver(note -> handleFineObservedStep(laneRow, note));
            laneCursors[row] = cursor;
            laneClips[row] = clip;
            fineLaneClips[row] = fineClip;
        }
    }

    private int visiblePlayingStep(final int absoluteStep) {
        if (absoluteStep < pageState.firstVisibleStep()
                || absoluteStep
                        >= pageState.firstVisibleStep()
                                + MulticlipPageState.STEPS_PER_PAGE) {
            return -1;
        }
        return absoluteStep - pageState.firstVisibleStep();
    }

    private void handleObservedStep(final int row, final NoteStep note) {
        if (note.x() < 0 || note.x() >= MulticlipPageState.STEPS_PER_PAGE || note.y() != 0) {
            return;
        }
        final int childPosition = childPositionForRow(row);
        if (childPosition < 0 || childPosition >= pageState.laneCount()) {
            return;
        }
        final int channel = TrackLaneMapping.fromChildPosition(childPosition).midiChannel();
        if (note.channel() == channel) {
            laneState.setOccupied(row, note.x(), note.state() == NoteStep.State.NoteOn);
        }
    }

    private void handleFineObservedStep(final int row, final NoteStep note) {
        if (note.x() < 0 || note.x() >= FINE_OBSERVATION_STEPS || note.y() != 0) {
            return;
        }
        final Set<Integer> channels =
                fineNotes[row].computeIfAbsent(note.x(), ignored -> new HashSet<>());
        if (note.state() == NoteStep.State.NoteOn) {
            channels.add(note.channel());
        } else {
            channels.remove(note.channel());
            if (channels.isEmpty()) {
                fineNotes[row].remove(note.x());
            }
        }
    }

    private void retargetVisibleClipCursors() {
        targetGeneration++;
        for (int row = 0; row < VISIBLE_LANES; row++) {
            laneState.clearRow(row);
            fineNotes[row].clear();
            final int childPosition = childPositionForRow(row);
            if (childPosition < 0
                    || childPosition >= pageState.laneCount()
                    || !laneExists[childPosition]) {
                continue;
            }
            final TrackLaneMapping mapping =
                    TrackLaneMapping.fromChildPosition(childPosition);
            laneCursors[row].selectChannel(laneBank.getItemAt(childPosition));
            laneCursors[row].isPinned().set(true);
            laneClips[row].scrollToKey(mapping.midiNote());
            laneClips[row].scrollToStep(pageState.firstVisibleStep());
            fineLaneClips[row].scrollToKey(mapping.midiNote());
            fineLaneClips[row].scrollToStep(0);
            laneCursors[row].selectSlot(activeScene);
        }
    }

    private int childPositionForRow(final int row) {
        return pageState.lanePage() * TrackLaneMapping.LANES_PER_PAGE + row;
    }

    private MulticlipTargetIdentity currentTarget(final int row) {
        return new MulticlipTargetIdentity(
                targetGeneration, childPositionForRow(row), activeScene);
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
                || !laneExists[childPosition]) {
            return;
        }
        final Track track = laneBank.getItemAt(childPosition);
        pressWasOccupied[padIndex] = laneState.isOccupied(row, step);
        heldGestureConsumed[padIndex] = false;
        if (!track.clipLauncherSlotBank().getItemAt(activeScene).hasContent().get()) {
            createClipAndAddFirstStep(row, step, velocity, track);
            return;
        }
        final int channel = TrackLaneMapping.fromChildPosition(childPosition).midiChannel();
        if (!pressWasOccupied[padIndex]) {
            laneClips[row].setStep(channel, step, 0, velocity, DEFAULT_GATE);
        }
    }

    private void handleStepRelease(final int padIndex) {
        if (!pressWasOccupied[padIndex] || heldGestureConsumed[padIndex]) {
            return;
        }
        final int row = padIndex / MulticlipPageState.STEPS_PER_PAGE;
        final int step = padIndex % MulticlipPageState.STEPS_PER_PAGE;
        final int childPosition = childPositionForRow(row);
        if (childPosition < 0 || childPosition >= pageState.laneCount()) {
            return;
        }
        final int channel = TrackLaneMapping.fromChildPosition(childPosition).midiChannel();
        laneClips[row].clearStep(channel, step, 0);
    }

    private boolean activeLaneHasClip() {
        final int row = pageState.activeRow();
        final int childPosition = pageState.activeChildPosition();
        return row >= 0
                && childPosition >= 0
                && childPosition < pageState.laneCount()
                && laneBank
                        .getItemAt(childPosition)
                        .clipLauncherSlotBank()
                        .getItemAt(activeScene)
                        .hasContent()
                        .get();
    }

    private void adjustActiveLoopLength(final int increment) {
        if (!activeLaneHasClip()) {
            driver.getOled().valueInfo("Empty lane", "Length unchanged");
            return;
        }
        final PinnableCursorClip clip = laneClips[pageState.activeRow()];
        final int currentSteps = MulticlipTiming.stepsForBeats(clip.getLoopLength().get());
        final int newSteps = MulticlipTiming.adjustLoopSteps(currentSteps, increment);
        clip.getLoopLength().set(MulticlipTiming.beatsForSteps(newSteps));
        driver.getOled()
                .valueInfo(
                        "Length " + newSteps + " steps",
                        activeLaneName());
    }

    private void movePlayStart(final int direction) {
        if (!activeLaneHasClip()) {
            driver.getOled().valueInfo("Empty lane", "Rotation unchanged");
            return;
        }
        final PinnableCursorClip clip = laneClips[pageState.activeRow()];
        final double loopLength =
                Math.max(MulticlipTiming.STEP_BEATS, clip.getLoopLength().get());
        double newStart =
                clip.getPlayStart().get() + direction * MulticlipTiming.STEP_BEATS;
        newStart %= loopLength;
        if (newStart < 0) {
            newStart += loopLength;
        }
        clip.getPlayStart().set(newStart);
        driver.getOled()
                .valueInfo(
                        "Play start "
                                + MulticlipTiming.stepsForBeats(newStart)
                                + " steps",
                        activeLaneName());
    }

    private void fineNudge(final int direction, final boolean heldOnly) {
        if (!activeLaneHasClip()) {
            driver.getOled().valueInfo("Empty lane", "Nudge ignored");
            return;
        }
        final int row = pageState.activeRow();
        final int loopFineSteps =
                Math.min(
                        FINE_OBSERVATION_STEPS,
                        Math.max(
                                1,
                                (int)
                                        Math.round(
                                                laneClips[row].getLoopLength().get()
                                                        / MulticlipTiming.FINE_STEP_BEATS)));
        final List<FineTarget> targets = collectFineTargets(row, heldOnly, loopFineSteps);
        targets.sort(
                direction > 0
                        ? Comparator.comparingInt(FineTarget::fineStep).reversed()
                        : Comparator.comparingInt(FineTarget::fineStep));
        for (final FineTarget target : targets) {
            int destination = target.fineStep() + direction;
            if (destination < 0) {
                destination = loopFineSteps - 1;
            } else if (destination >= loopFineSteps) {
                destination = 0;
            }
            fineLaneClips[row]
                    .moveStep(
                            target.channel(),
                            target.fineStep(),
                            0,
                            destination - target.fineStep(),
                            0);
        }
        if (hasHeldPads()) {
            markHeldStepsConsumed(row);
        }
        driver.getOled()
                .valueInfo(
                        heldOnly ? "Step nudge " + signed(direction) : "Lane nudge " + signed(direction),
                        activeLaneName());
    }

    private List<FineTarget> collectFineTargets(
            final int row, final boolean heldOnly, final int loopFineSteps) {
        final List<FineTarget> targets = new ArrayList<>();
        for (final Map.Entry<Integer, Set<Integer>> entry : fineNotes[row].entrySet()) {
            final int fineStep = entry.getKey();
            if (fineStep < 0 || fineStep >= loopFineSteps) {
                continue;
            }
            if (heldOnly && !fineStepBelongsToHeldPad(row, fineStep)) {
                continue;
            }
            for (final int channel : entry.getValue()) {
                targets.add(new FineTarget(fineStep, channel));
            }
        }
        return targets;
    }

    private boolean fineStepBelongsToHeldPad(final int row, final int fineStep) {
        final int coarseStep = fineStep / FINE_STEPS_PER_COARSE_STEP;
        final int visibleStep = coarseStep - pageState.firstVisibleStep();
        if (visibleStep < 0 || visibleStep >= MulticlipPageState.STEPS_PER_PAGE) {
            return false;
        }
        return heldPads[row * MulticlipPageState.STEPS_PER_PAGE + visibleStep];
    }

    private void markHeldStepsConsumed(final int row) {
        final int start = row * MulticlipPageState.STEPS_PER_PAGE;
        for (int step = 0; step < MulticlipPageState.STEPS_PER_PAGE; step++) {
            if (heldPads[start + step]) {
                heldGestureConsumed[start + step] = true;
            }
        }
    }

    private String activeLaneName() {
        final int childPosition = pageState.activeChildPosition();
        if (childPosition < 0 || childPosition >= laneNames.length) {
            return "Lane";
        }
        final String name = laneNames[childPosition];
        return name == null || name.isBlank() ? "Lane " + (childPosition + 1) : name;
    }

    private String signed(final int direction) {
        return direction > 0 ? "+1/64" : "-1/64";
    }

    private record FineTarget(int fineStep, int channel) {}

    private void createClipAndAddFirstStep(
            final int row, final int step, final int velocity, final Track track) {
        final PendingStepCreation request =
                new PendingStepCreation(currentTarget(row), row, step, velocity);
        pendingCreations[row] = request;
        track.createNewLauncherClip(activeScene, 4);
        laneCursors[row].selectSlot(activeScene);
        driver.getOled().valueInfo("Creating clip", "Lane " + (childPositionForRow(row) + 1));
        schedulePendingCreation(request, 0);
    }

    private void schedulePendingCreation(
            final PendingStepCreation request, final int attempt) {
        host.scheduleTask(
                () -> {
                    final int row = request.row();
                    if (!active
                            || pendingCreations[row] != request
                            || !request.matches(currentTarget(row))) {
                        return;
                    }
                    final int childPosition = childPositionForRow(row);
                    final Track track = laneBank.getItemAt(childPosition);
                    final ClipLauncherSlot slot =
                            track.clipLauncherSlotBank().getItemAt(activeScene);
                    if (!slot.hasContent().get()) {
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
                    laneClips[row]
                            .setStep(channel, request.step(), 0, request.velocity(), DEFAULT_GATE);
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
                    .addValueObserver(
                            value -> groupDeviceIsDrumMachine[deviceIndex] = value);
            groupDeviceIsDrumMachine[deviceIndex] = device.hasDrumPads().get();
        }
    }

    private void observeTrackLanes() {
        for (int childPosition = 0;
                childPosition < TrackLaneMapping.MAX_LANES;
                childPosition++) {
            final int position = childPosition;
            final Track track = laneBank.getItemAt(position);
            track.exists().markInterested();
            track.name().markInterested();
            track.color().markInterested();
            track.exists()
                    .addValueObserver(
                            exists -> {
                                laneExists[position] = exists;
                                refreshLaneCount();
                            });
            track.name().addValueObserver(name -> laneNames[position] = name);
            track.color()
                    .addValueObserver(
                            (red, green, blue) ->
                                    laneColors[position] =
                                            ColorLookup.getColor(red, green, blue));
            track.addIsSelectedInMixerObserver(selected -> handleExternalSelection(position, selected));
            track.addIsSelectedInEditorObserver(selected -> handleExternalSelection(position, selected));
            for (int scene = 0; scene < SCENE_BANK_SIZE; scene++) {
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(scene);
                slot.exists().markInterested();
                slot.hasContent().markInterested();
                slot.isSelected().markInterested();
            }
            laneExists[position] = track.exists().get();
            laneNames[position] = track.name().get();
            laneColors[position] = ColorLookup.getColor(track.color().get());
        }
        refreshLaneCount();
    }

    private void refreshLaneCount() {
        int count = 0;
        while (count < laneExists.length && laneExists[count]) {
            count++;
        }
        final int activePosition = pageState.activeChildPosition();
        pageState = MulticlipPageState.initial(count);
        if (activePosition >= 0 && activePosition < count) {
            pageState = pageState.withActiveChildPosition(activePosition);
        }
    }

    private void handleExternalSelection(final int childPosition, final boolean selected) {
        if (!selected || childPosition >= pageState.laneCount()) {
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

    private void selectLaneForPad(final int padIndex) {
        if (!isValidContext()) {
            showInvalidContext();
            return;
        }
        final int row = padIndex / MulticlipPageState.STEPS_PER_PAGE;
        final int childPosition =
                pageState.lanePage() * TrackLaneMapping.LANES_PER_PAGE + row;
        if (childPosition < 0
                || childPosition >= pageState.laneCount()
                || !laneExists[childPosition]) {
            return;
        }
        pageState = pageState.withActiveChildPosition(childPosition);
        final Track track = laneBank.getItemAt(childPosition);
        track.selectInMixer();
        track.selectInEditor();
        showLaneInfo();
    }

    private RgbLightState laneIdentityLight(final int padIndex) {
        final int row = padIndex / MulticlipPageState.STEPS_PER_PAGE;
        final int step = padIndex % MulticlipPageState.STEPS_PER_PAGE;
        final int childPosition =
                pageState.lanePage() * TrackLaneMapping.LANES_PER_PAGE + row;
        if (!isValidContext()
                || childPosition >= pageState.laneCount()
                || !laneExists[childPosition]) {
            return RgbLightState.OFF;
        }
        final RgbLightState color =
                laneColors[childPosition] == null
                        ? RgbLightState.WHITE
                        : laneColors[childPosition];
        if (laneState.isPlaying(row, step)) {
            return RgbLightState.WHITE;
        }
        if (laneState.isOccupied(row, step)) {
            return childPosition == pageState.activeChildPosition()
                    ? color.getBrightend()
                    : color.getSoftDimmed();
        }
        return childPosition == pageState.activeChildPosition()
                ? color.getDimmed()
                : color.getVeryDimmed();
    }

    private boolean isValidContext() {
        if (!groupCursor.exists().get()
                || !groupCursor.isGroup().get()
                || pageState.laneCount() == 0) {
            return false;
        }
        for (final boolean drumMachine : groupDeviceIsDrumMachine) {
            if (drumMachine) {
                return true;
            }
        }
        return false;
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
                .valueInfo(
                        "Lane " + (position + 1),
                        laneNames[position] == null ? "Track" : laneNames[position]);
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
        clearPatternButtons();
        driver.getOled().setFooterLegend(null);
        groupCursor.isPinned().set(false);
        targetGeneration++;
        for (int row = 0; row < VISIBLE_LANES; row++) {
            pendingCreations[row] = null;
        }
    }
}
