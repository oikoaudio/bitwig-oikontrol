package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bitwig.extension.controller.api.Application;
import org.junit.jupiter.api.Test;

class GlobalTrackCreationControllerTest {
    @Test
    void patternBankCreatesInstrumentBeforeOrAfterSelectedMainTrack() {
        final Application application = mock(Application.class);
        final Runnable consumePattern = mock(Runnable.class);
        final GlobalTrackCreationController controller =
                new GlobalTrackCreationController(
                        application, consumePattern, (title, value) -> {});

        assertTrue(controller.handleBankButton(true, -1, true, false, "Audio", 3, -1));
        assertTrue(controller.handleBankButton(false, -1, false, false, "Audio", 3, -1));
        assertTrue(controller.handleBankButton(true, 1, true, false, "Audio", 3, -1));

        verify(application).createInstrumentTrack(3);
        verify(application).createInstrumentTrack(4);
        verify(consumePattern, org.mockito.Mockito.times(2)).run();
    }

    @Test
    void altPatternBankCreatesAudioTrack() {
        final Application application = mock(Application.class);
        final GlobalTrackCreationController controller =
                new GlobalTrackCreationController(application, () -> {}, (title, value) -> {});

        assertTrue(controller.handleBankButton(true, 1, true, true, "Instrument", 2, -1));

        verify(application).createAudioTrack(3);
    }

    @Test
    void selectedEffectTrackAlwaysCreatesEffectAtEffectPosition() {
        final Application application = mock(Application.class);
        final GlobalTrackCreationController controller =
                new GlobalTrackCreationController(application, () -> {}, (title, value) -> {});

        assertTrue(controller.handleBankButton(true, -1, true, false, "Effect", 12, 1));
        assertTrue(controller.handleBankButton(true, 1, true, true, "Effect", 12, 1));

        verify(application).createEffectTrack(1);
        verify(application).createEffectTrack(2);
        verify(application, never()).createInstrumentTrack(org.mockito.ArgumentMatchers.anyInt());
        verify(application, never()).createAudioTrack(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void ordinaryBankGestureIsNotConsumed() {
        final Application application = mock(Application.class);
        final GlobalTrackCreationController controller =
                new GlobalTrackCreationController(application, () -> {}, (title, value) -> {});

        assertFalse(controller.handleBankButton(true, 1, false, false, "Instrument", 2, -1));
    }
}
