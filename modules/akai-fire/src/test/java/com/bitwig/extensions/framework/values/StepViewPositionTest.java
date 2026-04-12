package com.bitwig.extensions.framework.values;

import com.bitwig.extension.callback.DoubleValueChangedCallback;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StepViewPositionTest {

    @Test
    void loopLengthObserverUpdatesStepsAndScrollState() {
        final Clip clip = mock(Clip.class);
        final SettableBeatTimeValue loopLength = mock(SettableBeatTimeValue.class);
        when(clip.getLoopLength()).thenReturn(loopLength);

        final StepViewPosition position = new StepViewPosition(clip, 32, "TEST");

        final ArgumentCaptor<DoubleValueChangedCallback> observerCaptor =
                ArgumentCaptor.forClass(DoubleValueChangedCallback.class);
        verify(loopLength).addValueObserver(observerCaptor.capture());

        observerCaptor.getValue().valueChanged(16.0);

        assertEquals(64, position.getSteps());
        assertEquals(64, position.getStepsValue().get());
        assertEquals(2, position.getPages());
        assertFalse(position.canScrollLeft().get());
        assertTrue(position.canScrollRight().get());
    }
}
