package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.sequence.NoteStepAccess;
import java.util.EnumSet;
import java.util.Set;

/** Session-local per-note values applied when Poly Step inserts a new note or chord. */
final class ChordStepInsertionDefaults {
    static final Set<NoteStepAccess> SUPPORTED_ACCESSORS =
            EnumSet.of(
                    NoteStepAccess.VELOCITY,
                    NoteStepAccess.PRESSURE,
                    NoteStepAccess.TIMBRE,
                    NoteStepAccess.PITCH,
                    NoteStepAccess.DURATION,
                    NoteStepAccess.CHANCE,
                    NoteStepAccess.VELOCITY_SPREAD,
                    NoteStepAccess.REPEATS);

    private final int originalVelocity;
    private final double originalDuration;
    private int velocity;
    private double pressure;
    private double timbre;
    private double pitch;
    private double duration;
    private double chance;
    private double velocitySpread;
    private int repeats;

    ChordStepInsertionDefaults(final int velocity, final double duration) {
        this.originalVelocity = clampInt(velocity, 1, 127);
        this.originalDuration = duration;
        resetAll();
    }

    boolean adjust(final NoteStepAccess access, final int amount) {
        if (amount == 0 || !SUPPORTED_ACCESSORS.contains(access)) {
            return false;
        }
        return switch (access) {
            case VELOCITY -> setVelocity(velocity + amount);
            case PRESSURE -> setPressure(pressure + amount * 0.01);
            case TIMBRE -> setTimbre(timbre + amount * 0.01);
            case PITCH -> setPitch(pitch + amount);
            case DURATION -> setDuration(duration + originalDuration * amount * 0.01);
            case CHANCE -> setChance(chance + amount * 0.05);
            case VELOCITY_SPREAD -> setVelocitySpread(velocitySpread + amount * 0.01);
            case REPEATS -> setRepeats(repeats + amount);
            default -> false;
        };
    }

    void reset(final NoteStepAccess access) {
        switch (access) {
            case VELOCITY -> velocity = originalVelocity;
            case PRESSURE -> pressure = 0.0;
            case TIMBRE -> timbre = 0.0;
            case PITCH -> pitch = 0.0;
            case DURATION -> duration = originalDuration;
            case CHANCE -> chance = 1.0;
            case VELOCITY_SPREAD -> velocitySpread = 0.0;
            case REPEATS -> repeats = 0;
            default -> {}
        }
    }

    Values snapshot() {
        return new Values(pressure, timbre, pitch, chance, velocitySpread, repeats);
    }

    int velocity() {
        return velocity;
    }

    double pressure() {
        return pressure;
    }

    double timbre() {
        return timbre;
    }

    double pitch() {
        return pitch;
    }

    double duration() {
        return duration;
    }

    double chance() {
        return chance;
    }

    double velocitySpread() {
        return velocitySpread;
    }

    int repeats() {
        return repeats;
    }

    private void resetAll() {
        for (final NoteStepAccess access : SUPPORTED_ACCESSORS) {
            reset(access);
        }
    }

    private boolean setVelocity(final int value) {
        final int next = clampInt(value, 1, 127);
        final boolean changed = next != velocity;
        velocity = next;
        return changed;
    }

    private boolean setPressure(final double value) {
        return updateDouble(value, 0.0, 1.0, pressure, next -> pressure = next);
    }

    private boolean setTimbre(final double value) {
        return updateDouble(value, -1.0, 1.0, timbre, next -> timbre = next);
    }

    private boolean setPitch(final double value) {
        return updateDouble(value, -96.0, 96.0, pitch, next -> pitch = next);
    }

    private boolean setDuration(final double value) {
        return updateDouble(
                value,
                originalDuration * 0.1,
                originalDuration * 16.0,
                duration,
                next -> duration = next);
    }

    private boolean setChance(final double value) {
        return updateDouble(value, 0.0, 1.0, chance, next -> chance = next);
    }

    private boolean setVelocitySpread(final double value) {
        return updateDouble(value, 0.0, 1.0, velocitySpread, next -> velocitySpread = next);
    }

    private boolean setRepeats(final int value) {
        final int next = clampInt(value, 0, 16);
        final boolean changed = next != repeats;
        repeats = next;
        return changed;
    }

    private static boolean updateDouble(
            final double value,
            final double min,
            final double max,
            final double current,
            final java.util.function.DoubleConsumer setter) {
        final double next =
                Math.round(Math.max(min, Math.min(max, value)) * 1_000_000.0) / 1_000_000.0;
        if (Double.compare(next, current) == 0) {
            return false;
        }
        setter.accept(next);
        return true;
    }

    private static int clampInt(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    record Values(
            double pressure,
            double timbre,
            double pitch,
            double chance,
            double velocitySpread,
            int repeats) {
        void applyTo(final NoteStep note) {
            note.setPressure(pressure);
            note.setTimbre(timbre);
            note.setTranspose(pitch);
            note.setChance(chance);
            note.setIsChanceEnabled(chance < 1.0);
            note.setVelocitySpread(velocitySpread);
            note.setRepeatCount(repeats);
            note.setIsRepeatEnabled(repeats > 0);
        }
    }
}
