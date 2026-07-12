package com.oikoaudio.fire.fugue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FugueTemplatePadControllerTest {
    private static final double STEP_LENGTH = 0.125;

    @Test
    void createsANoteOnReleaseForAnEmptyBucket() {
        final FakePort port = new FakePort();
        final FugueTemplatePadController controller = controller(port);

        controller.press(3, 32);
        assertTrue(controller.isEditing());
        assertEquals(6, controller.edit().step());
        assertEquals(60, controller.edit().pitch());

        assertEquals(FugueTemplatePadController.ReleaseResult.ADDED, controller.release(3));
        assertEquals(List.of(new Write(6, 60, 96, 0.25, 1.0)), port.writes);
        assertFalse(controller.isEditing());
    }

    @Test
    void findsAndRemovesTheLowestPitchExistingNoteWhenReleasedUnchanged() {
        final FakePort port = new FakePort();
        port.notes = List.of(
                new FugueTemplatePadController.Note(7, 67, 100, 0.8, 0.5),
                new FugueTemplatePadController.Note(6, 64, 80, 0.7, 0.375));
        final FugueTemplatePadController controller = controller(port);

        controller.press(3, 32);
        assertEquals(64, controller.edit().pitch());
        assertEquals(FugueTemplatePadController.ReleaseResult.REMOVED, controller.release(3));
        assertEquals(List.of("6:64"), port.clears);
        assertTrue(port.writes.isEmpty());
    }

    @Test
    void ignoresAReleaseFromAnotherPad() {
        final FakePort port = new FakePort();
        final FugueTemplatePadController controller = controller(port);

        controller.press(2, 32);

        assertEquals(FugueTemplatePadController.ReleaseResult.NO_OP, controller.release(3));
        assertTrue(controller.isEditing());
    }

    @Test
    void encoderEditsClampAndRewriteTheLiveNote() {
        final FakePort port = new FakePort();
        final FugueTemplatePadController controller = controller(port);
        controller.press(0, 16);

        controller.adjustVelocity(-100);
        controller.adjustChance(-100);
        controller.adjustGate(-100);
        controller.transpose(100);

        assertEquals(1, controller.edit().velocity());
        assertEquals(0.0, controller.edit().chance());
        assertEquals(STEP_LENGTH * 0.02, controller.edit().duration());
        assertEquals(127, controller.edit().pitch());
        assertTrue(controller.edit().changed());
        assertEquals(4, port.writes.size());
        assertEquals(3, port.clears.size());
    }

    @Test
    void resetsEachEditableValue() {
        final FakePort port = new FakePort();
        final FugueTemplatePadController controller = controller(port);
        controller.press(0, 16);
        controller.adjustVelocity(-1);
        controller.adjustChance(-1);
        controller.adjustGate(1);
        controller.transpose(1);

        controller.resetVelocity();
        controller.resetChance();
        controller.resetGate();
        controller.resetPitch();

        assertEquals(96, controller.edit().velocity());
        assertEquals(1.0, controller.edit().chance());
        assertEquals(STEP_LENGTH * 2, controller.edit().duration());
        assertEquals(60, controller.edit().pitch());
    }

    @Test
    void changedSessionIsKeptOnRelease() {
        final FakePort port = new FakePort();
        final FugueTemplatePadController controller = controller(port);
        controller.press(0, 16);
        controller.adjustVelocity(1);

        assertEquals(FugueTemplatePadController.ReleaseResult.KEPT, controller.release(0));
        assertEquals(100, port.writes.getFirst().velocity());
    }

    private static FugueTemplatePadController controller(final FakePort port) {
        return new FugueTemplatePadController(port, STEP_LENGTH, () -> 60,
                (pitch, degrees) -> Math.max(0, Math.min(127, pitch + degrees)));
    }

    private static final class FakePort implements FugueTemplatePadController.ClipPort {
        private List<FugueTemplatePadController.Note> notes = List.of();
        private final List<Write> writes = new ArrayList<>();
        private final List<String> clears = new ArrayList<>();

        @Override
        public Optional<FugueTemplatePadController.Note> findNote(final int start, final int end) {
            return notes.stream()
                    .filter(note -> note.step() >= start && note.step() < end)
                    .min(FugueTemplatePadController.NOTE_ORDER);
        }

        @Override
        public void clear(final int step, final int pitch) {
            clears.add(step + ":" + pitch);
        }

        @Override
        public void write(final FugueTemplatePadController.Edit edit) {
            writes.add(new Write(edit.step(), edit.pitch(), edit.velocity(), edit.duration(), edit.chance()));
        }
    }

    private record Write(int step, int pitch, int velocity, double duration, double chance) {
    }
}
