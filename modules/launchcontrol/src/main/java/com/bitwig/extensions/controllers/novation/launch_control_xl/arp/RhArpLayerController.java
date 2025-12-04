package com.bitwig.extensions.controllers.novation.launch_control_xl.arp;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.IntegerValue;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.SpecificBitwigDevice;
import com.bitwig.extensions.controllers.novation.common.SimpleLedColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Mini controller that toggles Arpeggiator steps when the arp layer is active.
 */
public final class RhArpLayerController {

   private static final double[] GLOBAL_VELOCITY_VALUES = {0, 0.1, 0.25, 0.40, 0.55, 0.75, 0.9, 1.0};
   private static final double[] GLOBAL_GATE_VALUES = {0, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0};
   private static final int[] QUANTIZE_FOCUS_NOTES = {-1, 1, 3, -1, 6, 8, 10, -1};
   private static final int[] QUANTIZE_CONTROL_NOTES = {0, 2, 4, 5, 7, 9, 11, 0};

   private enum MatrixMode
   {
      DEFAULT,
      GLOBAL_VELOCITY,
      GLOBAL_GATE,
      TIMING,
      PATTERN,
      VELOCITY_GATE,
      QUANTIZE
   }

   private final ControllerHost host;
   private final List<Parameter> skipParameters = new ArrayList<>();
   private final List<Parameter> velocityParameters = new ArrayList<>();
   private final List<Parameter> gateParameters = new ArrayList<>();
   private final List<Parameter> transposeParameters = new ArrayList<>();
   private final CursorTrack cursorTrack;
   private final PinnableCursorDevice cursorDevice;
   private final SpecificBitwigDevice arpDevice;
   private final Parameter octavesParameter;
   private final Parameter stepsParameter;
   private final IntegerValue stepPosition;
   private final Parameter globalVelocityParameter;
   private final Parameter globalGateParameter;
   private final Parameter rateModeParameter;
   private final Parameter shuffleParameter;
   private final Parameter rateParameter;
   private final Parameter retriggerParameter;
   private final Parameter modeParameter;
   private final double[] knobPitchOffsets = new double[8];
   private final double[] sliderPitchOffsets = new double[8];
   private final double[] storedVelocityValues = new double[8];
   private final boolean[] velocityBoostEnabled = new boolean[8];
   private final double[] storedGateValues = new double[8];
   private final boolean[] gateMuted = new boolean[8];
   private final boolean[] quantizeNotes = new boolean[12];
   private final boolean[] quantizeMutedSteps = new boolean[8];
   private final double[] quantizeStoredGates = new double[8];
   private boolean active;
   private MatrixMode matrixMode = MatrixMode.DEFAULT;

   public RhArpLayerController(final ControllerHost host) {
      this.host = host;
      this.cursorTrack = host.createCursorTrack(0, 0);
      this.cursorDevice = cursorTrack.createCursorDevice();
      this.arpDevice = cursorDevice.createSpecificBitwigDevice(UUID.fromString("4d407a2b-c91b-4e4c-9a89-c53c19fe6251"));
      this.octavesParameter = arpDevice.createParameter("OCTAVES");
      this.octavesParameter.markInterested();
      setUpArpParameters();
      this.stepsParameter = arpDevice.createParameter("STEPS");
      this.stepsParameter.markInterested();
      this.stepPosition = arpDevice.createIntegerOutputValue("STEP");
      this.stepPosition.markInterested();
      this.globalVelocityParameter = arpDevice.createParameter("GLOBAL_VEL");
      this.globalVelocityParameter.markInterested();
      this.globalGateParameter = arpDevice.createParameter("GLOBAL_GATE");
      this.globalGateParameter.markInterested();
      this.rateModeParameter = arpDevice.createParameter("RATE_MODE");
      this.rateModeParameter.markInterested();
      this.shuffleParameter = arpDevice.createParameter("SHUFFLE");
      this.shuffleParameter.markInterested();
      this.rateParameter = arpDevice.createParameter("RATE");
      this.rateParameter.markInterested();
      this.retriggerParameter = arpDevice.createParameter("RETRIGGER");
      this.retriggerParameter.markInterested();
      this.modeParameter = arpDevice.createParameter("MODE");
      this.modeParameter.markInterested();
      Arrays.fill(quantizeNotes, true);
   }

