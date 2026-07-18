package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PendingStepCreationTest {
    @Test
    void completesOnlyAgainstTheExactLaneSceneAndGeneration() {
        final MulticlipTargetIdentity target = new MulticlipTargetIdentity(7, 5, 12);
        final PendingStepCreation request = new PendingStepCreation(target, 2, 9, 96);

        assertTrue(request.matches(new MulticlipTargetIdentity(7, 5, 12)));
        assertFalse(request.matches(new MulticlipTargetIdentity(8, 5, 12)));
        assertFalse(request.matches(new MulticlipTargetIdentity(7, 6, 12)));
        assertFalse(request.matches(new MulticlipTargetIdentity(7, 5, 13)));
    }
}
