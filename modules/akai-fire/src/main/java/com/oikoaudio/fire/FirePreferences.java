package com.oikoaudio.fire;

import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.SettableRangedValue;

import java.util.function.LongSupplier;

/**
 * Owns the Bitwig preference handles used by the Akai Fire extension and exposes normalized, typed access.
 */
public final class FirePreferences {
    private final Listener listener;
    private final SettableEnumValue clipLaunchMode;
    private final SettableEnumValue launchQuantization;
    private final SettableEnumValue performLayout;
    private final SettableEnumValue defaultClipLength;
    private final SettableEnumValue launcherRecordLength;
    private final SettableEnumValue startupMode;
    private final SettableEnumValue mainEncoderStartup;
    private final SettableEnumValue euclidScope;
    private final SettableEnumValue defaultScale;
    private final SettableEnumValue defaultRootKey;
    private final SettableEnumValue defaultNoteInputOctave;
    private final SettableEnumValue defaultVelocitySensitivity;
    private final SettableEnumValue melodicSeedMode;
    private final SettableRangedValue melodicFixedSeed;
    private final SettableEnumValue drumPinMode;
    private final SettableRangedValue padBrightnessValue;
    private final SettableRangedValue padSaturationValue;
    private final SettableEnumValue screenMessageHold;
    private final SettableEnumValue idleOledMode;
    private final SettableEnumValue encoderLegendPosition;
    private final SettableEnumValue noteChordDisplay;
    private final SettableBooleanValue showDeactivatedTracks;
    private final SettableBooleanValue exclusiveTrackArmValue;
    private final SettableBooleanValue stepSequencerPadAudition;
    private final SettableBooleanValue screenNotifications;

    private double padBrightness;
    private double padSaturation;
    private boolean exclusiveTrackArm;

