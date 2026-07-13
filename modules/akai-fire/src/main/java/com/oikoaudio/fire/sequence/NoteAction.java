package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.NoteStep;
import java.util.List;

public class NoteAction {
    public enum Type {
        CLEAR,
        COPY_PAD;
    }

    private final int destPadIndex;
    private final int srcPadIndex;
    private final Type type;
    private final List<DrumNoteStepValues> copyNotes;

    NoteAction(final int srcPadIndex, final int destPadIndex, final Type type) {
        this(srcPadIndex, destPadIndex, type, null);
    }

    NoteAction(
            final int srcPadIndex,
            final int destPadIndex,
            final Type type,
            final List<NoteStep> copyNotes) {
        this.destPadIndex = destPadIndex;
        this.srcPadIndex = srcPadIndex;
        this.type = type;
        this.copyNotes =
                copyNotes == null
                        ? List.of()
                        : copyNotes.stream().map(DrumNoteStepValues::capture).toList();
    }

    public Type getType() {
        return type;
    }

    public int getSrcPadIndex() {
        return srcPadIndex;
    }

    public int getDestPadIndex() {
        return destPadIndex;
    }

    public List<DrumNoteStepValues> getCopyNotes() {
        return copyNotes;
    }
}
