package com.oikoaudio.fire.sequence;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.SettableColorValue;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.testutil.BitwigApiValueStubs.BooleanValueStub;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClipRowHandlerTest {
    @Test
    void existingClipPadLaunchesFromStart() {
        final ClipLauncherSlot slot = slot(true);
        final ClipRowHandler handler = new ClipRowHandler(new Host(bank(slot)));

        handler.handlePadPress(0, true);

        verify(slot).select();
        verify(slot).launchWithOptions("default", "from_start");
        verify(slot, never()).launch();
    }

    private static ClipLauncherSlotBank bank(final ClipLauncherSlot firstSlot) {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        when(bank.getItemAt(0)).thenReturn(firstSlot);
        for (int index = 1; index < 16; index++) {
            final ClipLauncherSlot emptySlot = slot(false);
            when(bank.getItemAt(index)).thenReturn(emptySlot);
        }
        return bank;
    }

    private static ClipLauncherSlot slot(final boolean hasContent) {
        final ClipLauncherSlot slot = mock(ClipLauncherSlot.class);
        final SettableColorValue color = mock(SettableColorValue.class);
        final Color slotColor = Color.fromRGB255(32, 64, 96);
        doReturn(slotColor).when(color).get();
        doReturn(color).when(slot).color();
        final BooleanValue exists = new BooleanValueStub(true).value();
        final BooleanValue content = new BooleanValueStub(hasContent).value();
        final BooleanValue off = new BooleanValueStub(false).value();
        when(slot.exists()).thenReturn(exists);
        when(slot.hasContent()).thenReturn(content);
        when(slot.isPlaybackQueued()).thenReturn(off);
        when(slot.isPlaying()).thenReturn(off);
        when(slot.isRecording()).thenReturn(off);
        when(slot.isRecordingQueued()).thenReturn(off);
        when(slot.isSelected()).thenReturn(off);
        when(slot.isStopQueued()).thenReturn(off);
        return slot;
    }

    private record Host(ClipLauncherSlotBank slotBank) implements SeqClipRowHost {
        @Override
        public AkaiFireOikontrolExtension getDriver() {
            return null;
        }

        @Override
        public OledDisplay getOled() {
            return null;
        }

        @Override
        public ClipLauncherSlotBank getClipSlotBank() {
            return slotBank;
        }

        @Override
        public PinnableCursorClip getClipCursor() {
            return mock(PinnableCursorClip.class);
        }

        @Override
        public boolean isSelectHeld() {
            return false;
        }

        @Override
        public boolean isCopyHeld() {
            return false;
        }

        @Override
        public boolean isDeleteHeld() {
            return false;
        }

        @Override
        public boolean isShiftHeld() {
            return false;
        }

        @Override
        public void notifyPopup(final String title, final String value) {
        }
    }
}
