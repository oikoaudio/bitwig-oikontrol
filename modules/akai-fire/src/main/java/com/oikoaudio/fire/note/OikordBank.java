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

    public record Slot(String family, String sourcePack, String sourceFamily, String name, String shortLabel,
                       int[] degrees) {
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
        addFamily(slots, "Barker", "classic", "QUARTAL",
                new String[]{"quartal stack", "quartal stack with b7 and 9", "quartal stack with Maj7 and 10",
                        "quartal add6 color", "open fifth with quartal top", "quartal stack with upper 11",
                        "quartal stack with octave support", "quartal add6 with upper 10"},
                new String[]{"Q stack", "Q b7/9", "Q Maj7/10", "Q add6", "5th+Q", "Q +11", "Q oct", "Q 6/10"},
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
        addFamily(slots, "Plaits", "plaits", "PLAITS-JON",
                new String[]{"Octave", "Fifth", "Minor", "Minor 7th", "Minor 9th", "Minor 11th", "Major",
                        "Major 7th"},
                new String[]{"Oct", "5th", "Min", "m7", "m9", "m11", "Maj", "Maj7"},
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
        addFamily(slots, "Sus Motion", "color", "SUSMOTION",
                new String[]{"7sus4", "7sus2", "6sus4", "6sus2", "7sus4(add9)", "7sus2(add9)", "sus4(add9)",
                        "9sus4 shell"},
                new String[]{"7sus4", "7sus2", "6sus4", "6sus2", "7s4+9", "7s2+9", "sus4+9", "9sus4"},
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
        addFamily(slots, "Quartal", "color", "QUARTALCOLOR",
                new String[]{"quartal stack", "quartal stack with b7 and 9", "quartal stack with Maj7 and 10",
                        "quartal add6 color", "open fifth with quartal top", "quartal stack with upper 11",
                        "quartal stack with octave support", "quartal add6 with upper 10"},
                new String[]{"Q stack", "Q b7/9", "Q Maj7/10", "Q add6", "5th+Q", "Q +11", "Q oct", "Q 6/10"},
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
        addFamily(slots, "Cluster", "color", "CLUSTERLIGHT",
                new String[]{"add9 cluster", "sus2sus4 cluster", "6/9 cluster", "6/9 shell", "add3/#11 rub",
                        "m(add9) cluster", "b9sus4 cluster", "6/9 sus cluster"},
                new String[]{"add9", "sus2/4", "6/9", "6/9 sh", "3/#11", "m+9", "b9sus4", "6/9sus"},
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
        addFamily(slots, "Minor Drift", "motion", "MINORDRIFT",
                new String[]{"m7", "m(add9)", "m6", "major shell with b6 rub", "7sus4(add9)", "m7sus4",
                        "7sus2(add9)", "minor triad"},
                new String[]{"m7", "m+9", "m6", "Maj/b6", "7s4+9", "m7sus4", "7s2+9", "min tri"},
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
        addFamily(slots, "Dorian Lift", "motion", "DORIANLIFT",
                new String[]{"m7", "m6", "Maj7", "sus2sus4", "7sus2", "6/9 sus shell", "Maj7(add9) shell",
                        "m6 reprise"},
                new String[]{"m7", "m6", "Maj7", "sus2/4", "7sus2", "6/9sus", "Maj7+9", "m6 rep"},
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
        addFamily(slots, "Rootless", "classic", "ROOTLESS",
                new String[]{"Major 9 shell", "Major 6/9 shell", "Dominant 9 shell", "Minor 9 shell",
                        "Major 11 shell", "Minor 11 shell", "9/11 dominant shell", "m11 shell"},
                new String[]{"Maj9", "Maj6/9", "Dom9", "Min9", "Maj11", "Min11", "9/11", "m11"},
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
                                  final String sourceFamily, final String[] names, final String[] shortLabels,
                                  final int[][] degrees) {
        for (int i = 0; i < degrees.length; i++) {
            slots.add(new Slot(family, sourcePack, sourceFamily, names[i], shortLabels[i], degrees[i]));
        }
    }
}
