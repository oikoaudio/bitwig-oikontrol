package com.oikoaudio.fire.melodic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MelodicStepClipWriterTest {
    @Test
    void remembersOnlyWritableActiveUntiedSteps() {
        final MelodicStepClipWriter writer = new MelodicStepClipWriter();
        final MelodicPattern.Step active = activeStep(2, 64);
        MelodicPattern pattern = MelodicPattern.empty(16)
                .withStep(active)
                .withStep(activeStep(3, 65).withTieFromPrevious(true))
                .withStep(MelodicPattern.Step.rest(4).withActive(true));

        writer.rememberPendingWrite(12, activeStep(12, 72));
        writer.rememberPendingWrites(pattern);

        assertEquals(1, writer.pendingWriteCount());
        assertSame(active, writer.pendingStepAt(2));
        assertNull(writer.pendingStepAt(3));
        assertNull(writer.pendingStepAt(4));
        assertNull(writer.pendingStepAt(12));
    }

    @Test
    void clearsIndividualPendingWrites() {
        final MelodicStepClipWriter writer = new MelodicStepClipWriter();

        writer.rememberPendingWrite(5, activeStep(5, 67));
        writer.clearPendingWrite(5);

        assertNull(writer.pendingStepAt(5));
    }

    private static MelodicPattern.Step activeStep(final int index, final int pitch) {
        return new MelodicPattern.Step(index, true, false, pitch, 96, 0.8, false, false);
    }
}
