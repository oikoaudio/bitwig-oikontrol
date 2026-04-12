package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.lights.BiColorLightState;
import com.bitwig.extension.controller.api.NoteStep;

public class AccentHandler {
	private final AccentButtonModel model = new AccentButtonModel();
	private final DrumSequenceMode parent;

	public AccentHandler(final DrumSequenceMode drumSequenceMode) {
		this.parent = drumSequenceMode;
	}

	public int getCurrenVel() {
		return model.currentVelocity(parent.getDefaultVelocity());
	}

	public int getStandardVelocity() {
		return parent.getDefaultVelocity();
	}

	public int getAccentedVelocity() {
		return model.accentedVelocity();
	}

	public boolean isAccented(final NoteStep noteStep) {
		final int velocity = (int) Math.round(noteStep.velocity() * 127);
		final int distanceToAccent = Math.abs(velocity - getAccentedVelocity());
		final int distanceToStandard = Math.abs(velocity - getStandardVelocity());
		return distanceToAccent <= distanceToStandard;
	}

	public void markModified() {
		model.markModified();
	}

	BiColorLightState getLightState() {
		return model.lightState();
	}

	public boolean isHolding() {
		return model.isHolding();
	}

	public boolean isActive() {
		return model.isActive();
	}

	void handlePressed(final boolean pressed) {
		final AccentLatchState.Transition transition = model.handlePressed(pressed);
		if (transition == AccentLatchState.Transition.PRESSED) {
			displayAccentInfo();
			return;
		}
		if (transition == AccentLatchState.Transition.TOGGLED_ON_RELEASE) {
			this.parent.getDrumPadHandler().getNoteRepeaterHandler().setNoteInputVelocity(this.getCurrenVel());
			displayAccentInfo();
			return;
		}
		parent.getOled().clearScreenDelayed();
	}

	private void displayAccentInfo() {
		parent.getOled().valueInfo("Accent", model.label());
	}

}
