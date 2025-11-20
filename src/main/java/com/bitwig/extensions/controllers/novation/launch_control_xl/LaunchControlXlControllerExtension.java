package com.bitwig.extensions.controllers.novation.launch_control_xl;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extensions.controllers.novation.common.SimpleLed;
import com.bitwig.extensions.controllers.novation.common.SimpleLedColor;
import com.bitwig.extensions.controllers.novation.launch_control_xl.arp.RhArpLayerController;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.TemplateChangeMessageParser;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.UserModeNoteInputInstaller;
import com.bitwig.extensions.rh.Midi;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.OptionalInt;

public class LaunchControlXlControllerExtension extends ControllerExtension
{
   private static final boolean DEBUG_TELEMETRY = false;
   private static void debug(final ControllerHost host, final String message)
   {
      if (DEBUG_TELEMETRY)
      {
         host.println(message);
      }
   }

   private static final int[] TRACK_FOCUS_NOTES = {41, 42, 43, 44, 57, 58, 59, 60};
   private static final int[] TRACK_CONTROL_NOTES = {73, 74, 75, 76, 89, 90, 91, 92};
   private static final int[] KNOB_CC_OFFSETS = {13, 29, 49};
   private static final int SLIDER_CC_BASE = 77;
   private static final int SEND_UP_CC = 104;
   private static final int SEND_DOWN_CC = 105;
   private static final int TRACK_LEFT_CC = 106;
   private static final int TRACK_RIGHT_CC = 107;
   private static final int DEVICE_NOTE = 105;
   private static final int MUTE_NOTE = 106;
   private static final int SOLO_NOTE = 107;
   private static final int RECORD_ARM_NOTE = 108;
   private static final int ARP_USER_TEMPLATE_ID = 7;

   // Identify possible modes
   enum Mode
   {
      Send2FullDevice(8, "Switched to 2 Sends and Selected DEVICE Controls Mode"),
      Send2Device1(9, "Switched to 2 Sends and 1 per Channel DEVICE Control Mode"),
      Send2Project(10, "Switched to 2 Sends and PROJECT Controls Mode"),
      Send3(11, "Switched to 3 Sends Mode"),
      Send1Device2(12, "Switched to 1 Send and 2 per Channel DEVICE Controls Mode"),
      Device3(13, "Switched to per Channel DEVICE Controls Mode"),
      Track3(15, "Switched to per Channel TRACK Controls Mode"),

      None(0, "Unsupported Template. We provide Modes for the Factory Template 1 to 8, except 7."),
      Send2Pan1(0, "Switched to 2 Sends and Pan Mode");

      Mode(final int channel, final String notification)
      {
         mChannel = channel; mNotification = notification;
      }

      public String getNotification()
      {
         return mNotification;
      }

      public int getChannel()
      {
         return mChannel;
      }

      public String getHexChannel()
      {
         return Integer.toHexString(mChannel);
      }

      private final String mNotification;
      private final int mChannel;
   }

   enum TrackControl
   {
      Mute,
      Solo,
      RecordArm
   }

   public LaunchControlXlControllerExtension(
      final LaunchControlXlControllerExtensionDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      mHost = getHost();

      mMidiIn = mHost.getMidiInPort(0);
      mMidiOut = mHost.getMidiOutPort(0);
      mArpLayerController = new RhArpLayerController(mHost);
      mUserModeNoteInput = UserModeNoteInputInstaller.ensureUserModeInput(mMidiIn, ARP_USER_TEMPLATE_ID);

      mMidiIn.setSysexCallback(this::onSysex);
      mMidiIn.setMidiCallback(this::handleIncomingMidi);

      initializeDeviceWithMode(Mode.Send2FullDevice);

      mCursorTrack = mHost.createCursorTrack("cursor-track", "Launch Control XL Track Cursor", 0, 0, true);
      mCursorDevice = mCursorTrack.createCursorDevice();
      mCursorDevice.hasNext().markInterested();
      mCursorDevice.hasPrevious().markInterested();
      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);
      mRemoteControls.setHardwareLayout(HardwareControlType.KNOB, 8);
      mRemoteControls.selectedPageIndex().markInterested();
      mRemoteControls.pageCount().markInterested();

      final Project project = mHost.getProject();
      final Track rootTrackGroup = project.getRootTrackGroup();
      mProjectRemoteControlsCursor = rootTrackGroup.createCursorRemoteControlsPage("project-remotes", 8, null);

      for (int i = 0; i < 8; ++i)
      {
         markParameterInterested(mRemoteControls.getParameter(i));
         markParameterInterested(mProjectRemoteControlsCursor.getParameter(i));
      }

      mTrackBank = mHost.createMainTrackBank(8, 3, 0);
      mTrackBank.followCursorTrack(mCursorTrack);
      mTrackBank.canScrollBackwards().markInterested();
      mTrackBank.canScrollForwards().markInterested();

      mTrackBank.cursorIndex().markInterested();
      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         track.solo().markInterested();
         track.arm().markInterested();
         track.mute().markInterested();
         track.volume().markInterested();
         track.exists().markInterested();

         final SendBank sendBank = track.sendBank();
         sendBank.canScrollBackwards().markInterested();
         sendBank.canScrollForwards().markInterested();
         for (int j = 0; j < 3; ++j)
         {
            sendBank.getItemAt(j).exists().markInterested();
            sendBank.getItemAt(j).value().markInterested();
         }

         mTrackDeviceCursors[i] = track.createCursorDevice();
         mTrackCursorDeviceRemoteControls[i] = mTrackDeviceCursors[i].createCursorRemoteControlsPage(3);
         mTrackCursorDeviceRemoteControls[i].setHardwareLayout(HardwareControlType.KNOB, 1);

         mTrackRemoteControls[i] = track.createCursorRemoteControlsPage(3);

