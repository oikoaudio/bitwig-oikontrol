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

    public MusicalScale getMusicalScale() {
        return scaleLibrary.getMusicalScale(getScaleIndex());
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
        for (int i = 0; i < scaleLibrary.getMusicalScalesCount(); i++) {
            if (scaleLibrary.getMusicalScale(i).getName().equals(scaleName)) {
                return i;
            }
        }
        return fallbackIndex;
    }
}
