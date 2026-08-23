package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MulticlipPatternGestureTest {
    @Test
    void pagesTimeNormallyAndScenesWithShift() {
        assertEquals(MulticlipPatternGesture.TIME_PAGE, MulticlipPatternGesture.resolve(false));
        assertEquals(MulticlipPatternGesture.SCENE_PAGE, MulticlipPatternGesture.resolve(true));
    }
}