         for (int j = 0; j < 3; ++j)
         {
            markParameterInterested(mTrackCursorDeviceRemoteControls[i].getParameter(j));
            markParameterInterested(mTrackRemoteControls[i].getParameter(j));
         }
      }

      createHardwareSurface();
      createLayers();

      mMainLayer.activate();
      selectMode(Mode.Send2FullDevice);
      setTrackControl(TrackControl.Mute);
      setDeviceOn(false);
   }

   private void initializeDeviceWithMode(final Mode mode)
   {
      mMidiOut.sendSysex("f00020290211770" + mode.getHexChannel() + "f7");
      mIgnoreNextSysex = true;
   }

   private static void markParameterInterested(final RemoteControl parameter)
   {
      parameter.markInterested();
      parameter.exists().markInterested();
      parameter.value().markInterested();
   }

   private static int levelColor(final double value, final int offColor, final int dimColor, final int brightColor)
   {
      final double normalized = Math.max(0, Math.min(1, value));
      if (normalized < 0.02)
      {
         return offColor;
      }
      if (normalized < 0.5)
      {
         return dimColor;
      }
      return brightColor;
   }

   private void createHardwareSurface()
   {
      mHardwareSurface = mHost.createHardwareSurface();
      for (int i = 0; i < 8; ++i)
      {
         for (int j = 0; j < 3; ++j)
         {
            final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("knob-" + i + "-" + j);
            knob.setIndexInGroup(i);
            final int index = 8 * j + i;
            mHardwareKnobs[index] = knob;
            mKnobCcNumbers[index] = KNOB_CC_OFFSETS[j] + i;
         }
         mHardwareSliders[i] = mHardwareSurface.createHardwareSlider("slider-" + i);
         mHardwareSliders[i].setIndexInGroup(i);
         mSliderCcNumbers[i] = SLIDER_CC_BASE + i;
      }

      mBtSendUp = mHardwareSurface.createHardwareButton("bt-send-up");
      setCcButtonMatcher(mBtSendUp, SEND_UP_CC, 0);

      mBtSendDown = mHardwareSurface.createHardwareButton("bt-send-down");
      setCcButtonMatcher(mBtSendDown, SEND_DOWN_CC, 0);

      mBtTrackLeft = mHardwareSurface.createHardwareButton("bt-track-left");
      setCcButtonMatcher(mBtTrackLeft, TRACK_LEFT_CC, 0);

      mBtTrackRight = mHardwareSurface.createHardwareButton("bt-track-right");
      setCcButtonMatcher(mBtTrackRight, TRACK_RIGHT_CC, 0);

      for (int i = 0; i < mBtTrackFocus.length; i++)
      {
         mBtTrackFocus[i] = createHardwareButtonWithNote("bt-track-focus-" + i, TRACK_FOCUS_NOTES[i], i);
      }

      for (int i = 0; i < mBtTrackControl.length; i++)
      {
         mBtTrackControl[i] = createHardwareButtonWithNote("bt-track-control-" + i, TRACK_CONTROL_NOTES[i], i);
      }

      mBtDevice = createHardwareButtonWithNote("bt-device", DEVICE_NOTE, 0);
      mBtMute = createHardwareButtonWithNote("bt-mute", MUTE_NOTE, 0);
      mBtSolo = createHardwareButtonWithNote("bt-solo", SOLO_NOTE, 0);
      mBtRecordArm = createHardwareButtonWithNote("bt-record-arm", RECORD_ARM_NOTE, 0);

      attachHardwareMatchers();
   }

   private HardwareButton createHardwareButtonWithNote(final String id, final int note, final int indexInGroup)
   {
      final HardwareButton bt = mHardwareSurface.createHardwareButton(id);
      bt.setIndexInGroup(indexInGroup);
      setNoteButtonActionMatchers(bt, note, 0);
      return bt;
   }

   private void setNoteButtonActionMatchers(final HardwareButton button, final int note, final Integer channel)
   {
      if (channel == null)
      {
         button.pressedAction().setActionMatcher(null);
         button.releasedAction().setActionMatcher(null);
         return;
      }
      button.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(channel, note));
      button.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(channel, note));
   }

   private void setCcButtonMatcher(final HardwareButton button, final int cc, final Integer channel)
   {
      if (channel == null)
      {
         button.pressedAction().setActionMatcher(null);
         return;
      }
      button.pressedAction().setActionMatcher(mMidiIn.createCCActionMatcher(channel, cc, 127));
   }

   private void attachHardwareMatchers()
   {
      final int channel = mCurrentTemplateChannel;
      debug(mHost, "[LCXL] attachHardwareMatchers mode=" + mMode + " channel=" + channel +
         " factoryActive=" + mFactoryTemplateActive + " arpActive=" + mArpLayerActive);
      for (int i = 0; i < mHardwareKnobs.length; i++)
      {
         final AbsoluteHardwareKnob knob = mHardwareKnobs[i];
         if (knob != null)
         {
            knob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(channel, mKnobCcNumbers[i]));
         }
      }
      for (int i = 0; i < mHardwareSliders.length; i++)
      {
         final HardwareSlider slider = mHardwareSliders[i];
         if (slider != null)
         {
            slider.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(channel, mSliderCcNumbers[i]));
         }
      }
      setCcButtonMatcher(mBtSendUp, SEND_UP_CC, channel);
      setCcButtonMatcher(mBtSendDown, SEND_DOWN_CC, channel);
      setCcButtonMatcher(mBtTrackLeft, TRACK_LEFT_CC, channel);
      setCcButtonMatcher(mBtTrackRight, TRACK_RIGHT_CC, channel);
      for (int i = 0; i < mBtTrackFocus.length; i++)
      {
         setNoteButtonActionMatchers(mBtTrackFocus[i], TRACK_FOCUS_NOTES[i], channel);
      }
      for (int i = 0; i < mBtTrackControl.length; i++)
      {
         setNoteButtonActionMatchers(mBtTrackControl[i], TRACK_CONTROL_NOTES[i], channel);
      }
      setNoteButtonActionMatchers(mBtDevice, DEVICE_NOTE, channel);
      setNoteButtonActionMatchers(mBtMute, MUTE_NOTE, channel);
      setNoteButtonActionMatchers(mBtSolo, SOLO_NOTE, channel);
      setNoteButtonActionMatchers(mBtRecordArm, RECORD_ARM_NOTE, channel);
   }

   private void clearHardwareMatchers()
   {
      debug(mHost, "[LCXL] clearHardwareMatchers");
      for (final AbsoluteHardwareKnob knob : mHardwareKnobs)
      {
         if (knob != null)
         {
            knob.setAdjustValueMatcher(null);
         }
      }
      for (final HardwareSlider slider : mHardwareSliders)
      {
         if (slider != null)
         {
            slider.setAdjustValueMatcher(null);
         }
      }
      setCcButtonMatcher(mBtSendUp, SEND_UP_CC, null);
      setCcButtonMatcher(mBtSendDown, SEND_DOWN_CC, null);
      setCcButtonMatcher(mBtTrackLeft, TRACK_LEFT_CC, null);
      setCcButtonMatcher(mBtTrackRight, TRACK_RIGHT_CC, null);
      for (int i = 0; i < mBtTrackFocus.length; i++)
      {
         setNoteButtonActionMatchers(mBtTrackFocus[i], TRACK_FOCUS_NOTES[i], null);
      }
      for (int i = 0; i < mBtTrackControl.length; i++)
      {
         setNoteButtonActionMatchers(mBtTrackControl[i], TRACK_CONTROL_NOTES[i], null);
      }
      setNoteButtonActionMatchers(mBtDevice, DEVICE_NOTE, null);
      setNoteButtonActionMatchers(mBtMute, MUTE_NOTE, null);
      setNoteButtonActionMatchers(mBtSolo, SOLO_NOTE, null);
      setNoteButtonActionMatchers(mBtRecordArm, RECORD_ARM_NOTE, null);
   }

   private void createLayers()
   {
      final Layers layers = new Layers(this);
      mMainLayer = new Layer(layers, "Main");

      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         mMainLayer.bind(mHardwareSliders[i], track.volume());
         mMainLayer.bindPressed(mBtTrackFocus[i], () -> mCursorTrack.selectChannel(track));
      }

      mMainLayer.bindPressed(mBtSendUp, () -> {
         for (int i = 0; i < 8; ++i)
            mTrackBank.getItemAt(i).sendBank().scrollBackwards();
      });
      mMainLayer.bindPressed(mBtSendDown, () -> {
         for (int i = 0; i < 8; ++i)
            mTrackBank.getItemAt(i).sendBank().scrollForwards();
      });
      mMainLayer.bindPressed(mBtTrackLeft, mTrackBank.scrollBackwardsAction());
      mMainLayer.bindPressed(mBtTrackRight, mTrackBank.scrollForwardsAction());
      mMainLayer.bindPressed(mBtDevice, () -> setDeviceOn(true));
      mMainLayer.bindReleased(mBtDevice, () -> setDeviceOn(false));
      mMainLayer.bindPressed(mBtMute, () -> setTrackControl(TrackControl.Mute));
      mMainLayer.bindPressed(mBtSolo, () -> setTrackControl(TrackControl.Solo));
      mMainLayer.bindPressed(mBtRecordArm, () -> setTrackControl(TrackControl.RecordArm));

      createModeLayers(layers);
      createTrackControlsLayers(layers);
      createDeviceLayer(layers);
   }

   private void createDeviceLayer(final Layers layers)
   {
      mDeviceLayer = new Layer(layers, "Device");
      mDeviceLayer.bindPressed(mBtTrackLeft, mCursorDevice.selectPreviousAction());
      mDeviceLayer.bindPressed(mBtTrackRight, mCursorDevice.selectNextAction());

      for (int i = 0; i < 8; ++i)
      {
         final int I = i;
         mDeviceLayer.bindPressed(mBtTrackControl[i], () -> mRemoteControls.selectedPageIndex().set(I));
      }
   }

   private void createModeLayers(final Layers layers)
   {
      mSend2FullDeviceLayer = new Layer(layers, "2 Sends Full Device");
      for (int i = 0; i < 8; ++i)
      {
         final SendBank sendBank = mTrackBank.getItemAt(i).sendBank();
         mSend2FullDeviceLayer.bind(mHardwareKnobs[i], sendBank.getItemAt(0));
         mSend2FullDeviceLayer.bind(mHardwareKnobs[8 + i], sendBank.getItemAt(1));
         mSend2FullDeviceLayer.bind(mHardwareKnobs[16 + i], mRemoteControls.getParameter(i));
      }

      mSend2ProjectLayer = new Layer(layers, "2 Sends and Project Remotes");
      for (int i = 0; i < 8; ++i)
      {
         final SendBank sendBank = mTrackBank.getItemAt(i).sendBank();
         mSend2ProjectLayer.bind(mHardwareKnobs[i], sendBank.getItemAt(0));
         mSend2ProjectLayer.bind(mHardwareKnobs[8 + i], sendBank.getItemAt(1));
         mSend2ProjectLayer.bind(mHardwareKnobs[16 + i], mProjectRemoteControlsCursor.getParameter(i));
      }

      mSend2Device1Layer = new Layer(layers, "2 Sends 1 Device");
      for (int i = 0; i < 8; ++i)
      {
         final SendBank sendBank = mTrackBank.getItemAt(i).sendBank();
         mSend2Device1Layer.bind(mHardwareKnobs[i], sendBank.getItemAt(0));
         mSend2Device1Layer.bind(mHardwareKnobs[8 + i], sendBank.getItemAt(1));
         mSend2Device1Layer.bind(mHardwareKnobs[16 + i], mTrackCursorDeviceRemoteControls[i].getParameter(0));
      }

      mSend1Device2Layer = new Layer(layers, "1 Sends 2 Device");
      for (int i = 0; i < 8; ++i)
      {
         final SendBank sendBank = mTrackBank.getItemAt(i).sendBank();
         mSend1Device2Layer.bind(mHardwareKnobs[i], sendBank.getItemAt(0));
         mSend1Device2Layer.bind(mHardwareKnobs[8 + i], mTrackCursorDeviceRemoteControls[i].getParameter(0));
         mSend1Device2Layer.bind(mHardwareKnobs[16 + i], mTrackCursorDeviceRemoteControls[i].getParameter(1));
      }

      mDevice3Layer = new Layer(layers, "3 Device");
      for (int i = 0; i < 8; ++i)
      {
         mDevice3Layer.bind(mHardwareKnobs[i], mTrackCursorDeviceRemoteControls[i].getParameter(0));
         mDevice3Layer.bind(mHardwareKnobs[8 + i], mTrackCursorDeviceRemoteControls[i].getParameter(1));
         mDevice3Layer.bind(mHardwareKnobs[16 + i], mTrackCursorDeviceRemoteControls[i].getParameter(2));
      }

      mSend2Pan1Layer = new Layer(layers, "2 Sends 1 Pan");
      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         final SendBank sendBank = track.sendBank();
         mSend2Pan1Layer.bind(mHardwareKnobs[i], sendBank.getItemAt(0));
         mSend2Pan1Layer.bind(mHardwareKnobs[8 + i], sendBank.getItemAt(1));
         mSend2Pan1Layer.bind(mHardwareKnobs[16 + i], track.pan());
      }

      mSend3Layer = new Layer(layers, "3 Sends");
      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         final SendBank sendBank = track.sendBank();
         mSend3Layer.bind(mHardwareKnobs[i], sendBank.getItemAt(0));
         mSend3Layer.bind(mHardwareKnobs[8 + i], sendBank.getItemAt(1));
         mSend3Layer.bind(mHardwareKnobs[16 + i], sendBank.getItemAt(2));
      }

      mTrack3layer = new Layer(layers, "3 Track Remotes");
      for (int i = 0; i < 8; ++i)
      {
         final CursorRemoteControlsPage remoteControlPage = mTrackRemoteControls[i];
         mTrack3layer.bind(mHardwareKnobs[i], remoteControlPage.getParameter(0));
         mTrack3layer.bind(mHardwareKnobs[8 + i], remoteControlPage.getParameter(1));
         mTrack3layer.bind(mHardwareKnobs[16 + i], remoteControlPage.getParameter(2));
      }
   }

   private void createTrackControlsLayers(final Layers layers)
   {
      mMuteLayer = new Layer(layers, "Mute");
      for (int i = 0; i < 8; ++i)
         mMuteLayer.bindToggle(mBtTrackControl[i], mTrackBank.getItemAt(i).mute());

      mSoloLayer = new Layer(layers, "Solo");
      for (int i = 0; i < 8; ++i)
         mSoloLayer.bindToggle(mBtTrackControl[i], mTrackBank.getItemAt(i).solo());

      mRecordArmLayer = new Layer(layers, "Record Arm");
      for (int i = 0; i < 8; ++i)
         mRecordArmLayer.bindToggle(mBtTrackControl[i], mTrackBank.getItemAt(i).arm());
   }

   private void setTrackControl(final TrackControl trackControl)
   {
      mTrackControl = trackControl;
      mMuteLayer.setIsActive(trackControl == TrackControl.Mute);
      mSoloLayer.setIsActive(trackControl == TrackControl.Solo);
      mRecordArmLayer.setIsActive(trackControl == TrackControl.RecordArm);
   }

   private void setDeviceOn(final boolean isDeviceOn)
   {
      mIsDeviceOn = isDeviceOn;
      mDeviceLayer.setIsActive(isDeviceOn);
   }

   private void setFactoryLayersEnabled(final boolean enabled)
   {
      debug(mHost, "[LCXL] setFactoryLayersEnabled=" + enabled);
      if (enabled)
      {
         // Reactivate only the current mode-specific layers; do not turn on every layer at once.
         mMainLayer.setIsActive(true);
         selectMode(mMode);
         setTrackControl(mTrackControl);
         attachHardwareMatchers();
      }
      else
      {
         // Fully disable all factory layers when leaving factory templates.
         mMainLayer.setIsActive(false);
         mDeviceLayer.setIsActive(false);
         mSend2Device1Layer.setIsActive(false);
         mSend2Pan1Layer.setIsActive(false);
         mSend3Layer.setIsActive(false);
         mSend1Device2Layer.setIsActive(false);
         mDevice3Layer.setIsActive(false);
         mTrack3layer.setIsActive(false);
         mSend2FullDeviceLayer.setIsActive(false);
         mSend2ProjectLayer.setIsActive(false);
         mMuteLayer.setIsActive(false);
         mSoloLayer.setIsActive(false);
         mRecordArmLayer.setIsActive(false);

         clearHardwareMatchers();
      }
   }

   private void handleIncomingMidi(final int status, final int data1, final int data2)
   {
      if (handleArpMidi(status, data1, data2))
      {
         return;
      }
      if (mFactoryTemplateActive)
      {
         debug(mHost, String.format("[LCXL] MIDI status=%02X data1=%02X data2=%02X", status, data1, data2));
      }
   }

   private boolean handleArpMidi(final int status, final int data1, final int data2)
   {
      if (!mArpLayerActive || mArpLayerController == null)
      {
         return false;
      }
      final int channel = status & 0x0F;
      if (channel != ARP_USER_TEMPLATE_ID)
      {
         return false;
      }
      mArpLayerController.handleMidiEvent(status, data1, data2);
      final int message = status & 0xF0;
      if (message == Midi.NOTE_ON && data2 > 0)
      {
         handleArpNote(data1);
      }
      else if (message == Midi.CC)
      {
         handleArpCc(data1, data2);
      }
      return true;
   }

   private void handleArpNote(final int note)
   {
      if (note == DEVICE_NOTE)
      {
         mArpLayerController.toggleTimingMode();
         return;
      }
      if (note == SOLO_NOTE)
      {
         mArpLayerController.togglePatternMode();
         return;
      }
      if (note == MUTE_NOTE)
      {
         mArpLayerController.toggleVelocityGateMode();
         return;
      }
      if (note == RECORD_ARM_NOTE)
      {
         mArpLayerController.toggleQuantizeMode();
         return;
      }
      final int focusIndex = indexOf(TRACK_FOCUS_NOTES, note);
      if (focusIndex >= 0)
      {
         mArpLayerController.handleTrackFocusPressed(focusIndex);
         return;
      }
      final int controlIndex = indexOf(TRACK_CONTROL_NOTES, note);
      if (controlIndex >= 0)
      {
         mArpLayerController.handleTrackControlPressed(controlIndex);
      }
   }

   private void handleArpCc(final int cc, final int value)
   {
      if (cc == SEND_UP_CC)
      {
         mArpLayerController.handleOctaveIncrease();
         return;
      }
      if (cc == SEND_DOWN_CC)
      {
         mArpLayerController.handleOctaveDecrease();
         return;
      }
      if (cc == TRACK_LEFT_CC)
      {
         mArpLayerController.toggleGlobalVelocityMode();
         return;
      }
      if (cc == TRACK_RIGHT_CC)
      {
         mArpLayerController.toggleGlobalGateMode();
         return;
      }
      final Integer pitchKnobIndex = knobIndex(cc, KNOB_CC_OFFSETS[0]);
      if (pitchKnobIndex != null)
      {
         mArpLayerController.handlePitchOffsetKnob(pitchKnobIndex, value);
         return;
      }
      final Integer velocityIndex = knobIndex(cc, KNOB_CC_OFFSETS[1]);
      if (velocityIndex != null)
      {
         mArpLayerController.handleVelocityKnob(velocityIndex, value);
         return;
      }
      final Integer gateIndex = knobIndex(cc, KNOB_CC_OFFSETS[2]);
      if (gateIndex != null)
      {
         mArpLayerController.handleGateKnob(gateIndex, value);
         return;
      }
      final Integer sliderIndex = sliderIndex(cc);
      if (sliderIndex != null)
      {
         mArpLayerController.handlePitchOffsetSlider(sliderIndex, value);
      }
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
      final int offset = cc - SLIDER_CC_BASE;
      if (0 <= offset && offset < 8)
      {
         return offset;
      }
      return null;
   }

   private void setArpLayerActive(final boolean active)
   {
      if (mArpLayerController == null || mArpLayerActive == active)
      {
         return;
      }
      mArpLayerActive = active;
      if (active)
      {
         debug(mHost, "[LCXL] arp layer engaged (user template 8)");
         mArpLayerController.activate();
         mHost.showPopupNotification("Launch Control XL: arp layer active (User Template 8)");
      }
      else
      {
         debug(mHost, "[LCXL] arp layer disengaged");
         mArpLayerController.deactivate();
      }
   }

   private void selectModeFromSysex(final String sysex)
   {
      final String prefix = "f00020290211770";
      if (!sysex.startsWith(prefix))
         return;

      final int n = prefix.length();
      final int ch = Integer.parseInt(sysex.substring(n, n + 1), 16);

      if (8 <= ch && ch <= 15)
      {
         for (final Mode mode : Mode.values())
         {
            if (mode.getChannel() == ch)
            {
               debug(mHost, "[LCXL] factory template channel " + ch + " -> mode " + mode.name());
               selectMode(mode);
               return;
            }
         }
         debug(mHost, "[LCXL] No factory mode mapped for channel " + ch + " (factory template not supported)");
         mFactoryTemplateActive = false;
         setFactoryLayersEnabled(false);
         setArpLayerActive(false);
         selectMode(Mode.None);
      }
      else
      {
         // User templates (0–7) report other channel codes. We simply ignore the mode change so the
         // template-change handler can deal with enabling/disabling the layers, but we still log it
         // so it shows up in the Bitwig controller console.
         debug(mHost, "[LCXL] user template sysex channel " + ch + " received");
      }
   }

   private void selectMode(final Mode mode)
   {
      mMode = mode;
      mSend2Device1Layer.setIsActive(mode == Mode.Send2Device1);
      mSend2Pan1Layer.setIsActive(mode == Mode.Send2Pan1);
      mSend3Layer.setIsActive(mode == Mode.Send3);
      mSend1Device2Layer.setIsActive(mode == Mode.Send1Device2);
      mDevice3Layer.setIsActive(mode == Mode.Device3);
      mTrack3layer.setIsActive(mode == Mode.Track3);
      mSend2FullDeviceLayer.setIsActive(mode == Mode.Send2FullDevice);
      mSend2ProjectLayer.setIsActive(mode == Mode.Send2Project);

      switch (mode)
      {
         case Send2FullDevice, Send2Project, Send2Device1, Send2Pan1 -> setSizeOfSendBank(2);
         case Send3 -> setSizeOfSendBank(3);
         case Send1Device2 -> setSizeOfSendBank(1);
      }

      mHost.showPopupNotification(mode.getNotification());
   }

   private void setSizeOfSendBank(final int size)
   {
      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         final SendBank sendBank = track.sendBank();
         sendBank.setSizeOfBank(size);
      }
   }

   private void onSysex(final String sysex)
   {
      if (mIgnoreNextSysex)
      {
         mIgnoreNextSysex = false;
         return;
      }

      if (mArpLayerController != null)
      {
         mArpLayerController.handleSysex(sysex);
      }

      selectModeFromSysex(sysex);
      final OptionalInt templateId = TemplateChangeMessageParser.parseTemplateId(sysex);
      if (templateId.isPresent())
      {
         final int id = templateId.getAsInt();
         debug(mHost, "[LCXL] onSysex templateId=" + id + " arpTemplate=" + (id == ARP_USER_TEMPLATE_ID) +
            " factory=" + (id >= 8));
         if (id < 8)
         {
            final boolean arpTemplate = id == ARP_USER_TEMPLATE_ID;
            mCurrentTemplateChannel = id;
            if (arpTemplate)
            {
               debug(mHost, "[LCXL] handleTemplateChange -> arp template (user template 8)");
            }
            else
            {
               debug(mHost, "[LCXL] handleTemplateChange -> user template " + (id + 1));
            }
            mFactoryTemplateActive = false;
            setFactoryLayersEnabled(false);
            setArpLayerActive(arpTemplate);
         }
         else
         {
            debug(mHost, "[LCXL] handleTemplateChange -> factory template channel " + id);
            mCurrentTemplateChannel = id;
            mFactoryTemplateActive = true;
            setArpLayerActive(false);
            setFactoryLayersEnabled(true);
         }
         if (mArpLayerController != null)
         {
            mArpLayerController.handleSysexTemplateChange(id);
         }
      }
   }

   @Override
   public void exit()
   {
   }

   @Override
   public void flush()
   {
      // Do not repaint LEDs in raw user templates; keep the controller's stored colors intact.
      if (!mFactoryTemplateActive && !mArpLayerActive)
      {
         return;
      }

      paintRightButtons();
      paintKnobs();
      paintBottomButtons();

      final StringBuilder sb = new StringBuilder();

      mDeviceLed.flush(sb);
      mMuteLed.flush(sb);
      mSoloLed.flush(sb);
      mRecordArmLed.flush(sb);
      mUpButtonLed.flush(sb);
      mDownButtonLed.flush(sb);
      mLeftButtonLed.flush(sb);
      mRightButtonLed.flush(sb);

      for (final SimpleLed simpleLed : mKnobsLed)
         simpleLed.flush(sb);

      for (final SimpleLed simpleLed : mBottomButtonsLed)
         simpleLed.flush(sb);

      if (!sb.toString().isEmpty())
      {
         final int channel = mFactoryTemplateActive ? mMode.getChannel() : mCurrentTemplateChannel;
         final String sysex = "F0 00 20 29 02 11 78 0" + Integer.toHexString(channel) + sb + " F7";
         mMidiOut.sendSysex(sysex);
      }
   }

   protected void paintBottomButtons()
   {
      final int selectedTrack = mTrackBank.cursorIndex().get();

      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         final boolean trackExists = track.exists().get();
         final int defaultFocusColor = trackExists
            ? (selectedTrack == i ? SimpleLedColor.Amber.value() : SimpleLedColor.AmberLow.value())
            : SimpleLedColor.Off.value();
         final int focusColor = mArpLayerController != null
            ? mArpLayerController.applyFocusColor(i, defaultFocusColor)
            : defaultFocusColor;
         mBottomButtonsLed[i].setColor(focusColor);

         int controlColor;
         if (mIsDeviceOn)
         {
            SimpleLedColor color = SimpleLedColor.Off;

            if (mRemoteControls.selectedPageIndex().get() == i)
               color = SimpleLedColor.Amber;
            else if (i < mRemoteControls.pageCount().get())
               color = SimpleLedColor.AmberLow;

            controlColor = color.value();
         }
         else if (trackExists)
         {
            switch (mTrackControl)
            {
               case Mute -> controlColor = track.mute().get()
                  ? SimpleLedColor.Green.value()
                  : SimpleLedColor.GreenLow.value();
               case Solo -> controlColor = track.solo().get()
                  ? SimpleLedColor.Amber.value()
                  : SimpleLedColor.AmberLow.value();
               case RecordArm -> controlColor = track.arm().get()
                  ? SimpleLedColor.Red.value()
                  : SimpleLedColor.RedLow.value();
               default -> controlColor = SimpleLedColor.Off.value();
            }
         }
         else
         {
            controlColor = SimpleLedColor.Off.value();
         }
         final int appliedControlColor = mArpLayerController != null
            ? mArpLayerController.applyControlColor(i, controlColor)
            : controlColor;
         mBottomButtonsLed[8 + i].setColor(appliedControlColor);
      }
   }

   protected void paintKnobs()
   {
      if (!mFactoryTemplateActive && !mArpLayerActive)
      {
         return;
      }

      if (mArpLayerActive && mArpLayerController != null)
      {
         for (int i = 0; i < 8; ++i)
         {
            mKnobsLed[i].setColor(mArpLayerController.getPitchLedColor(i));
            mKnobsLed[8 + i].setColor(mArpLayerController.getVelocityLedColor(i));
            mKnobsLed[16 + i].setColor(mArpLayerController.getGateLedColor(i));
         }
         return;
      }

      for (int i = 0; i < 8; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         final SendBank sendBank = track.sendBank();

         final int green = SimpleLedColor.Green.value();
         final int greenLow = SimpleLedColor.GreenLow.value();
         final int off = SimpleLedColor.Off.value();
         final int amber = SimpleLedColor.Amber.value();
         final int amberLow = SimpleLedColor.AmberLow.value();
         final int red = SimpleLedColor.Red.value();

         switch (mMode)
         {
            case Send2Device1 ->
            {
               mKnobsLed[i].setColor(levelColor(sendBank.getItemAt(0).exists().get() ? sendBank.getItemAt(0).value().get() : 0, off, greenLow, green));
               mKnobsLed[8 + i].setColor(levelColor(sendBank.getItemAt(1).exists().get() ? sendBank.getItemAt(1).value().get() : 0, off, greenLow, green));
               mKnobsLed[16 + i].setColor(levelColor(mTrackCursorDeviceRemoteControls[i].getParameter(0).exists().get() ? mTrackCursorDeviceRemoteControls[i].getParameter(0).value().get() : 0, off, amberLow, amber));
            }
            case Send2Pan1 ->
            {
               mKnobsLed[i].setColor(levelColor(sendBank.getItemAt(0).exists().get() ? sendBank.getItemAt(0).value().get() : 0, off, greenLow, green));
               mKnobsLed[8 + i].setColor(levelColor(sendBank.getItemAt(1).exists().get() ? sendBank.getItemAt(1).value().get() : 0, off, greenLow, green));
               mKnobsLed[16 + i].setColor(track.exists().get() ? red : off);
            }
            case Send3 ->
            {
               mKnobsLed[i].setColor(levelColor(sendBank.getItemAt(0).exists().get() ? sendBank.getItemAt(0).value().get() : 0, off, greenLow, green));
               mKnobsLed[8 + i].setColor(levelColor(sendBank.getItemAt(1).exists().get() ? sendBank.getItemAt(1).value().get() : 0, off, greenLow, green));
               mKnobsLed[16 + i].setColor(levelColor(sendBank.getItemAt(2).exists().get() ? sendBank.getItemAt(2).value().get() : 0, off, greenLow, green));
            }
            case Send1Device2 ->
            {
               mKnobsLed[i].setColor(levelColor(sendBank.getItemAt(0).exists().get() ? sendBank.getItemAt(0).value().get() : 0, off, greenLow, green));
               mKnobsLed[8 + i].setColor(levelColor(mTrackCursorDeviceRemoteControls[i].getParameter(0).exists().get() ? mTrackCursorDeviceRemoteControls[i].getParameter(0).value().get() : 0, off, amberLow, amber));
               mKnobsLed[16 + i].setColor(levelColor(mTrackCursorDeviceRemoteControls[i].getParameter(1).exists().get() ? mTrackCursorDeviceRemoteControls[i].getParameter(1).value().get() : 0, off, amberLow, amber));
            }
            case Device3 ->
            {
               mKnobsLed[i].setColor(levelColor(mTrackCursorDeviceRemoteControls[i].getParameter(0).exists().get() ? mTrackCursorDeviceRemoteControls[i].getParameter(0).value().get() : 0, off, amberLow, amber));
               mKnobsLed[8 + i].setColor(levelColor(mTrackCursorDeviceRemoteControls[i].getParameter(1).exists().get() ? mTrackCursorDeviceRemoteControls[i].getParameter(1).value().get() : 0, off, amberLow, amber));
               mKnobsLed[16 + i].setColor(levelColor(mTrackCursorDeviceRemoteControls[i].getParameter(2).exists().get() ? mTrackCursorDeviceRemoteControls[i].getParameter(2).value().get() : 0, off, amberLow, amber));
            }
            case Track3 ->
            {
               mKnobsLed[i].setColor(levelColor(mTrackRemoteControls[i].getParameter(0).exists().get() ? mTrackRemoteControls[i].getParameter(0).value().get() : 0, off, amberLow, amber));
               mKnobsLed[8 + i].setColor(levelColor(mTrackRemoteControls[i].getParameter(1).exists().get() ? mTrackRemoteControls[i].getParameter(1).value().get() : 0, off, amberLow, amber));
               mKnobsLed[16 + i].setColor(levelColor(mTrackRemoteControls[i].getParameter(2).exists().get() ? mTrackRemoteControls[i].getParameter(2).value().get() : 0, off, amberLow, amber));
            }
            case Send2FullDevice ->
            {
               mKnobsLed[i].setColor(levelColor(sendBank.getItemAt(0).exists().get() ? sendBank.getItemAt(0).value().get() : 0, off, greenLow, green));
               mKnobsLed[8 + i].setColor(levelColor(sendBank.getItemAt(1).exists().get() ? sendBank.getItemAt(1).value().get() : 0, off, greenLow, green));
               mKnobsLed[16 + i].setColor(levelColor(mRemoteControls.getParameter(i).exists().get() ? mRemoteControls.getParameter(i).value().get() : 0, off, amberLow, amber));
            }
            case Send2Project ->
            {
               mKnobsLed[i].setColor(levelColor(sendBank.getItemAt(0).exists().get() ? sendBank.getItemAt(0).value().get() : 0, off, greenLow, green));
               mKnobsLed[8 + i].setColor(levelColor(sendBank.getItemAt(1).exists().get() ? sendBank.getItemAt(1).value().get() : 0, off, greenLow, green));
               mKnobsLed[16 + i].setColor(levelColor(mProjectRemoteControlsCursor.getParameter(i).exists().get() ? mProjectRemoteControlsCursor.getParameter(i).value().get() : 0, off, amberLow, amber));
            }
            case None ->
            {
               mKnobsLed[i].setColor(off);
               mKnobsLed[8 + i].setColor(off);
               mKnobsLed[16 + i].setColor(off);
            }
         }
      }
   }

   protected void paintRightButtons()
   {
      final int yellow = SimpleLedColor.Yellow.value();
      final int off = SimpleLedColor.Off.value();

      if (mArpLayerActive && mArpLayerController != null)
      {
         mDeviceLed.setColor(mArpLayerController.isTimingModeActive() ? yellow : off);
         mSoloLed.setColor(mArpLayerController.isPatternModeActive() ? yellow : off);
         mMuteLed.setColor(mArpLayerController.isVelocityGateModeActive() ? yellow : off);
         mRecordArmLed.setColor(mArpLayerController.isQuantizeModeActive() ? yellow : off);
         mUpButtonLed.setColor(off);
         mDownButtonLed.setColor(off);
         mLeftButtonLed.setColor(off);
         mRightButtonLed.setColor(off);
         return;
      }

      mDeviceLed.setColor(mIsDeviceOn ? yellow : off);
      mMuteLed.setColor(mTrackControl == TrackControl.Mute ? yellow : off);
      mSoloLed.setColor(mTrackControl == TrackControl.Solo ? yellow : off);
      mRecordArmLed.setColor(mTrackControl == TrackControl.RecordArm ? yellow : off);

      final SendBank sendBank = mTrackBank.getItemAt(0).sendBank();
      mUpButtonLed.setColor(sendBank.canScrollBackwards().get() ? yellow : off);
      mDownButtonLed.setColor(sendBank.canScrollForwards().get() ? yellow : off);

      if (mIsDeviceOn)
      {
         mLeftButtonLed.setColor(mCursorDevice.hasPrevious().get() ? yellow : off);
         mRightButtonLed.setColor(mCursorDevice.hasNext().get() ? yellow : off);
      }
      else
      {
         mLeftButtonLed.setColor(mTrackBank.canScrollBackwards().get() ? yellow : off);
         mRightButtonLed.setColor(mTrackBank.canScrollForwards().get() ? yellow : off);
      }
   }

   private ControllerHost mHost;
   private MidiIn mMidiIn;
   private MidiOut mMidiOut;
   private NoteInput mUserModeNoteInput;
   private TrackBank mTrackBank;
   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;
   private final CursorDevice[] mTrackDeviceCursors = new CursorDevice[8];
   private final CursorRemoteControlsPage[] mTrackCursorDeviceRemoteControls = new CursorRemoteControlsPage[8];
   private final CursorRemoteControlsPage[] mTrackRemoteControls = new CursorRemoteControlsPage[8];
   private CursorRemoteControlsPage mProjectRemoteControlsCursor;
   private RhArpLayerController mArpLayerController;

   private boolean mIsDeviceOn = false;
   private boolean mIgnoreNextSysex = false;
   private boolean mFactoryTemplateActive = true;
   private boolean mArpLayerActive = false;
   private int mCurrentTemplateChannel = Mode.Send2FullDevice.getChannel();
   private TrackControl mTrackControl = TrackControl.Mute;
   private Mode mMode = Mode.Send2Device1;

   private final SimpleLed[] mKnobsLed = new SimpleLed[] {
      new SimpleLed(0x90, 0),
      new SimpleLed(0x90, 1),
      new SimpleLed(0x90, 2),
      new SimpleLed(0x90, 3),
      new SimpleLed(0x90, 4),
      new SimpleLed(0x90, 5),
      new SimpleLed(0x90, 6),
      new SimpleLed(0x90, 7),

      new SimpleLed(0x90, 8),
      new SimpleLed(0x90, 9),
      new SimpleLed(0x90, 10),
      new SimpleLed(0x90, 11),
      new SimpleLed(0x90, 12),
      new SimpleLed(0x90, 13),
      new SimpleLed(0x90, 14),
      new SimpleLed(0x90, 15),

      new SimpleLed(0x90, 16),
      new SimpleLed(0x90, 17),
      new SimpleLed(0x90, 18),
      new SimpleLed(0x90, 19),
      new SimpleLed(0x90, 20),
      new SimpleLed(0x90, 21),
      new SimpleLed(0x90, 22),
      new SimpleLed(0x90, 23),
   };

   private final SimpleLed[] mBottomButtonsLed = new SimpleLed[] {
      new SimpleLed(0x90, 24),
      new SimpleLed(0x90, 25),
      new SimpleLed(0x90, 26),
      new SimpleLed(0x90, 27),
      new SimpleLed(0x90, 28),
      new SimpleLed(0x90, 29),
      new SimpleLed(0x90, 30),
      new SimpleLed(0x90, 31),

      new SimpleLed(0x90, 32),
      new SimpleLed(0x90, 33),
      new SimpleLed(0x90, 34),
      new SimpleLed(0x90, 35),
      new SimpleLed(0x90, 36),
      new SimpleLed(0x90, 37),
      new SimpleLed(0x90, 38),
      new SimpleLed(0x90, 39),
   };

   private final SimpleLed mDeviceLed = new SimpleLed(0x90, 40);
   private final SimpleLed mMuteLed = new SimpleLed(0x90, 41);
   private final SimpleLed mSoloLed = new SimpleLed(0x90, 42);
   private final SimpleLed mRecordArmLed = new SimpleLed(0x90, 43);
   private final SimpleLed mUpButtonLed = new SimpleLed(0x90, 44);
   private final SimpleLed mDownButtonLed = new SimpleLed(0x90, 45);
   private final SimpleLed mLeftButtonLed = new SimpleLed(0x90, 46);
   private final SimpleLed mRightButtonLed = new SimpleLed(0x90, 47);

   private HardwareSurface mHardwareSurface;
   private final int[] mKnobCcNumbers = new int[3 * 8];
   private final int[] mSliderCcNumbers = new int[8];
   private HardwareButton mBtSendUp;
   private HardwareButton mBtSendDown;
   private HardwareButton mBtTrackLeft;
   private HardwareButton mBtTrackRight;
   private final HardwareButton[] mBtTrackFocus = new HardwareButton[8];
   private final HardwareButton[] mBtTrackControl = new HardwareButton[8];
   private HardwareButton mBtDevice;
   private HardwareButton mBtMute;
   private HardwareButton mBtSolo;
   private HardwareButton mBtRecordArm;
   private final AbsoluteHardwareKnob[] mHardwareKnobs = new AbsoluteHardwareKnob[3 * 8];
   private final HardwareSlider[] mHardwareSliders = new HardwareSlider[8];

   private Layer mSend2Device1Layer;
   private Layer mSend2Pan1Layer;
   private Layer mSend3Layer;
   private Layer mSend1Device2Layer;
   private Layer mDevice3Layer;
   private Layer mTrack3layer;
   private Layer mSend2FullDeviceLayer;
   private Layer mMuteLayer;
   private Layer mSoloLayer;
   private Layer mRecordArmLayer;
   private Layer mMainLayer;
   private Layer mDeviceLayer;
   private Layer mSend2ProjectLayer;
}
