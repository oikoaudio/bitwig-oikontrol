package com.oikoaudio.fire;

public final class FireControlPreferences {
    public static final String CATEGORY_FUNCTIONALITIES = "Functionalities";
    public static final String CATEGORY_CLIP_LAUNCH = "Clip Launch";
    public static final String CATEGORY_PINNING = "Pinning";
    public static final String CATEGORY_HARDWARE = "Hardware";
    public static final String CATEGORY_GENERATIVE_CONTROL = "Generative control";

    public static final double PAD_BRIGHTNESS_MIN = 20.0;
    public static final double PAD_BRIGHTNESS_MAX = 100.0;
    public static final double PAD_BRIGHTNESS_STEP = 5.0;
    public static final double PAD_BRIGHTNESS_DEFAULT = 60.0;
    public static final double PAD_BRIGHTNESS_SCALE_MIN = 0.70;
    public static final double PAD_BRIGHTNESS_SCALE_MAX = 2.50;
    public static final double PAD_SATURATION_MIN = 0.0;
    public static final double PAD_SATURATION_MAX = 150.0;
    public static final double PAD_SATURATION_STEP = 5.0;
    public static final double PAD_SATURATION_DEFAULT = 100.0;
    public static final boolean ENCODER_TOUCH_RESET_DEFAULT = true;

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

    public static final String CLIP_LENGTH_1_BAR = "1 bar";
    public static final String CLIP_LENGTH_2_BARS = "2 bars";
    public static final String CLIP_LENGTH_4_BARS = "4 bars";
    public static final String[] DEFAULT_CLIP_LENGTHS = {
            CLIP_LENGTH_1_BAR,
            CLIP_LENGTH_2_BARS,
            CLIP_LENGTH_4_BARS
    };

