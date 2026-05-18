package com.oikoaudio.fire.display;

public final class VuMeterFormatter {
    public static final int RANGE = 128;
    private static final double MIN_DB = -60.0;

    private VuMeterFormatter() {
    }

    public static String meterValue(final int value) {
        return formatDb(value, true);
    }

    public static String meterValueShort(final int value) {
        return formatDb(value, false);
    }

    public static String meterPairLine(final int peakValue, final int rmsValue) {
        final String line = "%s | %s".formatted(meterValueShort(peakValue), meterValueShort(rmsValue));
        return line.length() <= 20 ? line : line.replace(" | ", "|");
    }

    public static String formatDb(final int value, final boolean includeUnit) {
        final double db = valueToDb(value);
        final String suffix = includeUnit ? " dB" : "";
        if (db <= MIN_DB) {
            return "-inf" + suffix;
        }
        return String.format("%+.1f%s", db, suffix);
    }

    public static double valueToDb(final int value) {
        final int clamped = clamp(value, 0, RANGE - 1);
        if (clamped <= 0) {
            return MIN_DB;
        }
        final double amplitude = (double) clamped / (double) (RANGE - 1);
        return Math.max(MIN_DB, 20.0 * Math.log10(amplitude));
    }

    public static int meterHeight(final int value, final int maxHeight) {
        if (maxHeight <= 0) {
            return 0;
        }
        final int clamped = clamp(value, 0, RANGE - 1);
        return Math.max(0, Math.min(maxHeight, (int) Math.round(clamped * (double) maxHeight / (RANGE - 1))));
    }

    public static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}
