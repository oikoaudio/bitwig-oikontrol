package com.oikoaudio.fire.note;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * Live pad-note performance state for Note mode: held pads, sounding notes, retuning, and note on/off output.
 */
final class NoteLivePadPerformer {
    private final MidiOut midiOut;
    private final IntUnaryOperator midiNoteResolver;
    private final IntBinaryOperator velocityResolver;
    private final Set<Integer> heldPads = new HashSet<>();
    private final Map<Integer, LivePadNote> soundingNotesByPad = new HashMap<>();

    NoteLivePadPerformer(final MidiOut midiOut,
                         final IntUnaryOperator midiNoteResolver,
                         final IntBinaryOperator velocityResolver) {
        this.midiOut = midiOut;
        this.midiNoteResolver = midiNoteResolver;
        this.velocityResolver = velocityResolver;
    }

    void handlePadPress(final int padIndex, final boolean pressed, final int rawVelocity, final int configuredVelocity) {
        if (pressed) {
            heldPads.add(padIndex);
            noteOn(padIndex, rawVelocity, configuredVelocity);
        } else {
            heldPads.remove(padIndex);
            noteOff(padIndex);
        }
    }

    void releaseHeldNotes() {
        for (final int padIndex : new ArrayList<>(soundingNotesByPad.keySet())) {
            noteOff(padIndex);
        }
        soundingNotesByPad.clear();
        heldPads.clear();
    }

    void retuneHeldPads(final Runnable stateChange, final int configuredVelocity) {
        final Map<Integer, LivePadNote> heldNotes = new HashMap<>(soundingNotesByPad);
        heldNotes.keySet().forEach(this::noteOff);
        stateChange.run();
        heldNotes.keySet().stream()
                .filter(heldPads::contains)
                .sorted()
                .forEach(padIndex -> noteOn(padIndex, heldNotes.get(padIndex).velocity(), configuredVelocity));
    }

    boolean isPadHeld(final int padIndex) {
        return heldPads.contains(padIndex);
    }

    private void noteOn(final int padIndex, final int rawVelocity, final int configuredVelocity) {
        noteOff(padIndex);
        final int midiNote = midiNoteResolver.applyAsInt(padIndex);
        if (midiNote < 0) {
            return;
        }
        final int appliedVelocity = velocityResolver.applyAsInt(configuredVelocity, rawVelocity);
        midiOut.noteOn(midiNote, appliedVelocity);
        soundingNotesByPad.put(padIndex, new LivePadNote(midiNote, appliedVelocity));
    }

    private void noteOff(final int padIndex) {
        final LivePadNote activeNote = soundingNotesByPad.remove(padIndex);
        if (activeNote == null) {
            return;
        }
        midiOut.noteOff(activeNote.midiNote());
    }

    @FunctionalInterface
    interface MidiOut {
        void noteOn(int midiNote, int velocity);

        default void noteOff(final int midiNote) {
        }
    }

    private record LivePadNote(int midiNote, int velocity) {
    }
}
