package com.bitwig.extensions.controllers.novation.launch_control_xl;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.RemoteControlsPage;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.controllers.novation.launch_control_xl.LaunchControlXlControllerExtension.TrackControl;
import com.bitwig.extensions.controllers.novation.launch_control_xl.drum.DrumMapping;
import com.bitwig.extensions.framework.Layer;

/**
 * Implements the Launch Control XL drum user layer (template 7).
 * <p>
 * Handles binding of hardware controls to drum pads (sliders -> pad volumes, knobs -> per-pad remote
 * controls, buttons -> triggers/mutes/solo) and provides a mute/solo mode that mirrors the hardware
 * Track Control buttons.
 */
final class DrumLayerController
{
   static final int PADS_PER_BANK = 8;
   private static final boolean DEBUG_DRUM = true;

   private final ControllerHost host;
   private final DrumPadBank padBank;
   private final PinnableCursorDevice cursorDevice;
   private final RemoteControlsPage[] padRemoteControls;
   private final NoteInput noteInput;
   private final int midiChannel;
   private final Layer layer;
   private final HardwareSlider[] faders;
   private final AbsoluteHardwareKnob[][] knobs; // [row][col]
   private final HardwareButton[] bottomButtons;
   private final HardwareButton[] topButtons;
   private final HardwareButton trackLeft;
   private final HardwareButton trackRight;
   private boolean auditionOnSelect;
   private boolean accentMomentary;

   private boolean soloMode;
   private boolean activeRequested;
   private TrackControl trackControlMode = TrackControl.None;
   private int selectedPadIndex = -1;

   DrumLayerController(final ControllerHost host,
                       final NoteInput noteInput,
                       final int midiChannel,
                       final DrumPadBank padBank,
                       final PinnableCursorDevice cursorDevice,
                       final RemoteControlsPage[] padRemoteControls,
                       final Layer layer,
                       final HardwareSlider[] faders,
                       final AbsoluteHardwareKnob[][] knobs,
                       final HardwareButton[] bottomButtons,
                       final HardwareButton[] topButtons,
                       final HardwareButton trackLeft,
                       final HardwareButton trackRight,
                       final boolean auditionOnSelect,
                       final boolean accentMomentary)
   {
      this.host = host;
      this.padBank = padBank;
      this.cursorDevice = cursorDevice;
      this.padRemoteControls = padRemoteControls;
      this.noteInput = noteInput;
      this.midiChannel = midiChannel;
      this.layer = layer;
      this.faders = faders;
      this.knobs = knobs;
      this.bottomButtons = bottomButtons;
      this.topButtons = topButtons;
      this.trackLeft = trackLeft;
      this.trackRight = trackRight;
      this.auditionOnSelect = auditionOnSelect;
      this.accentMomentary = accentMomentary;
   }

