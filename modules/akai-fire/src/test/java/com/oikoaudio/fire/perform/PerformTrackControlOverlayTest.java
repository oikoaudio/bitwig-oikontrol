package com.oikoaudio.fire.perform;

import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.sequence.EncoderMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerformTrackControlOverlayTest {

    @Test
    void topRowPadsSelectTracksByDefault() {
        assertEquals("Select", PerformClipLauncherMode.trackControlActionForPad(0, false));
        assertEquals("Select", PerformClipLauncherMode.trackControlActionForPad(15, false));
    }

    @Test
    void altTopRowPadsStopTracks() {
        assertEquals("Stop", PerformClipLauncherMode.trackControlActionForPad(0, true));
        assertEquals("Stop", PerformClipLauncherMode.trackControlActionForPad(15, true));
    }

    @Test
    void lowerTrackControlRowsKeepMixerActions() {
        assertEquals("Solo", PerformClipLauncherMode.trackControlActionForPad(16, false));
        assertEquals("Mute", PerformClipLauncherMode.trackControlActionForPad(32, false));
        assertEquals("Arm", PerformClipLauncherMode.trackControlActionForPad(48, false));
    }

    @Test
    void onlySelectAndExclusiveArmActionsFollowSelection() {
        assertEquals(true, PerformClipLauncherMode.trackActionShouldSelectForPad(0, false, false));
        assertEquals(false, PerformClipLauncherMode.trackActionShouldSelectForPad(0, true, false));
        assertEquals(false, PerformClipLauncherMode.trackActionShouldSelectForPad(16, false, true));
        assertEquals(false, PerformClipLauncherMode.trackActionShouldSelectForPad(32, false, true));
        assertEquals(false, PerformClipLauncherMode.trackActionShouldSelectForPad(48, false, false));
        assertEquals(true, PerformClipLauncherMode.trackActionShouldSelectForPad(48, false, true));
        assertEquals(true, PerformClipLauncherMode.trackActionShouldSelectForPad(48, true, false));
        assertEquals(false, PerformClipLauncherMode.trackActionShouldSelectForPad(48, true, true));
    }

    @Test
    void altArmInvertsExclusiveArmPreference() {
        assertEquals(false, PerformClipLauncherMode.trackArmUsesExclusive(false, false));
        assertEquals(true, PerformClipLauncherMode.trackArmUsesExclusive(false, true));
        assertEquals(true, PerformClipLauncherMode.trackArmUsesExclusive(true, false));
        assertEquals(false, PerformClipLauncherMode.trackArmUsesExclusive(true, true));
    }

    @Test
    void mixKeepsSixteenTrackColumnsWhenLauncherLayoutIsHorizontal() {
        final PerformLayout horizontal = PerformLayout.horizontal();

        assertEquals(4, PerformClipLauncherMode.visibleTrackCountForPage(horizontal, false));
        assertEquals(16, PerformClipLauncherMode.visibleTrackCountForPage(horizontal, true));
    }

    @Test
    void externalTrackSelectionInfoIgnoresLocalAndDuplicateSelections() {
        assertEquals(false, PerformClipLauncherMode.shouldShowExternalTrackSelectionInfo(false, 2, -1, -1));
        assertEquals(false, PerformClipLauncherMode.shouldShowExternalTrackSelectionInfo(true, 2, 2, -1));
        assertEquals(false, PerformClipLauncherMode.shouldShowExternalTrackSelectionInfo(true, 2, -1, 2));
        assertEquals(true, PerformClipLauncherMode.shouldShowExternalTrackSelectionInfo(true, 2, -1, 1));
    }

    @Test
    void performMetersOnlyShowWhileTransportIsPlaying() {
        assertEquals(true, PerformClipLauncherMode.shouldShowPerformMeters(true, false, false, false, true));
        assertEquals(false, PerformClipLauncherMode.shouldShowPerformMeters(true, false, false, false, false));
        assertEquals(false, PerformClipLauncherMode.shouldShowPerformMeters(true, true, false, false, true));
    }

    @Test
    void contextIdleUsesTrackLegendForRemoteEncoderPages() {
        assertEquals(true, PerformClipLauncherMode.shouldShowPerformTrackLegendIdle(EncoderMode.CHANNEL, false));
        assertEquals(true, PerformClipLauncherMode.shouldShowPerformTrackLegendIdle(EncoderMode.USER_1, false));
        assertEquals(true, PerformClipLauncherMode.shouldShowPerformTrackLegendIdle(EncoderMode.USER_2, false));
        assertEquals(false, PerformClipLauncherMode.shouldShowPerformTrackLegendIdle(EncoderMode.MIXER, false));
        assertEquals(false, PerformClipLauncherMode.shouldShowPerformTrackLegendIdle(EncoderMode.CHANNEL, true));
    }

    @Test
    void selectRowUsesTrackColorForAvailableTracks() {
        final RgbLightState trackColor = new RgbLightState(10, 90, 30, true);

        assertEquals(trackColor, PerformPadRenderer.mixSelect(trackColor, false, false));
        assertEquals(trackColor.getDimmed(), PerformPadRenderer.mixSelect(trackColor, false, true));
        assertEquals(trackColor.getBrightest(), PerformPadRenderer.mixSelect(trackColor, true, true));
    }

    @Test
    void mixStatusLedsFollowSoloAndMuteProjectState() {
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.mixStatusLightState(false, true, true, 1));
        assertEquals(BiColorLightState.AMBER_FULL,
                PerformClipLauncherMode.mixStatusLightState(true, true, false, 1));
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.mixStatusLightState(true, false, true, 1));
        assertEquals(BiColorLightState.AMBER_FULL,
                PerformClipLauncherMode.mixStatusLightState(true, false, true, 2));
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.mixStatusLightState(true, true, false, 2));
    }

    @Test
    void clipModifierStatusLedsReserveRedForDelete() {
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.clipModifierStatusLightState(false, false, false, false, 0));
        assertEquals(BiColorLightState.AMBER_FULL,
                PerformClipLauncherMode.clipModifierStatusLightState(true, false, false, false, 0));
        assertEquals(BiColorLightState.AMBER_FULL,
                PerformClipLauncherMode.clipModifierStatusLightState(false, true, false, false, 1));
        assertEquals(BiColorLightState.AMBER_FULL,
                PerformClipLauncherMode.clipModifierStatusLightState(false, false, true, false, 2));
        assertEquals(BiColorLightState.GREEN_HALF,
                PerformClipLauncherMode.clipModifierStatusLightState(false, false, false, false, 3));
        assertEquals(BiColorLightState.GREEN_FULL,
                PerformClipLauncherMode.clipModifierStatusLightState(false, false, false, true, 3));
    }

    @Test
    void patternSceneNavigationIsUnlitInMix() {
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.patternSceneNavigationLightState(false, false, 0, false, 0, 0, 1, true));
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.patternSceneNavigationLightState(false, false, 0, false, 0, 0, 1, false));
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, false, 0, false, 0, 0, -1, true));
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, false, 0, false, 0, 0, 1, true));
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 0, false, 0, 0, 1, true));
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 0, false, 0, 0, -1, true));
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 1, false, 0, 0, 1, true));
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 1, false, 0, 0, -1, true));
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 0, true, 0, 3, -1, true));
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 0, true, 0, 3, 1, true));
        assertEquals(BiColorLightState.AMBER_HALF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 0, true, 2, 3, -1, true));
        assertEquals(BiColorLightState.OFF,
                PerformClipLauncherMode.patternSceneNavigationLightState(true, true, 0, true, 2, 3, 1, true));
    }

    @Test
    void mixDevicePadsMapRowsToPagedTrackDevices() {
        assertEquals(0, PerformMixController.deviceIndexForPad(0, 0));
        assertEquals(0, PerformMixController.deviceIndexForPad(15, 0));
        assertEquals(1, PerformMixController.deviceIndexForPad(16, 0));
        assertEquals(2, PerformMixController.deviceIndexForPad(32, 0));
        assertEquals(3, PerformMixController.deviceIndexForPad(48, 0));
        assertEquals(4, PerformMixController.deviceIndexForPad(0, 1));
        assertEquals(5, PerformMixController.deviceIndexForPad(16, 1));
        assertEquals(6, PerformMixController.deviceIndexForPad(32, 1));
        assertEquals(7, PerformMixController.deviceIndexForPad(48, 1));
    }

    @Test
    void mixDeviceRowsMapToPagedTrackDevices() {
        assertEquals(0, PerformMixController.deviceIndexForRow(0, 0));
        assertEquals(3, PerformMixController.deviceIndexForRow(3, 0));
        assertEquals(4, PerformMixController.deviceIndexForRow(0, 1));
        assertEquals(7, PerformMixController.deviceIndexForRow(3, 1));
        assertEquals(-1, PerformMixController.deviceIndexForRow(4, 1));
    }

    @Test
    void mixDevicePadColorShowsEnabledState() {
        final RgbLightState trackColor = new RgbLightState(10, 90, 30, true);

        assertEquals(trackColor.getBrightest(), mixDeviceColor(trackColor, true, true));
        assertEquals(trackColor, mixDeviceColor(trackColor, true, false));
        assertEquals(trackColor.getDimmed(), mixDeviceColor(trackColor, false, false));
        assertEquals(trackColor.getSoftDimmed(), mixDeviceColor(trackColor, false, true));
    }

    @Test
    void mixDevicePadActionTitlesDistinguishSelectFromAltToggle() {
        assertEquals("Device Select", PerformClipLauncherMode.mixDeviceActionTitle(false, true));
        assertEquals("Device On", PerformClipLauncherMode.mixDeviceActionTitle(true, true));
        assertEquals("Device Off", PerformClipLauncherMode.mixDeviceActionTitle(true, false));
    }

    @Test
    void rowWideDeviceToggleTurnsOffWhenAnyVisibleDeviceIsEnabled() {
        assertEquals(false, PerformMixController.rowWideToggleTarget(true));
        assertEquals(true, PerformMixController.rowWideToggleTarget(false));
        assertEquals("Device Row Off", PerformClipLauncherMode.rowWideDeviceToggleTitle(false));
        assertEquals("Device Row On", PerformClipLauncherMode.rowWideDeviceToggleTitle(true));
    }

    @Test
    void mixDevicePadTogglesWindowWhenMainEncoderIsHeldWithoutAlt() {
        assertEquals(PerformMixController.DeviceAction.TOGGLE_WINDOW,
                PerformMixController.deviceAction(false, true));
        assertEquals(PerformMixController.DeviceAction.TOGGLE_ENABLED,
                PerformMixController.deviceAction(true, true));
        assertEquals(PerformMixController.DeviceAction.SELECT,
                PerformMixController.deviceAction(false, false));
    }

    @Test
    void birdsEyeVerticalPadsMapColumnsAndRowsToLauncherPages() {
        final PerformLayout layout = PerformLayout.vertical();

        assertEquals(0, PerformClipLauncherMode.birdsEyeTrackOffsetForPad(0, layout, 64));
        assertEquals(16, PerformClipLauncherMode.birdsEyeTrackOffsetForPad(1, layout, 64));
        assertEquals(48, PerformClipLauncherMode.birdsEyeTrackOffsetForPad(3, layout, 64));
        assertEquals(0, PerformClipLauncherMode.birdsEyeSceneOffsetForPad(0, layout, 20));
        assertEquals(4, PerformClipLauncherMode.birdsEyeSceneOffsetForPad(16, layout, 20));
    }

    @Test
    void birdsEyeHorizontalPadsMapRowsAndColumnsToLauncherPages() {
        final PerformLayout layout = PerformLayout.horizontal();

        assertEquals(0, PerformClipLauncherMode.birdsEyeTrackOffsetForPad(0, layout, 32));
        assertEquals(4, PerformClipLauncherMode.birdsEyeTrackOffsetForPad(16, layout, 32));
        assertEquals(0, PerformClipLauncherMode.birdsEyeSceneOffsetForPad(0, layout, 64));
        assertEquals(16, PerformClipLauncherMode.birdsEyeSceneOffsetForPad(1, layout, 64));
    }

    @Test
    void birdsEyeAvailabilityRequiresTrackAndSceneBlocks() {
        final PerformLayout layout = PerformLayout.vertical();

        assertEquals(true, PerformClipLauncherMode.birdsEyePadAvailable(0, layout, 16, 4));
        assertEquals(false, PerformClipLauncherMode.birdsEyePadAvailable(1, layout, 16, 4));
        assertEquals(false, PerformClipLauncherMode.birdsEyePadAvailable(16, layout, 16, 4));
        assertEquals(false, PerformClipLauncherMode.birdsEyePadAvailable(-1, layout, 16, 4));
    }

    @Test
    void birdsEyePadColorHighlightsCurrentBlock() {
        assertEquals(RgbLightState.OFF, PerformPadRenderer.birdsEye(false, false));
        assertEquals(new RgbLightState(0, 36, 84, true),
                PerformPadRenderer.birdsEye(true, false));
        assertEquals(new RgbLightState(0, 108, 127, true),
                PerformPadRenderer.birdsEye(true, true));
    }

    @Test
    void deviceLayerMixerPadColorsFollowLayerRows() {
        final RgbLightState layerColor = new RgbLightState(12, 80, 44, true);

        assertEquals(layerColor, deviceLayerColor(0, layerColor, false, false, true));
        assertEquals(new RgbLightState(96, 96, 0, true),
                deviceLayerColor(16, layerColor, true, false, true));
        assertEquals(RgbLightState.OFF,
                deviceLayerColor(16, layerColor, false, false, true));
        assertEquals(new RgbLightState(110, 48, 0, true),
                deviceLayerColor(32, layerColor, false, true, true));
        assertEquals(new RgbLightState(110, 0, 0, true),
                deviceLayerColor(48, layerColor, false, false, true));
        assertEquals(new RgbLightState(110, 0, 0, true).getSoftDimmed(),
                deviceLayerColor(48, layerColor, false, false, false));
        assertEquals(layerColor.getSoftDimmed(),
                deviceLayerColor(0, layerColor, false, false, false));
    }

    private static RgbLightState mixDeviceColor(final RgbLightState trackColor,
                                                final boolean enabled,
                                                final boolean selected) {
        return PerformPadRenderer.mixDevice(
                new PerformPadRenderer.MixDeviceSnapshot(true, trackColor, enabled, selected));
    }

    private static RgbLightState deviceLayerColor(final int padIndex,
                                                  final RgbLightState layerColor,
                                                  final boolean solo,
                                                  final boolean muted,
                                                  final boolean active) {
        return PerformPadRenderer.deviceLayer(new PerformPadRenderer.DeviceLayerSnapshot(
                PerformPadRenderer.TrackAction.fromPadIndex(padIndex), true, layerColor, solo, muted, active));
    }

}
