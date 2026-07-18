package com.oikoaudio.fire.multiclip;

import com.oikoaudio.fire.sequence.EncoderMode;

record MulticlipEncoderTarget(Kind kind, int parameterIndex) {
    static final MulticlipEncoderTarget NONE = new MulticlipEncoderTarget(Kind.NONE, -1);

    enum Kind {
        NONE,
        CLIP_LENGTH,
        PLAY_START,
        PAD_MIXER,
        PAD_REMOTE
    }

    static MulticlipEncoderTarget resolve(final EncoderMode mode, final int encoderIndex) {
        if (encoderIndex < 0 || encoderIndex >= 4) {
            return NONE;
        }
        return switch (mode) {
            case CHANNEL ->
                    switch (encoderIndex) {
                        case 0 -> new MulticlipEncoderTarget(Kind.CLIP_LENGTH, 0);
                        case 1 -> new MulticlipEncoderTarget(Kind.PLAY_START, 0);
                        default -> NONE;
                    };
            case MIXER -> new MulticlipEncoderTarget(Kind.PAD_MIXER, encoderIndex);
            case USER_1 -> new MulticlipEncoderTarget(Kind.PAD_REMOTE, encoderIndex);
            case USER_2 -> new MulticlipEncoderTarget(Kind.PAD_REMOTE, encoderIndex + 4);
        };
    }
}
