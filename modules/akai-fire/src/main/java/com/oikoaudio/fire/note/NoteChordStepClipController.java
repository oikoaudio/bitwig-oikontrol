package com.oikoaudio.fire.note;

import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.NoteClipAvailability;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;

import java.util.function.BooleanSupplier;

/**
 * Owns chord-step selected-clip state refresh and clip-availability checks.
 */
final class NoteChordStepClipController {
    private final ChordStepSelectedClipState selectedClipState = new ChordStepSelectedClipState();
    private final BooleanSupplier canHoldNoteData;
    private final BooleanSupplier hasLoadedNoteClipContent;
    private final ResyncRequester resyncRequester;
    private final AvailabilityFeedback availabilityFeedback;

    NoteChordStepClipController(final BooleanSupplier canHoldNoteData,
                                final BooleanSupplier hasLoadedNoteClipContent,
                                final ResyncRequester resyncRequester,
                                final AvailabilityFeedback availabilityFeedback) {
        this.canHoldNoteData = canHoldNoteData;
        this.hasLoadedNoteClipContent = hasLoadedNoteClipContent;
        this.resyncRequester = resyncRequester;
        this.availabilityFeedback = availabilityFeedback;
    }

    void refresh(final SelectedClipSlotState state) {
        if (selectedClipState.refresh(state)) {
            resyncRequester.queueResync();
        }
    }

    boolean ensureSelectedClip() {
        final NoteClipAvailability.Failure failure =
                selectedClipState.requireClip(canHoldNoteData.getAsBoolean(), hasLoadedNoteClipContent.getAsBoolean());
        if (failure != null) {
            availabilityFeedback.show(failure);
            return false;
        }
        return true;
    }

    boolean ensureSelectedClipSlot() {
        final NoteClipAvailability.Failure failure =
                selectedClipState.requireSelectedClipSlot(canHoldNoteData.getAsBoolean());
        if (failure != null) {
            availabilityFeedback.show(failure);
            return false;
        }
        return true;
    }

    int slotIndex() {
        return selectedClipState.slotIndex();
    }

    boolean hasContent() {
        return selectedClipState.hasContent();
    }

    RgbLigthState color() {
        return selectedClipState.color();
    }

    @FunctionalInterface
    interface ResyncRequester {
        void queueResync();
    }

    @FunctionalInterface
    interface AvailabilityFeedback {
        void show(NoteClipAvailability.Failure failure);
    }
}
