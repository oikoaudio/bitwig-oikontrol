package com.oikoaudio.fire.sequence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oikoaudio.fire.AkaiFireDrumSeqExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.utils.PatternButtons;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.NoteOccurrence;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.framework.Layer;

public class SequencEncoderHandler extends Layer {

private final static String[] mixerParamNames = { "Volume", "Panning", "Send 1", "Send 2" };
private final static String[] user2ParamNames = { "LEN", "PULS", "ROT", "INV/PULS" };
private final static String[] user2ShiftParamNames = { "LEN", "PULS", "ROT", "INV/PULS" };

	private final DrumSequenceMode parent;

	private EncoderMode encoderMode = EncoderMode.CHANNEL;
	private final Layer channelLayer;
	private final Layer mixerLayer;
	private final Layer mixerShiftLayer;
	private final Layer user1Layer;
	private final Layer user2Layer;
	private final Layer user2ShiftLayer;
	private EuclidAdjuster euclidAdjuster;

	private Layer currentLayer;
	private final OledDisplay oled;
	private final Map<EncoderMode, Layer> modeMapping = new HashMap<>();
	private final TouchEncoder[] encoders;

	private final PadHandler padHandler;

	@FunctionalInterface
	interface NoteDoubleGetter {
		double get(NoteStep step);
	}

	@FunctionalInterface
	interface NoteDoubleSetter {
		void set(NoteStep step, double value);
	}

	@FunctionalInterface
	interface NoteIntGetter {
		int get(NoteStep step);
	}

	@FunctionalInterface
	interface NoteIntSetter {
		void set(NoteStep step, int value);
	}

	public SequencEncoderHandler(final DrumSequenceMode drumMode, final AkaiFireDrumSeqExtension driver,
                                 final PadHandler padHandler) {
		super(driver.getLayers(), "Encoder_layer");
		this.parent = drumMode;
        this.oled = driver.getOled();
		this.padHandler = padHandler;
		channelLayer = new Layer(driver.getLayers(), "ENC_CHANNEL_LAYER");
		mixerLayer = new Layer(driver.getLayers(), "ENC_MIXER_LAYER");
		mixerShiftLayer = new Layer(driver.getLayers(), "ENC_SHIFT_MIXER_LAYER");
		user1Layer = new Layer(driver.getLayers(), "ENC_USER1_LAYER");
		user2Layer = new Layer(driver.getLayers(), "ENC_USER2_LAYER");
		user2ShiftLayer = new Layer(driver.getLayers(), "ENC_USER2SHIFT_LAYER");
		encoders = driver.getEncoders();
		assign(EncoderMode.CHANNEL, channelLayer, encoders);
		assignPadParams(EncoderMode.MIXER, mixerLayer, encoders);
		assign(EncoderMode.MIXER_SHIFT, mixerShiftLayer, encoders);
		assign(EncoderMode.USER_1, user1Layer, encoders);
		assignUser2Params(EncoderMode.USER_2, user2Layer, encoders);
		assignUser2ShiftParams(EncoderMode.USER_2_SHIFT, user2ShiftLayer, encoders);
		currentLayer = channelLayer;
		final BiColorButton modeButon = driver.getButton(NoteAssign.KNOB_MODE);
		modeButon.bindPressed(this, this::handleModeAdvance, this::modeToLight);

	}


	private void assign(final EncoderMode mode, final Layer layer, final TouchEncoder[] encoders) {
		modeMapping.put(mode, layer);
		final EncoderAccess[] assignments = mode.getAssignments();
		for (int i = 0; i < assignments.length; i++) {
			if (assignments[i] instanceof NoteStepAccess) {
				bindEncoder(layer, encoders[i], (NoteStepAccess) assignments[i]);
			}
		}
	}

	private void assignPadParams(final EncoderMode mode, final Layer layer, final TouchEncoder[] encoders) {
		modeMapping.put(mode, layer);
		for (int i = 0; i < encoders.length; i++) {
			bindPadEncoder(i, layer, encoders[i], mixerParamNames[i]);
		}
	}

