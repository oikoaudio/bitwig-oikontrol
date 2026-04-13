package com.oikoaudio.fire.sequence;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.SettableColorValue;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.lights.RgbLigthState;
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
        final SettableColorValue selectedColor = mockColorValue(1.0, 0.0, 0.0);
        stubSlot(firstSlot, false, false, false, null);
        stubSlot(selectedSlot, true, true, true, selectedColor);
        when(bank.getSizeOfBank()).thenReturn(2);
        when(bank.getItemAt(0)).thenReturn(firstSlot);
        when(bank.getItemAt(1)).thenReturn(selectedSlot);

        final SelectedClipSlotState state = SelectedClipSlotState.scan(bank, RgbLigthState.GRAY_1);

        assertTrue(state.hasSelection());
        assertEquals(1, state.slotIndex());
        assertTrue(state.hasContent());
        assertEquals(ColorLookup.getColor(selectedColor.get()), state.color());
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
                                 final SettableColorValue colorValue) {
        final BooleanValue exists = mock(BooleanValue.class);
        final BooleanValue hasContent = mock(BooleanValue.class);
        final BooleanValue isSelected = mock(BooleanValue.class);
        when(exists.get()).thenReturn(existsValue);
        when(hasContent.get()).thenReturn(hasContentValue);
        when(isSelected.get()).thenReturn(selectedValue);
        when(slot.exists()).thenReturn(exists);
        when(slot.hasContent()).thenReturn(hasContent);
        when(slot.isSelected()).thenReturn(isSelected);
        if (colorValue != null) {
            when(slot.color()).thenReturn(colorValue);
        }
    }

    private static SettableColorValue mockColorValue(final double red, final double green, final double blue) {
        final SettableColorValue colorValue = mock(SettableColorValue.class);
        final Color color = mock(Color.class);
        when(color.getRed255()).thenReturn((int) (red * 255.0));
        when(color.getGreen255()).thenReturn((int) (green * 255.0));
        when(color.getBlue255()).thenReturn((int) (blue * 255.0));
        when(colorValue.get()).thenReturn(color);
        return colorValue;
    }
}
