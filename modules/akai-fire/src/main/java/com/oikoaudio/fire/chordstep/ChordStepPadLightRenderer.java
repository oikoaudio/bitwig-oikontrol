package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.note.ChordBank;
import com.oikoaudio.fire.sequence.StepPadLightHelper;

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Owns chord-step pad RGB projection for clip-row, chord-source, and step pads.
 */
public final class ChordStepPadLightRenderer {
    private static final int PAD_COLUMNS = 16;
    private static final RgbLigthState ROOT_COLOR = new RgbLigthState(120, 64, 0, true);
    private static final RgbLigthState IN_SCALE_COLOR = new RgbLigthState(0, 72, 110, true);
    private static final RgbLigthState OUT_OF_SCALE_COLOR = RgbLigthState.GRAY_1;
    private static final RgbLigthState OCCUPIED_STEP = new RgbLigthState(0, 90, 38, true);
    private static final RgbLigthState HELD_STEP = new RgbLigthState(120, 88, 0, true);
    private static final RgbLigthState SELECTED_CHORD = new RgbLigthState(110, 24, 118, true);
    private static final RgbLigthState SELECTED_BUILDER_NOTE = new RgbLigthState(88, 18, 127, true);

    private final ChordStepPadSurface surface;
    private final ChordStepBuilderController builder;
    private final ChordStepChordSelection selection;
    private final IntFunction<RgbLigthState> clipPadLight;
    private final Supplier<List<NoteStep>> recurrenceTargets;
    private final Supplier<RgbLigthState> occupiedStepColor;
    private final IntSupplier availableSteps;
    private final IntSupplier playingStep;
    private final IntSupplier shiftedClipStartColumn;
    private final IntPredicate occupiedStep;
    private final IntPredicate accentedStep;
    private final IntPredicate sustainedStep;

    public ChordStepPadLightRenderer(final ChordStepPadSurface surface,
                                     final ChordStepBuilderController builder,
                                     final ChordStepChordSelection selection,
                                     final IntFunction<RgbLigthState> clipPadLight,
                                     final Supplier<List<NoteStep>> recurrenceTargets,
                                     final Supplier<RgbLigthState> occupiedStepColor,
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

    public RgbLigthState padLight(final int padIndex,
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

    public static RgbLigthState defaultOccupiedStepColor() {
        return OCCUPIED_STEP;
    }

    public static RgbLigthState inScaleColor() {
        return IN_SCALE_COLOR;
    }

    private RgbLigthState clipRowPadLight(final int padIndex) {
        if (!surface.shouldShowRecurrenceRow()) {
            return clipPadLight.apply(padIndex);
        }
        final List<NoteStep> targets = recurrenceTargets.get();
        if (targets.isEmpty()) {
            return clipPadLight.apply(padIndex);
        }
        return surface.recurrencePadLight(padIndex, targets, occupiedStepColor.get(), clipPadLight.apply(padIndex));
    }

    private RgbLigthState sourcePadLight(final int sourcePadIndex) {
        if (selection.isBuilderFamily()) {
            return builderSourcePadLight(sourcePadIndex);
        }
        if (!selection.hasSlot(sourcePadIndex)) {
            return RgbLigthState.OFF;
        }
        final ChordBank.Slot slot = selection.slot(sourcePadIndex);
        final int groupIndex = sourcePadIndex / 8;
        final RgbLigthState grouped = familyGroupColor(slot.family(), groupIndex, selection.page(),
                selection.pageCount());
        return sourcePadIndex == selection.selectedSlot() ? SELECTED_CHORD : grouped.getDimmed();
    }

    private RgbLigthState builderSourcePadLight(final int sourcePadIndex) {
        final int midiNote = builder.noteMidiForPad(sourcePadIndex);
        if (midiNote < 0) {
            return RgbLigthState.OFF;
        }
        if (builder.isNoteSelectedForPad(sourcePadIndex)) {
            return SELECTED_BUILDER_NOTE;
        }
        final RgbLigthState base = switch (builder.padRole(sourcePadIndex)) {
            case ROOT -> ROOT_COLOR;
            case IN_SCALE -> IN_SCALE_COLOR;
            case OUT_OF_SCALE -> OUT_OF_SCALE_COLOR;
            case UNAVAILABLE -> RgbLigthState.OFF;
        };
        return base;
    }

    private RgbLigthState stepPadLight(final int stepIndex) {
        final boolean occupied = occupiedStep.test(stepIndex);
        final RgbLigthState occupiedColor = occupiedStepColor.get();
        final int visibleSteps = availableSteps.getAsInt();
        final RgbLigthState base = surface.stepPadLight(
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
                        Math.floorMod(stepIndex, PAD_COLUMNS), shiftedClipStartColumn.getAsInt(), base)
                : base;
    }

    private RgbLigthState familyColor(final String family) {
        return switch (family) {
            case "Barker" -> new RgbLigthState(120, 70, 0, true);
            case "Audible" -> new RgbLigthState(0, 90, 110, true);
            case "Sus Motion" -> new RgbLigthState(12, 100, 58, true);
            case "Quartal" -> new RgbLigthState(0, 58, 120, true);
            case "Cluster" -> new RgbLigthState(70, 0, 110, true);
            case "Minor Drift" -> new RgbLigthState(110, 20, 36, true);
            case "Dorian Lift" -> new RgbLigthState(30, 90, 18, true);
            default -> new RgbLigthState(88, 64, 0, true);
        };
    }

    private RgbLigthState familyGroupColor(final String family, final int groupIndex, final int pageIndex,
                                           final int pageCount) {
        final boolean alternatePageVariant = pageCount > 1 && (pageIndex % 2 == 1);
        return switch (groupIndex % 4) {
            case 0 -> alternatePageVariant ? alternateFamilyColor(family).getBrightend()
                    : familyColor(family).getBrightend();
            case 1 -> alternatePageVariant ? familyColor(family) : alternateFamilyColor(family);
            case 2 -> alternatePageVariant ? alternateFamilyColor(family).getDimmed()
                    : familyColor(family).getDimmed();
            default -> alternatePageVariant ? familyColor(family).getDimmed()
                    : alternateFamilyColor(family).getDimmed();
        };
    }

    private RgbLigthState alternateFamilyColor(final String family) {
        return switch (family) {
            case "Barker" -> new RgbLigthState(24, 100, 34, true);
            case "Audible" -> new RgbLigthState(110, 72, 0, true);
            case "Sus Motion" -> new RgbLigthState(0, 66, 122, true);
            case "Quartal" -> new RgbLigthState(112, 22, 70, true);
            case "Cluster" -> new RgbLigthState(24, 108, 44, true);
            case "Minor Drift" -> new RgbLigthState(0, 94, 110, true);
            case "Dorian Lift" -> new RgbLigthState(114, 64, 0, true);
            default -> new RgbLigthState(44, 86, 24, true);
        };
    }
}
