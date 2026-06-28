package com.oikoaudio.fire.display;

import com.personal.chords.ChordDetector;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * SPIKE ONLY: renders BitX ChordDetector output on the Fire OLED for note/chord display.
 *
 * Chord naming currently uses a local copy of Per-Sonal's BitX ChordDetector.
 * Do not ship until reuse permission/license is recorded or the detector is replaced.
 */
public final class NoteChordOledView {
    private static final long TRANSIENT_DISPLAY_QUIET_PERIOD_MS = 900L;

    private final OledDisplay oled;
    private final ChordDetector chordDetector = new ChordDetector();
    private String lastNotes = "";
    private String lastChord = "";
    private String lastStatus = "";
    private String lastLegend = "";
    private long layoutRevision = Long.MIN_VALUE;

    public NoteChordOledView(final OledDisplay oled) {
        this.oled = oled;
    }

    public boolean show(final int[] midiNotes, final String footerLegend) {
        return show(midiNotes, footerLegend, "");
    }

    public boolean show(final int[] midiNotes, final String footerLegend, final String statusLine) {
        if (midiNotes == null) {
            return show(List.of(), footerLegend, statusLine);
        }
        return show(Arrays.stream(midiNotes).boxed().toList(), footerLegend, statusLine);
    }

    public boolean show(final List<Integer> midiNotes, final String footerLegend) {
        return show(midiNotes, footerLegend, "");
    }

    public boolean show(final List<Integer> midiNotes, final String footerLegend, final String statusLine) {
        if (oled.hasRecentTransientMessage(TRANSIENT_DISPLAY_QUIET_PERIOD_MS)) {
            reset();
            return false;
        }
        if (midiNotes == null || midiNotes.isEmpty()) {
            reset();
            return false;
        }

        final ChordDetector.ChordResult result = chordDetector.detect(midiNotes);
        final String notes = String.join(" ", result.midiNoteNames());
        final String chord = displayChord(result.chordName(), result.midiNoteNames().size());
        final String status = normalize(statusLine);
        final String normalizedLegend = normalize(footerLegend);
        final EncoderLegendPosition legendPosition = oled.footerLegendPosition() == null
                ? EncoderLegendPosition.BOTTOM
                : oled.footerLegendPosition();

        if (layoutRevision == oled.layoutRevision()
                && Objects.equals(lastNotes, notes)
                && Objects.equals(lastChord, chord)
                && Objects.equals(lastStatus, status)
                && Objects.equals(lastLegend, normalizedLegend)) {
            return true;
        }

        oled.clearScreen();
        if (legendPosition == EncoderLegendPosition.TOP) {
            paintLegend(normalizedLegend, legendPosition);
            paintNoteChord(notes, chord, status, 1);
        } else {
            paintNoteChord(notes, chord, status, 0);
            paintLegend(normalizedLegend, legendPosition);
        }
        lastNotes = notes;
        lastChord = chord;
        lastStatus = status;
        lastLegend = normalizedLegend;
        layoutRevision = oled.layoutRevision();
        return true;
    }

    public void reset() {
        lastNotes = "";
        lastChord = "";
        lastStatus = "";
        lastLegend = "";
        layoutRevision = Long.MIN_VALUE;
    }

    private void paintNoteChord(final String notes, final String chord, final String status, final int topRow) {
        if (!status.isBlank()) {
            oled.sendString(0, OledDisplay.TextJustification.CENTER, topRow, status);
            oled.sendString(0, OledDisplay.TextJustification.CENTER, topRow + 1, notes);
        } else {
            oled.sendString(0, OledDisplay.TextJustification.CENTER, topRow, notes);
        }
        final int valueRow = status.isBlank() ? topRow + 2 : topRow + 3;
        if (chord.isBlank()) {
            oled.sendString(2, OledDisplay.TextJustification.CENTER, valueRow, notes);
            return;
        }
        oled.sendString(3, OledDisplay.TextJustification.CENTER, valueRow, chord);
    }

    private void paintLegend(final String legend, final EncoderLegendPosition position) {
        if (!legend.isBlank()) {
            oled.sendString(0, OledDisplay.TextJustification.LEFT, position.row(), legend);
        }
    }

    private String displayChord(final String chordName, final int noteCount) {
        if (noteCount < 2 || chordName == null || chordName.isBlank() || "N.C.".equals(chordName)) {
            return "";
        }
        return chordName;
    }

    private String normalize(final String value) {
        return value == null ? "" : value;
    }
}
