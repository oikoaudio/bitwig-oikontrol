package com.oikoaudio.fire.display;

import java.util.Objects;

public final class PeakRmsOledView {
    public static final String LEGEND = "Peak          RMS";
    private static final String BLANK_TEXT_ROW = "                    ";

    private final OledDisplay oled;
    private boolean initialized = false;
    private String bottomLegend = null;
    private EncoderLegendPosition legendPosition = EncoderLegendPosition.BOTTOM;
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
        final EncoderLegendPosition displayLegendPosition = oled.footerLegendPosition();
        final EncoderLegendPosition currentLegendPosition = displayLegendPosition == null
                ? EncoderLegendPosition.BOTTOM
                : displayLegendPosition;
        if (!initialized
                || layoutRevision != oled.layoutRevision()
                || !Objects.equals(this.bottomLegend, normalizedBottomLegend)
                || legendPosition != currentLegendPosition) {
            oled.clearScreen();
            if (currentLegendPosition == EncoderLegendPosition.BOTTOM) {
                paintMeterLabel(currentLegendPosition);
                paintBottomLegend(normalizedBottomLegend, currentLegendPosition);
            } else {
                paintBottomLegend(normalizedBottomLegend, currentLegendPosition);
                paintMeterLabel(currentLegendPosition);
            }
            initialized = true;
            this.bottomLegend = normalizedBottomLegend;
            legendPosition = currentLegendPosition;
            layoutRevision = oled.layoutRevision();
        }
        oled.sendString(2, OledDisplay.TextJustification.LEFT, maxMeterRow(legendPosition),
                VuMeterFormatter.meterPairLine(maxPeak, maxRms));
        oled.sendString(2, OledDisplay.TextJustification.LEFT, currentMeterRow(legendPosition),
                VuMeterFormatter.meterPairLine(currentPeak, currentRms));
    }

    public void showValueInfo(final String title, final String value) {
        prepareValueInfo();
        oled.valueInfoNoClear(title, value);
    }

    public void showValueInfo(final String title, final String value, final double normalizedValue,
                              final boolean biPolar) {
        prepareValueInfo();
        oled.valueInfoWithBarNoClear(title, value, normalizedValue, biPolar);
    }

    private void prepareValueInfo() {
        if (hasBottomLegend()) {
            clearRowsAroundLegend();
            initialized = false;
            paintBottomLegend(bottomLegend, legendPosition);
        } else {
            reset();
            oled.clearScreen();
        }
    }

    public void reset() {
        initialized = false;
        bottomLegend = null;
        legendPosition = EncoderLegendPosition.BOTTOM;
        layoutRevision = Long.MIN_VALUE;
    }

    private boolean hasBottomLegend() {
        return bottomLegend != null;
    }

    private void paintBottomLegend(final String legend, final EncoderLegendPosition position) {
        if (legend != null) {
            oled.sendString(0, OledDisplay.TextJustification.LEFT, position.row(), legend);
        }
    }

    private void paintMeterLabel(final EncoderLegendPosition position) {
        oled.sendString(0, OledDisplay.TextJustification.LEFT, meterLabelRow(position), LEGEND);
    }

    private int meterLabelRow(final EncoderLegendPosition position) {
        return position == EncoderLegendPosition.TOP ? 2 : 0;
    }

    private int maxMeterRow(final EncoderLegendPosition position) {
        return position == EncoderLegendPosition.TOP ? 3 : 1;
    }

    private int currentMeterRow(final EncoderLegendPosition position) {
        return position == EncoderLegendPosition.TOP ? 6 : 4;
    }

    private void clearRowsAroundLegend() {
        if (legendPosition == EncoderLegendPosition.TOP) {
            for (int row = 1; row < 8; row++) {
                oled.sendString(0, OledDisplay.TextJustification.LEFT, row, BLANK_TEXT_ROW);
            }
            return;
        }
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
