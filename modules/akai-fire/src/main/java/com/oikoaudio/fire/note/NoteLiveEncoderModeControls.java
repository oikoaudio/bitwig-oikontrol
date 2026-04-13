package com.oikoaudio.fire.note;

import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.sequence.EncoderMode;

import java.util.EnumMap;

/**
 * Owns the live encoder-page selection and layer activation used by Note mode's live surface.
 */
final class NoteLiveEncoderModeControls {
    private final EnumMap<EncoderMode, LayerHandle> layers = new EnumMap<>(EncoderMode.class);
    private final StepSizeApplier stepSizeApplier;
    private EncoderMode mode = EncoderMode.CHANNEL;

    NoteLiveEncoderModeControls(final LayerHandle channelLayer,
                                final LayerHandle mixerLayer,
                                final LayerHandle user1Layer,
                                final LayerHandle user2Layer,
                                final StepSizeApplier stepSizeApplier) {
        layers.put(EncoderMode.CHANNEL, channelLayer);
        layers.put(EncoderMode.MIXER, mixerLayer);
        layers.put(EncoderMode.USER_1, user1Layer);
        layers.put(EncoderMode.USER_2, user2Layer);
        this.stepSizeApplier = stepSizeApplier;
    }

    void resetToChannel() {
        mode = EncoderMode.CHANNEL;
        activateCurrent();
    }

    void advanceMode() {
        mode = switch (mode) {
            case CHANNEL -> EncoderMode.MIXER;
            case MIXER -> EncoderMode.USER_1;
            case USER_1 -> EncoderMode.USER_2;
            case USER_2 -> EncoderMode.CHANNEL;
        };
        activateCurrent();
    }

    void activateCurrent() {
        deactivateAll();
        stepSizeApplier.apply(mode);
        layers.get(mode).activate();
    }

    void deactivateAll() {
        for (final LayerHandle layer : layers.values()) {
            layer.deactivate();
        }
    }

    BiColorLightState lightState() {
        return mode.getState();
    }

    String modeInfo() {
        return modeInfo(mode);
    }

    EncoderMode mode() {
        return mode;
    }

    static String modeInfo(final EncoderMode mode) {
        return switch (mode) {
            case CHANNEL -> "1: Mod\n2: Pitch Gliss\n3: Velocity\n4: Scale";
            case MIXER -> "1: Volume\n2: Pan\n3: Send 1\n4: Send 2";
            case USER_1 -> "1: Aftertouch\n2: Pressure\n3: Timbre\n4: Pitch Expr";
            case USER_2 -> "1: Remote 1\n2: Remote 2\n3: Remote 3\n4: Remote 4";
        };
    }

    interface LayerHandle {
        void activate();

        void deactivate();
    }

    @FunctionalInterface
    interface StepSizeApplier {
        void apply(EncoderMode mode);
    }
}
