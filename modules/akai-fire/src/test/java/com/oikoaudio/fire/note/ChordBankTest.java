package com.oikoaudio.fire.note;

import com.bitwig.extensions.framework.MusicalScaleLibrary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChordBankTest {

    @Test
    void exposesEightCuratedFamiliesWithVariableTraversalLengths() {
        final ChordBank bank = new ChordBank();

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
        final ChordBank bank = new ChordBank();

        assertEquals("plaits", bank.family(0).sourcePack());
        assertEquals("PLAITS-JON", bank.family(0).sourceFamily());
        assertEquals("classic", bank.family(1).sourcePack());
        assertEquals("QUARTAL", bank.family(1).sourceFamily());
    }

    @Test
    void familySlotsExpandEachFormulaIntoEightVariants() {
        final ChordBank bank = new ChordBank();

        assertEquals("quartal stack", bank.slot(1, 0, 0).name());
        assertEquals("Q stack 1", bank.slot(1, 0, 0).shortLabel());
        assertEquals("Q stack 8", bank.slot(1, 0, 7).shortLabel());
        assertEquals("quartal stack with b7 and 9", bank.slot(1, 0, 8).name());
        assertEquals("quartal add6 with upper 10", bank.slot(1, 3, 15).name());
    }

    @Test
    void audibleUsesAllSeventeenConcreteSourceEntries() {
        final ChordBank bank = new ChordBank();

        assertEquals(2, bank.pageCount(0));
        assertEquals("Oct", bank.slot(0, 0, 0).shortLabel());
        assertEquals("Major 7th", bank.slot(0, 0, 7).name());
        assertEquals("Fully diminished", bank.slot(0, 1, 0).name());
    }

    @Test
    void respectsSixteenSlotPageBoundariesForCuratedFamilies() {
        final ChordBank bank = new ChordBank();

        assertTrue(bank.hasSlot(0, 0, 15));
        assertFalse(bank.hasSlot(0, 0, 16));
        assertTrue(bank.hasSlot(0, 1, 0));
        assertFalse(bank.hasSlot(0, 1, 1));

        assertThrows(IllegalArgumentException.class, () -> bank.slot(0, 0, 16));
    }

    @Test
    void lastBarkerVariantLivesOnFourthSixteenSlotPage() {
        final ChordBank bank = new ChordBank();

        assertEquals(4, bank.pageCount(1));
        assertTrue(bank.hasSlot(1, 3, 15));
        assertFalse(bank.hasSlot(1, 4, 0));
        assertEquals("quartal add6 with upper 10", bank.slot(1, 3, 15).name());
    }

    @Test
    void rejectsOutOfRangePageRequests() {
        final ChordBank bank = new ChordBank();

        assertThrows(IllegalArgumentException.class, () -> bank.slot(0, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> bank.slot(1, 4, 0));
    }

    @Test
    void keepsDistinctRenderedVoicingsAcrossGeneratedVariants() {
        final ChordBank bank = new ChordBank();
        final int[] first = bank.renderAsIs(0, 0, 6, 48);
        final int[] second = bank.renderAsIs(0, 0, 7, 48);

        assertFalse(java.util.Arrays.equals(first, second));
    }

    @Test
    void canRenderSlotsAsIsOrCastThroughScale() {
        final ChordBank bank = new ChordBank();
        final ChordBank.Slot slot = bank.slot(2, 0, 0);
        final int[] asIs = bank.renderAsIs(2, 0, 0, 48);
        final int[] cast = bank.renderCast(2, 0, 0, MusicalScaleLibrary.getInstance().getMusicalScale("Ionan (Major)"), 0);

        assertEquals(asIs.length, cast.length);
        assertTrue(java.util.Arrays.mismatch(asIs, cast) >= 0);
        assertArrayEquals(slot.pcs(), bank.slot(2, 0, 0).pcs());
    }
}
