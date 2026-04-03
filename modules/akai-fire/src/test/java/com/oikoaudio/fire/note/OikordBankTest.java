package com.oikoaudio.fire.note;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OikordBankTest {

    @Test
    void exposesTwoCuratedPagesOrderedByFamily() {
        final OikordBank bank = new OikordBank();

        final List<OikordBank.Slot> page0 = bank.page(0);
        final List<OikordBank.Slot> page1 = bank.page(1);

        assertEquals(32, page0.size());
        assertEquals(32, page1.size());
        assertEquals("Barker", page0.get(0).family());
        assertEquals("Plaits", page0.get(8).family());
        assertEquals("Sus Motion", page0.get(16).family());
        assertEquals("Quartal", page0.get(24).family());
        assertEquals("Cluster", page1.get(0).family());
        assertEquals("Minor Drift", page1.get(8).family());
        assertEquals("Dorian Lift", page1.get(16).family());
        assertEquals("Rootless", page1.get(24).family());
    }

    @Test
    void keepsBarkerAndPlaitsFirstAcrossTheCuratedBank() {
        final OikordBank bank = new OikordBank();

        assertEquals("Barker", bank.slot(0, 0).family());
        assertEquals("classic", bank.slot(0, 0).sourcePack());
        assertEquals("QUARTAL", bank.slot(0, 0).sourceFamily());
        assertEquals("quartal stack", bank.slot(0, 0).name());
        assertEquals("Plaits", bank.slot(0, 8).family());
        assertEquals("plaits", bank.slot(0, 8).sourcePack());
        assertEquals("PLAITS-JON", bank.slot(0, 8).sourceFamily());
        assertEquals("Octave", bank.slot(0, 8).name());
    }

    @Test
    void rendersDedicatedPlaitsVoicingsDifferentlyFromBarker() {
        final OikordBank bank = new OikordBank();
        final int[] barker = bank.slot(0, 0).degrees();
        final int[] plaits = bank.slot(0, 8).degrees();

        assertArrayEquals(new int[]{0, 2, 4, 6}, barker);
        assertArrayEquals(new int[]{0, 4, 1, 6}, plaits);
        assertTrue(plaits[1] > plaits[2]);
    }
}
