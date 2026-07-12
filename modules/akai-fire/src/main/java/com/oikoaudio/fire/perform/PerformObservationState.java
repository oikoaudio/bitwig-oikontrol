package com.oikoaudio.fire.perform;

import com.oikoaudio.fire.display.VuMeterFormatter;
import com.oikoaudio.fire.display.VuMeterPeakHold;
import com.oikoaudio.fire.lights.RgbLigthState;

/** Owns bounded Perform observation caches; track indices are source-bank indices unless named absolute. */
public final class PerformObservationState {
    private final RgbLigthState[] slotColors;
    private final RgbLigthState[] sceneColors;
    private final String[] sceneNames;
    private final RgbLigthState[] trackColors;
    private final String[] trackNames;
    private final boolean[] selectedVisibleTracks;
    private final String[][] deviceNames;
    private final String[] layerNames;
    private final RgbLigthState[] layerColors;
    private final int[] peakMeters;
    private final int[] rmsMeters;
    private final VuMeterPeakHold peakHold;
    private int selectedAbsoluteTrackIndex = -1;
    private int selectedAbsoluteSceneIndex = -1;

    public PerformObservationState(final int trackCount, final int sceneCount, final int deviceCount) {
        final int tracks = Math.max(0, trackCount);
        final int scenes = Math.max(0, sceneCount);
        slotColors = new RgbLigthState[tracks * scenes];
        sceneColors = new RgbLigthState[scenes];
        sceneNames = new String[scenes];
        trackColors = new RgbLigthState[tracks];
        trackNames = new String[tracks];
        selectedVisibleTracks = new boolean[tracks];
        deviceNames = new String[tracks][Math.max(0, deviceCount)];
        layerNames = new String[tracks];
        layerColors = new RgbLigthState[tracks];
        peakMeters = new int[tracks];
        rmsMeters = new int[tracks];
        peakHold = new VuMeterPeakHold(tracks);
    }

    public void setTrackName(final int sourceIndex, final String name) { if (track(sourceIndex)) trackNames[sourceIndex] = name; }
    public String trackName(final int sourceIndex) { return track(sourceIndex) ? text(trackNames[sourceIndex]) : ""; }
    public void setTrackColor(final int sourceIndex, final RgbLigthState color) { if (track(sourceIndex)) trackColors[sourceIndex] = color; }
    public RgbLigthState trackColor(final int sourceIndex) { return track(sourceIndex) ? trackColors[sourceIndex] : null; }
    public void setSelectedVisibleTrack(final int sourceIndex, final boolean selected) { if (track(sourceIndex)) selectedVisibleTracks[sourceIndex] = selected; }
    public boolean isSelectedVisibleTrack(final int sourceIndex) { return track(sourceIndex) && selectedVisibleTracks[sourceIndex]; }
    public void setSceneName(final int visibleIndex, final String name) { if (scene(visibleIndex)) sceneNames[visibleIndex] = name; }
    public String sceneName(final int visibleIndex) { return scene(visibleIndex) ? text(sceneNames[visibleIndex]) : ""; }
    public void setSceneColor(final int visibleIndex, final RgbLigthState color) { if (scene(visibleIndex)) sceneColors[visibleIndex] = color; }
    public RgbLigthState sceneColor(final int visibleIndex) { return scene(visibleIndex) ? sceneColors[visibleIndex] : null; }
    public void setSlotColor(final int sourceTrackIndex, final int visibleSceneIndex, final RgbLigthState color) {
        final int index = slotIndex(sourceTrackIndex, visibleSceneIndex); if (index >= 0) slotColors[index] = color;
    }
    public RgbLigthState slotColor(final int sourceTrackIndex, final int visibleSceneIndex) {
        final int index = slotIndex(sourceTrackIndex, visibleSceneIndex); return index >= 0 ? slotColors[index] : null;
    }
    public void setDeviceName(final int sourceTrackIndex, final int deviceIndex, final String name) {
        if (track(sourceTrackIndex) && deviceIndex >= 0 && deviceIndex < deviceNames[sourceTrackIndex].length) deviceNames[sourceTrackIndex][deviceIndex] = name;
    }
    public String deviceName(final int sourceTrackIndex, final int deviceIndex) {
        return track(sourceTrackIndex) && deviceIndex >= 0 && deviceIndex < deviceNames[sourceTrackIndex].length ? text(deviceNames[sourceTrackIndex][deviceIndex]) : "";
    }
    public void setLayerName(final int index, final String name) { if (track(index)) layerNames[index] = name; }
    public String layerName(final int index) { return track(index) ? text(layerNames[index]) : ""; }
    public void setLayerColor(final int index, final RgbLigthState color) { if (track(index)) layerColors[index] = color; }
    public RgbLigthState layerColor(final int index) { return track(index) ? layerColors[index] : null; }
    public void selectSlot(final int absoluteTrackIndex, final int absoluteSceneIndex) { selectedAbsoluteTrackIndex = absoluteTrackIndex; selectedAbsoluteSceneIndex = absoluteSceneIndex; }
    public void selectTrack(final int absoluteTrackIndex) { selectedAbsoluteTrackIndex = absoluteTrackIndex; }
    public int selectedAbsoluteTrackIndex() { return selectedAbsoluteTrackIndex; }
    public int selectedAbsoluteSceneIndex() { return selectedAbsoluteSceneIndex; }
    public void updatePeak(final int sourceIndex, final int value) { if (track(sourceIndex)) { peakMeters[sourceIndex] = clampMeter(value); peakHold.update(sourceIndex, value); } }
    public void updateRms(final int sourceIndex, final int value) { if (track(sourceIndex)) rmsMeters[sourceIndex] = clampMeter(value); }
    public int peak(final int sourceIndex) { return track(sourceIndex) ? peakMeters[sourceIndex] : 0; }
    public int rms(final int sourceIndex) { return track(sourceIndex) ? rmsMeters[sourceIndex] : 0; }
    public int peakHold(final int sourceIndex) { return peakHold.valueAt(sourceIndex); }
    public void decayPeakHold() { peakHold.decay(); }
    public void resetPeakHold(final int sourceIndex) { peakHold.reset(sourceIndex); }

    private boolean track(final int index) { return index >= 0 && index < trackNames.length; }
    private boolean scene(final int index) { return index >= 0 && index < sceneNames.length; }
    private int slotIndex(final int track, final int scene) { return track(track) && scene(scene) ? scene * trackNames.length + track : -1; }
    private static int clampMeter(final int value) { return VuMeterFormatter.clamp(value, 0, VuMeterFormatter.RANGE - 1); }
    private static String text(final String value) { return value == null ? "" : value; }
}
