package com.oikoaudio.fire.nestedrhythm;

import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.ClipRowHandler;
import com.oikoaudio.fire.sequence.RecurrencePadInteraction;
import com.oikoaudio.fire.sequence.RecurrencePattern;
import com.oikoaudio.fire.sequence.StepPadLightHelper;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

final class NestedRhythmPadSurface {
    static final int CLIP_ROW_PAD_COUNT = 16;
    static final int STRUCTURE_PAD_OFFSET = 16;
    static final int STRUCTURE_PAD_COUNT = 32;
    static final int VELOCITY_PAD_OFFSET = 48;
    static final int VELOCITY_PAD_COUNT = 16;

    private final List<NestedRhythmEditablePulse> pulses;
    private final ClipRowHandler clipHandler;
    private final OledDisplay oled;
    private final BooleanSupplier fixedLengthHeld;
    private final BooleanSupplier shiftHeld;
    private final IntSupplier totalFineStepCount;
    private final IntSupplier shiftedClipStartColumn;
    private final IntConsumer setLastStep;
    private final IntFunction<RgbLigthState> lastStepPadLight;
    private final Supplier<RgbLigthState> clipBaseColor;
    private final BiConsumer<String, String> applyEditablePattern;
    private final RecurrencePadInteraction recurrencePads = new RecurrencePadInteraction(true);
    private int selectedPulseIndex = -1;
    private int heldPulseIndex = -1;
    private int pressedHitIndex = -1;
    private boolean heldPulseConsumed = false;
    private int playingFineStep = -1;

    NestedRhythmPadSurface(final List<NestedRhythmEditablePulse> pulses,
                           final ClipRowHandler clipHandler,
                           final OledDisplay oled,
                           final BooleanSupplier fixedLengthHeld,
                           final BooleanSupplier shiftHeld,
                           final IntSupplier totalFineStepCount,
                           final IntSupplier shiftedClipStartColumn,
                           final IntConsumer setLastStep,
                           final IntFunction<RgbLigthState> lastStepPadLight,
                           final Supplier<RgbLigthState> clipBaseColor,
                           final BiConsumer<String, String> applyEditablePattern) {
        this.pulses = pulses;
        this.clipHandler = clipHandler;
        this.oled = oled;
        this.fixedLengthHeld = fixedLengthHeld;
        this.shiftHeld = shiftHeld;
        this.totalFineStepCount = totalFineStepCount;
        this.shiftedClipStartColumn = shiftedClipStartColumn;
        this.setLastStep = setLastStep;
        this.lastStepPadLight = lastStepPadLight;
        this.clipBaseColor = clipBaseColor;
        this.applyEditablePattern = applyEditablePattern;
    }

    void handlePadPress(final int padIndex, final boolean pressed) {
        if (padIndex < CLIP_ROW_PAD_COUNT && hasHeldPulse()) {
            handleRecurrencePadPress(padIndex, pressed);
            return;
        }
        if (padIndex < CLIP_ROW_PAD_COUNT) {
            clipHandler.handlePadPress(padIndex, pressed);
            return;
        }
        if (fixedLengthHeld.getAsBoolean() && padIndex < VELOCITY_PAD_OFFSET) {
            if (pressed) {
                setLastStep.accept(padIndex - STRUCTURE_PAD_OFFSET);
            }
            return;
        }
        if (padIndex < VELOCITY_PAD_OFFSET) {
            if (!pressed) {
                heldPulseIndex = -1;
                recurrencePads.clearHold();
            } else {
                holdPulseForStructurePad(padIndex - STRUCTURE_PAD_OFFSET);
            }
            return;
        }
        handleHitPadPress(padIndex - VELOCITY_PAD_OFFSET, pressed);
    }

    RgbLigthState getPadLight(final int padIndex) {
        if (padIndex < CLIP_ROW_PAD_COUNT && recurrencePads.shouldShowRow(hasHeldPulse())) {
            return recurrencePadLight(padIndex);
        }
        if (padIndex < CLIP_ROW_PAD_COUNT) {
            return clipHandler.getPadLight(padIndex);
        }
        if (fixedLengthHeld.getAsBoolean() && padIndex < VELOCITY_PAD_OFFSET) {
            return lastStepPadLight.apply(padIndex - STRUCTURE_PAD_OFFSET);
        }
        if (padIndex < VELOCITY_PAD_OFFSET) {
            return structurePadLight(padIndex - STRUCTURE_PAD_OFFSET);
        }
        return velocityPadLight(padIndex - VELOCITY_PAD_OFFSET);
    }

