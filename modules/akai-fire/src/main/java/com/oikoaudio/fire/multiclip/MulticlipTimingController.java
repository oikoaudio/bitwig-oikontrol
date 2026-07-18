package com.oikoaudio.fire.multiclip;

import com.oikoaudio.fire.display.OledDisplay;

/** Owns active-Lane Clip length, play-start, and 1/64-note nudge operations. */
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

    void adjustLoopLength(final int increment) {
        if (!context.activeLaneHasClip()) {
            oled.valueInfo("Empty lane", "Length unchanged");
            return;
        }
        final int currentSteps = MulticlipTiming.stepsForBeats(clips.loopLength());
        final int newSteps = MulticlipTiming.adjustLoopSteps(currentSteps, increment);
        clips.setLoopLength(MulticlipTiming.beatsForSteps(newSteps));
        oled.valueInfo("Length " + newSteps + " steps", context.activeLaneName());
    }

    void movePlayStart(final int direction) {
        if (!context.activeLaneHasClip()) {
            oled.valueInfo("Empty lane", "Rotation unchanged");
            return;
        }
        final double loopLength = Math.max(MulticlipTiming.STEP_BEATS, clips.loopLength());
        double newStart = clips.playStart() + direction * MulticlipTiming.STEP_BEATS;
        newStart %= loopLength;
        if (newStart < 0) {
            newStart += loopLength;
        }
        clips.setPlayStart(newStart);
        final int startStep = (int) Math.round(newStart / MulticlipTiming.STEP_BEATS);
        oled.valueInfo("Play start " + startStep + " steps", context.activeLaneName());
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
