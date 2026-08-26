package com.oikoaudio.fire.multiclip;

import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.values.ClipLoopWindow;

/** Owns active-Lane Clip play-start and 1/64-note nudge operations. */
final class MulticlipTimingController {
    private final MulticlipClipController clips;
    private final MulticlipPadInteractionState pads;
    private final OledDisplay oled;
    private final Context context;

    MulticlipTimingController(
            final MulticlipClipController clips,
            final MulticlipPadInteractionState pads,
            final OledDisplay oled,
            final Context context) {
        this.clips = clips;
        this.pads = pads;
        this.oled = oled;
        this.context = context;
    }

    void movePlayStart(final int direction, final boolean fine) {
        if (!context.activeLaneHasClip()) {
            oled.valueInfo("Empty lane", "Rotation unchanged");
            return;
        }
        final double loopLength = Math.max(MulticlipTiming.STEP_BEATS, clips.loopLength());
        final double step = fine ? MulticlipTiming.FINE_STEP_BEATS : MulticlipTiming.STEP_BEATS;
        final double absoluteStart =
                ClipLoopWindow.movePlayStart(
                        clips.playStart(), clips.loopStart(), loopLength, direction * step);
        final double newStart =
                ClipLoopWindow.relativeBeat(absoluteStart, clips.loopStart(), loopLength);
        clips.setPlayStart(absoluteStart);
        final String value =
                fine
                        ? "Play start %.3f beats".formatted(newStart)
                        : "Play start %d steps"
                                .formatted((int) Math.round(newStart / MulticlipTiming.STEP_BEATS));
        oled.valueInfo(value, context.activeLaneName());
    }

    void adjustLength(final int direction) {
        if (!context.activeLaneHasClip()) {
            oled.valueInfo("Empty lane", "Length unchanged");
            return;
        }
        final double current = Math.max(MulticlipTiming.STEP_BEATS, clips.loopLength());
        final double maximum = MulticlipTiming.beatsForSteps(MulticlipTiming.MAX_LOOP_STEPS);
        final double next =
                direction < 0
                        ? Math.max(MulticlipTiming.STEP_BEATS, current / 2.0)
                        : Math.min(maximum, current * 2.0);
        if (Math.abs(next - current) <= 0.0001) {
            oled.valueInfo("Clip length", direction < 0 ? "Min" : "Max");
            return;
        }
        if (direction > 0) {
            clips.duplicateContent();
        }
        clips.setLoopLength(next);
        oled.valueInfo(
                "Clip length " + MulticlipTiming.stepsForBeats(next) + " steps",
                context.activeLaneName());
    }

    void fineNudge(final int direction, final boolean heldOnly) {
        if (!context.activeLaneHasClip()) {
            oled.valueInfo("Empty lane", "Nudge ignored");
            return;
        }
        clips.fineNudge(direction, fineStep -> !heldOnly || fineStepBelongsToHeldPad(fineStep));
        if (pads.hasHeldPads()) {
            pads.consumeHeldPattern();
        }
        oled.valueInfo(
                heldOnly ? "Step nudge " + signed(direction) : "Lane nudge " + signed(direction),
                context.activeLaneName());
    }

    private boolean fineStepBelongsToHeldPad(final int fineStep) {
        final int coarseStep = MulticlipClipController.coarseStepForFineStep(fineStep);
        final int visibleStep = coarseStep - context.firstVisibleStep();
        if (visibleStep < 0 || visibleStep >= MulticlipXoxLayout.PATTERN_COUNT) {
            return false;
        }
        return pads.isHeld(MulticlipXoxLayout.PATTERN_START + visibleStep);
    }

    private String signed(final int direction) {
        return direction > 0 ? "+1/64" : "-1/64";
    }

    interface Context {
        int firstVisibleStep();

        boolean activeLaneHasClip();

        String activeLaneName();
    }
}
