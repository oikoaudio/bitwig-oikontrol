package com.oikoaudio.fire.sequence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClipRowCopySourceResolverTest {

    @Test
    void prefersExplicitSelection() {
        assertEquals(3, ClipRowCopySourceResolver.resolve(3,
                new boolean[]{false, true, false, false},
                new boolean[]{false, false, false, false}));
    }

    @Test
    void fallsBackToPlayingClipWhenNothingSelected() {
        assertEquals(1, ClipRowCopySourceResolver.resolve(-1,
                new boolean[]{false, true, false, false},
                new boolean[]{false, false, true, false}));
    }

    @Test
    void fallsBackToRecordingClipWhenNothingSelectedOrPlaying() {
        assertEquals(2, ClipRowCopySourceResolver.resolve(-1,
                new boolean[]{false, false, false, false},
                new boolean[]{false, false, true, false}));
    }

    @Test
    void returnsMissingWhenNoUsableSourceExists() {
        assertEquals(-1, ClipRowCopySourceResolver.resolve(-1,
                new boolean[]{false, false, false, false},
                new boolean[]{false, false, false, false}));
    }
}
