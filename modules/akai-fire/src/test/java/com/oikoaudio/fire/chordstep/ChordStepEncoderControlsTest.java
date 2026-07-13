package com.oikoaudio.fire.chordstep;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bitwig.extension.controller.api.CursorTrack;
import com.oikoaudio.fire.MainEncoderRouting;
import com.oikoaudio.fire.display.OledDisplay;
import org.junit.jupiter.api.Test;

class ChordStepEncoderControlsTest {
    @Test
    void touchingChordSetEncoderShowsSetInfoInsteadOfCurrentChordOverlay() {
        final ChordStepEncoderControls.Host host = mock(ChordStepEncoderControls.Host.class);
        final ChordStepEncoderControls controls =
                new ChordStepEncoderControls(
                        mock(MainEncoderRouting.class),
                        mock(OledDisplay.class),
                        mock(CursorTrack.class),
                        host);

        controls.handleChordFamilyTouched(true);

        verify(host).showChordFamilyInfo();
    }

    @Test
    void rotatingChordSetEncoderShowsSetInfoInsteadOfCurrentChordOverlay() {
        final ChordStepEncoderControls.Host host = mock(ChordStepEncoderControls.Host.class);
        final ChordStepEncoderControls controls =
                new ChordStepEncoderControls(
                        mock(MainEncoderRouting.class),
                        mock(OledDisplay.class),
                        mock(CursorTrack.class),
                        host);

        controls.handleChordFamilyTurn(8);

        verify(host).adjustChordFamily(1);
        verify(host).showChordFamilyInfo();
    }
}
