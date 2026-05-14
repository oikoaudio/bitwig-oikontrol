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

    public record PulseEvent(int order, int fineStart, int duration, int midiNote, int velocity, Role role,
                             double indispensability) {
        public PulseEvent(final int order, final int fineStart, final int duration, final int midiNote,
                          final int velocity, final Role role) {
            this(order, fineStart, duration, midiNote, velocity, role, 0.5);
        }
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
