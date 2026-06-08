package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.BiColorLightState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChordStepPatternButtonControlsTest {
    @Test
    void patternUpPagesLeftUnlessAltIsHeld() {
        final Host host = new Host();
        host.canLeft = true;
        final ChordStepPatternButtonControls controls = new ChordStepPatternButtonControls(host);

        controls.handleUpPressed(true);
        host.alt = true;
        controls.handleUpPressed(true);

        assertEquals(-1, host.pageDirectionTotal);
    }

    @Test
    void patternDownPagesRightUnlessAltIsHeld() {
        final Host host = new Host();
        host.canRight = true;
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

    @Test
    void patternButtonsShowPageInfoWhenOnlyOnePageExists() {
        final Host host = new Host();
        final ChordStepPatternButtonControls controls = new ChordStepPatternButtonControls(host);

        controls.handleUpPressed(true);
        controls.handleDownPressed(true);

        assertEquals(0, host.pageDirectionTotal);
        assertEquals(2, host.pageInfoCount);
    }

    @Test
    void shiftedPatternButtonsSetBuilderLatchWithoutPaging() {
        final Host host = new Host();
        host.shift = true;
        host.canLeft = true;
        host.canRight = true;
        final ChordStepPatternButtonControls controls = new ChordStepPatternButtonControls(host);

        controls.handleDownPressed(true);
        controls.handleUpPressed(true);

        assertEquals(0, host.pageDirectionTotal);
        assertEquals(false, host.latchStates.get(0));
        assertEquals(true, host.latchStates.get(1));
    }

    @Test
    void altStillBlocksShiftedPatternButtons() {
        final Host host = new Host();
        host.alt = true;
        host.shift = true;
        final ChordStepPatternButtonControls controls = new ChordStepPatternButtonControls(host);

        controls.handleDownPressed(true);
        controls.handleUpPressed(true);

        assertEquals(0, host.pageDirectionTotal);
        assertEquals(0, host.latchStates.size());
    }

    private static final class Host implements ChordStepPatternButtonControls.Host {
        private boolean alt;
        private boolean shift;
        private boolean canLeft;
        private boolean canRight;
        private int pageDirectionTotal;
        private int pageInfoCount;
        private final java.util.List<Boolean> latchStates = new java.util.ArrayList<>();

        @Override
        public boolean isAltHeld() {
            return alt;
        }

        @Override
        public boolean isShiftHeld() {
            return shift;
        }

        @Override
        public void setBuilderLatchEnabled(final boolean enabled) {
            latchStates.add(enabled);
        }

        @Override
        public void page(final int direction) {
            pageDirectionTotal += direction;
        }

        @Override
        public void showPageInfo() {
            pageInfoCount++;
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
