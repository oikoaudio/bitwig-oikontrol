package com.oikoaudio.fire.melodic;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class MelodicClipAdapter {
    private MelodicClipAdapter() {
    }

    public static MelodicPattern fromNoteSteps(final Map<Integer, Map<Integer, NoteStep>> noteStepsByPosition,
                                               final int loopSteps, final double stepLength) {
        final List<MelodicPattern.Step> steps = new ArrayList<>(
                Collections.nCopies(MelodicPattern.MAX_STEPS, MelodicPattern.Step.rest(0)));
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            steps.set(i, MelodicPattern.Step.rest(i));
        }
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            final Map<Integer, NoteStep> notesAtStep = noteStepsByPosition.get(i);
            if (notesAtStep == null || notesAtStep.isEmpty()) {
                continue;
            }
            final NoteStep note = notesAtStep.values().stream()
                    .filter(step -> step.state() == NoteStep.State.NoteOn)
                    .min(Comparator.comparingInt(NoteStep::y))
                    .orElse(null);
            if (note == null) {
                continue;
            }
            final double duration = Math.max(stepLength * 0.25, note.duration());
            final int tiedSteps = (int) Math.floor((duration - stepLength * 0.98) / stepLength);
            steps.set(i, new MelodicPattern.Step(i, true, false, note.y(), (int) Math.round(note.velocity() * 127.0),
                    Math.max(0.1, duration / stepLength), note.velocity() >= 0.87,
                    duration > stepLength * 1.05, note.recurrenceLength(), note.recurrenceMask()));
            for (int offset = 1; offset <= tiedSteps && i + offset < MelodicPattern.MAX_STEPS; offset++) {
                steps.set(i + offset, MelodicPattern.Step.rest(i + offset).withTieFromPrevious(true));
            }
        }
        return new MelodicPattern(steps, loopSteps);
    }

    public static void writeToClip(final PinnableCursorClip clip, final MelodicPattern pattern, final double stepLength) {
        clip.clearSteps();
        clip.getLoopLength().set(pattern.loopSteps() * stepLength);
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (!step.active() || step.tieFromPrevious() || step.pitch() == null) {
                continue;
            }
            int tieCount = 0;
            int index = i + 1;
            while (index < MelodicPattern.MAX_STEPS && pattern.step(index).tieFromPrevious()) {
                tieCount++;
                index++;
            }
            final double duration = stepLength * Math.max(step.gate(), 0.15 + tieCount);
            clip.setStep(i, step.pitch(), step.velocity(), duration);
        }
    }
}
