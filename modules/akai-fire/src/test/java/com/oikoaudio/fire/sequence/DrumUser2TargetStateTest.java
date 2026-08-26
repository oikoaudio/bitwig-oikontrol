package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DrumUser2TargetStateTest {
    @Test
    void cyclesPadKitAndEuclidTargets() {
        final DrumUser2TargetState state = new DrumUser2TargetState();

        assertEquals(DrumUser2TargetState.Target.PAD_REMOTES, state.target());
        assertEquals(DrumUser2TargetState.Target.KIT_REMOTES, state.cycle());
        assertEquals(DrumUser2TargetState.Target.EUCLID, state.cycle());
        assertEquals(DrumUser2TargetState.Target.PAD_REMOTES, state.cycle());
    }

    @Test
    void altSelectsSecondHalfOfRemotePage() {
        assertEquals(0, DrumUser2TargetState.remoteParameterIndex(0, false));
        assertEquals(3, DrumUser2TargetState.remoteParameterIndex(3, false));
        assertEquals(4, DrumUser2TargetState.remoteParameterIndex(0, true));
        assertEquals(7, DrumUser2TargetState.remoteParameterIndex(3, true));
    }
}