   private void setUpArpParameters() {
      skipParameters.clear();
      velocityParameters.clear();
      gateParameters.clear();
      transposeParameters.clear();
      for (int i = 0; i < 16; i++) {
         final Parameter skip = arpDevice.createParameter("SKIP_" + (i + 1));
         skip.markInterested();
         skipParameters.add(skip);
         final Parameter velocity = arpDevice.createParameter("STEP_" + (i + 1));
         velocity.markInterested();
         velocityParameters.add(velocity);
         final Parameter gate = arpDevice.createParameter("GATE_" + (i + 1));
         gate.markInterested();
         gateParameters.add(gate);
         final Parameter transpose = arpDevice.createParameter("STEP_" + (i + 1) + "_TRANSPOSE");
         transpose.markInterested();
         transposeParameters.add(transpose);
      }
   }

   public void activate() {
      if (active) {
         return;
      }
      active = true;
      host.println("RhArpLayerController: arp layer engaged");
      cursorDevice.isPinned().set(true);
   }

   public void deactivate() {
      if (!active) {
         return;
      }
      active = false;
      matrixMode = MatrixMode.DEFAULT;
      host.println("RhArpLayerController: arp layer disengaged");
   }

   public boolean isActive() {
      return active;
   }

   public void handleMidiEvent(final int status, final int data1, final int data2) {
      if (!active) {
         return;
      }
      host.println("RhArpLayerController MIDI: %02X %02X %02X".formatted(status, data1, data2));
   }

   public void handleSysex(final String data) {
      if (!active) {
         return;
      }
      host.println("RhArpLayerController SYSEX: " + data);
   }

   public void handleSysexTemplateChange(final int templateId) {
      if (!active) {
         return;
      }
      host.println("RhArpLayerController template change: " + templateId);
   }

   public void handleTrackFocusPressed(final int index) {
      if (!active) {
         return;
      }
      switch (matrixMode)
      {
         case GLOBAL_VELOCITY -> handleStepCountPressed(index);
         case DEFAULT, GLOBAL_GATE -> toggleSkip(index);
         case TIMING -> handleTimingFocus(index);
         case PATTERN -> setPatternMode(index);
         case VELOCITY_GATE -> toggleVelocityGateFocus(index);
         case QUANTIZE -> toggleQuantizeButton(index);
      }
   }

   public void handleTrackControlPressed(final int index) {
      if (!active) {
         return;
      }
      switch (matrixMode)
      {
         case DEFAULT -> handleStepCountPressed(index);
         case GLOBAL_VELOCITY -> handleGlobalVelocityPressed(index);
         case GLOBAL_GATE -> handleGlobalGatePressed(index);
         case TIMING -> handleTimingControl(index);
         case PATTERN -> setPatternMode(index + 8);
         case VELOCITY_GATE -> toggleVelocityGateControl(index);
         case QUANTIZE -> toggleQuantizeButton(index + 8);
      }
   }

   public void handleOctaveIncrease() {
      if (!active) {
         return;
      }
      octavesParameter.value().incRaw(1);
   }

   public void handleOctaveDecrease() {
      if (!active) {
         return;
      }
      octavesParameter.value().incRaw(-1);
   }

   public void toggleGlobalVelocityMode() {
      toggleMatrixMode(MatrixMode.GLOBAL_VELOCITY, "Launch Control XL: arp global velocity");
   }

   public void toggleGlobalGateMode() {
      toggleMatrixMode(MatrixMode.GLOBAL_GATE, "Launch Control XL: arp global gate");
   }

   public void toggleTimingMode() {
      toggleMatrixMode(MatrixMode.TIMING, "Launch Control XL: arp timing");
   }

   public void togglePatternMode() {
      toggleMatrixMode(MatrixMode.PATTERN, "Launch Control XL: arp patterns");
   }

   public void toggleVelocityGateMode() {
      toggleMatrixMode(MatrixMode.VELOCITY_GATE, "Launch Control XL: arp velocity/gate");
   }

   public void toggleQuantizeMode() {
      toggleMatrixMode(MatrixMode.QUANTIZE, "Launch Control XL: arp quantize");
   }

