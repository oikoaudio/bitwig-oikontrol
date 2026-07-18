package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

class MulticlipGroupCursorControllerTest {
    @Test
    void acceptsAnAlreadySelectedGroupWithoutChangingTrackSelection() {
        final ControllerHost host = mock(ControllerHost.class);
        final CursorTrack cursor = mock(CursorTrack.class, Mockito.RETURNS_DEEP_STUBS);
        final AtomicReference<Runnable> scheduled = new AtomicReference<>();
        Mockito.doAnswer(
                        invocation -> {
                            scheduled.set(invocation.getArgument(0));
                            return null;
                        })
                .when(host)
                .scheduleTask(any(Runnable.class), anyLong());
        when(cursor.exists().get()).thenReturn(true);
        when(cursor.isGroup().get()).thenReturn(true);
        final AtomicReference<Boolean> completed = new AtomicReference<>();
        final MulticlipGroupCursorController controller =
                new MulticlipGroupCursorController(host, cursor);

        controller.activate(completed::set);
        scheduled.get().run();

        assertTrue(completed.get());
        verify(cursor, never()).selectParent();
    }

    @Test
    void pinsOnlyAfterTheCursorHasSettledOnAGroupTrack() {
        final ControllerHost host = mock(ControllerHost.class);
        final CursorTrack cursor = mock(CursorTrack.class, Mockito.RETURNS_DEEP_STUBS);
        final Deque<Runnable> tasks = new ArrayDeque<>();
        Mockito.doAnswer(
                        invocation -> {
                            tasks.addLast(invocation.getArgument(0));
                            return null;
                        })
                .when(host)
                .scheduleTask(any(Runnable.class), anyLong());
        when(cursor.exists().get()).thenReturn(true);
        when(cursor.isGroup().get()).thenReturn(false);
        final AtomicReference<Boolean> completed = new AtomicReference<>();
        final MulticlipGroupCursorController controller =
                new MulticlipGroupCursorController(host, cursor);

        controller.activate(completed::set);

        assertNull(completed.get());
        final InOrder order = inOrder(cursor.isPinned(), cursor);
        order.verify(cursor.isPinned()).set(false);
        order.verify(cursor).selectParent();

        when(cursor.isGroup().get()).thenReturn(true);
        tasks.removeFirst().run();
        order.verify(cursor.isPinned()).set(true);
        assertNull(completed.get());

        tasks.removeFirst().run();
        assertTrue(completed.get());
    }
}
