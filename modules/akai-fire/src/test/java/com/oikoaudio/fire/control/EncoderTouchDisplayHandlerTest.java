package com.oikoaudio.fire.control;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EncoderTouchDisplayHandlerTest {
    @Test
    void touchShowsImmediately() {
        final List<String> events = new ArrayList<>();
        final EncoderTouchDisplayHandler handler =
                new EncoderTouchDisplayHandler(() -> events.add("clear"));

        handler.handleTouch(true, () -> events.add("show"));

        assertEquals(List.of("show"), events);
    }

    @Test
    void touchReleaseClearsDisplay() {
        final List<String> events = new ArrayList<>();
        final EncoderTouchDisplayHandler handler =
                new EncoderTouchDisplayHandler(() -> events.add("clear"));

        handler.handleTouch(true, () -> events.add("show"));
        handler.handleTouch(false, () -> events.add("show"));

        assertEquals(List.of("show", "clear"), events);
    }
}
