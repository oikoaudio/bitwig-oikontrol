package com.oikoaudio.fire;

public enum VuMeterMode {
    OFF(FireControlPreferences.VU_METER_MODE_OFF),
    SELECTED(FireControlPreferences.VU_METER_MODE_SELECTED),
    ALL(FireControlPreferences.VU_METER_MODE_ALL);

    private final String preferenceValue;

    VuMeterMode(final String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    public String preferenceValue() {
        return preferenceValue;
    }

    public static VuMeterMode fromPreference(final String value) {
        for (final VuMeterMode mode : values()) {
            if (mode.preferenceValue.equals(value)) {
                return mode;
            }
        }
        return SELECTED;
    }
}
