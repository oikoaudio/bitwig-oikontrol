package com.oikoaudio.fire.fugue;

import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.EncoderFooterLegend;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.sequence.EncoderMode;

/** Owns Fugue's encoder page/selected-line state and page presentation. */
public final class FugueEncoderControls {
    private static final int THRESHOLD = 5;
    private static final int FINE_THRESHOLD = 10;
    private EncoderMode mode = EncoderMode.CHANNEL;

    public EncoderMode mode() {
        return mode;
    }

    public int selectedLine() {
        return switch (mode) {
            case CHANNEL -> 0;
            case MIXER -> 1;
            case USER_1 -> 2;
            case USER_2 -> 3;
        };
    }

    public void selectLine(final int line) {
        mode = switch (Math.max(0, Math.min(3, line))) {
            case 1 -> EncoderMode.MIXER;
            case 2 -> EncoderMode.USER_1;
            case 3 -> EncoderMode.USER_2;
            default -> EncoderMode.CHANNEL;
        };
    }

    public void select(final EncoderMode mode) {
        this.mode = mode;
    }

    public void cycle() {
        mode = switch (mode) {
            case CHANNEL -> EncoderMode.MIXER;
            case MIXER -> EncoderMode.USER_1;
            case USER_1 -> EncoderMode.USER_2;
            case USER_2 -> EncoderMode.CHANNEL;
        };
    }

    public BiColorLightState light() {
        return mode.getState();
    }

    public String title() {
        return selectedLine() == FugueClipAdapter.SOURCE_CHANNEL ? "Fugue Settings" : "Var " + (selectedLine() + 1);
    }

    public String details() {
        return selectedLine() == FugueClipAdapter.SOURCE_CHANNEL
                ? "1 Root\n2 Scale\n3 Clip Len\n4 Clip Start"
                : "1 Dir\n2 Tempo\n3 Start\n4 Pitch";
    }

    public String footer() {
        return selectedLine() == FugueClipAdapter.SOURCE_CHANNEL
                ? EncoderFooterLegend.of("Root", "Scal", "Lgth", "Strt")
                : EncoderFooterLegend.of("Dir", "Temp", "Strt", "Ptch");
    }

    public void bind(final AkaiFireOikontrolExtension driver, final Layer owner, final Handler handler) {
        final TouchEncoder[] encoders = driver.getEncoders();
        for (int index = 0; index < encoders.length; index++) {
            final int encoderIndex = index;
            encoders[index].bindThresholdedEncoder(owner, THRESHOLD, FINE_THRESHOLD,
                    driver::isGlobalShiftHeld, increment -> handler.turn(encoderIndex, increment));
            encoders[index].bindTouched(owner, touched -> handler.touch(encoderIndex, touched));
        }
    }

    public interface Handler {
        void turn(int encoderIndex, int increment);

        void touch(int encoderIndex, boolean touched);
    }
}
