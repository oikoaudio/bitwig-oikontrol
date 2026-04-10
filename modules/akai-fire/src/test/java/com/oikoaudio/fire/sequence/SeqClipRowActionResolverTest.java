package com.oikoaudio.fire.sequence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeqClipRowActionResolverTest {

    @Test
    void ignoresReleasedPads() {
        assertEquals(SeqClipRowActionResolver.Action.IGNORE,
                SeqClipRowActionResolver.resolve(false, true, false, false, false, false, -1, 0));
    }

    @Test
    void launchesExistingClipByDefault() {
        assertEquals(SeqClipRowActionResolver.Action.SELECT_AND_LAUNCH,
                SeqClipRowActionResolver.resolve(true, true, false, false, false, false, -1, 0));
    }

    @Test
    void createsEmptyClipOnEmptySlotByDefault() {
        assertEquals(SeqClipRowActionResolver.Action.CREATE_EMPTY,
                SeqClipRowActionResolver.resolve(true, false, false, false, false, false, -1, 0));
    }

    @Test
    void selectsWithoutLaunchWhenSelectHeld() {
        assertEquals(SeqClipRowActionResolver.Action.SELECT_ONLY,
                SeqClipRowActionResolver.resolve(true, true, false, false, true, false, -1, 0));
    }

    @Test
    void clearsStepsWhenDeleteHeld() {
        assertEquals(SeqClipRowActionResolver.Action.CLEAR_STEPS,
                SeqClipRowActionResolver.resolve(true, true, true, false, false, false, -1, 0));
    }

    @Test
    void deletesObjectWhenShiftDeleteHeld() {
        assertEquals(SeqClipRowActionResolver.Action.DELETE_OBJECT,
                SeqClipRowActionResolver.resolve(true, true, true, false, false, true, -1, 0));
    }

    @Test
    void copiesIntoDifferentExistingSlot() {
        assertEquals(SeqClipRowActionResolver.Action.COPY_TO_TARGET,
                SeqClipRowActionResolver.resolve(true, true, false, true, false, false, 1, 2));
    }

    @Test
    void copiesIntoDifferentEmptySlot() {
        assertEquals(SeqClipRowActionResolver.Action.COPY_TO_TARGET,
                SeqClipRowActionResolver.resolve(true, false, false, true, false, false, 1, 2));
    }

    @Test
    void ignoresCopyWhenNoSourceSelected() {
        assertEquals(SeqClipRowActionResolver.Action.IGNORE,
                SeqClipRowActionResolver.resolve(true, true, false, true, false, false, -1, 2));
    }

    @Test
    void ignoresCopyToSameSlot() {
        assertEquals(SeqClipRowActionResolver.Action.IGNORE,
                SeqClipRowActionResolver.resolve(true, true, false, true, false, false, 2, 2));
    }

    @Test
    void cyclesColorOnShiftPress() {
        assertEquals(SeqClipRowActionResolver.Action.CYCLE_COLOR,
                SeqClipRowActionResolver.resolve(true, true, false, false, false, true, -1, 0));
    }
}
