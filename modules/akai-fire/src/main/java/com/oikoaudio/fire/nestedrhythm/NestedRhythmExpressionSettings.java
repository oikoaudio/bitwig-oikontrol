package com.oikoaudio.fire.nestedrhythm;

record NestedRhythmExpressionSettings(double pressureCenter,
                                      double pressureSpread,
                                      int pressureRotation,
                                      double timbreCenter,
                                      double timbreSpread,
                                      int timbreRotation,
                                      double pitchExpressionCenter,
                                      double pitchExpressionSpread,
                                      int pitchExpressionRotation,
                                      double chanceBaseline,
                                      double chancePlayProbability,
                                      int chanceRotation,
                                      double recurrenceDepth,
                                      double clipStepSize) {
}
