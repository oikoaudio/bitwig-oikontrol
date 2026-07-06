package com.oikoaudio.fire.control;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncoderTouchDisplayHandlerTest {
    @Test
    void touchShowsImmediately() {
        final List<String> events = new ArrayList<>();
        final EncoderTouchDisplayHandler handler = new EncoderTouchDisplayHandler(() -> events.add("clear"));

        handler.handleTouch(true, () -> events.add("show"));

        assertEquals(List.of("show"), events);
    }

    @Test
    void touchReleaseClearsDisplay() {
        final List<String> events = new ArrayList<>();
        final EncoderTouchDisplayHandler handler = new EncoderTouchDisplayHandler(() -> events.add("clear"));

        handler.handleTouch(true, () -> events.add("show"));
        handler.handleTouch(false, () -> events.add("show"));

        assertEquals(List.of("show", "clear"), events);
    }
}
