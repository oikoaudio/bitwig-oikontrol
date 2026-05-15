package com.oikoaudio.fire.music;

import com.bitwig.extensions.framework.MusicalScale;
import com.bitwig.extensions.framework.MusicalScaleLibrary;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.SharedMusicalContext;

public final class SharedPitchContextController {
    private final SharedMusicalContext context;
    private final MusicalScaleLibrary scaleLibrary;

    public SharedPitchContextController(final SharedMusicalContext context, final MusicalScaleLibrary scaleLibrary) {
        this.context = context;
        this.scaleLibrary = scaleLibrary;
    }

    public void initializeFromPreferences(final String defaultScalePreference,
                                          final int defaultRootKey,
                                          final int defaultOctave) {
        setScaleIndex(resolveDefaultScaleIndex(defaultScalePreference));
        setRootNote(defaultRootKey);
        setOctave(defaultOctave);
    }

    public int getRootNote() {
        return context.getRootNote();
    }

    public void setRootNote(final int rootNote) {
        context.setRootNote(rootNote);
    }

    public void adjustRootNote(final int amount) {
        context.adjustRootNote(amount);
    }

    public int getScaleIndex() {
        return context.getScaleIndex();
    }

    public void setScaleIndex(final int scaleIndex) {
        context.setScaleIndex(scaleIndex);
    }

    public boolean adjustScaleIndex(final int amount, final int minimumScaleIndex) {
        return context.adjustScaleIndex(amount, minimumScaleIndex);
    }

    public int getOctave() {
        return context.getOctave();
    }

    public void setOctave(final int octave) {
        context.setOctave(octave);
    }

    public void adjustOctave(final int amount) {
        context.adjustOctave(amount);
    }

    public int getBaseMidiNote() {
        return context.getBaseMidiNote();
    }

    public String getScaleDisplayName() {
        return context.getScaleDisplayName();
    }

    public int getScaleCount() {
        return scaleLibrary != null ? scaleLibrary.getMusicalScalesCount() : 0;
    }

    public String getShortScaleDisplayName() {
        return switch (getScaleDisplayName()) {
            case "Major" -> "Major";
            case "Minor" -> "Minor";
            case "Phrygian Dominant" -> "Phryg Dom";
            case "Double Harmonic Major" -> "DH Maj";
            case "Double Harmonic Minor" -> "DH Min";
            case "Harmonic Major" -> "Harm Maj";
            case "Harmonic Minor" -> "Harm Min";
            case "Jazz Minor" -> "Jazz Min";
            case "Overtone Scale" -> "Overtone";
            case "Hungarian Minor" -> "Hung Min";
            case "Ukranian Dorian" -> "Ukr Dor";
            case "Super Locrian" -> "Sup Loc";
            case "Half-diminished" -> "Half Dim";
            case "Diminished WH" -> "Dim WH";
            case "Diminished HW" -> "Dim HW";
            case "Major Pentatonic" -> "Maj Pent";
            case "Minor Pentatonic" -> "Min Pent";
            case "Blues Major" -> "Bl Maj";
            case "Blues Minor" -> "Bl Min";
            case "Whole Tone" -> "Whole";
            case "Major Triad" -> "Maj Tri";
            case "Minor Triad" -> "Min Tri";
            case "Bebop Major" -> "Bebop Maj";
            case "Bebop Dorian" -> "Bebop Dor";
            case "Bebop Mixolydian" -> "Bebop Mix";
            case "Bebop Minor" -> "Bebop Min";
            default -> getScaleDisplayName();
        };
    }

    public MusicalScale getMusicalScale() {
        if (scaleLibrary == null || scaleLibrary.getMusicalScalesCount() <= 0) {
            return null;
        }
        final MusicalScale preferred = scaleAt(getScaleIndex());
        if (preferred != null) {
            return preferred;
        }
        for (int i = 0; i < scaleLibrary.getMusicalScalesCount(); i++) {
            final MusicalScale candidate = scaleAt(i);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    public boolean isRootMidiNote(final int rootNote, final int midiNote) {
        final MusicalScale scale = getMusicalScale();
        return scale != null && scale.isRootMidiNote(rootNote, midiNote);
    }

    public boolean isMidiNoteInScale(final int rootNote, final int midiNote) {
        final MusicalScale scale = getMusicalScale();
        return scale != null && scale.isMidiNoteInScale(rootNote, midiNote);
    }

    public int nextScaleNote(final int currentNote, final int rootNote) {
        int note = currentNote + 1;
        while (note <= 127) {
            if (isMidiNoteInScale(rootNote, note)) {
                return note;
            }
            note++;
        }
        return -1;
    }

    public int transposeByScaleDegrees(final int midiNote, final int scaleDegrees) {
        if (scaleDegrees == 0) {
            return midiNote;
        }
        int note = midiNote;
        int remaining = Math.abs(scaleDegrees);
        final int direction = scaleDegrees > 0 ? 1 : -1;
        while (remaining > 0) {
            note += direction;
            while (note >= 0 && note <= 127 && !isMidiNoteInScale(getRootNote(), note)) {
                note += direction;
            }
            if (note < 0 || note > 127) {
                return -1;
            }
            remaining--;
        }
        return note;
    }

    public SharedMusicalContext context() {
        return context;
    }

    int resolveDefaultScaleIndex(final String defaultScalePreference) {
        return switch (defaultScalePreference) {
            case FireControlPreferences.DEFAULT_SCALE_MAJOR -> findScaleIndex("Major", 1);
            case FireControlPreferences.DEFAULT_SCALE_MINOR -> findScaleIndex("Minor", 2);
            case FireControlPreferences.DEFAULT_SCALE_HARMONIC_MINOR -> findScaleIndex("Harmonic Minor", 2);
            case FireControlPreferences.DEFAULT_SCALE_MELODIC_MINOR -> findScaleIndex("Jazz Minor", 2);
            case FireControlPreferences.DEFAULT_SCALE_MINOR_PENTATONIC -> findScaleIndex("Minor Pentatonic", 2);
            case FireControlPreferences.DEFAULT_SCALE_DORIAN -> findScaleIndex("Dorian", 2);
            case FireControlPreferences.DEFAULT_SCALE_MIXOLYDIAN -> findScaleIndex("Mixolydian", 1);
            default -> findScaleIndex("Major", 1);
        };
    }

    int findScaleIndex(final String scaleName, final int fallbackIndex) {
        if (scaleLibrary == null || scaleLibrary.getMusicalScalesCount() <= 0) {
            return fallbackIndex;
        }
        for (int i = 0; i < scaleLibrary.getMusicalScalesCount(); i++) {
            final MusicalScale scale = scaleAt(i);
            if (scale != null && scale.getName().equals(scaleName)) {
                return i;
            }
        }
        return fallbackIndex;
    }

    private MusicalScale scaleAt(final int index) {
        if (scaleLibrary == null || index < 0 || index >= scaleLibrary.getMusicalScalesCount()) {
            return null;
        }
        return scaleLibrary.getMusicalScale(index);
    }
}
