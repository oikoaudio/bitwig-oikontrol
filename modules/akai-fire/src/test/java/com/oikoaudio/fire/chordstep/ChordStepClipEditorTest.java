package com.oikoaudio.fire.chordstep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.NoteStep;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ChordStepClipEditorTest {
    @Test
    void replacesPitchesAtTheExistingFineStartWithPreservedVelocityAndDuration() {
        final Clip clip = mock(Clip.class);
        final AtomicInteger refreshes = new AtomicInteger();
        final ChordStepClipEditor<ChordStepEventIndex.Event> editor =
                new ChordStepClipEditor<>(
                        clip,
                        new ChordStepObservedState(),
                        new ChordStepFineNudgeSession<>(
                                step -> null, (amount, steps, events) -> {}),
                        step -> step,
                        step -> step * 16,
                        refreshes::incrementAndGet,
                        16);

        editor.replaceChordPitchesAtFineStart(3, 50, new int[] {62, 67}, 91, 0.75);

        for (int fineStep = 48; fineStep < 64; fineStep++) {
            verify(clip).clearStepsAtX(0, fineStep);
        }
        verify(clip).setStep(50, 62, 91, 0.75);
        verify(clip).setStep(50, 67, 91, 0.75);
        assertEquals(1, refreshes.get());
    }

    @Test
    void clearsTheExactFinePositionOwnedByTheTappedStep() {
        final Clip clip = mock(Clip.class);
        final ChordStepObservedState state = new ChordStepObservedState(() -> 8);
        state.handleObservedStepData(40, 60, NoteStep.State.NoteOn.ordinal(), 16);
        final ChordStepClipEditor<ChordStepEventIndex.Event> editor = editor(clip, state);

        editor.clearChordStep(3);

        verify(clip).clearStep(40, 60);
        verify(clip, never())
                .clearStepsAtX(
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void emptyPadDoesNotClearANeighboringFinePositionThatItDoesNotOwn() {
        final Clip clip = mock(Clip.class);
        final ChordStepObservedState state = new ChordStepObservedState(() -> 8);
        state.handleObservedStepData(40, 60, NoteStep.State.NoteOn.ordinal(), 16);
        final ChordStepClipEditor<ChordStepEventIndex.Event> editor = editor(clip, state);

        editor.clearChordStep(2);

        verify(clip, never())
                .clearStep(
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyInt());
        verify(clip, never())
                .clearStepsAtX(
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyInt());
    }

    private static ChordStepClipEditor<ChordStepEventIndex.Event> editor(
            final Clip clip, final ChordStepObservedState state) {
        return new ChordStepClipEditor<>(
                clip,
                state,
                new ChordStepFineNudgeSession<>(step -> null, (amount, steps, events) -> {}),
                step -> step,
                step -> step * 16,
                () -> {},
                16);
    }
}
