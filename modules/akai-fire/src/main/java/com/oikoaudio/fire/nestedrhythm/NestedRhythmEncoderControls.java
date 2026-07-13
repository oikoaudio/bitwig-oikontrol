package com.oikoaudio.fire.nestedrhythm;

import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.sequence.EncoderBankLayout;
import com.oikoaudio.fire.sequence.StepSequencerEncoderLayer;
import com.oikoaudio.fire.sequence.StepSequencerHost;

import java.util.function.Supplier;

/** Retained owner of Nested Rhythm's mode-specific encoder banks and physical encoder layer. */
final class NestedRhythmEncoderControls {
    private final EncoderBankLayout layout;
    private final StepSequencerEncoderLayer layer;

    NestedRhythmEncoderControls(final StepSequencerHost host, final AkaiFireOikontrolExtension driver,
                                final Supplier<EncoderBankLayout> bankFactory) {
        layout = bankFactory.get();
        layer = new StepSequencerEncoderLayer(host, driver);
    }

    EncoderBankLayout layout() {
        return layout;
    }

    void activate() {
        layer.activate();
    }

    void deactivate() {
        layer.deactivate();
    }
}
