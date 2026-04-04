package com.oikoaudio.fire.sequence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oikoaudio.fire.AkaiFireDrumSeqExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.EncoderStepAccumulator;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.bitwig.extension.controller.api.NoteOccurrence;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.framework.Layer;

public class SequencEncoderHandler extends Layer {
    private final StepSequencerHost parent;

	private EncoderMode encoderMode = EncoderMode.CHANNEL;
	private final Layer channelLayer;
	private final Layer mixerLayer;
	private final Layer user1Layer;
	private final Layer user2Layer;

	private Layer currentLayer;
	private final OledDisplay oled;
	private final Map<EncoderMode, Layer> modeMapping = new HashMap<>();
	private final TouchEncoder[] encoders;

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

	public SequencEncoderHandler(final StepSequencerHost host, final AkaiFireDrumSeqExtension driver) {
		super(driver.getLayers(), "Encoder_layer");
		this.parent = host;
        this.oled = driver.getOled();
		channelLayer = new Layer(driver.getLayers(), "ENC_CHANNEL_LAYER");
		mixerLayer = new Layer(driver.getLayers(), "ENC_MIXER_LAYER");
		user1Layer = new Layer(driver.getLayers(), "ENC_USER1_LAYER");
		user2Layer = new Layer(driver.getLayers(), "ENC_USER2_LAYER");
		encoders = driver.getEncoders();
		assign(EncoderMode.CHANNEL, channelLayer, encoders);
        modeMapping.put(EncoderMode.MIXER, mixerLayer);
        parent.bindMixerPage(this, mixerLayer, encoders);
		assign(EncoderMode.USER_1, user1Layer, encoders);
        modeMapping.put(EncoderMode.USER_2, user2Layer);
        parent.bindUser2Page(this, user2Layer, encoders);
		currentLayer = channelLayer;
		final BiColorButton modeButon = driver.getButton(NoteAssign.KNOB_MODE);
		modeButon.bindPressed(this, this::handleModeAdvance, this::modeToLight);

	}


	private void assign(final EncoderMode mode, final Layer layer, final TouchEncoder[] encoders) {
		modeMapping.put(mode, layer);
		final EncoderAccess[] assignments = mode.getAssignments();
		for (int i = 0; i < assignments.length; i++) {
			if (assignments[i] instanceof NoteStepAccess) {
				bindNoteAccess(layer, encoders[i], (NoteStepAccess) assignments[i]);
			}
		}
	}

	public EncoderMode nextMode() {
		if (encoderMode == EncoderMode.CHANNEL) {
			return EncoderMode.MIXER;
		} else if (encoderMode == EncoderMode.MIXER) {
			return EncoderMode.USER_1;
		} else if (encoderMode == EncoderMode.USER_1) {
			return EncoderMode.USER_2;
		}
		return EncoderMode.CHANNEL;
	}

	public void toggleShiftForCurrentMode() {
        // No alternate page variants in the shared step-page model.
	}

	public void bindNoteAccess(final Layer layer, final TouchEncoder encoder, final NoteStepAccess access) {
        final EncoderStepAccumulator accumulator = access.getStepThreshold() > 1
                ? new EncoderStepAccumulator(access.getStepThreshold())
                : null;
		encoder.bindEncoder(layer, inc -> {
            final int effectiveInc = accumulator != null ? accumulator.consume(inc) : inc;
            if (effectiveInc != 0) {
                handleMod(effectiveInc, access);
            }
        });
		encoder.bindTouched(layer, touched -> {
            if (!touched && accumulator != null) {
                accumulator.reset();
            }
            handleTouch(touched, access);
        });
	}

	private void handleModeAdvance(final boolean pressed) {
		if (!pressed) {
			oled.clearScreenDelayed();
			return;
		}
		if (parent.isSelectHeld()) { // display encoder details on select + mode
			oled.detailInfo("Encoder Mode", parent.getModeInfo(encoderMode));
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
		oled.detailInfo("Encoder Mode", parent.getModeInfo(encoderMode));
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
            if (accessor.getUnit() == NoteValueUnit.OCCURENCE || accessor.getUnit() == NoteValueUnit.RECURRENCE) {
                oled.valueInfo(accessor.getName(), "Hold Step");
            } else {
			    oled.paramInfo(accessor.getName(), parent.getPadInfo());
            }
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
