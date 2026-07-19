package com.oikoaudio.fire.multiclip;

import java.util.Locale;

/** Forgiving project marker matcher; the documented spelling remains {@code [PolySeq]}. */
final class MulticlipPolySeqGroup {
    private MulticlipPolySeqGroup() {}

    static boolean matches(final String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        final String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
        return normalized.contains("polyseq");
    }
}
