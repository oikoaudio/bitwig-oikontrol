package com.oikoaudio.fire.fugue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.display.EncoderFooterLegend;
import com.oikoaudio.fire.sequence.EncoderMode;
import org.junit.jupiter.api.Test;

class FugueEncoderControlsTest {
    @Test
    void mapsPagesAndLinesInHardwareOrder() {
        final FugueEncoderControls controls = new FugueEncoderControls();

        assertEquals(0, controls.selectedLine());
        controls.selectLine(2);
        assertEquals(EncoderMode.USER_1, controls.mode());
        assertEquals(2, controls.selectedLine());
        controls.selectLine(99);
        assertEquals(3, controls.selectedLine());
    }

    @Test
    void cyclesPagesAndProvidesTheirPresentation() {
        final FugueEncoderControls controls = new FugueEncoderControls();

        controls.cycle();
        assertEquals(1, controls.selectedLine());
        assertEquals("Var 2", controls.title());
        assertEquals("1 Dir\n2 Tempo\n3 Start\n4 Pitch", controls.details());
        assertEquals(EncoderFooterLegend.of("Dir", "Temp", "Strt", "Ptch"), controls.footer());
    }
}
