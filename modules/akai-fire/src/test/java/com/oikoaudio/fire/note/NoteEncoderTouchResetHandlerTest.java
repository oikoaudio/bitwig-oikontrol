package com.oikoaudio.fire.note;

import com.oikoaudio.fire.control.TouchResetGesture;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoteEncoderTouchResetHandlerTest {

    @Test
    void touchShowsImmediatelyAndResetsAfterHoldWhenStillTouched() {
        final List<String> events = new ArrayList<>();
        Runnable[] scheduledTask = new Runnable[1];
        final NoteEncoderTouchResetHandler handler = new NoteEncoderTouchResetHandler(
                new TouchResetGesture(4, 0L, 300L, 2),
                () -> true,
                (task, delayMs) -> {
                    events.add("schedule:" + delayMs);
                    scheduledTask[0] = task;
                },
                0L,
                () -> events.add("clear"));

        handler.handleResettableTouch(1, true, () -> events.add("show"), () -> events.add("reset"));
        scheduledTask[0].run();

        assertEquals(List.of("schedule:0", "show", "reset", "show"), events);
    }

    @Test
    void touchReleaseClearsDisplayAndCancelsResetEffect() {
        final List<String> events = new ArrayList<>();
        Runnable[] scheduledTask = new Runnable[1];
        final NoteEncoderTouchResetHandler handler = new NoteEncoderTouchResetHandler(
                new TouchResetGesture(4, 1000L, 300L, 2),
                () -> true,
                (task, delayMs) -> scheduledTask[0] = task,
                1000L,
                () -> events.add("clear"));

        handler.handleResettableTouch(2, true, () -> events.add("show"), () -> events.add("reset"));
        handler.handleResettableTouch(2, false, () -> events.add("show"), () -> events.add("reset"));
        scheduledTask[0].run();

        assertEquals(List.of("show", "clear"), events);
    }

    @Test
    void recentAdjustmentSuppressesTouchHoldReset() {
        final List<String> events = new ArrayList<>();
        Runnable[] scheduledTask = new Runnable[1];
        final NoteEncoderTouchResetHandler handler = new NoteEncoderTouchResetHandler(
                new TouchResetGesture(4, 1000L, 300L, 2),
                () -> true,
                (task, delayMs) -> scheduledTask[0] = task,
                1000L,
                () -> events.add("clear"));

        handler.handleResettableTouch(3, true, () -> events.add("show"), () -> events.add("reset"));
        handler.markAdjusted(3);
        scheduledTask[0].run();

        assertEquals(List.of("show"), events);
    }

    @Test
    void disabledResetStillShowsAndClearsWithoutScheduling() {
        final List<String> events = new ArrayList<>();
        final AtomicBoolean scheduled = new AtomicBoolean();
        final NoteEncoderTouchResetHandler handler = new NoteEncoderTouchResetHandler(
                new TouchResetGesture(4, 1000L, 300L, 2),
                () -> false,
                (task, delayMs) -> scheduled.set(true),
                1000L,
                () -> events.add("clear"));

        handler.handleResettableTouch(0, true, () -> events.add("show"), () -> events.add("reset"));
        handler.handleResettableTouch(0, false, () -> events.add("show"), () -> events.add("reset"));

        assertEquals(List.of("show", "clear"), events);
        assertEquals(false, scheduled.get());
    }
}
