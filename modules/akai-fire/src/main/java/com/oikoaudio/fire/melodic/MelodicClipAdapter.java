package com.oikoaudio.fire.melodic;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class MelodicClipAdapter {
    public record PendingNoteWrite(int stepIndex, int pitch, double chance, int recurrenceLength, int recurrenceMask) {
    }

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
            final List<NoteStep> notes = notesAtStep.values().stream()
                    .filter(step -> step.state() == NoteStep.State.NoteOn)
                    .sorted(Comparator.comparingInt(NoteStep::y))
                    .limit(2)
                    .toList();
            if (notes.isEmpty()) {
                continue;
            }
            final NoteStep note = notes.get(0);
            final double duration = Math.max(stepLength * 0.25, note.duration());
            final int tiedSteps = (int) Math.floor((duration - stepLength * 0.98) / stepLength);
            MelodicPattern.Step step = new MelodicPattern.Step(i, true, false, note.y(), (int) Math.round(note.velocity() * 127.0),
                    Math.max(0.1, duration / stepLength), note.chance(), note.velocity() >= 0.87,
                    duration > stepLength * 1.05, note.recurrenceLength(), note.recurrenceMask(),
                    null, 96, 0.8, 1.0, false, false, 0, 0);
            if (notes.size() > 1) {
                final NoteStep alternate = notes.get(1);
                final double alternateDuration = Math.max(stepLength * 0.25, alternate.duration());
                step = step.withAlternate(alternate.y(), (int) Math.round(alternate.velocity() * 127.0),
                        Math.max(0.1, alternateDuration / stepLength), alternate.chance(),
                        alternate.velocity() >= 0.87, alternateDuration > stepLength * 1.05,
                        alternate.recurrenceLength(), alternate.recurrenceMask());
            }
            steps.set(i, step);
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
            if (step.hasAlternate()) {
                final double alternateDuration = stepLength * Math.max(step.alternateGate(), 0.15);
                clip.setStep(i, step.alternatePitch(), step.alternateVelocity(), alternateDuration);
            }
        }
    }

    public static List<PendingNoteWrite> pendingWrites(final MelodicPattern pattern) {
        final List<PendingNoteWrite> writes = new ArrayList<>();
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (!step.active() || step.tieFromPrevious() || step.pitch() == null) {
                continue;
            }
            writes.add(new PendingNoteWrite(i, step.pitch(), step.chance(),
                    step.bitwigRecurrenceLength(), step.bitwigRecurrenceMask()));
            if (step.hasAlternate()) {
                writes.add(new PendingNoteWrite(i, step.alternatePitch(), step.alternateChance(),
                        step.bitwigAlternateRecurrenceLength(), step.bitwigAlternateRecurrenceMask()));
            }
        }
        return writes;
    }
}
