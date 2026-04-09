package com.oikoaudio.fire.sequence;

import java.util.List;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.NoteOccurrence;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public interface StepSequencerHost {
    boolean isSelectHeld();

    CursorRemoteControlsPage getActiveRemoteControlsPage();

    boolean isPadBeingHeld();

    List<NoteStep> getOnNotes();

    List<NoteStep> getHeldNotes();

    default List<NoteStep> getFocusedNotes() {
        return getHeldNotes();
    }

    String getDetails(List<NoteStep> heldNotes);

    double getGridResolution();

    BooleanValueObject getLengthDisplay();

    BooleanValueObject getDeleteHeld();

    String getPadInfo();

    void exitRecurrenceEdit();

    void enterRecurrenceEdit(List<NoteStep> notes);

    void updateRecurrencLength(int length);

    void registerModifiedSteps(List<NoteStep> notes);

    EncoderBankLayout getEncoderBankLayout();

    default int getDefaultStepVelocity() {
        return 100;
    }

    default double getDefaultStepPressure() {
        return 0.0;
    }

    default double getDefaultStepDuration() {
        return getGridResolution();
    }

    default NoteOccurrence getDefaultStepOccurrence() {
        return NoteOccurrence.values()[0];
    }

    default String getModeInfo(final EncoderMode mode) {
        return getEncoderBankLayout().bank(mode).modeInfo();
    }
}
