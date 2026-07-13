package com.oikoaudio.fire.note;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Live pad-note performance state for Note mode: held pads, sounding notes, retuning, and note
 * on/off output.
 */
final class NoteLivePadPerformer {
    private final MidiOut midiOut;
    private final java.util.function.IntFunction<int[]> midiNotesResolver;
    private final VelocityResolver velocityResolver;
    private final ExpressionResolver timbreResolver;
    private final Consumer<List<Integer>> soundingNotesListener;
    private final Set<Integer> physicalHeldPads = new HashSet<>();
    private final Set<Integer> holdCapturedPads = new HashSet<>();
    private final Map<Integer, LivePadNote> soundingNotesByPad = new HashMap<>();
    private boolean holdModeActive;

    NoteLivePadPerformer(
            final MidiOut midiOut,
            final java.util.function.IntFunction<int[]> midiNotesResolver,
            final BiFunction<Integer, Integer, Integer> velocityResolver) {
        this(
                midiOut,
                midiNotesResolver,
                (padIndex, configuredVelocity, rawVelocity) ->
                        velocityResolver.apply(configuredVelocity, rawVelocity));
    }

    NoteLivePadPerformer(
            final MidiOut midiOut,
            final java.util.function.IntFunction<int[]> midiNotesResolver,
            final VelocityResolver velocityResolver) {
        this(midiOut, midiNotesResolver, velocityResolver, padIndex -> -1);
    }

    NoteLivePadPerformer(
            final MidiOut midiOut,
            final java.util.function.IntFunction<int[]> midiNotesResolver,
            final VelocityResolver velocityResolver,
            final ExpressionResolver timbreResolver) {
        this(midiOut, midiNotesResolver, velocityResolver, timbreResolver, notes -> {});
    }

    NoteLivePadPerformer(
            final MidiOut midiOut,
            final java.util.function.IntFunction<int[]> midiNotesResolver,
            final VelocityResolver velocityResolver,
            final ExpressionResolver timbreResolver,
            final Consumer<List<Integer>> soundingNotesListener) {
        this.midiOut = midiOut;
        this.midiNotesResolver = midiNotesResolver;
        this.velocityResolver = velocityResolver;
        this.timbreResolver = timbreResolver;
        this.soundingNotesListener =
                soundingNotesListener == null ? notes -> {} : soundingNotesListener;
    }

    void handlePadPress(
            final int padIndex,
            final boolean pressed,
            final int rawVelocity,
            final int configuredVelocity) {
        if (holdModeActive) {
            handleHoldPadPress(padIndex, pressed, rawVelocity, configuredVelocity);
            return;
        }
        if (pressed) {
            physicalHeldPads.add(padIndex);
            noteOn(padIndex, rawVelocity, configuredVelocity);
        } else {
            physicalHeldPads.remove(padIndex);
            noteOff(padIndex);
        }
        notifySoundingNotesChanged();
    }

    boolean toggleHoldMode() {
        holdModeActive = !holdModeActive;
        if (holdModeActive) {
            holdCapturedPads.clear();
            soundingNotesByPad.keySet().stream()
                    .filter(physicalHeldPads::contains)
                    .forEach(holdCapturedPads::add);
        } else {
            releaseHoldCapturedNotes();
        }
        return holdModeActive;
    }

    boolean isHoldModeActive() {
        return holdModeActive;
    }

    void releaseHeldNotes() {
        holdModeActive = false;
        for (final int padIndex : new ArrayList<>(soundingNotesByPad.keySet())) {
            noteOff(padIndex);
        }
        soundingNotesByPad.clear();
        physicalHeldPads.clear();
        holdCapturedPads.clear();
        notifySoundingNotesChanged();
    }

