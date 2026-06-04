package com.oikoaudio.fire.display;

import java.util.Objects;

public final class PeakRmsOledView {
    public static final String LEGEND = "Peak          RMS";
    private static final String BLANK_TEXT_ROW = "                    ";

    private final OledDisplay oled;
    private boolean initialized = false;
    private String bottomLegend = null;
    private long layoutRevision = Long.MIN_VALUE;

    public PeakRmsOledView(final OledDisplay oled) {
        this.oled = oled;
    }

    public void show(final int maxPeak,
                     final int maxRms,
                     final int currentPeak,
                     final int currentRms,
                     final String bottomLegend) {
        final String normalizedBottomLegend = normalizeBottomLegend(bottomLegend);
        if (!initialized
                || layoutRevision != oled.layoutRevision()
                || !Objects.equals(this.bottomLegend, normalizedBottomLegend)) {
            oled.clearScreen();
            oled.sendString(0, OledDisplay.TextJustification.LEFT, 0, LEGEND);
            paintBottomLegend(normalizedBottomLegend);
            initialized = true;
            this.bottomLegend = normalizedBottomLegend;
            layoutRevision = oled.layoutRevision();
        }
        oled.sendString(2, OledDisplay.TextJustification.LEFT, 1,
                VuMeterFormatter.meterPairLine(maxPeak, maxRms));
        oled.sendString(2, OledDisplay.TextJustification.LEFT, 4,
                VuMeterFormatter.meterPairLine(currentPeak, currentRms));
    }

    public void showValueInfo(final String title, final String value) {
        if (hasBottomLegend()) {
            clearRowsAboveBottomLegend();
            initialized = false;
            paintBottomLegend(bottomLegend);
        } else {
            reset();
            oled.clearScreen();
        }
        oled.valueInfoNoClear(title, value);
    }

    public void reset() {
        initialized = false;
        bottomLegend = null;
        layoutRevision = Long.MIN_VALUE;
    }

    private boolean hasBottomLegend() {
        return bottomLegend != null;
    }

    private void paintBottomLegend(final String legend) {
        if (legend != null) {
            oled.sendString(0, OledDisplay.TextJustification.LEFT, 7, legend);
        }
    }

    private void clearRowsAboveBottomLegend() {
        oled.sendString(2, OledDisplay.TextJustification.LEFT, 0, BLANK_TEXT_ROW);
        oled.sendString(2, OledDisplay.TextJustification.LEFT, 1, BLANK_TEXT_ROW);
        oled.sendString(3, OledDisplay.TextJustification.LEFT, 2, BLANK_TEXT_ROW);
        oled.sendString(2, OledDisplay.TextJustification.LEFT, 4, BLANK_TEXT_ROW);
        for (int row = 0; row < 7; row++) {
            oled.sendString(0, OledDisplay.TextJustification.LEFT, row, BLANK_TEXT_ROW);
        }
    }

    private String normalizeBottomLegend(final String legend) {
        return legend == null || legend.isBlank() ? null : legend;
    }
}
