package com.oikoaudio.fire.perform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PerformDeviceLayersControllerTest {
    @Test
    void mapsEachLayerRowToOneExplicitAction() {
        assertEquals(
                PerformDeviceLayersController.Action.SELECT,
                PerformDeviceLayersController.actionForPad(0));
        assertEquals(
                PerformDeviceLayersController.Action.SOLO,
                PerformDeviceLayersController.actionForPad(16));
        assertEquals(
                PerformDeviceLayersController.Action.MUTE,
                PerformDeviceLayersController.actionForPad(32));
        assertEquals(
                PerformDeviceLayersController.Action.TOGGLE_ACTIVE,
                PerformDeviceLayersController.actionForPad(48));
        assertEquals(
                PerformDeviceLayersController.Action.NONE,
                PerformDeviceLayersController.actionForPad(-1));
    }
}