   /**
    * Bind all hardware controls to pads/parameters. Call once during initialization before the layer
    * can be engaged.
    */
   void init()
   {
      cursorDevice.hasDrumPads().markInterested();
      cursorDevice.hasDrumPads().addValueObserver(hasPads -> {
         if (!hasPads)
         {
            if (activeRequested)
            {
               host.showPopupNotification("Select a Drum Machine");
            }
            padBank.setIndication(false);
            layer.setIsActive(false);
         }
         else if (activeRequested)
         {
            engage();
         }
      });

      padBank.setChannelScrollStepSize(PADS_PER_BANK);
      padBank.canScrollBackwards().markInterested();
      padBank.canScrollForwards().markInterested();
      padBank.scrollPosition().markInterested();

      forEachPad((index, pad) -> {
         pad.exists().markInterested();
         pad.mute().markInterested();
         pad.solo().markInterested();
         pad.volume().markInterested();
         pad.channelIndex().markInterested();

         final DeviceBank deviceBank = pad.createDeviceBank(1);
         final Device primaryDevice = deviceBank.getItemAt(0);
         primaryDevice.exists().markInterested();

         final CursorRemoteControlsPage rc = primaryDevice.createCursorRemoteControlsPage(4);
         padRemoteControls[index] = rc;

         rc.setHardwareLayout(HardwareControlType.KNOB, 3);
         for (int p = 0; p < 4; p++)
         {
            rc.getParameter(p).markInterested();
            rc.getParameter(p).value().markInterested();
            rc.getParameter(p).exists().markInterested();
         }

         layer.bind(faders[index], pad.volume());
         layer.bind(knobs[0][index], rc.getParameter(0));
         layer.bind(knobs[1][index], rc.getParameter(1));
         layer.bind(knobs[2][index], rc.getParameter(2));
         layer.bindPressed(topButtons[index], () -> {
            selectedPadIndex = index;
            pad.selectInEditor();
            if (auditionOnSelect)
               playPad(pad, index, 100);
         });
         final int buttonIndex = index;
         layer.bindPressed(bottomButtons[index], () -> handleBottomButton(buttonIndex));
         layer.bindReleased(bottomButtons[index], () -> releaseBottomButton(buttonIndex));
         log("Pad " + index + " bindings installed");
      });

      layer.bindPressed(trackLeft, () -> {
         padBank.scrollBy(-PADS_PER_BANK);
         log("Pad bank scroll left");
      });
      layer.bindPressed(trackRight, () -> {
         padBank.scrollBy(PADS_PER_BANK);
         log("Pad bank scroll right");
      });
      log("Drum layer init complete");
   }

   /**
    * Activate the layer when the user template is selected. Verifies that the cursor device exposes
    * drum pads and shows guidance if not.
    */
   void engage()
   {
      activeRequested = true;
      if (!cursorDevice.hasDrumPads().get())
      {
         host.showPopupNotification("Select a Drum Machine");
         layer.setIsActive(false);
         padBank.setIndication(false);
         log("Engage aborted: no drum pads on cursor device");
         return;
      }

      padBank.setIndication(true);
      layer.setIsActive(true);
      host.showPopupNotification("Drum layer engaged (User Template 7)");
      log("Drum layer engaged: channel=" + midiChannel);
   }

   /** Deactivate the layer and reset mode state (solo/mute). */
   void disengage()
   {
      activeRequested = false;
      soloMode = false;
      trackControlMode = TrackControl.None;
      selectedPadIndex = -1;
      padBank.setIndication(false);
      layer.setIsActive(false);
      log("Drum layer disengaged");
   }

   /** Toggle between mute and solo behaviour for the bottom buttons. */
   void toggleSoloMode()
   {
      soloMode = !soloMode;
      // Solo mode and mute mode are mutually exclusive; turning on solo clears drum mute mode.
      trackControlMode = soloMode ? TrackControl.None : trackControlMode;
      host.showPopupNotification(soloMode ? "Drum layer: Solo buttons" : "Drum layer: Default buttons");
      log("Solo mode toggled -> " + soloMode + " (trackControl=" + trackControlMode + ")");
   }

   boolean isSoloMode()
   {
      return soloMode;
   }

   private void playPad(final DrumPad pad, final int padIndex, final int velocity)
   {
      if (noteInput == null)
      {
         log("playPad ignored: noteInput null");
         return;
      }
      // Derive the outgoing note purely from the current pad bank position and the pad index so it
      // stays consistent across pages (C1 upward).
      final int bankOffset = padBank.scrollPosition().get();
      final int key = bankOffset + padIndex;
      final int appliedVelocity = velocity > 0 ? 100 : 0;
      final int statusOn = 0x90 + midiChannel;
      final int statusOff = 0x80 + midiChannel;
      log("bankOffset=" + bankOffset + " padIndex=" + padIndex);
      log("playPad send status=" + statusOn + " key=" + key + " vel=" + appliedVelocity);
      if (appliedVelocity > 0)
      {
         noteInput.sendRawMidiEvent(statusOn, key, appliedVelocity);
      }
      noteInput.sendRawMidiEvent(statusOff, key, 0);
   }

