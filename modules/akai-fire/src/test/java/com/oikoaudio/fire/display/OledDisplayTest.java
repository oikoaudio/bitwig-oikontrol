package com.oikoaudio.fire.display;

import com.bitwig.extension.controller.api.MidiOut;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
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

    @Test
    void barWritesCanBeFollowedByTextWithoutClearingGraphics() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);

        display.showBar(false, 126, 1, OledDisplay.Fill.Fifty, OledDisplay.Fill.Empty, 6, 0, 64);
        display.sendString(0, OledDisplay.TextJustification.LEFT, 0, "Peak          RMS");

        verify(midiOut, times(2)).sendSysex(any(byte[].class));
    }

    @Test
    void highLevelTextMarksPendingTransientUntilGraphicsReturn() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);
        display.setClearDelayMs(10_000);

        display.valueInfo("Tempo", "120");

        assertTrue(display.hasPendingTransientMessage());

        display.sendImage(new int[1024]);

        assertFalse(display.hasPendingTransientMessage());
    }

    @Test
    void persistentTextCancelsPendingTransientAndClear() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);
        display.setClearDelayMs(10_000);

        display.clearScreenDelayed();
        assertTrue(display.hasPendingTransientMessage());

        display.valueInfoPersistentNoClear("Note", "Track 1");

        assertFalse(display.hasPendingTransientMessage());
    }

    @Test
    void delayedClearCanUsePerMessageDelay() throws InterruptedException {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);
        display.setClearDelayMs(10_000);

        display.clearScreenDelayed(1);
        Thread.sleep(5);
        display.notifyBlink(1);

        assertFalse(display.hasPendingTransientMessage());
    }

    @Test
    void screenClearsAndImagesAdvanceLayoutRevision() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);

        final long initialRevision = display.layoutRevision();

        display.clearScreen();
        assertEquals(initialRevision + 1, display.layoutRevision());

        display.sendImage(new int[1024]);
        assertEquals(initialRevision + 2, display.layoutRevision());
    }

    @Test
    void highLevelTextCanRenderActiveFooterLegend() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);
        display.setFooterLegend("Engi Dens Pool Mut");

        display.valueInfo("Engine", "Acid");

        final ArgumentCaptor<byte[]> sysex = ArgumentCaptor.forClass(byte[].class);
        verify(midiOut, atLeastOnce()).sendSysex(sysex.capture());
        assertTrue(messagesContain(sysex.getAllValues(), "Engi Dens Pool Mut"));
    }

    @Test
    void imageWithFooterSendsTopGraphicPagesAndFooterText() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);
        final int[] image = new int[1024];

        display.sendImageWithFooter(image, "Vol  Pan  S1  S2");

        final ArgumentCaptor<byte[]> sysex = ArgumentCaptor.forClass(byte[].class);
        verify(midiOut, atLeastOnce()).sendSysex(sysex.capture());
        assertTrue(messagesContain(sysex.getAllValues(), "Vol  Pan  S1  S2"));
        assertTrue(sysex.getAllValues().stream()
                .filter(message -> message.length > 8)
                .noneMatch(message -> message[7] == 7 && message[8] == 7));
    }

    @Test
    void imageWithFooterPreservesTopPageCache() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);
        final int[] image = new int[1024];

        display.sendImageWithFooter(image, "Vol  Pan  S1  S2");
        reset(midiOut);

        display.sendImageWithFooter(image, "Vol  Pan  S1  S2");

        final ArgumentCaptor<byte[]> sysex = ArgumentCaptor.forClass(byte[].class);
        verify(midiOut).sendSysex(sysex.capture());
        assertTrue(messagesContain(sysex.getAllValues(), "Vol  Pan  S1  S2"));
    }

    @Test
    void normalTextAfterImageWithFooterClearsGraphics() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);
        final int[] image = new int[1024];
        image[0] = 1;

        display.sendImageWithFooter(image, "Vol  Pan  S1  S2");
        reset(midiOut);

        display.sendString(0, OledDisplay.TextJustification.LEFT, 0, "Track");

        verify(midiOut, atLeastOnce()).sendSysex(any(byte[].class));
        final ArgumentCaptor<byte[]> sysex = ArgumentCaptor.forClass(byte[].class);
        verify(midiOut, atLeastOnce()).sendSysex(sysex.capture());
        assertTrue(sysex.getAllValues().size() > 1);
        assertTrue(messagesContain(sysex.getAllValues(), "Track"));
    }

    private boolean messagesContain(final List<byte[]> messages, final String text) {
        for (final byte[] message : messages) {
            if (new String(message, StandardCharsets.US_ASCII).contains(text)) {
                return true;
            }
        }
        return false;
    }
}
