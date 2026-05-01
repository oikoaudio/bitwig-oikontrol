package com.oikoaudio.fire.note;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteLivePadPerformerTest {

    @Test
    void handlePadPressSendsNoteOnAndOff() {
        final List<String> events = new ArrayList<>();
        final NoteLivePadPerformer performer = new NoteLivePadPerformer(
                new TestMidiOut(events),
                pad -> new int[]{60 + pad},
                (configured, raw) -> raw);

        performer.handlePadPress(2, true, 99, 100);
        performer.handlePadPress(2, false, 0, 100);

        assertEquals(List.of("on:62:99", "off:62"), events);
        assertFalse(performer.isPadHeld(2));
    }

    @Test
    void retriggeringPadSendsNoteOffBeforeNewNoteOn() {
        final List<String> events = new ArrayList<>();
        final NoteLivePadPerformer performer = new NoteLivePadPerformer(
                new TestMidiOut(events),
                pad -> new int[]{60 + pad},
                (configured, raw) -> raw);

        performer.handlePadPress(1, true, 80, 100);
        performer.handlePadPress(1, true, 96, 100);

        assertEquals(List.of("on:61:80", "off:61", "on:61:96"), events);
        assertTrue(performer.isPadHeld(1));
    }

    @Test
    void retuneHeldPadsRestartsOnlyCurrentlyHeldPads() {
        final List<String> events = new ArrayList<>();
        final NoteLivePadPerformer performer = new NoteLivePadPerformer(
                new TestMidiOut(events),
                pad -> new int[]{60 + pad},
                (configured, raw) -> configured);

        performer.handlePadPress(0, true, 90, 100);
        performer.handlePadPress(1, true, 91, 100);
        performer.handlePadPress(1, false, 0, 100);

        performer.retuneHeldPads(() -> {
        }, 77);

        assertEquals(List.of("on:60:100", "on:61:100", "off:61", "off:60", "on:60:77"), events);
    }

    @Test
    void releaseHeldNotesClearsHeldPadState() {
        final List<String> events = new ArrayList<>();
        final NoteLivePadPerformer performer = new NoteLivePadPerformer(
                new TestMidiOut(events),
                pad -> new int[]{60 + pad},
                (configured, raw) -> raw);

        performer.handlePadPress(3, true, 70, 100);

        performer.releaseHeldNotes();

        assertEquals(List.of("on:63:70", "off:63"), events);
        assertFalse(performer.isPadHeld(3));
    }

    @Test
    void handlePadPressSupportsMultipleNotesPerPad() {
        final List<String> events = new ArrayList<>();
        final NoteLivePadPerformer performer = new NoteLivePadPerformer(
                new TestMidiOut(events),
                pad -> new int[]{60 + pad, 64 + pad, 67 + pad},
                (configured, raw) -> configured);

        performer.handlePadPress(0, true, 99, 100);
        performer.handlePadPress(0, false, 0, 100);

        assertEquals(List.of("on:60:100", "on:64:100", "on:67:100", "off:60", "off:64", "off:67"), events);
    }

    @Test
    void handlePadPressCanSendPerNoteTimbreAfterNoteOn() {
        final List<String> events = new ArrayList<>();
        final NoteLivePadPerformer performer = new NoteLivePadPerformer(
                new TestMidiOut(events),
                pad -> new int[]{60 + pad},
                (pad, configured, raw) -> raw,
                pad -> 40 + pad);

        performer.handlePadPress(2, true, 99, 100);
        performer.handlePadPress(2, false, 0, 100);

        assertEquals(List.of("on:62:99", "timbre:62:42", "off:62"), events);
    }

    @Test
    void sameMidiNoteIsExclusiveAcrossPads() {
        final List<String> events = new ArrayList<>();
        final NoteLivePadPerformer performer = new NoteLivePadPerformer(
                new TestMidiOut(events),
                pad -> new int[]{60},
                (configured, raw) -> raw);

        performer.handlePadPress(0, true, 70, 100);
        performer.handlePadPress(1, true, 90, 100);
        performer.handlePadPress(0, false, 0, 100);
        performer.handlePadPress(1, false, 0, 100);

        assertEquals(List.of("on:60:70", "off:60", "on:60:90", "off:60"), events);
    }

    private record TestMidiOut(List<String> events) implements NoteLivePadPerformer.MidiOut {
        @Override
        public void noteOn(final int midiNote, final int velocity) {
            events.add("on:" + midiNote + ":" + velocity);
        }

        @Override
        public void noteOff(final int midiNote) {
            events.add("off:" + midiNote);
        }

        @Override
        public void timbre(final int midiNote, final int value) {
            events.add("timbre:" + midiNote + ":" + value);
        }
    }
}
