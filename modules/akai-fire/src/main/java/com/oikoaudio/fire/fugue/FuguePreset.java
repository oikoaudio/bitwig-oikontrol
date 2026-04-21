package com.oikoaudio.fire.fugue;

public enum FuguePreset {
    INIT("Init", FugueLineSettings.init()),
    BASS_AUGMENTED("Bass /8", new FugueLineSettings(FugueDirection.FORWARD, FugueSpeed.DIVIDE_8, 1, -7)),
    HIGH_HALF("High /2", new FugueLineSettings(FugueDirection.FORWARD, FugueSpeed.DIVIDE_2, 1, 18)),
    REVERSE_DOUBLE("Rev x2", new FugueLineSettings(FugueDirection.REVERSE, FugueSpeed.TIMES_2, 1, 11)),
    FIFTH_DOUBLE("5th x2", new FugueLineSettings(FugueDirection.FORWARD, FugueSpeed.TIMES_2, 0, 4)),
    FOURTH_REVERSE("4th Rev", new FugueLineSettings(FugueDirection.REVERSE, FugueSpeed.NORMAL, 0, 3)),
    OCTAVE_PING("8ve Ping", new FugueLineSettings(FugueDirection.PING_PONG, FugueSpeed.NORMAL, 0, 7));

    private final String label;
    private final FugueLineSettings settings;

    FuguePreset(final String label, final FugueLineSettings settings) {
        this.label = label;
        this.settings = settings;
    }

    public String label() {
        return label;
    }

    public FugueLineSettings settings() {
        return settings;
    }

    public FuguePreset next(final int amount) {
        final FuguePreset[] values = values();
        return values[Math.floorMod(ordinal() + amount, values.length)];
    }
}
