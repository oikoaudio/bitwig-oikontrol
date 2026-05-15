package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.music.SharedPitchContextController;

import java.util.function.IntSupplier;

/**
 * Owns chord-builder source-pad note mapping and builder-note selection gestures.
 */
public final class ChordStepBuilderController {
    public enum PadRole {
        ROOT,
        IN_SCALE,
        OUT_OF_SCALE,
        UNAVAILABLE
    }

    private final ChordStepChordSelection selection;
    private final SharedPitchContextController pitchContext;
    private final IntSupplier firstVisibleMidiNote;
    private final int sourcePadCount;

    private boolean inKey = true;

    public ChordStepBuilderController(final ChordStepChordSelection selection,
                                      final SharedPitchContextController pitchContext,
                                      final IntSupplier firstVisibleMidiNote,
                                      final int sourcePadCount) {
        this.selection = selection;
        this.pitchContext = pitchContext;
        this.firstVisibleMidiNote = firstVisibleMidiNote;
        this.sourcePadCount = sourcePadCount;
    }

    public void toggleLayout() {
        inKey = !inKey;
    }

    public boolean isInKey() {
        return inKey;
    }

    public String layoutDisplayName() {
        return inKey ? "In Key" : "Chromatic";
    }

    public void ensureSeededIfEmpty() {
        if (!selection.isBuilderFamily() || selection.hasBuilderNotes()) {
            return;
        }
        final int firstVisibleNote = firstVisibleMidiNote.getAsInt();
        if (firstVisibleNote < 0) {
            return;
        }
        final int builderRoot = Math.floorMod(firstVisibleNote, 12);
        for (int padIndex = 0; padIndex < sourcePadCount; padIndex++) {
            final int midiNote = noteMidiForPad(padIndex);
            if (midiNote >= 0 && pitchContext.isRootMidiNote(builderRoot, midiNote)) {
                selection.addBuilderNote(midiNote);
                return;
            }
        }
        final int firstPadNote = noteMidiForPad(0);
        if (firstPadNote >= 0) {
            selection.addBuilderNote(firstPadNote);
        }
    }

    public void toggleNoteOffset(final int padIndex) {
        final int midiNote = noteMidiForPad(padIndex);
        if (midiNote >= 0) {
            selection.toggleBuilderNote(midiNote);
        }
    }

    public boolean isNoteSelectedForPad(final int padIndex) {
        final int midiNote = noteMidiForPad(padIndex);
        return midiNote >= 0 && selection.isBuilderNoteSelected(midiNote);
    }

    public int noteMidiForPad(final int padIndex) {
        if (padIndex < 0 || padIndex >= sourcePadCount) {
            return -1;
        }
        final int firstVisibleNote = firstVisibleMidiNote.getAsInt();
        if (firstVisibleNote < 0) {
            return -1;
        }
        if (!inKey) {
            final int note = firstVisibleNote + padIndex;
            return note >= 0 && note <= 127 ? note : -1;
        }
        final int builderRoot = Math.floorMod(firstVisibleNote, 12);
        int note = firstVisibleNote;
        for (int i = 0; i < padIndex; i++) {
            note = pitchContext.nextScaleNote(note, builderRoot);
            if (note < 0) {
                return -1;
            }
        }
        return note;
    }

    public PadRole padRole(final int padIndex) {
        final int midiNote = noteMidiForPad(padIndex);
        if (midiNote < 0) {
            return PadRole.UNAVAILABLE;
        }
        final int firstVisibleNote = firstVisibleMidiNote.getAsInt();
        final int builderRoot = firstVisibleNote >= 0
                ? Math.floorMod(firstVisibleNote, 12)
                : Math.floorMod(pitchContext.getRootNote(), 12);
        if (pitchContext.isRootMidiNote(builderRoot, midiNote)) {
            return PadRole.ROOT;
        }
        if (pitchContext.isMidiNoteInScale(builderRoot, midiNote)) {
            return PadRole.IN_SCALE;
        }
        return PadRole.OUT_OF_SCALE;
    }
}
