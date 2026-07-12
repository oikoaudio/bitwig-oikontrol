package com.oikoaudio.fire.fugue;

import com.bitwig.extension.controller.api.NoteStep;

import java.util.HashMap;
import java.util.Map;

/** Owns Fugue's host-observed note cache and playback position. */
public final class FugueObservationController {
    private final int stepCount;
    private final Map<Integer, Map<Integer, Map<Integer, NoteStep>>> steps = new HashMap<>();
    private boolean active;
    private int playingStep = -1;

    public FugueObservationController(final int stepCount) {
        this.stepCount = stepCount;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public Map<Integer, Map<Integer, Map<Integer, NoteStep>>> steps() {
        return steps;
    }

    public int playingStep() {
        return playingStep;
    }

    public void updatePlayingStep(final int step) {
        playingStep = step >= 0 && step < stepCount ? step : -1;
    }

    public void observe(final NoteStep note) {
        if (!active) {
            return;
        }
        if (note.state() == NoteStep.State.Empty) {
            remove(note.channel(), note.x(), note.y());
            return;
        }
        steps.computeIfAbsent(note.channel(), ignored -> new HashMap<>())
                .computeIfAbsent(note.x(), ignored -> new HashMap<>())
                .put(note.y(), note);
    }

    public void refreshSource(final StepLookup lookup) {
        steps.clear();
        for (int x = 0; x < stepCount; x++) {
            for (int y = 0; y < 128; y++) {
                final NoteStep note = lookup.get(x, y);
                if (note.state() == NoteStep.State.NoteOn) {
                    steps.computeIfAbsent(FugueClipAdapter.SOURCE_CHANNEL, ignored -> new HashMap<>())
                            .computeIfAbsent(x, ignored -> new HashMap<>())
                            .put(y, note);
                }
            }
        }
    }

    public void cacheSource(final NoteStep note) {
        if (note.state() == NoteStep.State.NoteOn) {
            steps.computeIfAbsent(FugueClipAdapter.SOURCE_CHANNEL, ignored -> new HashMap<>())
                    .computeIfAbsent(note.x(), ignored -> new HashMap<>())
                    .put(note.y(), note);
        }
    }

    public void removeSource(final int step, final int pitch) {
        remove(FugueClipAdapter.SOURCE_CHANNEL, step, pitch);
    }

    private void remove(final int channel, final int step, final int pitch) {
        final Map<Integer, Map<Integer, NoteStep>> channelSteps = steps.get(channel);
        if (channelSteps == null) {
            return;
        }
        final Map<Integer, NoteStep> notesAtStep = channelSteps.get(step);
        if (notesAtStep != null) {
            notesAtStep.remove(pitch);
            if (notesAtStep.isEmpty()) {
                channelSteps.remove(step);
            }
        }
        if (channelSteps.isEmpty()) {
            steps.remove(channel);
        }
    }

    @FunctionalInterface
    public interface StepLookup {
        NoteStep get(int step, int pitch);
    }
}
