package com.oikoaudio.fire;

import com.oikoaudio.fire.display.OledDisplay;

final class ModeChangeFeedback {
    @FunctionalInterface
    interface Scheduler {
        void schedule(Runnable task, long delayMs);
    }

    private long generation;
    private boolean suppressesIncidentalFeedback;

    void show(
            final OledDisplay oled,
            final String modeLabel,
            final long holdMs,
            final Scheduler scheduler) {
        final long currentGeneration = ++generation;
        suppressesIncidentalFeedback = true;
        oled.valueInfo("Mode", modeLabel);
        scheduler.schedule(
                () -> {
                    if (generation == currentGeneration) {
                        suppressesIncidentalFeedback = false;
                    }
                },
                Math.max(0, holdMs));
    }

    void showDuring(
            final OledDisplay oled,
            final String modeLabel,
            final long holdMs,
            final Scheduler scheduler,
            final Runnable transition) {
        show(oled, modeLabel, holdMs, scheduler);
        try {
            transition.run();
        } finally {
            oled.valueInfo("Mode", modeLabel);
        }
    }

    boolean suppressesIncidentalFeedback() {
        return suppressesIncidentalFeedback;
    }

    void runIncidental(final Runnable feedback) {
        if (!suppressesIncidentalFeedback) {
            feedback.run();
        }
    }
}
