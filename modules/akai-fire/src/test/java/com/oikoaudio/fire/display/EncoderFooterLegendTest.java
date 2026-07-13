package com.oikoaudio.fire.display;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EncoderFooterLegendTest {
    @Test
    void laysOutFourEncoderSlotsOnStableColumns() {
        assertEquals("Mod  Bend Glis Timb", EncoderFooterLegend.of("Mod", "Bend", "Glis", "Timb"));
        assertEquals("Velo Aft  Brth PExp", EncoderFooterLegend.of("Velo", "Aft", "Brth", "PExp"));
        assertEquals("Dev1 Dev2 Dev3 Dev4", EncoderFooterLegend.of("Dev1", "Dev2", "Dev3", "Dev4"));
    }

    @Test
    void exposesSharedMixerLegend() {
        assertEquals("Vol  Pan  S1  S2", EncoderFooterLegend.MIXER);
    }

    @Test
    void normalizesEmptyAndLongLabels() {
        assertEquals("Root --   PExp --", EncoderFooterLegend.of("Root", "", "Pitch Expr", null));
    }

    @Test
    void derivesFooterFromModeInfoLines() {
        assertEquals(
                "Engn Dens Pool MutT",
                EncoderFooterLegend.fromModeInfo(
                        "1: Engine\n2: Density\n3: Pool Oct\n4: Mut Type"));
        assertEquals(
                "Vol  Pan  S1  S2",
                EncoderFooterLegend.fromModeInfo("1: Volume\n2: Pan\n3: Send 1\n4: Send 2"));
        assertEquals(
                "Dens Tupl Ratc Clus",
                EncoderFooterLegend.fromModeInfo(
                        "1: Density / Alt\n2: Tuplet / Alt\n3: Ratchet\n4: Cluster"));
    }

    @Test
    void derivesScopedRemoteFooterFromNamesAndFallbacks() {
        assertEquals(
                "Cuto Reso Dev3 Gain",
                EncoderFooterLegend.remoteControls(
                        "D", 1, "Cutoff", "Resonance", "Remote 3", "Gain"));
        assertEquals("Trk5 Trk6 Trk7 Trk8", EncoderFooterLegend.remoteControls("T", 5));
    }

    @Test
    void derivesScopedRemoteModeInfo() {
        assertEquals(
                """
                        Device Remotes
                        1: Cutoff
                        2: Dev2 Remote
                        3: Dev3 Remote
                        4: Gain""",
                EncoderFooterLegend.remoteModeInfo(
                        "Device Remotes", "D", 1, "Cutoff", "Remote 2", "", "Gain"));
    }
}
