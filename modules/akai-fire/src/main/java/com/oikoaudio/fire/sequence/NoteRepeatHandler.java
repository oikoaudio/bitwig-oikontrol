package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.framework.values.BooleanValueObject;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class NoteRepeatHandler {
    private static final int DEFAULT_TOGGLE_RATE_INDEX = 2;

	private final OledDisplay oled;
	private final BooleanValueObject noteRepeatActive = new BooleanValueObject();
	private boolean repeatButtonHeld = false;
	private static final double[] ARP_RATES = new double[] { 0.125, 0.25, 0.5, 1.0, 1.0 / 12, 1.0 / 6, 1.0 / 3,
			2.0 / 3 };
	private static final String[] GRID_RATES_STR = new String[] { "Off", "1/32", "1/16", "1/8", "1/4", //
			"1/32T", "1/16T", "1/8T", "1/4T" };
	private final Arpeggiator arp;
	private final NoteInput noteInput;
	private final Supplier<CursorRemoteControlsPage> remotePageSupplier;
	private final IntSupplier velocitySupplier;
	private int selectedArpIndex;

	public NoteRepeatHandler(final NoteInput noteInput,
			final OledDisplay oled,
			final Supplier<CursorRemoteControlsPage> remotePageSupplier,
			final IntSupplier velocitySupplier) {
		this.oled = oled;
		this.noteInput = noteInput;
		this.remotePageSupplier = remotePageSupplier;
		this.velocitySupplier = velocitySupplier;
		this.setNoteInputVelocity(velocitySupplier.getAsInt());
		this.selectedArpIndex = 0;
		arp = noteInput.arpeggiator();
		arp.usePressureToVelocity().set(true);
		// arp.shuffle().set(true);
		arp.mode().set("all"); // that's the note repeat way
		arp.octaves().set(0);
		arp.humanize().set(0);
		arp.isFreeRunning().set(false);
		arp.isEnabled().set(false);
		noteRepeatActive.set(false);
	}

	BiColorLightState getLightState() {
		return noteRepeatActive.get() ? BiColorLightState.HALF : BiColorLightState.OFF;
	}

	public void handlePressed(final boolean pressed) {
		repeatButtonHeld = pressed;
		if (pressed) {
			oled.valueInfo("Note Repeat", GRID_RATES_STR[selectedArpIndex]);
		} else {
			oled.clearScreenDelayed();
		}
	}

	boolean isHolding() {
		return repeatButtonHeld;
	}

	public BooleanValueObject getNoteRepeatActive() {
		return noteRepeatActive;
	}

    public void toggleActive() {
        if (noteRepeatActive.get()) {
            setNoteRateValue(0);
            return;
        }
        setNoteRateValue(selectedArpIndex == 0 ? DEFAULT_TOGGLE_RATE_INDEX : selectedArpIndex);
    }

	//BUG Pagename is not updated correctly

	public void handleMainEncoder(final int inc, final boolean altHeld) {
		handleMainEncoder(inc, altHeld, true);
	}

	public void handleMainEncoder(final int inc, final boolean altHeld, final boolean allowOff) {
		if (altHeld) {
			CursorRemoteControlsPage remotePage = remotePageSupplier.get();
			if (remotePage != null) {
				int pageCount = remotePage.pageCount().getAsInt();
				int currentPage = remotePage.selectedPageIndex().get();

				int newPage = currentPage + inc;
				if (newPage >= 0 && newPage < pageCount) {
					remotePage.selectedPageIndex().set(newPage);
					String pageName = remotePage.pageNames().get(newPage);
					oled.valueInfo("Remote Page", pageName);
				}
			}
		} else {
			final int minValue = allowOff ? 0 : 1;
			final int newValue = selectedArpIndex + inc;
			if (newValue >= minValue && newValue < GRID_RATES_STR.length) {
				setNoteRateValue(newValue);
			}
		}
	}



	private void setNoteRateValue(final int index) {
		this.selectedArpIndex = index;
		if (index == 0) {
			noteRepeatActive.set(false);
			arp.isEnabled().set(false);
			oled.valueInfo("Note Repeat", "Off");
			return;
		}
		noteRepeatActive.set(true);
		setNoteInputVelocity(velocitySupplier.getAsInt());
		arp.isEnabled().set(true);
		arp.rate().set(ARP_RATES[index - 1]);
		oled.valueInfo("Note Repeat", GRID_RATES_STR[index]);
	}

	public void activate() {
		arp.mode().set("all"); // that's the note repeat way
		arp.octaves().set(0);
		arp.humanize().set(0);
		arp.isFreeRunning().set(false);
		if (noteRepeatActive.get() && selectedArpIndex > 0) {
			setNoteInputVelocity(velocitySupplier.getAsInt());
			arp.isEnabled().set(true);
			arp.rate().set(ARP_RATES[selectedArpIndex - 1]);
		} else {
			arp.isEnabled().set(false);
		}
	}

	public void deactivate() {
		noteRepeatActive.set(false);
		repeatButtonHeld = false;
		arp.isEnabled().set(false);
	}

	public void setNoteInputVelocity(final int velocity) {
		// NOTE: note repeat velocity
		final Integer[] notesToDrumTable = new Integer[128];
		for (int i = 0; i < notesToDrumTable.length; i++) {
			notesToDrumTable[i] = velocity;
		}
		noteInput.setVelocityTranslationTable(notesToDrumTable);
	}
}
