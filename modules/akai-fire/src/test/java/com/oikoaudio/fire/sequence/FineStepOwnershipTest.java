package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FineStepOwnershipTest {
    @Test
    void retainsTheAnchorUntilTheMidpointThenTransfersToTheNextStep() {
        assertEquals(2, FineStepOwnership.ownerOf(32, 16, 8));
        assertEquals(2, FineStepOwnership.ownerOf(39, 16, 8));
        assertEquals(3, FineStepOwnership.ownerOf(40, 16, 8));
    }

    @Test
    void wrapsOwnershipAcrossTheEndOfTheLoop() {
        assertEquals(0, FineStepOwnership.ownerOf(127, 16, 8));
        assertEquals(0, FineStepOwnership.ownerOf(120, 16, 8));
        assertEquals(0, FineStepOwnership.ownerOf(-1, 16, 8));
    }

    @Test
    void everyFineStartHasExactlyOneOwner() {
        for (int fineStart = -16; fineStart < 144; fineStart++) {
            final int expectedOwner = FineStepOwnership.ownerOf(fineStart, 16, 8);
            int matches = 0;
            for (int step = 0; step < 8; step++) {
                if (FineStepOwnership.isOwnedBy(fineStart, step, 16, 8)) {
                    matches++;
                    assertEquals(expectedOwner, step);
                }
            }
            assertEquals(1, matches);
        }
    }

    @Test
    void rejectsInvalidGridDimensions() {
        assertTrue(assertThrowsIllegalArgument(() -> FineStepOwnership.ownerOf(0, 0, 8)));
        assertTrue(assertThrowsIllegalArgument(() -> FineStepOwnership.ownerOf(0, 16, 0)));
    }

    private static boolean assertThrowsIllegalArgument(final Runnable action) {
        try {
            action.run();
            return false;
        } catch (final IllegalArgumentException expected) {
            return true;
        }
    }
}