    void handlePlayingStep(final int fineStep) {
        playingFineStep = fineStep >= 0 ? fineStep : -1;
    }

    boolean hasHeldPulse() {
        return activePulseIndex() >= 0;
    }

    int activePulseIndex() {
        return heldPulseIndex >= 0 && heldPulseIndex < pulses.size() ? heldPulseIndex : -1;
    }

    void markHeldPulseConsumed() {
        if (hasHeldPulse()) {
            heldPulseConsumed = true;
        }
    }

    int heldPulseFineStart() {
        return heldPulseIndex >= 0 && heldPulseIndex < pulses.size()
                ? pulses.get(heldPulseIndex).fineStart
                : -1;
    }

    void afterPatternRegenerated(final int previousSelectionFineStart,
                                 final NestedRhythmEditablePattern editablePattern) {
        selectedPulseIndex = editablePattern.findSelectedPulseIndex(previousSelectionFineStart, selectedPulseIndex);
        heldPulseIndex = -1;
    }

    int structureBinFor(final int fineStart) {
        final double normalized = fineStart / (double) Math.max(1, totalFineStepCount.getAsInt());
        return Math.max(0, Math.min(STRUCTURE_PAD_COUNT - 1,
                (int) Math.floor(normalized * STRUCTURE_PAD_COUNT)));
    }

    NestedRhythmEditablePulse strongestPulseInBin(final int bin) {
        NestedRhythmEditablePulse best = null;
        for (final NestedRhythmEditablePulse pulse : pulses) {
            if (structureBinFor(pulse.fineStart) != bin) {
                continue;
            }
            if (best == null || pulse.effectiveVelocity() > best.effectiveVelocity()) {
                best = pulse;
            }
        }
        return best;
    }

    int playingPulseIndex() {
        if (playingFineStep < 0) {
            return -1;
        }
        for (int index = 0; index < pulses.size(); index++) {
            final NestedRhythmEditablePulse pulse = pulses.get(index);
            if (pulse.enabled && pulse.containsFineStep(playingFineStep, totalFineStepCount.getAsInt())) {
                return index;
            }
        }
        return -1;
    }

    private void handleHitPadPress(final int hitIndex, final boolean pressed) {
        if (!pressed) {
            if (hitIndex == pressedHitIndex && !heldPulseConsumed && !shiftHeld.getAsBoolean()
                    && hitIndex >= 0 && hitIndex < pulses.size()) {
                final NestedRhythmEditablePulse pulse = pulses.get(hitIndex);
                pulse.enabled = !pulse.enabled;
                applyEditablePattern.accept("Hit", pulse.enabled ? "On" : "Off");
            }
            pressedHitIndex = -1;
            heldPulseConsumed = false;
            heldPulseIndex = -1;
            recurrencePads.clearHold();
            return;
        }
        if (hitIndex >= pulses.size()) {
            return;
        }
        heldPulseIndex = hitIndex;
        pressedHitIndex = hitIndex;
        heldPulseConsumed = false;
        recurrencePads.beginHoldIfNeeded(false);
        showHeldPulse();
        if (shiftHeld.getAsBoolean()) {
            pulses.get(hitIndex).resetEdits();
            heldPulseConsumed = true;
            applyEditablePattern.accept("Hit", "Reset");
        }
    }

    private void holdPulseForStructurePad(final int structurePad) {
        final int bin = Math.max(0, Math.min(STRUCTURE_PAD_COUNT - 1, structurePad));
        int bestIndex = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < pulses.size(); index++) {
            final NestedRhythmEditablePulse pulse = pulses.get(index);
            final int pulseBin = structureBinFor(pulse.fineStart);
            final int distance = Math.abs(pulseBin - bin);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        if (bestIndex >= 0) {
            recurrencePads.beginHoldIfNeeded(hasHeldPulse());
            heldPulseIndex = bestIndex;
            showHeldPulse();
        }
    }