    public static final String MAIN_ENCODER_LAST_TOUCHED = "Last Touched Parameter";
    public static final String MAIN_ENCODER_SHUFFLE = "Shuffle";
    public static final String MAIN_ENCODER_TEMPO = "Tempo";
    public static final String MAIN_ENCODER_NOTE_REPEAT = "Note Repeat";
    public static final String MAIN_ENCODER_TRACK_SELECT = "Track Select";
    public static final String MAIN_ENCODER_DRUM_GRID = "Drum Grid";
    public static final String[] MAIN_ENCODER_ROLES = {
            MAIN_ENCODER_LAST_TOUCHED,
            MAIN_ENCODER_SHUFFLE,
            MAIN_ENCODER_TEMPO,
            MAIN_ENCODER_NOTE_REPEAT,
            MAIN_ENCODER_TRACK_SELECT,
            MAIN_ENCODER_DRUM_GRID
    };
    public static final String MAIN_ENCODER_STARTUP_LAST_TOUCHED = "Last Touched";
    public static final String MAIN_ENCODER_STARTUP_FUNCTION_SET = "Function Set";
    public static final String[] MAIN_ENCODER_STARTUP_STATES = {
            MAIN_ENCODER_STARTUP_LAST_TOUCHED,
            MAIN_ENCODER_STARTUP_FUNCTION_SET
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

    public static final String DEFAULT_SCALE_PIANO = "Piano/Chromatic";
    public static final String DEFAULT_SCALE_MINOR = "Minor";
    public static final String DEFAULT_SCALE_HARMONIC_MINOR = "Harmonic Minor";
    public static final String DEFAULT_SCALE_MELODIC_MINOR = "Melodic Minor";
    public static final String DEFAULT_SCALE_MAJOR = "Major";
    public static final String DEFAULT_SCALE_MINOR_PENTATONIC = "Minor Pentatonic";
    public static final String DEFAULT_SCALE_DORIAN = "Dorian";
    public static final String DEFAULT_SCALE_MIXOLYDIAN = "Mixolydian";
    public static final String[] DEFAULT_SCALES = {
            DEFAULT_SCALE_PIANO,
            DEFAULT_SCALE_MAJOR,
            DEFAULT_SCALE_MINOR,
            DEFAULT_SCALE_HARMONIC_MINOR,
            DEFAULT_SCALE_MELODIC_MINOR,
            DEFAULT_SCALE_MINOR_PENTATONIC,
            DEFAULT_SCALE_DORIAN,
            DEFAULT_SCALE_MIXOLYDIAN
    };
    public static final String[] DEFAULT_ROOT_KEYS = {
            "C",
            "C#",
            "D",
            "D#",
            "E",
            "F",
            "F#",
            "G",
            "G#",
            "A",
            "A#",
            "B"
    };
    public static final String DEFAULT_ROOT_KEY = "C";
    public static final String[] DEFAULT_NOTE_INPUT_OCTAVES = {
            "2",
            "3",
            "4"
    };
    public static final String DEFAULT_NOTE_INPUT_OCTAVE = "3";
    public static final String[] DEFAULT_VELOCITY_SENSITIVITIES = {
            "0",
            "10",
            "20",
            "30",
            "40",
            "50",
            "60",
            "70",
            "80",
            "90",
            "100"
    };
    public static final String DEFAULT_VELOCITY_SENSITIVITY = "80";

    public static final String MELODIC_SEED_MODE_RANDOM = "Random";
    public static final String MELODIC_SEED_MODE_FIXED = "Fixed";
    public static final String[] MELODIC_SEED_MODES = {
            MELODIC_SEED_MODE_RANDOM,
            MELODIC_SEED_MODE_FIXED
    };
    public static final long MELODIC_FIXED_SEED_DEFAULT = 1234L;
    public static final long MELODIC_FIXED_SEED_MIN = 1L;
    public static final long MELODIC_FIXED_SEED_MAX = 999999L;

    public static final String DRUM_PIN_MODE_FOLLOW_SELECTION = "Follow Selection";
    public static final String DRUM_PIN_MODE_FIRST_DRUM_MACHINE = "Auto-select First Drum Machine";
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

    public static String nextAlternateMainEncoderRole(final String currentRole) {
        final String normalizedRole = normalizeMainEncoderRole(currentRole);
        if (MAIN_ENCODER_SHUFFLE.equals(normalizedRole)) {
            return MAIN_ENCODER_TEMPO;
        }
        if (MAIN_ENCODER_TEMPO.equals(normalizedRole)) {
            return MAIN_ENCODER_NOTE_REPEAT;
        }
        if (MAIN_ENCODER_NOTE_REPEAT.equals(normalizedRole)) {
            return MAIN_ENCODER_TRACK_SELECT;
        }
        if (MAIN_ENCODER_TRACK_SELECT.equals(normalizedRole)) {
            return MAIN_ENCODER_DRUM_GRID;
        }
        return MAIN_ENCODER_SHUFFLE;
    }

    public static String normalizeMainEncoderStartupState(final String preferenceValue) {
        for (final String value : MAIN_ENCODER_STARTUP_STATES) {
            if (value.equals(preferenceValue)) {
                return value;
            }
        }
        return MAIN_ENCODER_STARTUP_FUNCTION_SET;
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

    public static String normalizeDefaultScale(final String preferenceValue) {
        for (final String value : DEFAULT_SCALES) {
            if (value.equals(preferenceValue)) {
                return value;
            }
        }
        return DEFAULT_SCALE_PIANO;
    }

    public static String normalizeMelodicSeedMode(final String preferenceValue) {
        for (final String value : MELODIC_SEED_MODES) {
            if (value.equals(preferenceValue)) {
                return value;
            }
        }
        return MELODIC_SEED_MODE_RANDOM;
    }


    public static String normalizeDefaultRootKey(final String preferenceValue) {
        for (final String value : DEFAULT_ROOT_KEYS) {
            if (value.equals(preferenceValue)) {
                return value;
            }
        }
        return DEFAULT_ROOT_KEY;
    }

    public static int toDefaultRootKey(final String preferenceValue) {
        final String normalized = normalizeDefaultRootKey(preferenceValue);
        for (int i = 0; i < DEFAULT_ROOT_KEYS.length; i++) {
            if (DEFAULT_ROOT_KEYS[i].equals(normalized)) {
                return i;
            }
        }
        return 0;
    }

    public static String normalizeDefaultVelocitySensitivity(final String preferenceValue) {
        for (final String value : DEFAULT_VELOCITY_SENSITIVITIES) {
            if (value.equals(preferenceValue)) {
                return value;
            }
        }
        return DEFAULT_VELOCITY_SENSITIVITY;
    }

    public static int toDefaultVelocitySensitivity(final String preferenceValue) {
        return Integer.parseInt(normalizeDefaultVelocitySensitivity(preferenceValue));
    }

    public static String normalizeDefaultNoteInputOctave(final String preferenceValue) {
        for (final String value : DEFAULT_NOTE_INPUT_OCTAVES) {
            if (value.equals(preferenceValue)) {
                return value;
            }
        }
        return DEFAULT_NOTE_INPUT_OCTAVE;
    }

    public static int toDefaultNoteInputOctave(final String preferenceValue) {
        return Integer.parseInt(normalizeDefaultNoteInputOctave(preferenceValue));
    }

    public static String normalizeDrumPinMode(final String preferenceValue) {
        for (final String value : DRUM_PIN_MODES) {
            if (value.equals(preferenceValue)) {
                return value;
            }
        }
        return DRUM_PIN_MODE_FIRST_DRUM_MACHINE;
    }

    public static String normalizeDefaultClipLength(final String preferenceValue) {
        for (final String value : DEFAULT_CLIP_LENGTHS) {
            if (value.equals(preferenceValue)) {
                return value;
            }
        }
        return CLIP_LENGTH_2_BARS;
    }

    public static double toClipLengthBeats(final String preferenceValue) {
        return switch (normalizeDefaultClipLength(preferenceValue)) {
            case CLIP_LENGTH_1_BAR -> 4.0;
            case CLIP_LENGTH_4_BARS -> 16.0;
            default -> 8.0;
        };
    }

    public static boolean shouldAutoPinFirstDrumMachine(final String preferenceValue) {
        return DRUM_PIN_MODE_FIRST_DRUM_MACHINE.equals(normalizeDrumPinMode(preferenceValue));
    }

    public static double normalizePadBrightness(final double preferenceValue) {
        if (Double.isNaN(preferenceValue) || Double.isInfinite(preferenceValue)) {
            return PAD_BRIGHTNESS_DEFAULT;
        }
        return Math.max(PAD_BRIGHTNESS_MIN, Math.min(PAD_BRIGHTNESS_MAX, preferenceValue));
    }

    public static double toPadBrightnessScale(final double preferenceValue) {
        final double normalizedBrightness = normalizePadBrightness(preferenceValue);
        final double rangePosition = (normalizedBrightness - PAD_BRIGHTNESS_MIN)
                / (PAD_BRIGHTNESS_MAX - PAD_BRIGHTNESS_MIN);
        return PAD_BRIGHTNESS_SCALE_MIN
                + rangePosition * (PAD_BRIGHTNESS_SCALE_MAX - PAD_BRIGHTNESS_SCALE_MIN);
    }

    public static int scalePadColorComponent(final int component, final double brightnessPreference) {
        if (component <= 0) {
            return 0;
        }
        final double scale = toPadBrightnessScale(brightnessPreference);
        return Math.max(0, Math.min(127, (int) Math.round(component * scale)));
    }

    public static double normalizePadSaturation(final double preferenceValue) {
        if (Double.isNaN(preferenceValue) || Double.isInfinite(preferenceValue)) {
            return PAD_SATURATION_DEFAULT;
        }
        return Math.max(PAD_SATURATION_MIN, Math.min(PAD_SATURATION_MAX, preferenceValue));
    }

    public static int scalePadColorComponent(final int component,
                                             final int red,
                                             final int green,
                                             final int blue,
                                             final double brightnessPreference,
                                             final double saturationPreference) {
        if (component <= 0 && red <= 0 && green <= 0 && blue <= 0) {
            return 0;
        }
        final double saturation = normalizePadSaturation(saturationPreference) / 100.0;
        final double gray = (red + green + blue) / 3.0;
        final double saturatedComponent = gray + (component - gray) * saturation;
        final double brightness = toPadBrightnessScale(brightnessPreference);
        return Math.max(0, Math.min(127, (int) Math.round(saturatedComponent * brightness)));
    }
}
