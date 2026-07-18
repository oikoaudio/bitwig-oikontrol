package com.oikoaudio.fire.multiclip;

final class MulticlipXoxLayout {
    static final int SCENE_START = 0;
    static final int SCENE_COUNT = 16;
    static final int LANE_START = 16;
    static final int LANE_COUNT = 16;
    static final int PATTERN_START = 32;
    static final int PATTERN_COUNT = 32;

    private MulticlipXoxLayout() {}

    static boolean isScenePad(final int padIndex) {
        return padIndex >= SCENE_START && padIndex < SCENE_START + SCENE_COUNT;
    }

    static int sceneInPage(final int padIndex) {
        return padIndex - SCENE_START;
    }

    static boolean isLanePad(final int padIndex) {
        return padIndex >= LANE_START && padIndex < LANE_START + LANE_COUNT;
    }

    static int childPosition(final int padIndex) {
        return padIndex - LANE_START;
    }

    static boolean isPatternPad(final int padIndex) {
        return padIndex >= PATTERN_START && padIndex < PATTERN_START + PATTERN_COUNT;
    }

    static int visibleStep(final int padIndex) {
        return padIndex - PATTERN_START;
    }
}
