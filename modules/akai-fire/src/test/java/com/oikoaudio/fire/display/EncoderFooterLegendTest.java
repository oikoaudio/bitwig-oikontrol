package com.oikoaudio.fire.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncoderFooterLegendTest {
    @Test
    void laysOutFourEncoderSlotsOnStableColumns() {
        assertEquals("Mod  Bnd  Gli  Tmb", EncoderFooterLegend.of("Mod", "Bnd", "Gli", "Tmb"));
        assertEquals("Vel  Aft  Br   PEx", EncoderFooterLegend.of("Vel", "Aft", "Br", "PEx"));
        assertEquals("D1   D2   D3   D4", EncoderFooterLegend.of("D1", "D2", "D3", "D4"));
    }

    @Test
    void exposesSharedMixerLegend() {
        assertEquals("Vol  Pan  S1  S2", EncoderFooterLegend.MIXER);
    }

    @Test
    void normalizesEmptyAndLongLabels() {
        assertEquals("Root --   Pitc --", EncoderFooterLegend.of("Root", "", "Pitch Expr", null));
    }

    @Test
    void derivesFooterFromModeInfoLines() {
        assertEquals("Engi Dens Pool Mut",
                EncoderFooterLegend.fromModeInfo("1: Engine\n2: Density\n3: Pool Oct\n4: Mut Type"));
        assertEquals("Vol  Pan  S1  S2",
                EncoderFooterLegend.fromModeInfo("1: Volume\n2: Pan\n3: Send 1\n4: Send 2"));
        assertEquals("Dens Tupl Ratc Clus",
                EncoderFooterLegend.fromModeInfo("1: Density / Alt\n2: Tuplet / Alt\n3: Ratchet\n4: Cluster"));
    }

    @Test
    void derivesScopedRemoteFooterFromNamesAndFallbacks() {
        assertEquals("Cuto Reso D3   Gain",
                EncoderFooterLegend.remoteControls("D", 1, "Cutoff", "Resonance", "Remote 3", "Gain"));
        assertEquals("T5   T6   T7   T8", EncoderFooterLegend.remoteControls("T", 5));
    }

    @Test
    void derivesScopedRemoteModeInfo() {
        assertEquals("""
                        Device Remotes
                        1: Cutoff
                        2: D2 Remote
                        3: D3 Remote
                        4: Gain""",
                EncoderFooterLegend.remoteModeInfo("Device Remotes", "D", 1,
                        "Cutoff", "Remote 2", "", "Gain"));
    }
}
