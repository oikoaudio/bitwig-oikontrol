package com.oikoaudio.fire.fugue;

import com.bitwig.extensions.framework.MusicalScale;
import com.oikoaudio.fire.melodic.MelodicPattern;

import java.util.List;

public final class MelodicLineTransformer {
    private MelodicLineTransformer() {
    }

    public static FuguePattern transform(final FuguePattern source,
                                           final FugueLineSettings settings,
                                           final MusicalScale scale,
                                           final int rootNote) {
        final List<List<MelodicPattern.Step>> out = FuguePattern.emptyPolySteps();

        final int loopSteps = source.loopSteps();
        final int phraseSteps = settings.speed().isSpeedUp() ? repeatedPhraseSteps(source) : loopSteps;
        for (int sourceIndex = 0; sourceIndex < phraseSteps; sourceIndex++) {
            for (final MelodicPattern.Step sourceStep : source.notesAt(sourceIndex)) {
                if (!sourceStep.active() || sourceStep.tieFromPrevious() || sourceStep.pitch() == null) {
                    continue;
                }
                writeTransformedStep(out, sourceStep, sourceIndex, phraseSteps, loopSteps, settings, scale, rootNote);
            }
        }
        return new FuguePattern(out, loopSteps, true);
    }

    private static void writeTransformedStep(final List<List<MelodicPattern.Step>> out,
                                             final MelodicPattern.Step sourceStep,
                                             final int sourceIndex,
                                             final int phraseSteps,
                                             final int loopSteps,
                                             final FugueLineSettings settings,
                                             final MusicalScale scale,
                                             final int rootNote) {
        final int directionalIndex = directionalIndex(sourceIndex, phraseSteps, settings.direction());
        final int destination = transformedDestination(directionalIndex, phraseSteps, loopSteps, settings);
        final int pitch = ScaleAwareTransposer.transposeDiatonicThenChromatic(sourceStep.pitch(),
                settings.pitchDegreeOffset(), settings.pitchSemitoneOffset(), scale, rootNote);
        final MelodicPattern.Step transformed = transformedStep(sourceStep, destination, pitch, settings);
        out.get(destination).add(transformed);
        if (settings.speed().isSpeedUp()) {
            repeatSpeedUpStep(out, transformed, loopSteps, settings.speed().transformedLoopSteps(phraseSteps));
        }
    }

    private static MelodicPattern.Step transformedStep(final MelodicPattern.Step sourceStep,
                                                       final int destination,
                                                       final int pitch,
                                                       final FugueLineSettings settings) {
        return new MelodicPattern.Step(destination, true, false, pitch,
                Math.max(1, Math.min(127, sourceStep.velocity() + settings.velocityOffset())),
                Math.max(0.02, settings.speed().scaleGate(sourceStep.gate()) * settings.gatePercent() / 100.0),
                sourceStep.chance() * settings.chancePercent() / 100.0,
                sourceStep.accent(), sourceStep.slide(),
                sourceStep.recurrenceLength(), sourceStep.recurrenceMask());
    }

    private static int transformedDestination(final int directionalIndex,
                                              final int phraseSteps,
                                              final int loopSteps,
                                              final FugueLineSettings settings) {
        final int period = settings.speed().isSpeedUp()
                ? settings.speed().transformedLoopSteps(phraseSteps)
                : loopSteps;
        return Math.floorMod(settings.startOffset() + settings.speed().scaleStart(directionalIndex), period);
    }

    private static void repeatSpeedUpStep(final List<List<MelodicPattern.Step>> out,
                                          final MelodicPattern.Step transformed,
                                          final int loopSteps,
                                          final int period) {
        for (int destination = transformed.index() + period; destination < loopSteps; destination += period) {
            out.get(destination).add(transformed.withIndex(destination));
        }
    }

    private static int repeatedPhraseSteps(final FuguePattern source) {
        final int loopSteps = source.loopSteps();
        for (int candidate = 1; candidate <= loopSteps / 2; candidate++) {
            if (loopSteps % candidate == 0 && repeatsEvery(source, candidate)) {
                return candidate;
            }
        }
        return loopSteps;
    }

    private static boolean repeatsEvery(final FuguePattern source, final int period) {
        final int loopSteps = source.loopSteps();
        for (int step = period; step < loopSteps; step++) {
            if (!sameMusicalStepList(source.notesAt(step), source.notesAt(step % period))) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameMusicalStepList(final List<MelodicPattern.Step> a,
                                               final List<MelodicPattern.Step> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!sameMusicalStep(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameMusicalStep(final MelodicPattern.Step a, final MelodicPattern.Step b) {
        return a.active() == b.active()
                && a.tieFromPrevious() == b.tieFromPrevious()
                && java.util.Objects.equals(a.pitch(), b.pitch())
                && a.velocity() == b.velocity()
                && Math.abs(a.gate() - b.gate()) < 0.0001
                && Math.abs(a.chance() - b.chance()) < 0.0001
                && a.accent() == b.accent()
                && a.slide() == b.slide();
    }

    private static int directionalIndex(final int sourceIndex, final int loopSteps, final FugueDirection direction) {
        return switch (direction) {
            case FORWARD -> sourceIndex;
            case REVERSE -> loopSteps - 1 - sourceIndex;
            case PING_PONG -> sourceIndex < loopSteps / 2
                    ? sourceIndex
                    : loopSteps / 2 + loopSteps - 1 - sourceIndex;
        };
    }
}
