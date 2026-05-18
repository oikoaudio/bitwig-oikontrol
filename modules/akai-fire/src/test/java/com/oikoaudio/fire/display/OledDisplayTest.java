package com.oikoaudio.fire.display;

import com.bitwig.extension.controller.api.MidiOut;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OledDisplayTest {

    @Test
    void sendImageOnlyResendsChangedPages() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);
        final int[] image = new int[1024];

        display.sendImage(image);

        verify(midiOut, times(8)).sendSysex(any(byte[].class));
        reset(midiOut);

        display.sendImage(image);

        verifyNoInteractions(midiOut);

        image[7 * 128 + 12] = 1;
        display.sendImage(image);

        final ArgumentCaptor<byte[]> sysex = ArgumentCaptor.forClass(byte[].class);
        verify(midiOut).sendSysex(sysex.capture());
        assertEquals(7, sysex.getValue()[7]);
        assertEquals(7, sysex.getValue()[8]);
    }

    @Test
    void textWritesInvalidateImageCache() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);
        final int[] image = new int[1024];

        display.sendImage(image);
        reset(midiOut);

        display.valueInfoNoClear("Volume", "0.0 dB");
        reset(midiOut);

        display.sendImage(image);

        verify(midiOut, times(8)).sendSysex(any(byte[].class));
    }
}
