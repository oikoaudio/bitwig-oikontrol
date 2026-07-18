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
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/** Observes and edits the one child-track clip selected by Multiclip Seq. */
final class MulticlipClipController {
    private static final int MAX_TARGET_ATTEMPTS = 20;
    private static final long TARGET_RETRY_MS = 50;
    private static final int FINE_STEPS_PER_COARSE_STEP = 16;
    private static final int FINE_OBSERVATION_STEPS =
            MulticlipTiming.MAX_LOOP_STEPS * FINE_STEPS_PER_COARSE_STEP;

    private final ControllerHost host;
    private final CursorTrack cursor;
    private final PinnableCursorClip clip;
    private final PinnableCursorClip fineClip;
    private final Set<Integer>[] observedChannels;
    private final Map<Integer, Set<Integer>> fineNotes = new HashMap<>();

    private long targetGeneration;
    private int firstVisibleStep;
    private int midiNote;
    private int playingStep = -1;
    private boolean ready;

    @SuppressWarnings("unchecked")
    MulticlipClipController(final ControllerHost host, final CursorTrack cursor) {
        this.host = host;
        this.cursor = cursor;
        observedChannels = new Set[MulticlipXoxLayout.PATTERN_COUNT];
        for (int step = 0; step < observedChannels.length; step++) {
            observedChannels[step] = new HashSet<>();
        }

        clip =
                cursor.createLauncherCursorClip(
                        "MULTICLIP_ACTIVE_CLIP",
                        "Multiclip Active Clip",
                        MulticlipXoxLayout.PATTERN_COUNT,
                        1);
        clip.isPinned().markInterested();
        clip.exists().markInterested();
        clip.clipLauncherSlot().sceneIndex().markInterested();
        clip.getLoopLength().markInterested();
        clip.getPlayStart().markInterested();
        clip.setStepSize(MulticlipTiming.STEP_BEATS);
        clip.addNoteStepObserver(this::observeStep);
        clip.playingStep().addValueObserver(this::observePlayingStep);

        fineClip =
                cursor.createLauncherCursorClip(
                        "MULTICLIP_ACTIVE_FINE",
                        "Multiclip Active Fine",
                        FINE_OBSERVATION_STEPS,
                        1);
        fineClip.isPinned().markInterested();
        fineClip.exists().markInterested();
        fineClip.clipLauncherSlot().sceneIndex().markInterested();
        fineClip.setStepSize(MulticlipTiming.FINE_STEP_BEATS);
        fineClip.addNoteStepObserver(this::observeFineStep);
    }

    void retarget(
            final Track track,
            final ClipLauncherSlot targetSlot,
            final TrackLaneMapping mapping,
            final int firstVisibleStep,
            final int absoluteScene,
            final boolean expectedClip,
            final Consumer<Boolean> onComplete) {
        final long generation = ++targetGeneration;
        clearObservedState();
        this.firstVisibleStep = firstVisibleStep;
        midiNote = mapping.midiNote();
        cursor.isPinned().set(false);
        clip.isPinned().set(false);
        fineClip.isPinned().set(false);
        cursor.selectChannel(track);
        targetSlot.select();
        scheduleTarget(generation, track, absoluteScene, expectedClip, onComplete, 0);
    }

    private void scheduleTarget(
            final long generation,
            final Track track,
            final int absoluteScene,
            final boolean expectedClip,
            final Consumer<Boolean> onComplete,
            final int attempt) {
        host.scheduleTask(
                () -> {
                    if (targetGeneration != generation) {
                        return;
                    }
                    if (cursor.position().get() != track.position().get()
                            || !clipTargetSettled(absoluteScene, expectedClip)) {
                        if (attempt < MAX_TARGET_ATTEMPTS) {
                            scheduleTarget(
                                    generation,
                                    track,
                                    absoluteScene,
                                    expectedClip,
                                    onComplete,
                                    attempt + 1);
                        } else {
                            clearObservedState();
                            onComplete.accept(false);
                        }
                        return;
                    }
                    ready = true;
                    refreshObservedViews();
                    host.scheduleTask(
                            () -> {
                                if (targetGeneration == generation) {
                                    onComplete.accept(true);
                                }
                            },
                            TARGET_RETRY_MS);
                },
                TARGET_RETRY_MS);
    }

