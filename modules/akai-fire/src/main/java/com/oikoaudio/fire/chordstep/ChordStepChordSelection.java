package com.oikoaudio.fire.chordstep;

import com.bitwig.extensions.framework.MusicalScale;
import com.oikoaudio.fire.note.ChordBank;
import com.oikoaudio.fire.note.NoteGridLayout;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Owns the selected chord source, preset page, octave, interpretation, and builder-note selection.
 */
public final class ChordStepChordSelection {
    public static final int BUILDER_FAMILY_INDEX = 0;
    public static final String BUILDER_FAMILY_LABEL = "Builder";
    private static final int MIN_CHORD_OCTAVE_OFFSET = -3;
    private static final int MAX_CHORD_OCTAVE_OFFSET = 3;

    private enum ChordInterpretation {
        AS_IS("Raw"),
        IN_SCALE("InKey");

        private final String displayName;

        ChordInterpretation(final String displayName) {
            this.displayName = displayName;
        }
    }

    private final ChordBank chordBank = new ChordBank();
    private final Set<Integer> builderSelectedNotes = new HashSet<>();

    private ChordInterpretation interpretation = ChordInterpretation.AS_IS;
    private int selectedFamily = BUILDER_FAMILY_INDEX;
    private int page = 0;
    private int selectedSlot = 0;
    private int octaveOffset = 0;

    public boolean isBuilderFamily() {
        return selectedFamily == BUILDER_FAMILY_INDEX;
    }

    public boolean hasSlot(final int sourcePadIndex) {
        return !isBuilderFamily() && chordBank.hasSlot(currentPresetFamilyIndex(), page, sourcePadIndex);
    }

    public ChordBank.Slot slot(final int sourcePadIndex) {
        return chordBank.slot(currentPresetFamilyIndex(), page, sourcePadIndex);
    }

    public void selectSlot(final int sourcePadIndex) {
        selectedSlot = sourcePadIndex;
    }

    public int selectedSlot() {
        return selectedSlot;
    }

    public int page() {
        return page;
    }

    public int octaveOffset() {
        return octaveOffset;
    }

    public int pageCount() {
        return isBuilderFamily() ? 1 : chordBank.pageCount(currentPresetFamilyIndex());
    }

    public int familyCount() {
        return chordBank.families().size() + 1;
    }

    public boolean adjustPage(final int amount) {
        if (amount == 0 || isBuilderFamily()) {
            return false;
        }
        final int nextPage = Math.max(0, Math.min(pageCount() - 1, page + amount));
        if (nextPage == page) {
            return false;
        }
        page = nextPage;
        ensureSelectedSlotValid();
        return true;
    }

    public boolean adjustFamily(final int amount) {
        if (amount == 0) {
            return false;
        }
        final int nextFamily = Math.max(0, Math.min(familyCount() - 1, selectedFamily + amount));
        if (nextFamily == selectedFamily) {
            return false;
        }
        selectedFamily = nextFamily;
        page = 0;
        selectedSlot = 0;
        ensureSelectedSlotValid();
        return true;
    }

    public boolean adjustOctave(final int amount) {
        if (amount == 0) {
            return false;
        }
        final int nextOffset = Math.max(MIN_CHORD_OCTAVE_OFFSET,
                Math.min(MAX_CHORD_OCTAVE_OFFSET, octaveOffset + amount));
        if (nextOffset == octaveOffset) {
            return false;
        }
        octaveOffset = nextOffset;
        return true;
    }

    public void resetOctave() {
        octaveOffset = 0;
    }

    public boolean canLowerOctave() {
        return octaveOffset > MIN_CHORD_OCTAVE_OFFSET;
    }

    public boolean canRaiseOctave() {
        return octaveOffset < MAX_CHORD_OCTAVE_OFFSET;
    }

    public void toggleInterpretation() {
        interpretation = interpretation == ChordInterpretation.AS_IS
                ? ChordInterpretation.IN_SCALE
                : ChordInterpretation.AS_IS;
    }

    public void resetInterpretation() {
        interpretation = ChordInterpretation.AS_IS;
    }

    public String interpretationDisplayName() {
        return interpretation.displayName;
    }

    public boolean adjustInterpretation(final int amount) {
        if (amount > 0 && interpretation == ChordInterpretation.AS_IS) {
            toggleInterpretation();
            return true;
        }
        if (amount < 0 && interpretation == ChordInterpretation.IN_SCALE) {
            toggleInterpretation();
            return true;
        }
        return false;
    }

    public String interpretationSuffix(final int rootNote) {
        return "F%d %s K%s O%s".formatted(selectedFamily + 1,
                interpretation.displayName,
                NoteGridLayout.noteName(rootNote),
                formatSignedValue(octaveOffset));
    }

    public int[] renderSelectedChord(final MusicalScale scale, final int rootNote) {
        if (isBuilderFamily()) {
            return renderBuilderChord();
        }
        ensureSelectedSlotValid();
        if (interpretation == ChordInterpretation.IN_SCALE) {
            return transpose(chordBank.renderCast(currentPresetFamilyIndex(), page, selectedSlot, scale,
                    Math.floorMod(rootNote, 12)), octaveOffset * 12);
        }
        return chordBank.renderAsIs(currentPresetFamilyIndex(), page, selectedSlot, chordRootMidi(rootNote));
    }

    public void resetToBuilder() {
        selectedFamily = BUILDER_FAMILY_INDEX;
        page = 0;
        selectedSlot = 0;
        builderSelectedNotes.clear();
    }