   public void handlePitchOffsetKnob(final int index, final int value) {
      if (!active) {
         return;
      }
      knobPitchOffsets[index] = ccValueToPitch(value);
      applyPitchValue(index);
      applyQuantize();
   }

   public void handlePitchOffsetSlider(final int index, final int value) {
      if (!active) {
         return;
      }
      sliderPitchOffsets[index] = ccValueToPitch(value);
      applyPitchValue(index);
      applyQuantize();
   }

   public void handleVelocityKnob(final int index, final int value) {
      if (!active) {
         return;
      }
      final double normalized = ccValueToNormalized(value);
      final Parameter parameter = velocityParameters.get(index);
      parameter.value().set(normalized);
   }

   public void handleGateKnob(final int index, final int value) {
      if (!active) {
         return;
      }
      final double normalized = ccValueToNormalized(value);
      final Parameter parameter = gateParameters.get(index);
      parameter.value().set(normalized);
   }

   public boolean isTimingModeActive() {
      return matrixMode == MatrixMode.TIMING;
   }

   public boolean isPatternModeActive() {
      return matrixMode == MatrixMode.PATTERN;
   }

   public boolean isVelocityGateModeActive() {
      return matrixMode == MatrixMode.VELOCITY_GATE;
   }

   public boolean isQuantizeModeActive() {
      return matrixMode == MatrixMode.QUANTIZE;
   }

   public int applyFocusColor(final int index, final int defaultColor) {
      if (!active) {
         return defaultColor;
      }
      return switch (matrixMode)
      {
         case GLOBAL_VELOCITY -> stepCountColor(index);
         case DEFAULT, GLOBAL_GATE -> skipColor(index);
         case TIMING -> timingFocusColor(index);
         case PATTERN -> patternColor(index);
         case VELOCITY_GATE -> velocityToggleColor(index);
         case QUANTIZE -> quantizeColor(index);
      };
   }

   public int applyControlColor(final int index, final int defaultColor) {
      if (!active) {
         return defaultColor;
      }
      return switch (matrixMode)
      {
         case DEFAULT -> stepCountColor(index);
         case GLOBAL_VELOCITY -> globalVelocityColor(index);
         case GLOBAL_GATE -> globalGateColor(index);
         case TIMING -> timingControlColor(index);
         case PATTERN -> patternColor(index + 8);
         case VELOCITY_GATE -> gateToggleColor(index);
         case QUANTIZE -> quantizeColor(index + 8);
      };
   }

   private static double ccValueToPitch(final int value) {
      return (-24d) + (48d * ccValueToNormalized(value));
   }

   private static double ccValueToNormalized(final int value) {
      return Math.max(0, Math.min(1, value / 127.0));
   }

   private void applyPitchValue(final int index) {
      final double combined = knobPitchOffsets[index] + sliderPitchOffsets[index];
      final double clamped = Math.max(-24d, Math.min(24d, combined));
      final double normalized = (clamped + 24d) / 48d;
      transposeParameters.get(index).value().set(normalized);
   }

   private void toggleMatrixMode(final MatrixMode mode, final String popupMessage) {
      if (!active) {
         return;
      }
      if (matrixMode == mode) {
         matrixMode = MatrixMode.DEFAULT;
         host.showPopupNotification("Launch Control XL: arp default matrix");
      }
      else {
         matrixMode = mode;
         host.showPopupNotification(popupMessage);
      }
   }

   private void toggleSkip(final int index) {
      final Parameter skipParam = skipParameters.get(index);
      final double value = skipParam.value().get();
      skipParam.value().set(value == 0 ? 1 : 0);
   }

   private void handleStepCountPressed(final int index) {
      final double target = Math.max(1, Math.min(16, index + 1));
      stepsParameter.value().setRaw(target);
   }

   private void handleGlobalVelocityPressed(final int index) {
      if (index < GLOBAL_VELOCITY_VALUES.length) {
         globalVelocityParameter.value().setRaw(GLOBAL_VELOCITY_VALUES[index]);
      }
   }

   private void handleGlobalGatePressed(final int index) {
      if (index < GLOBAL_GATE_VALUES.length) {
         globalGateParameter.value().setRaw(GLOBAL_GATE_VALUES[index]);
      }
   }

