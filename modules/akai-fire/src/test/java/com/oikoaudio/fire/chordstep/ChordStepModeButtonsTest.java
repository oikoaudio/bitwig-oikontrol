package com.oikoaudio.fire.chordstep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChordStepModeButtonsTest {
    @Test
    void stepPressPrioritizesHeldStepAccent() {
        final Host host = new Host();
        host.heldSteps = true;

        controls(host).handleStepPressed(true);

        assertTrue(host.toggledHeldAccent);
        assertEquals(0, host.stepModeEntries);
    }

    @Test
    void altStepPressTogglesFillAndShowsFeedback() {
        final Host host = new Host();
        host.alt = true;
        host.fillActive = true;

        controls(host).handleStepPressed(true);

        assertTrue(host.fillToggled);
        assertEquals("Fill", host.lastTitle);
        assertEquals("On", host.lastValue);
    }

    @Test
    void patternButtonsPreserveModifierPrecedenceAndPaging() {
        final Host host = new Host();
        host.canLeft = true;
        host.canRight = true;
        final ChordStepModeButtons controls = controls(host);

        controls.handlePatternUpPressed(true);
        controls.handlePatternDownPressed(true);
        host.shift = true;
        controls.handlePatternDownPressed(true);
        controls.handlePatternUpPressed(true);
        host.alt = true;
        controls.handlePatternUpPressed(true);

        assertEquals(0, host.pageDirectionTotal);
        assertEquals(List.of(false, true), host.latchStates);
    }

    @Test
    void patternButtonsShowPageInfoAndLightsReflectAvailability() {
        final Host host = new Host();
        final ChordStepModeButtons controls = controls(host);

        controls.handlePatternUpPressed(true);
        controls.handlePatternDownPressed(true);

        assertEquals(2, host.pageInfoCount);
        assertEquals(BiColorLightState.OFF, controls.patternUpLight());
        assertEquals(BiColorLightState.OFF, controls.patternDownLight());

        host.canLeft = true;
        host.canRight = true;
        assertEquals(BiColorLightState.GREEN_HALF, controls.patternUpLight());
        assertEquals(BiColorLightState.GREEN_HALF, controls.patternDownLight());
    }

    @Test
    void pitchContextButtonsAdjustRootAndOctaveAndReportAvailability() {
        final Host host = new Host();
        final ChordStepModeButtons controls = controls(host);

        controls.handlePitchContextPressed(true, 1, true);
        controls.handlePitchContextPressed(true, -1, false);

        assertEquals(1, host.rootAmount);
        assertEquals(-1, host.octaveAmount);
        assertEquals(BiColorLightState.AMBER_HALF, controls.pitchContextLight(1, true));
        assertEquals(BiColorLightState.OFF, controls.pitchContextLight(-1, false));
        host.canLower = true;
        assertEquals(BiColorLightState.AMBER_HALF, controls.pitchContextLight(-1, false));
    }

    private static ChordStepModeButtons controls(final Host host) {
        return new ChordStepModeButtons(new ChordStepAccentControls(mock(OledDisplay.class)), host);
    }

    private static final class Host implements ChordStepModeButtons.Host {
        private boolean heldSteps;
        private boolean shift;
        private boolean alt;
        private boolean fillActive;
        private boolean fillToggled;
        private boolean toggledHeldAccent;
        private boolean canLeft;
        private boolean canRight;
        private boolean canLower;
        private int stepModeEntries;
        private int pageDirectionTotal;
        private int pageInfoCount;
        private int rootAmount;
        private int octaveAmount;
        private String lastTitle = "";
        private String lastValue = "";
        private final List<Boolean> latchStates = new ArrayList<>();

        @Override
        public boolean hasHeldSteps() {
            return heldSteps;
        }

        @Override
        public void toggleAccentForHeldSteps() {
            toggledHeldAccent = true;
        }

        @Override
        public boolean isShiftHeld() {
            return shift;
        }

        @Override
        public boolean isAltHeld() {
            return alt;
        }

        @Override
        public boolean isStandaloneChordStepSurface() {
            return true;
        }

        @Override
        public void enterPlainStepPressTarget() {
            stepModeEntries++;
        }

        @Override
        public void toggleFillMode() {
            fillToggled = true;
        }

        @Override
        public boolean isFillModeActive() {
            return fillActive;
        }

        @Override
        public void showValueInfo(final String title, final String value) {
            lastTitle = title;
            lastValue = value;
        }

        @Override
        public BiColorLightState stepFillLightState() {
            return BiColorLightState.AMBER_HALF;
        }

        @Override
        public BiColorLightState modeButtonLightState() {
            return BiColorLightState.AMBER_FULL;
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
            return false;
        }
    }
}
