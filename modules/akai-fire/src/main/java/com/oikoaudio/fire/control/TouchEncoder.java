package com.oikoaudio.fire.control;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;

public class TouchEncoder {
	private final ControllerHost host;
	private final RelativeHardwareKnob encoder;
	private final HardwareButton touchButton;

	public TouchEncoder(final int controlId, final int buttonId, final AkaiFireOikontrolExtension driver) {
		this.host = driver.getHost();
		final int noteValue = controlId;
		final HardwareSurface surface = driver.getSurface();
		final MidiIn midiIn = driver.getMidiIn();
		encoder = surface.createRelativeHardwareKnob("ENDLESKNOB_" + controlId);

		encoder.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0, noteValue, 127));
		encoder.setStepSize(0.25);

		// mainLayer.bind(encoder, createIncrementBinder(this::handleIncrement));
		touchButton = surface.createHardwareButton("ENDLESKNOB_BUTTON_" + buttonId);
		touchButton.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(0, buttonId));
		touchButton.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, buttonId));
	}

	public void setStepSize(final double value) {
		encoder.setStepSize(value);
	}

	public void bindTouched(final Layer layer, final Consumer<Boolean> target) {
		layer.bind(touchButton, touchButton.pressedAction(), () -> target.accept(true));
		layer.bind(touchButton, touchButton.releasedAction(), () -> target.accept(false));
	}

	public void bindEncoder(final Layer layer, final IntConsumer action) {
		layer.bind(encoder, createRelativeMagnitudeBinder(rawUnits -> {
			final int inc = RelativeEncoderMagnitude.toDirectionStep(rawUnits);
			if (inc != 0) {
				action.accept(inc);
			}
		}));
	}

	public void bindEncoderBehavior(final Layer layer, final BooleanSupplier fineSupplier,
			final EncoderTurnBehavior behavior, final IntConsumer action) {
		layer.bind(encoder, createRelativeMagnitudeBinder(rawUnits -> {
			final boolean fine = fineSupplier.getAsBoolean();
			final int inc = RelativeEncoderMagnitude.toStandardTurnStep(rawUnits, fine);
			final int effective = behavior.apply(inc, fine);
			if (effective != 0) {
				action.accept(effective);
			}
		}));
	}

	public void bindContinuousEncoder(final Layer layer, final BooleanSupplier fineSupplier, final IntConsumer action) {
		bindContinuousEncoder(layer, fineSupplier, ContinuousEncoderScaler.Profile.STRONG, action);
	}

	public void bindContinuousEncoder(final Layer layer, final BooleanSupplier fineSupplier,
			final ContinuousEncoderScaler.Profile profile, final IntConsumer action) {
		final EncoderTurnBehavior behavior = EncoderTurnBehavior.acceleratedValue(profile);
		layer.bind(encoder, createRelativeMagnitudeBinder(rawUnits -> {
			final boolean fine = fineSupplier.getAsBoolean();
			final int inc = RelativeEncoderMagnitude.toStandardTurnStep(rawUnits, fine);
			final int effective = behavior.apply(inc, fine);
			if (effective != 0) {
				action.accept(effective);
			}
		}));
	}

	public void bindThresholdedEncoder(final Layer layer, final int normalThreshold, final int fineThreshold,
			final BooleanSupplier fineSupplier, final IntConsumer action) {
		bindEncoderBehavior(layer, fineSupplier, EncoderTurnBehavior.quantizedSteps(normalThreshold, fineThreshold), action);
	}

	public void bindRelativeMagnitudeEncoder(final Layer layer, final IntConsumer action) {
		layer.bind(encoder, createRelativeMagnitudeBinder(action));
	}

	public void bindRelativeMagnitudeEncoderBehavior(final Layer layer, final BooleanSupplier fineSupplier,
			final EncoderTurnBehavior behavior, final IntConsumer action) {
		layer.bind(encoder, createRelativeMagnitudeBinder(inc -> {
			final int effective = behavior.apply(inc, fineSupplier.getAsBoolean());
			if (effective != 0) {
				action.accept(effective);
			}
		}));
	}

	public RelativeHardwarControlBindable createIncrementBinder(final IntConsumer consumer) {
		return createRelativeMagnitudeBinder(rawUnits -> {
			final int inc = RelativeEncoderMagnitude.toDirectionStep(rawUnits);
			if (inc != 0) {
				consumer.accept(inc);
			}
		});
	}

	public RelativeHardwarControlBindable createRelativeMagnitudeBinder(final IntConsumer consumer) {
		return host.createRelativeHardwareControlAdjustmentTarget(adjustment -> {
			final int units = RelativeEncoderMagnitude.toSignedUnits(adjustment);
			if (units != 0) {
				consumer.accept(units);
			}
		});
	}
}
