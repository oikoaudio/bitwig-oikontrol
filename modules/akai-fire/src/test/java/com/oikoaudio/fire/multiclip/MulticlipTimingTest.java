package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MulticlipTimingTest {
    @ParameterizedTest
    @CsvSource({"1,0.25", "3,0.75", "5,1.25", "7,1.75", "12,3.0", "17,4.25"})
    void convertsEveryWholeStepLengthToBeatTime(final int steps, final double beats) {
        assertEquals(beats, MulticlipTiming.beatsForSteps(steps));
    }

    @ParameterizedTest
    @CsvSource({"1,-1,1", "3,1,4", "256,1,256"})
    void adjustsAndClampsLoopLengthInSingleSteps(
            final int current, final int delta, final int expected) {
        assertEquals(expected, MulticlipTiming.adjustLoopSteps(current, delta));
    }

    @ParameterizedTest
    @CsvSource({"7,0,32,7", "40,32,32,8", "7,32,32,0", "64,32,32,32"})
    void reportsHowMuchOfTheActiveClipLengthFallsOnTheVisiblePage(
            final int loopSteps,
            final int firstVisibleStep,
            final int visibleStepCount,
            final int expectedVisibleSteps) {
        assertEquals(
                expectedVisibleSteps,
                MulticlipTiming.visibleLoopStepCount(
                        MulticlipTiming.beatsForSteps(loopSteps),
                        firstVisibleStep,
                        visibleStepCount));
    }
}