    private boolean clipTargetSettled(final int absoluteScene, final boolean expectedClip) {
        if (!expectedClip) {
            return !clip.exists().get() && !fineClip.exists().get();
        }
        return clip.exists().get()
                && fineClip.exists().get()
                && clip.clipLauncherSlot().sceneIndex().get() == absoluteScene
                && fineClip.clipLauncherSlot().sceneIndex().get() == absoluteScene;
    }

    private void refreshObservedViews() {
        clip.scrollToKey(Math.max(0, midiNote - 1));
        clip.scrollToStep(firstVisibleStep + 1);
        clip.scrollToKey(midiNote);
        clip.scrollToStep(firstVisibleStep);
        fineClip.scrollToKey(Math.max(0, midiNote - 1));
        fineClip.scrollToStep(1);
        fineClip.scrollToKey(midiNote);
        fineClip.scrollToStep(0);
    }

    void clear() {
        targetGeneration++;
        clearObservedState();
        clip.isPinned().set(false);
        fineClip.isPinned().set(false);
    }

    boolean isReady() {
        return ready;
    }

    boolean exists() {
        return ready && clip.exists().get();
    }

    boolean isOccupied(final int step) {
        return validStep(step) && !observedChannels[step].isEmpty();
    }

    Set<Integer> channelsAt(final int step) {
        return validStep(step) ? Set.copyOf(observedChannels[step]) : Set.of();
    }

    boolean isPlaying(final int step) {
        return ready && step == playingStep;
    }

    void setStep(final int channel, final int step, final int velocity, final double duration) {
        clip.setStep(channel, step, 0, velocity, duration);
    }

    void clearStep(final int channel, final int step) {
        clip.clearStep(channel, step, 0);
    }

    double loopLength() {
        return clip.getLoopLength().get();
    }

    void setLoopLength(final double beats) {
        clip.getLoopLength().set(beats);
    }

    double playStart() {
        return clip.getPlayStart().get();
    }

    void setPlayStart(final double beats) {
        clip.getPlayStart().set(beats);
    }

    void showInEditor() {
        clip.showInEditor();
    }

    int fineNudge(final int direction, final IntPredicate includeFineStep) {
        final int loopFineSteps =
                Math.min(
                        FINE_OBSERVATION_STEPS,
                        Math.max(
                                1,
                                (int) Math.round(loopLength() / MulticlipTiming.FINE_STEP_BEATS)));
        final List<FineTarget> targets = new ArrayList<>();
        for (final Map.Entry<Integer, Set<Integer>> entry : fineNotes.entrySet()) {
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
            fineClip.moveStep(
                    target.channel(), target.fineStep(), 0, destination - target.fineStep(), 0);
        }
        return targets.size();
    }

    static int coarseStepForFineStep(final int fineStep) {
        return fineStep / FINE_STEPS_PER_COARSE_STEP;
    }

    private void observeStep(final NoteStep note) {
        if (!ready
                || note.x() < 0
                || note.x() >= MulticlipXoxLayout.PATTERN_COUNT
                || note.y() != 0) {
            return;
        }
        if (note.state() == NoteStep.State.NoteOn) {
            observedChannels[note.x()].add(note.channel());
        } else {
            observedChannels[note.x()].remove(note.channel());
        }
    }

    private void observePlayingStep(final int absoluteStep) {
        if (!ready
                || absoluteStep < firstVisibleStep
                || absoluteStep >= firstVisibleStep + MulticlipXoxLayout.PATTERN_COUNT) {
            playingStep = -1;
        } else {
            playingStep = absoluteStep - firstVisibleStep;
        }
    }

    private void observeFineStep(final NoteStep note) {
        if (!ready || note.x() < 0 || note.x() >= FINE_OBSERVATION_STEPS || note.y() != 0) {
            return;
        }
        final Set<Integer> channels =
                fineNotes.computeIfAbsent(note.x(), ignored -> new HashSet<>());
        if (note.state() == NoteStep.State.NoteOn) {
            channels.add(note.channel());
        } else {
            channels.remove(note.channel());
            if (channels.isEmpty()) {
                fineNotes.remove(note.x());
            }
        }
    }

    private void clearObservedState() {
        ready = false;
        playingStep = -1;
        fineNotes.clear();
        for (final Set<Integer> channels : observedChannels) {
            channels.clear();
        }
    }

    private boolean validStep(final int step) {
        return step >= 0 && step < observedChannels.length;
    }

    private record FineTarget(int fineStep, int channel) {}
}
