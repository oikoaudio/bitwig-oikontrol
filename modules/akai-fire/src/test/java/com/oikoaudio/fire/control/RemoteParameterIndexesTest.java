package com.oikoaudio.fire.control;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RemoteParameterIndexesTest {
    @Test
    void altSelectsSecondFourRemoteParametersOnCurrentPage() {
        assertEquals(0, RemoteParameterIndexes.parameterIndex(0, false));
        assertEquals(3, RemoteParameterIndexes.parameterIndex(3, false));
        assertEquals(4, RemoteParameterIndexes.parameterIndex(0, true));
        assertEquals(7, RemoteParameterIndexes.parameterIndex(3, true));
    }
}
