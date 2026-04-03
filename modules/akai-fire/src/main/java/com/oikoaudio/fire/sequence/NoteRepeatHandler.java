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

	private final OledDisplay oled;
	private final BooleanValueObject noteRepeatActive = new BooleanValueObject();
	private boolean repeatButtonHeld = false;
	private double currentArpRate = ARP_RATES[1];
	private static final double[] ARP_RATES = new double[] { 0.125, 0.25, 0.5, 1.0, 1.0 / 12, 1.0 / 6, 1.0 / 3,
			2.0 / 3 };
	private static final String[] GRID_RATES_STR = new String[] { "1/32", "1/16", "1/8", "1/4", //
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
		this.selectedArpIndex = 1;
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
		if (pressed) {
			oled.valueInfo("Note Repeat",
					noteRepeatActive.get() ? GRID_RATES_STR[selectedArpIndex] : "Off");
		} else {
			noteRepeatActive.toggle();
			if (noteRepeatActive.get()) {
				setNoteInputVelocity(velocitySupplier.getAsInt());
				oled.valueInfo("Note Repeat", GRID_RATES_STR[selectedArpIndex]);
			} else {
				oled.valueInfo("Note Repeat", "Off");
			}
		}
		repeatButtonHeld = pressed;
	}

	boolean isHolding() {
		return repeatButtonHeld;
	}

	public BooleanValueObject getNoteRepeatActive() {
		return noteRepeatActive;
	}

	//BUG Pagename is not updated correctly

	public void handleMainEncoder(final int inc, final boolean altHeld) {
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
			// Regular behavior...
			final int newValue = selectedArpIndex + inc;
			if (newValue >= 0 && newValue < ARP_RATES.length) {
				selectedArpIndex = newValue;
				setNoteRateValue(newValue);
			}
		}
	}



	private void setNoteRateValue(final int index) {
		this.selectedArpIndex = index;
		this.currentArpRate = ARP_RATES[index];
		arp.rate().set(currentArpRate);
		oled.valueInfo("Note Repeat", GRID_RATES_STR[index]);
	}

	public void activate() {
		setNoteInputVelocity(velocitySupplier.getAsInt());
		arp.isEnabled().set(true);
		arp.mode().set("all"); // that's the note repeat way
		arp.octaves().set(0);
		arp.humanize().set(0);
		arp.isFreeRunning().set(false);
		arp.rate().set(currentArpRate);
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
