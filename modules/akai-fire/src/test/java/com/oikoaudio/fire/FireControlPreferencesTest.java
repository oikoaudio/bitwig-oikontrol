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
        assertEquals(FireControlPreferences.MAIN_ENCODER_DRUM_GRID,
                FireControlPreferences.normalizeMainEncoderRole(FireControlPreferences.MAIN_ENCODER_DRUM_GRID));
        assertEquals(FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED,
                FireControlPreferences.normalizeMainEncoderRole("unexpected"));
        assertEquals(FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED,
                FireControlPreferences.normalizeMainEncoderRole(FireControlPreferences.MAIN_ENCODER_PLAYBACK_START));
    }

    @Test
    void normalizesStartupModePreferenceValues() {
        assertEquals(FireControlPreferences.STARTUP_MODE_NOTE,
                FireControlPreferences.normalizeStartupMode(FireControlPreferences.STARTUP_MODE_NOTE));
        assertEquals(FireControlPreferences.STARTUP_MODE_HARMONY,
                FireControlPreferences.normalizeStartupMode(FireControlPreferences.STARTUP_MODE_HARMONY));
        assertEquals(FireControlPreferences.STARTUP_MODE_DRUM_XOX,
                FireControlPreferences.normalizeStartupMode(FireControlPreferences.STARTUP_MODE_DRUM_XOX));
        assertEquals(FireControlPreferences.STARTUP_MODE_LAUNCHER,
                FireControlPreferences.normalizeStartupMode(FireControlPreferences.STARTUP_MODE_LAUNCHER));
        assertEquals(FireControlPreferences.STARTUP_MODE_MIX,
                FireControlPreferences.normalizeStartupMode(FireControlPreferences.STARTUP_MODE_MIX));
        assertEquals(FireControlPreferences.STARTUP_MODE_NOTE,
                FireControlPreferences.normalizeStartupMode("unexpected"));
    }

    @Test
    void cyclesAlternateMainEncoderRolesWithoutLastTouched() {
        assertEquals(FireControlPreferences.MAIN_ENCODER_TEMPO,
                FireControlPreferences.nextAlternateMainEncoderRole(FireControlPreferences.MAIN_ENCODER_SHUFFLE));
        assertEquals(FireControlPreferences.MAIN_ENCODER_NOTE_REPEAT,
                FireControlPreferences.nextAlternateMainEncoderRole(FireControlPreferences.MAIN_ENCODER_TEMPO));
        assertEquals(FireControlPreferences.MAIN_ENCODER_TRACK_SELECT,
                FireControlPreferences.nextAlternateMainEncoderRole(FireControlPreferences.MAIN_ENCODER_NOTE_REPEAT));
        assertEquals(FireControlPreferences.MAIN_ENCODER_DRUM_GRID,
                FireControlPreferences.nextAlternateMainEncoderRole(FireControlPreferences.MAIN_ENCODER_TRACK_SELECT));
        assertEquals(FireControlPreferences.MAIN_ENCODER_DRUM_GRID,
                FireControlPreferences.nextAlternateMainEncoderRole(FireControlPreferences.MAIN_ENCODER_PLAYBACK_START));
        assertEquals(FireControlPreferences.MAIN_ENCODER_SHUFFLE,
                FireControlPreferences.nextAlternateMainEncoderRole(FireControlPreferences.MAIN_ENCODER_DRUM_GRID));
        assertEquals(FireControlPreferences.MAIN_ENCODER_SHUFFLE,
                FireControlPreferences.nextAlternateMainEncoderRole(FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED));
        assertEquals(FireControlPreferences.MAIN_ENCODER_SHUFFLE,
                FireControlPreferences.nextAlternateMainEncoderRole("unexpected"));
    }

    @Test
    void skipsDrumGridWhenCyclingAlternateMainEncoderRolesOutsideDrumMode() {
        assertEquals(FireControlPreferences.MAIN_ENCODER_SHUFFLE,
                FireControlPreferences.nextAlternateMainEncoderRole(
                        FireControlPreferences.MAIN_ENCODER_TRACK_SELECT, false));
        assertEquals(FireControlPreferences.MAIN_ENCODER_SHUFFLE,
                FireControlPreferences.nextAlternateMainEncoderRole(
                        FireControlPreferences.MAIN_ENCODER_PLAYBACK_START, false));
        assertEquals(FireControlPreferences.MAIN_ENCODER_SHUFFLE,
                FireControlPreferences.nextAlternateMainEncoderRole(
                        FireControlPreferences.MAIN_ENCODER_DRUM_GRID, false));
    }

    @Test
    void normalizesMainEncoderStartupStatePreferenceValues() {
        assertEquals(FireControlPreferences.MAIN_ENCODER_STARTUP_LAST_TOUCHED,
                FireControlPreferences.normalizeMainEncoderStartupState(
                        FireControlPreferences.MAIN_ENCODER_STARTUP_LAST_TOUCHED));
        assertEquals(FireControlPreferences.MAIN_ENCODER_STARTUP_FUNCTION_SET,
                FireControlPreferences.normalizeMainEncoderStartupState(
                        FireControlPreferences.MAIN_ENCODER_STARTUP_FUNCTION_SET));
        assertEquals(FireControlPreferences.MAIN_ENCODER_STARTUP_FUNCTION_SET,
                FireControlPreferences.normalizeMainEncoderStartupState("unexpected"));
    }

    @Test
    void normalizesDrumPinModes() {
        assertEquals(FireControlPreferences.DRUM_PIN_MODE_FOLLOW_SELECTION,
                FireControlPreferences.normalizeDrumPinMode(FireControlPreferences.DRUM_PIN_MODE_FOLLOW_SELECTION));
        assertEquals(FireControlPreferences.DRUM_PIN_MODE_FIRST_DRUM_MACHINE,
                FireControlPreferences.normalizeDrumPinMode(FireControlPreferences.DRUM_PIN_MODE_FIRST_DRUM_MACHINE));
        assertEquals(FireControlPreferences.DRUM_PIN_MODE_FIRST_DRUM_MACHINE,
                FireControlPreferences.normalizeDrumPinMode("unexpected"));
    }

    @Test
    void normalizesScreenMessageHoldPreferenceValues() {
        assertEquals(FireControlPreferences.SCREEN_MESSAGE_HOLD_SHORT,
                FireControlPreferences.normalizeScreenMessageHold(FireControlPreferences.SCREEN_MESSAGE_HOLD_SHORT));
        assertEquals(FireControlPreferences.SCREEN_MESSAGE_HOLD_NORMAL,
                FireControlPreferences.normalizeScreenMessageHold(FireControlPreferences.SCREEN_MESSAGE_HOLD_NORMAL));
        assertEquals(FireControlPreferences.SCREEN_MESSAGE_HOLD_LONG,
                FireControlPreferences.normalizeScreenMessageHold(FireControlPreferences.SCREEN_MESSAGE_HOLD_LONG));
        assertEquals(FireControlPreferences.SCREEN_MESSAGE_HOLD_NORMAL,
                FireControlPreferences.normalizeScreenMessageHold("unexpected"));
    }

    @Test
    void normalizesEncoderLegendPositionPreferenceValues() {
        assertEquals(FireControlPreferences.ENCODER_LEGEND_POSITION_BOTTOM,
                FireControlPreferences.normalizeEncoderLegendPosition(
                        FireControlPreferences.ENCODER_LEGEND_POSITION_BOTTOM));
        assertEquals(FireControlPreferences.ENCODER_LEGEND_POSITION_TOP,
                FireControlPreferences.normalizeEncoderLegendPosition(
                        FireControlPreferences.ENCODER_LEGEND_POSITION_TOP));
        assertEquals(FireControlPreferences.ENCODER_LEGEND_POSITION_BOTTOM,
                FireControlPreferences.normalizeEncoderLegendPosition("unexpected"));
    }

    @Test
    void normalizesNoteChordDisplayPreferenceValues() {
        assertEquals(FireControlPreferences.NOTE_CHORD_DISPLAY_PADS,
                FireControlPreferences.normalizeNoteChordDisplay(
                        FireControlPreferences.NOTE_CHORD_DISPLAY_PADS));
        assertEquals(FireControlPreferences.NOTE_CHORD_DISPLAY_PADS_AND_DAW,
                FireControlPreferences.normalizeNoteChordDisplay(
                        FireControlPreferences.NOTE_CHORD_DISPLAY_PADS_AND_DAW));
        assertEquals(FireControlPreferences.NOTE_CHORD_DISPLAY_PADS,
                FireControlPreferences.normalizeNoteChordDisplay("Live"));
        assertEquals(FireControlPreferences.NOTE_CHORD_DISPLAY_PADS_AND_DAW,
                FireControlPreferences.normalizeNoteChordDisplay("Live + Playback"));
        assertEquals(FireControlPreferences.NOTE_CHORD_DISPLAY_PADS,
                FireControlPreferences.normalizeNoteChordDisplay("unexpected"));
    }

    @Test
    void detectsWhenPlaybackNotesShouldUseNoteChordDisplay() {
        assertEquals(false,
                FireControlPreferences.shouldShowPlaybackNoteChordDisplay(
                        FireControlPreferences.NOTE_CHORD_DISPLAY_PADS));
        assertEquals(true,
                FireControlPreferences.shouldShowPlaybackNoteChordDisplay(
                        FireControlPreferences.NOTE_CHORD_DISPLAY_PADS_AND_DAW));
        assertEquals(false, FireControlPreferences.shouldShowPlaybackNoteChordDisplay("unexpected"));
    }

    @Test
    void mapsScreenMessageHoldPreferenceValuesToMilliseconds() {
        assertEquals(750,
                FireControlPreferences.toScreenMessageHoldMillis(FireControlPreferences.SCREEN_MESSAGE_HOLD_SHORT));
        assertEquals(1500,
                FireControlPreferences.toScreenMessageHoldMillis(FireControlPreferences.SCREEN_MESSAGE_HOLD_NORMAL));
        assertEquals(3000,
                FireControlPreferences.toScreenMessageHoldMillis(FireControlPreferences.SCREEN_MESSAGE_HOLD_LONG));
        assertEquals(1500, FireControlPreferences.toScreenMessageHoldMillis("unexpected"));
    }

    @Test
    void detectsWhenDrumModeShouldAutoPinFirstDrumMachine() {
        assertEquals(true,
                FireControlPreferences.shouldAutoPinFirstDrumMachine(
                        FireControlPreferences.DRUM_PIN_MODE_FIRST_DRUM_MACHINE));
        assertEquals(false,
                FireControlPreferences.shouldAutoPinFirstDrumMachine(
                        FireControlPreferences.DRUM_PIN_MODE_FOLLOW_SELECTION));
        assertEquals(true,
                FireControlPreferences.shouldAutoPinFirstDrumMachine("unexpected"));
    }

    @Test
    void normalizesPerformClipLauncherLayoutPreferenceValues() {
        assertEquals(FireControlPreferences.PERFORM_LAYOUT_VERTICAL,
                FireControlPreferences.normalizePerformLayout(FireControlPreferences.PERFORM_LAYOUT_VERTICAL));
        assertEquals(FireControlPreferences.PERFORM_LAYOUT_HORIZONTAL,
                FireControlPreferences.normalizePerformLayout(FireControlPreferences.PERFORM_LAYOUT_HORIZONTAL));
        assertEquals(FireControlPreferences.PERFORM_LAYOUT_VERTICAL,
                FireControlPreferences.normalizePerformLayout("unexpected"));
    }

    @Test
    void normalizesDefaultClipLengthPreferenceValues() {
        assertEquals(FireControlPreferences.CLIP_LENGTH_1_BAR,
                FireControlPreferences.normalizeDefaultClipLength(FireControlPreferences.CLIP_LENGTH_1_BAR));
        assertEquals(FireControlPreferences.CLIP_LENGTH_2_BARS,
                FireControlPreferences.normalizeDefaultClipLength(FireControlPreferences.CLIP_LENGTH_2_BARS));
        assertEquals(FireControlPreferences.CLIP_LENGTH_4_BARS,
                FireControlPreferences.normalizeDefaultClipLength(FireControlPreferences.CLIP_LENGTH_4_BARS));
        assertEquals(FireControlPreferences.CLIP_LENGTH_8_BARS,
                FireControlPreferences.normalizeDefaultClipLength(FireControlPreferences.CLIP_LENGTH_8_BARS));
        assertEquals(FireControlPreferences.CLIP_LENGTH_2_BARS,
                FireControlPreferences.normalizeDefaultClipLength(FireControlPreferences.CLIP_LENGTH_OFF));
        assertEquals(FireControlPreferences.CLIP_LENGTH_2_BARS,
                FireControlPreferences.normalizeDefaultClipLength(FireControlPreferences.CLIP_LENGTH_ROUND_NEAREST_BAR));
        assertEquals(FireControlPreferences.CLIP_LENGTH_2_BARS,
                FireControlPreferences.normalizeDefaultClipLength("unexpected"));
    }

    @Test
    void normalizesLauncherRecordLengthPreferenceValues() {
        assertEquals(FireControlPreferences.LAUNCHER_RECORD_LENGTH_MANUAL,
                FireControlPreferences.normalizeLauncherRecordLength(FireControlPreferences.CLIP_LENGTH_OFF));
        assertEquals(FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_1_BAR,
                FireControlPreferences.normalizeLauncherRecordLength(FireControlPreferences.CLIP_LENGTH_1_BAR));
        assertEquals(FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_2_BARS,
                FireControlPreferences.normalizeLauncherRecordLength(FireControlPreferences.CLIP_LENGTH_2_BARS));
        assertEquals(FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_4_BARS,
                FireControlPreferences.normalizeLauncherRecordLength(FireControlPreferences.CLIP_LENGTH_4_BARS));
        assertEquals(FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_8_BARS,
                FireControlPreferences.normalizeLauncherRecordLength(FireControlPreferences.CLIP_LENGTH_8_BARS));
        assertEquals(FireControlPreferences.CLIP_LENGTH_ROUND_NEAREST_BAR,
                FireControlPreferences.normalizeLauncherRecordLength("Round to nearest bar"));
        assertEquals(FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_2_BARS,
                FireControlPreferences.normalizeLauncherRecordLength("unexpected"));
    }

    @Test
    void mapsDefaultClipLengthPreferenceToBeats() {
        assertEquals(4.0,
                FireControlPreferences.toClipLengthBeats(FireControlPreferences.CLIP_LENGTH_1_BAR));
        assertEquals(8.0,
                FireControlPreferences.toClipLengthBeats(FireControlPreferences.CLIP_LENGTH_2_BARS));
        assertEquals(16.0,
                FireControlPreferences.toClipLengthBeats(FireControlPreferences.CLIP_LENGTH_4_BARS));
        assertEquals(32.0,
                FireControlPreferences.toClipLengthBeats(FireControlPreferences.CLIP_LENGTH_8_BARS));
        assertEquals(8.0,
                FireControlPreferences.toClipLengthBeats(FireControlPreferences.CLIP_LENGTH_ROUND_NEAREST_BAR));
        assertEquals(8.0, FireControlPreferences.toClipLengthBeats("unexpected"));
    }

    @Test
    void mapsLauncherRecordLengthPreferenceToBeats() {
        assertEquals(8.0,
                FireControlPreferences.toLauncherRecordLengthBeats(FireControlPreferences.LAUNCHER_RECORD_LENGTH_MANUAL));
        assertEquals(4.0,
                FireControlPreferences.toLauncherRecordLengthBeats(
                        FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_1_BAR));
        assertEquals(8.0,
                FireControlPreferences.toLauncherRecordLengthBeats(
                        FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_2_BARS));
        assertEquals(16.0,
                FireControlPreferences.toLauncherRecordLengthBeats(
                        FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_4_BARS));
        assertEquals(32.0,
                FireControlPreferences.toLauncherRecordLengthBeats(
                        FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_8_BARS));
    }

    @Test
    void detectsRoundLauncherRecordLengthPreference() {
        assertEquals(true,
                FireControlPreferences.isRoundLauncherRecordLength(
                        FireControlPreferences.CLIP_LENGTH_ROUND_NEAREST_BAR));
        assertEquals(true,
                FireControlPreferences.isRoundLauncherRecordLength("Round to nearest bar"));
        assertEquals(false,
                FireControlPreferences.isRoundLauncherRecordLength(FireControlPreferences.CLIP_LENGTH_2_BARS));
        assertEquals(false, FireControlPreferences.isRoundLauncherRecordLength("unexpected"));
    }

    @Test
    void detectsManualLauncherRecordLengthPreference() {
        assertEquals(true,
                FireControlPreferences.isManualLauncherRecordLength(FireControlPreferences.CLIP_LENGTH_OFF));
        assertEquals(false,
                FireControlPreferences.isManualLauncherRecordLength(FireControlPreferences.CLIP_LENGTH_ROUND_NEAREST_BAR));
        assertEquals(false, FireControlPreferences.isManualLauncherRecordLength("unexpected"));
    }

    @Test
    void normalizesDefaultRootKeyPreferenceValues() {
        assertEquals(FireControlPreferences.DEFAULT_ROOT_KEY,
                FireControlPreferences.normalizeDefaultRootKey(FireControlPreferences.DEFAULT_ROOT_KEY));
        assertEquals("F#",
                FireControlPreferences.normalizeDefaultRootKey("F#"));
        assertEquals(FireControlPreferences.DEFAULT_ROOT_KEY,
                FireControlPreferences.normalizeDefaultRootKey("unexpected"));
    }

    @Test
    void mapsDefaultRootKeyPreferenceToPitchClass() {
        assertEquals(0, FireControlPreferences.toDefaultRootKey("C"));
        assertEquals(6, FireControlPreferences.toDefaultRootKey("F#"));
        assertEquals(11, FireControlPreferences.toDefaultRootKey("B"));
        assertEquals(0, FireControlPreferences.toDefaultRootKey("unexpected"));
    }

    @Test
    void normalizesDefaultNoteInputOctavePreferenceToSupportedRange() {
        assertEquals("2", FireControlPreferences.normalizeDefaultNoteInputOctave("2"));
        assertEquals("3", FireControlPreferences.normalizeDefaultNoteInputOctave("3"));
        assertEquals("4", FireControlPreferences.normalizeDefaultNoteInputOctave("4"));
        assertEquals(FireControlPreferences.DEFAULT_NOTE_INPUT_OCTAVE,
                FireControlPreferences.normalizeDefaultNoteInputOctave("7"));
    }

    @Test
    void normalizesDefaultVelocitySensitivityPreferenceValues() {
        assertEquals(FireControlPreferences.DEFAULT_VELOCITY_SENSITIVITY,
                FireControlPreferences.normalizeDefaultVelocitySensitivity(
                        FireControlPreferences.DEFAULT_VELOCITY_SENSITIVITY));
        assertEquals("50", FireControlPreferences.normalizeDefaultVelocitySensitivity("50"));
        assertEquals(FireControlPreferences.DEFAULT_VELOCITY_SENSITIVITY,
                FireControlPreferences.normalizeDefaultVelocitySensitivity("85"));
    }

    @Test
    void mapsDefaultVelocitySensitivityPreferenceToPercent() {
        assertEquals(0, FireControlPreferences.toDefaultVelocitySensitivity("0"));
        assertEquals(80, FireControlPreferences.toDefaultVelocitySensitivity("80"));
        assertEquals(100, FireControlPreferences.toDefaultVelocitySensitivity("100"));
        assertEquals(80, FireControlPreferences.toDefaultVelocitySensitivity("unexpected"));
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
