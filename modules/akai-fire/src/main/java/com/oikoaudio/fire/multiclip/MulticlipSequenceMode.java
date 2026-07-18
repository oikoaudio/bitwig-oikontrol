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
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.utils.PatternButtons;

/** Four-row sequencer whose lanes are direct child tracks of a Drum Machine group. */
public final class MulticlipSequenceMode extends Layer {
    private static final int DEVICE_SCAN_SIZE = 16;
    private static final int SCENE_BANK_SIZE = 16;
    private static final int VISIBLE_LANES = 4;
    private static final double STEP_SIZE = 0.25;
    private static final double DEFAULT_GATE = 0.12;

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
    private final CursorTrack[] laneCursors = new CursorTrack[VISIBLE_LANES];
    private final PinnableCursorClip[] laneClips = new PinnableCursorClip[VISIBLE_LANES];
    private final MulticlipLaneState laneState = new MulticlipLaneState();
    private final PendingStepCreation[] pendingCreations =
            new PendingStepCreation[VISIBLE_LANES];

    private MulticlipPageState pageState = MulticlipPageState.initial(0);
    private int activeScene;
    private long targetGeneration;
    private boolean active;

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
                        heldPads[padIndex] = pressed;
                        if (pressed) {
                            handleStepPress(padIndex, velocity);
                        }
                    }

                    @Override
                    public RgbLightState padLight(final int padIndex) {
                        return laneIdentityLight(padIndex);
                    }
                });
        bindNavigationButtons();
    }

    private void bindNavigationButtons() {
        driver.getButton(NoteAssign.BANK_L)
                .bindPressed(
                        padLayer,
                        pressed -> {
                            if (pressed && !hasHeldPads()) {
                                pageTime(-1);
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
                            if (pressed && !hasHeldPads()) {
                                pageTime(1);
                            }
                        },
                        () -> BiColorLightState.HALF);
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
            laneCursors[row] = cursor;
            laneClips[row] = clip;
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

    private void retargetVisibleClipCursors() {
        targetGeneration++;
        for (int row = 0; row < VISIBLE_LANES; row++) {
            laneState.clearRow(row);
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
        if (!track.clipLauncherSlotBank().getItemAt(activeScene).hasContent().get()) {
            createClipAndAddFirstStep(row, step, velocity, track);
            return;
        }
        final int channel = TrackLaneMapping.fromChildPosition(childPosition).midiChannel();
        if (laneState.isOccupied(row, step)) {
            laneClips[row].clearStep(channel, step, 0);
        } else {
            laneClips[row].setStep(channel, step, 0, velocity, DEFAULT_GATE);
        }
    }

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
    }

    @Override
    protected void onDeactivate() {
        active = false;
        padLayer.deactivate();
        clearPatternButtons();
        groupCursor.isPinned().set(false);
        targetGeneration++;
        for (int row = 0; row < VISIBLE_LANES; row++) {
            pendingCreations[row] = null;
        }
    }
}
