package com.oikoaudio.fire.note;

import com.bitwig.extension.controller.api.NoteOccurrence;
import com.bitwig.extension.controller.api.NoteStep;

final class NoteStepMoveSnapshot {
    final int x;
    final int y;
    final int velocity;
    final double duration;
    final double chance;
    final double pressure;
    final double timbre;
    final double velocitySpread;
    final int repeatCount;
    final double repeatCurve;
    final double repeatVelocityCurve;
    final double repeatVelocityEnd;
    final double pan;
    final int recurrenceLength;
    final int recurrenceMask;
    final NoteOccurrence occurrence;

    private NoteStepMoveSnapshot(final NoteStep step) {
        this.x = step.x();
        this.y = step.y();
        this.velocity = (int) Math.round(step.velocity() * 127);
        this.duration = step.duration();
        this.chance = step.chance();
        this.pressure = step.pressure();
        this.timbre = step.timbre();
        this.velocitySpread = step.velocitySpread();
        this.repeatCount = step.repeatCount();
        this.repeatCurve = step.repeatCurve();
        this.repeatVelocityCurve = step.repeatVelocityCurve();
        this.repeatVelocityEnd = step.repeatVelocityEnd();
        this.pan = step.pan();
        this.recurrenceLength = step.recurrenceLength();
        this.recurrenceMask = step.recurrenceMask();
        this.occurrence = step.occurrence();
    }

    static NoteStepMoveSnapshot capture(final NoteStep step) {
        return new NoteStepMoveSnapshot(step);
    }
}
