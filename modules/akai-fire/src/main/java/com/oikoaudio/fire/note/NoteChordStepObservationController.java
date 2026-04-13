package com.oikoaudio.fire.note;

import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.NoteClipCursorRefresher;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;
import com.oikoaudio.fire.sequence.SelectedClipSlotObserver;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Owns the selected-clip observation lifecycle used by chord-step mode.
 */
final class NoteChordStepObservationController {
    private final ClipLauncherSlotBank noteClipSlotBank;
    private final IntSupplier selectedClipSlotIndex;
    private final Supplier<RgbLigthState> defaultColorSupplier;
    private final NoteChordStepClipController clipController;
    private final Runnable clearObservedCaches;
    private final Runnable scrollNoteClipToKeyStart;
    private final Runnable scrollObservedClipToKeyStart;
    private final Runnable scrollNoteClipToCurrentStep;
    private final Runnable scrollObservedClipToStepStart;
    private final CursorRefresh cursorRefresh;
    private final StateScanner stateScanner;
    private final ChordStepObservationRefresher refresher;

    NoteChordStepObservationController(final ChordStepObservationRefresher.TaskScheduler scheduler,
                                       final ClipLauncherSlotBank noteClipSlotBank,
                                       final IntSupplier selectedClipSlotIndex,
                                       final Supplier<RgbLigthState> defaultColorSupplier,
                                       final NoteChordStepClipController clipController,
                                       final Runnable clearObservedCaches,
                                       final Runnable scrollNoteClipToKeyStart,
                                       final Runnable scrollObservedClipToKeyStart,
                                       final Runnable scrollNoteClipToCurrentStep,
                                       final Runnable scrollObservedClipToStepStart) {
        this(scheduler, noteClipSlotBank, selectedClipSlotIndex, defaultColorSupplier, clipController,
                clearObservedCaches, scrollNoteClipToKeyStart, scrollObservedClipToKeyStart,
                scrollNoteClipToCurrentStep, scrollObservedClipToStepStart, NoteClipCursorRefresher::refresh,
                SelectedClipSlotState::scan);
    }

    NoteChordStepObservationController(final ChordStepObservationRefresher.TaskScheduler scheduler,
                                       final ClipLauncherSlotBank noteClipSlotBank,
                                       final IntSupplier selectedClipSlotIndex,
                                       final Supplier<RgbLigthState> defaultColorSupplier,
                                       final NoteChordStepClipController clipController,
                                       final Runnable clearObservedCaches,
                                       final Runnable scrollNoteClipToKeyStart,
                                       final Runnable scrollObservedClipToKeyStart,
                                       final Runnable scrollNoteClipToCurrentStep,
                                       final Runnable scrollObservedClipToStepStart,
                                       final CursorRefresh cursorRefresh) {
        this(scheduler, noteClipSlotBank, selectedClipSlotIndex, defaultColorSupplier, clipController,
                clearObservedCaches, scrollNoteClipToKeyStart, scrollObservedClipToKeyStart,
                scrollNoteClipToCurrentStep, scrollObservedClipToStepStart, cursorRefresh,
                SelectedClipSlotState::scan);
    }

    NoteChordStepObservationController(final ChordStepObservationRefresher.TaskScheduler scheduler,
                                       final ClipLauncherSlotBank noteClipSlotBank,
                                       final IntSupplier selectedClipSlotIndex,
                                       final Supplier<RgbLigthState> defaultColorSupplier,
                                       final NoteChordStepClipController clipController,
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
        this.refresher = new ChordStepObservationRefresher(scheduler, this::refreshSelectedClipState, this::refreshPass);
    }

    void observeSelectedClip() {
        SelectedClipSlotObserver.observe(noteClipSlotBank, true, true, this::refreshSelectedClipState);
    }

    void refreshSelectedClipState() {
        clipController.refresh(stateScanner.scan(noteClipSlotBank, defaultColorSupplier.get()));
    }

    void queueResync() {
        refresher.queueResync();
    }

    void refresh() {
        refresher.refresh();
    }

    void refreshPass() {
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

    boolean isResyncQueued() {
        return refresher.isResyncQueued();
    }

    @FunctionalInterface
    interface CursorRefresh {
        void refresh(ClipLauncherSlotBank slotBank,
                     int selectedClipSlotIndex,
                     Runnable refreshSelectedClipState,
                     IntSupplier slotIndexSupplier,
                     Runnable scrollNoteClipToKeyStart,
                     Runnable scrollObservedClipToKeyStart,
                     Runnable scrollNoteClipToCurrentStep,
                     Runnable scrollObservedClipToStepStart);
    }

    @FunctionalInterface
    interface StateScanner {
        SelectedClipSlotState scan(ClipLauncherSlotBank slotBank, RgbLigthState defaultColor);
    }
}
