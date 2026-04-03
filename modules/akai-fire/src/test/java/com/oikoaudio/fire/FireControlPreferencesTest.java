package com.oikoaudio.fire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FireControlPreferencesTest {

    @Test
    void mapsClipLaunchModePreferenceValues() {
        assertEquals("synced",
                FireControlPreferences.toClipLaunchModeValue(FireControlPreferences.CLIP_LAUNCH_MODE_SYNCED));
        assertEquals("from_start",
                FireControlPreferences.toClipLaunchModeValue(FireControlPreferences.CLIP_LAUNCH_MODE_FROM_START));
        assertEquals("synced", FireControlPreferences.toClipLaunchModeValue("unexpected"));
    }

    @Test
    void mapsLaunchQuantizationPreferenceValues() {
        assertEquals("none",
                FireControlPreferences.toLaunchQuantizationValue(FireControlPreferences.QUANTIZATION_NONE));
        assertEquals("1/4",
                FireControlPreferences.toLaunchQuantizationValue(FireControlPreferences.QUANTIZATION_1_4));
        assertEquals("1/16", FireControlPreferences.toLaunchQuantizationValue("unexpected"));
    }
}
