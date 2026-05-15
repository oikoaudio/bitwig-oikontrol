package com.oikoaudio.fire.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LiveVelocityLogicTest {

    @Test
    void zeroSensitivityOutputsDefaultVelocityOnly() {
        assertEquals(100, LiveVelocityLogic.resolveVelocity(100, 0, 1));
        assertEquals(100, LiveVelocityLogic.resolveVelocity(100, 0, 127));
        assertEquals(20, LiveVelocityLogic.resolveVelocity(20, 0, 90));
    }

    @Test
    void fullSensitivityOutputsRawPadVelocity() {
        assertEquals(1, LiveVelocityLogic.resolveVelocity(100, 100, 1));
        assertEquals(64, LiveVelocityLogic.resolveVelocity(20, 100, 64));
        assertEquals(127, LiveVelocityLogic.resolveVelocity(80, 100, 127));
    }

    @Test
    void intermediateSensitivityBlendsBetweenDefaultAndRawVelocity() {
        assertEquals(64, LiveVelocityLogic.resolveVelocity(127, 50, 1));
        assertEquals(82, LiveVelocityLogic.resolveVelocity(100, 50, 64));
        assertEquals(60, LiveVelocityLogic.resolveVelocity(40, 25, 120));
    }

    @Test
    void valuesAreClampedToSupportedRanges() {
        assertEquals(1, LiveVelocityLogic.resolveVelocity(0, 100, -5));
        assertEquals(127, LiveVelocityLogic.resolveVelocity(200, 100, 200));
        assertEquals(100, LiveVelocityLogic.resolveVelocity(100, -10, 1));
        assertEquals(1, LiveVelocityLogic.resolveVelocity(100, 200, 1));
    }

    @Test
    void sensitivityClampingKeepsPercentInRange() {
        assertEquals(0, LiveVelocityLogic.clampSensitivity(-1));
        assertEquals(37, LiveVelocityLogic.clampSensitivity(37));
        assertEquals(100, LiveVelocityLogic.clampSensitivity(101));
    }
}
