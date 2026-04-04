package com.oikoaudio.fire.display;

import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.bitwig.extension.api.Host;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.IntegerValue;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.framework.Binding;

public class ParameterDisplayBinding extends Binding<Parameter, DisplayTarget> {

	ControllerHost host = AkaiFireOikontrolExtension.getGlobalHost();
	private double rawValue;
	private String displayValue;
	private final int index;
	private final int typeIndex;
	private final boolean bipolar;
	private int discreteStepCount = -1; // Default to -1 (continuous)
	private String paramName = ""; // Store parameter name

	private static final String[] NOTE_NAMES_TUNE = { // 0 = G#
			"G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G"
	};
	private static final String[] NOTE_NAMES_PITCH = { // 0 = C
			"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
	};

	public ParameterDisplayBinding(final int typeIndex, final int index, final Parameter source,
								   final DisplayTarget target, final boolean bipolar) {
		super(target, source, target);
		this.index = index;
		this.typeIndex = typeIndex;
		this.bipolar = bipolar;


		source.value().addValueObserver(this::handleRawValue);
		source.displayedValue().addValueObserver(this::handleDisplayValue);

		// Observe discreteValueCount()
		IntegerValue stepCount = source.discreteValueCount();
		stepCount.markInterested();
		stepCount.addValueObserver(value -> {
			discreteStepCount = value; // Store detected step count dynamically
		});

		// Observe and store parameter name
		source.name().markInterested();
		source.name().addValueObserver(name -> {
			paramName = name.toLowerCase(); // Store as lowercase for comparison
		});
	}

	private void handleRawValue(final double rawValue) {
		this.rawValue = rawValue;
		if (isActive()) {
			getTarget().update(index, typeIndex, rawValue, displayValue, bipolar);
		}
	}

	private void handleDisplayValue(final String displayValue) {
		this.displayValue = displayValue;
		host.showPopupNotification(paramName);
		if (isActive()) {
			if (typeIndex == 4) { // Assuming typeIndex 4 is for Tune/Pitch

				// Default to using the standard display value
				String outputDisplay = displayValue;

				// If the parameter name contains "tune" or "pitch", calculate note name
				if (paramName.contains("tune") || paramName.contains("pitch")) {

					// Determine range
					int minRange = -12;
					int maxRange = 12;
					boolean isPitch = paramName.contains("pitch");

					if (isPitch) { // Pitch-based range (-36 to +36)
						minRange = -36;
						maxRange = 36;
					}

					// Scale raw value (0.0 - 1.0) to the detected range
					int knobValue = (int) Math.round(rawValue * (maxRange - minRange)) + minRange;

					// Convert to note name with octave shift
					outputDisplay = getNoteDisplay(knobValue, isPitch);
				}

				// Update display with either note name or default value
				getTarget().update(index, typeIndex, rawValue, outputDisplay, bipolar);
			} else {
				// Default display for other parameters
				getTarget().update(index, typeIndex, rawValue, displayValue, bipolar);
			}
		}
	}

	@Override
	protected void deactivate() {
	}

	@Override
	protected void activate() {
		update();
	}

	public void modify(final double inc) {
		final SettableRangedValue value = getSource().value();
		final double preValue = value.get();
		final double newValue = Math.min(1, Math.max(0, preValue + inc));
		if (preValue != newValue) {
			value.setImmediately(newValue);
		}
	}

	public void update() {
		getTarget().update(index, typeIndex, rawValue, displayValue, bipolar);
	}

	// Method to get the note name and relative octave shift
	private String getNoteDisplay(int knobValue, boolean isPitch) {
		String[] noteNames = isPitch ? NOTE_NAMES_PITCH : NOTE_NAMES_TUNE;

		// Calculate note index
		int noteIndex = ((knobValue % 12) + 12) % 12; // Normalize note position

		// Determine octave shift
		int octaveShift = (knobValue + 12) / 12; // Calculate octave relative to -12 or -36

		// Tune only has -1 or +1 octave
		if (!isPitch) {
			if (octaveShift < 1) {
				return noteNames[noteIndex] + " (-1)";
			} else if (octaveShift > 1) {
				return noteNames[noteIndex] + " (+1)";
			}
			return noteNames[noteIndex]; // No shift displayed
		}

		// Pitch can have multiple octaves, so always display relative octave
		String octaveSign = octaveShift == 0 ? "" : (octaveShift > 0 ? " (+" + octaveShift + ")" : " (" + octaveShift + ")");
		return noteNames[noteIndex] + octaveSign;
	}
}
