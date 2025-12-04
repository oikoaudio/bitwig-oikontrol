package com.oikoaudio.fire.sequence;

import java.util.ArrayList;
import java.util.List;

class EuclidUtil {
    static boolean[] build(int length, int pulses) {
        if (length <= 0) {
            return new boolean[0];
        }
        pulses = Math.max(0, Math.min(length, pulses));
        boolean[] result = new boolean[length];
        if (pulses == 0) {
            return result;
        }
        for (int i = 0; i < length; i++) {
            // Evenly space pulses using a simple distribution formula.
            result[i] = ((i * pulses) % length) < pulses;
        }
        return result;
    }

    static boolean[] rotate(boolean[] pattern, int rot) {
        int len = pattern.length;
        if (len == 0) return pattern;
        boolean[] out = new boolean[len];
        for (int i = 0; i < len; i++) {
            int src = (i - rot) % len;
            if (src < 0) src += len;
            out[i] = pattern[src];
        }
        return out;
    }
}
