package com.oikoaudio.fire.values;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.Clip;
import com.oikoaudio.fire.testutil.BitwigApiValueStubs.BeatTimeValueStub;
import org.junit.jupiter.api.Test;

class StepViewPositionTest {

    @Test
    void loopLengthObserverUpdatesStepsAndScrollState() {
        final Clip clip = mock(Clip.class);
        final BeatTimeValueStub loopLength = new BeatTimeValueStub();
        final BeatTimeValueStub loopStart = new BeatTimeValueStub();
        when(clip.getLoopLength()).thenReturn(loopLength.value());
        when(clip.getLoopStart()).thenReturn(loopStart.value());

        final StepViewPosition position = new StepViewPosition(clip, 32, "TEST");

        loopLength.emit(16.0);

        assertEquals(64, position.getSteps());
        assertEquals(64, position.getStepsValue().get());
        assertEquals(2, position.getPages());
        assertFalse(position.canScrollLeft().get());
        assertTrue(position.canScrollRight().get());
    }

    @Test
    void loopStartMovesTheObservedPageToTheLoopWindow() {
        final Clip clip = mock(Clip.class);
        final BeatTimeValueStub loopLength = new BeatTimeValueStub();
        final BeatTimeValueStub loopStart = new BeatTimeValueStub();
        when(clip.getLoopLength()).thenReturn(loopLength.value());
        when(clip.getLoopStart()).thenReturn(loopStart.value());

        final StepViewPosition position = new StepViewPosition(clip, 32, "TEST");

        loopLength.emit(16.0);
        loopStart.emit(8.0);
        verify(clip).scrollToStep(32);

        position.setPage(1);
        verify(clip).scrollToStep(64);
        assertEquals(64, position.getAbsoluteStepOffset());
    }
}
