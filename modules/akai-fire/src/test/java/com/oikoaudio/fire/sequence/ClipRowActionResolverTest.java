package com.oikoaudio.fire.sequence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClipRowActionResolverTest {

    @Test
    void ignoresReleasedPads() {
        assertEquals(ClipRowActionResolver.Action.IGNORE,
                ClipRowActionResolver.resolve(false, true, false, false, false, false, -1, 0));
    }

    @Test
    void launchesExistingClipByDefault() {
        assertEquals(ClipRowActionResolver.Action.SELECT_AND_LAUNCH,
                ClipRowActionResolver.resolve(true, true, false, false, false, false, -1, 0));
    }

    @Test
    void createsEmptyClipOnEmptySlotByDefault() {
        assertEquals(ClipRowActionResolver.Action.CREATE_EMPTY,
                ClipRowActionResolver.resolve(true, false, false, false, false, false, -1, 0));
    }

    @Test
    void selectsWithoutLaunchWhenSelectHeld() {
        assertEquals(ClipRowActionResolver.Action.SELECT_ONLY,
                ClipRowActionResolver.resolve(true, true, false, false, true, false, -1, 0));
    }

    @Test
    void clearsStepsWhenDeleteHeld() {
        assertEquals(ClipRowActionResolver.Action.CLEAR_STEPS,
                ClipRowActionResolver.resolve(true, true, true, false, false, false, -1, 0));
    }

    @Test
    void deletesObjectWhenShiftDeleteHeld() {
        assertEquals(ClipRowActionResolver.Action.DELETE_OBJECT,
                ClipRowActionResolver.resolve(true, true, true, false, false, true, -1, 0));
    }

    @Test
    void copiesIntoDifferentExistingSlot() {
        assertEquals(ClipRowActionResolver.Action.COPY_TO_TARGET,
                ClipRowActionResolver.resolve(true, true, false, true, false, false, 1, 2));
    }

    @Test
    void copiesIntoDifferentEmptySlot() {
        assertEquals(ClipRowActionResolver.Action.COPY_TO_TARGET,
                ClipRowActionResolver.resolve(true, false, false, true, false, false, 1, 2));
    }

    @Test
    void ignoresCopyWhenNoSourceSelected() {
        assertEquals(ClipRowActionResolver.Action.IGNORE,
                ClipRowActionResolver.resolve(true, true, false, true, false, false, -1, 2));
    }

    @Test
    void ignoresCopyToSameSlot() {
        assertEquals(ClipRowActionResolver.Action.IGNORE,
                ClipRowActionResolver.resolve(true, true, false, true, false, false, 2, 2));
    }

    @Test
    void cyclesColorOnShiftPress() {
        assertEquals(ClipRowActionResolver.Action.CYCLE_COLOR,
                ClipRowActionResolver.resolve(true, true, false, false, false, true, -1, 0));
    }
}
