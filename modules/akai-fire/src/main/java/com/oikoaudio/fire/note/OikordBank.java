package com.oikoaudio.fire.note;

import com.bitwig.extensions.framework.MusicalScale;

import java.util.ArrayList;
import java.util.List;

public final class OikordBank {
    public static final int PAGE_SIZE = 32;
    public static final int PAGE_COUNT = 2;
    public static final int SLOT_COUNT = PAGE_SIZE * PAGE_COUNT;
    public static final int MID_REGISTER_OCTAVE = 3;

    private static final List<Slot> SLOTS = createSlots();

    public List<Slot> page(final int pageIndex) {
        if (pageIndex < 0 || pageIndex >= PAGE_COUNT) {
            throw new IllegalArgumentException("Invalid Oikord page: " + pageIndex);
        }
        final int start = pageIndex * PAGE_SIZE;
        return SLOTS.subList(start, start + PAGE_SIZE);
    }

    public Slot slot(final int pageIndex, final int slotIndex) {
        if (slotIndex < 0 || slotIndex >= PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid Oikord slot: " + slotIndex);
        }
        return SLOTS.get(pageIndex * PAGE_SIZE + slotIndex);
    }

    public record Slot(String family, String sourcePack, String name, String shortLabel, int[] degrees) {
        public int[] render(final MusicalScale scale, final int rootNote) {
            final int[] notes = new int[degrees.length];
            for (int i = 0; i < degrees.length; i++) {
                notes[i] = scale.computeNote(rootNote, MID_REGISTER_OCTAVE, degrees[i]);
            }
            return notes;
        }
    }

    private static List<Slot> createSlots() {
        final List<Slot> slots = new ArrayList<>(SLOT_COUNT);
        addFamily(slots, "Barker", "classic",
                new String[]{"Open", "Lean", "Frame", "Lift", "Bloom", "Reach", "Shade", "Tense"},
                new String[]{"B-Op", "B-Ln", "B-Fr", "B-Lf", "B-Bl", "B-Rc", "B-Sh", "B-Tn"},
                new int[][]{
                        {0, 2, 4, 6},
                        {0, 2, 4, 8},
                        {0, 3, 5, 7},
                        {0, 2, 5, 7},
                        {0, 4, 6, 9},
                        {0, 2, 5, 9},
                        {0, 3, 6, 8},
                        {0, 4, 7, 9}
                });
        addFamily(slots, "Plaits", "plaits",
                new String[]{"Seed", "Bloom", "Split", "Arc", "Wake", "Halo", "Drift", "Crown"},
                new String[]{"P-Se", "P-Bl", "P-Sp", "P-Ar", "P-Wk", "P-Ha", "P-Dr", "P-Cr"},
                new int[][]{
                        {0, 4, 1, 6},
                        {0, 5, 2, 7},
                        {0, 3, 7, 9},
                        {0, 4, 8, 11},
                        {0, 2, 6, 10},
                        {0, 5, 9, 12},
                        {0, 3, 6, 11},
                        {0, 4, 7, 12}
                });
        addFamily(slots, "Sus Motion", "sus-motion",
                new String[]{"Sus2", "Sus4", "Wing", "Add6", "Open2", "Open4", "Lift4", "Wide"},
                new String[]{"S2", "S4", "SW", "S6", "O2", "O4", "L4", "Wd"},
                new int[][]{
                        {0, 1, 4, 7},
                        {0, 3, 4, 7},
                        {0, 1, 5, 8},
                        {0, 1, 4, 9},
                        {0, 1, 4, 8},
                        {0, 3, 5, 8},
                        {0, 3, 6, 9},
                        {0, 1, 4, 10}
                });
        addFamily(slots, "Quartal", "quartal",
                new String[]{"Stack 1", "Stack 2", "Lift 1", "Lift 2", "Split 1", "Split 2", "Reach", "Float"},
                new String[]{"Q1", "Q2", "QL", "Q+", "QS", "Q2", "QR", "QF"},
                new int[][]{
                        {0, 3, 6, 9},
                        {0, 4, 7, 10},
                        {0, 3, 7, 10},
                        {0, 4, 8, 11},
                        {0, 3, 6, 10},
                        {0, 4, 7, 11},
                        {0, 5, 8, 11},
                        {0, 3, 7, 11}
                });
        addFamily(slots, "Cluster", "cluster",
                new String[]{"Mist", "Curl", "Halo", "Needle", "Blur", "Grit", "Fold", "Glass"},
                new String[]{"C-Mi", "C-Cu", "C-Ha", "C-Ne", "C-Bl", "C-Gr", "C-Fo", "C-Gl"},
                new int[][]{
                        {0, 1, 2, 5},
                        {0, 1, 3, 5},
                        {0, 2, 3, 6},
                        {0, 1, 4, 5},
                        {0, 2, 4, 5},
                        {0, 1, 3, 6},
                        {0, 2, 3, 7},
                        {0, 1, 4, 7}
                });
        addFamily(slots, "Minor Drift", "minor-drift",
                new String[]{"Drift 1", "Drift 2", "Drift 3", "Drift 4", "Veil", "Lean", "Edge", "Late"},
                new String[]{"M1", "M2", "M3", "M4", "MV", "ML", "ME", "MT"},
                new int[][]{
                        {0, 2, 4, 8},
                        {0, 2, 5, 8},
                        {0, 3, 5, 8},
                        {0, 2, 4, 9},
                        {0, 3, 6, 8},
                        {0, 2, 6, 8},
                        {0, 4, 6, 8},
                        {0, 3, 5, 9}
                });
        addFamily(slots, "Dorian Lift", "dorian-lift",
                new String[]{"Lift 1", "Lift 2", "Lift 3", "Lift 4", "Open", "Rise", "Reach", "Sky"},
                new String[]{"D1", "D2", "D3", "D4", "DO", "DR", "DRc", "DS"},
                new int[][]{
                        {0, 2, 5, 9},
                        {0, 3, 5, 9},
                        {0, 2, 6, 9},
                        {0, 4, 6, 9},
                        {0, 2, 5, 10},
                        {0, 3, 7, 9},
                        {0, 2, 6, 10},
                        {0, 4, 7, 10}
                });
        addFamily(slots, "Rootless", "rootless",
                new String[]{"Shell 1", "Shell 2", "Shell 3", "Shell 4", "Air", "Float", "Lean", "Wide"},
                new String[]{"R1", "R2", "R3", "R4", "RA", "RF", "RL", "RW"},
                new int[][]{
                        {1, 4, 6, 9},
                        {1, 4, 7, 9},
                        {2, 4, 7, 9},
                        {2, 5, 7, 10},
                        {1, 4, 6, 10},
                        {2, 4, 7, 11},
                        {1, 5, 7, 10},
                        {2, 5, 9, 11}
                });
        return List.copyOf(slots);
    }

    private static void addFamily(final List<Slot> slots, final String family, final String sourcePack,
                                  final String[] names, final String[] shortLabels, final int[][] degrees) {
        for (int i = 0; i < degrees.length; i++) {
            slots.add(new Slot(family, sourcePack, names[i], shortLabels[i], degrees[i]));
        }
    }
}
