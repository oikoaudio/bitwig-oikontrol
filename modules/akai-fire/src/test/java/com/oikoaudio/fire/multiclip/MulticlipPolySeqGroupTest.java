package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MulticlipPolySeqGroupTest {
    @Test
    void matchesPolySeqNamesWithoutDependingOnCaseOrSeparators() {
        assertTrue(MulticlipPolySeqGroup.matches("PolySeq"));
        assertTrue(MulticlipPolySeqGroup.matches("POLY SEQ drums"));
        assertTrue(MulticlipPolySeqGroup.matches("[poly-seq] percussion"));
        assertTrue(MulticlipPolySeqGroup.matches("my_poly_seq_grid"));
    }

    @Test
    void rejectsUnmarkedGroups() {
        assertFalse(MulticlipPolySeqGroup.matches("Drums"));
        assertFalse(MulticlipPolySeqGroup.matches("Polyphonic sequence"));
        assertFalse(MulticlipPolySeqGroup.matches(""));
    }
}
