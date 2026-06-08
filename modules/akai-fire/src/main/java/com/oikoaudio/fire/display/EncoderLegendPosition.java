package com.oikoaudio.fire.display;

public enum EncoderLegendPosition {
    BOTTOM(7),
    TOP(0);

    private final int row;

    EncoderLegendPosition(final int row) {
        this.row = row;
    }

    public int row() {
        return row;
    }
}
