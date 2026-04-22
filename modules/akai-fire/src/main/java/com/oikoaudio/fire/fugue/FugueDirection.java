package com.oikoaudio.fire.fugue;

public enum FugueDirection {
    FORWARD("Forward"),
    REVERSE("Reverse"),
    PING_PONG("PingPong");

    private final String label;

    FugueDirection(final String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public FugueDirection next(final int amount) {
        final FugueDirection[] values = values();
        return values[Math.floorMod(ordinal() + amount, values.length)];
    }
}
