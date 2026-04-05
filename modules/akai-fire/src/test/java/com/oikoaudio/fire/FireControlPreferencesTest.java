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

    @Test
    void normalizesMainEncoderRolePreferenceValues() {
        assertEquals(FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED,
                FireControlPreferences.normalizeMainEncoderRole(FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED));
        assertEquals(FireControlPreferences.MAIN_ENCODER_SHUFFLE,
                FireControlPreferences.normalizeMainEncoderRole(FireControlPreferences.MAIN_ENCODER_SHUFFLE));
        assertEquals(FireControlPreferences.MAIN_ENCODER_TEMPO,
                FireControlPreferences.normalizeMainEncoderRole(FireControlPreferences.MAIN_ENCODER_TEMPO));
        assertEquals(FireControlPreferences.MAIN_ENCODER_TRACK_SELECT,
                FireControlPreferences.normalizeMainEncoderRole(FireControlPreferences.MAIN_ENCODER_TRACK_SELECT));
        assertEquals(FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED,
                FireControlPreferences.normalizeMainEncoderRole("unexpected"));
    }

    @Test
    void cyclesMainEncoderRolesInConfiguredOrder() {
        assertEquals(FireControlPreferences.MAIN_ENCODER_SHUFFLE,
                FireControlPreferences.nextMainEncoderRole(FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED));
        assertEquals(FireControlPreferences.MAIN_ENCODER_TEMPO,
                FireControlPreferences.nextMainEncoderRole(FireControlPreferences.MAIN_ENCODER_SHUFFLE));
        assertEquals(FireControlPreferences.MAIN_ENCODER_NOTE_REPEAT,
                FireControlPreferences.nextMainEncoderRole(FireControlPreferences.MAIN_ENCODER_TEMPO));
        assertEquals(FireControlPreferences.MAIN_ENCODER_TRACK_SELECT,
                FireControlPreferences.nextMainEncoderRole(FireControlPreferences.MAIN_ENCODER_NOTE_REPEAT));
        assertEquals(FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED,
                FireControlPreferences.nextMainEncoderRole(FireControlPreferences.MAIN_ENCODER_TRACK_SELECT));
        assertEquals(FireControlPreferences.MAIN_ENCODER_SHUFFLE,
                FireControlPreferences.nextMainEncoderRole("unexpected"));
    }

    @Test
    void normalizesDrumPinModes() {
        assertEquals(FireControlPreferences.DRUM_PIN_MODE_FOLLOW_SELECTION,
                FireControlPreferences.normalizeDrumPinMode(FireControlPreferences.DRUM_PIN_MODE_FOLLOW_SELECTION));
        assertEquals(FireControlPreferences.DRUM_PIN_MODE_FIRST_DRUM_MACHINE,
                FireControlPreferences.normalizeDrumPinMode(FireControlPreferences.DRUM_PIN_MODE_FIRST_DRUM_MACHINE));
        assertEquals(FireControlPreferences.DRUM_PIN_MODE_FOLLOW_SELECTION,
                FireControlPreferences.normalizeDrumPinMode("unexpected"));
    }

    @Test
    void detectsWhenDrumModeShouldAutoPinFirstDrumMachine() {
        assertEquals(true,
                FireControlPreferences.shouldAutoPinFirstDrumMachine(
                        FireControlPreferences.DRUM_PIN_MODE_FIRST_DRUM_MACHINE));
        assertEquals(false,
                FireControlPreferences.shouldAutoPinFirstDrumMachine(
                        FireControlPreferences.DRUM_PIN_MODE_FOLLOW_SELECTION));
        assertEquals(false,
                FireControlPreferences.shouldAutoPinFirstDrumMachine("unexpected"));
    }

    @Test
    void normalizesPadBrightnessToSupportedRange() {
        assertEquals(FireControlPreferences.PAD_BRIGHTNESS_MIN,
                FireControlPreferences.normalizePadBrightness(0));
        assertEquals(100.0, FireControlPreferences.normalizePadBrightness(100.0));
        assertEquals(FireControlPreferences.PAD_BRIGHTNESS_MAX,
                FireControlPreferences.normalizePadBrightness(500.0));
        assertEquals(FireControlPreferences.PAD_BRIGHTNESS_DEFAULT,
                FireControlPreferences.normalizePadBrightness(Double.NaN));
    }

    @Test
    void scalesPadColorComponentsByBrightnessPreference() {
        assertEquals(69, FireControlPreferences.scalePadColorComponent(50, 50.0));
        assertEquals(125, FireControlPreferences.scalePadColorComponent(50, 100.0));
        assertEquals(35, FireControlPreferences.scalePadColorComponent(50, 0.0));
        assertEquals(127, FireControlPreferences.scalePadColorComponent(100, 100.0));
        assertEquals(0, FireControlPreferences.scalePadColorComponent(0, 100.0));
    }

    @Test
    void normalizesPadSaturationToSupportedRange() {
        assertEquals(FireControlPreferences.PAD_SATURATION_MIN,
                FireControlPreferences.normalizePadSaturation(-10.0));
        assertEquals(100.0, FireControlPreferences.normalizePadSaturation(100.0));
        assertEquals(FireControlPreferences.PAD_SATURATION_MAX,
                FireControlPreferences.normalizePadSaturation(1000.0));
        assertEquals(FireControlPreferences.PAD_SATURATION_DEFAULT,
                FireControlPreferences.normalizePadSaturation(Double.NaN));
    }

    @Test
    void scalesPadColorComponentsByBrightnessAndSaturation() {
        assertEquals(69, FireControlPreferences.scalePadColorComponent(50, 50, 0, 0, 50.0, 100.0));
        assertEquals(92, FireControlPreferences.scalePadColorComponent(50, 50, 0, 0, 50.0, 150.0));
        assertEquals(46, FireControlPreferences.scalePadColorComponent(50, 50, 0, 0, 50.0, 50.0));
        assertEquals(46, FireControlPreferences.scalePadColorComponent(0, 100, 0, 0, 50.0, 0.0));
        assertEquals(0, FireControlPreferences.scalePadColorComponent(0, 0, 0, 0, 50.0, 150.0));
    }
}
