package com.oikoaudio.fire.lights;

import java.util.Objects;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class RgbLightState extends InternalHardwareLightState {

	private static final int ULTRA_DIM_FACTOR = 15;
	private static final int DIM_FACTOR = 4;
	private static final int SOFT_DIM_NUMERATOR = 2;
	private static final int SOFT_DIM_DENOMINATOR = 5;
	private static final int BRIGHT_FACTOR = 20;
	private static final int MAX_BRIGHT_FACTOR = 40;
	public static final RgbLightState OFF = new RgbLightState(0, 0, 0, true);
	public static final RgbLightState PURPLE = new RgbLightState(80, 0, 80, true);
	public static final RgbLightState WHITE = new RgbLightState(100, 100, 100, true);
	public static final RgbLightState GRAY_1 = new RgbLightState(10, 10, 10, true);
	public static final RgbLightState GRAY_2 = new RgbLightState(40, 40, 40, true);

	private final byte red;
	private final byte green;
	private final byte blue;

	private RgbLightState veryDimmed;
	private RgbLightState dimmed;
	private RgbLightState softDimmed;
	private RgbLightState brightend;
	private RgbLightState brightest;

	public RgbLightState(final int red, final int green, final int blue, final boolean variants) {
		this((byte) red, (byte) green, (byte) blue, variants);
	}

	public RgbLightState(final byte red, final byte green, final byte blue) {
		this(red, green, blue, true);
	}

	private RgbLightState(final byte red, final byte green, final byte blue, final boolean variants) {
		super();
		this.red = red;
		this.green = green;
		this.blue = blue;
		if (variants) {
			softDimmed = new RgbLightState(scaleSoftDim(red), scaleSoftDim(green), scaleSoftDim(blue), false);
			dimmed = new RgbLightState(red / DIM_FACTOR, green / DIM_FACTOR, blue / DIM_FACTOR, false);
			veryDimmed = new RgbLightState(red / ULTRA_DIM_FACTOR, green / ULTRA_DIM_FACTOR, blue / ULTRA_DIM_FACTOR,
					false);
			brightend = new RgbLightState(Math.min(red + BRIGHT_FACTOR, 127), Math.min(green + BRIGHT_FACTOR, 127),
					Math.min(blue + BRIGHT_FACTOR, 127), false);
			brightest = new RgbLightState(Math.min(red + MAX_BRIGHT_FACTOR, 127),
					Math.min(green + MAX_BRIGHT_FACTOR, 127), Math.min(blue + MAX_BRIGHT_FACTOR, 127), false);
		}
	}

	@Override
	public HardwareLightVisualState getVisualState() {
		return null;
	}

	public RgbLightState getVeryDimmed() {
		return veryDimmed != null ? veryDimmed : this;
	}

	public RgbLightState getDimmed() {
		return dimmed != null ? dimmed : this;
	}

	public RgbLightState getSoftDimmed() {
		return softDimmed != null ? softDimmed : this;
	}

	public RgbLightState getBrightend() {
		return brightend != null ? brightend : this;
	}

	public RgbLightState getBrightest() {
		return brightest != null ? brightest : this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(blue, green, red);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final RgbLightState other = (RgbLightState) obj;
		return blue == other.blue && green == other.green && red == other.red;
	}

	public byte getRed() {
		return red;
	}

	public byte getBlue() {
		return blue;
	}

	public byte getGreen() {
		return green;
	}

	private static byte scaleSoftDim(final byte component) {
		return (byte) ((component & 0xFF) * SOFT_DIM_NUMERATOR / SOFT_DIM_DENOMINATOR);
	}

}
