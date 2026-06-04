package com.oikoaudio.fire.display;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PeakRmsOledViewTest {
    @Test
    void drawsPeakRmsRowsWithBottomLegend() {
        final OledDisplay oled = mock(OledDisplay.class);
        final PeakRmsOledView view = new PeakRmsOledView(oled);

        view.show(127, 64, 20, 0, EncoderFooterLegend.MIXER);

        final InOrder order = inOrder(oled);
        order.verify(oled).clearScreen();
        order.verify(oled).sendString(0, OledDisplay.TextJustification.LEFT, 0, "Peak          RMS");
        order.verify(oled).sendString(0, OledDisplay.TextJustification.LEFT, 7, EncoderFooterLegend.MIXER);
        order.verify(oled).sendString(2, OledDisplay.TextJustification.LEFT, 1, "+0.0   -6.0");
        order.verify(oled).sendString(2, OledDisplay.TextJustification.LEFT, 4, "-16.1  -inf");
    }

    @Test
    void transientValuePreservesBottomLegendRows() {
        final OledDisplay oled = mock(OledDisplay.class);
        final PeakRmsOledView view = new PeakRmsOledView(oled);
        view.show(127, 64, 20, 0, EncoderFooterLegend.MIXER);

        view.showValueInfo("Volume", "-3.0 dB");

        final InOrder order = inOrder(oled);
        order.verify(oled).clearScreen();
        order.verify(oled).sendString(0, OledDisplay.TextJustification.LEFT, 0, "Peak          RMS");
        order.verify(oled).sendString(0, OledDisplay.TextJustification.LEFT, 7, EncoderFooterLegend.MIXER);
        order.verify(oled).sendString(2, OledDisplay.TextJustification.LEFT, 1, "+0.0   -6.0");
        order.verify(oled).sendString(2, OledDisplay.TextJustification.LEFT, 4, "-16.1  -inf");
        order.verify(oled).sendString(2, OledDisplay.TextJustification.LEFT, 0, "                    ");
        order.verify(oled).sendString(2, OledDisplay.TextJustification.LEFT, 1, "                    ");
        order.verify(oled).sendString(3, OledDisplay.TextJustification.LEFT, 2, "                    ");
        order.verify(oled).sendString(2, OledDisplay.TextJustification.LEFT, 4, "                    ");
        for (int row = 0; row < 7; row++) {
            order.verify(oled).sendString(0, OledDisplay.TextJustification.LEFT, row, "                    ");
        }
        order.verify(oled).sendString(0, OledDisplay.TextJustification.LEFT, 7, EncoderFooterLegend.MIXER);
        order.verify(oled).valueInfoNoClear("Volume", "-3.0 dB");
    }

    @Test
    void externalScreenClearInvalidatesCachedStaticRows() {
        final OledDisplay oled = mock(OledDisplay.class);
        final AtomicLong revision = new AtomicLong();
        when(oled.layoutRevision()).thenAnswer(invocation -> revision.get());
        doAnswer(invocation -> {
            revision.incrementAndGet();
            return null;
        }).when(oled).clearScreen();
        final PeakRmsOledView view = new PeakRmsOledView(oled);

        view.show(127, 64, 20, 0, EncoderFooterLegend.MIXER);
        view.show(127, 64, 20, 0, EncoderFooterLegend.MIXER);
        revision.incrementAndGet();
        view.show(127, 64, 20, 0, EncoderFooterLegend.MIXER);

        verify(oled, times(2)).clearScreen();
    }
}
