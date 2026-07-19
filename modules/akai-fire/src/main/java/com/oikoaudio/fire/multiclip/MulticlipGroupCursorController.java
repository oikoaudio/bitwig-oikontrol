package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import java.util.function.Consumer;

/** Acquires a selected parent group first, with a project-wide named PolySeq fallback. */
final class MulticlipGroupCursorController {
    private static final int MAX_TARGET_ATTEMPTS = 20;
    private static final long TARGET_RETRY_MS = 50;

    private final ControllerHost host;
    private final CursorTrack cursor;
    private final TrackBank projectTracks;
    private long generation;

    MulticlipGroupCursorController(final ControllerHost host, final CursorTrack cursor) {
        this(host, cursor, null);
    }

    MulticlipGroupCursorController(
            final ControllerHost host, final CursorTrack cursor, final TrackBank projectTracks) {
        this.host = host;
        this.cursor = cursor;
        this.projectTracks = projectTracks;
        cursor.name().markInterested();
        if (projectTracks != null) {
            observeProjectTracks();
        }
    }

    void activate(final Consumer<Boolean> onComplete) {
        final long activation = ++generation;
        cursor.isPinned().set(false);
        seekGroup(activation, onComplete, 0);
    }

    void findNamed(final Consumer<Discovery> onComplete) {
        final long activation = ++generation;
        cursor.isPinned().set(false);
        if (projectTracks == null) {
            onComplete.accept(Discovery.NOT_FOUND);
            return;
        }
        projectTracks.scrollPosition().set(0);
        host.scheduleTask(() -> scanPage(activation, onComplete, new ScanState()), TARGET_RETRY_MS);
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

    private void observeProjectTracks() {
        projectTracks.itemCount().markInterested();
        projectTracks.scrollPosition().markInterested();
        for (int index = 0; index < projectTracks.getSizeOfBank(); index++) {
            final Track track = projectTracks.getItemAt(index);
            track.exists().markInterested();
            track.isGroup().markInterested();
            track.name().markInterested();
        }
    }

    private void scanPage(
            final long activation, final Consumer<Discovery> onComplete, final ScanState state) {
        if (generation != activation) {
            return;
        }
        final int itemCount = projectTracks.itemCount().get();
        final int bankSize = projectTracks.getSizeOfBank();
        final int pageStart = projectTracks.scrollPosition().get();
        final int visibleCount = Math.min(bankSize, Math.max(0, itemCount - pageStart));
        final int firstUnscannedIndex = Math.max(0, state.nextAbsoluteIndex - pageStart);
        for (int index = firstUnscannedIndex; index < visibleCount; index++) {
            final Track track = projectTracks.getItemAt(index);
            if (track.exists().get()
                    && track.isGroup().get()
                    && MulticlipPolySeqGroup.matches(track.name().get())) {
                state.matches++;
                state.absoluteIndex = pageStart + index;
                if (state.matches > 1) {
                    onComplete.accept(Discovery.MULTIPLE);
                    return;
                }
            }
        }
        state.nextAbsoluteIndex = pageStart + visibleCount;
        if (state.nextAbsoluteIndex < itemCount) {
            final int nextPage =
                    Math.min(state.nextAbsoluteIndex, Math.max(0, itemCount - bankSize));
            projectTracks.scrollPosition().set(nextPage);
            host.scheduleTask(() -> scanPage(activation, onComplete, state), TARGET_RETRY_MS);
            return;
        }
        if (state.matches == 0) {
            onComplete.accept(Discovery.NOT_FOUND);
            return;
        }
        projectTracks.scrollPosition().set(state.absoluteIndex);
        host.scheduleTask(
                () -> targetNamedGroup(activation, onComplete, state.absoluteIndex),
                TARGET_RETRY_MS);
    }

    private void targetNamedGroup(
            final long activation, final Consumer<Discovery> onComplete, final int absoluteIndex) {
        if (generation != activation) {
            return;
        }
        final int indexInBank = absoluteIndex - projectTracks.scrollPosition().get();
        if (indexInBank < 0 || indexInBank >= projectTracks.getSizeOfBank()) {
            onComplete.accept(Discovery.NOT_FOUND);
            return;
        }
        final Track track = projectTracks.getItemAt(indexInBank);
        if (!track.exists().get()
                || !track.isGroup().get()
                || !MulticlipPolySeqGroup.matches(track.name().get())) {
            onComplete.accept(Discovery.NOT_FOUND);
            return;
        }
        final String targetName = track.name().get();
        cursor.selectChannel(track);
        host.scheduleTask(
                () -> seekNamedGroup(activation, onComplete, targetName, 0), TARGET_RETRY_MS);
    }

    private void seekNamedGroup(
            final long activation,
            final Consumer<Discovery> onComplete,
            final String targetName,
            final int attempt) {
        if (generation != activation) {
            return;
        }
        if (cursor.exists().get()
                && cursor.isGroup().get()
                && targetName.equals(cursor.name().get())) {
            cursor.isPinned().set(true);
            host.scheduleTask(
                    () -> verifyNamedGroup(activation, onComplete, targetName, attempt),
                    TARGET_RETRY_MS);
            return;
        }
        if (attempt < MAX_TARGET_ATTEMPTS) {
            host.scheduleTask(
                    () -> seekNamedGroup(activation, onComplete, targetName, attempt + 1),
                    TARGET_RETRY_MS);
        } else {
            onComplete.accept(Discovery.NOT_FOUND);
        }
    }

    private void verifyNamedGroup(
            final long activation,
            final Consumer<Discovery> onComplete,
            final String targetName,
            final int attempt) {
        if (generation != activation) {
            return;
        }
        if (cursor.exists().get()
                && cursor.isGroup().get()
                && targetName.equals(cursor.name().get())) {
            onComplete.accept(Discovery.NAMED);
            return;
        }
        cursor.isPinned().set(false);
        seekNamedGroup(activation, onComplete, targetName, attempt + 1);
    }

    enum Discovery {
        SELECTED,
        NAMED,
        NOT_FOUND,
        MULTIPLE
    }

    private static final class ScanState {
        private int matches;
        private int absoluteIndex = -1;
        private int nextAbsoluteIndex;
    }
}
