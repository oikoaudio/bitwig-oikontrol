package com.oikoaudio.fire.perform;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.oikoaudio.fire.testutil.BitwigApiValueStubs.BooleanValueStub;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LauncherSlotRecorderTest {

    @Test
    void firstFreeSlotIndexSkipsOccupiedAndRecordingSlots() {
        final ClipLauncherSlotBank bank = bank(
                slot(true, true, false, false, false, false),
                slot(true, false, true, false, false, false),
                slot(true, false, false, true, false, false),
                slot(true, false, false, false, false, false));

        assertEquals(3, LauncherSlotRecorder.firstFreeSlotIndex(bank));
    }

    @Test
    void firstFreeSlotIndexReturnsMinusOneWhenNoFreeSlotExists() {
        final ClipLauncherSlotBank bank = bank(
                slot(false, false, false, false, false, false),
                slot(true, true, false, false, false, false));

        assertEquals(-1, LauncherSlotRecorder.firstFreeSlotIndex(bank));
    }

    @Test
    void hasLauncherActivityDetectsPlayingQueuedAndRecordingSlots() {
        assertFalse(LauncherSlotRecorder.hasLauncherActivity(bank(
                slot(true, false, false, false, false, false))));
        assertTrue(LauncherSlotRecorder.hasLauncherActivity(bank(
                slot(true, false, false, false, true, false))));
        assertTrue(LauncherSlotRecorder.hasLauncherActivity(bank(
                slot(true, false, false, false, false, true))));
        assertTrue(LauncherSlotRecorder.hasLauncherActivity(bank(
                slot(true, false, true, false, false, false))));
    }

    private static ClipLauncherSlotBank bank(final ClipLauncherSlot... slots) {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        when(bank.getSizeOfBank()).thenReturn(slots.length);
        for (int index = 0; index < slots.length; index++) {
            when(bank.getItemAt(index)).thenReturn(slots[index]);
        }
        return bank;
    }

    private static ClipLauncherSlot slot(final boolean exists,
                                         final boolean hasContent,
                                         final boolean recording,
                                         final boolean recordingQueued,
                                         final boolean playing,
                                         final boolean playbackQueued) {
        final ClipLauncherSlot slot = mock(ClipLauncherSlot.class);
        when(slot.exists()).thenReturn(new BooleanValueStub(exists).value());
        when(slot.hasContent()).thenReturn(new BooleanValueStub(hasContent).value());
        when(slot.isRecording()).thenReturn(new BooleanValueStub(recording).value());
        when(slot.isRecordingQueued()).thenReturn(new BooleanValueStub(recordingQueued).value());
        when(slot.isPlaying()).thenReturn(new BooleanValueStub(playing).value());
        when(slot.isPlaybackQueued()).thenReturn(new BooleanValueStub(playbackQueued).value());
        return slot;
    }
}
