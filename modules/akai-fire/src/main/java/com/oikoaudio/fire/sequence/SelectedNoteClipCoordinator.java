package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.oikoaudio.fire.lights.RgbLightState;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/** Composes selected-slot observation, availability, state scanning, and note-clip cursor refresh. */
public final class SelectedNoteClipCoordinator {
    private final ClipLauncherSlotBank slotBank;
    private final RgbLightState defaultColor;
    private final BooleanSupplier canHoldNoteData;
    private final IntSupplier viewSelectedSlot;
    private final Consumer<NoteClipAvailability.Failure> failureHandler;
    private final Consumer<SelectedClipSlotState> stateHandler;
    private final Runnable[] cursorResetActions;
    private SelectedClipSlotState state;
    private int generation;

    public SelectedNoteClipCoordinator(final ClipLauncherSlotBank slotBank, final RgbLightState defaultColor,
                                       final BooleanSupplier canHoldNoteData, final IntSupplier viewSelectedSlot,
                                       final Consumer<NoteClipAvailability.Failure> failureHandler,
                                       final Consumer<SelectedClipSlotState> stateHandler,
                                       final Runnable... cursorResetActions) {
        this.slotBank = slotBank;
        this.defaultColor = defaultColor;
        this.canHoldNoteData = canHoldNoteData;
        this.viewSelectedSlot = viewSelectedSlot;
        this.failureHandler = failureHandler;
        this.stateHandler = stateHandler;
        this.cursorResetActions = cursorResetActions;
        state = SelectedClipSlotState.fromValues(-1, false, defaultColor);
    }

    public void observe(final boolean includeColor, final boolean includePlaybackState) {
        SelectedClipSlotObserver.observe(slotBank, includeColor, includePlaybackState, this::refreshState);
    }

    public SelectedClipSlotState refreshState() {
        final SelectedClipSlotState next = SelectedClipSlotState.scan(slotBank, defaultColor);
        if (next.slotIndex() != state.slotIndex()) {
            generation++;
        }
        state = next;
        stateHandler.accept(next);
        return next;
    }

    public SelectedClipSlotState state() {
        return state;
    }

    public boolean ensureAvailable() {
        final NoteClipAvailability.Failure failure = availabilityFailure();
        if (failure != null) {
            failureHandler.accept(failure);
            return false;
        }
        refreshCursor();
        return true;
    }

    public NoteClipAvailability.Failure availabilityFailure() {
        refreshState();
        return NoteClipAvailability.requireSelectedClipSlot(canHoldNoteData.getAsBoolean(), state.hasSelection());
    }

    public boolean refreshCursor() {
        return NoteClipCursorRefresher.refresh(slotBank, viewSelectedSlot.getAsInt(), this::refreshState,
                () -> state.slotIndex(), cursorResetActions);
    }

    public void scheduleRefresh(final Scheduler scheduler, final long delayMs, final Runnable completion) {
        final int scheduledGeneration = generation;
        final int scheduledSlot = state.slotIndex();
        scheduler.schedule(() -> {
            refreshState();
            if (generation != scheduledGeneration || state.slotIndex() != scheduledSlot) {
                return;
            }
            refreshCursor();
            completion.run();
        }, delayMs);
    }

    @FunctionalInterface
    public interface Scheduler {
        void schedule(Runnable task, long delayMs);
    }
}
