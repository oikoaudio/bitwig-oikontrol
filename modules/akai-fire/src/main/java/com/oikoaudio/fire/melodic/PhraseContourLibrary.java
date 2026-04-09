package com.oikoaudio.fire.melodic;

import java.util.Random;

final class PhraseContourLibrary {
    enum ContourAction {
        REST,
        ANCHOR,
        CADENCE,
        REPEAT,
        PICKUP,
        NEIGHBOR,
        STEP,
        LEAP,
        ORNAMENT,
        OCTAVE
    }

    record ContourTemplate(String name, ContourAction[] actions, int[] anchorDegrees, int[] coreDegrees,
                           int[] colorDegrees, int[] cadenceDegrees) {
        ContourTemplate {
            if (actions.length != 16) {
                throw new IllegalArgumentException("Contour templates must be 16 steps");
            }
        }
    }

    record ContourBlueprint(ContourTemplate template, ContourAction[] actions) {
    }

    private static final ContourTemplate[] ACID_BASES = {
            new ContourTemplate(
                    "FundamentalPulse",
                    new ContourAction[]{
                            ContourAction.ANCHOR, ContourAction.PICKUP, ContourAction.NEIGHBOR, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.REPEAT, ContourAction.REST, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.PICKUP, ContourAction.NEIGHBOR, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.OCTAVE, ContourAction.CADENCE, ContourAction.REST
                    },
                    new int[]{0, 0, 0, 2},
                    new int[]{0, 1, 2, 4},
                    new int[]{1, 2, 3, 5},
                    new int[]{0, 0, 2}),
            new ContourTemplate(
                    "RootRoller",
                    new ContourAction[]{
                            ContourAction.ANCHOR, ContourAction.PICKUP, ContourAction.NEIGHBOR, ContourAction.REPEAT,
                            ContourAction.REST, ContourAction.ANCHOR, ContourAction.PICKUP, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.PICKUP, ContourAction.NEIGHBOR, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.OCTAVE, ContourAction.CADENCE, ContourAction.REST
                    },
                    new int[]{0, 0, 2},
                    new int[]{0, 1, 2, 4},
                    new int[]{1, 2, 3, 5},
                    new int[]{0, 2}),
            new ContourTemplate(
                    "OctaveLead",
                    new ContourAction[]{
                            ContourAction.ANCHOR, ContourAction.PICKUP, ContourAction.OCTAVE, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.NEIGHBOR, ContourAction.REST, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.PICKUP, ContourAction.OCTAVE, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.NEIGHBOR, ContourAction.CADENCE, ContourAction.REST
                    },
                    new int[]{0, 0, 2},
                    new int[]{0, 1, 2, 4},
                    new int[]{1, 2, 3, 5},
                    new int[]{0, 2}),
            new ContourTemplate(
                    "SupportCadence",
                    new ContourAction[]{
                            ContourAction.ANCHOR, ContourAction.PICKUP, ContourAction.REST, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.LEAP, ContourAction.REST, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.NEIGHBOR, ContourAction.REPEAT, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.OCTAVE, ContourAction.CADENCE, ContourAction.REST
                    },
                    new int[]{0, 0, 2, 4},
                    new int[]{0, 1, 2, 4, 5},
                    new int[]{1, 2, 3, 5, 6},
                    new int[]{0, 2, 4}),
            new ContourTemplate(
                    "DoubleTimeNibble",
                    new ContourAction[]{
                            ContourAction.ANCHOR, ContourAction.PICKUP, ContourAction.NEIGHBOR, ContourAction.REPEAT,
                            ContourAction.REST, ContourAction.REST, ContourAction.ANCHOR, ContourAction.PICKUP,
                            ContourAction.NEIGHBOR, ContourAction.REPEAT, ContourAction.REST, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.OCTAVE, ContourAction.CADENCE, ContourAction.REST
                    },
                    new int[]{0, 0, 2},
                    new int[]{0, 1, 2, 4},
                    new int[]{1, 2, 3, 5},
                    new int[]{0, 2})
    };

    private static final ContourAction[][] ACID_OVERLAYS = {
            new ContourAction[]{
                    ContourAction.REST, ContourAction.REPEAT, ContourAction.NEIGHBOR, ContourAction.REST,
                    ContourAction.REST, ContourAction.PICKUP, ContourAction.REST, ContourAction.REST,
                    ContourAction.REST, ContourAction.REPEAT, ContourAction.NEIGHBOR, ContourAction.REST,
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST
            },
            new ContourAction[]{
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST,
                    ContourAction.REST, ContourAction.REST, ContourAction.OCTAVE, ContourAction.REST,
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST,
                    ContourAction.REST, ContourAction.REST, ContourAction.OCTAVE, ContourAction.REST
            },
            new ContourAction[]{
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST,
                    ContourAction.REST, ContourAction.REPEAT, ContourAction.REPEAT, ContourAction.REST,
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST,
                    ContourAction.REST, ContourAction.REPEAT, ContourAction.REST, ContourAction.REST
            },
            new ContourAction[]{
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST,
                    ContourAction.REST, ContourAction.LEAP, ContourAction.REST, ContourAction.REST,
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST,
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST
            }
    };

