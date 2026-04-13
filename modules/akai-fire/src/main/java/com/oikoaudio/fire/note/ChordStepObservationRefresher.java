package com.oikoaudio.fire.note;

/**
 * Coordinates queued chord-step observation refreshes and the follow-up passes needed for Bitwig's
 * delayed clip observation updates.
 */
final class ChordStepObservationRefresher {
    private final TaskScheduler scheduler;
    private final Runnable refreshSelectedClipState;
    private final Runnable refreshPass;
    private boolean resyncQueued;

    ChordStepObservationRefresher(final TaskScheduler scheduler,
                                  final Runnable refreshSelectedClipState,
                                  final Runnable refreshPass) {
        this.scheduler = scheduler;
        this.refreshSelectedClipState = refreshSelectedClipState;
        this.refreshPass = refreshPass;
    }

    void queueResync() {
        if (resyncQueued) {
            return;
        }
        resyncQueued = true;
        scheduler.schedule(() -> {
            resyncQueued = false;
            refresh();
        }, 0);
    }

    void refresh() {
        refreshSelectedClipState.run();
        refreshPass.run();
        scheduler.schedule(refreshPass, 1);
        scheduler.schedule(refreshPass, 6);
        scheduler.schedule(refreshPass, 18);
    }

    boolean isResyncQueued() {
        return resyncQueued;
    }

    @FunctionalInterface
    interface TaskScheduler {
        void schedule(Runnable task, int delayTicks);
    }
}
