package com.oikoaudio.fire.note;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OikordBankTest {

    @Test
    void exposesEightCuratedFamiliesWithVariableTraversalLengths() {
        final OikordBank bank = new OikordBank();

        assertEquals(8, bank.families().size());
        assertEquals("Audible", bank.family(0).family());
        assertEquals("Barker", bank.family(1).family());
        assertEquals("Sus Motion", bank.family(2).family());
        assertEquals("Quartal", bank.family(3).family());
        assertEquals("Cluster", bank.family(4).family());
        assertEquals("Minor Drift", bank.family(5).family());
        assertEquals("Dorian Lift", bank.family(6).family());
        assertEquals("Root Drone", bank.family(7).family());

        assertEquals(17, bank.family(0).slots().size());
        assertEquals(64, bank.family(1).slots().size());
        assertEquals(64, bank.family(7).slots().size());
    }

    @Test
    void keepsBarkerAndAudibleFirstAcrossTheCuratedFamilies() {
        final OikordBank bank = new OikordBank();

        assertEquals("plaits", bank.family(0).sourcePack());
        assertEquals("PLAITS-JON", bank.family(0).sourceFamily());
        assertEquals("classic", bank.family(1).sourcePack());
        assertEquals("QUARTAL", bank.family(1).sourceFamily());
    }

    @Test
    void familySlotsExpandEachFormulaIntoEightVariants() {
        final OikordBank bank = new OikordBank();

        assertEquals("quartal stack", bank.slot(1, 0, 0).name());
        assertEquals("Q stack 1", bank.slot(1, 0, 0).shortLabel());
        assertEquals("Q stack 8", bank.slot(1, 0, 7).shortLabel());
        assertEquals("quartal stack with b7 and 9", bank.slot(1, 0, 8).name());
        assertEquals("quartal add6 with upper 10", bank.slot(1, 1, 31).name());
    }

    @Test
    void audibleUsesAllSeventeenConcreteSourceEntries() {
        final OikordBank bank = new OikordBank();

        assertEquals(1, bank.pageCount(0));
        assertEquals("Oct", bank.slot(0, 0, 0).shortLabel());
        assertEquals("Major 7th", bank.slot(0, 0, 7).name());
        assertEquals("Fully diminished", bank.slot(0, 0, 16).name());
    }

    @Test
    void keepsDistinctRenderedVoicingsAcrossGeneratedVariants() {
        final OikordBank bank = new OikordBank();
        final int[] first = bank.renderAsIs(0, 0, 6, 48);
        final int[] second = bank.renderAsIs(0, 0, 7, 48);

        assertFalse(java.util.Arrays.equals(first, second));
    }

    @Test
    void canRenderSlotsAsIsOrCastThroughScale() {
        final OikordBank bank = new OikordBank();
        final OikordBank.Slot slot = bank.slot(2, 0, 0);
        final int[] asIs = bank.renderAsIs(2, 0, 0, 48);
        final int[] cast = bank.renderCast(2, 0, 0, MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"), 0);

        assertEquals(asIs.length, cast.length);
        assertTrue(java.util.Arrays.mismatch(asIs, cast) >= 0);
        assertArrayEquals(slot.pcs(), bank.slot(2, 0, 0).pcs());
    }
}
