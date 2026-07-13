package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import java.util.List;
import org.junit.jupiter.api.Test;

class DrumNoteStepValuesTest {
    @Test
    void insertsCopiedStepWithItsOriginalVelocityAndDuration() {
        final NoteStep source = mock(NoteStep.class);
        when(source.x()).thenReturn(6);
        when(source.velocity()).thenReturn(0.65);
        when(source.duration()).thenReturn(0.375);
        final PinnableCursorClip destination = mock(PinnableCursorClip.class);

        DrumNoteStepValues.capture(source).insertInto(destination);

        verify(destination).setStep(6, 0, 83, 0.375);
    }

    @Test
    void appliesProbabilityCapturedBeforeTheCursorMovesToTheDestinationPad() {
        final NoteStep source = mock(NoteStep.class);
        when(source.chance()).thenReturn(0.42);
        when(source.isChanceEnabled()).thenReturn(true);
        final DrumNoteStepValues copiedValues = DrumNoteStepValues.capture(source);
        when(source.chance()).thenReturn(0.0);
        when(source.isChanceEnabled()).thenReturn(false);
        final NoteStep destination = mock(NoteStep.class);

        copiedValues.applyParametersTo(destination);

        verify(destination).setChance(0.42);
        verify(destination).setIsChanceEnabled(true);
    }

    @Test
    void preservesTheDefaultOneHundredPercentProbability() {
        final NoteStep source = mock(NoteStep.class);
        when(source.chance()).thenReturn(1.0);
        when(source.isChanceEnabled()).thenReturn(false);
        final NoteStep destination = mock(NoteStep.class);

        DrumNoteStepValues.capture(source).applyParametersTo(destination);

        verify(destination).setChance(1.0);
        verify(destination).setIsChanceEnabled(false);
    }

    @Test
    void pendingPadCopyOwnsASnapshotIndependentOfLaterCursorUpdates() {
        final NoteStep source = mock(NoteStep.class);
        when(source.velocity()).thenReturn(0.55);
        when(source.chance()).thenReturn(0.72);

        final NoteAction action = new NoteAction(2, 5, NoteAction.Type.COPY_PAD, List.of(source));
        when(source.velocity()).thenReturn(1.0);
        when(source.chance()).thenReturn(0.0);

        assertEquals(0.55, action.getCopyNotes().get(0).velocity());
        assertEquals(0.72, action.getCopyNotes().get(0).chance());
    }
}
