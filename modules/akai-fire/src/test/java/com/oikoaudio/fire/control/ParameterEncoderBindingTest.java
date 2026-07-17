package com.oikoaudio.fire.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParameterEncoderBindingTest {
    @Test
    void explicitResetTouchRunsResetAndConsumesModifier() {
        final TestExplicitResetControl resetControl = new TestExplicitResetControl(true);
        final List<String> events = new ArrayList<>();

        final boolean handled =
                ParameterEncoderBinding.handleExplicitResetTouch(
                        true,
                        resetControl,
                        true,
                        "Pan",
                        "No reset",
                        () -> events.add("reset"),
                        () -> events.add("show"),
                        (title, value) -> events.add(title + ":" + value));

        assertTrue(handled);
        assertEquals(List.of("consume", "reset", "show"), eventsWithConsume(resetControl, events));
    }

    @Test
    void explicitResetTouchReportsUnavailableTarget() {
        final TestExplicitResetControl resetControl = new TestExplicitResetControl(true);
        final List<String> events = new ArrayList<>();

        final boolean handled =
                ParameterEncoderBinding.handleExplicitResetTouch(
                        true,
                        resetControl,
                        false,
                        "Volume",
                        "No reset",
                        () -> events.add("reset"),
                        () -> events.add("show"),
                        (title, value) -> events.add(title + ":" + value));

        assertTrue(handled);
        assertEquals(
                List.of("consume", "Volume:No reset"), eventsWithConsume(resetControl, events));
    }

    @Test
    void explicitResetTouchIgnoresPlainEncoderTouch() {
        final TestExplicitResetControl resetControl = new TestExplicitResetControl(false);
        final List<String> events = new ArrayList<>();

        final boolean handled =
                ParameterEncoderBinding.handleExplicitResetTouch(
                        true,
                        resetControl,
                        true,
                        "Pan",
                        "No reset",
                        () -> events.add("reset"),
                        () -> events.add("show"),
                        (title, value) -> events.add(title + ":" + value));

        assertFalse(handled);
        assertEquals(List.of(), eventsWithConsume(resetControl, events));
    }

    private static List<String> eventsWithConsume(
            final TestExplicitResetControl resetControl, final List<String> events) {
        final List<String> result = new ArrayList<>();
        if (resetControl.consumed()) {
            result.add("consume");
        }
        result.addAll(events);
        return result;
    }

    private static final class TestExplicitResetControl
            implements ParameterEncoderBinding.ExplicitResetControl {
        private final boolean held;
        private boolean consumed;

        private TestExplicitResetControl(final boolean held) {
            this.held = held;
        }

        @Override
        public boolean isHeld() {
            return held;
        }

        @Override
        public void consume() {
            consumed = true;
        }

        private boolean consumed() {
            return consumed;
        }
    }
}
