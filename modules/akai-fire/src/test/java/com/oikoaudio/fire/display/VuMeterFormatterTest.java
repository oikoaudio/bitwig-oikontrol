package com.oikoaudio.fire.display;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VuMeterFormatterTest {

    @Test
    void formatsScaledMeterValuesAsDbFs() {
        assertEquals("-inf dB", VuMeterFormatter.meterValue(0));
        assertEquals("-6.0 dB", VuMeterFormatter.meterValue(64));
        assertEquals("+0.0 dB", VuMeterFormatter.meterValue(127));
    }

    @Test
    void formatsShortMeterValuesWithoutUnit() {
        assertEquals("-inf", VuMeterFormatter.meterValueShort(0));
        assertEquals("-6.0", VuMeterFormatter.meterValueShort(64));
        assertEquals("+0.0", VuMeterFormatter.meterValueShort(127));
    }

    @Test
    void meterHeightClampsToVisibleRange() {
        assertEquals(0, VuMeterFormatter.meterHeight(-1, 40));
        assertEquals(20, VuMeterFormatter.meterHeight(64, 40));
        assertEquals(40, VuMeterFormatter.meterHeight(999, 40));
    }

    @Test
    void meterPairLineFitsOledTextWidth() {
        assertEquals("+0.0   -6.0", VuMeterFormatter.meterPairLine(127, 64));
    }

    @Test
    void meterPairLineKeepsCompactSpacingAfterDoubleDigitPeakValues() {
        assertEquals("-16.1  -6.0", VuMeterFormatter.meterPairLine(20, 64));
    }
}
