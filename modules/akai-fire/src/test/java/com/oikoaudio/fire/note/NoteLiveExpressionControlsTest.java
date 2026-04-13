package com.oikoaudio.fire.note;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteLiveExpressionControlsTest {

    @Test
    void adjustPressureSendsChannelAftertouch() {
        final List<String> events = new ArrayList<>();
        final NoteLiveExpressionControls controls = new NoteLiveExpressionControls(new TestMidi(events));

        assertTrue(controls.adjustPressure(7));
        assertEquals(7, controls.pressure());
        assertEquals(List.of("pressure:7"), events);
    }

    @Test
    void adjustTimbreAndModulationClampToMidiRange() {
        final List<String> events = new ArrayList<>();
        final NoteLiveExpressionControls controls = new NoteLiveExpressionControls(new TestMidi(events));

        assertTrue(controls.adjustTimbre(100));
        assertTrue(controls.adjustModulation(200));

        assertEquals(127, controls.timbre());
        assertEquals(127, controls.modulation());
        assertEquals(List.of("timbre:127", "mod:127"), events);
    }

    @Test
    void adjustPitchExpressionSendsPitchBend() {
        final List<String> events = new ArrayList<>();
        final NoteLiveExpressionControls controls = new NoteLiveExpressionControls(new TestMidi(events));

        assertTrue(controls.adjustPitchExpression(10));
        assertEquals(74, controls.pitchExpression());
        assertEquals(List.of("bend:" + NoteLiveExpressionControls.pitchBendValueFor(74)), events);
    }

    @Test
    void resetMethodsRestoreDefaults() {
        final List<String> events = new ArrayList<>();
        final NoteLiveExpressionControls controls = new NoteLiveExpressionControls(new TestMidi(events));
        controls.adjustPressure(10);
        controls.adjustTimbre(5);
        controls.adjustModulation(9);
        controls.adjustPitchExpression(8);
        events.clear();

        controls.resetPressure();
        controls.resetTimbre();
        controls.resetModulation();
        controls.resetPitchExpression();

        assertEquals(List.of(
                "pressure:0",
                "timbre:64",
                "mod:0",
                "bend:" + NoteLiveExpressionControls.pitchBendValueFor(64)), events);
    }

    @Test
    void noChangeReturnsFalse() {
        final NoteLiveExpressionControls controls =
                new NoteLiveExpressionControls(new TestMidi(new ArrayList<>()));

        assertFalse(controls.adjustPressure(0));
    }

    private record TestMidi(List<String> events) implements NoteLiveExpressionControls.MidiExpressionOut {
        @Override
        public void channelAftertouch(final int value) {
            events.add("pressure:" + value);
        }

        @Override
        public void modulation(final int value) {
            events.add("mod:" + value);
        }

        @Override
        public void timbre(final int value) {
            events.add("timbre:" + value);
        }

        @Override
        public void pitchBend(final int bend) {
            events.add("bend:" + bend);
        }
    }
}
