package com.oikoaudio.fire.nestedrhythm;

import java.util.List;

public final class NestedRhythmPattern {
    private final List<PulseEvent> events;

    public NestedRhythmPattern(final List<PulseEvent> events) {
        this.events = List.copyOf(events);
    }

    public List<PulseEvent> events() {
        return events;
    }

    public record PulseEvent(int order, int fineStart, int duration, int midiNote, int velocity, Role role) {
    }

    public enum Role {
        PRIMARY_ANCHOR,
        SECONDARY_ANCHOR,
        TUPLET_LEAD,
        TUPLET_INTERIOR,
        RATCHET_LEAD,
        RATCHET_INTERIOR,
        PICKUP
    }
}
