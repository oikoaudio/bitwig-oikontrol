package com.oikoaudio.fire.fugue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FugueGridGestureTest {
    @Test
    void resolvesSourceAndDerivedTargetsBeforeTheSharedLengthModifier() {
        assertEquals(FugueGridGesture.SOURCE_PLAY_START, FugueGridGesture.resolve(true, false));
        assertEquals(FugueGridGesture.DERIVED_LINE_START, FugueGridGesture.resolve(false, false));
        assertEquals(FugueGridGesture.CLIP_LENGTH, FugueGridGesture.resolve(true, true));
        assertEquals(FugueGridGesture.CLIP_LENGTH, FugueGridGesture.resolve(false, true));
    }
}
