package com.oikoaudio.fire.perform;

final class PerformLayout {
    static final int PAD_COLUMNS = 16;
    static final int PAD_ROWS = 4;

    private final Orientation orientation;

    private PerformLayout(final Orientation orientation) {
        this.orientation = orientation;
    }

    static PerformLayout vertical() {
        return new PerformLayout(Orientation.VERTICAL);
    }

    static PerformLayout horizontal() {
        return new PerformLayout(Orientation.HORIZONTAL);
    }

    PerformLayout toggle() {
        return new PerformLayout(orientation == Orientation.VERTICAL ? Orientation.HORIZONTAL : Orientation.VERTICAL);
    }

    String label() {
        return orientation.label;
    }

    int visibleTrackCount() {
        return orientation == Orientation.VERTICAL ? PAD_COLUMNS : PAD_ROWS;
    }

    int visibleSceneCount() {
        return orientation == Orientation.VERTICAL ? PAD_ROWS : PAD_COLUMNS;
    }

    int visibleTrackIndexForPad(final int padIndex) {
        final int row = padIndex / PAD_COLUMNS;
        final int column = padIndex % PAD_COLUMNS;
        return orientation == Orientation.VERTICAL ? column : row;
    }

    int visibleSceneIndexForPad(final int padIndex) {
        final int row = padIndex / PAD_COLUMNS;
        final int column = padIndex % PAD_COLUMNS;
        return orientation == Orientation.VERTICAL ? row : column;
    }

    int toPadIndex(final int visibleTrackIndex, final int visibleSceneIndex) {
        if (orientation == Orientation.VERTICAL) {
            return visibleSceneIndex * PAD_COLUMNS + visibleTrackIndex;
        }
        return visibleTrackIndex * PAD_COLUMNS + visibleSceneIndex;
    }

    int trackScrollAmount(final boolean shiftHeld) {
        return shiftHeld ? 1 : visibleTrackCount();
    }

    int sceneScrollAmount(final boolean shiftHeld) {
        return shiftHeld ? 1 : visibleSceneCount();
    }

    int maxTrackOffset(final int totalTrackCount) {
        return Math.max(0, totalTrackCount - visibleTrackCount());
    }

    int maxSceneOffset(final int totalSceneCount) {
        return Math.max(0, totalSceneCount - visibleSceneCount());
    }

    private enum Orientation {
        VERTICAL("PerformV"),
        HORIZONTAL("PerformH");

        private final String label;

        Orientation(final String label) {
            this.label = label;
        }
    }
}
