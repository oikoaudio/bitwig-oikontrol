package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.AkaiFireDrumSeqExtension;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extensions.framework.Layer;

public class PadRowMuteHandler {

    private final DrumSequenceMode parent;
    private final DrumPadBank padBank;
    private final RgbLigthState[] slotColors = new RgbLigthState[16];

    public PadRowMuteHandler(final AkaiFireDrumSeqExtension driver, final DrumSequenceMode parent, final Layer muteLayer) {
        this.parent = parent;
        padBank = driver.getViewControl().getDrumPadBank();
        final RgbButton[] rgbButtons = driver.getRgbButtons();

        // Initialize slotColors[0] to the red color ("#d92e24")
        slotColors[0] = ColorLookup.getColor(Color.fromHex("#d92e24"));

        // Using the first 16 buttons for mute handling (i.e. the second row: indices 16-31)
        for (int i = 0; i < 16; i++) {
            final RgbButton button = rgbButtons[i + 16];
            final DrumPad drumPad = padBank.getItemAt(i);

            // Bind the mute action to the pad: toggle mute and notify the parent.
            button.bind(muteLayer, () -> {
                drumPad.mute().toggle();
                parent.notifyMuteAction();
            });

            // Bind the light state to reflect the mute status.
            button.bindLight(muteLayer, () ->
                    drumPad.mute().get() ? slotColors[0] : RgbLigthState.OFF
            );
        }
    }

    private Color getSlotColor(ClipLauncherSlot slot) {
        Color[] colors = {
                Color.fromHex("#d92e24"), // red
                Color.fromHex("#ff5706"), // orange
                Color.fromHex("#44c8ff"), // dark blue
                Color.fromHex("#0099d9"), // light blue
                Color.fromHex("#009d47"), // dark green
                Color.fromHex("#3ebb62"), // light green
                Color.fromHex("#d99d10"), // yellow
                Color.fromHex("#c9c9c9"), // white
                Color.fromHex("#5761c6"), // dark purple
                Color.fromHex("#bc76f0"), // light purple
        };
        Color currentColor = slot.color().get();

        int colorIndex = 0;
        for (int i = 0; i < colors.length; i++) {
            if (colors[i].toHex().equals(currentColor.toHex())) {
                colorIndex = i + 1;
            }
        }
        if (colorIndex == colors.length) {
            colorIndex = 0;
        }
        return colors[colorIndex];
    }
}
