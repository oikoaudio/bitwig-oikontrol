package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.NoteClipAvailability;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;

import java.util.function.BooleanSupplier;

/**
 * Owns chord-step selected-clip state refresh and clip-availability checks.
 */
public final class ChordStepClipController {
    private final ChordStepSelectedClipState selectedClipState = new ChordStepSelectedClipState();
    private final BooleanSupplier canHoldNoteData;
    private final BooleanSupplier hasLoadedNoteClipContent;
    private final ResyncRequester resyncRequester;
    private final AvailabilityFeedback availabilityFeedback;

    public ChordStepClipController(final BooleanSupplier canHoldNoteData,
                                   final BooleanSupplier hasLoadedNoteClipContent,
                                   final ResyncRequester resyncRequester,
                                   final AvailabilityFeedback availabilityFeedback) {
        this.canHoldNoteData = canHoldNoteData;
        this.hasLoadedNoteClipContent = hasLoadedNoteClipContent;
        this.resyncRequester = resyncRequester;
        this.availabilityFeedback = availabilityFeedback;
    }

    public void refresh(final SelectedClipSlotState state) {
        if (selectedClipState.refresh(state)) {
            resyncRequester.queueResync();
        }
    }

    public boolean ensureSelectedClip() {
        final NoteClipAvailability.Failure failure =
                selectedClipState.requireClip(canHoldNoteData.getAsBoolean(), hasLoadedNoteClipContent.getAsBoolean());
        if (failure != null) {
            availabilityFeedback.show(failure);
            return false;
        }
        return true;
    }

    public boolean ensureSelectedClipSlot() {
        final NoteClipAvailability.Failure failure =
                selectedClipState.requireSelectedClipSlot(canHoldNoteData.getAsBoolean());
        if (failure != null) {
            availabilityFeedback.show(failure);
            return false;
        }
        return true;
    }

    public int slotIndex() {
        return selectedClipState.slotIndex();
    }

    public boolean hasContent() {
        return selectedClipState.hasContent();
    }

    public RgbLigthState color() {
        return selectedClipState.color();
    }

    @FunctionalInterface
    public interface ResyncRequester {
        void queueResync();
    }

    @FunctionalInterface
    public interface AvailabilityFeedback {
        void show(NoteClipAvailability.Failure failure);
    }
}
