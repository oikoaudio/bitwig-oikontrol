package com.oikoaudio.fire;

import com.bitwig.extensions.framework.MusicalScaleLibrary;

/**
 * Global musical context shared across controller modes.
 * Overview is the canonical full editor; other modes can expose focused shortcuts against the same state.
 */
public final class SharedMusicalContext {
    private final MusicalScaleLibrary scaleLibrary;
    private int rootNote = 0;
    private int scaleIndex = -1;
    private int octave = 3;

    public SharedMusicalContext(final MusicalScaleLibrary scaleLibrary) {
        this.scaleLibrary = scaleLibrary;
    }

    public int getRootNote() {
        return rootNote;
    }

    public void setRootNote(final int rootNote) {
        this.rootNote = Math.floorMod(rootNote, 12);
    }

    public void adjustRootNote(final int amount) {
        if (amount == 0) {
            return;
        }
        setRootNote(rootNote + amount);
    }

    public int getScaleIndex() {
        return scaleIndex;
    }

    public void setScaleIndex(final int scaleIndex) {
        if (scaleIndex < 0) {
            this.scaleIndex = 1;
            return;
        }
        if (scaleIndex < scaleLibrary.getMusicalScalesCount()
                && "Chromatic".equals(scaleLibrary.getMusicalScale(scaleIndex).getName())) {
            this.scaleIndex = 1;
            return;
        }
        this.scaleIndex = scaleIndex;
    }

    public boolean adjustScaleIndex(final int amount, final int minimumScaleIndex) {
        if (amount == 0) {
            return false;
        }
        int nextScaleIndex = scaleIndex + amount;
        while (nextScaleIndex >= minimumScaleIndex && nextScaleIndex < scaleLibrary.getMusicalScalesCount()) {
            if (!"Chromatic".equals(scaleLibrary.getMusicalScale(nextScaleIndex).getName())) {
                setScaleIndex(nextScaleIndex);
                return true;
            }
            nextScaleIndex += amount;
        }
        return false;
    }

    public int getOctave() {
        return octave;
    }

    public void setOctave(final int octave) {
        this.octave = Math.max(0, Math.min(7, octave));
    }

    public void adjustOctave(final int amount) {
        if (amount == 0) {
            return;
        }
        setOctave(octave + amount);
    }

    public int getBaseMidiNote() {
        return octave * 12 + rootNote;
    }

    public String getScaleDisplayName() {
        final int safeIndex = Math.max(0, Math.min(scaleLibrary.getMusicalScalesCount() - 1, scaleIndex));
        return scaleLibrary.getMusicalScale(safeIndex).getName();
    }
}
