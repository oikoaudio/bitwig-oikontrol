package com.oikoaudio.fire.fugue;

public record FugueLineSettings(FugueDirection direction, FugueSpeed speed, int startOffset,
                                int pitchDegreeOffset, int pitchSemitoneOffset,
                                int velocityOffset, int chancePercent, int gatePercent) {
    public static FugueLineSettings init() {
        return new FugueLineSettings(FugueDirection.FORWARD, FugueSpeed.NORMAL, 0, 0, 0, 0, 100, 100);
    }

    public static FugueLineSettings semitone(final FugueDirection direction, final FugueSpeed speed,
                                             final int startOffset, final int pitchSemitoneOffset) {
        return new FugueLineSettings(direction, speed, startOffset, 0, pitchSemitoneOffset, 0, 100, 100);
    }

    public FugueLineSettings(final FugueDirection direction, final FugueSpeed speed,
                             final int startOffset, final int pitchDegreeOffset) {
        this(direction, speed, startOffset, pitchDegreeOffset, 0, 0, 100, 100);
    }

    public FugueLineSettings {
        direction = direction == null ? FugueDirection.FORWARD : direction;
        speed = speed == null ? FugueSpeed.NORMAL : speed;
        startOffset = Math.max(0, Math.min(FuguePattern.MAX_STEPS - 1, startOffset));
        pitchDegreeOffset = Math.max(-24, Math.min(24, pitchDegreeOffset));
        pitchSemitoneOffset = Math.max(-48, Math.min(48, pitchSemitoneOffset));
        velocityOffset = Math.max(-126, Math.min(126, velocityOffset));
        chancePercent = Math.max(0, Math.min(100, chancePercent));
        gatePercent = Math.max(10, Math.min(200, gatePercent));
    }

    public FugueLineSettings withDirection(final FugueDirection value) {
        return new FugueLineSettings(value, speed, startOffset, pitchDegreeOffset, pitchSemitoneOffset,
                velocityOffset, chancePercent, gatePercent);
    }

    public FugueLineSettings withSpeed(final FugueSpeed value) {
        return new FugueLineSettings(direction, value, startOffset, pitchDegreeOffset, pitchSemitoneOffset,
                velocityOffset, chancePercent, gatePercent);
    }

    public FugueLineSettings withStartOffset(final int value) {
        return new FugueLineSettings(direction, speed, value, pitchDegreeOffset, pitchSemitoneOffset,
                velocityOffset, chancePercent, gatePercent);
    }

    public FugueLineSettings withPitchDegreeOffset(final int value) {
        return new FugueLineSettings(direction, speed, startOffset, value, pitchSemitoneOffset,
                velocityOffset, chancePercent, gatePercent);
    }

    public FugueLineSettings withPitchSemitoneOffset(final int value) {
        return new FugueLineSettings(direction, speed, startOffset, pitchDegreeOffset, value,
                velocityOffset, chancePercent, gatePercent);
    }

    public FugueLineSettings withVelocityOffset(final int value) {
        return new FugueLineSettings(direction, speed, startOffset, pitchDegreeOffset, pitchSemitoneOffset,
                value, chancePercent, gatePercent);
    }

    public FugueLineSettings withChancePercent(final int value) {
        return new FugueLineSettings(direction, speed, startOffset, pitchDegreeOffset, pitchSemitoneOffset,
                velocityOffset, value, gatePercent);
    }

    public FugueLineSettings withGatePercent(final int value) {
        return new FugueLineSettings(direction, speed, startOffset, pitchDegreeOffset, pitchSemitoneOffset,
                velocityOffset, chancePercent, value);
    }

    public String summary() {
        return "%s %s st%d deg%+d sem%+d vel%+d ch%d gate%d".formatted(direction.label(), speed.label(), startOffset,
                pitchDegreeOffset, pitchSemitoneOffset, velocityOffset, chancePercent, gatePercent);
    }
}
