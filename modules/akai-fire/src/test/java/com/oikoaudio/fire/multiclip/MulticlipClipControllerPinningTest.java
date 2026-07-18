package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.Track;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MulticlipClipControllerPinningTest {
    @Test
    void selectsTheExactChildSlotAndWaitsForTheSharedCursorToFollow() {
        final Fixture fixture = new Fixture();
        final Track targetTrack = mock(Track.class, Mockito.RETURNS_DEEP_STUBS);
        final ClipLauncherSlot targetSlot = mock(ClipLauncherSlot.class);
        when(targetTrack.position().get()).thenReturn(7);
        when(fixture.cursor.position().get()).thenReturn(2);
        fixture.existingClipAtScene(5);
        final AtomicReference<Boolean> completed = new AtomicReference<>();

        fixture.controller.retarget(
                targetTrack,
                targetSlot,
                TrackLaneMapping.fromChildPosition(0),
                0,
                5,
                true,
                completed::set);

        verify(fixture.cursor).selectChannel(targetTrack);
        verify(targetSlot).select();
        verify(fixture.cursor, never()).selectSlot(5);
        assertNull(completed.get());

        fixture.runNextTask();
        assertNull(completed.get());
        when(fixture.cursor.position().get()).thenReturn(7);
        fixture.runNextTask();
        assertTrue(fixture.controller.isReady());
        fixture.runNextTask();
        assertTrue(completed.get());
        verify(fixture.clip.isPinned(), never()).set(true);
        verify(fixture.fineClip.isPinned(), never()).set(true);
    }

    @Test
    void acceptsAnEmptyExactChildSlot() {
        final Fixture fixture = new Fixture();
        final Track targetTrack = mock(Track.class, Mockito.RETURNS_DEEP_STUBS);
        final ClipLauncherSlot targetSlot = mock(ClipLauncherSlot.class);
        when(targetTrack.position().get()).thenReturn(7);
        when(fixture.cursor.position().get()).thenReturn(7);
        when(fixture.clip.exists().get()).thenReturn(false);
        when(fixture.fineClip.exists().get()).thenReturn(false);
        final AtomicReference<Boolean> completed = new AtomicReference<>();

        fixture.controller.retarget(
                targetTrack,
                targetSlot,
                TrackLaneMapping.fromChildPosition(0),
                0,
                5,
                false,
                completed::set);
        fixture.runAllTasks();

        assertTrue(fixture.controller.isReady());
        assertTrue(completed.get());
        verify(targetSlot).select();
        verify(fixture.cursor, never()).selectSlot(5);
    }

    @Test
    void reportsFailureWhenTheSelectedTrackNeverSettles() {
        final Fixture fixture = new Fixture();
        final Track targetTrack = mock(Track.class, Mockito.RETURNS_DEEP_STUBS);
        final ClipLauncherSlot targetSlot = mock(ClipLauncherSlot.class);
        when(targetTrack.position().get()).thenReturn(7);
        when(fixture.cursor.position().get()).thenReturn(2);
        fixture.existingClipAtScene(5);
        final AtomicReference<Boolean> completed = new AtomicReference<>();

        fixture.controller.retarget(
                targetTrack,
                targetSlot,
                TrackLaneMapping.fromChildPosition(0),
                0,
                5,
                true,
                completed::set);
        fixture.runAllTasks();

        assertFalse(fixture.controller.isReady());
        assertFalse(completed.get());
        verify(targetSlot).select();
        verify(fixture.cursor, never()).selectSlot(5);
    }

    private static final class Fixture {
        private final ControllerHost host = mock(ControllerHost.class);
        private final CursorTrack cursor = mock(CursorTrack.class, Mockito.RETURNS_DEEP_STUBS);
        private final PinnableCursorClip clip =
                mock(PinnableCursorClip.class, Mockito.RETURNS_DEEP_STUBS);
        private final PinnableCursorClip fineClip =
                mock(PinnableCursorClip.class, Mockito.RETURNS_DEEP_STUBS);
        private final Deque<Runnable> tasks = new ArrayDeque<>();
        private final MulticlipClipController controller;

        private Fixture() {
            when(cursor.createLauncherCursorClip(
                            anyString(), anyString(), eq(MulticlipXoxLayout.PATTERN_COUNT), eq(1)))
                    .thenReturn(clip);
            when(cursor.createLauncherCursorClip(anyString(), anyString(), eq(4096), eq(1)))
                    .thenReturn(fineClip);
            Mockito.doAnswer(
                            invocation -> {
                                tasks.addLast(invocation.getArgument(0));
                                return null;
                            })
                    .when(host)
                    .scheduleTask(any(Runnable.class), anyLong());
            controller = new MulticlipClipController(host, cursor);
        }

        private void existingClipAtScene(final int scene) {
            when(clip.exists().get()).thenReturn(true);
            when(fineClip.exists().get()).thenReturn(true);
            when(clip.clipLauncherSlot().sceneIndex().get()).thenReturn(scene);
            when(fineClip.clipLauncherSlot().sceneIndex().get()).thenReturn(scene);
        }

        private void runNextTask() {
            tasks.removeFirst().run();
        }

        private void runAllTasks() {
            while (!tasks.isEmpty()) {
                runNextTask();
            }
        }
    }
}
