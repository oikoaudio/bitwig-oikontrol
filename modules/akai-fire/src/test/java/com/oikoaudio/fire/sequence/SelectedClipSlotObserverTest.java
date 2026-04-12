package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import org.junit.jupiter.api.Test;
import com.bitwig.extension.controller.api.SettableColorValue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SelectedClipSlotObserverTest {

    @Test
    void observeMinimalWiresExistenceContentAndSelection() {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        final ClipLauncherSlot slot = mock(ClipLauncherSlot.class);
        final BooleanValue exists = mock(BooleanValue.class);
        final BooleanValue hasContent = mock(BooleanValue.class);
        final BooleanValue isSelected = mock(BooleanValue.class);
        final Runnable refresh = mock(Runnable.class);

        stubSlot(bank, slot, exists, hasContent, isSelected, null, null, null);

        SelectedClipSlotObserver.observe(bank, false, false, refresh);

        verify(exists).markInterested();
        verify(hasContent).markInterested();
        verify(isSelected).markInterested();
        verify(exists).addValueObserver(any());
        verify(hasContent).addValueObserver(any());
        verify(isSelected).addValueObserver(any());
        verify(refresh).run();
    }

    @Test
    void observeExtendedAlsoWiresColorPlayingAndRecording() {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        final ClipLauncherSlot slot = mock(ClipLauncherSlot.class);
        final BooleanValue exists = mock(BooleanValue.class);
        final BooleanValue hasContent = mock(BooleanValue.class);
        final BooleanValue isSelected = mock(BooleanValue.class);
        final SettableColorValue color = mock(SettableColorValue.class);
        final BooleanValue isPlaying = mock(BooleanValue.class);
        final BooleanValue isRecording = mock(BooleanValue.class);
        final Runnable refresh = mock(Runnable.class);

        stubSlot(bank, slot, exists, hasContent, isSelected, color, isPlaying, isRecording);

        SelectedClipSlotObserver.observe(bank, true, true, refresh);

        verify(color).markInterested();
        verify(color).addValueObserver(any());
        verify(isPlaying).markInterested();
        verify(isPlaying).addValueObserver(any());
        verify(isRecording).markInterested();
        verify(isRecording).addValueObserver(any());
        verify(refresh, times(1)).run();
    }

    private static void stubSlot(final ClipLauncherSlotBank bank,
                                 final ClipLauncherSlot slot,
                                 final BooleanValue exists,
                                 final BooleanValue hasContent,
                                 final BooleanValue isSelected,
                                 final SettableColorValue color,
                                 final BooleanValue isPlaying,
                                 final BooleanValue isRecording) {
        org.mockito.Mockito.when(bank.getSizeOfBank()).thenReturn(1);
        org.mockito.Mockito.when(bank.getItemAt(0)).thenReturn(slot);
        org.mockito.Mockito.when(slot.exists()).thenReturn(exists);
        org.mockito.Mockito.when(slot.hasContent()).thenReturn(hasContent);
        org.mockito.Mockito.when(slot.isSelected()).thenReturn(isSelected);
        if (color != null) {
            org.mockito.Mockito.when(slot.color()).thenReturn(color);
        }
        if (isPlaying != null) {
            org.mockito.Mockito.when(slot.isPlaying()).thenReturn(isPlaying);
        }
        if (isRecording != null) {
            org.mockito.Mockito.when(slot.isRecording()).thenReturn(isRecording);
        }
    }
}
