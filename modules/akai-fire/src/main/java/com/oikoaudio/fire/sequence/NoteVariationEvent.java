package com.oikoaudio.fire.sequence;

import java.util.Objects;

/** Stable identity and onset grouping for one note-on event. */
public record NoteVariationEvent(Id id, long onsetKey) {
    public NoteVariationEvent {
        Objects.requireNonNull(id, "id");
    }

    public record Id(int channel, int step, int pitch) implements Comparable<Id> {
        @Override
        public int compareTo(final Id other) {
            int result = Integer.compare(channel, other.channel);
            if (result != 0) {
                return result;
            }
            result = Integer.compare(step, other.step);
            return result != 0 ? result : Integer.compare(pitch, other.pitch);
        }
    }
}
