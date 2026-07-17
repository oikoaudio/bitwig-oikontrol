package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SelectedNoteClipCoordinatorTest {
    @Test
    void composesAvailabilityStateAndCursorRefresh() {
        final Fixture fixture = new Fixture();
        final AtomicInteger resets = new AtomicInteger();
        final AtomicInteger failures = new AtomicInteger();
        final SelectedNoteClipCoordinator coordinator = fixture.coordinator(failures, resets);

        assertTrue(coordinator.ensureAvailable());
        assertEquals(0, coordinator.state().slotIndex());
        assertEquals(1, resets.get());

        fixture.selected.set(false);
        assertFalse(coordinator.ensureAvailable());
        assertEquals(1, failures.get());
    }

    @Test
    void switchingSlotsCancelsDelayedRefreshCompletion() {
        final Fixture fixture = new Fixture();
        final AtomicInteger completions = new AtomicInteger();
        final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        final SelectedNoteClipCoordinator coordinator =
                fixture.coordinator(new AtomicInteger(), new AtomicInteger());
        coordinator.refreshState();

        coordinator.scheduleRefresh(
                (task, delay) -> tasks.add(task), 150, completions::incrementAndGet);
        fixture.selected.set(false);
        coordinator.refreshState();
        tasks.remove().run();

        assertEquals(0, completions.get());
    }

    private static final class Fixture {
        private final AtomicBoolean selected = new AtomicBoolean(true);
        private final ClipLauncherSlotBank bank = mock(ClipLauncherSlotBank.class);

        Fixture() {
            final ClipLauncherSlot slot = mock(ClipLauncherSlot.class);
            final BooleanValue exists = booleanValue(() -> true);
            final BooleanValue hasContent = booleanValue(() -> true);
            final BooleanValue isSelected = booleanValue(selected::get);
            when(slot.exists()).thenReturn(exists);
            when(slot.hasContent()).thenReturn(hasContent);
            when(slot.isSelected()).thenReturn(isSelected);
            when(bank.getSizeOfBank()).thenReturn(1);
            when(bank.getItemAt(0)).thenReturn(slot);
        }

        SelectedNoteClipCoordinator coordinator(
                final AtomicInteger failures, final AtomicInteger resets) {
            return new SelectedNoteClipCoordinator(
                    bank,
                    null,
                    () -> true,
                    () -> 0,
                    ignored -> failures.incrementAndGet(),
                    ignored -> {},
                    resets::incrementAndGet);
        }

        private static BooleanValue booleanValue(
                final java.util.function.BooleanSupplier supplier) {
            final BooleanValue value = mock(BooleanValue.class);
            when(value.get()).thenAnswer(ignored -> supplier.getAsBoolean());
            return value;
        }
    }
}
