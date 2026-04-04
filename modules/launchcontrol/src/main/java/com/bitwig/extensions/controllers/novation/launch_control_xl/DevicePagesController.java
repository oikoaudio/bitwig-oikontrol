package com.bitwig.extensions.controllers.novation.launch_control_xl;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extensions.controllers.novation.common.SimpleLed;
import com.bitwig.extensions.controllers.novation.common.SimpleLedColor;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.LedUtil;
import com.bitwig.extensions.rh.Midi;

/**
 * Handles the device-focused user template (hardware User 6, channel 5) by mapping controls
 * directly to fixed remote pages and consuming the MIDI so it does not leak to Bitwig mappings.
 */
final class DevicePagesController
{
   static final int USER_TEMPLATE_CHANNEL = 5; // zero-based channel for hardware User 6

   private final CursorRemoteControlsPage[] devicePages;
   private final AbsoluteHardwareKnob[] knobs;
   private final HardwareSlider[] sliders;
   private final SimpleLed[] knobLeds;
   private final SimpleLed[] bottomButtonLeds;

   DevicePagesController(final CursorRemoteControlsPage[] devicePages,
                         final AbsoluteHardwareKnob[] knobs,
                         final HardwareSlider[] sliders,
                         final SimpleLed[] knobLeds,
                         final SimpleLed[] bottomButtonLeds)
   {
      this.devicePages = devicePages;
      this.knobs = knobs;
      this.sliders = sliders;
      this.knobLeds = knobLeds;
      this.bottomButtonLeds = bottomButtonLeds;
   }

   boolean handleMidi(final int status, final int data1, final int data2)
   {
      final int channel = status & 0x0F;
      if (channel != USER_TEMPLATE_CHANNEL)
      {
         return false;
      }
      final int message = status & 0xF0;
      if (message == Midi.CC)
      {
         handleCc(data1, data2);
         return true;
      }
      if (message == Midi.NOTE_ON && data2 > 0)
      {
         handleNote(data1);
         return true;
      }
      if (message == Midi.NOTE_OFF)
      {
         return true;
      }
      return false;
   }

   void paintKnobs()
   {
      final int off = SimpleLedColor.Off.value();
      final int amberLow = SimpleLedColor.AmberLow.value();
      final int amber = SimpleLedColor.Amber.value();

      for (int i = 0; i < 8; ++i)
      {
         setLedForRemote(knobLeds[i], devicePages[0].getParameter(i), off, amberLow, amber);
         setLedForRemote(knobLeds[8 + i], devicePages[1].getParameter(i), off, amberLow, amber);
         setLedForRemote(knobLeds[16 + i], devicePages[2].getParameter(i), off, amberLow, amber);
      }
   }

   void paintButtons()
   {
      final int off = SimpleLedColor.Off.value();
      final int amberLow = SimpleLedColor.AmberLow.value();
      final int amber = SimpleLedColor.Amber.value();

      for (int i = 0; i < 8; ++i)
      {
         setLedForRemote(bottomButtonLeds[i], devicePages[4].getParameter(i), off, amberLow, amber);
         setLedForRemote(bottomButtonLeds[8 + i], devicePages[5].getParameter(i), off, amberLow, amber);
      }
   }

   private void handleCc(final int cc, final int value)
   {
      final Integer row0Index = knobIndex(cc, LaunchControlXlControllerExtension.KNOB_CC_OFFSETS[0]);
      if (row0Index != null)
      {
         LaunchControlXlControllerExtension.logDebug("[LCXL-DP] row0 idx=" + row0Index + " cc=" + cc + " val=" + value);
         setRemoteValue(devicePages[0].getParameter(row0Index), value);
         return;
      }
      final Integer row1Index = knobIndex(cc, LaunchControlXlControllerExtension.KNOB_CC_OFFSETS[1]);
      if (row1Index != null)
      {
         LaunchControlXlControllerExtension.logDebug("[LCXL-DP] row1 idx=" + row1Index + " cc=" + cc + " val=" + value);
         setRemoteValue(devicePages[1].getParameter(row1Index), value);
         return;
      }
      final Integer row2Index = knobIndex(cc, LaunchControlXlControllerExtension.KNOB_CC_OFFSETS[2]);
      if (row2Index != null)
      {
         LaunchControlXlControllerExtension.logDebug("[LCXL-DP] row2 idx=" + row2Index + " cc=" + cc + " val=" + value);
         setRemoteValue(devicePages[2].getParameter(row2Index), value);
         return;
      }
      final Integer slider = sliderIndex(cc);
      if (slider != null)
      {
         LaunchControlXlControllerExtension.logDebug("[LCXL-DP] fader idx=" + slider + " cc=" + cc + " val=" + value);
         setRemoteValue(devicePages[3].getParameter(slider), value);
      }
   }

   private void handleNote(final int note)
   {
      final int focusIndex = indexOf(LaunchControlXlControllerExtension.TRACK_FOCUS_NOTES, note);
      if (focusIndex >= 0)
      {
         toggleRemoteParameter(devicePages[4].getParameter(focusIndex));
         return;
      }
      final int controlIndex = indexOf(LaunchControlXlControllerExtension.TRACK_CONTROL_NOTES, note);
      if (controlIndex >= 0)
      {
         toggleRemoteParameter(devicePages[5].getParameter(controlIndex));
      }
   }

   private void setRemoteValue(final RemoteControl param, final double value)
   {
      if (param == null || !param.exists().get())
      {
         return;
      }
      param.value().set(value, 127);
   }

   private void toggleRemoteParameter(final RemoteControl param)
   {
      if (param == null || !param.exists().get())
      {
         return;
      }
      final double current = param.value().get();
      param.value().set(current > 0 ? 0 : 127, 127);
   }

   private void setLedForRemote(final SimpleLed led, final RemoteControl param, final int off, final int mid, final int high)
   {
      final boolean exists = param != null && param.exists().get();
      final double value = exists ? param.value().get() : 0;
      led.setColor(exists ? LedUtil.levelColor(value, off, mid, high) : off);
   }

   private static Integer knobIndex(final int cc, final int base)
   {
      final int offset = cc - base;
      if (0 <= offset && offset < 8)
      {
         return offset;
      }
      return null;
   }

   private static Integer sliderIndex(final int cc)
   {
      final int offset = cc - LaunchControlXlControllerExtension.SLIDER_CC_BASE;
      if (0 <= offset && offset < 8)
      {
         return offset;
      }
      return null;
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
}
