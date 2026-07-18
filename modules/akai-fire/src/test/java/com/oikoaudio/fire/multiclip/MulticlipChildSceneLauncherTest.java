package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import java.util.List;
import org.junit.jupiter.api.Test;

class MulticlipChildSceneLauncherTest {
    @Test
    void launchesOnlyTheExplicitChildSlotsSuppliedByTheLaneBank() {
        final ClipLauncherSlot childOne = mock(ClipLauncherSlot.class);
        final ClipLauncherSlot childTwo = mock(ClipLauncherSlot.class);

        assertEquals(2, MulticlipChildSceneLauncher.launch(List.of(childOne, childTwo)));

        verify(childOne).launch();
        verify(childTwo).launch();
    }
}
