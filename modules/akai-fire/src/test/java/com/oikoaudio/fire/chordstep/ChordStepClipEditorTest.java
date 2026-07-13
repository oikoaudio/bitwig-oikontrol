package com.oikoaudio.fire.chordstep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bitwig.extension.controller.api.Clip;
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
}
