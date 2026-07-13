package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.note.ChordBank;
import com.oikoaudio.fire.sequence.StepPadLightHelper;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Owns chord-step pad RGB projection for clip-row, chord-source, and step pads. */
final class ChordStepPadLightRenderer {
    private static final int PAD_COLUMNS = 16;
    private static final RgbLightState ROOT_COLOR = new RgbLightState(120, 64, 0, true);
    private static final RgbLightState IN_SCALE_COLOR = new RgbLightState(0, 72, 110, true);
    private static final RgbLightState OUT_OF_SCALE_COLOR = RgbLightState.GRAY_1;
    private static final RgbLightState OCCUPIED_STEP = new RgbLightState(0, 90, 38, true);
    private static final RgbLightState HELD_STEP = new RgbLightState(120, 88, 0, true);
    private static final RgbLightState SELECTED_CHORD = new RgbLightState(110, 24, 118, true);
    private static final RgbLightState SELECTED_BUILDER_NOTE = new RgbLightState(88, 18, 127, true);

    private final ChordStepPadSurface surface;
    private final ChordStepBuilderController builder;
    private final ChordStepChordSelection selection;
    private final IntFunction<RgbLightState> clipPadLight;
    private final Supplier<List<NoteStep>> recurrenceTargets;
    private final Supplier<RgbLightState> occupiedStepColor;
    private final IntSupplier availableSteps;
    private final IntSupplier playingStep;
    private final IntSupplier shiftedClipStartColumn;
    private final IntPredicate occupiedStep;
    private final IntPredicate accentedStep;
    private final IntPredicate sustainedStep;

    public ChordStepPadLightRenderer(
            final ChordStepPadSurface surface,
            final ChordStepBuilderController builder,
            final ChordStepChordSelection selection,
            final IntFunction<RgbLightState> clipPadLight,
            final Supplier<List<NoteStep>> recurrenceTargets,
            final Supplier<RgbLightState> occupiedStepColor,
            final IntSupplier availableSteps,
            final IntSupplier playingStep,
            final IntSupplier shiftedClipStartColumn,
            final IntPredicate occupiedStep,
            final IntPredicate accentedStep,
            final IntPredicate sustainedStep) {
        this.surface = surface;
        this.builder = builder;
        this.selection = selection;
        this.clipPadLight = clipPadLight;
        this.recurrenceTargets = recurrenceTargets;
        this.occupiedStepColor = occupiedStepColor;
        this.availableSteps = availableSteps;
        this.playingStep = playingStep;
        this.shiftedClipStartColumn = shiftedClipStartColumn;
        this.occupiedStep = occupiedStep;
        this.accentedStep = accentedStep;
        this.sustainedStep = sustainedStep;
    }

    public RgbLightState padLight(
            final int padIndex,
            final int clipRowPadCount,
            final int chordSourcePadOffset,
            final int stepPadOffset) {
        if (padIndex < clipRowPadCount) {
            return clipRowPadLight(padIndex);
        }
        if (padIndex < stepPadOffset) {
            return sourcePadLight(padIndex - chordSourcePadOffset);
        }
        return stepPadLight(padIndex - stepPadOffset);
    }

    public static RgbLightState defaultOccupiedStepColor() {
        return OCCUPIED_STEP;
    }

    public static RgbLightState inScaleColor() {
        return IN_SCALE_COLOR;
    }

    private RgbLightState clipRowPadLight(final int padIndex) {
        if (!surface.shouldShowRecurrenceRow()) {
            return clipPadLight.apply(padIndex);
        }
        final List<NoteStep> targets = recurrenceTargets.get();
        if (targets.isEmpty()) {
            return clipPadLight.apply(padIndex);
        }
        return surface.recurrencePadLight(
                padIndex, targets, occupiedStepColor.get(), clipPadLight.apply(padIndex));
    }

    private RgbLightState sourcePadLight(final int sourcePadIndex) {
        if (selection.isBuilderFamily()) {
            return builderSourcePadLight(sourcePadIndex);
        }
        if (!selection.hasSlot(sourcePadIndex)) {
            return RgbLightState.OFF;
        }
        final ChordBank.Slot slot = selection.slot(sourcePadIndex);
        final int groupIndex = sourcePadIndex / 8;
        final RgbLightState grouped =
                familyGroupColor(
                        slot.family(), groupIndex, selection.page(), selection.pageCount());
        return selection.isSlotSelected(sourcePadIndex) ? SELECTED_CHORD : grouped.getDimmed();
    }

