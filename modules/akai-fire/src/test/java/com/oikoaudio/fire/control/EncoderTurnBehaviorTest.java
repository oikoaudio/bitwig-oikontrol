package com.oikoaudio.fire.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncoderTurnBehaviorTest {

    @Test
    void thresholdedBehaviorAccumulatesAndResets() {
        final EncoderTurnBehavior behavior = EncoderTurnBehavior.thresholded(2, 3);

        assertEquals(0, behavior.apply(1, false));
        assertEquals(1, behavior.apply(1, false));
        assertEquals(0, behavior.apply(2, true));
        behavior.reset();
        assertEquals(0, behavior.apply(2, true));
        assertEquals(1, behavior.apply(1, true));
    }

    @Test
    void continuousBehaviorUsesFinePathWithoutAccelerationState() {
        final EncoderTurnBehavior behavior = EncoderTurnBehavior.continuous();

        assertEquals(1, behavior.apply(1, false));
        assertEquals(1, behavior.apply(1, true));
        assertEquals(1, behavior.apply(1, false));
    }
}