    public FirePreferences(final Preferences preferences, final Listener listener) {
        this.listener = listener;

        clipLaunchMode = interested(preferences.getEnumSetting(
                "Clip Launch Mode",
                FireControlPreferences.CATEGORY_CLIP_LAUNCH,
                FireControlPreferences.CLIP_LAUNCH_MODES,
                FireControlPreferences.CLIP_LAUNCH_MODE_SYNCED));
        launchQuantization = interested(preferences.getEnumSetting(
                "Clip Launch Quantization",
                FireControlPreferences.CATEGORY_CLIP_LAUNCH,
                FireControlPreferences.CLIP_LAUNCH_QUANTIZATIONS,
                FireControlPreferences.QUANTIZATION_1));
        performLayout = interested(preferences.getEnumSetting(
                "Perform Clip Launcher Layout",
                FireControlPreferences.CATEGORY_CLIP_LAUNCH,
                FireControlPreferences.PERFORM_LAYOUTS,
                FireControlPreferences.PERFORM_LAYOUT_VERTICAL));
        defaultClipLength = interested(preferences.getEnumSetting(
                "Default Clip Length",
                FireControlPreferences.CATEGORY_CLIP_LAUNCH,
                FireControlPreferences.DEFAULT_CLIP_LENGTHS,
                FireControlPreferences.CLIP_LENGTH_2_BARS));
        launcherRecordLength = interested(preferences.getEnumSetting(
                "Launcher Record Length",
                FireControlPreferences.CATEGORY_CLIP_LAUNCH,
                FireControlPreferences.LAUNCHER_RECORD_LENGTHS,
                FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_2_BARS));
        startupMode = interested(preferences.getEnumSetting(
                "Startup Mode",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.STARTUP_MODES,
                FireControlPreferences.STARTUP_MODE_NOTE));
        mainEncoderStartup = interested(preferences.getEnumSetting(
                "SELECT Encoder Startup",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.MAIN_ENCODER_STARTUP_STATES,
                FireControlPreferences.MAIN_ENCODER_STARTUP_FUNCTION_SET));
        euclidScope = interested(preferences.getEnumSetting(
                "Euclid Scope",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.EUCLID_SCOPES,
                FireControlPreferences.EUCLID_SCOPE_FULL_CLIP));
        defaultScale = interested(preferences.getEnumSetting(
                "Default Scale",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.DEFAULT_SCALES,
                FireControlPreferences.DEFAULT_SCALE_MAJOR));
        defaultRootKey = interested(preferences.getEnumSetting(
                "Default Root Key",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.DEFAULT_ROOT_KEYS,
                FireControlPreferences.DEFAULT_ROOT_KEY));
        defaultNoteInputOctave = interested(preferences.getEnumSetting(
                "Default Note Input Octave",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.DEFAULT_NOTE_INPUT_OCTAVES,
                FireControlPreferences.DEFAULT_NOTE_INPUT_OCTAVE));
        melodicSeedMode = interested(preferences.getEnumSetting(
                "Melodic Seed Mode",
                FireControlPreferences.CATEGORY_GENERATIVE_CONTROL,
                FireControlPreferences.MELODIC_SEED_MODES,
                FireControlPreferences.MELODIC_SEED_MODE_RANDOM));
        defaultVelocitySensitivity = interested(preferences.getEnumSetting(
                "Default Velocity Sensitivity",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.DEFAULT_VELOCITY_SENSITIVITIES,
                FireControlPreferences.DEFAULT_VELOCITY_SENSITIVITY));
        melodicFixedSeed = interested(preferences.getNumberSetting(
                "Melodic Fixed Seed",
                FireControlPreferences.CATEGORY_GENERATIVE_CONTROL,
                FireControlPreferences.MELODIC_FIXED_SEED_MIN,
                FireControlPreferences.MELODIC_FIXED_SEED_MAX,
                1,
                "",
                FireControlPreferences.MELODIC_FIXED_SEED_DEFAULT));
        drumPinMode = interested(preferences.getEnumSetting(
                "Drum Mode Pinning",
                FireControlPreferences.CATEGORY_PINNING,
                FireControlPreferences.DRUM_PIN_MODES,
                FireControlPreferences.DRUM_PIN_MODE_FIRST_DRUM_MACHINE));
        padBrightnessValue = interested(preferences.getNumberSetting(
                "Pad Brightness",
                FireControlPreferences.CATEGORY_HARDWARE,
                FireControlPreferences.PAD_BRIGHTNESS_MIN,
                FireControlPreferences.PAD_BRIGHTNESS_MAX,
                FireControlPreferences.PAD_BRIGHTNESS_STEP,
                "%",
                FireControlPreferences.PAD_BRIGHTNESS_DEFAULT));
        padSaturationValue = interested(preferences.getNumberSetting(
                "Pad Saturation",
                FireControlPreferences.CATEGORY_HARDWARE,
                FireControlPreferences.PAD_SATURATION_MIN,
                FireControlPreferences.PAD_SATURATION_MAX,
                FireControlPreferences.PAD_SATURATION_STEP,
                "%",
                FireControlPreferences.PAD_SATURATION_DEFAULT));
        screenMessageHold = interested(preferences.getEnumSetting(
                "Screen Message Hold",
                FireControlPreferences.CATEGORY_HARDWARE,
                FireControlPreferences.SCREEN_MESSAGE_HOLDS,
                FireControlPreferences.SCREEN_MESSAGE_HOLD_NORMAL));
        idleOledMode = interested(preferences.getEnumSetting(
                "Idle Perf & Drum OLED",
                FireControlPreferences.CATEGORY_HARDWARE,
                FireControlPreferences.IDLE_OLED_MODES,
                FireControlPreferences.IDLE_OLED_CONTEXT));
        encoderLegendPosition = interested(preferences.getEnumSetting(
                "Encoder Legend Position",
                FireControlPreferences.CATEGORY_HARDWARE,
                FireControlPreferences.ENCODER_LEGEND_POSITIONS,
                FireControlPreferences.ENCODER_LEGEND_POSITION_BOTTOM));
        noteChordDisplay = interested(preferences.getEnumSetting(
                "Note OLED Notes/Chords",
                FireControlPreferences.CATEGORY_HARDWARE,
                FireControlPreferences.NOTE_CHORD_DISPLAY_MODES,
                FireControlPreferences.NOTE_CHORD_DISPLAY_PADS));
        showDeactivatedTracks = interested(preferences.getBooleanSetting(
                "Show deactivated tracks",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.SHOW_DEACTIVATED_TRACKS_DEFAULT));
        exclusiveTrackArmValue = interested(preferences.getBooleanSetting(
                "Exclusive Track Arm",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                FireControlPreferences.EXCLUSIVE_TRACK_ARM_DEFAULT));
        stepSequencerPadAudition = interested(preferences.getBooleanSetting(
                "Step Seq Pad Audition",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                true));
        screenNotifications = interested(preferences.getBooleanSetting(
                "On-screen action notifications",
                FireControlPreferences.CATEGORY_FUNCTIONALITIES,
                true));

        padBrightness = FireControlPreferences.normalizePadBrightness(padBrightnessValue.getRaw());
        padSaturation = FireControlPreferences.normalizePadSaturation(padSaturationValue.getRaw());
        exclusiveTrackArm = exclusiveTrackArmValue.get();

        launchQuantization.addValueObserver(value ->
                listener.launchQuantizationChanged(FireControlPreferences.toLaunchQuantizationValue(value)));
        mainEncoderStartup.addValueObserver(value -> listener.mainEncoderStartupChanged(
                FireControlPreferences.normalizeMainEncoderStartupState(value)));
        drumPinMode.addValueObserver(value ->
                listener.drumPinModeChanged(FireControlPreferences.shouldAutoPinFirstDrumMachine(value)));
        padBrightnessValue.addRawValueObserver(value -> {
            padBrightness = FireControlPreferences.normalizePadBrightness(value);
            listener.padAppearanceChanged(padBrightness, padSaturation);
        });
        padSaturationValue.addRawValueObserver(value -> {
            padSaturation = FireControlPreferences.normalizePadSaturation(value);
            listener.padAppearanceChanged(padBrightness, padSaturation);
        });
        screenMessageHold.addValueObserver(value -> listener.screenMessageHoldChanged(
                FireControlPreferences.toScreenMessageHoldMillis(value)));
        encoderLegendPosition.addValueObserver(value -> listener.encoderLegendPositionChanged(
                FireControlPreferences.normalizeEncoderLegendPosition(value)));
        exclusiveTrackArmValue.addValueObserver(value -> exclusiveTrackArm = value);

        listener.launchQuantizationChanged(
                FireControlPreferences.toLaunchQuantizationValue(launchQuantization.get()));
        listener.mainEncoderStartupChanged(
                FireControlPreferences.normalizeMainEncoderStartupState(mainEncoderStartup.get()));
        listener.screenMessageHoldChanged(
                FireControlPreferences.toScreenMessageHoldMillis(screenMessageHold.get()));
        listener.encoderLegendPositionChanged(
                FireControlPreferences.normalizeEncoderLegendPosition(encoderLegendPosition.get()));
        listener.padAppearanceChanged(padBrightness, padSaturation);
    }

