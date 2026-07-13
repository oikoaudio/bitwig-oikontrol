package com.oikoaudio.fire.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bitwig.extension.controller.api.MidiOut;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    void persistentTextRejectsLaterDelayedClear() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);

        display.valueInfoPersistentNoClear("Perform", "Drums synt");
        display.clearScreenDelayed();

        assertFalse(display.hasPendingClear());
        assertFalse(display.hasPendingTransientMessage());
    }

    @Test
    void persistentTextPreventsExpiredClearFromBlankingScreen() throws InterruptedException {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);

        display.clearScreenDelayed(1);
        display.valueInfoPersistentNoClear("Perform", "Drums synt");
        reset(midiOut);
        Thread.sleep(5);
        display.notifyBlink(1);

        verifyNoInteractions(midiOut);
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
    void highLevelTextCanRenderActiveFooterLegendAtTop() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);
        display.setFooterLegendPosition(EncoderLegendPosition.TOP);
        display.setFooterLegend("Engi Dens Pool Mut");

        display.valueInfo("Engine", "Acid");

        final ArgumentCaptor<byte[]> sysex = ArgumentCaptor.forClass(byte[].class);
        verify(midiOut, atLeastOnce()).sendSysex(sysex.capture());
        assertTrue(messagesContainAtRow(sysex.getAllValues(), "Engi Dens Pool Mut", 0));
        assertTrue(messagesContainAtRow(sysex.getAllValues(), "Engine", 2));
        assertTrue(messagesContainAtRow(sysex.getAllValues(), "Acid", 4));
    }

    @Test
    void persistentValueInfoOffsetsTitleWhenFooterLegendIsAtTop() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);
        display.setFooterLegendPosition(EncoderLegendPosition.TOP);
        display.setFooterLegend("Mod Bnd Gli Tmb");

        display.valueInfoPersistentNoClear("Note", "Drums synt");

        final ArgumentCaptor<byte[]> sysex = ArgumentCaptor.forClass(byte[].class);
        verify(midiOut, atLeastOnce()).sendSysex(sysex.capture());
        assertTrue(messagesContainAtRow(sysex.getAllValues(), "Mod Bnd Gli Tmb", 0));
        assertTrue(messagesContainAtRow(sysex.getAllValues(), "Note", 2));
        assertTrue(messagesContainAtRow(sysex.getAllValues(), "Drums synt", 4));
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
        assertTrue(
                sysex.getAllValues().stream()
                        .filter(this::isImagePageMessage)
                        .noneMatch(message -> message[7] == 7 && message[8] == 7));
    }

    @Test
    void imageWithTopFooterSendsBottomGraphicPagesAndFooterText() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);
        display.setFooterLegendPosition(EncoderLegendPosition.TOP);
        final int[] image = new int[1024];

        display.sendImageWithFooter(image, "Vol  Pan  S1  S2");

        final ArgumentCaptor<byte[]> sysex = ArgumentCaptor.forClass(byte[].class);
        verify(midiOut, atLeastOnce()).sendSysex(sysex.capture());
        assertTrue(messagesContainAtRow(sysex.getAllValues(), "Vol  Pan  S1  S2", 0));
        assertTrue(
                sysex.getAllValues().stream()
                        .filter(this::isImagePageMessage)
                        .noneMatch(message -> message[7] == 0 && message[8] == 0));
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

    @Test
    void compactUnipolarValueBarKeepsValueTextPrimary() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);

        display.valueInfoWithBar("Volume", "0.0 dB", 0.5, false);

        final ArgumentCaptor<byte[]> sysex = ArgumentCaptor.forClass(byte[].class);
        verify(midiOut, atLeastOnce()).sendSysex(sysex.capture());
        assertTrue(messagesContainAtRow(sysex.getAllValues(), "Volume", 0));
        assertTrue(messagesContainAtRow(sysex.getAllValues(), "0.0 dB", 3));
        final byte[] bar = lastBarMessage(sysex.getAllValues());
        assertEquals(126, Byte.toUnsignedInt(bar[7]));
        assertEquals(1, Byte.toUnsignedInt(bar[8]));
        assertEquals(0, Byte.toUnsignedInt(bar[9]));
        assertEquals(6, Byte.toUnsignedInt(bar[12]));
        assertEquals(0, Byte.toUnsignedInt(bar[13]));
        assertEquals(63, Byte.toUnsignedInt(bar[14]));
    }

    @Test
    void compactBipolarValueBarDrawsFromCenterWithCenterMarker() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);

        display.valueInfoWithBar("Pan", "25% R", 0.75, true);

        final ArgumentCaptor<byte[]> sysex = ArgumentCaptor.forClass(byte[].class);
        verify(midiOut, atLeastOnce()).sendSysex(sysex.capture());
        final List<byte[]> bars = sysex.getAllValues().stream().filter(this::isBarMessage).toList();
        assertEquals(1, bars.size());
        assertEquals(0, Byte.toUnsignedInt(bars.get(0)[9]));
        assertEquals(OledDisplay.Fill.Solid.ordinal(), Byte.toUnsignedInt(bars.get(0)[10]));
        assertEquals(63, Byte.toUnsignedInt(bars.get(0)[13]));
        assertEquals(95, Byte.toUnsignedInt(bars.get(0)[14]));
    }

    @Test
    void compactValueBarMovesBelowTextWhenFooterLegendIsAtTop() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);
        display.setFooterLegendPosition(EncoderLegendPosition.TOP);
        display.setFooterLegend("Vol  Pan  S1  S2");

        display.valueInfoWithBar("Send 1", "-6.0 dB", 0.25, false);

        final ArgumentCaptor<byte[]> sysex = ArgumentCaptor.forClass(byte[].class);
        verify(midiOut, atLeastOnce()).sendSysex(sysex.capture());
        assertTrue(messagesContainAtRow(sysex.getAllValues(), "Vol  Pan  S1  S2", 0));
        assertTrue(messagesContainAtRow(sysex.getAllValues(), "Send 1", 2));
        assertTrue(messagesContainAtRow(sysex.getAllValues(), "-6.0 dB", 5));
        assertEquals(7, Byte.toUnsignedInt(lastBarMessage(sysex.getAllValues())[12]));
    }

    @Test
    void integerParamInfoUsesCompactBorderlessBar() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);

        display.paramInfo("Mod", 64, "", 0, 127);

        final ArgumentCaptor<byte[]> sysex = ArgumentCaptor.forClass(byte[].class);
        verify(midiOut, atLeastOnce()).sendSysex(sysex.capture());
        final byte[] bar = lastBarMessage(sysex.getAllValues());
        assertEquals(0, Byte.toUnsignedInt(bar[9]));
        assertEquals(OledDisplay.Fill.Fifty.ordinal(), Byte.toUnsignedInt(bar[10]));
        assertEquals(0, Byte.toUnsignedInt(bar[13]));
        assertEquals(63, Byte.toUnsignedInt(bar[14]));
    }

    @Test
    void bipolarParamInfoPercentUsesCompactCenterOutBar() {
        final MidiOut midiOut = mock(MidiOut.class);
        final OledDisplay display = new OledDisplay(midiOut);

        display.paramInfoPercent("Timbre", 0.5, "Drum Default", -1.0, 1.0);

        final ArgumentCaptor<byte[]> sysex = ArgumentCaptor.forClass(byte[].class);
        verify(midiOut, atLeastOnce()).sendSysex(sysex.capture());
        final List<byte[]> bars = sysex.getAllValues().stream().filter(this::isBarMessage).toList();
        assertEquals(1, bars.size());
        assertEquals(0, Byte.toUnsignedInt(bars.get(0)[9]));
        assertEquals(OledDisplay.Fill.Solid.ordinal(), Byte.toUnsignedInt(bars.get(0)[10]));
        assertEquals(63, Byte.toUnsignedInt(bars.get(0)[13]));
        assertEquals(95, Byte.toUnsignedInt(bars.get(0)[14]));
    }

    private boolean messagesContain(final List<byte[]> messages, final String text) {
        for (final byte[] message : messages) {
            if (new String(message, StandardCharsets.US_ASCII).contains(text)) {
                return true;
            }
        }
        return false;
    }

    private boolean messagesContainAtRow(
            final List<byte[]> messages, final String text, final int row) {
        for (final byte[] message : messages) {
            if (message.length > 9
                    && message[9] == row
                    && new String(message, StandardCharsets.US_ASCII).contains(text)) {
                return true;
            }
        }
        return false;
    }

    private boolean isImagePageMessage(final byte[] message) {
        return message.length > 8 && message[4] == 0x0E;
    }

    private boolean isBarMessage(final byte[] message) {
        return message.length > 14 && message[4] == 0x09;
    }

    private byte[] lastBarMessage(final List<byte[]> messages) {
        byte[] last = null;
        for (final byte[] message : messages) {
            if (isBarMessage(message)) {
                last = message;
            }
        }
        return last;
    }
}
