package com.oikoaudio.fire.multiclip;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import org.junit.jupiter.api.Test;

class MulticlipChildClipCreatorTest {
    @Test
    void createsOnlyTheExactSlotInTheSuppliedChildBank() {
        final ClipLauncherSlotBank childSlots = mock(ClipLauncherSlotBank.class);

        MulticlipChildClipCreator.create(childSlots, 3, 4);

        verify(childSlots).createEmptyClip(3, 4);
    }
}
