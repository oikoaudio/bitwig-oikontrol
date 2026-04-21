package com.oikoaudio.fire.fugue;

public enum FugueSpeed {
    DIVIDE_8("/8", 8, 1),
    DIVIDE_4("/4", 4, 1),
    DIVIDE_3("/3", 3, 1),
    DIVIDE_2("/2", 2, 1),
    DIVIDE_1_5("/1.5", 3, 2),
    NORMAL("1", 1, 1),
    TIMES_2_DOTTED("2 dt", 3, 4),
    TIMES_TRIPLET("1 trp", 2, 3),
    TIMES_2("2", 1, 2),
    TIMES_2_TRIPLET("2 trp", 1, 3),
    TIMES_3_DOTTED("3 dt", 2, 9),
    TIMES_4("4", 1, 4),
    TIMES_6("6", 1, 6),
    TIMES_8("8", 1, 8);

    private final String label;
    private final int denominator;
    private final int numerator;

    FugueSpeed(final String label, final int denominator, final int numerator) {
        this.label = label;
        this.denominator = denominator;
        this.numerator = numerator;
    }

    public String label() {
        return label;
    }

    int scaleStart(final int sourceStep) {
        return Math.floorDiv(sourceStep * denominator, numerator);
    }

    boolean isSpeedUp() {
        return numerator > denominator;
    }

    int transformedLoopSteps(final int sourceLoopSteps) {
        return Math.max(1, (int) Math.ceil(sourceLoopSteps * (double) denominator / numerator));
    }

    double scaleGate(final double gate) {
        return gate * denominator / numerator;
    }

    public FugueSpeed next(final int amount) {
        final FugueSpeed[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal() + amount))];
    }
}
