package com.oikoaudio.fire.sequence;

public record RecurrencePattern(int length, int mask) {
    public static final int DEFAULT_BITWIG_LENGTH = 1;
    public static final int DEFAULT_BITWIG_MASK = 0b1;
    public static final int EDITOR_DEFAULT_SPAN = 8;

    public RecurrencePattern {
        if (length <= 1) {
            length = 0;
            mask = 0;
        } else {
            length = Math.min(8, length);
            mask &= (1 << length) - 1;
            if (mask == 0 || mask == ((1 << length) - 1)) {
                length = 0;
                mask = 0;
            }
        }
    }

    public static RecurrencePattern of(final int length, final int mask) {
        return new RecurrencePattern(length, mask);
    }

    public boolean isDefault() {
        return length <= 1;
    }

    public int bitwigLength() {
        return isDefault() ? DEFAULT_BITWIG_LENGTH : length;
    }

    public int bitwigMask() {
        return isDefault() ? DEFAULT_BITWIG_MASK : mask;
    }

    public int effectiveSpan() {
        return isDefault() ? EDITOR_DEFAULT_SPAN : length;
    }

    public int effectiveMask(final int span) {
        return isDefault() ? ((1 << span) - 1) : (mask & ((1 << span) - 1));
    }

    public String summary() {
        if (isDefault()) {
            return "Off";
        }
        final int span = effectiveSpan();
        final int shownMask = effectiveMask(span);
        return "%d:%s".formatted(span, Integer.toBinaryString((1 << span) | shownMask).substring(1));
    }

    public RecurrencePattern toggledAt(final int padIndex) {
        final int span = effectiveSpan();
        if (padIndex < 0 || padIndex >= span) {
            return this;
        }
        final int bit = 1 << padIndex;
        int updatedMask = effectiveMask(span) ^ bit;
        if ((updatedMask & ((1 << span) - 1)) == 0) {
            updatedMask = bit;
        }
        return of(span, updatedMask);
    }

    public RecurrencePattern applySpanGesture(final int newSpan) {
        final int currentSpan = effectiveSpan();
        if (isDefault()) {
            return of(newSpan, 0b1);
        }
        if (newSpan == currentSpan) {
            return of(0, 0);
        }
        final int currentMask = effectiveMask(currentSpan);
        final int resizedMask = newSpan < currentSpan
                ? currentMask & ((1 << newSpan) - 1)
                : currentMask | (((1 << (newSpan - currentSpan)) - 1) << currentSpan);
        final int normalizedMask = resizedMask == ((1 << newSpan) - 1) ? 0 : resizedMask;
        return of(newSpan, normalizedMask);
    }
}
