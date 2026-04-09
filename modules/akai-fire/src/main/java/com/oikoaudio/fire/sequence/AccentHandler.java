package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.lights.BiColorLightState;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class AccentHandler {
	private static final int ACCENTED_VELOCITY = 127;
	private final BooleanValueObject accentActive = new BooleanValueObject();
	private boolean accenButtonHeld = false;
	private boolean modified = false;
	private final DrumSequenceMode parent;

	public AccentHandler(final DrumSequenceMode drumSequenceMode) {
		this.parent = drumSequenceMode;
	}

	public int getCurrenVel() {
		return accentActive.get() ? ACCENTED_VELOCITY : parent.getDefaultVelocity();
	}

	public int getStandardVelocity() {
		return parent.getDefaultVelocity();
	}

	public int getAccentedVelocity() {
		return ACCENTED_VELOCITY;
	}

	public boolean isAccented(final NoteStep noteStep) {
		final int velocity = (int) Math.round(noteStep.velocity() * 127);
		final int distanceToAccent = Math.abs(velocity - getAccentedVelocity());
		final int distanceToStandard = Math.abs(velocity - getStandardVelocity());
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
		parent.getOled().valueInfo("Accent", accentActive.get() ? "On" : "Off");
	}

}
