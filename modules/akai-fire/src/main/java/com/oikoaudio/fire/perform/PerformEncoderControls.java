package com.oikoaudio.fire.perform;

import com.oikoaudio.fire.sequence.EncoderMode;
import com.bitwig.extensions.framework.Layer;

import java.util.EnumMap;
import java.util.Map;

/** Owns Perform encoder-page identity, cycling, remote target, and temporary Device-page restoration. */
public final class PerformEncoderControls {
    public enum RemoteTarget { NONE, PROJECT, TRACK, DEVICE }

    private EncoderMode mode = EncoderMode.CHANNEL;
    private EncoderMode modeBeforeMixDevice;
    private final Map<EncoderMode, Layer> layers = new EnumMap<>(EncoderMode.class);

    public EncoderMode mode() { return mode; }

    public void registerLayer(final EncoderMode mode, final Layer layer) { layers.put(mode, layer); }

    public Layer currentLayer() { return layers.get(mode); }

    public void switchMode(final EncoderMode mode) { this.mode = mode; }

    public EncoderMode nextMode() {
        return switch (mode) {
            case CHANNEL -> EncoderMode.MIXER;
            case MIXER -> EncoderMode.USER_1;
            case USER_1 -> EncoderMode.USER_2;
            case USER_2 -> EncoderMode.CHANNEL;
        };
    }

    public void enterMixDeviceMode() {
        modeBeforeMixDevice = mode;
        mode = EncoderMode.USER_2;
    }

    public void leaveMixDeviceMode() {
        if (modeBeforeMixDevice != null) {
            mode = modeBeforeMixDevice;
            modeBeforeMixDevice = null;
        }
    }

    public boolean hasModeBeforeMixDevice() { return modeBeforeMixDevice != null; }

    public void forgetModeBeforeMixDevice() { modeBeforeMixDevice = null; }

    public RemoteTarget remoteTarget() {
        return switch (mode) {
            case CHANNEL -> RemoteTarget.PROJECT;
            case MIXER -> RemoteTarget.NONE;
            case USER_1 -> RemoteTarget.TRACK;
            case USER_2 -> RemoteTarget.DEVICE;
        };
    }
}
