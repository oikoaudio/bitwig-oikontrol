package com.oikoaudio.fire.nestedrhythm;

import com.oikoaudio.fire.sequence.RecurrencePattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestedRhythmContourShaperTest {

    @Test
    void unitExpressionUsesContourAndClampsIntoUnitRange() {
        assertEquals(1.0, NestedRhythmContourShaper.shapeUnitExpression(0, 0.8, 0.4, 0));
        assertEquals(0.3, NestedRhythmContourShaper.shapeUnitExpression(1, 0.5, 0.4, 0));
    }

    @Test
    void signedExpressionAndPitchExpressionClampTheirRanges() {
        assertEquals(-1.0, NestedRhythmContourShaper.shapeSignedExpression(3, -0.8, 0.6, 0));
        assertEquals(24.0, NestedRhythmContourShaper.shapePitchExpression(0, 20.0, 10.0, 0));
    }

    @Test
    void fullPlayChanceKeepsBaselineForAllRoles() {
        final double anchor = NestedRhythmContourShaper.shapeChance(
                1, NestedRhythmPattern.Role.PRIMARY_ANCHOR, 0.8, 1.0, 0);
        final double interior = NestedRhythmContourShaper.shapeChance(
                1, NestedRhythmPattern.Role.RATCHET_INTERIOR, 0.8, 1.0, 0);

        assertEquals(0.8, anchor);
        assertEquals(0.8, interior);
    }

    @Test
    void loweredPlayChanceAttenuatesInteriorHitsMoreThanAnchors() {
        final double anchor = NestedRhythmContourShaper.shapeChance(
                1, NestedRhythmPattern.Role.PRIMARY_ANCHOR, 0.8, 0.0, 0);
        final double interior = NestedRhythmContourShaper.shapeChance(
                1, NestedRhythmPattern.Role.RATCHET_INTERIOR, 0.8, 0.0, 0);

        assertTrue(interior < anchor);
    }

    @Test
    void loweredPlayChancePreservesContourPriorityWithinRole() {
        final double strong = NestedRhythmContourShaper.shapeChance(
                0, NestedRhythmPattern.Role.RATCHET_INTERIOR, 0.8, 0.0, 0);
        final double weak = NestedRhythmContourShaper.shapeChance(
                1, NestedRhythmPattern.Role.RATCHET_INTERIOR, 0.8, 0.0, 0);

        assertTrue(strong > weak);
    }

    @Test
    void recurrenceDefaultsCanBeDisabled() {
        assertEquals(RecurrencePattern.of(0, 0),
                NestedRhythmContourShaper.generatedRecurrence(0, NestedRhythmPattern.Role.TUPLET_INTERIOR, 0.0));
    }

    @Test
    void recurrenceDefaultsPreferDropoutsForWeakerRoles() {
        final RecurrencePattern anchor = NestedRhythmContourShaper.generatedRecurrence(
                0, NestedRhythmPattern.Role.PRIMARY_ANCHOR, 1.0);
        final RecurrencePattern interior = NestedRhythmContourShaper.generatedRecurrence(
                0, NestedRhythmPattern.Role.RATCHET_INTERIOR, 1.0);

        assertTrue(interior.length() >= anchor.length());
        assertTrue(Integer.bitCount(interior.mask()) <= Integer.bitCount(anchor.mask()));
    }

    @Test
    void distributedMaskRotatesActiveBits() {
        assertEquals(0b10101010, NestedRhythmContourShaper.distributedMask(8, 4, 0));
        assertEquals(0b01010101, NestedRhythmContourShaper.distributedMask(8, 4, 1));
    }
}
