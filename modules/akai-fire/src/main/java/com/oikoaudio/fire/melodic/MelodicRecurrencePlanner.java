package com.oikoaudio.fire.melodic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class MelodicRecurrencePlanner {
    public enum Style {
        ACID,
        MOTIF,
        CALL_RESPONSE,
        ROLLING,
        OCTAVE
    }

    private MelodicRecurrencePlanner() {
    }

    public static MelodicPattern apply(final MelodicPattern pattern, final MelodicPhraseContext context,
                                       final Style style, final double timeVariance, final long seed) {
        if (timeVariance < 0.12) {
            return clearRecurrence(pattern);
        }

        final Random random = new Random(seed ^ 0x5EEDC0DEL);
        MelodicPattern out = clearRecurrence(pattern);
        final MelodicPatternAnalyzer.Analysis analysis = MelodicPatternAnalyzer.analyze(out);
        final int span = timeVariance < 0.34 ? 2 : timeVariance < 0.67 ? 4 : 8;
        final RoleBuckets buckets = bucketize(out, analysis, style, timeVariance);

        final int themeTarget = targetCount(buckets.themeSteps().size(), out.loopSteps(), timeVariance, style, 0.12, 0.32);
        final int alternateTarget = targetCount(buckets.alternateSteps().size(), out.loopSteps(), timeVariance, style, 0.08, 0.26);

        for (final int stepIndex : selectRecurringSteps(buckets.themeSteps(), themeTarget, timeVariance, random)) {
            final MelodicPattern.Step step = out.step(stepIndex);
            out = out.withStep(step.withRecurrence(span, chooseThemeMask(span, style, timeVariance, random)));
        }

        for (final int stepIndex : selectRecurringSteps(buckets.alternateSteps(), alternateTarget, timeVariance, random)) {
            final MelodicPattern.Step step = out.step(stepIndex);
            out = out.withStep(step.withRecurrence(span, chooseAlternateMask(span, style, timeVariance, random)));
        }

        if (timeVariance >= 0.6) {
            out = maybeInjectOrnament(out, context, style, span, random, timeVariance);
        }
        return out;
    }

    private static MelodicPattern clearRecurrence(final MelodicPattern pattern) {
        MelodicPattern out = pattern;
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = out.step(i);
            if (step.recurrenceLength() > 0 || step.recurrenceMask() != 0) {
                out = out.withStep(step.withRecurrence(0, 0));
            }
        }
        return out;
    }

    private static List<Integer> selectRecurringSteps(final List<Integer> rankedCandidates, final int targetCount,
                                                      final double timeVariance, final Random random) {
        if (targetCount <= 0 || rankedCandidates.isEmpty()) {
            return List.of();
        }
        final List<Integer> pool = new ArrayList<>(rankedCandidates);
        final List<Integer> selected = new ArrayList<>();
        while (selected.size() < targetCount && !pool.isEmpty()) {
            final int limit = Math.min(pool.size(), Math.max(1, 2 + (int) Math.round(timeVariance * 6.0)));
            double totalWeight = 0.0;
            final double[] cumulative = new double[limit];
            for (int i = 0; i < limit; i++) {
                final int stepIndex = pool.get(i);
                final double rankWeight = Math.max(0.35, limit - i);
                final double spacingWeight = spacingWeight(stepIndex, selected, timeVariance);
                totalWeight += rankWeight * spacingWeight;
                cumulative[i] = totalWeight;
            }

            final double roll = random.nextDouble() * totalWeight;
            int chosen = 0;
            while (chosen < limit - 1 && roll > cumulative[chosen]) {
                chosen++;
            }
            selected.add(pool.remove(chosen));
        }
        selected.sort(Integer::compareTo);
        return selected;
    }

    private static double spacingWeight(final int stepIndex, final List<Integer> selected, final double timeVariance) {
        if (selected.isEmpty()) {
            return 1.0;
        }
        int nearestDistance = Integer.MAX_VALUE;
        for (final int chosen : selected) {
            nearestDistance = Math.min(nearestDistance, Math.abs(stepIndex - chosen));
        }
        if (nearestDistance <= 1) {
            return 0.35 + timeVariance * 0.25;
        }
        if (nearestDistance == 2) {
            return 0.65 + timeVariance * 0.2;
        }
        return 1.0 + Math.min(0.4, nearestDistance * 0.06);
    }

    private static int targetCount(final int available, final double timeVariance, final Style style,
                                   final double baseFactor, final double varianceFactor) {
        if (available <= 0) {
            return 0;
        }
        final double styleBias = switch (style) {
            case CALL_RESPONSE -> 0.18;
            case ACID, MOTIF -> 0.10;
            case ROLLING, OCTAVE -> 0.06;
        };
        return Math.max(1, Math.min(available,
                (int) Math.round(available * (baseFactor + styleBias + timeVariance * varianceFactor))));
    }

    private static int targetCount(final int available, final int loopSteps, final double timeVariance, final Style style,
                                   final double baseFactor, final double varianceFactor) {
        int baseTarget = targetCount(available, timeVariance, style, baseFactor, varianceFactor);
        if (style == Style.ACID) {
            if (timeVariance >= 0.9 && loopSteps >= 24) {
                baseTarget = Math.max(baseTarget, Math.min(available, 4));
            } else if (timeVariance >= 0.72) {
                baseTarget = Math.max(baseTarget, Math.min(available, loopSteps >= 24 ? 3 : 2));
            } else if (timeVariance >= 0.45) {
                baseTarget = Math.max(baseTarget, Math.min(available, 2));
            }
        }
        if (timeVariance >= 0.85 && loopSteps >= 24) {
            final int minimum = style == Style.ACID ? 4 : 3;
            baseTarget = Math.max(baseTarget, Math.min(available, minimum));
        } else if (timeVariance >= 0.65 && loopSteps >= 24) {
            baseTarget = Math.max(baseTarget, Math.min(available, style == Style.ACID ? 3 : 2));
        }
        return baseTarget;
    }

    private static RoleBuckets bucketize(final MelodicPattern pattern, final MelodicPatternAnalyzer.Analysis analysis,
                                         final Style style, final double timeVariance) {
        final List<Integer> themeSteps = new ArrayList<>();
        final List<Integer> alternateSteps = new ArrayList<>();
        final int responseStart = pattern.loopSteps() / 2;
        final boolean allowAnchors = timeVariance >= 0.72;
        for (final int stepIndex : analysis.activeSteps()) {
            if (!allowAnchors && analysis.anchorSteps().contains(stepIndex)) {
                continue;
            }
            if (allowAnchors && isBoundaryAnchor(stepIndex, pattern.loopSteps())) {
                continue;
            }
            final MelodicPattern.Step step = pattern.step(stepIndex);
            if (style == Style.ACID && timeVariance < 0.45 && shouldSkipAcidPulse(stepIndex, pattern.loopSteps())) {
                continue;
            }
            final boolean themeCandidate = step.accent()
                    || stepIndex % 8 == 0
                    || (style == Style.CALL_RESPONSE && stepIndex >= pattern.loopSteps() / 2)
                    || (style == Style.ACID && isAcidHookWindow(stepIndex, pattern.loopSteps()))
                    || (style == Style.MOTIF && stepIndex % 4 == 2);
            if (themeCandidate) {
                themeSteps.add(stepIndex);
            } else {
                if (style == Style.CALL_RESPONSE && stepIndex < responseStart) {
                    continue;
                }
                alternateSteps.add(stepIndex);
            }
        }
        themeSteps.sort(Comparator.comparingInt(stepIndex -> priorityForTheme(stepIndex, pattern.loopSteps(), style)));
        alternateSteps.sort(Comparator.comparingInt(stepIndex -> priorityForAlternate(stepIndex, pattern.loopSteps(), style)));
        backfillCandidateBuckets(themeSteps, alternateSteps, analysis.activeSteps(), pattern.loopSteps(), style, timeVariance);
        return new RoleBuckets(themeSteps, alternateSteps);
    }

    private static void backfillCandidateBuckets(final List<Integer> themeSteps, final List<Integer> alternateSteps,
                                                 final List<Integer> activeSteps, final int loopSteps,
                                                 final Style style, final double timeVariance) {
        final double backfillThreshold = style == Style.ACID ? 0.5 : 0.72;
        if (timeVariance < backfillThreshold) {
            return;
        }
        final int minimumTotal = style == Style.ACID
                ? (timeVariance >= 0.9 ? 6 : 5)
                : (timeVariance >= 0.9 ? 6 : 4);
        if (themeSteps.size() + alternateSteps.size() >= minimumTotal) {
            return;
        }
        for (final int stepIndex : activeSteps) {
            if (isBoundaryAnchor(stepIndex, loopSteps)
                    || themeSteps.contains(stepIndex)
                    || alternateSteps.contains(stepIndex)) {
                continue;
            }
            if (style == Style.CALL_RESPONSE && stepIndex < loopSteps / 2) {
                alternateSteps.add(stepIndex);
            } else if (stepIndex % 4 == 0 || stepIndex % 8 == 0) {
                themeSteps.add(stepIndex);
            } else {
                alternateSteps.add(stepIndex);
            }
            if (themeSteps.size() + alternateSteps.size() >= minimumTotal) {
                break;
            }
        }
        themeSteps.sort(Comparator.comparingInt(stepIndex -> priorityForTheme(stepIndex, loopSteps, style)));
        alternateSteps.sort(Comparator.comparingInt(stepIndex -> priorityForAlternate(stepIndex, loopSteps, style)));
    }

    private static boolean isBoundaryAnchor(final int stepIndex, final int loopSteps) {
        return stepIndex == 0 || stepIndex == loopSteps - 1;
    }

    private static int priorityForTheme(final int stepIndex, final int loopSteps, final Style style) {
        int score = stepIndex;
        if (style == Style.CALL_RESPONSE && stepIndex >= loopSteps / 2) {
            score -= 12;
        }
        if (style == Style.ACID) {
            if (isAcidLateWindow(stepIndex, loopSteps)) {
                score -= 16;
            } else if (isAcidHookWindow(stepIndex, loopSteps)) {
                score -= 10;
            }
        }
        if (stepIndex % 8 == 0) {
            score -= 10;
        } else if (stepIndex % 4 == 0) {
            score -= 4;
        }
        return score;
    }

    private static int priorityForAlternate(final int stepIndex, final int loopSteps, final Style style) {
        int score = stepIndex;
        if (style == Style.ACID) {
            if (isAcidLateWindow(stepIndex, loopSteps)) {
                score -= 14;
            } else if (isAcidHookWindow(stepIndex, loopSteps)) {
                score -= 8;
            } else {
                score += 8;
            }
        }
        if (style == Style.ROLLING && stepIndex % 2 != 0) {
            score -= 6;
        }
        if (style == Style.OCTAVE && stepIndex % 4 == 2) {
            score -= 8;
        }
        if (style == Style.CALL_RESPONSE && stepIndex >= loopSteps / 2) {
            score -= 6;
        }
        return score;
    }

    private static int chooseThemeMask(final int span, final Style style, final double timeVariance,
                                       final Random random) {
        final int[] masks = switch (span) {
            case 2 -> new int[]{0b01};
            case 4 -> switch (style) {
                case CALL_RESPONSE -> new int[]{0b0011, 0b0101, 0b0111};
                case ACID, MOTIF -> new int[]{0b0011, 0b0101};
                case ROLLING, OCTAVE -> new int[]{0b0011, 0b0111};
            };
            default -> switch (style) {
                case CALL_RESPONSE -> new int[]{0b00001111, 0b00110011, 0b01010101, 0b01110111};
                case ACID -> new int[]{0b00001111, 0b00111100, 0b01010101};
                case MOTIF -> new int[]{0b00001111, 0b00110011, 0b01110111};
                case ROLLING -> new int[]{0b00001111, 0b00110011, 0b01101111};
                case OCTAVE -> new int[]{0b00001111, 0b01010101, 0b01110111};
            };
        };
        return masks[random.nextInt(masks.length)];
    }

    private static int chooseAlternateMask(final int span, final Style style, final double timeVariance,
                                           final Random random) {
        final int[] masks = switch (span) {
            case 2 -> new int[]{0b01};
            case 4 -> timeVariance < 0.55
                    ? new int[]{0b0001, 0b0101, 0b1001}
                    : new int[]{0b0001, 0b0101, 0b1001, 0b1010};
            default -> switch (style) {
                case CALL_RESPONSE -> new int[]{0b00000001, 0b00001001, 0b00100010, 0b10001000};
                case ACID -> new int[]{0b00000001, 0b00001001, 0b00100010};
                case MOTIF -> new int[]{0b00000001, 0b00010001, 0b01000100};
                case ROLLING -> new int[]{0b00000001, 0b00100010, 0b10001000};
                case OCTAVE -> new int[]{0b00000001, 0b00001010, 0b01000100};
            };
        };
        return masks[random.nextInt(masks.length)];
    }

    private static MelodicPattern maybeInjectOrnament(final MelodicPattern pattern, final MelodicPhraseContext context,
                                                      final Style style, final int span, final Random random,
                                                      final double timeVariance) {
        final List<Integer> ornamentSlots = new ArrayList<>();
        for (int i = 1; i < pattern.loopSteps() - 1; i++) {
            if (pattern.step(i).active()) {
                continue;
            }
            final MelodicPattern.Step previous = pattern.step(i - 1);
            final MelodicPattern.Step next = pattern.step(i + 1);
            if (!previous.active() || previous.pitch() == null || !next.active() || next.pitch() == null) {
                continue;
            }
            if (i % 4 == 0) {
                continue;
            }
            ornamentSlots.add(i);
        }
        if (ornamentSlots.isEmpty()) {
            return pattern;
        }
        final int slot = ornamentSlots.get(random.nextInt(ornamentSlots.size()));
        final MelodicPattern.Step previous = pattern.step(slot - 1);
        final MelodicPattern.Step next = pattern.step(slot + 1);
        final int lower = Math.min(previous.pitch(), next.pitch());
        final int upper = Math.max(previous.pitch(), next.pitch());
        final int pitch = lower == upper
                ? context.pitchForDegree(0, 1)
                : lower + Math.max(1, (upper - lower) / 2);
        final int mask = chooseOrnamentMask(span, style, timeVariance, random);
        return pattern.withStep(new MelodicPattern.Step(slot, true, false, pitch, 92, 0.55, false, false)
                .withRecurrence(span, mask));
    }

    private static int chooseOrnamentMask(final int span, final Style style, final double timeVariance,
                                          final Random random) {
        if (span <= 2) {
            return 0b01;
        }
        if (span == 4) {
            return switch (style) {
                case CALL_RESPONSE -> new int[]{0b0001, 0b1001}[random.nextInt(2)];
                case ACID, ROLLING -> new int[]{0b0001, 0b0101}[random.nextInt(2)];
                case MOTIF, OCTAVE -> new int[]{0b0001, 0b0010}[random.nextInt(2)];
            };
        }
        return switch (style) {
            case CALL_RESPONSE -> new int[]{0b00001001, 0b10000001, 0b00100010}[random.nextInt(3)];
            case ACID -> new int[]{0b00001001, 0b00100010, 0b10100000}[random.nextInt(3)];
            case MOTIF -> new int[]{0b00010001, 0b01000001, 0b00100100}[random.nextInt(3)];
            case ROLLING -> new int[]{0b00001001, 0b10001000, 0b00100010}[random.nextInt(3)];
            case OCTAVE -> new int[]{0b00000101, 0b01000100, 0b00100010}[random.nextInt(3)];
        };
    }

    private static boolean shouldSkipAcidPulse(final int stepIndex, final int loopSteps) {
        return stepIndex < Math.max(4, loopSteps / 4);
    }

    private static boolean isAcidHookWindow(final int stepIndex, final int loopSteps) {
        final int start = loopSteps / 4;
        final int end = loopSteps / 2;
        return stepIndex >= start && stepIndex < end;
    }

    private static boolean isAcidLateWindow(final int stepIndex, final int loopSteps) {
        return stepIndex >= Math.max(loopSteps / 2, loopSteps - Math.max(4, loopSteps / 4));
    }

    private record RoleBuckets(List<Integer> themeSteps, List<Integer> alternateSteps) {
    }
}
