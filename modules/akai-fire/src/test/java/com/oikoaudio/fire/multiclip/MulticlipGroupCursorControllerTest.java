package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.bitwig.extension.controller.api.SettableIntegerValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
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
        when(cursor.name().get()).thenReturn("[PolySeq] Grid");
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

    @Test
    void selectsTheOnlyNamedPolySeqGroupFromTheFlatProjectBank() {
        final ControllerHost host = mock(ControllerHost.class);
        final CursorTrack cursor = mock(CursorTrack.class, Mockito.RETURNS_DEEP_STUBS);
        final TrackBank tracks = mock(TrackBank.class, Mockito.RETURNS_DEEP_STUBS);
        final Track drums = track("Drums", true);
        final Track polySeq = track("[PolySeq] Grid", true);
        final Track instrument = track("Synth", false);
        final Track empty = track("", false);
        when(tracks.getSizeOfBank()).thenReturn(4);
        when(tracks.itemCount().get()).thenReturn(3);
        when(tracks.scrollPosition().get()).thenReturn(0);
        when(tracks.getItemAt(0)).thenReturn(drums);
        when(tracks.getItemAt(1)).thenReturn(polySeq);
        when(tracks.getItemAt(2)).thenReturn(instrument);
        when(tracks.getItemAt(3)).thenReturn(empty);
        when(cursor.exists().get()).thenReturn(true);
        when(cursor.isGroup().get()).thenReturn(true);
        when(cursor.name().get()).thenReturn("[PolySeq] Grid");
        final Deque<Runnable> tasks = scheduledTasks(host);
        final AtomicReference<MulticlipGroupCursorController.Discovery> completed =
                new AtomicReference<>();
        final MulticlipGroupCursorController controller =
                new MulticlipGroupCursorController(host, cursor, tracks);

        controller.findNamed(completed::set);
        while (!tasks.isEmpty()) {
            tasks.removeFirst().run();
        }

        assertEquals(MulticlipGroupCursorController.Discovery.NAMED, completed.get());
        verify(cursor).selectChannel(polySeq);
    }

    @Test
    void refusesToChooseBetweenMultipleNamedPolySeqGroups() {
        final ControllerHost host = mock(ControllerHost.class);
        final CursorTrack cursor = mock(CursorTrack.class, Mockito.RETURNS_DEEP_STUBS);
        final TrackBank tracks = mock(TrackBank.class, Mockito.RETURNS_DEEP_STUBS);
        final Track polySeqA = track("Poly Seq A", true);
        final Track polySeqB = track("polyseq b", true);
        when(tracks.getSizeOfBank()).thenReturn(2);
        when(tracks.itemCount().get()).thenReturn(2);
        when(tracks.scrollPosition().get()).thenReturn(0);
        when(tracks.getItemAt(0)).thenReturn(polySeqA);
        when(tracks.getItemAt(1)).thenReturn(polySeqB);
        final Deque<Runnable> tasks = scheduledTasks(host);
        final AtomicReference<MulticlipGroupCursorController.Discovery> completed =
                new AtomicReference<>();
        final MulticlipGroupCursorController controller =
                new MulticlipGroupCursorController(host, cursor, tracks);

        controller.findNamed(completed::set);
        while (!tasks.isEmpty()) {
            tasks.removeFirst().run();
        }

        assertEquals(MulticlipGroupCursorController.Discovery.MULTIPLE, completed.get());
        verify(cursor, never()).selectChannel(any());
    }

    @Test
    void pagesAcrossTheFlatProjectBankWithoutCountingOverlappingTracksTwice() {
        final ControllerHost host = mock(ControllerHost.class);
        final CursorTrack cursor = mock(CursorTrack.class, Mockito.RETURNS_DEEP_STUBS);
        final TrackBank tracks = mock(TrackBank.class, Mockito.RETURNS_DEEP_STUBS);
        final SettableIntegerValue scroll = mock(SettableIntegerValue.class);
        final AtomicInteger position = new AtomicInteger();
        final Track first = scrollingTrack(position, 0);
        final Track second = scrollingTrack(position, 1);
        when(tracks.getSizeOfBank()).thenReturn(2);
        when(tracks.itemCount().get()).thenReturn(3);
        when(tracks.scrollPosition()).thenReturn(scroll);
        when(scroll.get()).thenAnswer(ignored -> position.get());
        Mockito.doAnswer(
                        invocation -> {
                            final int requested = invocation.getArgument(0, Integer.class);
                            position.set(Math.min(requested, 1));
                            return null;
                        })
                .when(scroll)
                .set(Mockito.anyInt());
        when(tracks.getItemAt(0)).thenReturn(first);
        when(tracks.getItemAt(1)).thenReturn(second);
        when(cursor.exists().get()).thenReturn(true);
        when(cursor.isGroup().get()).thenReturn(true);
        when(cursor.name().get()).thenReturn("[PolySeq] Last");
        final Deque<Runnable> tasks = scheduledTasks(host);
        final AtomicReference<MulticlipGroupCursorController.Discovery> completed =
                new AtomicReference<>();
        final MulticlipGroupCursorController controller =
                new MulticlipGroupCursorController(host, cursor, tracks);

        controller.findNamed(completed::set);
        while (!tasks.isEmpty()) {
            tasks.removeFirst().run();
        }

        assertEquals(MulticlipGroupCursorController.Discovery.NAMED, completed.get());
        verify(cursor).selectChannel(second);
    }

    private static Track track(final String name, final boolean group) {
        final Track track = mock(Track.class, Mockito.RETURNS_DEEP_STUBS);
        when(track.exists().get()).thenReturn(!name.isEmpty());
        when(track.isGroup().get()).thenReturn(group);
        when(track.name().get()).thenReturn(name);
        return track;
    }

    private static Track scrollingTrack(final AtomicInteger bankPosition, final int bankIndex) {
        final Track track = mock(Track.class, Mockito.RETURNS_DEEP_STUBS);
        when(track.exists().get()).thenAnswer(ignored -> bankPosition.get() + bankIndex < 3);
        when(track.isGroup().get()).thenAnswer(ignored -> bankPosition.get() + bankIndex == 2);
        when(track.name().get())
                .thenAnswer(
                        ignored ->
                                bankPosition.get() + bankIndex == 2 ? "[PolySeq] Last" : "Track");
        return track;
    }

    private static Deque<Runnable> scheduledTasks(final ControllerHost host) {
        final Deque<Runnable> tasks = new ArrayDeque<>();
        Mockito.doAnswer(
                        invocation -> {
                            tasks.addLast(invocation.getArgument(0));
                            return null;
                        })
                .when(host)
                .scheduleTask(any(Runnable.class), anyLong());
        return tasks;
    }
}
