package com.oikoaudio.fire;

import com.bitwig.extensions.framework.MusicalScale;
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
        if (scaleLibrary == null || scaleLibrary.getMusicalScalesCount() <= 0) {
            this.scaleIndex = 0;
            return;
        }
        final int firstSelectable = firstSelectableScaleIndex();
        if (firstSelectable < 0) {
            this.scaleIndex = 0;
            return;
        }
        final int lastSelectable = lastSelectableScaleIndex();
        final int clamped = Math.max(firstSelectable, Math.min(lastSelectable, scaleIndex));
        if (!isSelectableScale(clamped)) {
            this.scaleIndex = findNearestSelectableScale(clamped, scaleIndex >= this.scaleIndex ? 1 : -1);
            return;
        }
        this.scaleIndex = clamped;
    }

    public boolean adjustScaleIndex(final int amount, final int minimumScaleIndex) {
        if (amount == 0) {
            return false;
        }
        if (scaleLibrary == null || scaleLibrary.getMusicalScalesCount() <= 0) {
            return false;
        }
        final int firstSelectable = firstSelectableScaleIndex();
        if (firstSelectable < 0) {
            return false;
        }
        final int lastSelectable = lastSelectableScaleIndex();
        int nextScaleIndex = scaleIndex + amount;
        while (nextScaleIndex >= minimumScaleIndex && nextScaleIndex < scaleLibrary.getMusicalScalesCount()) {
            if (isSelectableScale(nextScaleIndex)) {
                setScaleIndex(nextScaleIndex);
                return true;
            }
            nextScaleIndex += amount;
        }
        final int clamped = amount > 0 ? lastSelectable : Math.max(firstSelectable, minimumScaleIndex);
        if (clamped != scaleIndex && isSelectableScale(clamped)) {
            setScaleIndex(clamped);
            return true;
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
        if (scaleLibrary == null || scaleLibrary.getMusicalScalesCount() <= 0) {
            return "Scale";
        }
        final int safeIndex = Math.max(0, Math.min(scaleLibrary.getMusicalScalesCount() - 1, scaleIndex));
        final MusicalScale scale = scaleAt(safeIndex);
        return scale != null ? scale.getName() : "Scale";
    }

    private boolean isChromaticScale(final int index) {
        final MusicalScale scale = scaleAt(index);
        return scale != null && "Chromatic".equals(scale.getName());
    }

    private boolean isSelectableScale(final int index) {
        final MusicalScale scale = scaleAt(index);
        return scale != null && !"Chromatic".equals(scale.getName());
    }

    private int firstSelectableScaleIndex() {
        for (int i = 0; i < scaleLibrary.getMusicalScalesCount(); i++) {
            if (isSelectableScale(i)) {
                return i;
            }
        }
        return -1;
    }

    private int lastSelectableScaleIndex() {
        for (int i = scaleLibrary.getMusicalScalesCount() - 1; i >= 0; i--) {
            if (isSelectableScale(i)) {
                return i;
            }
        }
        return -1;
    }

    private int findNearestSelectableScale(final int startIndex, final int direction) {
        final int normalizedDirection = direction >= 0 ? 1 : -1;
        for (int i = startIndex; i >= 0 && i < scaleLibrary.getMusicalScalesCount(); i += normalizedDirection) {
            if (isSelectableScale(i)) {
                return i;
            }
        }
        return normalizedDirection >= 0 ? lastSelectableScaleIndex() : firstSelectableScaleIndex();
    }

    private MusicalScale scaleAt(final int index) {
        if (scaleLibrary == null || index < 0 || index >= scaleLibrary.getMusicalScalesCount()) {
            return null;
        }
        return scaleLibrary.getMusicalScale(index);
    }
}
