package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.Track;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

class MulticlipClipControllerPinningTest {
    @Test
    void waitsForTheExactChildTrackAndSceneBeforePinningTheRow() {
        final Fixture fixture = new Fixture();
        final Track targetTrack = mock(Track.class, Mockito.RETURNS_DEEP_STUBS);
        when(targetTrack.position().get()).thenReturn(7);
        when(fixture.cursor.position().get()).thenReturn(2);
        when(fixture.clip.exists().get()).thenReturn(true);
        when(fixture.fineClip.exists().get()).thenReturn(true);
        when(fixture.clip.clipLauncherSlot().sceneIndex().get()).thenReturn(1);
        when(fixture.fineClip.clipLauncherSlot().sceneIndex().get()).thenReturn(1);
        final AtomicReference<Boolean> completed = new AtomicReference<>();

        fixture.controller.retarget(
                0, targetTrack, TrackLaneMapping.fromChildPosition(0), 0, 5, true, completed::set);

        verify(fixture.cursor).selectChannel(targetTrack);
        verify(fixture.cursor, never()).selectSlot(5);
        assertNull(completed.get());

        fixture.runNextTask();
        verify(fixture.cursor, never()).selectSlot(5);
        assertNull(completed.get());

        when(fixture.cursor.position().get()).thenReturn(7);
        fixture.runNextTask();
        verify(fixture.cursor).selectSlot(5);
        verify(fixture.clip.isPinned(), never()).set(true);
        verify(fixture.fineClip.isPinned(), never()).set(true);

        fixture.runNextTask();
        verify(fixture.clip.isPinned(), never()).set(true);
        assertNull(completed.get());

        when(fixture.clip.clipLauncherSlot().sceneIndex().get()).thenReturn(5);
        when(fixture.fineClip.clipLauncherSlot().sceneIndex().get()).thenReturn(5);
        fixture.runNextTask();
        verify(fixture.clip.isPinned()).set(true);
        verify(fixture.fineClip.isPinned()).set(true);

        fixture.runNextTask();
        assertTrue(fixture.controller.isReady(0));
        assertTrue(completed.get());

        final InOrder order =
                inOrder(
                        fixture.cursor,
                        fixture.cursor.isPinned(),
                        fixture.clip.isPinned(),
                        fixture.fineClip.isPinned());
        order.verify(fixture.cursor.isPinned()).set(false);
        order.verify(fixture.cursor).selectChannel(targetTrack);
        order.verify(fixture.cursor.isPinned()).set(true);
        order.verify(fixture.cursor).selectSlot(5);
        order.verify(fixture.clip.isPinned()).set(true);
        order.verify(fixture.fineClip.isPinned()).set(true);
    }

    @Test
    void acceptsAnEmptySlotAfterTheExactChildTrackHasSettled() {
        final Fixture fixture = new Fixture();
        final Track targetTrack = mock(Track.class, Mockito.RETURNS_DEEP_STUBS);
        when(targetTrack.position().get()).thenReturn(7);
        when(fixture.cursor.position().get()).thenReturn(7);
        when(fixture.clip.exists().get()).thenReturn(false);
        when(fixture.fineClip.exists().get()).thenReturn(false);
        final AtomicReference<Boolean> completed = new AtomicReference<>();

        fixture.controller.retarget(
                0, targetTrack, TrackLaneMapping.fromChildPosition(0), 0, 5, false, completed::set);

        fixture.runNextTask();
        verify(fixture.cursor).selectSlot(5);
        fixture.runNextTask();
        verify(fixture.clip.isPinned()).set(true);
        verify(fixture.fineClip.isPinned()).set(true);
        fixture.runNextTask();

        assertTrue(fixture.controller.isReady(0));
        assertTrue(completed.get());
    }

    @Test
    void reportsFailureWithoutPinningWhenTheChildTrackNeverSettles() {
        final Fixture fixture = new Fixture();
        final Track targetTrack = mock(Track.class, Mockito.RETURNS_DEEP_STUBS);
        when(targetTrack.position().get()).thenReturn(7);
        when(fixture.cursor.position().get()).thenReturn(2);
        final AtomicReference<Boolean> completed = new AtomicReference<>();

        fixture.controller.retarget(
                0, targetTrack, TrackLaneMapping.fromChildPosition(0), 0, 5, true, completed::set);

        fixture.runAllTasks();

        assertFalse(fixture.controller.isReady(0));
        assertFalse(completed.get());
        verify(fixture.cursor, never()).selectSlot(5);
        verify(fixture.clip.isPinned(), never()).set(true);
        verify(fixture.fineClip.isPinned(), never()).set(true);
    }

    private static final class Fixture {
        private final ControllerHost host = mock(ControllerHost.class);
        private final Deque<Runnable> tasks = new ArrayDeque<>();
        private final CursorTrack cursor;
        private final PinnableCursorClip clip;
        private final PinnableCursorClip fineClip;
        private final MulticlipClipController controller;

        private Fixture() {
            final CursorTrack[] cursors = new CursorTrack[MulticlipClipController.VISIBLE_LANES];
            final PinnableCursorClip[] clips =
                    new PinnableCursorClip[MulticlipClipController.VISIBLE_LANES];
            final PinnableCursorClip[] fineClips =
                    new PinnableCursorClip[MulticlipClipController.VISIBLE_LANES];
            for (int row = 0; row < MulticlipClipController.VISIBLE_LANES; row++) {
                cursors[row] = mock(CursorTrack.class, Mockito.RETURNS_DEEP_STUBS);
                clips[row] = mock(PinnableCursorClip.class, Mockito.RETURNS_DEEP_STUBS);
                fineClips[row] = mock(PinnableCursorClip.class, Mockito.RETURNS_DEEP_STUBS);
                when(cursors[row].createLauncherCursorClip(anyString(), anyString(), eq(16), eq(1)))
                        .thenReturn(clips[row]);
                when(cursors[row].createLauncherCursorClip(
                                anyString(), anyString(), eq(4096), eq(1)))
                        .thenReturn(fineClips[row]);
            }
            when(host.createCursorTrack(anyString(), anyString(), eq(0), eq(16), anyBoolean()))
                    .thenReturn(cursors[0])
                    .thenReturn(cursors[1])
                    .thenReturn(cursors[2])
                    .thenReturn(cursors[3]);
            Mockito.doAnswer(
                            invocation -> {
                                tasks.addLast(invocation.getArgument(0));
                                return null;
                            })
                    .when(host)
                    .scheduleTask(any(Runnable.class), anyLong());

            controller = new MulticlipClipController(host);
            cursor = cursors[0];
            clip = clips[0];
            fineClip = fineClips[0];
            when(cursor.exists().get()).thenReturn(true);
            clearInvocations(cursor, cursor.isPinned(), clip.isPinned(), fineClip.isPinned());
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
