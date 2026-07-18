package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.Track;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/** Owns the row-scoped Lane Clip cursors, with only the active row pinned for editing. */
final class MulticlipClipController {
    static final int VISIBLE_LANES = 4;
    private static final int SCENE_BANK_SIZE = 16;
    private static final int FINE_STEPS_PER_COARSE_STEP = 16;
    private static final int FINE_OBSERVATION_STEPS =
            MulticlipTiming.MAX_LOOP_STEPS * FINE_STEPS_PER_COARSE_STEP;

    private final ControllerHost host;
    private final CursorTrack[] cursors = new CursorTrack[VISIBLE_LANES];
    private final PinnableCursorClip[] clips = new PinnableCursorClip[VISIBLE_LANES];
    private final PinnableCursorClip[] fineClips = new PinnableCursorClip[VISIBLE_LANES];
    private final MulticlipLaneState state = new MulticlipLaneState();

    @SuppressWarnings("unchecked")
    private final Map<Integer, Set<Integer>>[] fineNotes = new Map[VISIBLE_LANES];

    private final int[] firstVisibleSteps = new int[VISIBLE_LANES];
    private final long[] targetGenerations = new long[VISIBLE_LANES];

    MulticlipClipController(final ControllerHost host) {
        this.host = host;
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
            cursor.isPinned().set(false);
            final PinnableCursorClip clip =
                    cursor.createLauncherCursorClip(
                            "MULTICLIP_CLIP_" + (row + 1),
                            "Multiclip Clip " + (row + 1),
                            MulticlipPageState.STEPS_PER_PAGE,
                            1);
            clip.isPinned().markInterested();
            clip.exists().markInterested();
            clip.setStepSize(MulticlipTiming.STEP_BEATS);
            clip.addNoteStepObserver(note -> observeStep(laneRow, note));
            clip.playingStep().addValueObserver(step -> observePlayingStep(laneRow, step));
            clip.getLoopLength().markInterested();
            clip.getPlayStart().markInterested();

            final PinnableCursorClip fineClip =
                    cursor.createLauncherCursorClip(
                            "MULTICLIP_FINE_" + (row + 1),
                            "Multiclip Fine " + (row + 1),
                            FINE_OBSERVATION_STEPS,
                            1);
            fineClip.isPinned().markInterested();
            fineClip.setStepSize(MulticlipTiming.FINE_STEP_BEATS);
            fineNotes[row] = new HashMap<>();
            fineClip.addNoteStepObserver(note -> observeFineStep(laneRow, note));
            cursors[row] = cursor;
            clips[row] = clip;
            fineClips[row] = fineClip;
        }
    }

    void retarget(
            final int row,
            final Track track,
            final ClipLauncherSlot targetSlot,
            final TrackLaneMapping mapping,
            final int firstVisibleStep,
            final Runnable onReady) {
        final long generation = ++targetGenerations[row];
        beginRetarget(row);
        firstVisibleSteps[row] = firstVisibleStep;
        cursors[row].selectChannel(track);
        cursors[row].isPinned().set(true);
        clips[row].scrollToKey(mapping.midiNote());
        clips[row].scrollToStep(firstVisibleStep);
        fineClips[row].scrollToKey(mapping.midiNote());
        fineClips[row].scrollToStep(0);
        selectIndependentSlot(row, targetSlot);
        scheduleObservationRefresh(row, generation, onReady);
    }

    void clearRow(final int row) {
        targetGenerations[row]++;
        state.deactivateRow(row);
        fineNotes[row].clear();
        cursors[row].isPinned().set(false);
        clips[row].isPinned().set(false);
        fineClips[row].isPinned().set(false);
    }

    boolean isReady(final int row) {
        return state.isReady(row);
    }

    void setPinned(final boolean pinned) {
        for (int row = 0; row < VISIBLE_LANES; row++) {
            cursors[row].isPinned().set(pinned);
            clips[row].isPinned().set(pinned);
            fineClips[row].isPinned().set(pinned);
        }
    }

    boolean exists(final int row) {
        return clips[row].exists().get();
    }

    boolean isOccupied(final int row, final int step) {
        return state.isOccupied(row, step);
    }

    Set<Integer> channelsAt(final int row, final int step) {
        return state.channelsAt(row, step);
    }

    boolean isPlaying(final int row, final int step) {
        return state.isPlaying(row, step);
    }

    void setStep(
            final int row,
            final int channel,
            final int step,
            final int velocity,
            final double duration) {
        clips[row].setStep(channel, step, 0, velocity, duration);
    }

    void clearStep(final int row, final int channel, final int step) {
        clips[row].clearStep(channel, step, 0);
    }

    private void scheduleObservationRefresh(
            final int row, final long generation, final Runnable onReady) {
        host.scheduleTask(
                () -> {
                    if (targetGenerations[row] != generation) {
                        return;
                    }
                    clips[row].isPinned().set(true);
                    fineClips[row].isPinned().set(true);
                    clips[row].scrollToStep(firstVisibleSteps[row]);
                    fineClips[row].scrollToStep(0);
                    scheduleWriteReadiness(row, generation, onReady);
                },
                50);
    }

    private void scheduleWriteReadiness(
            final int row, final long generation, final Runnable onReady) {
        host.scheduleTask(
                () -> {
                    if (targetGenerations[row] == generation) {
                        state.finishRetarget(row);
                        onReady.run();
                    }
                },
                50);
    }

    double loopLength(final int row) {
        return clips[row].getLoopLength().get();
    }

    void setLoopLength(final int row, final double beats) {
        clips[row].getLoopLength().set(beats);
    }

    double playStart(final int row) {
        return clips[row].getPlayStart().get();
    }

    void setPlayStart(final int row, final double beats) {
        clips[row].getPlayStart().set(beats);
    }

    void showInEditor(final int row) {
        clips[row].showInEditor();
    }

    int fineNudge(final int row, final int direction, final IntPredicate includeFineStep) {
        final int loopFineSteps =
                Math.min(
                        FINE_OBSERVATION_STEPS,
                        Math.max(
                                1,
                                (int)
                                        Math.round(
                                                loopLength(row)
                                                        / MulticlipTiming.FINE_STEP_BEATS)));
        final List<FineTarget> targets = new ArrayList<>();
        for (final Map.Entry<Integer, Set<Integer>> entry : fineNotes[row].entrySet()) {
            final int fineStep = entry.getKey();
            if (fineStep < 0 || fineStep >= loopFineSteps || !includeFineStep.test(fineStep)) {
                continue;
            }
            for (final int channel : entry.getValue()) {
                targets.add(new FineTarget(fineStep, channel));
            }
        }
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
            fineClips[row].moveStep(
                    target.channel(), target.fineStep(), 0, destination - target.fineStep(), 0);
        }
        return targets.size();
    }

    static int coarseStepForFineStep(final int fineStep) {
        return fineStep / FINE_STEPS_PER_COARSE_STEP;
    }

    private void observeStep(final int row, final NoteStep note) {
        if (!state.acceptsObservations(row)
                || note.x() < 0
                || note.x() >= MulticlipPageState.STEPS_PER_PAGE
                || note.y() != 0) {
            return;
        }
        state.observeChannel(row, note.x(), note.channel(), note.state() == NoteStep.State.NoteOn);
    }

    private void observePlayingStep(final int row, final int absoluteStep) {
        if (!state.acceptsObservations(row)) {
            return;
        }
        final int firstVisibleStep = firstVisibleSteps[row];
        if (absoluteStep < firstVisibleStep
                || absoluteStep >= firstVisibleStep + MulticlipPageState.STEPS_PER_PAGE) {
            state.setPlayingStep(row, -1);
        } else {
            state.setPlayingStep(row, absoluteStep - firstVisibleStep);
        }
    }

    private void observeFineStep(final int row, final NoteStep note) {
        if (!state.acceptsObservations(row)
                || note.x() < 0
                || note.x() >= FINE_OBSERVATION_STEPS
                || note.y() != 0) {
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

    private record FineTarget(int fineStep, int channel) {}

    private void beginRetarget(final int row) {
        state.beginRetarget(row);
        fineNotes[row].clear();
    }

    private void selectIndependentSlot(final int row, final ClipLauncherSlot targetSlot) {
        clips[row].isPinned().set(false);
        fineClips[row].isPinned().set(false);
        targetSlot.select();
    }
}