    public String clipLaunchMode() {
        return FireControlPreferences.CLIP_LAUNCH_MODE_FROM_START.equals(clipLaunchMode.get())
                ? FireControlPreferences.CLIP_LAUNCH_MODE_FROM_START
                : FireControlPreferences.CLIP_LAUNCH_MODE_SYNCED;
    }

    public String performLayout() {
        return FireControlPreferences.normalizePerformLayout(performLayout.get());
    }

    public int defaultClipLengthBeats() {
        return (int) Math.round(FireControlPreferences.toClipLengthBeats(defaultClipLength.get()));
    }

    public int launcherRecordLengthBeats() {
        return (int) Math.round(FireControlPreferences.toLauncherRecordLengthBeats(launcherRecordLength.get()));
    }

    public boolean roundLauncherRecordLength() {
        return FireControlPreferences.isRoundLauncherRecordLength(launcherRecordLength.get());
    }

    public boolean manualLauncherRecordLength() {
        return FireControlPreferences.isManualLauncherRecordLength(launcherRecordLength.get());
    }

    public String startupMode() {
        return FireControlPreferences.normalizeStartupMode(startupMode.get());
    }

    public boolean euclidFullClip() {
        return FireControlPreferences.EUCLID_SCOPE_FULL_CLIP.equals(
                FireControlPreferences.normalizeEuclidScope(euclidScope.get()));
    }

