package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoteClipCursorRefresherTest {

    @Test
    void refreshUsesUpdatedSelectedSlotStateBeforeResolving() {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        when(bank.getSizeOfBank()).thenReturn(0);
        final int[] selectedSlotIndex = {-1};
        final List<String> calls = new ArrayList<>();

        final boolean resolved =
                NoteClipCursorRefresher.refresh(
                        bank,
                        -1,
                        () -> {
                            calls.add("refresh");
                            selectedSlotIndex[0] = 3;
                        },
                        () -> selectedSlotIndex[0],
                        () -> calls.add("reset"));

        assertTrue(resolved);
        assertEquals(List.of("refresh", "reset"), calls);
    }

    @Test
    void refreshRunsCursorResetActionsEvenWhenSelectionCannotBeResolved() {
        final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);
        when(bank.getSizeOfBank()).thenReturn(0);
        final List<String> calls = new ArrayList<>();

        final boolean resolved =
                NoteClipCursorRefresher.refresh(
                        bank,
                        -1,
                        () -> calls.add("refresh"),
                        () -> -1,
                        () -> calls.add("reset-a"),
                        () -> calls.add("reset-b"));

        assertFalse(resolved);
        assertEquals(List.of("refresh", "reset-a", "reset-b"), calls);
    }
}
