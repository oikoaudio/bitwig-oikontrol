package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DrumSequenceModeIdleDisplayTest {
    @Test
    void drumMetersOnlyShowWhileTransportIsPlaying() {
        assertEquals(
                true,
                DrumSequenceMode.shouldShowDrumMeters(
                        true, false, false, false, false, false, false, true));
        assertEquals(
                false,
                DrumSequenceMode.shouldShowDrumMeters(
                        true, false, false, false, false, false, false, false));
        assertEquals(
                false,
                DrumSequenceMode.shouldShowDrumMeters(
                        true, true, false, false, false, false, false, true));
    }

    @Test
    void contextIdleUsesPadInfoForNonMixerEncoderPages() {
        assertEquals(true, DrumSequenceMode.shouldShowDrumContextIdle(EncoderMode.CHANNEL, false));
        assertEquals(true, DrumSequenceMode.shouldShowDrumContextIdle(EncoderMode.USER_1, false));
        assertEquals(true, DrumSequenceMode.shouldShowDrumContextIdle(EncoderMode.USER_2, false));
        assertEquals(false, DrumSequenceMode.shouldShowDrumContextIdle(EncoderMode.MIXER, false));
        assertEquals(false, DrumSequenceMode.shouldShowDrumContextIdle(EncoderMode.CHANNEL, true));
    }
}
