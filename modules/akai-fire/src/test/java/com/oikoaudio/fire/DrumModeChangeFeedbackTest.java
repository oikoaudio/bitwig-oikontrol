package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.oikoaudio.fire.TopLevelModeState.DrumMode;
import com.oikoaudio.fire.display.OledDisplay;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DrumModeChangeFeedbackTest {

    @Test
    void showsTheModeNameAsTransientFeedbackBeforeIdleModeContent() {
        final OledDisplay oled = mock(OledDisplay.class);
        final AtomicInteger incidentalDisplays = new AtomicInteger();
        final AtomicLong scheduledDelayMs = new AtomicLong();
        final AtomicReference<Runnable> release = new AtomicReference<>();
        final ModeChangeFeedback feedback = new ModeChangeFeedback();

        feedback.show(
                oled,
                "Drum XOX",
                1500,
                (task, delayMs) -> {
                    release.set(task);
                    scheduledDelayMs.set(delayMs);
                });

        verify(oled).valueInfo("Mode", "Drum XOX");
        verify(oled, never()).valueInfoPersistentNoClear(anyString(), anyString());
        assertEquals(1500, scheduledDelayMs.get());
        assertTrue(feedback.suppressesIncidentalFeedback());
        feedback.runIncidental(incidentalDisplays::incrementAndGet);
        assertEquals(0, incidentalDisplays.get());

        release.get().run();

        assertFalse(feedback.suppressesIncidentalFeedback());
        feedback.runIncidental(incidentalDisplays::incrementAndGet);
        assertEquals(1, incidentalDisplays.get());
    }

    @Test
    void installsFeedbackProtectionBeforeActivatingTheNextDrumMode() {
        final OledDisplay oled = mock(OledDisplay.class);
        final AtomicInteger incidentalDisplays = new AtomicInteger();
        final ModeChangeFeedback feedback = new ModeChangeFeedback();

        feedback.showDuring(
                oled,
                "Multiclp Seq",
                1500,
                (task, delayMs) -> {},
                () -> {
                    verify(oled).valueInfo("Mode", "Multiclp Seq");
                    assertTrue(feedback.suppressesIncidentalFeedback());
                    feedback.runIncidental(incidentalDisplays::incrementAndGet);
                });

        assertEquals(0, incidentalDisplays.get());
        verify(oled, times(2)).valueInfo("Mode", "Multiclp Seq");
    }

    @Test
    void incidentalTrackSelectionCannotReplaceModeNameDuringHold() {
        final OledDisplay oled = mock(OledDisplay.class);
        final ModeChangeFeedback feedback = new ModeChangeFeedback();

        feedback.show(oled, "NestedRytm", 1500, (task, delayMs) -> {});
        feedback.runIncidental(() -> oled.valueInfo("Track Select", "Drums"));

        verify(oled).valueInfo("Mode", "NestedRytm");
        verify(oled, never()).valueInfo("Track Select", "Drums");
    }

    @Test
    void usesTheShortMulticlipNameThatFitsTheOled() {
        assertEquals(
                "Multiclp Seq", AkaiFireOikontrolExtension.drumModeLabel(DrumMode.MULTICLIP_SEQ));
    }
}
