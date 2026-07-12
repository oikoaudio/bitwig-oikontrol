package com.oikoaudio.fire.perform;

import com.oikoaudio.fire.sequence.EncoderMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerformEncoderControlsTest {
    @Test
    void cyclesPagesAndRestoresThePageUsedBeforeDeviceMode() {
        final PerformEncoderControls controls = new PerformEncoderControls();
        assertEquals(EncoderMode.CHANNEL, controls.mode());
        assertEquals(EncoderMode.MIXER, controls.nextMode());
        controls.switchMode(EncoderMode.USER_1);
        controls.enterMixDeviceMode();
        assertEquals(EncoderMode.USER_2, controls.mode());
        controls.leaveMixDeviceMode();
        assertEquals(EncoderMode.USER_1, controls.mode());
    }

    @Test
    void remotePageTargetsMatchExistingEncoderPages() {
        final PerformEncoderControls controls = new PerformEncoderControls();
        assertEquals(PerformEncoderControls.RemoteTarget.PROJECT, controls.remoteTarget());
        controls.switchMode(EncoderMode.MIXER);
        assertEquals(PerformEncoderControls.RemoteTarget.NONE, controls.remoteTarget());
        controls.switchMode(EncoderMode.USER_1);
        assertEquals(PerformEncoderControls.RemoteTarget.TRACK, controls.remoteTarget());
        controls.switchMode(EncoderMode.USER_2);
        assertEquals(PerformEncoderControls.RemoteTarget.DEVICE, controls.remoteTarget());
    }
}
