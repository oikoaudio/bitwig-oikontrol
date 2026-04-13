package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClipSlotSelectionResolverTest {

    @Test
    void resolveUsesPreferredSelectedSlotWhenStillSelected() {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        final ClipLauncherSlot slot = slot(false, false, true, true);
        when(bank.getSizeOfBank()).thenReturn(1);
        when(bank.getItemAt(0)).thenReturn(slot);

        final boolean resolved = ClipSlotSelectionResolver.resolve(bank, 0, -1);

        assertTrue(resolved);
        verify(slot, never()).select();
    }

    @Test
    void resolveKeepsKnownSelectedStateWithoutSelectingPlayingSlot() {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        when(bank.getSizeOfBank()).thenReturn(0);

        final boolean resolved = ClipSlotSelectionResolver.resolve(bank, -1, 3);

        assertTrue(resolved);
    }

    @Test
    void resolveSelectsPlayingSlotWhenNothingElseIsSelected() {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        final ClipLauncherSlot slot = slot(true, false, false, true);
        when(bank.getSizeOfBank()).thenReturn(1);
        when(bank.getItemAt(0)).thenReturn(slot);

        final boolean resolved = ClipSlotSelectionResolver.resolve(bank, -1, -1);

        assertTrue(resolved);
        verify(slot).select();
    }

    @Test
    void resolveReturnsFalseWhenNoSelectedOrPlayingSlotExists() {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        final ClipLauncherSlot slot = slot(false, false, false, true);
        when(bank.getSizeOfBank()).thenReturn(1);
        when(bank.getItemAt(0)).thenReturn(slot);

        final boolean resolved = ClipSlotSelectionResolver.resolve(bank, -1, -1);

        assertFalse(resolved);
        verify(slot, never()).select();
    }

    private static ClipLauncherSlot slot(final boolean playing,
                                         final boolean recording,
                                         final boolean selected,
                                         final boolean existsValue) {
        final ClipLauncherSlot slot = mock(ClipLauncherSlot.class);
        final BooleanValue isPlaying = mock(BooleanValue.class);
        final BooleanValue isRecording = mock(BooleanValue.class);
        final BooleanValue isSelected = mock(BooleanValue.class);
        final BooleanValue exists = mock(BooleanValue.class);
        when(isPlaying.get()).thenReturn(playing);
        when(isRecording.get()).thenReturn(recording);
        when(isSelected.get()).thenReturn(selected);
        when(exists.get()).thenReturn(existsValue);
        when(slot.isPlaying()).thenReturn(isPlaying);
        when(slot.isRecording()).thenReturn(isRecording);
        when(slot.isSelected()).thenReturn(isSelected);
        when(slot.exists()).thenReturn(exists);
        return slot;
    }
}
