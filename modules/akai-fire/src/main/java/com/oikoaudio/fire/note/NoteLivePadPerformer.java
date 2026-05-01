package com.oikoaudio.fire.note;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Live pad-note performance state for Note mode: held pads, sounding notes, retuning, and note on/off output.
 */
final class NoteLivePadPerformer {
    private final MidiOut midiOut;
    private final java.util.function.IntFunction<int[]> midiNotesResolver;
    private final VelocityResolver velocityResolver;
    private final Set<Integer> heldPads = new HashSet<>();
    private final Map<Integer, LivePadNote> soundingNotesByPad = new HashMap<>();

    NoteLivePadPerformer(final MidiOut midiOut,
                         final java.util.function.IntFunction<int[]> midiNotesResolver,
                         final BiFunction<Integer, Integer, Integer> velocityResolver) {
        this(midiOut, midiNotesResolver, (padIndex, configuredVelocity, rawVelocity) ->
                velocityResolver.apply(configuredVelocity, rawVelocity));
    }

    NoteLivePadPerformer(final MidiOut midiOut,
                         final java.util.function.IntFunction<int[]> midiNotesResolver,
                         final VelocityResolver velocityResolver) {
        this.midiOut = midiOut;
        this.midiNotesResolver = midiNotesResolver;
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

    boolean isMidiNoteSounding(final int midiNote) {
        if (midiNote < 0) {
            return false;
        }
        return soundingNotesByPad.values().stream()
                .anyMatch(activeNote -> activeNote.midiNotes().contains(midiNote));
    }

    private void noteOn(final int padIndex, final int rawVelocity, final int configuredVelocity) {
        noteOff(padIndex);
        final int[] midiNotes = midiNotesResolver.apply(padIndex);
        if (midiNotes == null || midiNotes.length == 0) {
            return;
        }
        stopOtherPadsSoundingAny(padIndex, midiNotes);
        final int appliedVelocity = velocityResolver.resolve(padIndex, configuredVelocity, rawVelocity);
        final List<Integer> sounding = new ArrayList<>();
        for (final int midiNote : midiNotes) {
            if (midiNote < 0) {
                continue;
            }
            midiOut.noteOn(midiNote, appliedVelocity);
            sounding.add(midiNote);
        }
        if (!sounding.isEmpty()) {
            soundingNotesByPad.put(padIndex, new LivePadNote(sounding, appliedVelocity));
        }
    }

    private void stopOtherPadsSoundingAny(final int padIndex, final int[] midiNotes) {
        final Set<Integer> targetNotes = new HashSet<>();
        for (final int midiNote : midiNotes) {
            if (midiNote >= 0) {
                targetNotes.add(midiNote);
            }
        }
        if (targetNotes.isEmpty()) {
            return;
        }
        final List<Integer> duplicatePads = soundingNotesByPad.entrySet().stream()
                .filter(entry -> entry.getKey() != padIndex)
                .filter(entry -> entry.getValue().midiNotes().stream().anyMatch(targetNotes::contains))
                .map(Map.Entry::getKey)
                .toList();
        duplicatePads.forEach(this::noteOff);
    }

    private void noteOff(final int padIndex) {
        final LivePadNote activeNote = soundingNotesByPad.remove(padIndex);
        if (activeNote == null) {
            return;
        }
        for (final int midiNote : activeNote.midiNotes()) {
            midiOut.noteOff(midiNote);
        }
    }

    @FunctionalInterface
    interface MidiOut {
        void noteOn(int midiNote, int velocity);

        default void noteOff(final int midiNote) {
        }
    }

    @FunctionalInterface
    interface VelocityResolver {
        int resolve(int padIndex, int configuredVelocity, int rawVelocity);
    }

    private record LivePadNote(List<Integer> midiNotes, int velocity) {
    }
}