    public void replaceBuilderNotes(final Collection<Integer> notes) {
        selectedFamily = BUILDER_FAMILY_INDEX;
        page = 0;
        selectedSlot = 0;
        builderSelectedNotes.clear();
        builderSelectedNotes.addAll(notes);
    }

    public boolean hasBuilderNotes() {
        return !builderSelectedNotes.isEmpty();
    }

    public void addBuilderNote(final int midiNote) {
        builderSelectedNotes.add(midiNote);
    }

    public boolean isBuilderNoteSelected(final int midiNote) {
        return builderSelectedNotes.contains(midiNote);
    }

    public void toggleBuilderNote(final int midiNote) {
        if (!builderSelectedNotes.remove(midiNote)) {
            builderSelectedNotes.add(midiNote);
        }
    }

    public String familyLabel() {
        return isBuilderFamily()
                ? BUILDER_FAMILY_LABEL
                : oledFamilyLabel(chordBank.family(currentPresetFamilyIndex()).family());
    }

    public String chordName() {
        if (isBuilderFamily()) {
            return builderSelectedNotes.isEmpty() ? "Empty" : builderSelectionSummary();
        }
        return oledChordName(currentChordSlot());
    }

    public String rawFamilyName() {
        return isBuilderFamily() ? BUILDER_FAMILY_LABEL : chordBank.family(currentPresetFamilyIndex()).family();
    }

    public int currentPresetFamilyIndex() {
        return selectedFamily - 1;
    }

    public int chordRootMidi(final int rootNote) {
        return (ChordBank.MID_REGISTER_OCTAVE + 1) * 12 + rootNote + octaveOffset * 12;
    }

    public void ensureSelectedSlotValid() {
        if (isBuilderFamily()) {
            page = 0;
            selectedSlot = 0;
            return;
        }
        if (chordBank.hasSlot(currentPresetFamilyIndex(), page, selectedSlot)) {
            return;
        }
        final int pageStart = page * ChordBank.PAGE_SIZE;
        final int familySlotCount = chordBank.family(currentPresetFamilyIndex()).slots().size();
        if (pageStart >= familySlotCount) {
            page = Math.max(0, pageCount() - 1);
        }
        selectedSlot = 0;
        while (selectedSlot < ChordBank.PAGE_SIZE
                && !chordBank.hasSlot(currentPresetFamilyIndex(), page, selectedSlot)) {
            selectedSlot++;
        }
        if (selectedSlot >= ChordBank.PAGE_SIZE) {
            selectedSlot = 0;
        }
    }

    private ChordBank.Slot currentChordSlot() {
        if (isBuilderFamily()) {
            throw new IllegalStateException("Builder source has no preset slot");
        }
        ensureSelectedSlotValid();
        return chordBank.slot(currentPresetFamilyIndex(), page, selectedSlot);
    }

    private int[] renderBuilderChord() {
        return builderSelectedNotes.stream()
                .sorted()
                .mapToInt(Integer::intValue)
                .toArray();
    }

    private String builderSelectionSummary() {
        final List<Integer> renderedNotes = builderSelectedNotes.stream()
                .sorted()
                .toList();
        final List<String> noteNames = renderedNotes.stream()
                .limit(4)
                .map(midiNote -> NoteGridLayout.noteName(Math.floorMod(midiNote, 12)))
                .toList();
        final String suffix = renderedNotes.size() > 4 ? " +" + (renderedNotes.size() - 4) : "";
        return "%d notes %s%s".formatted(renderedNotes.size(), String.join(" ", noteNames), suffix).trim();
    }

    private static String oledChordName(final ChordBank.Slot slot) {
        if ("Barker".equals(slot.family())) {
            return switch (slot.formulaIndex()) {
                case 0 -> "Q stack";
                case 1 -> "Q b7/9";
                case 2 -> "Q Maj7";
                case 3 -> "Q add6";
                case 4 -> "5th+Q";
                case 5 -> "Q +11";
                case 6 -> "Q oct";
                case 7 -> "Q 6/10";
                default -> "Barker";
            };
        }
        if ("Root Drone".equals(slot.family())) {
            return switch (slot.formulaIndex()) {
                case 0 -> "9/11";
                case 1 -> "sus";
                case 2 -> "6/9";
                case 3 -> "#11";
                case 4 -> "5/10";
                case 5 -> "add9";
                case 6 -> "sus69";
                case 7 -> "Maj7";
                default -> "Drone";
            };
        }
        return switch (slot.name()) {
            case "Fully diminished" -> "Fully dim";
            case "Half-diminished" -> "Half dim";
            case "Dominant 7th" -> "Dom 7th";
            case "Dominant 7th (b9)" -> "Dom 7b9";
            case "10th (Spread maj7)" -> "10th sprd";
            default -> slot.name();
        };
    }

    private static String oledFamilyLabel(final String family) {
        return switch (family) {
            case BUILDER_FAMILY_LABEL -> BUILDER_FAMILY_LABEL;
            case "Sus Motion" -> "SusMot";
            case "Minor Drift" -> "MinDrft";
            case "Dorian Lift" -> "DorLift";
            case "Root Drone" -> "RootDrn";
            default -> family;
        };
    }

    private static int[] transpose(final int[] notes, final int semitones) {
        if (semitones == 0) {
            return notes;
        }
        final int[] transposed = new int[notes.length];
        for (int i = 0; i < notes.length; i++) {
            transposed[i] = notes[i] + semitones;
        }
        return transposed;
    }

    private static String formatSignedValue(final int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }
}
