package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MulticlipChildCopyControllerTest {
    @Test
    void copiesTheSelectedLaneClipDirectlyIntoItsTargetScene() {
        final ClipLauncherSlot source = mock(ClipLauncherSlot.class, Mockito.RETURNS_DEEP_STUBS);
        final ClipLauncherSlot target = mock(ClipLauncherSlot.class, Mockito.RETURNS_DEEP_STUBS);
        when(source.hasContent().get()).thenReturn(true);

        assertTrue(MulticlipChildCopyController.copyClip(source, target));

        verify(target.replaceInsertionPoint()).copySlotsOrScenes(source);
    }

    @Test
    void doesNotInventAClipWhenTheSelectedLaneSlotIsEmpty() {
        final ClipLauncherSlot source = mock(ClipLauncherSlot.class, Mockito.RETURNS_DEEP_STUBS);
        final ClipLauncherSlot target = mock(ClipLauncherSlot.class, Mockito.RETURNS_DEEP_STUBS);
        when(source.hasContent().get()).thenReturn(false);

        assertFalse(MulticlipChildCopyController.copyClip(source, target));

        verify(target.replaceInsertionPoint(), never()).copySlotsOrScenes(source);
    }

    @Test
    void copiesAnExactChildOnlySceneSnapshot() {
        final ClipLauncherSlot sourceOne = mock(ClipLauncherSlot.class, Mockito.RETURNS_DEEP_STUBS);
        final ClipLauncherSlot targetOne = mock(ClipLauncherSlot.class, Mockito.RETURNS_DEEP_STUBS);
        final ClipLauncherSlot sourceTwo = mock(ClipLauncherSlot.class, Mockito.RETURNS_DEEP_STUBS);
        final ClipLauncherSlot targetTwo = mock(ClipLauncherSlot.class, Mockito.RETURNS_DEEP_STUBS);
        when(sourceOne.hasContent().get()).thenReturn(true);
        when(sourceTwo.hasContent().get()).thenReturn(false);
        when(targetTwo.hasContent().get()).thenReturn(true);

        assertEquals(
                2,
                MulticlipChildCopyController.copyScene(
                        List.of(
                                new MulticlipChildCopyController.SlotPair(sourceOne, targetOne),
                                new MulticlipChildCopyController.SlotPair(sourceTwo, targetTwo))));

        verify(targetOne.replaceInsertionPoint()).copySlotsOrScenes(sourceOne);
        verify(targetTwo).deleteObject();
    }
}
