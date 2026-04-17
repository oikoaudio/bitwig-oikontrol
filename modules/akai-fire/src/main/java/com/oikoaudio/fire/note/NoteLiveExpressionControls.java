package com.oikoaudio.fire.note;

/**
 * Owns the live MIDI expression values used by Note mode and emits the corresponding MIDI events.
 */
final class NoteLiveExpressionControls {
    private static final int MIN_MIDI_VALUE = 0;
    private static final int MAX_MIDI_VALUE = 127;
    private static final int DEFAULT_TIMBRE = 0;
    private static final int DEFAULT_PITCH_EXPRESSION = 64;
    private static final int DEFAULT_PITCH_BEND = 8192;

    private final MidiExpressionOut midiOut;
    private int pressure = MIN_MIDI_VALUE;
    private int timbre = DEFAULT_TIMBRE;
    private int modulation = MIN_MIDI_VALUE;
    private int pitchExpression = DEFAULT_PITCH_EXPRESSION;
    private int transientPitchBendOffset = 0;

    NoteLiveExpressionControls(final MidiExpressionOut midiOut) {
        this.midiOut = midiOut;
    }

    int pressure() {
        return pressure;
    }

    int timbre() {
        return timbre;
    }

    int modulation() {
        return modulation;
    }

    int pitchExpression() {
        return pitchExpression;
    }

    boolean adjustPressure(final int inc) {
        final int next = clampMidiValue(pressure + inc);
        if (next == pressure) {
            return false;
        }
        pressure = next;
        midiOut.channelAftertouch(pressure);
        return true;
    }

    boolean adjustTimbre(final int inc) {
        final int next = clampMidiValue(timbre + inc);
        if (next == timbre) {
            return false;
        }
        timbre = next;
        midiOut.timbre(timbre);
        return true;
    }

    boolean adjustModulation(final int inc) {
        final int next = clampMidiValue(modulation + inc);
        if (next == modulation) {
            return false;
        }
        modulation = next;
        midiOut.modulation(modulation);
        return true;
    }

    boolean adjustPitchExpression(final int inc) {
        final int next = clampMidiValue(pitchExpression + inc);
        if (next == pitchExpression) {
            return false;
        }
        pitchExpression = next;
        emitPitchBend();
        return true;
    }

    void resetPressure() {
        pressure = MIN_MIDI_VALUE;
        midiOut.channelAftertouch(pressure);
    }

    void resetModulation() {
        modulation = MIN_MIDI_VALUE;
        midiOut.modulation(modulation);
    }

    void resetTimbre() {
        timbre = DEFAULT_TIMBRE;
        midiOut.timbre(timbre);
    }

    void resetPitchExpression() {
        pitchExpression = DEFAULT_PITCH_EXPRESSION;
        emitPitchBend();
    }

    void setTransientPitchBendValue(final int value) {
        transientPitchBendOffset = pitchBendValueFor(clampMidiValue(value)) - DEFAULT_PITCH_BEND;
        emitPitchBend();
    }

    private static int clampMidiValue(final int value) {
        return Math.max(MIN_MIDI_VALUE, Math.min(MAX_MIDI_VALUE, value));
    }

    static int pitchBendValueFor(final int value) {
        if (value <= DEFAULT_PITCH_EXPRESSION) {
            return (int) Math.round((value / (double) DEFAULT_PITCH_EXPRESSION) * 8192.0);
        }
        final int rangeAboveCenter = MAX_MIDI_VALUE - DEFAULT_PITCH_EXPRESSION;
        return 8192 + (int) Math.round(((value - DEFAULT_PITCH_EXPRESSION) / (double) rangeAboveCenter)
                * (16383.0 - 8192.0));
    }

    private static int clampPitchBend(final int bend) {
        return Math.max(0, Math.min(16383, bend));
    }

    private void emitPitchBend() {
        midiOut.pitchBend(clampPitchBend(pitchBendValueFor(pitchExpression) + transientPitchBendOffset));
    }

    interface MidiExpressionOut {
        void channelAftertouch(int value);

        void modulation(int value);

        void timbre(int value);

        void pitchBend(int bend);
    }
}