   private int skipColor(final int index) {
      final Parameter skipParam = skipParameters.get(index);
      final double value = skipParam.value().get();
      return value == 0 ? SimpleLedColor.Green.value() : SimpleLedColor.Red.value();
   }

   private int stepCountColor(final int index) {
      final double stepCount = stepsParameter.value().getRaw();
      if (index < stepCount) {
         return SimpleLedColor.Amber.value();
      }
      return SimpleLedColor.AmberLow.value();
   }

   private int globalVelocityColor(final int index) {
      final double raw = globalVelocityParameter.value().getRaw();
      final double target = index < GLOBAL_VELOCITY_VALUES.length ? GLOBAL_VELOCITY_VALUES[index] : -1;
      return Math.abs(raw - target) < 0.001
         ? SimpleLedColor.Red.value()
         : SimpleLedColor.RedLow.value();
   }

   private int globalGateColor(final int index) {
      final double raw = globalGateParameter.value().getRaw();
      final double target = index < GLOBAL_GATE_VALUES.length ? GLOBAL_GATE_VALUES[index] : -1;
      return Math.abs(raw - target) < 0.001
         ? SimpleLedColor.Amber.value()
         : SimpleLedColor.AmberLow.value();
   }

   private void handleTimingFocus(final int index) {
      if (index >= 0 && index <= 2) {
         rateModeParameter.value().setRaw(index);
      }
      else if (index == 4) {
         final double shuffle = shuffleParameter.value().getRaw();
         shuffleParameter.value().setRaw(shuffle == 0 ? 1 : 0);
      }
   }

   private void handleTimingControl(final int index) {
      if (index >= 0 && index <= 6) {
         rateParameter.value().setRaw(index);
      }
      else if (index == 7) {
         final double retrigger = retriggerParameter.value().getRaw();
         retriggerParameter.value().setRaw(retrigger == 0 ? 1 : 0);
      }
   }

   private int timingFocusColor(final int index) {
      if (index >= 0 && index <= 2) {
         final double raw = rateModeParameter.value().getRaw();
         return Math.abs(raw - index) < 0.001 ? SimpleLedColor.Amber.value() : SimpleLedColor.AmberLow.value();
      }
      if (index == 4) {
         return shuffleParameter.value().getRaw() == 0 ? SimpleLedColor.Red.value() : SimpleLedColor.RedLow.value();
      }
      return SimpleLedColor.Off.value();
   }

   private int timingControlColor(final int index) {
      if (index >= 0 && index <= 6) {
         final double raw = rateParameter.value().getRaw();
         return Math.abs(raw - index) < 0.001 ? SimpleLedColor.Green.value() : SimpleLedColor.GreenLow.value();
      }
      if (index == 7) {
         return retriggerParameter.value().getRaw() == 1 ? SimpleLedColor.Amber.value() : SimpleLedColor.AmberLow.value();
      }
      return SimpleLedColor.Off.value();
   }

   private void setPatternMode(final int index) {
      if (index < 0 || index >= 16) {
         return;
      }
      modeParameter.value().setRaw(index + 1);
   }

   private int patternColor(final int index) {
      if (index < 0 || index >= 16) {
         return SimpleLedColor.Off.value();
      }
      final double raw = modeParameter.value().getRaw();
      return Math.abs(raw - (index + 1)) < 0.001 ? SimpleLedColor.Red.value() : SimpleLedColor.RedLow.value();
   }

   private void toggleVelocityGateFocus(final int index) {
      if (index < 0 || index >= velocityParameters.size()) {
         return;
      }
      final Parameter parameter = velocityParameters.get(index);
      final double current = parameter.value().get();
      if (velocityBoostEnabled[index]) {
         parameter.value().set(storedVelocityValues[index]);
         velocityBoostEnabled[index] = false;
      }
      else {
         storedVelocityValues[index] = current;
         parameter.value().set(1.0);
         velocityBoostEnabled[index] = true;
      }
   }

