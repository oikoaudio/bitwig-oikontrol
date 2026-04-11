package com.oikoaudio.fire.melodic;

public interface MelodicGenerator {
    MelodicPattern generate(MelodicPhraseContext context, GenerateParameters parameters);

    default String lastFamilyLabel() {
        return "";
    }

    default boolean supportsSubtypeSelection() {
        return false;
    }

    default void cycleSubtype(final int direction) {
    }

    default String currentSubtypeLabel() {
        return "Any";
    }

    record GenerateParameters(int loopSteps, double density, double tension, double octaveActivity,
                              int pulses, int rotation, long seed) {
    }
}