	private void assignUser2Params(final EncoderMode mode, final Layer layer, final TouchEncoder[] encoders) {
		modeMapping.put(mode, layer);
		for (int i = 0; i < encoders.length; i++) {
			final int index = i;
			encoders[i].setStepSize(0.1);
			encoders[i].bindEncoder(layer, inc -> handleEuclid(index, inc));
		}
	}

	private void assignUser2ShiftParams(final EncoderMode mode, final Layer layer, final TouchEncoder[] encoders) {
		modeMapping.put(mode, layer);
		for (int i = 0; i < encoders.length; i++) {
			final int index = i;
			encoders[i].setStepSize(0.1);
			encoders[i].bindEncoder(layer, inc -> handleEuclidShift(index, inc));
		}
	}

	public EncoderMode nextMode() {
		if (encoderMode == EncoderMode.CHANNEL) {
			return EncoderMode.MIXER;
		} else if (encoderMode == EncoderMode.MIXER || encoderMode == EncoderMode.MIXER_SHIFT) {
			return EncoderMode.USER_1;
		} else if (encoderMode == EncoderMode.USER_1) {
			return EncoderMode.USER_2;
		}
		return EncoderMode.CHANNEL;
	}

	public void toggleShiftForCurrentMode() {
		switch (encoderMode) {
			case MIXER:
				switchMode(EncoderMode.MIXER_SHIFT);
				break;
			case MIXER_SHIFT:
				switchMode(EncoderMode.MIXER);
				break;
			case USER_2:
				switchMode(EncoderMode.USER_2_SHIFT);
				break;
			case USER_2_SHIFT:
				switchMode(EncoderMode.USER_2);
				break;
			default:
				// For modes that don't have a shift variant, do nothing or add custom behavior
				break;
		}
	}

	private void handleEuclid(final int index, final int inc) {
		if (euclidAdjuster != null) {
			euclidAdjuster.adjust(index, inc);
		}
	}

	private void handleEuclidShift(final int index, final int inc) {
		if (euclidAdjuster != null) {
			euclidAdjuster.adjustShift(index, inc);
		}
	}

	public interface EuclidAdjuster {
		void adjust(int index, int inc);
		void adjustShift(int index, int inc);
	}

	public void setEuclidAdjuster(EuclidAdjuster adjuster) {
		this.euclidAdjuster = adjuster;
	}


	private void bindEncoder(final Layer layer, final TouchEncoder encoder, final NoteStepAccess access) {
		encoder.bindEncoder(layer, inc -> handleMod(inc, access));
		encoder.bindTouched(layer, touched -> handleTouch(touched, access));
	}

	private void bindPadEncoder(final int index, final Layer layer, final TouchEncoder encoder,
			final String parameterName) {
		encoder.bindEncoder(layer, inc -> handleParam(index, inc));
		encoder.bindTouched(layer, touched -> handleTouchParam(index, touched, parameterName));
		padHandler.bindPadParameters(layer);
	}

	private void handleTouchParam(final int index, final Boolean touched, final String parameterName) {
		if (touched) {
			padHandler.activateView(index, parameterName);
			padHandler.updateDisplay(index);
		} else {
			padHandler.deactivateView();
		}
	}

	private void handleParam(final int index, final int inc) {
		padHandler.modifyValue(index, inc);
	}

	private void handleModeAdvance(final boolean pressed) {
		if (!pressed) {
			oled.clearScreenDelayed();
			return;
		}
		if (parent.isSelectHeld()) { // display encoder details on select + mode
			String infoToDisplay;
			// For USER_2 modes, get the dynamic info from the remote controls page.
			if (encoderMode == EncoderMode.USER_2 || encoderMode == EncoderMode.USER_2_SHIFT) {
				CursorRemoteControlsPage remotePage = parent.getActiveRemoteControlsPage();
				infoToDisplay = encoderMode.getDynamicInfo(remotePage);
			} else {
				infoToDisplay = encoderMode.getInfo();
			}
			oled.detailInfo("Encoder Mode", infoToDisplay);
		} else {
			switchMode(nextMode());
		}
	}


