package com.oikoaudio.fire.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelativeEncoderMagnitudeTest {
    @Test
    void returnsZeroForNoAdjustment() {
        assertEquals(0, RelativeEncoderMagnitude.toSignedUnits(0.0));
    }

    @Test
    void preservesDirectionForTinyAdjustments() {
        assertEquals(1, RelativeEncoderMagnitude.toSignedUnits(1.0 / 127.0));
        assertEquals(-1, RelativeEncoderMagnitude.toSignedUnits(-1.0 / 127.0));
    }

    @Test
    void preservesRelativeMagnitudeForFasterTurns() {
        assertEquals(4, RelativeEncoderMagnitude.toSignedUnits(8.0 / 127.0));
        assertEquals(-4, RelativeEncoderMagnitude.toSignedUnits(-8.0 / 127.0));
    }

    @Test
    void fullScaleAdjustmentMapsToMaximumRelativeSpeed() {
        assertEquals(63, RelativeEncoderMagnitude.toSignedUnits(1.0));
        assertEquals(-63, RelativeEncoderMagnitude.toSignedUnits(-1.0));
    }

    @Test
    void directionStepClampsMagnitudeToSign() {
        assertEquals(1, RelativeEncoderMagnitude.toDirectionStep(63));
        assertEquals(-1, RelativeEncoderMagnitude.toDirectionStep(-63));
        assertEquals(0, RelativeEncoderMagnitude.toDirectionStep(0));
    }

    @Test
    void boundedMagnitudeStepPreservesObservedSpeedWithinLimit() {
        assertEquals(1, RelativeEncoderMagnitude.toBoundedMagnitudeStep(1, 4));
        assertEquals(4, RelativeEncoderMagnitude.toBoundedMagnitudeStep(4, 4));
        assertEquals(4, RelativeEncoderMagnitude.toBoundedMagnitudeStep(8, 4));
        assertEquals(-4, RelativeEncoderMagnitude.toBoundedMagnitudeStep(-8, 4));
        assertEquals(0, RelativeEncoderMagnitude.toBoundedMagnitudeStep(0, 4));
    }

    @Test
    void boundedMagnitudeStepRequiresPositiveLimit() {
        assertThrows(IllegalArgumentException.class, () -> RelativeEncoderMagnitude.toBoundedMagnitudeStep(1, 0));
    }

    @Test
    void standardTurnStepNormalizesSmallTurnsAndPreservesFastTurns() {
        assertEquals(0, RelativeEncoderMagnitude.toStandardTurnStep(0));
        assertEquals(1, RelativeEncoderMagnitude.toStandardTurnStep(1));
        assertEquals(1, RelativeEncoderMagnitude.toStandardTurnStep(2));
        assertEquals(1, RelativeEncoderMagnitude.toStandardTurnStep(3));
        assertEquals(4, RelativeEncoderMagnitude.toStandardTurnStep(4));
        assertEquals(4, RelativeEncoderMagnitude.toStandardTurnStep(6));
        assertEquals(8, RelativeEncoderMagnitude.toStandardTurnStep(8));
        assertEquals(-1, RelativeEncoderMagnitude.toStandardTurnStep(-2));
        assertEquals(-1, RelativeEncoderMagnitude.toStandardTurnStep(-3));
        assertEquals(-4, RelativeEncoderMagnitude.toStandardTurnStep(-6));
        assertEquals(-8, RelativeEncoderMagnitude.toStandardTurnStep(-8));
    }

    @Test
    void standardTurnStepFineModeUsesDirectionOnly() {
        assertEquals(1, RelativeEncoderMagnitude.toStandardTurnStep(8, true));
        assertEquals(-1, RelativeEncoderMagnitude.toStandardTurnStep(-8, true));
        assertEquals(0, RelativeEncoderMagnitude.toStandardTurnStep(0, true));
    }
}
