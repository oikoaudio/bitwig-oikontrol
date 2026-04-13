package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.testutil.BitwigApiValueStubs.BooleanValueStub;
import com.oikoaudio.fire.testutil.BitwigApiValueStubs.ColorValueStub;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SelectedClipSlotStateTest {

    @Test
    void scanReturnsSelectedSlotContentAndConvertedColor() {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        final ClipLauncherSlot firstSlot = mock(ClipLauncherSlot.class);
        final ClipLauncherSlot selectedSlot = mock(ClipLauncherSlot.class);
        final ColorValueStub selectedColor = new ColorValueStub(255, 0, 0);
        stubSlot(firstSlot, false, false, false, null);
        stubSlot(selectedSlot, true, true, true, selectedColor);
        when(bank.getSizeOfBank()).thenReturn(2);
        when(bank.getItemAt(0)).thenReturn(firstSlot);
        when(bank.getItemAt(1)).thenReturn(selectedSlot);

        final SelectedClipSlotState state = SelectedClipSlotState.scan(bank, RgbLigthState.GRAY_1);

        assertTrue(state.hasSelection());
        assertEquals(1, state.slotIndex());
        assertTrue(state.hasContent());
        assertEquals(ColorLookup.getColor(selectedColor.color()), state.color());
    }

    @Test
    void scanFallsBackToDefaultColorWhenNoSlotIsSelected() {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        final ClipLauncherSlot slot = mock(ClipLauncherSlot.class);
        stubSlot(slot, true, true, false, null);
        when(bank.getSizeOfBank()).thenReturn(1);
        when(bank.getItemAt(0)).thenReturn(slot);

        final SelectedClipSlotState state = SelectedClipSlotState.scan(bank, RgbLigthState.GRAY_2);

        assertFalse(state.hasSelection());
        assertEquals(-1, state.slotIndex());
        assertFalse(state.hasContent());
        assertSame(RgbLigthState.GRAY_2, state.color());
    }

    @Test
    void scanSupportsContentBookkeepingWithoutColorTracking() {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        final ClipLauncherSlot slot = mock(ClipLauncherSlot.class);
        stubSlot(slot, true, true, true, null);
        when(bank.getSizeOfBank()).thenReturn(1);
        when(bank.getItemAt(0)).thenReturn(slot);

        final SelectedClipSlotState state = SelectedClipSlotState.scan(bank, null);

        assertTrue(state.hasSelection());
        assertEquals(0, state.slotIndex());
        assertTrue(state.hasContent());
        assertNull(state.color());
    }

    private static void stubSlot(final ClipLauncherSlot slot,
                                 final boolean existsValue,
                                 final boolean hasContentValue,
                                 final boolean selectedValue,
                                 final ColorValueStub colorValue) {
        when(slot.exists()).thenReturn(new BooleanValueStub(existsValue).value());
        when(slot.hasContent()).thenReturn(new BooleanValueStub(hasContentValue).value());
        when(slot.isSelected()).thenReturn(new BooleanValueStub(selectedValue).value());
        if (colorValue != null) {
            when(slot.color()).thenReturn(colorValue.value());
        }
    }
}