	private void switchMode(final EncoderMode newMode) {
		encoderMode = newMode;
		currentLayer.deactivate();
		currentLayer = modeMapping.get(encoderMode);
		currentLayer.activate();
		applyResolution(encoderMode);

		String infoToDisplay;
		if (encoderMode == EncoderMode.USER_2 || encoderMode == EncoderMode.USER_2_SHIFT) {
			// Fetch dynamic parameter names from the active remote controls page.
			CursorRemoteControlsPage remotePage = parent.getActiveRemoteControlsPage();
			infoToDisplay = encoderMode.getDynamicInfo(remotePage);
		} else {
			infoToDisplay = encoderMode.getInfo();
		}
		oled.detailInfo("Encoder Mode", infoToDisplay);
		oled.clearScreenDelayed();
	}


	private void applyResolution(final EncoderMode mode) {
		final EncoderAccess[] assignments = mode.getAssignments();
		for (int i = 0; i < assignments.length; i++) {
			encoders[i].setStepSize(assignments[i].getResolution());
		}
		if (assignments.length == 0) {
			for (int i = 0; i < encoders.length; i++) {
				encoders[i].setStepSize(0.25);
			}
		}
	}

	private BiColorLightState modeToLight() {
		return encoderMode.getState();
	}

    public EncoderMode getEncoderMode() {
        return encoderMode;
    }

	private void handleMod(final int inc, final NoteStepAccess accessor) {
		final List<NoteStep> notes = parent.isPadBeingHeld() ? parent.getOnNotes() : parent.getHeldNotes();
		if (notes.isEmpty()) {
			return;
		}
		for (int i = 0; i < notes.size(); i++) {
			final NoteStep note = notes.get(i);

			final String function = parent.isPadBeingHeld() ? "ALL " + accessor.getName() : accessor.getName();
			final String details = parent.getDetails(notes);
			final boolean first = i == 0;

			if (accessor.getUnit() == NoteValueUnit.MIDI || accessor.getUnit() == NoteValueUnit.NONE) {
				final Integer newValue = accessor.applyIntIncrement(inc, note);
				if (first && newValue != null) {
					oled.paramInfo(function, newValue, details, accessor.getMinInt(), accessor.getMaxInt());
				}
			} else if (accessor.getUnit() == NoteValueUnit.OCCURENCE) {
				final NoteOccurrence newValue = incrementOccurence(inc, note);
				if (newValue != null) {
					oled.paramInfo(function, newValue.toString().replace("_", " "), details);
				}
			} else if (accessor.getUnit() == NoteValueUnit.RECURRENCE) {
				final Integer newValue = accessor.applyIntIncrement(inc, note);
				if (first && newValue != null) {
					parent.updateRecurrencLength(newValue);
					oled.paramInfo(function, newValue, details, accessor.getMinInt(), accessor.getMaxInt(), 1);
				}
			} else {
				handleIncDouble(inc, accessor, notes, note, first);
			}
		}
	}

	private NoteOccurrence incrementOccurence(final int inc, final NoteStep note) {
		final NoteOccurrence occurrence = note.occurrence();
		final NoteOccurrence[] vs = NoteOccurrence.values();
		int index = -1;
		for (int en = 0; en < vs.length; en++) {
			if (occurrence == vs[en]) {
				index = en;
				break;
			}
		}
		final int next = index + inc;
		if (next >= 0 && next < vs.length) {
			final NoteOccurrence newValue = vs[next];
			note.setOccurrence(newValue);
			return newValue;
		}
		return null;
	}

	private void handleIncDouble(final int inc, final NoteStepAccess accessor, final List<NoteStep> notes,
			final NoteStep note, final boolean print) {
		Double newValue = null;
		if (accessor.getUnit() == NoteValueUnit.NOTE_LEN) {
			final double stepLen = note.duration() / parent.getGridResolution();

			final double newStepLen = incrementStepLength(inc, stepLen, 0.1, 16.0);
			if (newStepLen != stepLen) {
				newValue = newStepLen * parent.getGridResolution();
				note.setDuration(newValue);
			}
		} else {
			newValue = accessor.applyDoubleIncrement(inc, note);
		}

		if (print && newValue != null) {
			final String details = parent.getDetails(notes);
			showDoubleValue(accessor, newValue, details);
		}
	}