    private RgbLightState builderSourcePadLight(final int sourcePadIndex) {
        final int midiNote = builder.noteMidiForPad(sourcePadIndex);
        if (midiNote < 0) {
            return RgbLightState.OFF;
        }
        if (builder.isNoteSelectedForPad(sourcePadIndex)) {
            return SELECTED_BUILDER_NOTE;
        }
        final RgbLightState base =
                switch (builder.padRole(sourcePadIndex)) {
                    case ROOT -> ROOT_COLOR;
                    case IN_SCALE -> IN_SCALE_COLOR;
                    case OUT_OF_SCALE -> OUT_OF_SCALE_COLOR;
                    case UNAVAILABLE -> RgbLightState.OFF;
                };
        return base;
    }

    private RgbLightState stepPadLight(final int stepIndex) {
        final boolean occupied = occupiedStep.test(stepIndex);
        final RgbLightState occupiedColor = occupiedStepColor.get();
        final int visibleSteps = availableSteps.getAsInt();
        final RgbLightState base =
                surface.stepPadLight(
                        stepIndex,
                        visibleSteps,
                        occupied,
                        occupied && accentedStep.test(stepIndex),
                        !occupied && sustainedStep.test(stepIndex),
                        playingStep.getAsInt(),
                        occupiedColor,
                        occupiedColor.getVeryDimmed(),
                        HELD_STEP);
        return StepPadLightHelper.isStepWithinVisibleLoop(stepIndex, visibleSteps)
                ? StepPadLightHelper.renderClipStartColumnOverlay(
                        Math.floorMod(stepIndex, PAD_COLUMNS),
                        shiftedClipStartColumn.getAsInt(),
                        base)
                : base;
    }

    private RgbLightState familyColor(final String family) {
        return switch (family) {
            case "Barker" -> new RgbLightState(120, 70, 0, true);
            case "Audible" -> new RgbLightState(0, 90, 110, true);
            case "Sus Motion" -> new RgbLightState(12, 100, 58, true);
            case "Quartal" -> new RgbLightState(0, 58, 120, true);
            case "Cluster" -> new RgbLightState(70, 0, 110, true);
            case "Minor Drift" -> new RgbLightState(110, 20, 36, true);
            case "Dorian Lift" -> new RgbLightState(30, 90, 18, true);
            default -> new RgbLightState(88, 64, 0, true);
        };
    }

    private RgbLightState familyGroupColor(
            final String family, final int groupIndex, final int pageIndex, final int pageCount) {
        final boolean alternatePageVariant = pageCount > 1 && (pageIndex % 2 == 1);
        return switch (groupIndex % 4) {
            case 0 ->
                    alternatePageVariant
                            ? alternateFamilyColor(family).getBrightend()
                            : familyColor(family).getBrightend();
            case 1 -> alternatePageVariant ? familyColor(family) : alternateFamilyColor(family);
            case 2 ->
                    alternatePageVariant
                            ? alternateFamilyColor(family).getDimmed()
                            : familyColor(family).getDimmed();
            default ->
                    alternatePageVariant
                            ? familyColor(family).getDimmed()
                            : alternateFamilyColor(family).getDimmed();
        };
    }

    private RgbLightState alternateFamilyColor(final String family) {
        return switch (family) {
            case "Barker" -> new RgbLightState(24, 100, 34, true);
            case "Audible" -> new RgbLightState(110, 72, 0, true);
            case "Sus Motion" -> new RgbLightState(0, 66, 122, true);
            case "Quartal" -> new RgbLightState(112, 22, 70, true);
            case "Cluster" -> new RgbLightState(24, 108, 44, true);
            case "Minor Drift" -> new RgbLightState(0, 94, 110, true);
            case "Dorian Lift" -> new RgbLightState(114, 64, 0, true);
            default -> new RgbLightState(44, 86, 24, true);
        };
    }
}
