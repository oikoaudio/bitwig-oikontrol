package com.oikoaudio.fire.sequence;

import java.util.List;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.oikoaudio.fire.control.TouchEncoder;

public interface StepSequencerHost {
    boolean isSelectHeld();

    CursorRemoteControlsPage getActiveRemoteControlsPage();

    boolean isPadBeingHeld();

    List<NoteStep> getOnNotes();

    List<NoteStep> getHeldNotes();

    String getDetails(List<NoteStep> heldNotes);

    double getGridResolution();

    BooleanValueObject getLengthDisplay();

    BooleanValueObject getDeleteHeld();

    String getPadInfo();

    void exitRecurrenceEdit();

    void enterRecurrenceEdit(List<NoteStep> notes);

    void updateRecurrencLength(int length);

    void registerModifiedSteps(List<NoteStep> notes);

    default boolean hasCustomChannelPage() {
        return false;
    }

    default void bindChannelPage(SequencEncoderHandler handler, Layer layer, TouchEncoder[] encoders) {
    }

    void bindMixerPage(SequencEncoderHandler handler, Layer layer, TouchEncoder[] encoders);

    default void bindUser1Page(SequencEncoderHandler handler, Layer layer, TouchEncoder[] encoders) {
        handler.bindDefaultPage(EncoderMode.USER_1, layer, encoders);
    }

    void bindUser2Page(SequencEncoderHandler handler, Layer layer, TouchEncoder[] encoders);

    default String getModeInfo(final EncoderMode mode) {
        return mode.getInfo();
    }
}
