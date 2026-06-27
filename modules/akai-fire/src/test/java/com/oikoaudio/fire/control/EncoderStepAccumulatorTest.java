package com.oikoaudio.fire.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncoderStepAccumulatorTest {
    @Test
    void accumulatesUntilThreshold() {
        final EncoderStepAccumulator accumulator = new EncoderStepAccumulator(3);

        assertEquals(0, accumulator.consume(1));
        assertEquals(0, accumulator.consume(1));
        assertEquals(1, accumulator.consume(1));
    }

    @Test
    void preservesCarryAfterMultiStepTurn() {
        final EncoderStepAccumulator accumulator = new EncoderStepAccumulator(3);

        assertEquals(2, accumulator.consume(7));
        assertEquals(0, accumulator.consume(1));
        assertEquals(1, accumulator.consume(1));
    }

    @Test
    void accumulatesNegativeTurns() {
        final EncoderStepAccumulator accumulator = new EncoderStepAccumulator(3);

        assertEquals(0, accumulator.consume(-1));
        assertEquals(0, accumulator.consume(-1));
        assertEquals(-1, accumulator.consume(-1));
    }

    @Test
    void resetClearsCarry() {
        final EncoderStepAccumulator accumulator = new EncoderStepAccumulator(3);

        assertEquals(0, accumulator.consume(2));
        accumulator.reset();
        assertEquals(0, accumulator.consume(1));
    }

    @Test
    void thresholdMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new EncoderStepAccumulator(0));
    }
}
