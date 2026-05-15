package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteStep;

import java.util.Collection;
import java.util.Map;

public final class ChordStepAccentEditor {
    public static final int STANDARD_VELOCITY = 100;
    public static final int ACCENTED_VELOCITY = 127;

    public boolean toggleAccent(final Collection<NoteStep> notes, final int standardVelocity) {
        final boolean targetAccent = !notes.stream().allMatch(note -> isAccented(note, standardVelocity));
        final double targetVelocity = (targetAccent ? ACCENTED_VELOCITY : standardVelocity) / 127.0;
        notes.forEach(note -> note.setVelocity(targetVelocity));
        return targetAccent;
    }

    public boolean isStepAccented(final Map<Integer, NoteStep> notesAtStep, final int standardVelocity) {
        return notesAtStep != null
                && !notesAtStep.isEmpty()
                && notesAtStep.values().stream()
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .allMatch(note -> isAccented(note, standardVelocity));
    }

    public boolean isAccented(final NoteStep noteStep, final int standardVelocity) {
        final int velocity = (int) Math.round(noteStep.velocity() * 127);
        final int distanceToAccent = Math.abs(velocity - ACCENTED_VELOCITY);
        final int distanceToStandard = Math.abs(velocity - standardVelocity);
        return distanceToAccent <= distanceToStandard;
    }
}
