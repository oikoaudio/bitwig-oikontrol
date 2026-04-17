package com.oikoaudio.fire.note;

import com.bitwig.extensions.framework.MusicalScale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ChordBank {
    public static final int PAGE_SIZE = 16;
    public static final int MID_REGISTER_OCTAVE = 3;
    private static final int MID_REGISTER_CENTER = 64;

    private static final List<Family> FAMILIES = createFamilies();

    public List<Family> families() {
        return FAMILIES;
    }

    public Family family(final int familyIndex) {
        if (familyIndex < 0 || familyIndex >= FAMILIES.size()) {
            throw new IllegalArgumentException("Invalid Chord family index: " + familyIndex);
        }
        return FAMILIES.get(familyIndex);
    }

    public Slot slot(final int familyIndex, final int pageIndex, final int slotIndex) {
        final int absoluteIndex = pageIndex * PAGE_SIZE + slotIndex;
        if (pageIndex < 0 || pageIndex >= pageCount(familyIndex)) {
            throw new IllegalArgumentException("Invalid Chord page: " + pageIndex);
        }
        if (slotIndex < 0 || slotIndex >= PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid Chord slot: " + slotIndex);
        }
        if (absoluteIndex >= family(familyIndex).slots().size()) {
            throw new IllegalArgumentException("Invalid Chord slot index for family page: " + slotIndex);
        }
        return family(familyIndex).slots().get(absoluteIndex);
    }

    public boolean hasSlot(final int familyIndex, final int pageIndex, final int slotIndex) {
        if (familyIndex < 0 || familyIndex >= FAMILIES.size() || pageIndex < 0 || slotIndex < 0 || slotIndex >= PAGE_SIZE) {
            return false;
        }
        final int absoluteIndex = pageIndex * PAGE_SIZE + slotIndex;
        return absoluteIndex < family(familyIndex).slots().size();
    }

    public int pageCount(final int familyIndex) {
        final int slotCount = family(familyIndex).slots().size();
        return Math.max(1, (slotCount + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    public int[] renderAsIs(final int familyIndex, final int pageIndex, final int slotIndex, final int rootMidi) {
        return renderFamily(family(familyIndex), RenderMode.AS_IS, rootMidi, null, 0).get(pageIndex * PAGE_SIZE + slotIndex);
    }

    public int[] renderCast(final int familyIndex, final int pageIndex, final int slotIndex, final MusicalScale scale,
                            final int rootNote) {
        return renderFamily(family(familyIndex), RenderMode.CAST, 0, scale, rootNote).get(pageIndex * PAGE_SIZE + slotIndex);
    }

    public enum Style {
        CLASSIC,
        COLOR,
        PLAITS
    }

    public record Family(String family, String sourcePack, String sourceFamily, Style style, List<Formula> formulas,
                         List<Slot> slots) {
    }

    public record Formula(String name, String shortLabel, int[] pcs) {
    }

    public record Slot(String family, String sourcePack, String sourceFamily, Style style, String name,
                       String shortLabel, int[] pcs, int formulaIndex, int variantIndex, int slotIndex) {
    }

    private enum RenderMode {
        AS_IS,
        CAST
    }

    private static List<Family> createFamilies() {
        return List.of(
                // "Audible" is the public-facing label for the family that uses a Plaits-derived voicing rotation.
                createFamily("Audible", "plaits", "PLAITS-JON", Style.PLAITS, List.of(
                        formula("Octave", "Oct", 0, 0, 12, 12),
                        formula("Fifth", "5th", 0, 7, 7, 12),
                        formula("Minor", "Min", 0, 3, 7, 12),
                        formula("Minor 7th", "m7", 0, 3, 7, 10),
                        formula("Minor 9th", "m9", 0, 3, 10, 14),
                        formula("Minor 11th", "m11", 0, 3, 10, 17),
                        formula("Major", "Maj", 0, 4, 7, 12),
                        formula("Major 7th", "Maj7", 0, 4, 7, 11),
                        formula("Major 9th", "Maj9", 0, 4, 11, 14),
                        formula("Sus4", "sus4", 0, 5, 7, 12),
                        formula("6/9", "6/9", 0, 2, 9, 16),
                        formula("6th", "6th", 0, 4, 7, 9),
                        formula("10th (Spread maj7)", "10th", 0, 7, 16, 23),
                        formula("Dominant 7th", "7th", 0, 4, 7, 10),
                        formula("Dominant 7th (b9)", "7b9", 0, 7, 10, 13),
                        formula("Half-diminished", "m7b5", 0, 3, 6, 10),
                        formula("Fully diminished", "dim7", 0, 3, 6, 9))),
                createFamily("Barker", "classic", "QUARTAL", Style.CLASSIC, List.of(
                        formula("quartal stack", "Q stack", 0, 5, 10, 15),
                        formula("quartal stack with b7 and 9", "Q b7/9", 0, 5, 10, 14),
                        formula("quartal stack with Maj7 and 10", "Q Maj7/10", 0, 5, 11, 16),
                        formula("quartal add6 color", "Q add6", 0, 5, 9, 14),
                        formula("open fifth with quartal top", "5th+Q", 0, 7, 10, 15),
                        formula("quartal stack with upper 11", "Q +11", 0, 5, 10, 17),
                        formula("quartal stack with octave support", "Q oct", 0, 5, 12, 17),
                        formula("quartal add6 with upper 10", "Q 6/10", 0, 5, 9, 16))),
                createFamily("Sus Motion", "color", "SUSMOTION", Style.COLOR, List.of(
                        formula("7sus4", "7sus4", 0, 5, 7, 10),
                        formula("7sus2", "7sus2", 0, 2, 7, 10),
                        formula("6sus4", "6sus4", 0, 5, 7, 9),
                        formula("6sus2", "6sus2", 0, 2, 7, 9),
                        formula("7sus4(add9)", "7s4+9", 0, 5, 10, 14),
                        formula("7sus2(add9)", "7s2+9", 0, 2, 10, 14),
                        formula("sus4(add9)", "sus4+9", 0, 5, 7, 14),
                        formula("9sus4 shell", "9sus4", 0, 2, 5, 10))),
                createFamily("Quartal", "color", "QUARTALCOLOR", Style.COLOR, List.of(
                        formula("quartal stack", "Q stack", 0, 5, 10, 15),
                        formula("quartal stack with b7 and 9", "Q b7/9", 0, 5, 10, 14),
                        formula("quartal stack with Maj7 and 10", "Q Maj7/10", 0, 5, 11, 16),
                        formula("quartal add6 color", "Q add6", 0, 5, 9, 14),
                        formula("open fifth with quartal top", "5th+Q", 0, 7, 10, 15),
                        formula("quartal stack with upper 11", "Q +11", 0, 5, 10, 17),
                        formula("quartal stack with octave support", "Q oct", 0, 5, 12, 17),
                        formula("quartal add6 with upper 10", "Q 6/10", 0, 5, 9, 16))),
                createFamily("Cluster", "color", "CLUSTERLIGHT", Style.COLOR, List.of(
                        formula("add9 cluster", "add9", 0, 2, 4, 7),
                        formula("sus2sus4 cluster", "sus2/4", 0, 2, 5, 7),
                        formula("6/9 cluster", "6/9", 0, 2, 4, 9),
                        formula("6/9 shell", "6/9 sh", 0, 2, 7, 9),
                        formula("add3/#11 rub", "3/#11", 0, 4, 5, 7),
                        formula("m(add9) cluster", "m+9", 0, 2, 3, 7),
                        formula("b9sus4 cluster", "b9sus4", 0, 1, 5, 7),
                        formula("6/9 sus cluster", "6/9sus", 0, 2, 5, 9))),
                createFamily("Minor Drift", "motion", "MINORDRIFT", Style.COLOR, List.of(
                        formula("m7", "m7", 0, 3, 7, 10),
                        formula("m(add9)", "m+9", 0, 2, 3, 7),
                        formula("m6", "m6", 0, 3, 7, 9),
                        formula("major shell with b6 rub", "Maj/b6", 0, 4, 7, 8),
                        formula("7sus4(add9)", "7s4+9", 0, 5, 10, 14),
                        formula("m7sus4", "m7sus4", 0, 3, 5, 10),
                        formula("7sus2(add9)", "7s2+9", 0, 2, 10, 14),
                        formula("minor triad", "min tri", 0, 3, 7))),
                createFamily("Dorian Lift", "motion", "DORIANLIFT", Style.COLOR, List.of(
                        formula("m7", "m7", 0, 3, 7, 10),
                        formula("m6", "m6", 0, 3, 7, 9),
                        formula("Maj7", "Maj7", 0, 4, 7, 11),
                        formula("sus2sus4", "sus2/4", 0, 2, 5, 7),
                        formula("7sus2", "7sus2", 0, 2, 7, 10),
                        formula("6/9 sus shell", "6/9sus", 0, 2, 5, 9),
                        formula("Maj7(add9) shell", "Maj7+9", 0, 2, 4, 11),
                        formula("m6 reprise", "m6 rep", 0, 3, 7, 9))),
                createFamily("Root Drone", "pedalcolor", "ROOTDRONE", Style.COLOR, List.of(
                        formula("pedal root with add9/add11", "drone 9/11", 0, 7, 14, 17),
                        formula("pedal root with sus4 add9", "drone sus", 0, 5, 14, 19),
                        formula("pedal root with 6/9 add10", "drone 6/9", 0, 2, 9, 16),
                        formula("pedal root with Maj7(#11)", "drone #11", 0, 4, 11, 18),
                        formula("pedal root with fifth and 10th", "drone 5/10", 0, 7, 12, 16),
                        formula("pedal root with add9/fifth", "drone add9", 0, 2, 7, 14),
                        formula("pedal root with sus4/6/9", "drone sus69", 0, 5, 9, 14),
                        formula("pedal root with Maj7", "drone Maj7", 0, 4, 7, 11))));
    }

    private static Family createFamily(final String family, final String sourcePack, final String sourceFamily,
                                       final Style style, final List<Formula> formulas) {
        final List<Slot> slots = new ArrayList<>();
        final boolean directSlots = style == Style.PLAITS;
        for (int formulaIndex = 0; formulaIndex < formulas.size(); formulaIndex++) {
            final Formula formula = formulas.get(formulaIndex);
            final int variantCount = directSlots ? 1 : 8;
            for (int variantIndex = 0; variantIndex < variantCount; variantIndex++) {
                final int slotIndex = slots.size();
                slots.add(new Slot(
                        family,
                        sourcePack,
                        sourceFamily,
                        style,
                        formula.name(),
                        directSlots ? formula.shortLabel() : formula.shortLabel() + " " + (variantIndex + 1),
                        formula.pcs(),
                        formulaIndex,
                        variantIndex,
                        slotIndex));
            }
        }
        return new Family(family, sourcePack, sourceFamily, style, List.copyOf(formulas), List.copyOf(slots));
    }

    private static Formula formula(final String name, final String shortLabel, final int... pcs) {
        return new Formula(name, shortLabel, pcs);
    }

    private static List<int[]> renderFamily(final Family family, final RenderMode mode, final int rootMidi,
                                            final MusicalScale scale, final int rootNote) {
        final List<int[]> rendered = new ArrayList<>(family.slots().size());
        int[] previousVoicing = null;
        for (final Formula formula : family.formulas()) {
            final int[] baseNotes = mode == RenderMode.AS_IS
                    ? pcsToNotes(rootMidi, formula.pcs())
                    : scaleDegreesToNotes(scale, rootNote, formula.pcs());
            final int targetCount = family.style() == Style.PLAITS ? 1 : 8;
            final List<int[]> rawVariants = generateStepVariants(baseNotes, family.style(), targetCount);
            final List<int[]> ordered = smoothOuterOrder(rawVariants, previousVoicing);
            rendered.addAll(ordered);
            previousVoicing = copy(ordered.get(ordered.size() - 1));
        }
        return rendered;
    }

    private static List<int[]> generateStepVariants(final int[] notes, final Style style, final int targetCount) {
        final List<int[]> candidates = new ArrayList<>();
        for (int variantIndex = 0; variantIndex < 8; variantIndex++) {
            candidates.add(shapeVariant(notes, variantIndex, style));
        }
        candidates.addAll(fallbackVariants(notes, style));

        final List<int[]> unique = new ArrayList<>(targetCount);
        final List<String> seen = new ArrayList<>();
        for (final int[] candidate : candidates) {
            final int[] normalized = sort(copy(candidate));
            final String key = Arrays.toString(normalized);
            if (seen.contains(key)) {
                continue;
            }
            seen.add(key);
            unique.add(normalized);
            if (unique.size() == targetCount) {
                return unique;
            }
        }

        while (unique.size() < targetCount && !unique.isEmpty()) {
            unique.add(copy(unique.get(unique.size() - 1)));
        }
        return unique;
    }

    private static int[] shapeVariant(final int[] notes, final int variantIndex, final Style style) {
        return switch (style) {
            case CLASSIC -> classicShapeVariant(notes, variantIndex);
            case COLOR -> defaultShapeVariant(notes, variantIndex);
            case PLAITS -> plaitsShapeVariant(notes, variantIndex);
        };
    }

    private static List<int[]> fallbackVariants(final int[] notes, final Style style) {
        if (style == Style.PLAITS) {
            return plaitsFallbackVariants(notes);
        }
        final int[] base = sort(copy(notes));
        final List<int[]> variants = new ArrayList<>();
        if (base.length == 0) {
            return variants;
        }

        addFallbackVariant(variants, base, style);
        addFallbackVariant(variants, replace(base, 0, base[0] - 12), style);
        addFallbackVariant(variants, replace(base, base.length - 1, base[base.length - 1] + 12), style);
        if (base.length >= 2) {
            final int[] upperRaised = copy(base);
            upperRaised[upperRaised.length - 1] += 12;
            addFallbackVariant(variants, upperRaised, style);

            final int[] bassLowered = copy(base);
            bassLowered[0] -= 12;
            addFallbackVariant(variants, bassLowered, style);
        }
        if (base.length >= 3) {
            addFallbackVariant(variants, combine(base[0], base[1] - 12, tail(base, 2)), style);
            addFallbackVariant(variants, combine(head(base, base.length - 2), base[base.length - 2] + 12, base[base.length - 1]), style);
            addFallbackVariant(variants, combine(base[0] - 12, base[1], slice(base, 2, base.length - 1), base[base.length - 1] + 12), style);
        }
        if (base.length >= 4) {
            addFallbackVariant(variants, combine(base[0], base[1], base[2] + 12, base[3] + 12), style);
            addFallbackVariant(variants, combine(base[0] - 12, base[1], base[2], base[3] + 12), style);
        }
        return variants;
    }

    private static void addFallbackVariant(final List<int[]> variants, final int[] candidate, final Style style) {
        variants.add(applyRegisterTemplate(candidate, style));
    }

    private static List<int[]> plaitsFallbackVariants(final int[] notes) {
        final List<int[]> baseVariants = new ArrayList<>();
        final List<String> seen = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final int[] variant = applyRegisterTemplate(plaitsInversionVoicing(notes, i, 20), Style.PLAITS);
            final String key = Arrays.toString(variant);
            if (seen.contains(key)) {
                continue;
            }
            seen.add(key);
            baseVariants.add(variant);
        }

        final List<int[]> extras = new ArrayList<>();
        for (final int[] variant : baseVariants) {
            if (variant.length < 5) {
                continue;
            }
            addIfUnique(extras, seen, combine(variant[0] - 12, tail(variant, 1)));
            addIfUnique(extras, seen, combine(head(variant, variant.length - 1), variant[variant.length - 1] + 12));
            addIfUnique(extras, seen, combine(variant[0], variant[1] - 12, tail(variant, 2)));
            addIfUnique(extras, seen, combine(head(variant, variant.length - 2), variant[variant.length - 2] + 12, variant[variant.length - 1]));
            if (extras.size() >= 8) {
                break;
            }
        }
        return extras;
    }

    private static void addIfUnique(final List<int[]> extras, final List<String> seen, final int[] candidate) {
        final int[] normalized = applyRegisterTemplate(candidate, Style.PLAITS);
        final String key = Arrays.toString(normalized);
        if (seen.contains(key)) {
            return;
        }
        seen.add(key);
        extras.add(normalized);
    }

    private static List<int[]> smoothOuterOrder(final List<int[]> voicings, final int[] previousVoicing) {
        final List<int[]> remaining = new ArrayList<>();
        for (final int[] voicing : voicings) {
            remaining.add(sort(copy(voicing)));
        }
        if (remaining.isEmpty()) {
            return remaining;
        }

        final List<int[]> ordered = new ArrayList<>();
        int firstIndex = 0;
        for (int i = 1; i < remaining.size(); i++) {
            if (compareContourCost(previousVoicing, remaining.get(i), previousVoicing, remaining.get(firstIndex)) < 0) {
                firstIndex = i;
            }
        }
        ordered.add(remaining.remove(firstIndex));
        while (!remaining.isEmpty()) {
            final int[] prev = ordered.get(ordered.size() - 1);
            int nextIndex = 0;
            for (int i = 1; i < remaining.size(); i++) {
                if (compareContourCost(prev, remaining.get(i), prev, remaining.get(nextIndex)) < 0) {
                    nextIndex = i;
                }
            }
            ordered.add(remaining.remove(nextIndex));
        }
        return ordered;
    }

    private static int compareContourCost(final int[] a, final int[] b, final int[] c, final int[] d) {
        final int[] left = contourCost(a, b);
        final int[] right = contourCost(c, d);
        for (int i = 0; i < left.length; i++) {
            if (left[i] != right[i]) {
                return Integer.compare(left[i], right[i]);
            }
        }
        return 0;
    }

    private static int[] contourCost(final int[] previous, final int[] candidate) {
        final int duplicates = candidate.length - (int) Arrays.stream(candidate).distinct().count();
        if (previous == null || previous.length == 0) {
            final int span = candidate[candidate.length - 1] - candidate[0];
            final int center = average(candidate);
            return new int[]{duplicates * 24, span, Math.abs(center - MID_REGISTER_CENTER), sum(candidate)};
        }
        final int bass = Math.abs(candidate[0] - previous[0]);
        final int top = Math.abs(candidate[candidate.length - 1] - previous[previous.length - 1]);
        final int spanDelta = Math.abs((previous[previous.length - 1] - previous[0]) - (candidate[candidate.length - 1] - candidate[0]));
        final int total = voiceLeadingCost(previous, candidate);
        return new int[]{duplicates * 24 + bass * 4 + top * 3 + spanDelta, bass, top, total};
    }

    private static int voiceLeadingCost(final int[] a, final int[] b) {
        int total = 0;
        final int length = Math.min(a.length, b.length);
        for (int i = 0; i < length; i++) {
            total += Math.abs(a[i] - b[i]);
        }
        return total;
    }

    private static int[] defaultShapeVariant(final int[] notes, final int variantIndex) {
        final int[] base = sort(copy(notes));
        final int i = variantIndex % 8;
        final int[] rotated = rotateVoicing(base, i % Math.max(1, base.length));
        final int[] out = copy(rotated);
        switch (i) {
            case 1 -> out[out.length - 1] += 12;
            case 2 -> {
                if (out.length >= 2) {
                    out[out.length - 2] -= 12;
                }
            }
            case 3 -> {
                if (out.length >= 3) {
                    out[out.length - 1] += 12;
                    out[out.length - 2] += 12;
                }
            }
            case 4 -> {
                if (out.length >= 3) {
                    out[1] -= 12;
                }
            }
            case 5 -> {
                if (out.length >= 4) {
                    out[0] -= 12;
                    out[out.length - 1] += 12;
                }
            }
            case 6 -> {
                if (out.length >= 3) {
                    out[0] -= 12;
                }
            }
            case 7 -> {
                if (out.length >= 4) {
                    out[0] -= 12;
                    out[out.length - 2] += 12;
                }
            }
            default -> {
            }
        }
        return applyRegisterTemplate(out, Style.COLOR);
    }

    private static int[] classicShapeVariant(final int[] notes, final int variantIndex) {
        final int[] base = sort(copy(notes));
        final int i = variantIndex % 8;
        final int[] out = rotateVoicing(base, i % Math.max(1, base.length));
        if (i == 1) {
            out[out.length - 1] += 12;
        } else if (i == 2) {
            if (out.length >= 2) {
                out[out.length - 2] -= 12;
            }
        } else if (i == 3) {
            if (out.length >= 3) {
                out[0] -= 12;
            }
        } else if (i == 4) {
            if (out.length >= 4) {
                out[0] -= 12;
                out[out.length - 1] += 12;
            }
        } else if (i == 5) {
            out[0] += 12;
        }
        return applyRegisterTemplate(out, Style.CLASSIC);
    }

    private static int[] plaitsShapeVariant(final int[] notes, final int variantIndex) {
        return applyRegisterTemplate(plaitsInversionVoicing(notes, variantIndex, 8), Style.PLAITS);
    }

    private static int[] applyRegisterTemplate(final int[] notes, final Style style) {
        final int[] voiced = sort(copy(notes));
        if (style == Style.CLASSIC) {
            return voiced;
        }
        return normalizeNearCenter(voiced, MID_REGISTER_CENTER);
    }

    private static int[] normalizeNearCenter(final int[] notes, final int center) {
        final int[] adjusted = copy(notes);
        while (average(adjusted) < center - 6) {
            shiftAll(adjusted, 12);
        }
        while (average(adjusted) > center + 6) {
            shiftAll(adjusted, -12);
        }
        return sort(adjusted);
    }

    private static int[] scaleDegreesToNotes(final MusicalScale scale, final int rootNote, final int[] pcs) {
        final int[] notes = new int[pcs.length];
        for (int i = 0; i < pcs.length; i++) {
            notes[i] = scale.computeNote(rootNote, MID_REGISTER_OCTAVE, pcs[i]);
        }
        return notes;
    }

    private static int[] pcsToNotes(final int rootMidi, final int[] pcs) {
        final int[] notes = new int[pcs.length];
        int previous = rootMidi - 24;
        for (int i = 0; i < pcs.length; i++) {
            int note = rootMidi + pcs[i];
            while (note <= previous) {
                note += 12;
            }
            notes[i] = note;
            previous = note;
        }
        return notes;
    }

    private static int[] plaitsInversionVoicing(final int[] notes, final int variantIndex, final int steps) {
        final int[] base = sort(copy(notes));
        if (base.length == 0) {
            return base;
        }
        final int voiceCount = base.length + 1;
        final int inversionSteps = base.length * voiceCount;
        final int inversionIntegral = (variantIndex % steps) * inversionSteps / steps;
        final int numRotations = inversionIntegral / base.length;
        final int rotatedNote = inversionIntegral % base.length;

        final int[] voiced = new int[voiceCount];
        for (int i = 0; i < base.length; i++) {
            final int octave = (base.length - 1 + inversionIntegral - i) / base.length;
            final int transposedNote = base[i] + 12 * octave - 24;
            final int targetVoice = Math.floorMod(i - numRotations, voiceCount);
            final int previousVoice = Math.floorMod(targetVoice - 1, voiceCount);

            if (i == rotatedNote) {
                voiced[targetVoice] = transposedNote;
                voiced[previousVoice] = transposedNote + 12;
            } else if (i < rotatedNote) {
                voiced[previousVoice] = transposedNote;
            } else {
                voiced[targetVoice] = transposedNote;
            }
        }
        return sort(voiced);
    }

    private static int[] rotateVoicing(final int[] notes, final int amount) {
        if (notes.length == 0) {
            return notes;
        }
        final int[] rotated = copy(notes);
        for (int step = 0; step < amount; step++) {
            final int first = rotated[0];
            System.arraycopy(rotated, 1, rotated, 0, rotated.length - 1);
            rotated[rotated.length - 1] = first + 12;
        }
        return rotated;
    }

    private static int[] copy(final int[] source) {
        return Arrays.copyOf(source, source.length);
    }

    private static int[] sort(final int[] source) {
        Arrays.sort(source);
        return source;
    }

    private static int average(final int[] notes) {
        return sum(notes) / Math.max(1, notes.length);
    }

    private static int sum(final int[] notes) {
        int sum = 0;
        for (final int note : notes) {
            sum += note;
        }
        return sum;
    }

    private static void shiftAll(final int[] notes, final int semitones) {
        for (int i = 0; i < notes.length; i++) {
            notes[i] += semitones;
        }
    }

    private static int[] replace(final int[] notes, final int index, final int value) {
        final int[] replaced = copy(notes);
        replaced[index] = value;
        return replaced;
    }

    private static int[] head(final int[] notes, final int length) {
        return Arrays.copyOf(notes, length);
    }

    private static int[] tail(final int[] notes, final int startIndex) {
        return Arrays.copyOfRange(notes, startIndex, notes.length);
    }

    private static int[] slice(final int[] notes, final int startIndex, final int endExclusive) {
        return Arrays.copyOfRange(notes, startIndex, endExclusive);
    }

    private static int[] combine(final int first, final int[] rest) {
        final int[] out = new int[rest.length + 1];
        out[0] = first;
        System.arraycopy(rest, 0, out, 1, rest.length);
        return out;
    }

    private static int[] combine(final int[] start, final int last) {
        final int[] out = Arrays.copyOf(start, start.length + 1);
        out[out.length - 1] = last;
        return out;
    }

    private static int[] combine(final int first, final int second, final int[] tail) {
        final int[] out = new int[tail.length + 2];
        out[0] = first;
        out[1] = second;
        System.arraycopy(tail, 0, out, 2, tail.length);
        return out;
    }

    private static int[] combine(final int[] head, final int secondLast, final int last) {
        final int[] out = Arrays.copyOf(head, head.length + 2);
        out[out.length - 2] = secondLast;
        out[out.length - 1] = last;
        return out;
    }

    private static int[] combine(final int first, final int second, final int[] middle, final int last) {
        final int[] out = new int[middle.length + 3];
        out[0] = first;
        out[1] = second;
        System.arraycopy(middle, 0, out, 2, middle.length);
        out[out.length - 1] = last;
        return out;
    }

    private static int[] combine(final int a, final int b, final int c, final int d) {
        return new int[]{a, b, c, d};
    }
}
