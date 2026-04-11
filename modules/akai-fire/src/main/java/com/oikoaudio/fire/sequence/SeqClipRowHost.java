package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.display.OledDisplay;

public interface SeqClipRowHost {
    AkaiFireOikontrolExtension getDriver();

    default int getClipCreateLengthBeats() {
        return getDriver().getDefaultClipLengthBeats();
    }

    OledDisplay getOled();

    ClipLauncherSlotBank getClipSlotBank();

    PinnableCursorClip getClipCursor();

    boolean isSelectHeld();

    boolean isCopyHeld();

    boolean isDeleteHeld();

    boolean isShiftHeld();

    void notifyPopup(String title, String value);
}
