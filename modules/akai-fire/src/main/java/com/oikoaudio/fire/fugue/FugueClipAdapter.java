package com.oikoaudio.fire.fugue;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.oikoaudio.fire.melodic.MelodicPattern;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class FugueClipAdapter {
    public static final int SOURCE_CHANNEL = 0;
    public static final int FIRST_DERIVED_CHANNEL = 1;
    public static final int LAST_DERIVED_CHANNEL = 3;

    private FugueClipAdapter() {
    }

    public static FuguePattern sourceFromChannelOne(final Map<Integer, Map<Integer, Map<Integer, NoteStep>>> noteSteps,
                                                      final int loopSteps,
                                                      final double stepLength) {
        return fromChannel(noteSteps, SOURCE_CHANNEL, loopSteps, stepLength);
    }

    public static FuguePattern fromChannel(final Map<Integer, Map<Integer, Map<Integer, NoteStep>>> noteSteps,
                                             final int channel,
                                             final int loopSteps,
                                             final double stepLength) {
        final List<List<MelodicPattern.Step>> steps = FuguePattern.emptyPolySteps();
        for (int i = 0; i < FuguePattern.MAX_STEPS; i++) {
            final Map<Integer, NoteStep> notesAtStep = noteSteps.getOrDefault(channel, Map.of()).get(i);
            if (notesAtStep == null || notesAtStep.isEmpty()) {
                continue;
            }
            final List<NoteStep> notes = notesAtStep.values().stream()
                    .filter(step -> step.state() == NoteStep.State.NoteOn)
                    .sorted(Comparator.comparingInt(NoteStep::y))
                    .toList();
            if (notes.isEmpty()) {
                continue;
            }
            int tiedSteps = 0;
            for (final NoteStep note : notes) {
                final double duration = Math.max(stepLength * 0.25, note.duration());
                tiedSteps = Math.max(tiedSteps, (int) Math.floor((duration - stepLength * 0.98) / stepLength));
                steps.get(i).add(new MelodicPattern.Step(i, true, false, note.y(),
                        (int) Math.round(note.velocity() * 127.0),
                        Math.max(0.1, duration / stepLength), note.chance(), note.velocity() >= 0.87,
                        duration > stepLength * 1.05, note.recurrenceLength(), note.recurrenceMask()));
            }
            for (int offset = 1; offset <= tiedSteps && i + offset < FuguePattern.MAX_STEPS; offset++) {
                if (steps.get(i + offset).isEmpty()) {
                    steps.get(i + offset).add(MelodicPattern.Step.rest(i + offset).withTieFromPrevious(true));
                }
            }
        }
        return new FuguePattern(steps, loopSteps, true);
    }

    public static void writeDerivedLine(final PinnableCursorClip clip,
                                        final Map<Integer, Map<Integer, Map<Integer, NoteStep>>> observedSteps,
                                        final int channel,
                                        final FuguePattern pattern,
                                        final double stepLength) {
        clearChannel(clip, observedSteps, channel);
        for (int i = 0; i < FuguePattern.MAX_STEPS; i++) {
            for (final MelodicPattern.Step step : pattern.notesAt(i)) {
                if (!step.active() || step.tieFromPrevious() || step.pitch() == null) {
                    continue;
                }
                final double duration = stepLength * Math.max(step.gate(), 0.02);
                clip.setStep(channel, i, step.pitch(), step.velocity(), duration);
                applyGeneratedNoteExpression(clip.getStep(channel, i, step.pitch()), step);
            }
        }
    }

    private static void applyGeneratedNoteExpression(final NoteStep noteStep, final MelodicPattern.Step step) {
        noteStep.setChance(Math.min(0.999, step.chance()));
        noteStep.setIsChanceEnabled(step.chance() < 0.999);
    }

    public static void duplicateChannelRange(final PinnableCursorClip clip,
                                             final Map<Integer, Map<Integer, Map<Integer, NoteStep>>> observedSteps,
                                             final int channel,
                                             final int sourceStartStep,
                                             final int sourceStepCount,
                                             final int targetStartStep) {
        final Map<Integer, Map<Integer, NoteStep>> channelSteps = observedSteps.getOrDefault(channel, Map.of());
        for (final Map.Entry<Integer, Map<Integer, NoteStep>> stepEntry : channelSteps.entrySet()) {
            final int sourceX = stepEntry.getKey();
            if (sourceX < sourceStartStep || sourceX >= sourceStartStep + sourceStepCount) {
                continue;
            }
            final int targetX = targetStartStep + sourceX - sourceStartStep;
            for (final NoteStep note : stepEntry.getValue().values()) {
                if (note.state() != NoteStep.State.NoteOn) {
                    continue;
                }
                clip.setStep(channel, targetX, note.y(), (int) Math.round(note.velocity() * 127.0),
                        note.duration());
            }
        }
    }

    public static void clearChannel(final PinnableCursorClip clip,
                                    final Map<Integer, Map<Integer, Map<Integer, NoteStep>>> observedSteps,
                                    final int channel) {
        for (int x = 0; x < FuguePattern.MAX_STEPS; x++) {
            clip.clearStepsAtX(channel, x);
        }
    }
}