   /** Toggle the current mute/solo target for the given pad. */
   private void toggleMuteOrSolo(final DrumPad pad)
   {
      final SettableBooleanValue target = soloMode ? pad.solo() : pad.mute();
      target.toggle();
      log("toggleMuteOrSolo -> soloMode=" + soloMode + " state=" + target.get());
   }

   /** Toggle the drum-layer mute mode on/off (independent from factory track control layers). */
   void toggleMuteMode()
   {
      final boolean enableMute = this.trackControlMode != TrackControl.Mute;
      this.trackControlMode = enableMute ? TrackControl.Mute : TrackControl.None;
      if (enableMute)
         this.soloMode = false;
      this.host.showPopupNotification(enableMute ? "Drum layer: Mute buttons" : "Drum layer: Default buttons");
      this.log("Mute mode toggled -> " + this.trackControlMode);
   }

   boolean handleMidi(final int status, final int data1, final int data2)
   {
      final int channel = status & 0x0F;
      if (channel != midiChannel)
      {
         return false;
      }
      final int message = status & 0xF0;
      if (message == 0xB0) // CC
      {
         final int sliderIndex = indexOf(DrumMapping.SLIDER_CCS, data1);
         if (sliderIndex >= 0)
         {
            final DrumPad pad = padBank.getItemAt(sliderIndex);
            pad.volume().set(data2, 127);
            log("Slider " + sliderIndex + " -> pad volume " + data2);
            return true;
         }
         final int knobRow1 = indexOf(DrumMapping.KNOB_ROW1_CCS, data1);
         if (knobRow1 >= 0)
         {
            setKnobValue(knobRow1, 0, data2);
            return true;
         }
         final int knobRow2 = indexOf(DrumMapping.KNOB_ROW2_CCS, data1);
         if (knobRow2 >= 0)
         {
            setKnobValue(knobRow2, 1, data2);
            return true;
         }
         final int knobRow3 = indexOf(DrumMapping.KNOB_ROW3_CCS, data1);
         if (knobRow3 >= 0)
         {
            setKnobValue(knobRow3, 2, data2);
            return true;
         }
         final int topIndex = indexOf(DrumMapping.TOP_NOTES, data1);
         if (topIndex >= 0 && data2 > 0)
         {
            final DrumPad pad = padBank.getItemAt(topIndex);
            selectedPadIndex = topIndex;
            pad.selectInEditor();
            if (auditionOnSelect)
            {
               playPad(pad, topIndex, 100);
               log("Select+trigger pad " + topIndex + " (CC)");
            }
            else
            {
               log("Select pad " + topIndex + " (CC)");
            }
            return true;
         }
         if (data1 == DrumMapping.TRACK_LEFT_CC && data2 > 0)
         {
            padBank.scrollBy(-PADS_PER_BANK);
            log("Pad bank scroll left (MIDI)");
            return true;
         }
         if (data1 == DrumMapping.TRACK_RIGHT_CC && data2 > 0)
         {
            padBank.scrollBy(PADS_PER_BANK);
            log("Pad bank scroll right (MIDI)");
            return true;
         }
         if (data1 == DrumMapping.SEND_UP_CC || data1 == DrumMapping.SEND_DOWN_CC)
         {
            // No-op for now; keep turning off factory LED actions
            return true;
         }
      }
      else if (message == 0x90 || message == 0x80) // note on/off
      {
         final int topIndex = indexOf(DrumMapping.TOP_NOTES, data1);
         if (topIndex >= 0 && data2 > 0)
         {
            final DrumPad pad = padBank.getItemAt(topIndex);
            selectedPadIndex = topIndex;
            pad.selectInEditor();
            if (auditionOnSelect)
            {
               playPad(pad, topIndex, 100);
               log("Select+trigger pad " + topIndex + " (note)");
            }
            else
            {
               log("Select pad " + topIndex + " (note)");
            }
            return true;
         }

         final int index = indexOf(DrumMapping.BOTTOM_NOTES, data1);
         if (index >= 0)
         {
            if (data2 > 0)
               handleBottomButton(index);
            else
               releaseBottomButton(index);
            return true;
         }
      }
      return false;
   }

