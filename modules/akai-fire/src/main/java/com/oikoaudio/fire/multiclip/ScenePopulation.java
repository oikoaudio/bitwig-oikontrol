package com.oikoaudio.fire.multiclip;

public enum ScenePopulation {
    NEW,
    EMPTY,
    PARTIAL,
    POPULATED;

    public static ScenePopulation of(
            final boolean sceneExists, final int mappedClipCount, final int mappedLaneCount) {
        if (!sceneExists) {
            return NEW;
        }
        if (mappedClipCount <= 0) {
            return EMPTY;
        }
        return mappedClipCount >= mappedLaneCount ? POPULATED : PARTIAL;
    }
}
