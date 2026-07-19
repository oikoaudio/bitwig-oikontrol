package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.ClipLauncherSlot;

/** Copies the active child clip, or an exact child-only scene snapshot, without a buffer. */
final class MulticlipChildCopyController {
    record SlotPair(ClipLauncherSlot source, ClipLauncherSlot target) {}

    private MulticlipChildCopyController() {}

    static boolean copyClip(final ClipLauncherSlot source, final ClipLauncherSlot target) {
        if (source == target || !source.hasContent().get()) {
            return false;
        }
        target.replaceInsertionPoint().copySlotsOrScenes(source);
        return true;
    }

    static int copyScene(final Iterable<SlotPair> pairs) {
        int changed = 0;
        for (final SlotPair pair : pairs) {
            if (pair.source() == pair.target()) {
                continue;
            }
            if (pair.source().hasContent().get()) {
                pair.target().replaceInsertionPoint().copySlotsOrScenes(pair.source());
                changed++;
            } else if (pair.target().hasContent().get()) {
                pair.target().deleteObject();
                changed++;
            }
        }
        return changed;
    }
}
