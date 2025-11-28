package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extensions.controllers.novation.launch_control_xl.drum.DrumMapping;

/**
 * Encapsulates hardware matcher wiring for different modes (factory/drum/arp).
 */
public final class HardwareBindingManager
{
   private final MidiIn midiIn;
   private final AbsoluteHardwareKnob[] knobs;
   private final HardwareSlider[] sliders;
   private final HardwareButton[] trackFocusButtons;
   private final HardwareButton[] trackControlButtons;
   private final HardwareButton sendUp;
   private final HardwareButton sendDown;
   private final HardwareButton trackLeft;
   private final HardwareButton trackRight;
   private final HardwareButton device;
   private final HardwareButton mute;
   private final HardwareButton solo;
   private final HardwareButton recordArm;
   private final int[] knobCcNumbers;
   private final int[] sliderCcNumbers;

   public HardwareBindingManager(final MidiIn midiIn,
                                 final AbsoluteHardwareKnob[] knobs,
                                 final HardwareSlider[] sliders,
                                 final HardwareButton[] trackFocusButtons,
                                 final HardwareButton[] trackControlButtons,
                                 final HardwareButton sendUp,
                                 final HardwareButton sendDown,
                                 final HardwareButton trackLeft,
                                 final HardwareButton trackRight,
                                 final HardwareButton device,
                                 final HardwareButton mute,
                                 final HardwareButton solo,
                                 final HardwareButton recordArm,
                                 final int[] knobCcNumbers,
                                 final int[] sliderCcNumbers)
   {
      this.midiIn = midiIn;
      this.knobs = knobs;
      this.sliders = sliders;
      this.trackFocusButtons = trackFocusButtons;
      this.trackControlButtons = trackControlButtons;
      this.sendUp = sendUp;
      this.sendDown = sendDown;
      this.trackLeft = trackLeft;
      this.trackRight = trackRight;
      this.device = device;
      this.mute = mute;
      this.solo = solo;
      this.recordArm = recordArm;
      this.knobCcNumbers = knobCcNumbers;
      this.sliderCcNumbers = sliderCcNumbers;
   }

   public void attachFactory(final int channel, final int[] trackFocusNotes, final int[] trackControlNotes,
                             final int deviceNote, final int muteNote, final int soloNote, final int recordArmNote,
                             final int sendUpCc, final int sendDownCc, final int trackLeftCc, final int trackRightCc,
                             final int modeButtonChannel)
   {
      bindKnobsFactory(channel);
      bindSlidersFactory(channel);
      bindTransportAndTrack(channel, sendUpCc, sendDownCc, trackLeftCc, trackRightCc);
      bindNotes(channel, trackFocusNotes, trackControlNotes, deviceNote, muteNote, soloNote, recordArmNote, modeButtonChannel);
   }

   public void attachDrum(final int channel,
                          final int[] trackFocusNotes,
                          final int[] trackControlNotes,
                          final int deviceNote,
                          final int muteNote,
                          final int soloNote,
                          final int recordArmNote,
                          final int modeButtonChannel)
   {
      bindKnobsDrum(channel);
      bindSlidersDrum(channel);
      bindTransportAndTrack(channel, DrumMapping.SEND_UP_CC, DrumMapping.SEND_DOWN_CC, DrumMapping.TRACK_LEFT_CC, DrumMapping.TRACK_RIGHT_CC);
      bindNotes(channel, trackFocusNotes, trackControlNotes, deviceNote, muteNote, soloNote, recordArmNote, modeButtonChannel);
   }

   public void clear()
   {
      for (final AbsoluteHardwareKnob knob : this.knobs)
      {
         if (knob != null)
            knob.setAdjustValueMatcher(null);
      }
      for (final HardwareSlider slider : this.sliders)
      {
         if (slider != null)
            slider.setAdjustValueMatcher(null);
      }
      bindCC(this.sendUp, null, 0);
      bindCC(this.sendDown, null, 0);
      bindCC(this.trackLeft, null, 0);
      bindCC(this.trackRight, null, 0);
      for (final HardwareButton button : this.trackFocusButtons)
         bindNote(button, null, 0);
      for (final HardwareButton button : this.trackControlButtons)
         bindNote(button, null, 0);
      bindNote(this.device, null, 0);
      bindNote(this.mute, null, 0);
      bindNote(this.solo, null, 0);
      bindNote(this.recordArm, null, 0);
   }

