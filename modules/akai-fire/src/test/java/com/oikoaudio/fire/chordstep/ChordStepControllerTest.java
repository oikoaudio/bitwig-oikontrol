package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChordStepControllerTest {

    @Test
    void delegatesEditButtonsAndClipState() {
        final List<String> events = new ArrayList<>();
        final ChordStepClipController clipController = new ChordStepClipController(
                () -> true,
                () -> true,
                () -> events.add("resync"),
                failure -> events.add("failure:" + failure.title()));
        clipController.refresh(SelectedClipSlotState.fromValues(2, true, RgbLigthState.GRAY_2));
        final ChordStepController mode = new ChordStepController(
                new ChordStepEditControls((title, detail) -> events.add(title + ":" + detail), () -> events.add("clear")),
                clipController,
                new ChordStepObservationController(
                        (task, delayTicks) -> events.add("schedule:" + delayTicks),
                        null,
                        () -> 0,
                        () -> RgbLigthState.GRAY_1,
                        clipController,
                        () -> events.add("clear-cache"),
                        () -> {},
                        () -> {},
                        () -> {},
                        () -> {},
                        (slotBank, selectedClipSlotIndex, refreshSelectedClipState, slotIndexSupplier,
                         scrollNoteClipToKeyStart, scrollObservedClipToKeyStart,
                         scrollNoteClipToCurrentStep, scrollObservedClipToStepStart) -> events.add("pass"),
                        (slotBank, defaultColor) -> SelectedClipSlotState.fromValues(2, true, defaultColor)));

        mode.handleMute1(true);
        mode.handleMute4(true);

        assertTrue(mode.isSelectHeld());
        assertTrue(mode.isDeleteHeld());
        assertEquals(BiColorLightState.GREEN_FULL, mode.mute1LightState());
        assertEquals(BiColorLightState.RED_FULL, mode.mute4LightState());
        assertTrue(mode.ensureSelectedClip());
        assertNotNull(mode.color());
        assertEquals(2, mode.slotIndex());
    }
}
