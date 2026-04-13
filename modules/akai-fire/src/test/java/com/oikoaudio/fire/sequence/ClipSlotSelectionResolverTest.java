package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.oikoaudio.fire.testutil.BitwigApiValueStubs.BooleanValueStub;
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
        when(slot.isPlaying()).thenReturn(new BooleanValueStub(playing).value());
        when(slot.isRecording()).thenReturn(new BooleanValueStub(recording).value());
        when(slot.isSelected()).thenReturn(new BooleanValueStub(selected).value());
        when(slot.exists()).thenReturn(new BooleanValueStub(existsValue).value());
        return slot;
    }
}
