package com.oikoaudio.fire.melodic;

public interface MelodicGenerator {
    MelodicPattern generate(MelodicPhraseContext context, GenerateParameters parameters);

    default String lastFamilyLabel() {
        return "";
    }

    record GenerateParameters(int loopSteps, double density, double tension, double octaveActivity,
                              int pulses, int rotation, long seed) {
    }
}