	private double incrementStepLength(final int inc, final double stepLen, final double min, final double max) {
		double newStepLength = 0;
		if (stepLen <= 1.0) {
			newStepLength = incStep(inc, stepLen, 0.01);
		} else if (stepLen <= 2.0) {
			newStepLength = incStep(inc, stepLen, 0.02);
		} else if (stepLen <= 4.0) {
			newStepLength = incStep(inc, stepLen, 0.05);
		} else {
			newStepLength = incStep(inc, stepLen, 0.1);
		}
		if (newStepLength < min) {
			return min;
		} else if (newStepLength > max) {
			return max;
		}

		return newStepLength;
	}

	private double incStep(final int inc, final double stepLen, final double amount) {
		double newStepLength;
		newStepLength = stepLen + amount * inc;
		// TODO figure out snapping
//		final double roundValue = Math.round(newStepLength);
//		final double diff = Math.abs(newStepLength - roundValue);
//		if (diff < amount) {
//			return roundValue;
//		}
		return newStepLength;
	}

	private void showDoubleValue(final NoteStepAccess accessor, final Double value, final String details) {
		if (accessor.getUnit() == NoteValueUnit.SEMI) {
			oled.paramInfoDouble(accessor.getName(), value, details, accessor.getMin(), accessor.getMax());
		} else if (accessor.getUnit() == NoteValueUnit.NOTE_LEN) {
			oled.paramInfoDuration(accessor.getName(), value, details, parent.getGridResolution());
		} else {
			oled.paramInfoPercent(accessor.getName(), value, details, accessor.getMin(), accessor.getMax());
		}
	}

	private void handleTouch(final boolean touched, final NoteStepAccess accessor) {
		if (!touched) {
			oled.clearScreenDelayed();
			if (accessor.getUnit() == NoteValueUnit.RECURRENCE) {
				parent.exitRecurrenceEdit();
			} else if (accessor.getUnit() == NoteValueUnit.NOTE_LEN) {
				parent.getLengthDisplay().set(false);
			}
			return;
		}
		final List<NoteStep> heldNotes = parent.getHeldNotes();
		if (parent.getDeleteHeld().get() && accessor.canReset()) {
			accessor.applyReset(parent.getOnNotes());
			oled.paramInfo("Reset:" + accessor.getName(), parent.getPadInfo());
		} else if (heldNotes.isEmpty()) {
			oled.paramInfo(accessor.getName(), parent.getPadInfo());
		} else {
			final NoteStep note = heldNotes.get(0);
			final String details = parent.getDetails(heldNotes);
			if (accessor.getUnit() == NoteValueUnit.NOTE_LEN) {
				parent.getLengthDisplay().set(true);
			}

			if (accessor.getUnit() == NoteValueUnit.MIDI || accessor.getUnit() == NoteValueUnit.NONE) {
				final int value = accessor.getInt(note);
				oled.paramInfo(accessor.getName(), value, details, accessor.getMinInt(), accessor.getMaxInt());
			} else if (accessor.getUnit() == NoteValueUnit.OCCURENCE) {
				oled.paramInfo(accessor.getName(), note.occurrence().toString().replace("_", " "), details);
			} else if (accessor.getUnit() == NoteValueUnit.RECURRENCE) {
				final int value = accessor.getInt(note);
				oled.paramInfo(accessor.getName(), value, details, accessor.getMinInt(), accessor.getMaxInt());
				parent.enterRecurrenceEdit(heldNotes);
			} else {
				final double value = accessor.getDouble(note);
				showDoubleValue(accessor, value, details);
			}
			parent.registerModifiedSteps(heldNotes);
		}
	}

	@Override
	protected void onActivate() {
		currentLayer.activate();
		applyResolution(encoderMode);
	}

	@Override
	protected void onDeactivate() {
		currentLayer.deactivate();
	}

}
