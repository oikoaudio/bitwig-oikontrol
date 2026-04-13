package com.oikoaudio.fire.sequence;

import java.util.List;

import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.framework.Layer;

/**
 * Temporary editing layer for note recurrence masks in drum sequencing mode.
 * It owns the pad overlay used while one or more held steps are being edited.
 */
public class RecurrenceEditor {
	private List<NoteStep> editedSteps;
	private int currentMask = 0;
	private int currentLen = 1;
	private final Layer layer;
	private boolean pendingRelease;

	public RecurrenceEditor(final AkaiFireOikontrolExtension driver, final DrumSequenceMode parent) {
		layer = new Layer(driver.getLayers(), parent.getName() + "_RECURRENCE");
		initClipControlButtons(layer, driver);
		final IntSetValue heldSteps = parent.getHeldSteps();
		heldSteps.addSizeValueListener(stepsHeld -> finalizeEdit(stepsHeld));
	}

	private void finalizeEdit(final int stepsHeld) {
		if (pendingRelease && stepsHeld == 0) {
			pendingRelease = false;
			editedSteps = null;
			layer.deactivate();
		}
	}

	private void initClipControlButtons(final Layer clipLayer, final AkaiFireOikontrolExtension driver) {
		final RgbButton[] rgbButtons = driver.getRgbButtons();
		for (int i = 0; i < 16; i++) {
			final RgbButton button = rgbButtons[i + 16];
			final int index = i;
			final int mask = 0x1 << i;
			button.bindPressed(clipLayer, p -> handleMask(p, index, mask), () -> getState(index, mask));
		}
	}

	private RgbLigthState getState(final int index, final int mask) {
		if (index < currentLen && currentLen > 1) {
			if ((mask & currentMask) != 0) {
				return RgbLigthState.PURPLE;
			}
			return RgbLigthState.GRAY_1;
		}
		return RgbLigthState.OFF;
	}

	private void handleMask(final boolean pressed, final int index, final int mask) {
		if (!pressed || index >= currentLen) {
			return;
		}
		if ((mask & currentMask) != 0) {
			currentMask &= ~mask;
		} else {
			currentMask |= mask;
		}
		final NoteStep note = editedSteps.get(0);
		note.setRecurrence(currentLen, currentMask);

	}

	public void exitRecurrenceEdit() {
		pendingRelease = true;
	}

	public void enterRecurrenceEdit(final List<NoteStep> notes) {
		layer.activate();
		editedSteps = notes;
		if (notes.isEmpty()) {
			return;
		}
		final NoteStep note = notes.get(0);
		currentMask = note.recurrenceMask();
		currentLen = note.recurrenceLength();
	}

	public void updateLength(final int newLength) {
		currentLen = newLength;
	}

}
