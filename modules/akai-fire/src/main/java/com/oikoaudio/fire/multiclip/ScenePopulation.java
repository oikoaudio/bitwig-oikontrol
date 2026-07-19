package com.oikoaudio.fire.multiclip;

public enum ScenePopulation {
    NEW,
    PARTIAL,
    POPULATED;

    public static ScenePopulation ofChildClips(
            final int mappedClipCount, final int mappedLaneCount) {
        if (mappedClipCount <= 0) {
            return NEW;
        }
        return mappedClipCount >= mappedLaneCount ? POPULATED : PARTIAL;
    }
}
