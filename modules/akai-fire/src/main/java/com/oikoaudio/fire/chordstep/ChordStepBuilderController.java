package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.music.SharedPitchContextController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final Set<Integer> heldSourcePads = new HashSet<>();

    private boolean inKey = false;
    private boolean latchEnabled = false;

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

    public void setInKey(final boolean inKey) {
        this.inKey = inKey;
    }

    public boolean isInKey() {
        return inKey;
    }

    public String layoutDisplayName() {
        return inKey ? "In Key" : "Chromatic";
    }

    public boolean isLatchEnabled() {
        return latchEnabled;
    }

    public boolean setLatchEnabled(final boolean latchEnabled) {
        if (this.latchEnabled == latchEnabled) {
            return false;
        }
        this.latchEnabled = latchEnabled;
        heldSourcePads.clear();
        return true;
    }

    public String latchDisplayName() {
        return latchEnabled ? "On" : "Off";
    }

    public boolean handleSourcePad(final int padIndex, final boolean pressed) {
        if (latchEnabled) {
            return pressed && toggleNoteOffset(padIndex);
        }
        if (!pressed) {
            heldSourcePads.remove(padIndex);
            return false;
        }
        final int midiNote = noteMidiForPad(padIndex);
        if (midiNote < 0) {
            return false;
        }
        heldSourcePads.add(padIndex);
        final List<Integer> heldNotes = new ArrayList<>();
        for (final int heldPad : heldSourcePads) {
            final int heldMidiNote = noteMidiForPad(heldPad);
            if (heldMidiNote >= 0) {
                heldNotes.add(heldMidiNote);
            }
        }
        selection.replaceBuilderNotesIfChanged(heldNotes);
        return true;
    }

    public boolean toggleNoteOffset(final int padIndex) {
        final int midiNote = noteMidiForPad(padIndex);
        if (midiNote >= 0) {
            selection.toggleBuilderNote(midiNote);
            return true;
        }
        return false;
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
