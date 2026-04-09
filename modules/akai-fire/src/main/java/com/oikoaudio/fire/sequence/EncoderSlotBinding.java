package com.oikoaudio.fire.sequence;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.control.TouchEncoder;

public interface EncoderSlotBinding {
    double stepSize();

    void bind(StepSequencerEncoderHandler handler, Layer layer, TouchEncoder encoder, int slotIndex);
}
