package com.oikoaudio.fire;

import com.bitwig.extension.controller.api.Application;
import java.util.function.BiConsumer;

/** Owns the global PATTERN + BANK track-creation gesture. */
final class GlobalTrackCreationController {
    private final Application application;
    private final Runnable consumePatternGesture;
    private final BiConsumer<String, String> notifyAction;
    private final boolean[] capturedDirections = new boolean[2];

    GlobalTrackCreationController(
            final Application application,
            final Runnable consumePatternGesture,
            final BiConsumer<String, String> notifyAction) {
        this.application = application;
        this.consumePatternGesture = consumePatternGesture;
        this.notifyAction = notifyAction;
    }

    boolean handleBankButton(
            final boolean pressed,
            final int direction,
            final boolean patternHeld,
            final boolean altHeld,
            final String selectedTrackType,
            final int mainTrackPosition,
            final int effectTrackPosition) {
        final int directionIndex = direction < 0 ? 0 : 1;
        if (!pressed) {
            if (!capturedDirections[directionIndex]) {
                return false;
            }
            capturedDirections[directionIndex] = false;
            return true;
        }
        if (!patternHeld) {
            return false;
        }

        capturedDirections[directionIndex] = true;
        consumePatternGesture.run();
        final boolean after = direction > 0;
        final String placement = after ? "After" : "Before";
        if ("Effect".equals(selectedTrackType)) {
            application.createEffectTrack(insertionPosition(effectTrackPosition, after));
            notifyAction.accept("New FX Track", placement);
        } else if (altHeld) {
            application.createAudioTrack(insertionPosition(mainTrackPosition, after));
            notifyAction.accept("New Audio Track", placement);
        } else {
            application.createInstrumentTrack(insertionPosition(mainTrackPosition, after));
            notifyAction.accept("New Instrument", placement);
        }
        return true;
    }

    private static int insertionPosition(final int selectedPosition, final boolean after) {
        if (selectedPosition < 0) {
            return -1;
        }
        return selectedPosition + (after ? 1 : 0);
    }
}
