package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MulticlipRowButtonActionTest {
    @Test
    void altSelectsShiftSolosAndUnmodifiedPressMutes() {
        assertEquals(
                MulticlipRowButtonAction.SELECT, MulticlipRowButtonAction.resolve(true, false));
        assertEquals(MulticlipRowButtonAction.SELECT, MulticlipRowButtonAction.resolve(true, true));
        assertEquals(MulticlipRowButtonAction.SOLO, MulticlipRowButtonAction.resolve(false, true));
        assertEquals(MulticlipRowButtonAction.MUTE, MulticlipRowButtonAction.resolve(false, false));
    }
}
