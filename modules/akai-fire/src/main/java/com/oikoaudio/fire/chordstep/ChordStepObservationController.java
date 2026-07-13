package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.sequence.NoteClipCursorRefresher;
import com.oikoaudio.fire.sequence.SelectedClipSlotObserver;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Owns the selected-clip observation lifecycle used by chord-step mode. */
final class ChordStepObservationController {
    private final ClipLauncherSlotBank noteClipSlotBank;
    private final IntSupplier selectedClipSlotIndex;
    private final Supplier<RgbLightState> defaultColorSupplier;
    private final ChordStepClipController clipController;
    private final Runnable clearObservedCaches;
    private final Runnable scrollNoteClipToKeyStart;
    private final Runnable scrollObservedClipToKeyStart;
    private final Runnable scrollNoteClipToCurrentStep;
    private final Runnable scrollObservedClipToStepStart;
    private final CursorRefresh cursorRefresh;
    private final StateScanner stateScanner;
    private final TaskScheduler scheduler;
    private boolean resyncQueued;

    public ChordStepObservationController(
            final TaskScheduler scheduler,
            final ClipLauncherSlotBank noteClipSlotBank,
            final IntSupplier selectedClipSlotIndex,
            final Supplier<RgbLightState> defaultColorSupplier,
            final ChordStepClipController clipController,
            final Runnable clearObservedCaches,
            final Runnable scrollNoteClipToKeyStart,
            final Runnable scrollObservedClipToKeyStart,
            final Runnable scrollNoteClipToCurrentStep,
            final Runnable scrollObservedClipToStepStart) {
        this(
                scheduler,
                noteClipSlotBank,
                selectedClipSlotIndex,
                defaultColorSupplier,
                clipController,
                clearObservedCaches,
                scrollNoteClipToKeyStart,
                scrollObservedClipToKeyStart,
                scrollNoteClipToCurrentStep,
                scrollObservedClipToStepStart,
                NoteClipCursorRefresher::refresh,
                SelectedClipSlotState::scan);
    }

    public ChordStepObservationController(
            final TaskScheduler scheduler,
            final ClipLauncherSlotBank noteClipSlotBank,
            final IntSupplier selectedClipSlotIndex,
            final Supplier<RgbLightState> defaultColorSupplier,
            final ChordStepClipController clipController,
            final Runnable clearObservedCaches,
            final Runnable scrollNoteClipToKeyStart,
            final Runnable scrollObservedClipToKeyStart,
            final Runnable scrollNoteClipToCurrentStep,
            final Runnable scrollObservedClipToStepStart,
            final CursorRefresh cursorRefresh) {
        this(
                scheduler,
                noteClipSlotBank,
                selectedClipSlotIndex,
                defaultColorSupplier,
                clipController,
                clearObservedCaches,
                scrollNoteClipToKeyStart,
                scrollObservedClipToKeyStart,
                scrollNoteClipToCurrentStep,
                scrollObservedClipToStepStart,
                cursorRefresh,
                SelectedClipSlotState::scan);
    }

    public ChordStepObservationController(
            final TaskScheduler scheduler,
            final ClipLauncherSlotBank noteClipSlotBank,
            final IntSupplier selectedClipSlotIndex,
            final Supplier<RgbLightState> defaultColorSupplier,
            final ChordStepClipController clipController,
            final Runnable clearObservedCaches,
            final Runnable scrollNoteClipToKeyStart,
            final Runnable scrollObservedClipToKeyStart,
            final Runnable scrollNoteClipToCurrentStep,
            final Runnable scrollObservedClipToStepStart,
            final CursorRefresh cursorRefresh,
            final StateScanner stateScanner) {
        this.noteClipSlotBank = noteClipSlotBank;
        this.selectedClipSlotIndex = selectedClipSlotIndex;
        this.defaultColorSupplier = defaultColorSupplier;
        this.clipController = clipController;
        this.clearObservedCaches = clearObservedCaches;
        this.scrollNoteClipToKeyStart = scrollNoteClipToKeyStart;
        this.scrollObservedClipToKeyStart = scrollObservedClipToKeyStart;
        this.scrollNoteClipToCurrentStep = scrollNoteClipToCurrentStep;
        this.scrollObservedClipToStepStart = scrollObservedClipToStepStart;
        this.cursorRefresh = cursorRefresh;
        this.stateScanner = stateScanner;
        this.scheduler = scheduler;
    }

    public void observeSelectedClip() {
        SelectedClipSlotObserver.observe(
                noteClipSlotBank, true, true, this::refreshSelectedClipState);
    }

    public void refreshSelectedClipState() {
        clipController.refresh(stateScanner.scan(noteClipSlotBank, defaultColorSupplier.get()));
    }

    public void queueResync() {
        if (resyncQueued) {
            return;
        }
        resyncQueued = true;
        scheduler.schedule(
                () -> {
                    resyncQueued = false;
                    refresh();
                },
                0);
    }

    public void refresh() {
        refreshSelectedClipState();
        refreshPass();
        scheduler.schedule(this::refreshPass, 1);
        scheduler.schedule(this::refreshPass, 6);
        scheduler.schedule(this::refreshPass, 18);
    }

    public void refreshPass() {
        clearObservedCaches.run();
        cursorRefresh.refresh(
                noteClipSlotBank,
                selectedClipSlotIndex.getAsInt(),
                this::refreshSelectedClipState,
                clipController::slotIndex,
                scrollNoteClipToKeyStart,
                scrollObservedClipToKeyStart,
                scrollNoteClipToCurrentStep,
                scrollObservedClipToStepStart);
    }

    public boolean isResyncQueued() {
        return resyncQueued;
    }

    @FunctionalInterface
    public interface TaskScheduler {
        void schedule(Runnable task, int delayTicks);
    }

    @FunctionalInterface
    public interface CursorRefresh {
        void refresh(
                ClipLauncherSlotBank slotBank,
                int selectedClipSlotIndex,
                Runnable refreshSelectedClipState,
                IntSupplier slotIndexSupplier,
                Runnable scrollNoteClipToKeyStart,
                Runnable scrollObservedClipToKeyStart,
                Runnable scrollNoteClipToCurrentStep,
                Runnable scrollObservedClipToStepStart);
    }

    @FunctionalInterface
    public interface StateScanner {
        SelectedClipSlotState scan(ClipLauncherSlotBank slotBank, RgbLightState defaultColor);
    }
}
