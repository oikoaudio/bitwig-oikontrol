package com.oikoaudio.fire.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VuMeterPeakHoldTest {

    @Test
    void holdsThenDecaysPeakValues() {
        final VuMeterPeakHold hold = new VuMeterPeakHold(1);

        hold.update(0, 80);
        for (int i = 0; i < 5; i++) {
            hold.decay();
        }
        assertEquals(80, hold.valueAt(0));

        hold.decay();

        assertEquals(78, hold.valueAt(0));
    }

    @Test
    void lowerUpdatesDoNotPullHeldPeakDown() {
        final VuMeterPeakHold hold = new VuMeterPeakHold(1);

        hold.update(0, 80);
        hold.update(0, 20);

        assertEquals(80, hold.valueAt(0));
    }
}
