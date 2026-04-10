package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extensions.framework.Layer;

public class SeqClipHandler {

    private final SeqClipRowHost host;
    private final PinnableCursorClip cursorClip;
    private int selectedSlotIndex = -1;
    private final ClipLauncherSlotBank slotBank;
    private final RgbLigthState[] slotColors = new RgbLigthState[16];
    private int blinkState = 0;

    public SeqClipHandler(final SeqClipRowHost host) {
        this.host = host;
        this.cursorClip = host.getClipCursor();
        this.slotBank = host.getClipSlotBank();
        initSlotObservers();
    }

    public void bindClipRow(final Layer clipLayer, final RgbButton[] rgbButtons) {
        for (int i = 0; i < 16; i++) {
            final RgbButton button = rgbButtons[i];
            final int index = i;
            final ClipLauncherSlot cs = slotBank.getItemAt(index);
            button.bindPressed(clipLayer, p -> handleClip(index, cs, p), () -> getClipSate(index, cs));
        }
    }

    private void initSlotObservers() {
        for (int i = 0; i < 16; i++) {
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

        }
    }

    public void handlePadPress(final int index, final boolean pressed) {
        if (index < 0 || index >= 16) {
            return;
        }
        handleClip(index, slotBank.getItemAt(index), pressed);
    }

    public RgbLigthState getPadLight(final int index) {
        if (index < 0 || index >= 16) {
            return RgbLigthState.OFF;
        }
        return getClipSate(index, slotBank.getItemAt(index));
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
        final boolean hasContent = slot.hasContent().get();
        switch (SeqClipRowActionResolver.resolve(pressed, hasContent,
                host.isDeleteHeld(), host.isCopyHeld(), host.isSelectHeld(), host.isShiftHeld(),
                selectedSlotIndex, index)) {
            case IGNORE -> {
            }
            case DELETE_OBJECT -> slot.deleteObject();
            case CLEAR_STEPS -> {
                final int previous = selectedSlotIndex;
                slot.select();
                cursorClip.clearSteps();
                if (previous != -1) {
                    slotBank.getItemAt(previous).select();
                }
            }
            case COPY_TO_TARGET -> {
                slot.replaceInsertionPoint().copySlotsOrScenes(slotBank.getItemAt(selectedSlotIndex));
                host.getOled().valueInfo("Copy Clip", "Select target");
                host.notifyPopup("Copy Clip", slotLabel(selectedSlotIndex) + " -> " + slotLabel(index));
            }
            case SELECT_ONLY -> slot.select();
            case CYCLE_COLOR -> slot.color().set(getSlotColor(slot));
            case SELECT_AND_LAUNCH -> {
                slot.select();
                slot.launch();
            }
            case CREATE_EMPTY -> slot.createEmptyClip(host.getDriver().getDefaultClipLengthBeats());
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
