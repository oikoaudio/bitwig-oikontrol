package com.oikoaudio.fire;

public final class FireControlPreferences {
    public static final String CATEGORY_FUNCTIONALITIES = "Functionalities";
    public static final String CATEGORY_CLIP_LAUNCH = "Clip Launch";
    public static final String CATEGORY_PINNING = "Pinning";

    public static final String CLIP_LAUNCH_MODE_SYNCED = "Synced";
    public static final String CLIP_LAUNCH_MODE_FROM_START = "From Start";
    public static final String[] CLIP_LAUNCH_MODES = {
            CLIP_LAUNCH_MODE_SYNCED,
            CLIP_LAUNCH_MODE_FROM_START
    };

    public static final String QUANTIZATION_NONE = "None";
    public static final String QUANTIZATION_8 = "8";
    public static final String QUANTIZATION_4 = "4";
    public static final String QUANTIZATION_2 = "2";
    public static final String QUANTIZATION_1 = "1";
    public static final String QUANTIZATION_1_2 = "1/2";
    public static final String QUANTIZATION_1_4 = "1/4";
    public static final String QUANTIZATION_1_8 = "1/8";
    public static final String QUANTIZATION_1_16 = "1/16";
    public static final String[] CLIP_LAUNCH_QUANTIZATIONS = {
            QUANTIZATION_NONE,
            QUANTIZATION_8,
            QUANTIZATION_4,
            QUANTIZATION_2,
            QUANTIZATION_1,
            QUANTIZATION_1_2,
            QUANTIZATION_1_4,
            QUANTIZATION_1_8,
            QUANTIZATION_1_16
    };

    public static final String MAIN_ENCODER_LAST_TOUCHED = "Last Touched Parameter";
    public static final String MAIN_ENCODER_SHUFFLE = "Shuffle";
    public static final String MAIN_ENCODER_TEMPO = "Tempo";
    public static final String MAIN_ENCODER_NOTE_REPEAT = "Note Repeat";
    public static final String[] MAIN_ENCODER_ROLES = {
            MAIN_ENCODER_LAST_TOUCHED,
            MAIN_ENCODER_SHUFFLE,
            MAIN_ENCODER_TEMPO,
            MAIN_ENCODER_NOTE_REPEAT
    };

    public static final String EUCLID_SCOPE_VISIBLE_PAGE = "Visible Page";
    public static final String EUCLID_SCOPE_FULL_CLIP = "Full Clip";
    public static final String[] EUCLID_SCOPES = {
            EUCLID_SCOPE_VISIBLE_PAGE,
            EUCLID_SCOPE_FULL_CLIP
    };

    public static final String LIVE_PITCH_OFFSET_NEW_NOTES = "New Notes Only";
    public static final String LIVE_PITCH_OFFSET_RETUNE_HELD = "Retune Held Notes";
    public static final String[] LIVE_PITCH_OFFSET_BEHAVIORS = {
            LIVE_PITCH_OFFSET_NEW_NOTES,
            LIVE_PITCH_OFFSET_RETUNE_HELD
    };

    public static final String DRUM_PIN_MODE_FOLLOW_SELECTION = "Follow Selection";
    public static final String DRUM_PIN_MODE_FIRST_DRUM_MACHINE = "First Drum Machine";
    public static final String[] DRUM_PIN_MODES = {
            DRUM_PIN_MODE_FOLLOW_SELECTION,
            DRUM_PIN_MODE_FIRST_DRUM_MACHINE
    };

    private FireControlPreferences() {
    }

    public static String toClipLaunchModeValue(final String preferenceValue) {
        if (CLIP_LAUNCH_MODE_FROM_START.equals(preferenceValue)) {
            return "from_start";
        }
        return "synced";
    }

    public static String toLaunchQuantizationValue(final String preferenceValue) {
        if (QUANTIZATION_NONE.equals(preferenceValue)) {
            return "none";
        }
        for (final String value : CLIP_LAUNCH_QUANTIZATIONS) {
            if (value.equals(preferenceValue)) {
                return value;
            }
        }
        return QUANTIZATION_1_16;
    }

    public static String normalizeMainEncoderRole(final String preferenceValue) {
        for (final String value : MAIN_ENCODER_ROLES) {
            if (value.equals(preferenceValue)) {
                return value;
            }
        }
        return MAIN_ENCODER_LAST_TOUCHED;
    }

    public static String nextMainEncoderRole(final String currentRole) {
        final String normalizedRole = normalizeMainEncoderRole(currentRole);
        for (int index = 0; index < MAIN_ENCODER_ROLES.length; index++) {
            if (MAIN_ENCODER_ROLES[index].equals(normalizedRole)) {
                return MAIN_ENCODER_ROLES[(index + 1) % MAIN_ENCODER_ROLES.length];
            }
        }
        return MAIN_ENCODER_LAST_TOUCHED;
    }

    public static String normalizeEuclidScope(final String preferenceValue) {
        for (final String value : EUCLID_SCOPES) {
            if (value.equals(preferenceValue)) {
                return value;
            }
        }
        return EUCLID_SCOPE_FULL_CLIP;
    }

    public static String normalizeLivePitchOffsetBehavior(final String preferenceValue) {
        for (final String value : LIVE_PITCH_OFFSET_BEHAVIORS) {
            if (value.equals(preferenceValue)) {
                return value;
            }
        }
        return LIVE_PITCH_OFFSET_NEW_NOTES;
    }

    public static String normalizeDrumPinMode(final String preferenceValue) {
        for (final String value : DRUM_PIN_MODES) {
            if (value.equals(preferenceValue)) {
                return value;
            }
        }
        return DRUM_PIN_MODE_FOLLOW_SELECTION;
    }

    public static boolean shouldAutoPinFirstDrumMachine(final String preferenceValue) {
        return DRUM_PIN_MODE_FIRST_DRUM_MACHINE.equals(normalizeDrumPinMode(preferenceValue));
    }
}
