package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.sequence.AccentLatchState;

import java.util.Collection;
import java.util.Map;

public final class ChordStepAccentControls {
    private final OledDisplay oled;
    private final AccentLatchState latchState = new AccentLatchState();
    private final ChordStepAccentEditor editor = new ChordStepAccentEditor();

    public ChordStepAccentControls(final OledDisplay oled) {
        this.oled = oled;
    }

    public boolean isActive() {
        return latchState.isActive();
    }

    public boolean isHeld() {
        return latchState.isHeld();
    }

    public boolean isGestureActive() {
        return isHeld() || isActive();
    }

    public void markModified() {
        latchState.markModified();
    }

    public void handlePressed(final boolean pressed) {
        final AccentLatchState.Transition transition = latchState.handlePressed(pressed);
        if (transition == AccentLatchState.Transition.PRESSED) {
            oled.valueInfo("Accent Mode", isActive() ? "On" : "Off");
            return;
        }
        if (transition == AccentLatchState.Transition.TOGGLED_ON_RELEASE) {
            oled.valueInfo("Accent Mode", isActive() ? "On" : "Off");
            return;
        }
        oled.clearScreenDelayed();
    }

    public boolean toggleAccent(final Collection<NoteStep> notes, final int standardVelocity) {
        return editor.toggleAccent(notes, standardVelocity);
    }

    public boolean isStepAccented(final Map<Integer, NoteStep> notesAtStep, final int standardVelocity) {
        return editor.isStepAccented(notesAtStep, standardVelocity);
    }
}
