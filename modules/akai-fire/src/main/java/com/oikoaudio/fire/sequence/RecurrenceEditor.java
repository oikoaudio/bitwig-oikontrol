package com.oikoaudio.fire.sequence;

import java.util.List;

import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.control.PadMatrixBindings;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.framework.Layer;

/**
 * Temporary editing layer for note recurrence masks in drum sequencing mode.
 * It owns the pad overlay used while one or more held steps are being edited.
 *
 * <p>This is a legacy encoder-entered editor path. The current direct recurrence workflow for step/pulse modes is the
 * hold-step recurrence row backed by {@link RecurrencePadInteraction}; Drum XOX no longer exposes a recurrence encoder
 * in its encoder banks. Keep this class only while older entry points or compatibility needs remain.</p>
 */
public class RecurrenceEditor {
	private List<NoteStep> editedSteps;
	private int currentMask = 0;
	private int currentLen = 1;
	private final Layer layer;
	private final RecurrencePadInteraction recurrencePads = new RecurrencePadInteraction(true, 0L);
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
		PadMatrixBindings.bindPressed(clipLayer, driver.getRgbButtons(), 16, 16,
				new PadMatrixBindings.PressHost() {
					@Override
					public void handlePadPress(final int padIndex, final boolean pressed) {
						handleMask(pressed, padIndex);
					}

					@Override
					public RgbLigthState padLight(final int padIndex) {
						return getState(padIndex, 0x1 << padIndex);
					}
				});
	}

	private RgbLigthState getState(final int index, final int mask) {
		if (editedSteps == null) {
			return RgbLigthState.OFF;
		}
		return recurrencePads.padLight(index, RecurrencePattern.of(currentLen, currentMask), RgbLigthState.PURPLE);
	}

	private void handleMask(final boolean pressed, final int index) {
		final boolean hasTarget = editedSteps != null;
		final RecurrencePattern recurrence = RecurrencePattern.of(currentLen, currentMask);
		recurrencePads.handlePadPress(index, pressed, hasTarget, recurrence,
				() -> { }, this::toggleMask, this::applySpan);
	}

	private void toggleMask(final int index) {
		if (editedSteps == null || index < 0 || index >= currentLen) {
			return;
		}
		final int mask = 0x1 << index;
		if ((mask & currentMask) != 0) {
			currentMask &= ~mask;
		} else {
			currentMask |= mask;
		}
		final NoteStep note = editedSteps.get(0);
		note.setRecurrence(currentLen, currentMask);

	}

	private void applySpan(final int span) {
		if (editedSteps == null) {
			return;
		}
		final RecurrencePattern updated = RecurrencePattern.of(currentLen, currentMask).applySpanGesture(span);
		currentLen = updated.bitwigLength();
		currentMask = updated.bitwigMask();
		final NoteStep note = editedSteps.get(0);
		note.setRecurrence(currentLen, currentMask);
	}

	public void exitRecurrenceEdit() {
		pendingRelease = true;
		recurrencePads.clearHold();
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
