package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.BiColorLightState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChordStepPitchContextControlsTest {
    @Test
    void rootButtonAdjustsChordRoot() {
        final Host host = new Host();
        final ChordStepPitchContextControls controls = new ChordStepPitchContextControls(host);

        controls.handlePressed(true, 1, true);

        assertEquals(1, host.rootAmount);
        assertEquals(0, host.octaveAmount);
    }

    @Test
    void octaveButtonAdjustsChordOctave() {
        final Host host = new Host();
        final ChordStepPitchContextControls controls = new ChordStepPitchContextControls(host);

        controls.handlePressed(true, -1, false);

        assertEquals(0, host.rootAmount);
        assertEquals(-1, host.octaveAmount);
    }

    @Test
    void octaveLightsFollowAvailableDirections() {
        final Host host = new Host();
        final ChordStepPitchContextControls controls = new ChordStepPitchContextControls(host);

        assertEquals(BiColorLightState.OFF, controls.lightState(-1, false));
        assertEquals(BiColorLightState.OFF, controls.lightState(1, false));

        host.canLower = true;
        host.canRaise = true;

        assertEquals(BiColorLightState.AMBER_HALF, controls.lightState(-1, false));
        assertEquals(BiColorLightState.AMBER_HALF, controls.lightState(1, false));
    }

    @Test
    void rootLightIsAlwaysAvailable() {
        final ChordStepPitchContextControls controls = new ChordStepPitchContextControls(new Host());

        assertEquals(BiColorLightState.AMBER_HALF, controls.lightState(1, true));
    }

    private static final class Host implements ChordStepPitchContextControls.Host {
        private int rootAmount;
        private int octaveAmount;
        private boolean canLower;
        private boolean canRaise;

        @Override
        public void adjustRoot(final int amount) {
            rootAmount = amount;
        }

        @Override
        public void adjustOctave(final int amount) {
            octaveAmount = amount;
        }

        @Override
        public boolean canLowerOctave() {
            return canLower;
        }

        @Override
        public boolean canRaiseOctave() {
            return canRaise;
        }
    }
}
