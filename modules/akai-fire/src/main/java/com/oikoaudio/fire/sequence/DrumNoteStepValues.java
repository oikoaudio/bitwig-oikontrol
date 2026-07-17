package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.NoteOccurrence;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;

record DrumNoteStepValues(
        int x,
        double velocity,
        double duration,
        double chance,
        boolean chanceEnabled,
        double timbre,
        double pressure,
        int repeatCount,
        double repeatVelocityCurve,
        double pan,
        double repeatVelocityEnd,
        int recurrenceLength,
        int recurrenceMask,
        NoteOccurrence occurrence) {
    static DrumNoteStepValues capture(final NoteStep source) {
        return new DrumNoteStepValues(
                source.x(),
                source.velocity(),
                source.duration(),
                source.chance(),
                source.isChanceEnabled(),
                source.timbre(),
                source.pressure(),
                source.repeatCount(),
                source.repeatVelocityCurve(),
                source.pan(),
                source.repeatVelocityEnd(),
                source.recurrenceLength(),
                source.recurrenceMask(),
                source.occurrence());
    }

    void insertInto(final PinnableCursorClip destination) {
        destination.setStep(x, 0, (int) Math.round(velocity * 127), duration);
    }

    void applyParametersTo(final NoteStep destination) {
        destination.setChance(chance);
        destination.setIsChanceEnabled(chanceEnabled);
        destination.setTimbre(timbre);
        destination.setPressure(pressure);
        destination.setRepeatCount(repeatCount);
        destination.setRepeatVelocityCurve(repeatVelocityCurve);
        destination.setPan(pan);
        destination.setRepeatVelocityEnd(repeatVelocityEnd);
        destination.setRecurrence(recurrenceLength, recurrenceMask);
        destination.setOccurrence(occurrence);
    }
}
