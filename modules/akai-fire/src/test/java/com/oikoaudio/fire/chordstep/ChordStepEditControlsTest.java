package com.oikoaudio.fire.chordstep;

import com.oikoaudio.fire.lights.BiColorLightState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChordStepEditControlsTest {

    @Test
    void muteButtonsUpdateHeldStateAndDisplay() {
        final List<String> events = new ArrayList<>();
        final ChordStepEditControls controls = new ChordStepEditControls(
                (title, detail) -> events.add(title + ":" + detail),
                () -> events.add("clear"));

        controls.handleMute1(true);
        controls.handleMute1(false);
        controls.handleMute2(true);
        controls.handleMute3(true);
        controls.handleMute4(true);

        assertFalse(controls.isSelectHeld());
        assertTrue(controls.isFixedLengthHeld());
        assertTrue(controls.isCopyHeld());
        assertTrue(controls.isDeleteHeld());
        assertEquals(List.of(
                "Select:Load step",
                "clear",
                "Last Step:Target step",
                "Paste:Clip / step target",
                "Delete:Clip / step target"), events);
    }

    @Test
    void lightStatesFollowHeldFlags() {
        final ChordStepEditControls controls = new ChordStepEditControls((title, detail) -> {}, () -> {});

        assertEquals(BiColorLightState.GREEN_HALF, controls.mute1LightState());
        assertEquals(BiColorLightState.AMBER_HALF, controls.mute2LightState());
        assertEquals(BiColorLightState.OFF, controls.mute3LightState());
        assertEquals(BiColorLightState.OFF, controls.mute4LightState());

        controls.handleMute1(true);
        controls.handleMute2(true);
        controls.handleMute3(true);
        controls.handleMute4(true);

        assertEquals(BiColorLightState.GREEN_FULL, controls.mute1LightState());
        assertEquals(BiColorLightState.AMBER_FULL, controls.mute2LightState());
        assertEquals(BiColorLightState.GREEN_FULL, controls.mute3LightState());
        assertEquals(BiColorLightState.RED_FULL, controls.mute4LightState());
    }

    @Test
    void deleteHeldValueReflectsPressedState() {
        final ChordStepEditControls controls = new ChordStepEditControls((title, detail) -> {}, () -> {});

        assertFalse(controls.deleteHeldValue().get());
        controls.handleMute4(true);
        assertTrue(controls.deleteHeldValue().get());
        controls.handleMute4(false);
        assertFalse(controls.deleteHeldValue().get());
    }
}
