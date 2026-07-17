package com.oikoaudio.fire.note;

import java.util.Arrays;

enum HarmonicVoicing {
    DROP_2("Drop 2"),
    CLOSE("Close"),
    OPEN("Open");

    private final String displayName;

    HarmonicVoicing(final String displayName) {
        this.displayName = displayName;
    }

    String displayName() {
        return displayName;
    }

    HarmonicVoicing step(final int steps) {
        final int next = Math.max(0, Math.min(values().length - 1, ordinal() + steps));
        return values()[next];
    }

    int[] apply(final int[] latticeNotes) {
        final int validNoteCount = validNoteCount(latticeNotes);
        if (this == DROP_2 || validNoteCount <= 1) {
            return latticeNotes.clone();
        }

        final int[] voiced = closeVoicing(latticeNotes);
        if (this == OPEN) {
            final int voiceToRaise = validNoteCount == 2 ? 1 : validNoteCount - 2;
            voiced[voiceToRaise] += 12;
            Arrays.sort(voiced, 0, validNoteCount);
        }
        return voiced;
    }

    private static int[] closeVoicing(final int[] latticeNotes) {
        final int anchor =
                Arrays.stream(latticeNotes).filter(note -> note >= 0).findFirst().orElse(-1);
        final int[] voiced = new int[latticeNotes.length];
        int out = 0;
        for (final int note : latticeNotes) {
            if (note >= 0) {
                voiced[out++] = anchor + Math.floorMod(note - anchor, 12);
            }
        }
        Arrays.sort(voiced, 0, out);
        Arrays.fill(voiced, out, voiced.length, -1);
        return voiced;
    }

    private static int validNoteCount(final int[] notes) {
        return (int) Arrays.stream(notes).filter(note -> note >= 0).count();
    }
}