    private void showHeldPulse() {
        if (heldPulseIndex < 0 || heldPulseIndex >= pulses.size()) {
            oled.valueInfo("Hit", "None");
            return;
        }
        final NestedRhythmEditablePulse pulse = pulses.get(heldPulseIndex);
        final RecurrencePattern recurrence = pulse.effectiveRecurrence();
        oled.valueInfo("Hit %d".formatted(heldPulseIndex + 1),
                "%s Vel %d Rec %s".formatted(pulse.roleLabel(), pulse.effectiveVelocity(), recurrence.summary()));
    }

    private void handleRecurrencePadPress(final int padIndex, final boolean pressed) {
        if (padIndex >= RecurrencePattern.EDITOR_DEFAULT_SPAN) {
            return;
        }
        final RecurrencePattern recurrence = hasHeldPulse()
                ? pulses.get(activePulseIndex()).effectiveRecurrence()
                : RecurrencePattern.of(0, 0);
        recurrencePads.handlePadPress(padIndex, pressed, hasHeldPulse(),
                recurrence,
                this::markHeldPulseConsumed,
                this::toggleHeldPulseRecurrencePad,
                this::applyHeldPulseRecurrenceSpan);
    }

    private void toggleHeldPulseRecurrencePad(final int padIndex) {
        final NestedRhythmEditablePulse pulse = pulses.get(activePulseIndex());
        final RecurrencePattern updated = pulse.effectiveRecurrence().toggledAt(padIndex);
        pulse.recurrenceLength = updated.length();
        pulse.recurrenceMask = updated.mask();
        pulse.recurrenceEdited = true;
        applyEditablePattern.accept("Recurrence", updated.summary());
    }

    private void applyHeldPulseRecurrenceSpan(final int span) {
        final NestedRhythmEditablePulse pulse = pulses.get(activePulseIndex());
        final RecurrencePattern updated = pulse.effectiveRecurrence().applySpanGesture(span);
        pulse.recurrenceLength = updated.length();
        pulse.recurrenceMask = updated.mask();
        pulse.recurrenceEdited = true;
        applyEditablePattern.accept("Recurrence", updated.summary());
    }

    private RgbLigthState recurrencePadLight(final int padIndex) {
        if (!hasHeldPulse()) {
            return clipHandler.getPadLight(padIndex);
        }
        if (padIndex >= RecurrencePattern.EDITOR_DEFAULT_SPAN) {
            return RgbLigthState.OFF;
        }
        final NestedRhythmEditablePulse pulse = pulses.get(activePulseIndex());
        return recurrencePads.padLight(padIndex, pulse.effectiveRecurrence(), clipBaseColor.get());
    }

    private RgbLigthState structurePadLight(final int bin) {
        final NestedRhythmEditablePulse pulse = strongestPulseInBin(bin);
        final int playingBin = playingFineStep < 0 ? -1 : structureBinFor(playingFineStep);
        final RgbLigthState base;
        if (pulse == null) {
            base = StepPadLightHelper.renderEmptyStep(bin, playingBin);
        } else {
            base = pulse.enabled
                    ? colorForVelocity(pulse.effectiveVelocity(), clipBaseColor.get())
                    : disabledPulseColor();
        }
        final RgbLigthState withPlayhead = playingBin == bin ? StepPadLightHelper.renderPlayheadHighlight(base) : base;
        return StepPadLightHelper.renderClipStartColumnOverlay(
                Math.floorMod(bin, CLIP_ROW_PAD_COUNT), shiftedClipStartColumn.getAsInt(), withPlayhead);
    }

    private RgbLigthState velocityPadLight(final int hitIndex) {
        if (hitIndex >= pulses.size()) {
            return RgbLigthState.OFF;
        }
        final int playingHitIndex = playingPulseIndex();
        final NestedRhythmEditablePulse pulse = pulses.get(hitIndex);
        final RgbLigthState base = pulse.enabled
                ? colorForVelocity(pulse.effectiveVelocity(), clipBaseColor.get())
                : disabledPulseColor();
        return playingHitIndex == hitIndex ? StepPadLightHelper.renderPlayheadHighlight(base) : base;
    }

    private RgbLigthState colorForVelocity(final int velocity, final RgbLigthState base) {
        if (velocity >= 112) {
            return base.getBrightest();
        }
        if (velocity >= 96) {
            return base.getBrightend();
        }
        if (velocity >= 78) {
            return base;
        }
        return base.getDimmed();
    }

    private RgbLigthState disabledPulseColor() {
        return clipBaseColor.get().getVeryDimmed();
    }
}
