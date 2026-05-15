package com.oikoaudio.fire.sequence;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.control.TouchEncoder;

public interface EncoderSlotBinding {
    double stepSize();

    void bind(StepSequencerEncoderLayer encoderLayer, Layer layer, TouchEncoder encoder, int slotIndex);
}
