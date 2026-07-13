package com.oikoaudio.fire.control;

public final class RemoteParameterIndexes {
    private static final int ALTERNATE_OFFSET = 4;

    private RemoteParameterIndexes() {}

    public static int parameterIndex(final int encoderIndex, final boolean altHeld) {
        return (altHeld ? ALTERNATE_OFFSET : 0) + encoderIndex;
    }
}
