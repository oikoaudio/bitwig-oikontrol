package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.lights.BiColorLightState;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class AccentHandler {
	private static final int ACCENT_OFFSET = 11;
	private int velStandard = 100;
	private int velAccented = 127;
	private final BooleanValueObject accentActive = new BooleanValueObject();
	private boolean accenButtonHeld = false;
	private boolean modified = false;
	private final DrumSequenceMode parent;
	private int velPointer = 0;

	public AccentHandler(final DrumSequenceMode drumSequenceMode) {
		this.parent = drumSequenceMode;
	}

	public int getCurrenVel() {
		return accentActive.get() ? velAccented : velStandard;
	}

	public int getStandardVelocity() {
		return velStandard;
	}

	public int getAccentedVelocity() {
		return velAccented;
	}

	public boolean isAccented(final NoteStep noteStep) {
		final int velocity = (int) Math.round(noteStep.velocity() * 127);
		final int distanceToAccent = Math.abs(velocity - velAccented);
		final int distanceToStandard = Math.abs(velocity - velStandard);
		return distanceToAccent <= distanceToStandard;
	}

	public void markModified() {
		modified = true;
	}

	BiColorLightState getLightState() {
		return accentActive.get() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
	}

	public boolean isHolding() {
		return accenButtonHeld;
	}

	void handlePressed(final boolean pressed) {
		if (!pressed) {
			if (!modified) {
				accentActive.toggle();
				this.parent.getPadHandler().getNoteRepeaterHandler().setNoteInputVelocity(this.getCurrenVel());
			}
			parent.getOled().clearScreenDelayed();
			modified = false;
		} else {
			displayAccentInfo();
		}
		accenButtonHeld = pressed;
	}

	private void displayAccentInfo() {
		parent.getOled().lineInfo("Accents", //
				String.format("%sNormal: %d\n%sAccent: %d", velPointer == 0 ? ">" : " ", velStandard, //
						velPointer == 1 ? ">" : " ", velAccented));
	}

	void handleMainEncoder(final int inc) {
		if (!accenButtonHeld) {
			return;
		}
		if (velPointer == 0) {
			final int newValue = velStandard + inc;
			if (newValue > 0 && newValue < velAccented - ACCENT_OFFSET) {
				velStandard = newValue;
				displayAccentInfo();
			}
		} else if (velPointer == 1) {
			final int newValue = velAccented + inc;
			if (newValue > velStandard + ACCENT_OFFSET && newValue < 128) {
				velAccented = newValue;
				displayAccentInfo();
			}
		}
		modified = true;
		this.parent.getPadHandler().getNoteRepeaterHandler().setNoteInputVelocity(this.getCurrenVel());
	}

	void handeMainEncoderPress(final boolean pressed) {
		if (!accenButtonHeld || !pressed) {
			return;
		}
		velPointer = (velPointer + 1) % 2;
		displayAccentInfo();
	}

}
