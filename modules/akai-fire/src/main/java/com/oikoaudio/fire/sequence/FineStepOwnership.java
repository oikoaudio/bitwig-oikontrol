package com.oikoaudio.fire.sequence;

/** Maps a fine-grid note start to the single nearest coarse step in a looping sequence. */
public final class FineStepOwnership {
    private FineStepOwnership() {}

    /**
     * Returns the coarse step nearest to {@code fineStart}. Exact midpoint ties belong to the
     * following coarse step.
     */
    public static int ownerOf(
            final int fineStart, final int fineStepsPerStep, final int loopSteps) {
        requirePositive(fineStepsPerStep, "fineStepsPerStep");
        requirePositive(loopSteps, "loopSteps");
        final int loopFineSteps = Math.multiplyExact(fineStepsPerStep, loopSteps);
        final int normalizedFineStart = Math.floorMod(fineStart, loopFineSteps);
        final int nearestStep =
                Math.floorDiv(normalizedFineStart + fineStepsPerStep / 2, fineStepsPerStep);
        return Math.floorMod(nearestStep, loopSteps);
    }

    public static boolean isOwnedBy(
            final int fineStart,
            final int coarseStep,
            final int fineStepsPerStep,
            final int loopSteps) {
        return ownerOf(fineStart, fineStepsPerStep, loopSteps)
                == Math.floorMod(coarseStep, loopSteps);
    }

    private static void requirePositive(final int value, final String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
