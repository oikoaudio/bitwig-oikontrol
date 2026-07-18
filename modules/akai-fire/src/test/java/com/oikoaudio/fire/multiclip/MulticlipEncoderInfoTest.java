package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.sequence.EncoderMode;
import org.junit.jupiter.api.Test;

class MulticlipEncoderInfoTest {
    @Test
    void describesTheSameFourEncoderPagesAsDrumXox() {
        assertEquals(
                "1: Note Length\n2: Chance\n3: Vel Spread\n4: Repeat",
                MulticlipEncoderController.modeInfo(EncoderMode.CHANNEL));
        assertEquals(
                "1: Volume\n2: Pan\n3: Send 1\n4: Send 2",
                MulticlipEncoderController.modeInfo(EncoderMode.MIXER));
        assertEquals(
                "1: Velocity\n2: Pressure\n3: Timbre\n4: Pitch Expr",
                MulticlipEncoderController.modeInfo(EncoderMode.USER_1));
        assertEquals(
                "1: Euclid Len\n2: Euclid Pulses\n3: Euclid Rotation\n4: Accent Density",
                MulticlipEncoderController.modeInfo(EncoderMode.USER_2));
    }

    @Test
    void providesPersistentOledFooterLabelsForEveryPage() {
        assertEquals("Len  Chnc VSpr Rpt", MulticlipEncoderController.footer(EncoderMode.CHANNEL));
        assertEquals("Vol  Pan  S1   S2", MulticlipEncoderController.footer(EncoderMode.MIXER));
        assertEquals("Velo Pres Timb PExp", MulticlipEncoderController.footer(EncoderMode.USER_1));
        assertEquals("ELen EPul ERot ADen", MulticlipEncoderController.footer(EncoderMode.USER_2));
    }
}
