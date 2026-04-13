package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.oikoaudio.fire.testutil.BitwigApiValueStubs.BooleanValueStub;
import com.oikoaudio.fire.testutil.BitwigApiValueStubs.ColorValueStub;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SelectedClipSlotObserverTest {

    @Test
    void observeMinimalWiresExistenceContentAndSelection() {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        final ClipLauncherSlot slot = mock(ClipLauncherSlot.class);
        final BooleanValueStub exists = new BooleanValueStub(false);
        final BooleanValueStub hasContent = new BooleanValueStub(false);
        final BooleanValueStub isSelected = new BooleanValueStub(false);

        stubSlot(bank, slot, exists, hasContent, isSelected, null, null, null);

        SelectedClipSlotObserver.observe(bank, false, false, () -> { });

        assertEquals(1, exists.markInterestedCalls());
        assertEquals(1, hasContent.markInterestedCalls());
        assertEquals(1, isSelected.markInterestedCalls());
        assertEquals(1, exists.addObserverCalls());
        assertEquals(1, hasContent.addObserverCalls());
        assertEquals(1, isSelected.addObserverCalls());
    }

    @Test
    void observeExtendedAlsoWiresColorPlayingAndRecording() {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        final ClipLauncherSlot slot = mock(ClipLauncherSlot.class);
        final BooleanValueStub exists = new BooleanValueStub(false);
        final BooleanValueStub hasContent = new BooleanValueStub(false);
        final BooleanValueStub isSelected = new BooleanValueStub(false);
        final ColorValueStub color = new ColorValueStub(12, 34, 56);
        final BooleanValueStub isPlaying = new BooleanValueStub(false);
        final BooleanValueStub isRecording = new BooleanValueStub(false);

        stubSlot(bank, slot, exists, hasContent, isSelected, color, isPlaying, isRecording);

        SelectedClipSlotObserver.observe(bank, true, true, () -> { });

        assertEquals(1, color.markInterestedCalls());
        assertEquals(1, color.addObserverCalls());
        assertEquals(1, isPlaying.markInterestedCalls());
        assertEquals(1, isPlaying.addObserverCalls());
        assertEquals(1, isRecording.markInterestedCalls());
        assertEquals(1, isRecording.addObserverCalls());
    }

    private static void stubSlot(final ClipLauncherSlotBank bank,
                                 final ClipLauncherSlot slot,
                                 final BooleanValueStub exists,
                                 final BooleanValueStub hasContent,
                                 final BooleanValueStub isSelected,
                                 final ColorValueStub color,
                                 final BooleanValueStub isPlaying,
                                 final BooleanValueStub isRecording) {
        org.mockito.Mockito.when(bank.getSizeOfBank()).thenReturn(1);
        org.mockito.Mockito.when(bank.getItemAt(0)).thenReturn(slot);
        org.mockito.Mockito.when(slot.exists()).thenReturn(exists.value());
        org.mockito.Mockito.when(slot.hasContent()).thenReturn(hasContent.value());
        org.mockito.Mockito.when(slot.isSelected()).thenReturn(isSelected.value());
        if (color != null) {
            org.mockito.Mockito.when(slot.color()).thenReturn(color.value());
        }
        if (isPlaying != null) {
            org.mockito.Mockito.when(slot.isPlaying()).thenReturn(isPlaying.value());
        }
        if (isRecording != null) {
            org.mockito.Mockito.when(slot.isRecording()).thenReturn(isRecording.value());
        }
    }
}
