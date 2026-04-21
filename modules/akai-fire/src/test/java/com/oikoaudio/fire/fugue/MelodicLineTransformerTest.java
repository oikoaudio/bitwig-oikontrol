package com.oikoaudio.fire.fugue;

import com.bitwig.extensions.framework.MusicalScale;
import com.bitwig.extensions.framework.MusicalScaleLibrary;
import com.oikoaudio.fire.melodic.MelodicPattern;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MelodicLineTransformerTest {

    @Test
    void initKeepsSourcePlacementAndPitch() {
        final FuguePattern transformed = MelodicLineTransformer.transform(source(),
                FugueLineSettings.init(), major(), 0);

        assertStep(transformed, 0, 60);
        assertStep(transformed, 4, 62);
        assertStep(transformed, 8, 64);
    }

    @Test
    void reverseMirrorsInsideLoop() {
        final FuguePattern transformed = MelodicLineTransformer.transform(source(),
                new FugueLineSettings(FugueDirection.REVERSE, FugueSpeed.NORMAL, 0, 0), major(), 0);

        assertStep(transformed, 15, 60);
        assertStep(transformed, 11, 62);
        assertStep(transformed, 7, 64);
    }

    @Test
    void startOffsetMovesLineWithoutChangingSource() {
        final FuguePattern transformed = MelodicLineTransformer.transform(source(),
                new FugueLineSettings(FugueDirection.FORWARD, FugueSpeed.NORMAL, 2, 0), major(), 0);

        assertStep(transformed, 2, 60);
        assertStep(transformed, 6, 62);
        assertStep(transformed, 10, 64);
    }

    @Test
    void speedUpCompressesStepPositions() {
        final FuguePattern transformed = MelodicLineTransformer.transform(source(),
                new FugueLineSettings(FugueDirection.FORWARD, FugueSpeed.TIMES_2, 0, 0), major(), 0);

        assertStep(transformed, 0, 60);
        assertStep(transformed, 2, 62);
        assertStep(transformed, 4, 64);
    }

    @Test
    void speedUpRepeatsCompressedPhraseAcrossFullLoop() {
        final FuguePattern transformed = MelodicLineTransformer.transform(source(),
                new FugueLineSettings(FugueDirection.FORWARD, FugueSpeed.TIMES_2, 0, 0), major(), 0);

        assertStep(transformed, 8, 60);
        assertStep(transformed, 10, 62);
        assertStep(transformed, 12, 64);
    }

    @Test
    void speedUpUsesRepeatedPhraseUnitRatherThanCompressingWholeExpandedLoop() {
        final FuguePattern transformed = MelodicLineTransformer.transform(repeatedSource(),
                new FugueLineSettings(FugueDirection.FORWARD, FugueSpeed.TIMES_4, 0, 0), major(), 0);

        assertStep(transformed, 0, 60);
        assertStep(transformed, 1, 62);
        assertStep(transformed, 2, 64);
        assertStep(transformed, 4, 60);
        assertStep(transformed, 5, 62);
        assertStep(transformed, 6, 64);
        assertStep(transformed, 124, 60);
        assertStep(transformed, 125, 62);
        assertStep(transformed, 126, 64);
    }

    @Test
    void speedUpCanCreateThirtySecondNotesFromSixteenthSourceSteps() {
        final FuguePattern transformed = MelodicLineTransformer.transform(sixteenthRunSource(),
                new FugueLineSettings(FugueDirection.FORWARD, FugueSpeed.TIMES_2, 0, 0), major(), 0);

        assertStep(transformed, 0, 60);
        assertStep(transformed, 1, 62);
        assertStep(transformed, 2, 64);
    }

    @Test
    void transformPreservesPolyphonicSourceSteps() {
        final FuguePattern transformed = MelodicLineTransformer.transform(polyphonicSource(),
                FugueLineSettings.init(), major(), 0);

        assertEquals(2, transformed.notesAt(0).size());
        assertEquals(60, transformed.notesAt(0).get(0).pitch());
        assertEquals(67, transformed.notesAt(0).get(1).pitch());
    }

    @Test
    void speedScalingCanCreateShortAndLongGates() {
        final FuguePattern fast = MelodicLineTransformer.transform(oneStepSourceWithGate(1.0),
                new FugueLineSettings(FugueDirection.FORWARD, FugueSpeed.TIMES_8, 0, 0), major(), 0);
        final FuguePattern slow = MelodicLineTransformer.transform(oneStepSourceWithGate(1.0),
                new FugueLineSettings(FugueDirection.FORWARD, FugueSpeed.DIVIDE_8, 0, 0), major(), 0);

        assertEquals(0.125, fast.step(0).gate());
        assertEquals(8.0, slow.step(0).gate());
    }

    @Test
    void speedPaletteIncludesTripletAndDottedSlots() {
        assertEquals("2 dt", FugueSpeed.NORMAL.next(1).label());
        assertEquals("1 trp", FugueSpeed.TIMES_2_DOTTED.next(1).label());
        assertEquals("2", FugueSpeed.TIMES_TRIPLET.next(1).label());
        assertEquals("2 trp", FugueSpeed.TIMES_2.next(1).label());
        assertEquals("3 dt", FugueSpeed.TIMES_2_TRIPLET.next(1).label());
        assertEquals("8", FugueSpeed.TIMES_6.next(1).label());
        assertEquals("8", FugueSpeed.TIMES_8.next(1).label());
        assertEquals("/8", FugueSpeed.DIVIDE_8.next(-1).label());
    }

    @Test
    void pitchOffsetMovesByScaleDegreesByDefault() {
        final FuguePattern transformed = MelodicLineTransformer.transform(source(),
                new FugueLineSettings(FugueDirection.FORWARD, FugueSpeed.NORMAL, 0, 6), major(), 0);

        assertStep(transformed, 0, 71);
        assertStep(transformed, 4, 72);
        assertStep(transformed, 8, 74);
        assertTrue(major().isMidiNoteInScale(0, transformed.step(0).pitch()));
    }

    @Test
    void semitoneOffsetStaysExactEvenForChromaticSourceNotes() {
        final FuguePattern transformed = MelodicLineTransformer.transform(chromaticSource(),
                FugueLineSettings.semitone(FugueDirection.FORWARD, FugueSpeed.NORMAL, 0, 12), major(), 0);

        assertStep(transformed, 0, 73);
        assertStep(transformed, 4, 75);
    }

    @Test
    void chromaticScaleMakesDegreeOffsetBehaveLikeSemitones() {
        final FuguePattern transformed = MelodicLineTransformer.transform(chromaticSource(),
                new FugueLineSettings(FugueDirection.FORWARD, FugueSpeed.NORMAL, 0, 12), chromatic(), 0);

        assertStep(transformed, 0, 73);
        assertStep(transformed, 4, 75);
    }

    @Test
    void pitchIntervalsCanStepContinuouslyAndJumpOctaves() {
        assertEquals(1, FuguePitchIntervals.nextDegreeInterval(0, 1));
        assertEquals(2, FuguePitchIntervals.nextDegreeInterval(1, 1));
        assertEquals(12, FuguePitchIntervals.nextDegreeInterval(11, 1));
        assertEquals(19, FuguePitchIntervals.octaveJump(12, 1));
        assertEquals(5, FuguePitchIntervals.octaveJump(12, -1));
        assertEquals("+19", FuguePitchIntervals.label(11));
        assertEquals("+40", FuguePitchIntervals.label(23));
    }

    @Test
    void lineExpressionSettingsModifyGeneratedNotes() {
        final FuguePattern transformed = MelodicLineTransformer.transform(source(),
                new FugueLineSettings(FugueDirection.FORWARD, FugueSpeed.NORMAL, 0, 0, 0, -16, 50, 150),
                major(), 0);

        final MelodicPattern.Step step = transformed.step(0);
        assertEquals(80, step.velocity());
        assertEquals(0.5, step.chance());
        assertEquals(1.2, step.gate());
    }

    @Test
    void presetExampleStaysDeterministic() {
        final FuguePattern a = MelodicLineTransformer.transform(source(),
                FuguePreset.REVERSE_DOUBLE.settings(), major(), 0);
        final FuguePattern b = MelodicLineTransformer.transform(source(),
                FuguePreset.REVERSE_DOUBLE.settings(), major(), 0);

        assertEquals(activeMask(a), activeMask(b));
        assertEquals(pitchMask(a), pitchMask(b));
    }

    private static FuguePattern source() {
        final List<MelodicPattern.Step> steps = new ArrayList<>();
        for (int i = 0; i < FuguePattern.MAX_STEPS; i++) {
            steps.add(MelodicPattern.Step.rest(i));
        }
        steps.set(0, note(0, 60));
        steps.set(4, note(4, 62));
        steps.set(8, note(8, 64));
        return new FuguePattern(steps, 16);
    }

    private static FuguePattern chromaticSource() {
        final List<MelodicPattern.Step> steps = new ArrayList<>();
        for (int i = 0; i < FuguePattern.MAX_STEPS; i++) {
            steps.add(MelodicPattern.Step.rest(i));
        }
        steps.set(0, note(0, 61));
        steps.set(4, note(4, 63));
        return new FuguePattern(steps, 16);
    }

    private static FuguePattern repeatedSource() {
        final List<MelodicPattern.Step> steps = new ArrayList<>();
        for (int i = 0; i < FuguePattern.MAX_STEPS; i++) {
            steps.add(MelodicPattern.Step.rest(i));
        }
        for (int offset = 0; offset < FuguePattern.MAX_STEPS; offset += 16) {
            steps.set(offset, note(offset, 60));
            steps.set(offset + 4, note(offset + 4, 62));
            steps.set(offset + 8, note(offset + 8, 64));
        }
        return new FuguePattern(steps, 128);
    }

    private static FuguePattern sixteenthRunSource() {
        final List<MelodicPattern.Step> steps = emptySteps();
        steps.set(0, noteWithGate(0, 60, 2.0));
        steps.set(2, noteWithGate(2, 62, 2.0));
        steps.set(4, noteWithGate(4, 64, 2.0));
        return new FuguePattern(steps, 32);
    }

    private static FuguePattern polyphonicSource() {
        final List<List<MelodicPattern.Step>> steps = FuguePattern.emptyPolySteps();
        steps.get(0).add(note(0, 60));
        steps.get(0).add(note(0, 67));
        return new FuguePattern(steps, 16, true);
    }

    private static FuguePattern oneStepSourceWithGate(final double gate) {
        final List<MelodicPattern.Step> steps = emptySteps();
        steps.set(0, noteWithGate(0, 60, gate));
        return new FuguePattern(steps, 16);
    }

    private static List<MelodicPattern.Step> emptySteps() {
        final List<MelodicPattern.Step> steps = new ArrayList<>();
        for (int i = 0; i < FuguePattern.MAX_STEPS; i++) {
            steps.add(MelodicPattern.Step.rest(i));
        }
        return steps;
    }

    private static MelodicPattern.Step note(final int index, final int pitch) {
        return new MelodicPattern.Step(index, true, false, pitch, 96, 0.8, false, false);
    }

    private static MelodicPattern.Step noteWithGate(final int index, final int pitch, final double gate) {
        return new MelodicPattern.Step(index, true, false, pitch, 96, gate, false, false);
    }

    private static MusicalScale major() {
        return MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)");
    }

    private static MusicalScale chromatic() {
        return MusicalScaleLibrary.getInstance().getMusicalScale("Chromatic");
    }

    private static void assertStep(final FuguePattern pattern, final int stepIndex, final int pitch) {
        assertTrue(pattern.step(stepIndex).active(), "Expected active step " + stepIndex);
        assertEquals(pitch, pattern.step(stepIndex).pitch());
    }

    private static String activeMask(final FuguePattern pattern) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < pattern.loopSteps(); i++) {
            out.append(pattern.step(i).active() ? '1' : '0');
        }
        return out.toString();
    }

    private static String pitchMask(final FuguePattern pattern) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < pattern.loopSteps(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(pattern.step(i).pitch() == null ? -1 : pattern.step(i).pitch());
        }
        return out.toString();
    }
}
