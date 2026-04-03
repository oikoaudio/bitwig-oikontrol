package com.oikoaudio.fire;

public final class FireControlPreferences {
    public static final String CATEGORY_FUNCTIONALITIES = "Functionalities";
    public static final String CATEGORY_CLIP_LAUNCH = "Clip Launch";

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

    public static final String PATTERN_ACTION_AUTOMATION_WRITE = "Clip Launcher Automation Write";
    public static final String PATTERN_ACTION_DISABLED = "Disabled";
    public static final String[] PATTERN_ACTIONS = {
            PATTERN_ACTION_AUTOMATION_WRITE,
            PATTERN_ACTION_DISABLED
    };

    public static final String MAIN_ENCODER_LAST_TOUCHED = "Last Touched Parameter";
    public static final String MAIN_ENCODER_SHUFFLE = "Shuffle";
    public static final String MAIN_ENCODER_NOTE_REPEAT = "Note Repeat";
    public static final String[] MAIN_ENCODER_ROLES = {
            MAIN_ENCODER_LAST_TOUCHED,
            MAIN_ENCODER_SHUFFLE,
            MAIN_ENCODER_NOTE_REPEAT
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
}
