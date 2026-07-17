package com.oikoaudio.fire.perform;

/** Pure state and transitions for the mutually exclusive Perform pages. */
public record PerformPageState(Page page, int mixDevicePageIndex) {
    public enum Page {
        LAUNCHER,
        SCENE_LAUNCH,
        MIX,
        MIX_DEVICE,
        DEVICE_LAYERS,
        BIRDS_EYE
    }

    public PerformPageState {
        if (page == null) {
            throw new IllegalArgumentException("page must not be null");
        }
        if (mixDevicePageIndex < 0) {
            throw new IllegalArgumentException("mixDevicePageIndex must not be negative");
        }
        if (page != Page.MIX_DEVICE && page != Page.DEVICE_LAYERS) {
            mixDevicePageIndex = 0;
        }
    }

    public static PerformPageState launcher() {
        return new PerformPageState(Page.LAUNCHER, 0);
    }

    public boolean isTrackActionMode() {
        return page == Page.MIX || page == Page.MIX_DEVICE || page == Page.DEVICE_LAYERS;
    }

    public boolean isSceneLaunch() {
        return page == Page.SCENE_LAUNCH;
    }

    public boolean isBirdsEye() {
        return page == Page.BIRDS_EYE;
    }

    public boolean isMixDeviceMode() {
        return page == Page.MIX_DEVICE || page == Page.DEVICE_LAYERS;
    }

    public boolean isDeviceLayers() {
        return page == Page.DEVICE_LAYERS;
    }

    public PerformPageState withTrackActionMode(final boolean enabled) {
        if (enabled) {
            return isTrackActionMode() ? this : new PerformPageState(Page.MIX, 0);
        }
        return isTrackActionMode() ? launcher() : this;
    }

    public PerformPageState toggleTrackActionMode() {
        return withTrackActionMode(!isTrackActionMode());
    }

    public PerformPageState toggleSceneLaunch() {
        return isSceneLaunch() ? launcher() : new PerformPageState(Page.SCENE_LAUNCH, 0);
    }

    public PerformPageState toggleBirdsEye() {
        return isBirdsEye() ? launcher() : new PerformPageState(Page.BIRDS_EYE, 0);
    }

    public PerformPageState leaveBirdsEye() {
        return isBirdsEye() ? launcher() : this;
    }

    public PerformPageState withMixDeviceMode(final boolean enabled) {
        if (!enabled) {
            return leaveMixDeviceMode();
        }
        return isTrackActionMode() ? new PerformPageState(Page.MIX_DEVICE, 0) : this;
    }

    public PerformPageState leaveMixDeviceMode() {
        return isMixDeviceMode() ? new PerformPageState(Page.MIX, 0) : this;
    }

    public PerformPageState withMixDevicePage(final int pageIndex) {
        return isMixDeviceMode() ? new PerformPageState(page, Math.max(0, pageIndex)) : this;
    }

    public PerformPageState withDeviceLayers(final boolean enabled) {
        if (enabled && page == Page.MIX_DEVICE) {
            return new PerformPageState(Page.DEVICE_LAYERS, mixDevicePageIndex);
        }
        if (!enabled && page == Page.DEVICE_LAYERS) {
            return new PerformPageState(Page.MIX_DEVICE, mixDevicePageIndex);
        }
        return this;
    }
}
