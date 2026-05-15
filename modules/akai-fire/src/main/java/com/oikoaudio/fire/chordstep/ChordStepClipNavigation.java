package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.values.StepViewPosition;

/**
 * Owns chord-step clip navigation controls: step-page movement, clip length, and play-start offsets.
 */
public final class ChordStepClipNavigation {
    private static final double STEP_LENGTH = 0.25;

    private final PinnableCursorClip clip;
    private final StepViewPosition position;
    private final OledDisplay oled;
    private final PopupFeedback popupFeedback;
    private final Runnable clearFineNudgeSessions;
    private final Runnable refreshObservation;
    private final int maxSteps;
    private final int fineStepsPerStep;
    private final double fineStepLength;

    public ChordStepClipNavigation(final PinnableCursorClip clip,
                                   final StepViewPosition position,
                                   final OledDisplay oled,
                                   final PopupFeedback popupFeedback,
                                   final Runnable clearFineNudgeSessions,
                                   final Runnable refreshObservation,
                                   final int maxSteps,
                                   final int fineStepsPerStep,
                                   final double fineStepLength) {
        this.clip = clip;
        this.position = position;
        this.oled = oled;
        this.popupFeedback = popupFeedback;
        this.clearFineNudgeSessions = clearFineNudgeSessions;
        this.refreshObservation = refreshObservation;
        this.maxSteps = maxSteps;
        this.fineStepsPerStep = fineStepsPerStep;
        this.fineStepLength = fineStepLength;
    }

    public void page(final int direction, final String detail) {
        final int previousPage = position.getCurrentPage();
        if (direction < 0) {
            position.scrollLeft();
        } else if (direction > 0) {
            position.scrollRight();
        }
        if (position.getCurrentPage() == previousPage) {
            return;
        }
        clearFineNudgeSessions.run();
        refreshObservation.run();
        showPageInfo(detail);
    }

    public void adjustLength(final int direction, final ClipSlotAvailability availability) {
        if (!availability.ensureSelectedNoteClipSlot()) {
            return;
        }
        final double currentLength = clip.getLoopLength().get();
        if (direction < 0) {
            if (currentLength <= STEP_LENGTH) {
                oled.valueInfo("Clip Length", "Min");
                return;
            }
            final double newLength = Math.max(STEP_LENGTH, currentLength / 2.0);
            clip.getLoopLength().set(newLength);
            handleLengthChanged(newLength);
            oled.valueInfo("Clip Length", formatBars(newLength));
            return;
        }
        final double newLength = Math.min(maxSteps * STEP_LENGTH, currentLength * 2.0);
        if (newLength <= currentLength) {
            oled.valueInfo("Clip Length", "Max");
            return;
        }
        clip.duplicateContent();
        clip.getLoopLength().set(newLength);
        handleLengthChanged(newLength);
        oled.valueInfo("Clip Length", formatBars(newLength));
    }

    public void adjustPlayStart(final int direction, final boolean fine, final ClipSlotAvailability availability) {
        if (!availability.ensureSelectedNoteClipSlot()) {
            return;
        }
        final double loopLength = Math.max(STEP_LENGTH, clip.getLoopLength().get());
        final double step = fine ? fineStepLength : STEP_LENGTH;
        final double next = wrapBeatTime(clip.getPlayStart().get() + direction * step, loopLength);
        clip.getPlayStart().set(next);
        final String title = fine ? "Clip Start Fine" : "Clip Start";
        final String value = formatPlayStart(next);
        oled.valueInfo(title, value);
        popupFeedback.notify(title, value);
    }

    public void snapPlayStartToGrid(final ClipSlotAvailability availability) {
        if (!availability.ensureSelectedNoteClipSlot()) {
            return;
        }
        final double loopLength = Math.max(STEP_LENGTH, clip.getLoopLength().get());
        final double current = wrapBeatTime(clip.getPlayStart().get(), loopLength);
        final double next = wrapBeatTime(Math.round(current / STEP_LENGTH) * STEP_LENGTH, loopLength);
        clip.getPlayStart().set(next);
        final String value = formatPlayStart(next);
        oled.valueInfo("Clip Start Snap", value);
        popupFeedback.notify("Clip Start Snap", value);
    }

    public String pageLabel(final String detail) {
        return "Chord %d/%d".formatted(position.getCurrentPage() + 1, pageCount()) + "\n" + detail;
    }

    public int pageCount() {
        return Math.max(1, position.getPages());
    }

    public void showPageInfo(final String detail) {
        oled.valueInfo("Chord %d/%d".formatted(position.getCurrentPage() + 1, pageCount()), detail);
    }

    private void handleLengthChanged(final double newLength) {
        position.handleLoopLengthChanged(newLength);
        clearFineNudgeSessions.run();
        refreshObservation.run();
    }

    private String formatBars(final double beatLength) {
        final double bars = beatLength / 4.0;
        if (Math.rint(bars) == bars) {
            return Integer.toString((int) bars) + " Bars";
        }
        return String.format("%.2f Bars", bars);
    }

    private double wrapBeatTime(final double value, final double length) {
        final double wrapped = value % length;
        return wrapped < 0 ? wrapped + length : wrapped;
    }

    private String formatPlayStart(final double beatTime) {
        final int fineStepIndex = (int) Math.round(beatTime / fineStepLength);
        final int quarterNote = (int) Math.floor(beatTime);
        final int bar = quarterNote / 4;
        final int beat = quarterNote % 4;
        final int sixteenth = (int) Math.floor((beatTime - quarterNote) / STEP_LENGTH);
        final int fine = Math.floorMod(fineStepIndex, fineStepsPerStep);
        return fine == 0
                ? "%d.%d.%d".formatted(bar + 1, beat + 1, sixteenth + 1)
                : "%d.%d.%d+%d".formatted(bar + 1, beat + 1, sixteenth + 1, fine);
    }

    @FunctionalInterface
    public interface ClipSlotAvailability {
        boolean ensureSelectedNoteClipSlot();
    }

    @FunctionalInterface
    public interface PopupFeedback {
        void notify(String title, String value);
    }
}