   private void bindKnobsFactory(final int channel)
   {
      for (int i = 0; i < this.knobs.length; i++)
      {
         final AbsoluteHardwareKnob knob = this.knobs[i];
         if (knob != null)
            knob.setAdjustValueMatcher(this.midiIn.createAbsoluteCCValueMatcher(channel, this.knobCcNumbers[i]));
      }
   }

   private void bindKnobsDrum(final int channel)
   {
      for (int i = 0; i < this.knobs.length; i++)
      {
         final AbsoluteHardwareKnob knob = this.knobs[i];
         if (knob == null)
            continue;
         if (i < 8)
            knob.setAdjustValueMatcher(this.midiIn.createAbsoluteCCValueMatcher(channel, DrumMapping.KNOB_ROW1_CCS[i]));
         else if (i < 16)
            knob.setAdjustValueMatcher(this.midiIn.createAbsoluteCCValueMatcher(channel, DrumMapping.KNOB_ROW2_CCS[i - 8]));
         else
            knob.setAdjustValueMatcher(this.midiIn.createAbsoluteCCValueMatcher(channel, DrumMapping.KNOB_ROW3_CCS[i - 16]));
      }
   }

   private void bindSlidersFactory(final int channel)
   {
      for (int i = 0; i < this.sliders.length; i++)
      {
         final HardwareSlider slider = this.sliders[i];
         if (slider != null)
            slider.setAdjustValueMatcher(this.midiIn.createAbsoluteCCValueMatcher(channel, this.sliderCcNumbers[i]));
      }
   }

   private void bindSlidersDrum(final int channel)
   {
      for (int i = 0; i < this.sliders.length; i++)
      {
         final HardwareSlider slider = this.sliders[i];
         if (slider != null)
            slider.setAdjustValueMatcher(this.midiIn.createAbsoluteCCValueMatcher(channel, DrumMapping.SLIDER_CCS[i]));
      }
   }

   private void bindTransportAndTrack(final int channel, final int sendUpCc, final int sendDownCc,
      final int trackLeftCc, final int trackRightCc)
   {
      bindCC(this.sendUp, sendUpCc, channel);
      bindCC(this.sendDown, sendDownCc, channel);
      bindCC(this.trackLeft, trackLeftCc, channel);
      bindCC(this.trackRight, trackRightCc, channel);
   }

   private void bindNotes(final int channel, final int[] trackFocusNotes, final int[] trackControlNotes,
      final int deviceNote, final int muteNote, final int soloNote, final int recordArmNote, final int modeButtonChannel)
   {
      for (int i = 0; i < this.trackFocusButtons.length; i++)
         bindNote(this.trackFocusButtons[i], trackFocusNotes[i], channel);
      for (int i = 0; i < this.trackControlButtons.length; i++)
         bindNote(this.trackControlButtons[i], trackControlNotes[i], channel);

      bindNote(this.device, deviceNote, modeButtonChannel);
      bindNote(this.mute, muteNote, modeButtonChannel);
      bindNote(this.solo, soloNote, modeButtonChannel);
      bindNote(this.recordArm, recordArmNote, modeButtonChannel);
   }

   private void bindCC(final HardwareButton button, final Integer cc, final Integer channel)
   {
      if (button == null)
         return;
      if (cc == null || channel == null)
      {
         button.pressedAction().setActionMatcher(null);
         button.releasedAction().setActionMatcher(null);
         return;
      }
      button.pressedAction().setActionMatcher(this.midiIn.createCCActionMatcher(channel.intValue(), cc.intValue(), 127));
      button.releasedAction().setActionMatcher(this.midiIn.createCCActionMatcher(channel.intValue(), cc.intValue(), 0));
   }

   private void bindNote(final HardwareButton button, final Integer note, final Integer channel)
   {
      if (button == null)
         return;
      if (note == null || channel == null)
      {
         button.pressedAction().setActionMatcher(null);
         button.releasedAction().setActionMatcher(null);
         return;
      }
      button.pressedAction().setActionMatcher(this.midiIn.createNoteOnActionMatcher(channel.intValue(), note.intValue()));
      button.releasedAction().setActionMatcher(this.midiIn.createNoteOffActionMatcher(channel.intValue(), note.intValue()));
   }
}
