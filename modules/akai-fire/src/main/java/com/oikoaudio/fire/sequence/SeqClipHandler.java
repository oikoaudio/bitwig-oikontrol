package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extensions.framework.Layer;

public class SeqClipHandler {

    private final DrumSequenceMode parent;
    private final PinnableCursorClip cursorClip;
    private int selectedSlotIndex = -1;
    private final ClipLauncherSlotBank slotBank;
    private final RgbLigthState[] slotColors = new RgbLigthState[16];
    private final CursorTrack cursorTrack;
    private int blinkState = 0;

    public SeqClipHandler(final AkaiFireOikontrolExtension driver, final DrumSequenceMode parent, final Layer clipLayer) {
        this.parent = parent;
        this.cursorClip = parent.getCursorClip();
        cursorTrack = driver.getViewControl().getCursorTrack();
        slotBank = cursorTrack.clipLauncherSlotBank();
        initClipControlButtons(clipLayer, driver);
    }

    private void initClipControlButtons(final Layer clipLayer, final AkaiFireOikontrolExtension driver) {
        final RgbButton[] rgbButtons = driver.getRgbButtons();
        for (int i = 0; i < 16; i++) {
            final RgbButton button = rgbButtons[i];
            final int index = i;

            final ClipLauncherSlot cs = slotBank.getItemAt(index);

            cs.color().addValueObserver((r, g, b) -> {
                slotColors[index] = ColorLookup.getColor(r, g, b);
            });
            cs.isSelected().addValueObserver(selected -> {
                if (selected) {
                    selectedSlotIndex = index;
                }
            });
            slotColors[index] = ColorLookup.getColor(cs.color().get());
            cs.exists().markInterested();
            cs.hasContent().markInterested();
            cs.isPlaybackQueued().markInterested();
            cs.isPlaying().markInterested();
            cs.isRecording().markInterested();
            cs.isRecordingQueued().markInterested();
            cs.isSelected().markInterested();
            cs.isStopQueued().markInterested();

            button.bindPressed(clipLayer, p -> handleClip(index, cs, p), () -> getClipSate(index, cs));
        }
    }

    private RgbLigthState getClipSate(final int index, final ClipLauncherSlot slot) {
        if (slot.hasContent().get()) {
            if (slotColors[index] == null) {
                return RgbLigthState.OFF;
            }
            final RgbLigthState color = slotColors[index];
            if (slot.isSelected().get()) {

                if (slot.isPlaying().get()) {
                    return blinkSlow(color.getBrightest(), color);
                }
                if (slot.isPlaybackQueued().get()) {
                    return blinkFast(color.getBrightest(), color.getDimmed());
                }
                return color.getBrightend();
            } else {
                if (slot.isPlaying().get()) {
                    return blinkSlow(color, color.getDimmed());
                }
                if (slot.isPlaybackQueued().get()) {
                    return blinkFast(color, color.getDimmed());
                }
                return color.getDimmed();
            }
        }
        return RgbLigthState.OFF;
    }

    private RgbLigthState blinkSlow(final RgbLigthState on, final RgbLigthState off) {
        if (blinkState % 8 < 4) {
            return on;
        }
        return off;
    }

    private RgbLigthState blinkFast(final RgbLigthState on, final RgbLigthState off) {
        if (blinkState % 2 == 0) {
            return on;
        }
        return off;
    }

    private void handleClip(final int index, final ClipLauncherSlot slot, final boolean pressed) {
        if (!pressed) {
            return;
        }
        final boolean hasContent = slot.hasContent().get();
        if (hasContent) {
            if (parent.isDeleteHeld()) {
                if (parent.isShiftHeld()) { // SHIFT + DELETE => remove clip
                    slot.deleteObject();
                } else { // SHIFT + DELETE => clear all steps
                    final int previous = selectedSlotIndex;
                    slot.select();
                    cursorClip.clearSteps();
                    if (previous != -1) {
                        slotBank.getItemAt(previous).select();
                    }
                }
            } else if (parent.isCopyHeld()) { // copies note
                if (selectedSlotIndex != -1 && selectedSlotIndex != index) {
                    slot.replaceInsertionPoint().copySlotsOrScenes(slotBank.getItemAt(selectedSlotIndex));
                    parent.getOled().valueInfo("Copy Clip", "Select target");
                    parent.notifyPopup("Copy Clip", slotLabel(selectedSlotIndex) + " -> " + slotLabel(index));
                }
            } else if (parent.isSelectHeld()) {
                slot.select();
            } else if (parent.isShiftHeld()) {
                slot.color().set(getSlotColor(slot));
            } else {
                slot.select();
                slot.launch();
            }
        } else {
            if (parent.isCopyHeld()) {
                if (selectedSlotIndex != -1 && selectedSlotIndex != index) {
                    slot.replaceInsertionPoint().copySlotsOrScenes(slotBank.getItemAt(selectedSlotIndex));
                    parent.getOled().valueInfo("Copy Clip", "Select target");
                    parent.notifyPopup("Copy Clip", slotLabel(selectedSlotIndex) + " -> " + slotLabel(index));
                }
            } else {
                slot.createEmptyClip(4);
            }
        }
    }

    private String slotLabel(final int index) {
        return "Clip " + (index + 1);
    }

    public void notifyBlink(final int blinkState) {
        this.blinkState = blinkState;
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
