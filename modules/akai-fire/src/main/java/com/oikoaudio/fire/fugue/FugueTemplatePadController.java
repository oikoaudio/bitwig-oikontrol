package com.oikoaudio.fire.fugue;

import com.oikoaudio.fire.note.NoteGridLayout;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.IntSupplier;

/** Owns the lifecycle and clip mutations of one pressed Fugue template pad. */
public final class FugueTemplatePadController {
    public static final Comparator<Note> NOTE_ORDER =
            Comparator.comparingInt(Note::pitch).thenComparingInt(Note::step);

    private static final int DEFAULT_VELOCITY = 96;
    private static final double DEFAULT_CHANCE = 1.0;

    private final ClipPort clip;
    private final double stepLength;
    private final IntSupplier defaultPitch;
    private final Transposer transposer;
    private Edit edit;

    public FugueTemplatePadController(
            final ClipPort clip,
            final double stepLength,
            final IntSupplier defaultPitch,
            final Transposer transposer) {
        this.clip = clip;
        this.stepLength = stepLength;
        this.defaultPitch = defaultPitch;
        this.transposer = transposer;
    }

    public Edit press(final int column, final int loopSteps) {
        final int start = column * loopSteps / 16;
        final int end = Math.max(start + 1, (column + 1) * loopSteps / 16);
        edit =
                clip.findNote(start, end)
                        .map(
                                note ->
                                        new Edit(
                                                column,
                                                start,
                                                note.step(),
                                                note.pitch(),
                                                true,
                                                false,
                                                note.velocity(),
                                                note.chance(),
                                                note.duration()))
                        .orElseGet(
                                () ->
                                        new Edit(
                                                column,
                                                start,
                                                start,
                                                clampPitch(defaultPitch.getAsInt()),
                                                false,
                                                false,
                                                DEFAULT_VELOCITY,
                                                DEFAULT_CHANCE,
                                                stepLength * 2));
        return edit;
    }

    public ReleaseResult release(final int column) {
        if (edit == null || edit.column() != column) {
            return ReleaseResult.NO_OP;
        }
        final Edit finished = edit;
        edit = null;
        if (!finished.changed() && finished.existed()) {
            clip.clear(finished.step(), finished.pitch());
            return ReleaseResult.REMOVED;
        }
        if (!finished.changed()) {
            write(finished.withChanged(true));
            edit = null;
            return ReleaseResult.ADDED;
        }
        return ReleaseResult.KEPT;
    }

    public boolean isEditing() {
        return edit != null;
    }

    public Edit edit() {
        return edit;
    }

    public String pitchLabel() {
        return edit == null
                ? ""
                : "%s%d".formatted(NoteGridLayout.noteName(edit.pitch()), edit.pitch() / 12 - 1);
    }

    public void adjustVelocity(final int increment) {
        if (edit != null) {
            write(edit.withVelocity(edit.velocity() + increment * 4).withChanged(true));
        }
    }

    public void adjustChance(final int increment) {
        if (edit != null) {
            write(edit.withChance(edit.chance() + increment * 0.05).withChanged(true));
        }
    }

    public void adjustGate(final int increment) {
        if (edit != null) {
            write(
                    edit.withDuration(
                                    Math.max(
                                            stepLength * 0.02,
                                            edit.duration() + increment * stepLength))
                            .withChanged(true));
        }
    }

    public void transpose(final int scaleDegrees) {
        if (edit == null) {
            return;
        }
        final int pitch = clampPitch(transposer.transpose(edit.pitch(), scaleDegrees));
        if (pitch != edit.pitch() || !edit.existed()) {
            write(edit.withPitch(pitch).withChanged(true));
        }
    }

    public void resetVelocity() {
        update(edit == null ? null : edit.withVelocity(DEFAULT_VELOCITY));
    }

    public void resetChance() {
        update(edit == null ? null : edit.withChance(DEFAULT_CHANCE));
    }

    public void resetGate() {
        update(edit == null ? null : edit.withDuration(stepLength * 2));
    }

    public void resetPitch() {
        update(edit == null ? null : edit.withPitch(clampPitch(defaultPitch.getAsInt())));
    }

    private void update(final Edit next) {
        if (next != null) {
            write(next.withChanged(true));
        }
    }

    private void write(final Edit next) {
        if (edit != null && edit.existed()) {
            clip.clear(edit.step(), edit.pitch());
        }
        clip.write(next);
        edit = next.withExisted(true);
    }

    private static int clampPitch(final int pitch) {
        return Math.max(0, Math.min(127, pitch));
    }

    public interface ClipPort {
        Optional<Note> findNote(int start, int end);

        void clear(int step, int pitch);

        void write(Edit edit);
    }

    @FunctionalInterface
    public interface Transposer {
        int transpose(int pitch, int scaleDegrees);
    }

    public enum ReleaseResult {
        NO_OP,
        ADDED,
        REMOVED,
        KEPT
    }

    public record Note(int step, int pitch, int velocity, double chance, double duration) {}

    public record Edit(
            int column,
            int bucketStart,
            int step,
            int pitch,
            boolean existed,
            boolean changed,
            int velocity,
            double chance,
            double duration) {
        private Edit withPitch(final int value) {
            return new Edit(
                    column, bucketStart, step, value, existed, changed, velocity, chance, duration);
        }

        private Edit withVelocity(final int value) {
            return new Edit(
                    column,
                    bucketStart,
                    step,
                    pitch,
                    existed,
                    changed,
                    Math.max(1, Math.min(127, value)),
                    chance,
                    duration);
        }

        private Edit withChance(final double value) {
            return new Edit(
                    column,
                    bucketStart,
                    step,
                    pitch,
                    existed,
                    changed,
                    velocity,
                    Math.max(0.0, Math.min(1.0, value)),
                    duration);
        }

        private Edit withDuration(final double value) {
            return new Edit(
                    column, bucketStart, step, pitch, existed, changed, velocity, chance, value);
        }

        private Edit withExisted(final boolean value) {
            return new Edit(
                    column, bucketStart, step, pitch, value, changed, velocity, chance, duration);
        }

        private Edit withChanged(final boolean value) {
            return new Edit(
                    column, bucketStart, step, pitch, existed, value, velocity, chance, duration);
        }
    }
}
