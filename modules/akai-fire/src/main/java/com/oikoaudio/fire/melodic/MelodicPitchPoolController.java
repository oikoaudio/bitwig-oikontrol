package com.oikoaudio.fire.melodic;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Owns Melodic Step pitch membership, generator provenance, user edits, and MIDI audition cleanup. */
public final class MelodicPitchPoolController {
    public enum PressResult { INVALID, ASSIGNED, ENABLED, DISABLED }

    public interface AuditionPort {
        boolean enabled();
        void noteOn(int pitch);
        void noteOff(int pitch);
    }

    private final AuditionPort auditionPort;
    private final LinkedHashSet<Integer> pitches = new LinkedHashSet<>();
    private final Set<Integer> auditioningPitches = new LinkedHashSet<>();
    private boolean userEdited;
    private Object generatorSource;

    public MelodicPitchPoolController(final AuditionPort auditionPort) {
        this.auditionPort = auditionPort;
    }

    public Set<Integer> pitches() { return Collections.unmodifiableSet(pitches); }
    public boolean contains(final int pitch) { return pitches.contains(pitch); }
    public boolean isEmpty() { return pitches.isEmpty(); }
    public int size() { return pitches.size(); }
    public boolean userEdited() { return userEdited; }
    public boolean generatedBy(final Object source) { return !userEdited && generatorSource == source; }

    public boolean toggleMembership(final int pitch) {
        final boolean enabled;
        if (pitches.remove(pitch)) {
            enabled = false;
        } else {
            pitches.add(pitch);
            enabled = true;
        }
        userEdited = true;
        return enabled;
    }

    public PressResult pressPitch(final int pitch,
                                  final Integer heldStep,
                                  final BiConsumer<Integer, Integer> assignment) {
        if (pitch < 0 || pitch > 127) {
            return PressResult.INVALID;
        }
        startAudition(pitch);
        if (heldStep != null) {
            assignment.accept(heldStep, pitch);
            return PressResult.ASSIGNED;
        }
        return toggleMembership(pitch) ? PressResult.ENABLED : PressResult.DISABLED;
    }

    public void replaceGenerated(final Collection<Integer> generated, final Object source) {
        replace(generated);
        userEdited = false;
        generatorSource = source;
    }

    public void replaceUserEdited(final Collection<Integer> edited) {
        replace(edited);
        userEdited = true;
    }

    public void replaceObserved(final Collection<Integer> observed) {
        replace(observed);
    }

    public void markUserEdited() { userEdited = true; }

    public void clear() { pitches.clear(); }
    public void add(final int pitch) { pitches.add(pitch); }
    public Integer firstOrNull() { return pitches.isEmpty() ? null : pitches.iterator().next(); }

    public void startAudition(final int pitch) {
        if (!auditionPort.enabled() || pitch < 0 || pitch > 127 || !auditioningPitches.add(pitch)) {
            return;
        }
        auditionPort.noteOn(pitch);
    }

    public void stopAudition(final int pitch) {
        if (!auditioningPitches.remove(pitch)) {
            return;
        }
        auditionPort.noteOff(pitch);
    }

    public void stopAllAuditions() {
        for (final int pitch : auditioningPitches) {
            auditionPort.noteOff(pitch);
        }
        auditioningPitches.clear();
    }

    public static List<Integer> layout(final MelodicPhraseContext context,
                                       final int rootPitch,
                                       final int pitchCount) {
        if (pitchCount <= 0) {
            return List.of();
        }
        final List<Integer> notes = new ArrayList<>(Math.max(0, pitchCount));
        int candidate = rootPitch - 1;
        while (notes.size() < 4 && candidate >= 0) {
            if (context.scale().isMidiNoteInScale(context.rootNote(), candidate)) {
                notes.add(0, candidate);
            }
            candidate--;
        }
        notes.add(Math.max(0, Math.min(127, rootPitch)));
        candidate = rootPitch + 1;
        while (notes.size() < pitchCount && candidate <= 127) {
            if (context.scale().isMidiNoteInScale(context.rootNote(), candidate)) {
                notes.add(candidate);
            }
            candidate++;
        }
        candidate = notes.get(0) - 1;
        while (notes.size() < pitchCount && candidate >= 0) {
            if (context.scale().isMidiNoteInScale(context.rootNote(), candidate)) {
                notes.add(0, candidate);
            }
            candidate--;
        }
        while (notes.size() < pitchCount) {
            notes.add(notes.get(notes.size() - 1));
        }
        return List.copyOf(notes);
    }

    private void replace(final Collection<Integer> replacement) {
        stopAllAuditions();
        pitches.clear();
        for (final int pitch : replacement) {
            if (pitch >= 0 && pitch <= 127) {
                pitches.add(pitch);
            }
        }
    }
}