   private void forEachPad(final PadConsumer consumer)
   {
      for (int i = 0; i < PADS_PER_BANK; i++)
      {
         consumer.accept(i, padBank.getItemAt(i));
      }
   }

   @FunctionalInterface
   private interface PadConsumer
   {
      void accept(int index, DrumPad pad);
   }

   private static int indexOf(final int[] values, final int value)
   {
      for (int i = 0; i < values.length; i++)
      {
         if (values[i] == value)
         {
            return i;
         }
      }
      return -1;
   }

   private void setKnobValue(final int padIndex, final int parameterIndex, final int midiValue)
   {
      final RemoteControlsPage rc = padRemoteControls[padIndex];
      if (rc == null)
      {
         return;
      }
      rc.getParameter(parameterIndex).value().set(midiValue, 127);
      log("Knob row " + parameterIndex + " pad " + padIndex + " -> " + midiValue);
   }

   /**
    * Handle a bottom-row button press. When the global track control mode is {@link TrackControl#Mute}
    * the button toggles the pad mute/solo. Otherwise it toggles the pad's fourth remote parameter
    * (useful for accent/roll assignments).
    */
   private void handleBottomButton(final int padIndex)
   {
      if (soloMode || trackControlMode == TrackControl.Mute)
      {
         final DrumPad pad = padBank.getItemAt(padIndex);
         if (pad.exists().get())
            toggleMuteOrSolo(pad);
      }
      else
      {
         if (accentMomentary)
            setPadAccent(padIndex, true);
         else
            togglePadAccent(padIndex);
      }
   }

   private void releaseBottomButton(final int padIndex)
   {
      if (soloMode || trackControlMode == TrackControl.Mute || !accentMomentary)
         return;
      setPadAccent(padIndex, false);
   }

   private void setPadAccent(final int padIndex, final boolean enable)
   {
      final RemoteControlsPage rc = padRemoteControls[padIndex];
      if (rc == null)
         return;
      final RemoteControl param = rc.getParameter(3);
      if (!param.exists().get())
         return;
      // Force the target regardless of current value to support true momentary behaviour.
      param.value().set(enable ? 127 : 0, 127);
      log("Pad accent " + (enable ? "on" : "off") + " pad=" + padIndex);
   }

   private void togglePadAccent(final int padIndex)
   {
      final RemoteControlsPage rc = padRemoteControls[padIndex];
      if (rc == null)
         return;
      final RemoteControl param = rc.getParameter(3);
      if (!param.exists().get())
         return;
      final double current = param.value().get();
      final boolean enable = current <= 0;
      param.value().set(enable ? 127 : 0, 127);
      log("Pad accent toggle pad=" + padIndex + " -> " + (enable ? "on" : "off"));
   }

   /** External entry point to keep the drum-layer track control mode in sync with the main surface. */
   void setTrackControlMode(final TrackControl trackControl)
   {
      this.trackControlMode = trackControl == null ? TrackControl.None : trackControl;
   }

   TrackControl getTrackControlMode()
   {
      return this.trackControlMode;
   }

   int getSelectedPadIndex()
   {
      return this.selectedPadIndex;
   }

   void setAuditionOnSelect(final boolean auditionOnSelect)
   {
      this.auditionOnSelect = auditionOnSelect;
   }

   void setAccentMomentary(final boolean accentMomentary)
   {
      this.accentMomentary = accentMomentary;
   }

   private void triggerMacro(final int macroIndex)
   {
      // Placeholder for future macro routing; currently consumes the event without external action.
      log("Macro button " + macroIndex + " pressed");
   }

   private void log(final String message)
   {
      if (DEBUG_DRUM)
      {
         host.println("[LCXL-DRUM] " + message);
      }
   }
}
