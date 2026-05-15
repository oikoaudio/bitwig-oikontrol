package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.display.OledDisplay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChordStepAccentControlsTest {
    @Test
    void latchesAccentModeOnUnmodifiedPressRelease() {
        final OledDisplay oled = mock(OledDisplay.class);
        final ChordStepAccentControls controls = new ChordStepAccentControls(oled);

        controls.handlePressed(true);
        controls.handlePressed(false);

        assertTrue(controls.isActive());
        verify(oled).valueInfo("Accent Mode", "Off");
        verify(oled).valueInfo("Accent Mode", "On");
    }

    @Test
    void modifiedHoldDoesNotToggleAccentModeOnRelease() {
        final OledDisplay oled = mock(OledDisplay.class);
        final ChordStepAccentControls controls = new ChordStepAccentControls(oled);

        controls.handlePressed(true);
        controls.markModified();
        controls.handlePressed(false);

        assertFalse(controls.isActive());
        verify(oled).clearScreenDelayed();
    }
}
