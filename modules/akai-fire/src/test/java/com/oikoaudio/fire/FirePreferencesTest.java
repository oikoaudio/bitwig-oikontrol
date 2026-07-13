package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.callback.DoubleValueChangedCallback;
import com.bitwig.extension.callback.EnumValueChangedCallback;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FirePreferencesTest {
    @Test
    void registersSettingsAndExposesTypedDefaults() {
        final PreferenceSource source = new PreferenceSource();
        final FirePreferences.Listener listener = mock(FirePreferences.Listener.class);

        final FirePreferences preferences = new FirePreferences(source.preferences, listener);

        assertEquals(FireControlPreferences.CLIP_LAUNCH_MODE_SYNCED, preferences.clipLaunchMode());
        assertEquals(FireControlPreferences.PERFORM_LAYOUT_VERTICAL, preferences.performLayout());
        assertEquals(8, preferences.defaultClipLengthBeats());
        assertEquals(8, preferences.launcherRecordLengthBeats());
        assertFalse(preferences.manualLauncherRecordLength());
        assertFalse(preferences.roundLauncherRecordLength());
        assertEquals(FireControlPreferences.STARTUP_MODE_NOTE, preferences.startupMode());
        assertEquals(FireControlPreferences.DEFAULT_SCALE_MAJOR, preferences.defaultScale());
        assertEquals(0, preferences.defaultRootKey());
        assertEquals(3, preferences.defaultNoteInputOctave());
        assertEquals(80, preferences.defaultVelocitySensitivity());
        assertTrue(preferences.euclidFullClip());
        assertTrue(preferences.autoPinFirstDrumMachine());
        assertFalse(preferences.showDeactivatedTracks());
        assertFalse(preferences.exclusiveTrackArm());
        assertTrue(preferences.stepSequencerPadAudition());
        assertTrue(preferences.screenNotifications());
        assertEquals(FireControlPreferences.PAD_BRIGHTNESS_DEFAULT, preferences.padBrightness());
        assertEquals(FireControlPreferences.PAD_SATURATION_DEFAULT, preferences.padSaturation());

        verify(listener).launchQuantizationChanged("1");
        verify(listener)
                .mainEncoderStartupChanged(
                        FireControlPreferences.MAIN_ENCODER_STARTUP_FUNCTION_SET);
        verify(listener)
                .screenMessageHoldChanged(FireControlPreferences.SCREEN_MESSAGE_HOLD_NORMAL_MS);
        verify(listener)
                .encoderLegendPositionChanged(
                        FireControlPreferences.ENCODER_LEGEND_POSITION_BOTTOM);
        verify(listener)
                .padAppearanceChanged(
                        FireControlPreferences.PAD_BRIGHTNESS_DEFAULT,
                        FireControlPreferences.PAD_SATURATION_DEFAULT);
    }

    @Test
    void normalizesLiveValuesBeforeNotifyingListener() {
        final PreferenceSource source = new PreferenceSource();
        final FirePreferences.Listener listener = mock(FirePreferences.Listener.class);
        final FirePreferences preferences = new FirePreferences(source.preferences, listener);
        clearInvocations(listener);

        source.changeRaw("Pad Brightness", 500.0);
        source.changeRaw("Pad Saturation", -20.0);
        source.changeEnum("Screen Message Hold", "unexpected");
        source.changeEnum(
                "Drum Mode Pinning", FireControlPreferences.DRUM_PIN_MODE_FOLLOW_SELECTION);

        assertEquals(FireControlPreferences.PAD_BRIGHTNESS_MAX, preferences.padBrightness());
        assertEquals(FireControlPreferences.PAD_SATURATION_MIN, preferences.padSaturation());
        verify(listener)
                .padAppearanceChanged(
                        FireControlPreferences.PAD_BRIGHTNESS_MAX,
                        FireControlPreferences.PAD_SATURATION_DEFAULT);
        verify(listener)
                .padAppearanceChanged(
                        FireControlPreferences.PAD_BRIGHTNESS_MAX,
                        FireControlPreferences.PAD_SATURATION_MIN);
        verify(listener)
                .screenMessageHoldChanged(FireControlPreferences.SCREEN_MESSAGE_HOLD_NORMAL_MS);
        verify(listener).drumPinModeChanged(false);
    }

    @Test
    void writesThroughNarrowTypedMethods() {
        final PreferenceSource source = new PreferenceSource();
        final FirePreferences preferences =
                new FirePreferences(source.preferences, mock(FirePreferences.Listener.class));

        preferences.setPadBrightness(75.0);
        preferences.setPadSaturation(125.0);
        preferences.setDefaultClipLength(FireControlPreferences.CLIP_LENGTH_4_BARS);
        preferences.setLauncherRecordLength(FireControlPreferences.LAUNCHER_RECORD_LENGTH_MANUAL);
        preferences.setNoteChordDisplay(FireControlPreferences.NOTE_CHORD_DISPLAY_PADS_AND_DAW);
        preferences.setShowDeactivatedTracks(true);

        assertEquals(75.0, source.rawValue("Pad Brightness"));
        assertEquals(125.0, source.rawValue("Pad Saturation"));
        assertEquals(
                FireControlPreferences.CLIP_LENGTH_4_BARS, source.enumValue("Default Clip Length"));
        assertEquals(
                FireControlPreferences.LAUNCHER_RECORD_LENGTH_MANUAL,
                source.enumValue("Launcher Record Length"));
        assertEquals(
                FireControlPreferences.NOTE_CHORD_DISPLAY_PADS_AND_DAW,
                source.enumValue("Note OLED Notes/Chords"));
        assertTrue(source.booleanValue("Show deactivated tracks"));
    }

    private static final class PreferenceSource {
        private final Preferences preferences = mock(Preferences.class);
        private final Map<String, AtomicReference<String>> enumValues = new HashMap<>();
        private final Map<String, EnumValueChangedCallback> enumObservers = new HashMap<>();
        private final Map<String, AtomicReference<Double>> rawValues = new HashMap<>();
        private final Map<String, DoubleValueChangedCallback> rawObservers = new HashMap<>();
        private final Map<String, AtomicBoolean> booleanValues = new HashMap<>();
        private final Map<String, BooleanValueChangedCallback> booleanObservers = new HashMap<>();

        private PreferenceSource() {
            when(preferences.getEnumSetting(
                            anyString(), anyString(), any(String[].class), anyString()))
                    .thenAnswer(
                            invocation ->
                                    enumSetting(
                                            invocation.getArgument(0), invocation.getArgument(3)));
            when(preferences.getNumberSetting(
                            anyString(),
                            anyString(),
                            anyDouble(),
                            anyDouble(),
                            anyDouble(),
                            anyString(),
                            anyDouble()))
                    .thenAnswer(
                            invocation ->
                                    rangedSetting(
                                            invocation.getArgument(0), invocation.getArgument(6)));
            when(preferences.getBooleanSetting(anyString(), anyString(), anyBoolean()))
                    .thenAnswer(
                            invocation ->
                                    booleanSetting(
                                            invocation.getArgument(0), invocation.getArgument(2)));
        }

        private SettableEnumValue enumSetting(final String name, final String defaultValue) {
            final AtomicReference<String> state = new AtomicReference<>(defaultValue);
            enumValues.put(name, state);
            final SettableEnumValue value = mock(SettableEnumValue.class);
            when(value.get()).thenAnswer(ignored -> state.get());
            org.mockito.Mockito.doAnswer(
                            invocation -> {
                                state.set(invocation.getArgument(0));
                                return null;
                            })
                    .when(value)
                    .set(anyString());
            org.mockito.Mockito.doAnswer(
                            invocation -> {
                                enumObservers.put(name, invocation.getArgument(0));
                                return null;
                            })
                    .when(value)
                    .addValueObserver(any(EnumValueChangedCallback.class));
            return value;
        }

        private SettableRangedValue rangedSetting(final String name, final double defaultValue) {
            final AtomicReference<Double> state = new AtomicReference<>(defaultValue);
            rawValues.put(name, state);
            final SettableRangedValue value = mock(SettableRangedValue.class);
            when(value.getRaw()).thenAnswer(ignored -> state.get());
            org.mockito.Mockito.doAnswer(
                            invocation -> {
                                state.set(invocation.getArgument(0));
                                return null;
                            })
                    .when(value)
                    .setRaw(anyDouble());
            org.mockito.Mockito.doAnswer(
                            invocation -> {
                                rawObservers.put(name, invocation.getArgument(0));
                                return null;
                            })
                    .when(value)
                    .addRawValueObserver(any(DoubleValueChangedCallback.class));
            return value;
        }

        private SettableBooleanValue booleanSetting(final String name, final boolean defaultValue) {
            final AtomicBoolean state = new AtomicBoolean(defaultValue);
            booleanValues.put(name, state);
            final SettableBooleanValue value = mock(SettableBooleanValue.class);
            when(value.get()).thenAnswer(ignored -> state.get());
            org.mockito.Mockito.doAnswer(
                            invocation -> {
                                state.set(invocation.getArgument(0));
                                return null;
                            })
                    .when(value)
                    .set(anyBoolean());
            org.mockito.Mockito.doAnswer(
                            invocation -> {
                                booleanObservers.put(name, invocation.getArgument(0));
                                return null;
                            })
                    .when(value)
                    .addValueObserver(any(BooleanValueChangedCallback.class));
            return value;
        }

        private void changeEnum(final String name, final String value) {
            enumValues.get(name).set(value);
            enumObservers.get(name).valueChanged(value);
        }

        private void changeRaw(final String name, final double value) {
            rawValues.get(name).set(value);
            rawObservers.get(name).valueChanged(value);
        }

        private String enumValue(final String name) {
            return enumValues.get(name).get();
        }

        private double rawValue(final String name) {
            return rawValues.get(name).get();
        }

        private boolean booleanValue(final String name) {
            return booleanValues.get(name).get();
        }
    }
}
