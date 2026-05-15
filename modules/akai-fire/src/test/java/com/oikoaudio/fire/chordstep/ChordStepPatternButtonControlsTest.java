package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.BiColorLightState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChordStepPatternButtonControlsTest {
    @Test
    void patternUpPagesLeftUnlessAltIsHeld() {
        final Host host = new Host();
        final ChordStepPatternButtonControls controls = new ChordStepPatternButtonControls(host);

        controls.handleUpPressed(true);
        host.alt = true;
        controls.handleUpPressed(true);

        assertEquals(-1, host.pageDirectionTotal);
    }

    @Test
    void patternDownPagesRightUnlessAltIsHeld() {
        final Host host = new Host();
        final ChordStepPatternButtonControls controls = new ChordStepPatternButtonControls(host);

        controls.handleDownPressed(true);
        host.alt = true;
        controls.handleDownPressed(true);

        assertEquals(1, host.pageDirectionTotal);
    }

    @Test
    void lightsFollowAvailablePages() {
        final Host host = new Host();
        final ChordStepPatternButtonControls controls = new ChordStepPatternButtonControls(host);

        assertEquals(BiColorLightState.OFF, controls.upLight());
        assertEquals(BiColorLightState.OFF, controls.downLight());

        host.canLeft = true;
        host.canRight = true;

        assertEquals(BiColorLightState.GREEN_HALF, controls.upLight());
        assertEquals(BiColorLightState.GREEN_HALF, controls.downLight());
    }

    private static final class Host implements ChordStepPatternButtonControls.Host {
        private boolean alt;
        private boolean canLeft;
        private boolean canRight;
        private int pageDirectionTotal;

        @Override
        public boolean isAltHeld() {
            return alt;
        }

        @Override
        public void page(final int direction) {
            pageDirectionTotal += direction;
        }

        @Override
        public boolean canPageLeft() {
            return canLeft;
        }

        @Override
        public boolean canPageRight() {
            return canRight;
        }
    }
}