   private void toggleVelocityGateControl(final int index) {
      if (index < 0 || index >= gateParameters.size()) {
         return;
      }
      final Parameter parameter = gateParameters.get(index);
      if (gateMuted[index]) {
         final double restore = storedGateValues[index] > 0 ? storedGateValues[index] : 1.0;
         parameter.value().set(restore);
         gateMuted[index] = false;
      }
      else {
         storedGateValues[index] = parameter.value().get();
         parameter.value().set(0.0);
         gateMuted[index] = true;
      }
      applyQuantize();
   }

   private int velocityToggleColor(final int index) {
      if (index < 0 || index >= velocityBoostEnabled.length) {
         return SimpleLedColor.Off.value();
      }
      return velocityBoostEnabled[index] ? SimpleLedColor.Green.value() : SimpleLedColor.GreenLow.value();
   }

   private int gateToggleColor(final int index) {
      if (index < 0 || index >= gateMuted.length) {
         return SimpleLedColor.Off.value();
      }
      return gateMuted[index] ? SimpleLedColor.Red.value() : SimpleLedColor.Amber.value();
   }

   private void toggleQuantizeButton(final int index) {
      final int note = getQuantizeNoteForIndex(index);
      if (note < 0) {
         return;
      }
      quantizeNotes[note] = !quantizeNotes[note];
      applyQuantize();
   }

   private int quantizeColor(final int index) {
      final int note = getQuantizeNoteForIndex(index);
      if (note < 0) {
         return SimpleLedColor.Off.value();
      }
      return quantizeNotes[note] ? SimpleLedColor.Green.value() : SimpleLedColor.GreenLow.value();
   }

   private int getQuantizeNoteForIndex(final int index) {
      if (index < 0 || index >= 16) {
         return -1;
      }
      if (index < 8) {
         return QUANTIZE_FOCUS_NOTES[index];
      }
      return QUANTIZE_CONTROL_NOTES[index - 8];
   }

   private void applyQuantize() {
      for (int i = 0; i < Math.min(8, gateParameters.size()); i++) {
         final int note = getStepNoteIndex(i);
         final boolean allowed = note < 0 || quantizeNotes[note];
         final Parameter gate = gateParameters.get(i);
         if (allowed) {
            if (quantizeMutedSteps[i]) {
               gate.value().set(quantizeStoredGates[i]);
               quantizeMutedSteps[i] = false;
            }
         }
         else {
            if (!quantizeMutedSteps[i]) {
               quantizeStoredGates[i] = gate.value().get();
               gate.value().set(0.0);
               quantizeMutedSteps[i] = true;
            }
         }
      }
   }

   private int getStepNoteIndex(final int index) {
      if (index < 0 || index >= knobPitchOffsets.length) {
         return -1;
      }
      final double combined = knobPitchOffsets[index] + sliderPitchOffsets[index];
      final int semitone = (int)Math.round(combined);
      return Math.floorMod(semitone, 12);
   }

   public int getPitchLedColor(final int index) {
      if (!active || index >= transposeParameters.size()) {
         return SimpleLedColor.Off.value();
      }
      final double normalized = transposeParameters.get(index).value().get();
      final double semitones = normalized * 48.0 - 24.0;
      if (Math.abs(semitones) < 0.1) {
         return SimpleLedColor.Yellow.value();
      }
      if (semitones > 0) {
         return Math.abs(semitones) >= 12 ? SimpleLedColor.Green.value() : SimpleLedColor.GreenLow.value();
      }
      return Math.abs(semitones) >= 12 ? SimpleLedColor.Red.value() : SimpleLedColor.RedLow.value();
   }

   public int getVelocityLedColor(final int index) {
      if (!active || index >= velocityParameters.size()) {
         return SimpleLedColor.Off.value();
      }
      final double value = velocityParameters.get(index).value().get();
      if (value <= 0.01) {
         return SimpleLedColor.Off.value();
      }
      if (value >= 0.85) {
         return SimpleLedColor.Green.value();
      }
      return SimpleLedColor.GreenLow.value();
   }

   public int getGateLedColor(final int index) {
      if (!active || index >= gateParameters.size()) {
         return SimpleLedColor.Off.value();
      }
      final double value = gateParameters.get(index).value().get();
      if (value <= 0.01) {
         return SimpleLedColor.Off.value();
      }
      if (value >= 0.85) {
         return SimpleLedColor.Yellow.value();
      }
      return SimpleLedColor.YellowLow.value();
   }
}
