package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.oikoaudio.fire.values.StepViewPosition;

import java.util.function.IntConsumer;

/**
 * Owns the Bitwig clip/cursor resources used by chord-step sequencing.
 */
public final class ChordStepClipResources {
    private final PinnableCursorClip noteClip;
    private final Clip observedClip;
    private final StepViewPosition position;
    private final ClipLauncherSlotBank clipSlotBank;

    public ChordStepClipResources(final ControllerHost host,
                                  final CursorTrack cursorTrack,
                                  final int stepCount,
                                  final int observedFineStepCapacity,
                                  final double fineStepLength) {
        noteClip = cursorTrack.createLauncherCursorClip("NOTE_STEP", "NOTE_STEP", stepCount, 128);
        observedClip = host.createLauncherCursorClip(observedFineStepCapacity, 128);
        position = new StepViewPosition(noteClip, stepCount, "CHORD");
        clipSlotBank = cursorTrack.clipLauncherSlotBank();
        clipSlotBank.cursorIndex().markInterested();
        noteClip.scrollToKey(0);
        observedClip.scrollToKey(0);
        observedClip.setStepSize(fineStepLength);
        noteClip.getPlayStart().markInterested();
    }

    public void observe(final StepDataHandler stepDataHandler,
                        final NoteStepHandler noteStepHandler,
                        final StepDataHandler observedStepDataHandler,
                        final IntConsumer playingStepHandler) {
        noteClip.addStepDataObserver(stepDataHandler::handle);
        noteClip.addNoteStepObserver(noteStepHandler::handle);
        observedClip.addStepDataObserver(observedStepDataHandler::handle);
        noteClip.playingStep().addValueObserver(playingStepHandler::accept);
    }

    public PinnableCursorClip noteClip() {
        return noteClip;
    }

    public Clip observedClip() {
        return observedClip;
    }

    public StepViewPosition position() {
        return position;
    }

    public ClipLauncherSlotBank clipSlotBank() {
        return clipSlotBank;
    }

    public void scrollNoteClipToKeyStart() {
        noteClip.scrollToKey(0);
    }

    public void scrollObservedClipToKeyStart() {
        observedClip.scrollToKey(0);
    }

    public void scrollNoteClipToStep(final int step) {
        noteClip.scrollToStep(step);
    }

    public void scrollObservedClipToStepStart() {
        observedClip.scrollToStep(0);
    }

    @FunctionalInterface
    public interface StepDataHandler {
        void handle(int x, int y, int state);
    }

    @FunctionalInterface
    public interface NoteStepHandler {
        void handle(NoteStep noteStep);
    }
}
