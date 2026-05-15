package com.oikoaudio.fire.nestedrhythm;

final class NestedRhythmPlayStart {
    private NestedRhythmPlayStart() {
    }

    static double beatStep(final int meterDenominator) {
        return NestedRhythmGenerator.beatsPerBar(1, meterDenominator);
    }

    static double wrap(final double playStart, final double loopLength) {
        final double normalizedLoopLength = Math.max(beatStep(16), loopLength);
        final double wrapped = playStart % normalizedLoopLength;
        return wrapped < 0.0 ? wrapped + normalizedLoopLength : wrapped;
    }

    static double increment(final double currentPlayStart,
                            final double loopLength,
                            final int meterDenominator,
                            final int amount) {
        return wrap(currentPlayStart + amount * beatStep(meterDenominator), loopLength);
    }

    static double incrementByStep(final double currentPlayStart,
                                  final double loopLength,
                                  final double step,
                                  final int amount) {
        return wrap(currentPlayStart + amount * Math.max(0.0, step), loopLength);
    }

    static double snapToGrid(final double currentPlayStart,
                             final double loopLength,
                             final double step) {
        final double normalizedStep = Math.max(beatStep(16), step);
        final double wrapped = wrap(currentPlayStart, loopLength);
        return wrap(Math.round(wrapped / normalizedStep) * normalizedStep, loopLength);
    }
}
