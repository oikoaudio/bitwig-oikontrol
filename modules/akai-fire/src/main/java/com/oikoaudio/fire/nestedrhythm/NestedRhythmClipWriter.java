package com.oikoaudio.fire.nestedrhythm;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.oikoaudio.fire.sequence.RecurrencePattern;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class NestedRhythmClipWriter {
    private final PinnableCursorClip cursorClip;
    private final double clipStepSize;
    private final Map<Integer, PendingPulseWrite> pendingPulseWrites = new HashMap<>();

    NestedRhythmClipWriter(final PinnableCursorClip cursorClip, final double clipStepSize) {
        this.cursorClip = cursorClip;
        this.clipStepSize = clipStepSize;
    }

    void write(final List<NestedRhythmEditablePulse> pulses,
               final NestedRhythmExpressionSettings settings,
               final double loopLengthBeats) {
        cursorClip.setStepSize(clipStepSize);
        pendingPulseWrites.clear();
        cursorClip.clearSteps();
        cursorClip.getLoopLength().set(loopLengthBeats);
        final List<NestedRhythmEditablePulse> active = pulses.stream()
                .filter(pulse -> pulse.enabled)
                .sorted(Comparator.comparingInt(pulse -> pulse.fineStart))
                .toList();
        for (final NestedRhythmEditablePulse pulse : active) {
            pendingPulseWrites.put(pulse.fineStart, PendingPulseWrite.fromPulse(pulse, settings));
            cursorClip.setStep(pulse.fineStart, pulse.midiNote,
                    pulse.effectiveVelocity(), pulse.effectiveBeatDuration(settings));
        }
    }

    void handleNoteStepObject(final NoteStep noteStep) {
        if (noteStep.state() == NoteStep.State.Empty) {
            return;
        }
        final PendingPulseWrite pending = pendingPulseWrites.get(noteStep.x());
        if (pending == null || pending.midiNote() != noteStep.y()) {
            return;
        }
        applyExpressionValues(noteStep, pending);
        pendingPulseWrites.remove(noteStep.x());
    }

    private void applyExpressionValues(final NoteStep step, final PendingPulseWrite pulse) {
        step.setPressure(pulse.pressure());
        step.setTimbre(pulse.timbre());
        step.setTranspose(pulse.pitchExpression());
        final double chance = Math.min(0.999, pulse.chance());
        step.setChance(chance);
        step.setIsChanceEnabled(chance < 0.999);
        final RecurrencePattern recurrence = pulse.recurrence();
        step.setRecurrence(recurrence.bitwigLength(), recurrence.bitwigMask());
    }

    private record PendingPulseWrite(int midiNote,
                                     double pressure,
                                     double timbre,
                                     double pitchExpression,
                                     double chance,
                                     RecurrencePattern recurrence) {
        private static PendingPulseWrite fromPulse(final NestedRhythmEditablePulse pulse,
                                                   final NestedRhythmExpressionSettings settings) {
            return new PendingPulseWrite(
                    pulse.midiNote,
                    pulse.effectivePressure(settings),
                    pulse.effectiveTimbre(settings),
                    pulse.effectivePitchExpression(settings),
                    pulse.effectiveChance(settings),
                    pulse.effectiveRecurrence());
        }
    }
}
