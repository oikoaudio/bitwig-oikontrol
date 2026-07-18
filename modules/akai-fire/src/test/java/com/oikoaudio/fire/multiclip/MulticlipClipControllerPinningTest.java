package com.oikoaudio.fire.multiclip;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.Track;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

class MulticlipClipControllerPinningTest {
    @Test
    void retargetsEachIndependentClipViewBeforeMakingTheLaneReady() {
        final ControllerHost host = mock(ControllerHost.class);
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
            when(cursors[row].createLauncherCursorClip(anyString(), anyString(), eq(4096), eq(1)))
                    .thenReturn(fineClips[row]);
        }
        when(host.createCursorTrack(anyString(), anyString(), eq(0), eq(16), anyBoolean()))
                .thenReturn(cursors[0])
                .thenReturn(cursors[1])
                .thenReturn(cursors[2])
                .thenReturn(cursors[3]);

        final MulticlipClipController controller = new MulticlipClipController(host);
        clearInvocations(clips[0].isPinned(), fineClips[0].isPinned(), cursors[0]);
        final Track track = mock(Track.class);
        controller.retarget(0, track, TrackLaneMapping.fromChildPosition(0), 0, 2);

        final SettableBooleanValue clipPinned = clips[0].isPinned();
        final SettableBooleanValue fineClipPinned = fineClips[0].isPinned();
        final InOrder order = inOrder(clipPinned, fineClipPinned, cursors[0]);
        order.verify(clipPinned).set(false);
        order.verify(fineClipPinned).set(false);
        order.verify(cursors[0]).selectSlot(2);
        order.verify(clipPinned).set(true);
        order.verify(fineClipPinned).set(true);
    }
}
