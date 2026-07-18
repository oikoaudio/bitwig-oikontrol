package com.oikoaudio.fire.multiclip;

/** The fixed mapping from a direct child track to its Multiclip Seq lane identity. */
public record TrackLaneMapping(
        int childPosition, int laneNumber, int midiChannel, int midiNote, int page, int row) {
    public static final int MAX_LANES = 16;
    public static final int LANES_PER_PAGE = 4;
    public static final int FIRST_MIDI_NOTE = 36;

    public static TrackLaneMapping fromChildPosition(final int childPosition) {
        if (childPosition < 0 || childPosition >= MAX_LANES) {
            throw new IllegalArgumentException(
                    "Child position must be between 0 and " + (MAX_LANES - 1));
        }

        return new TrackLaneMapping(
                childPosition,
                childPosition + 1,
                childPosition,
                FIRST_MIDI_NOTE + childPosition,
                childPosition / LANES_PER_PAGE,
                childPosition % LANES_PER_PAGE);
    }
}