    private static final ContourTemplate[] MOTIF_BASES = {
            new ContourTemplate(
                    "RepeatAnswer",
                    new ContourAction[]{
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.STEP, ContourAction.REPEAT,
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.PICKUP, ContourAction.REPEAT,
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.STEP, ContourAction.REPEAT,
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.CADENCE, ContourAction.REPEAT
                    },
                    new int[]{0, 2, 4},
                    new int[]{0, 1, 2, 3, 4, 5},
                    new int[]{1, 3, 5, 6},
                    new int[]{0, 2, 4}),
            new ContourTemplate(
                    "CallResponse",
                    new ContourAction[]{
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.NEIGHBOR, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.LEAP, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.NEIGHBOR, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.CADENCE, ContourAction.REST
                    },
                    new int[]{0, 2, 4},
                    new int[]{0, 1, 2, 4, 5},
                    new int[]{1, 3, 6},
                    new int[]{0, 2}),
            new ContourTemplate(
                    "StaticPerturb",
                    new ContourAction[]{
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.REPEAT, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.NEIGHBOR, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.REPEAT, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.CADENCE, ContourAction.ORNAMENT
                    },
                    new int[]{0, 2, 4},
                    new int[]{0, 2, 3, 4, 5},
                    new int[]{1, 3, 6},
                    new int[]{0, 2, 4}),
            new ContourTemplate(
                    "WidePunct",
                    new ContourAction[]{
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.LEAP, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.REPEAT, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.OCTAVE, ContourAction.REST,
                            ContourAction.ANCHOR, ContourAction.REST, ContourAction.CADENCE, ContourAction.REST
                    },
                    new int[]{0, 2, 4},
                    new int[]{0, 2, 4, 5, 7},
                    new int[]{1, 3, 6, 7},
                    new int[]{0, 2})
    };

    private static final ContourAction[][] MOTIF_OVERLAYS = {
            new ContourAction[]{
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST,
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST,
                    ContourAction.REST, ContourAction.REST, ContourAction.REPEAT, ContourAction.REST,
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST
            },
            new ContourAction[]{
                    ContourAction.REST, ContourAction.REST, ContourAction.PICKUP, ContourAction.REST,
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST,
                    ContourAction.REST, ContourAction.REST, ContourAction.PICKUP, ContourAction.REST,
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST
            },
            new ContourAction[]{
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.ORNAMENT,
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST,
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.ORNAMENT,
                    ContourAction.REST, ContourAction.REST, ContourAction.REST, ContourAction.REST
            }
    };

    private PhraseContourLibrary() {
    }

    static ContourBlueprint acidBlueprint(final int loopSteps, final Random random, final double density,
                                          final double tension) {
        return compose(loopSteps, random, density, tension, ACID_BASES, ACID_OVERLAYS);
    }

    static ContourBlueprint motifBlueprint(final int loopSteps, final Random random, final double density,
                                           final double tension) {
        return compose(loopSteps, random, density, tension, MOTIF_BASES, MOTIF_OVERLAYS);
    }

    private static ContourBlueprint compose(final int loopSteps, final Random random, final double density,
                                            final double tension, final ContourTemplate[] templates,
                                            final ContourAction[][] overlays) {
        final ContourTemplate template = templates[random.nextInt(templates.length)];
        final ContourAction[] overlay = overlays[random.nextInt(overlays.length)];
        final ContourAction[] actions = new ContourAction[loopSteps];
        final int densityTier = densityTier(density);
        final double overlayChance = 0.28 + tension * 0.42;
        for (int step = 0; step < loopSteps; step++) {
            final ContourAction base = template.actions()[step % template.actions().length];
            final ContourAction detail = overlay[step % overlay.length];
            final boolean preserveBase = base == ContourAction.ANCHOR || base == ContourAction.CADENCE;
            ContourAction action = base;
            if (!preserveBase && detail != ContourAction.REST && random.nextDouble() < overlayChance) {
                action = detail;
            }
            if (!retainedByDensity(action, densityTier, step, loopSteps)) {
                action = ContourAction.REST;
            }
            actions[step] = action;
        }
        if (loopSteps > 0) {
            actions[0] = ContourAction.ANCHOR;
            if (loopSteps > 1) {
                actions[loopSteps - 1] = ContourAction.CADENCE;
            }
        }
        return new ContourBlueprint(template, actions);
    }

    private static int densityTier(final double density) {
        return Math.max(0, Math.min(6, (int) Math.round(density * 6.0)));
    }

    private static boolean retainedByDensity(final ContourAction action, final int densityTier, final int step,
                                             final int loopSteps) {
        if (action == ContourAction.ANCHOR || action == ContourAction.CADENCE) {
            return true;
        }
        if (step == 0 || step == loopSteps - 1) {
            return true;
        }
        return densityTier >= switch (action) {
            case REST -> 7;
            case STEP -> 1;
            case REPEAT -> 2;
            case PICKUP -> 3;
            case NEIGHBOR -> 3;
            case LEAP -> 3;
            case OCTAVE -> 4;
            case ORNAMENT -> 6;
            default -> 0;
        };
    }
}
