package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import java.util.function.Consumer;

/** Acquires and pins the parent Drum group only after its cursor identity has settled. */
final class MulticlipGroupCursorController {
    private static final int MAX_TARGET_ATTEMPTS = 20;
    private static final long TARGET_RETRY_MS = 50;

    private final ControllerHost host;
    private final CursorTrack cursor;
    private long generation;

    MulticlipGroupCursorController(final ControllerHost host, final CursorTrack cursor) {
        this.host = host;
        this.cursor = cursor;
    }

    void activate(final Consumer<Boolean> onComplete) {
        final long activation = ++generation;
        cursor.isPinned().set(false);
        seekGroup(activation, onComplete, 0);
    }

    void deactivate() {
        generation++;
        cursor.isPinned().set(false);
    }

    private void seekGroup(
            final long activation, final Consumer<Boolean> onComplete, final int attempt) {
        if (generation != activation) {
            return;
        }
        if (cursor.exists().get() && cursor.isGroup().get()) {
            cursor.isPinned().set(true);
            host.scheduleTask(
                    () -> verifyPinnedGroup(activation, onComplete, attempt), TARGET_RETRY_MS);
            return;
        }
        if (cursor.exists().get()) {
            cursor.selectParent();
        }
        if (attempt < MAX_TARGET_ATTEMPTS) {
            host.scheduleTask(
                    () -> seekGroup(activation, onComplete, attempt + 1), TARGET_RETRY_MS);
        } else {
            onComplete.accept(false);
        }
    }

    private void verifyPinnedGroup(
            final long activation, final Consumer<Boolean> onComplete, final int attempt) {
        if (generation != activation) {
            return;
        }
        if (cursor.exists().get() && cursor.isGroup().get()) {
            onComplete.accept(true);
            return;
        }
        cursor.isPinned().set(false);
        seekGroup(activation, onComplete, attempt + 1);
    }
}
