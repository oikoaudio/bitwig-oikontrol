package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ChordStepStepButtonControlsTest {
    @Test
    void heldStepPressTogglesHeldAccentInsteadOfChangingMode() {
        final Host host = new Host();
        host.heldSteps = true;
        final ChordStepStepButtonControls controls = controls(host);

        controls.handlePressed(true);

        assertTrue(host.toggledHeldAccent);
        assertEquals(0, host.fugueEntries);
        assertEquals(0, host.melodicEntries);
    }

    @Test
    void normalPressAdvancesStandaloneChordStepToFugue() {
        final Host host = new Host();
        host.standalone = true;
        final ChordStepStepButtonControls controls = controls(host);

        controls.handlePressed(true);

        assertEquals(1, host.fugueEntries);
        assertEquals(0, host.melodicEntries);
    }

    @Test
    void normalPressReturnsEmbeddedChordStepToMelodicStep() {
        final Host host = new Host();
        final ChordStepStepButtonControls controls = controls(host);

        controls.handlePressed(true);

        assertEquals(0, host.fugueEntries);
        assertEquals(1, host.melodicEntries);
    }

    @Test
    void altPressTogglesFillAndShowsFeedback() {
        final Host host = new Host();
        host.alt = true;
        host.fillActive = true;
        final ChordStepStepButtonControls controls = controls(host);

        controls.handlePressed(true);

        assertTrue(host.fillToggled);
        assertEquals("Fill", host.lastTitle);
        assertEquals("On", host.lastValue);
    }

    @Test
    void normalStandaloneLightUsesModeButtonLight() {
        final Host host = new Host();
        host.standalone = true;
        host.modeLight = BiColorLightState.AMBER_FULL;
        final ChordStepStepButtonControls controls = controls(host);

        assertEquals(BiColorLightState.AMBER_FULL, controls.lightState(true));
    }

    private static ChordStepStepButtonControls controls(final Host host) {
        return new ChordStepStepButtonControls(new ChordStepAccentControls(mock(OledDisplay.class)), host);
    }

    private static final class Host implements ChordStepStepButtonControls.Host {
        private boolean heldSteps;
        private boolean alt;
        private boolean standalone;
        private boolean fillActive;
        private boolean fillToggled;
        private boolean toggledHeldAccent;
        private int fugueEntries;
        private int melodicEntries;
        private String lastTitle = "";
        private String lastValue = "";
        private BiColorLightState modeLight = BiColorLightState.OFF;

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
            return false;
        }

        @Override
        public boolean isAltHeld() {
            return alt;
        }

        @Override
        public boolean isStandaloneChordStepSurface() {
            return standalone;
        }

        @Override
        public void enterFugueStepMode() {
            fugueEntries++;
        }

        @Override
        public void enterMelodicStepMode() {
            melodicEntries++;
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
            return modeLight;
        }
    }
}