    void retuneHeldPads(final Runnable stateChange, final int configuredVelocity) {
        final Map<Integer, LivePadNote> heldNotes = new HashMap<>(soundingNotesByPad);
        heldNotes.keySet().forEach(this::noteOff);
        stateChange.run();
        heldNotes.keySet().stream()
                .filter(this::shouldRestartAfterRetune)
                .sorted()
                .forEach(
                        padIndex ->
                                noteOn(
                                        padIndex,
                                        heldNotes.get(padIndex).velocity(),
                                        configuredVelocity));
        notifySoundingNotesChanged();
    }

    boolean isPadHeld(final int padIndex) {
        return physicalHeldPads.contains(padIndex) || holdCapturedPads.contains(padIndex);
    }

    boolean isPadHeldByHoldMode(final int padIndex) {
        return holdCapturedPads.contains(padIndex) && soundingNotesByPad.containsKey(padIndex);
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
        final int appliedVelocity =
                velocityResolver.resolve(padIndex, configuredVelocity, rawVelocity);
        final int timbre = timbreResolver.resolve(padIndex);
        final List<Integer> sounding = new ArrayList<>();
        for (final int midiNote : midiNotes) {
            if (midiNote < 0 || sounding.contains(midiNote)) {
                continue;
            }
            if (!isMidiNoteSounding(midiNote)) {
                midiOut.noteOn(midiNote, appliedVelocity);
                if (timbre >= 0) {
                    midiOut.timbre(midiNote, timbre);
                }
            }
            sounding.add(midiNote);
        }
        if (!sounding.isEmpty()) {
            soundingNotesByPad.put(padIndex, new LivePadNote(sounding, appliedVelocity));
        }
    }

    private void handleHoldPadPress(
            final int padIndex,
            final boolean pressed,
            final int rawVelocity,
            final int configuredVelocity) {
        if (!pressed) {
            physicalHeldPads.remove(padIndex);
            if (!holdCapturedPads.contains(padIndex)) {
                noteOff(padIndex);
            }
            notifySoundingNotesChanged();
            return;
        }
        if (holdCapturedPads.contains(padIndex) && soundingNotesByPad.containsKey(padIndex)) {
            holdCapturedPads.remove(padIndex);
            physicalHeldPads.remove(padIndex);
            noteOff(padIndex);
        } else {
            physicalHeldPads.add(padIndex);
            noteOn(padIndex, rawVelocity, configuredVelocity);
        }
        notifySoundingNotesChanged();
    }

    private void releaseHoldCapturedNotes() {
        for (final int padIndex : new ArrayList<>(holdCapturedPads)) {
            if (!physicalHeldPads.contains(padIndex)) {
                noteOff(padIndex);
            }
        }
        holdCapturedPads.clear();
        notifySoundingNotesChanged();
    }

    private boolean shouldRestartAfterRetune(final int padIndex) {
        return physicalHeldPads.contains(padIndex) || holdCapturedPads.contains(padIndex);
    }

    private void noteOff(final int padIndex) {
        final LivePadNote activeNote = soundingNotesByPad.remove(padIndex);
        if (activeNote == null) {
            return;
        }
        for (final int midiNote : activeNote.midiNotes()) {
            if (!isMidiNoteSounding(midiNote)) {
                midiOut.noteOff(midiNote);
            }
        }
    }

    private void notifySoundingNotesChanged() {
        final List<Integer> notes =
                soundingNotesByPad.values().stream()
                        .flatMap(activeNote -> activeNote.midiNotes().stream())
                        .distinct()
                        .sorted()
                        .toList();
        soundingNotesListener.accept(notes);
    }

    @FunctionalInterface
    interface MidiOut {
        void noteOn(int midiNote, int velocity);

        default void noteOff(final int midiNote) {}

        default void timbre(final int midiNote, final int value) {}
    }

    @FunctionalInterface
    interface VelocityResolver {
        int resolve(int padIndex, int configuredVelocity, int rawVelocity);
    }

    @FunctionalInterface
    interface ExpressionResolver {
        int resolve(int padIndex);
    }

    private record LivePadNote(List<Integer> midiNotes, int velocity) {}
}
