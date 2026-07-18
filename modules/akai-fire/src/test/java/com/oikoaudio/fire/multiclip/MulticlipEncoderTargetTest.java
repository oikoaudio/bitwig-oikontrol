package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.sequence.EncoderMode;
import org.junit.jupiter.api.Test;

class MulticlipEncoderTargetTest {
    @Test
    void channelEncodersControlClipLengthAndPlayStart() {
        assertEquals(
                new MulticlipEncoderTarget(MulticlipEncoderTarget.Kind.CLIP_LENGTH, 0),
                MulticlipEncoderTarget.resolve(EncoderMode.CHANNEL, 0));
        assertEquals(
                new MulticlipEncoderTarget(MulticlipEncoderTarget.Kind.PLAY_START, 0),
                MulticlipEncoderTarget.resolve(EncoderMode.CHANNEL, 1));
        assertEquals(
                MulticlipEncoderTarget.NONE,
                MulticlipEncoderTarget.resolve(EncoderMode.CHANNEL, 2));
    }

    @Test
    void mixerEncodersControlTheMatchingDrumPadMixerParameters() {
        for (int encoder = 0; encoder < 4; encoder++) {
            assertEquals(
                    new MulticlipEncoderTarget(MulticlipEncoderTarget.Kind.PAD_MIXER, encoder),
                    MulticlipEncoderTarget.resolve(EncoderMode.MIXER, encoder));
        }
    }

    @Test
    void userPagesExposeAllEightPadChainRemotes() {
        for (int encoder = 0; encoder < 4; encoder++) {
            assertEquals(
                    new MulticlipEncoderTarget(MulticlipEncoderTarget.Kind.PAD_REMOTE, encoder),
                    MulticlipEncoderTarget.resolve(EncoderMode.USER_1, encoder));
            assertEquals(
                    new MulticlipEncoderTarget(MulticlipEncoderTarget.Kind.PAD_REMOTE, encoder + 4),
                    MulticlipEncoderTarget.resolve(EncoderMode.USER_2, encoder));
        }
    }
}
