package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.sequence.NoteClipAvailability;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;
import java.util.function.BooleanSupplier;

/** Owns chord-step selected-clip state refresh and clip-availability checks. */
public final class ChordStepClipController {
    private final BooleanSupplier canHoldNoteData;
    private final BooleanSupplier hasLoadedNoteClipContent;
    private final ResyncRequester resyncRequester;
    private final AvailabilityFeedback availabilityFeedback;
    private int slotIndex = -1;
    private boolean hasContent;
    private RgbLightState color;

    public ChordStepClipController(
            final BooleanSupplier canHoldNoteData,
            final BooleanSupplier hasLoadedNoteClipContent,
            final ResyncRequester resyncRequester,
            final AvailabilityFeedback availabilityFeedback) {
        this.canHoldNoteData = canHoldNoteData;
        this.hasLoadedNoteClipContent = hasLoadedNoteClipContent;
        this.resyncRequester = resyncRequester;
        this.availabilityFeedback = availabilityFeedback;
    }

    public void refresh(final SelectedClipSlotState state) {
        final boolean changed = slotIndex != state.slotIndex() || hasContent != state.hasContent();
        slotIndex = state.slotIndex();
        hasContent = state.hasContent();
        color = state.color();
        if (changed) {
            resyncRequester.queueResync();
        }
    }

    public boolean ensureSelectedClip() {
        final NoteClipAvailability.Failure slotFailure = selectedClipSlotFailure();
        final NoteClipAvailability.Failure failure =
                slotFailure != null
                        ? slotFailure
                        : NoteClipAvailability.requireClipContent(
                                hasContent || hasLoadedNoteClipContent.getAsBoolean());
        if (failure != null) {
            availabilityFeedback.show(failure);
            return false;
        }
        return true;
    }

    public boolean ensureSelectedClipSlot() {
        final NoteClipAvailability.Failure failure = selectedClipSlotFailure();
        if (failure != null) {
            availabilityFeedback.show(failure);
            return false;
        }
        return true;
    }

    public int slotIndex() {
        return slotIndex;
    }

    public boolean hasContent() {
        return hasContent;
    }

    public RgbLightState color() {
        return color;
    }

    private NoteClipAvailability.Failure selectedClipSlotFailure() {
        return NoteClipAvailability.requireSelectedClipSlot(
                canHoldNoteData.getAsBoolean(), slotIndex >= 0);
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
