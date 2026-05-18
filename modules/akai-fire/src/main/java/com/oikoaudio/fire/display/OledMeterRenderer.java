package com.oikoaudio.fire.display;

import java.util.Arrays;

public final class OledMeterRenderer {
    public static final int WIDTH = 128;
    public static final int HEIGHT = 64;
    private static final int PAGE_HEIGHT = 8;
    private static final int IMAGE_BYTES = WIDTH * HEIGHT / PAGE_HEIGHT;
    private static final int TOP_MARGIN = 2;
    private static final int BOTTOM_MARGIN = 3;
    private static final int MIN_BAR_WIDTH = 2;

    private OledMeterRenderer() {
    }

    public static int[] verticalMeters(final int[] values, final int count) {
        return verticalMeters(values, null, null, count);
    }

    public static int[] verticalMeters(final int[] values, final int[] peakMarkers, final int count) {
        return verticalMeters(values, peakMarkers, null, count);
    }

    public static int[] verticalMeters(final int[] values, final int[] peakMarkers, final boolean[] muted,
                                       final int count) {
        final int visibleCount = Math.max(0, Math.min(count, values.length));
        final int[] image = new int[IMAGE_BYTES];
        if (visibleCount == 0) {
            return image;
        }

        final int slotWidth = Math.max(1, WIDTH / visibleCount);
        final int barWidth = Math.max(MIN_BAR_WIDTH, slotWidth - 2);
        final int maxHeight = HEIGHT - TOP_MARGIN - BOTTOM_MARGIN;
        for (int index = 0; index < visibleCount; index++) {
            final int left = index * slotWidth + Math.max(0, (slotWidth - barWidth) / 2);
            final int right = Math.min(WIDTH - 1, left + barWidth - 1);
            final int bottom = HEIGHT - 1 - BOTTOM_MARGIN;
            final int height = VuMeterFormatter.meterHeight(values[index], maxHeight);
            final int top = bottom - height + 1;
            final boolean mutedLane = muted != null && index < muted.length && muted[index];

            if (mutedLane) {
                drawHorizontalLine(image, left, right, bottom);
            } else if (height > 0) {
                fillRect(image, left, Math.max(TOP_MARGIN, top), right, bottom);
            }
            if (!mutedLane && peakMarkers != null && index < peakMarkers.length) {
                final int markerHeight = VuMeterFormatter.meterHeight(peakMarkers[index], maxHeight);
                if (markerHeight > 0) {
                    final int markerY = Math.max(TOP_MARGIN, bottom - markerHeight + 1);
                    drawHorizontalLine(image, left, right, markerY);
                }
            }
        }
        return image;
    }

    static int[] emptyImage() {
        return new int[IMAGE_BYTES];
    }

    static void fillRect(final int[] image, final int left, final int top, final int right, final int bottom) {
        for (int y = Math.max(0, top); y <= Math.min(HEIGHT - 1, bottom); y++) {
            for (int x = Math.max(0, left); x <= Math.min(WIDTH - 1, right); x++) {
                setPixel(image, x, y);
            }
        }
    }

    static void drawHorizontalLine(final int[] image, final int left, final int right, final int y) {
        for (int x = Math.max(0, left); x <= Math.min(WIDTH - 1, right); x++) {
            setPixel(image, x, y);
        }
    }

    static void drawRect(final int[] image, final int left, final int top, final int right, final int bottom,
                         final boolean filled) {
        if (filled) {
            fillRect(image, left, top, right, bottom);
            return;
        }
        for (int x = Math.max(0, left); x <= Math.min(WIDTH - 1, right); x++) {
            setPixel(image, x, top);
            setPixel(image, x, bottom);
        }
        for (int y = Math.max(0, top); y <= Math.min(HEIGHT - 1, bottom); y++) {
            setPixel(image, left, y);
            setPixel(image, right, y);
        }
    }

    static void setPixel(final int[] image, final int x, final int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            return;
        }
        final int page = y / PAGE_HEIGHT;
        final int bit = y % PAGE_HEIGHT;
        image[page * WIDTH + x] |= 1 << bit;
    }

    static int pixel(final int[] image, final int x, final int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            return 0;
        }
        final int page = y / PAGE_HEIGHT;
        final int bit = y % PAGE_HEIGHT;
        return (image[page * WIDTH + x] >> bit) & 1;
    }

    static boolean isBlank(final int[] image) {
        return Arrays.stream(image).allMatch(value -> value == 0);
    }
}