    public String defaultScale() {
        return FireControlPreferences.normalizeDefaultScale(defaultScale.get());
    }

    public int defaultRootKey() {
        return FireControlPreferences.toDefaultRootKey(defaultRootKey.get());
    }

    public int defaultNoteInputOctave() {
        return FireControlPreferences.toDefaultNoteInputOctave(defaultNoteInputOctave.get());
    }

    public int defaultVelocitySensitivity() {
        return FireControlPreferences.toDefaultVelocitySensitivity(defaultVelocitySensitivity.get());
    }

    public long initialMelodicSeed(final LongSupplier randomSeed) {
        final String mode = FireControlPreferences.normalizeMelodicSeedMode(melodicSeedMode.get());
        if (FireControlPreferences.MELODIC_SEED_MODE_FIXED.equals(mode)) {
            return clampSeed(Math.round(melodicFixedSeed.getRaw()));
        }
        return clampSeed(randomSeed.getAsLong());
    }

    public boolean autoPinFirstDrumMachine() {
        return FireControlPreferences.shouldAutoPinFirstDrumMachine(drumPinMode.get());
    }

    public double padBrightness() {
        return padBrightness;
    }

    public double padSaturation() {
        return padSaturation;
    }

    public boolean idleOledMeters() {
        return FireControlPreferences.IDLE_OLED_METERS.equals(idleOledMode.get());
    }

    public String noteChordDisplay() {
        return FireControlPreferences.normalizeNoteChordDisplay(noteChordDisplay.get());
    }

    public String defaultClipLength() {
        return FireControlPreferences.normalizeDefaultClipLength(defaultClipLength.get());
    }

    public String launcherRecordLength() {
        return FireControlPreferences.normalizeLauncherRecordLength(launcherRecordLength.get());
    }

    public boolean showDeactivatedTracks() {
        return showDeactivatedTracks.get();
    }

    public boolean exclusiveTrackArm() {
        return exclusiveTrackArm;
    }

    public boolean stepSequencerPadAudition() {
        return stepSequencerPadAudition.get();
    }

    public boolean screenNotifications() {
        return screenNotifications.get();
    }

    public void setDefaultClipLength(final String value) {
        defaultClipLength.set(FireControlPreferences.normalizeDefaultClipLength(value));
    }

    public void setLauncherRecordLength(final String value) {
        launcherRecordLength.set(FireControlPreferences.normalizeLauncherRecordLength(value));
    }

    public void setNoteChordDisplay(final String value) {
        noteChordDisplay.set(FireControlPreferences.normalizeNoteChordDisplay(value));
    }

    public void setPadBrightness(final double value) {
        padBrightnessValue.setRaw(FireControlPreferences.normalizePadBrightness(value));
    }

    public void setPadSaturation(final double value) {
        padSaturationValue.setRaw(FireControlPreferences.normalizePadSaturation(value));
    }

    public void setShowDeactivatedTracks(final boolean value) {
        showDeactivatedTracks.set(value);
    }

    private long clampSeed(final long seed) {
        return Math.max(FireControlPreferences.MELODIC_FIXED_SEED_MIN,
                Math.min(FireControlPreferences.MELODIC_FIXED_SEED_MAX, seed));
    }

    private static SettableEnumValue interested(final SettableEnumValue value) {
        value.markInterested();
        return value;
    }

    private static SettableRangedValue interested(final SettableRangedValue value) {
        value.markInterested();
        return value;
    }

    private static SettableBooleanValue interested(final SettableBooleanValue value) {
        value.markInterested();
        return value;
    }

    public interface Listener {
        default void launchQuantizationChanged(final String quantizationValue) {
        }

        default void mainEncoderStartupChanged(final String startupState) {
        }

        default void drumPinModeChanged(final boolean autoPin) {
        }

        default void padAppearanceChanged(final double brightness, final double saturation) {
        }

        default void screenMessageHoldChanged(final long holdMillis) {
        }

        default void encoderLegendPositionChanged(final String position) {
        }
    }
}
