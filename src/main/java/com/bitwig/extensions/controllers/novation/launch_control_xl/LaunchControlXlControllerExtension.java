package com.bitwig.extensions.controllers.novation.launch_control_xl;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
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
import com.bitwig.extension.controller.api.RemoteControlsPage;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.launch_control_xl.drum.DrumMapping;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.DeviceLocator;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.DeviceLocator.FocusResult;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.DeviceLocator.Role;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.TemplateChangeMessageParser;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.HostNotifications;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.UserModeNoteInputInstaller;
import com.bitwig.extensions.rh.Midi;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Entry point for the Launch Control XL controller extension.
 * <p>
 * This class wires all hardware controls to Bitwig features by creating layers (factory modes,
 * drum layer, arp layer, etc.), reacts to template changes, and coordinates helpers such as
 * {@link DeviceLocator} and {@link DrumLayerController}. Keep most view-specific logic in those
 * helpers so this class remains the orchestrator.
 */
public class LaunchControlXlControllerExtension extends ControllerExtension
{
   private static final boolean DEBUG_TELEMETRY = true;
   private static final int DEVICE_DISCOVERY_WIDTH = 128;

   // Launch Control XL (default user mode) MIDI note and CC numbers
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

   // hardcoded user template IDs for Drum and Arp layers
   private static final int DRUM_USER_TEMPLATE_ID = 6; // user template 7 (0-based)
   private static final int ARP_USER_TEMPLATE_ID = 7;

   // Define available Factory modes
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
      None,
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
      mHostActions = new HostNotifications(mHost, DEBUG_TELEMETRY);

      mMidiIn = mHost.getMidiInPort(0);
      mMidiOut = mHost.getMidiOutPort(0);
      mArpLayerController = new RhArpLayerController(mHost);
      mUserModeNoteInput = UserModeNoteInputInstaller.ensureUserModeInput(
         mMidiIn,
         ARP_USER_TEMPLATE_ID,
         DRUM_USER_TEMPLATE_ID);
      mAutoAttachToFirst = mHost.getPreferences().getBooleanSetting(
         "Auto-attach to first Drum Machine and Arpeggiator",
         "LaunchControl XL",
         true);
      mAuditionOnDrumSelect = mHost.getPreferences().getBooleanSetting(
         "Audition on drum pad select",
         "LaunchControl XL",
         true);
      mDrumAccentMomentary = mHost.getPreferences().getBooleanSetting(
         "Drum accent buttons momentary",
         "LaunchControl XL",
         true);

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

      mDrumPadBank = mCursorDevice.createDrumPadBank(DrumLayerController.PADS_PER_BANK);

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
         mTrackCursorDeviceRemoteControls[i] = mTrackDeviceCursors[i].createCursorRemoteControlsPage(4);
         mTrackCursorDeviceRemoteControls[i].setHardwareLayout(HardwareControlType.KNOB, 1);

         mTrackRemoteControls[i] = track.createCursorRemoteControlsPage(4);

         for (int j = 0; j < 4; ++j)
         {
            markParameterInterested(mTrackCursorDeviceRemoteControls[i].getParameter(j));
            markParameterInterested(mTrackRemoteControls[i].getParameter(j));
         }
      }

      initDiscoveryBanks();

      createHardwareSurface();
      createLayers();

      mMainLayer.activate();
      selectMode(Mode.Send2FullDevice);
      setTrackControl(TrackControl.None);
      setDeviceOn(false);
   }

   private void initializeDeviceWithMode(final Mode mode)
   {
      mMidiOut.sendSysex("f00020290211770" + mode.getHexChannel() + "f7");
      mIgnoreNextSysex = true;
   }

   private void initDiscoveryBanks()
   {
      mDeviceLocator = new DeviceLocator(mHost, DEVICE_DISCOVERY_WIDTH);
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
         mBtTrackFocus[i].isPressed().markInterested();
      }

      for (int i = 0; i < mBtTrackControl.length; i++)
      {
         mBtTrackControl[i] = createHardwareButtonWithNote("bt-track-control-" + i, TRACK_CONTROL_NOTES[i], i);
         mBtTrackControl[i].isPressed().markInterested();
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

   private void setCcButtonActionMatchers(final HardwareButton button, final int cc, final Integer channel)
   {
      if (channel == null)
      {
         button.pressedAction().setActionMatcher(null);
         button.releasedAction().setActionMatcher(null);
         return;
      }
      button.pressedAction().setActionMatcher(mMidiIn.createCCActionMatcher(channel, cc, 127));
      button.releasedAction().setActionMatcher(mMidiIn.createCCActionMatcher(channel, cc, 0));
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
      final int channel = mDrumLayerActive ? DRUM_USER_TEMPLATE_ID : mCurrentTemplateChannel;
      mHostActions.debug("[LCXL] attachHardwareMatchers mode=" + mMode + " channel=" + channel +
         " factoryActive=" + mFactoryTemplateActive + " arpActive=" + mArpLayerActive +
         " drumActive=" + mDrumLayerActive);
      for (int i = 0; i < mHardwareKnobs.length; i++)
      {
         final AbsoluteHardwareKnob knob = mHardwareKnobs[i];
         if (knob != null)
         {
            if (mDrumLayerActive)
            {
               if (i < 8)
                  knob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(channel, DrumMapping.KNOB_ROW1_CCS[i]));
               else if (i < 16)
                  knob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(channel, DrumMapping.KNOB_ROW2_CCS[i - 8]));
               else
                  knob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(channel, DrumMapping.KNOB_ROW3_CCS[i - 16]));
            }
            else
            {
               knob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(channel, mKnobCcNumbers[i]));
            }
         }
      }
      for (int i = 0; i < mHardwareSliders.length; i++)
      {
         final HardwareSlider slider = mHardwareSliders[i];
         if (slider != null)
         {
            if (mDrumLayerActive)
            {
               slider.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(channel, DrumMapping.SLIDER_CCS[i]));
            }
            else
            {
               slider.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(channel, mSliderCcNumbers[i]));
            }
         }
      }
      if (mDrumLayerActive)
      {
         setCcButtonMatcher(mBtSendUp, DrumMapping.SEND_UP_CC, channel);
         setCcButtonMatcher(mBtSendDown, DrumMapping.SEND_DOWN_CC, channel);
         setCcButtonMatcher(mBtTrackLeft, DrumMapping.TRACK_LEFT_CC, channel);
         setCcButtonMatcher(mBtTrackRight, DrumMapping.TRACK_RIGHT_CC, channel);
      }
      else
      {
         setCcButtonMatcher(mBtSendUp, SEND_UP_CC, channel);
         setCcButtonMatcher(mBtSendDown, SEND_DOWN_CC, channel);
         setCcButtonMatcher(mBtTrackLeft, TRACK_LEFT_CC, channel);
         setCcButtonMatcher(mBtTrackRight, TRACK_RIGHT_CC, channel);
      }
      for (int i = 0; i < mBtTrackFocus.length; i++)
      {
         final boolean drum = mDrumLayerActive;
         setNoteButtonActionMatchers(mBtTrackFocus[i], drum ? DrumMapping.TOP_NOTES[i] : TRACK_FOCUS_NOTES[i], channel);
      }
      for (int i = 0; i < mBtTrackControl.length; i++)
      {
         final boolean drum = mDrumLayerActive;
         setNoteButtonActionMatchers(mBtTrackControl[i], drum ? DrumMapping.BOTTOM_NOTES[i] : TRACK_CONTROL_NOTES[i], channel);
      }
      setNoteButtonActionMatchers(mBtDevice, DEVICE_NOTE, channel);
      setNoteButtonActionMatchers(mBtMute, MUTE_NOTE, channel);
      setNoteButtonActionMatchers(mBtSolo, SOLO_NOTE, channel);
      setNoteButtonActionMatchers(mBtRecordArm, RECORD_ARM_NOTE, channel);
   }

   private void clearHardwareMatchers()
   {
      mHostActions.debug("[LCXL] clearHardwareMatchers");
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
      mMainLayer.bindPressed(mBtMute, () -> toggleTrackControl(TrackControl.Mute));
      mMainLayer.bindPressed(mBtSolo, () -> toggleTrackControl(TrackControl.Solo));
      mMainLayer.bindPressed(mBtRecordArm, () -> toggleTrackControl(TrackControl.RecordArm));

      createModeLayers(layers);
      createTrackControlsLayers(layers);
      createDeviceLayer(layers);
      createDrumLayer(layers);
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

   /**
    * Create the drum layer (user template 7) and wire all hardware controls to the
    * {@link DrumLayerController}. This layer takes over knobs/sliders/buttons when the drum template
    * is active, so the helper receives references to all relevant controls.
    */
    private void createDrumLayer(final Layers layers)
   {
      mDrumLayer = new Layer(layers, "Drum");

      final AbsoluteHardwareKnob[][] drumKnobs = new AbsoluteHardwareKnob[3][DrumLayerController.PADS_PER_BANK];
      for (int row = 0; row < 3; row++)
      {
         for (int col = 0; col < DrumLayerController.PADS_PER_BANK; col++)
         {
            drumKnobs[row][col] = mHardwareKnobs[row * 8 + col];
         }
      }

      mDrumLayerController = new DrumLayerController(
         mHost,
         mUserModeNoteInput,
         DRUM_USER_TEMPLATE_ID,
         mDrumPadBank,
         mCursorDevice,
         mDrumPadRemoteControls,
         mDrumLayer,
         mHardwareSliders,
         drumKnobs,
         mBtTrackControl,
         mBtTrackFocus,
         mBtTrackLeft,
         mBtTrackRight,
         mAuditionOnDrumSelect.get(),
         mDrumAccentMomentary.get());
      mAuditionOnDrumSelect.addValueObserver(value -> {
         if (mDrumLayerController != null)
            mDrumLayerController.setAuditionOnSelect(value);
      });
      mDrumAccentMomentary.addValueObserver(value -> {
         if (mDrumLayerController != null)
            mDrumLayerController.setAccentMomentary(value);
      });
      mDrumLayer.bindPressed(mBtMute, mDrumLayerController::toggleMuteMode);
      mDrumLayer.bindPressed(mBtSolo, mDrumLayerController::toggleSoloMode);
      mDrumLayerController.init();
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

      mTrackRemoteButtonLayer = new Layer(layers, "Track Remote Button");
      for (int i = 0; i < 8; ++i)
      {
         final int I = i;
         mTrackRemoteButtonLayer.bindPressed(mBtTrackControl[i], () -> {
            final CursorRemoteControlsPage remotePage = mTrackRemoteControls[I];
            if (remotePage != null)
            {
               final RemoteControl param = remotePage.getParameter(3);
               final double current = param.value().get();
               param.value().set(current > 0 ? 0 : 127, 127);
            }
         });
      }
   }

   private void setTrackControl(final TrackControl trackControl)
   {
      mTrackControl = trackControl;
      mMuteLayer.setIsActive(trackControl == TrackControl.Mute);
      mSoloLayer.setIsActive(trackControl == TrackControl.Solo);
      mRecordArmLayer.setIsActive(trackControl == TrackControl.RecordArm);
      mTrackRemoteButtonLayer.setIsActive(trackControl == TrackControl.None);
      if (mDrumLayerController != null)
         mDrumLayerController.setTrackControlMode(trackControl);
      if (mDrumLayerActive)
      {
         switch (trackControl)
         {
            case Mute -> mHostActions.showPopup("Drum layer: Mute buttons");
            case None -> mHostActions.showPopup("Drum layer: Default buttons");
            default -> {
               // No popup for other track control modes while in drum layer.
            }
         }
      }
   }

   private void toggleTrackControl(final TrackControl desired)
   {
      if (mTrackControl == desired)
      {
         setTrackControl(TrackControl.None);
      }
      else
      {
         setTrackControl(desired);
      }
   }

   private void setDeviceOn(final boolean isDeviceOn)
   {
      mIsDeviceOn = isDeviceOn;
      mDeviceLayer.setIsActive(isDeviceOn);
   }

   private void setFactoryLayersEnabled(final boolean enabled)
   {
      mHostActions.debug("[LCXL] setFactoryLayersEnabled=" + enabled);
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
      if (mDrumLayerActive && mDrumLayerController != null && mDrumLayerController.handleMidi(status, data1, data2))
      {
         return;
      }
      if (mDrumLayerActive)
      {
         mHostActions.debug(String.format("[LCXL] DRUM MIDI status=%02X data1=%02X data2=%02X (channel %d)",
            status, data1, data2, status & 0x0F));
      }
      if (mFactoryTemplateActive)
      {
         mHostActions.debug(String.format("[LCXL] MIDI status=%02X data1=%02X data2=%02X", status, data1, data2));
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
      if (value == 0)
         return; // ignore releases

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

   /**
    * Focus the track/device for a specific role (drum or arp). Tries the cached index first (when
    * requested) before scanning via the {@link DeviceLocator}, then applies the focus (select track,
    * select device, notify cursor device).
    *
    * @param role The device role to focus (drum or arp).
    * @param tryCacheFirst Whether the cached index should be tried before re-scanning.
    * @return True when a matching device was focused.
    */
   private boolean focusDevice(final Role role, final boolean tryCacheFirst)
   {
      if (this.mDeviceLocator == null)
         return false;

      Optional<FocusResult> focusResult = Optional.empty();
      if (tryCacheFirst)
         focusResult = this.mDeviceLocator.focusCached(role);

      if (focusResult.isEmpty())
         focusResult = this.mDeviceLocator.focusFirst(role);

      focusResult.ifPresent(result -> this.applyFocusedDevice(role, result));
      return focusResult.isPresent();
   }

   /**
    * Apply the device focus result by selecting the track/device in Bitwig and logging the action.
    */
   private void applyFocusedDevice(final Role role, final FocusResult focus)
   {
      final Track track = focus.track();
      track.selectInMixer();
      track.selectInEditor();

      if (this.mCursorDevice != null)
         this.mCursorDevice.selectDevice(focus.device());

      this.mHostActions.debug("[LCXL] Focused " + getRoleLabel(role) + " on track index " + focus.trackIndex());
   }

   private static String getRoleLabel(final Role role)
   {
      return switch (role)
      {
         case DRUM -> "Drum Machine";
         case ARP -> "Arpeggiator";
      };
   }

   /**
    * Engage or disengage the arp user layer (user template 8). Activation auto-selects the arp
    * device (using {@link DeviceLocator}) and pins the cursor device so the RhArp layer can safely
    * control parameters inside Bitwig.
    */
   private void setArpLayerActive(final boolean active)
   {
      if (mArpLayerController == null || mArpLayerActive == active)
      {
         return;
      }
      mArpLayerActive = active;
      if (active)
      {
         mHostActions.debug("[LCXL] arp layer engaged (user template 8)");
         boolean attached = false;
         if (mAutoAttachToFirst == null || mAutoAttachToFirst.get())
            attached = focusDevice(Role.ARP, true);
         if (mCursorDevice != null)
            mCursorDevice.isPinned().set(attached);
         mArpLayerController.activate();
         mHostActions.showPopup("Arp layer active (Template 8)");
      }
      else
      {
         mHostActions.debug("[LCXL] arp layer disengaged");
         if (mCursorDevice != null)
         {
            mCursorDevice.isPinned().set(false);
         }
         mArpLayerController.deactivate();
      }
   }

   /**
    * Engage or disengage the drum user layer (user template 7). When enabling, attempt to auto-focus
    * the drum machine track/device and rebind hardware controls so the {@link DrumLayerController}
    * owns the knobs/sliders/buttons.
    */
   private void setDrumLayerActive(final boolean active)
   {
      if (mDrumLayerController == null || mDrumLayerActive == active)
      {
         return;
      }
      mDrumLayerActive = active;
      if (active)
      {
         mHostActions.debug("[LCXL] drum layer engage request (user template 7) currentTemplate=" + mCurrentTemplateChannel);
         if (mAutoAttachToFirst == null || mAutoAttachToFirst.get())
         {
            final boolean attached = focusDevice(Role.DRUM, true);
            if (!attached)
               mHostActions.debug("[LCXL] Unable to focus Drum Machine automatically");
         }
         attachHardwareMatchers();
         mDrumLayerController.engage();
      }
      else
      {
         mHostActions.debug("[LCXL] drum layer disengaged");
         mDrumLayerController.disengage();
         clearHardwareMatchers();
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
               mHostActions.debug("[LCXL] factory template channel " + ch + " -> mode " + mode.name());
               selectMode(mode);
               return;
            }
         }
         mHostActions.debug("[LCXL] No factory mode mapped for channel " + ch + " (factory template not supported)");
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
         mHostActions.debug("[LCXL] user template sysex channel " + ch + " received");
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

      mHostActions.showPopup(mode.getNotification());
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
         mHostActions.debug("[LCXL] onSysex templateId=" + id + " arpTemplate=" + (id == ARP_USER_TEMPLATE_ID) +
            " factory=" + (id >= 8));
         if (id < 8)
         {
            final boolean arpTemplate = id == ARP_USER_TEMPLATE_ID;
            final boolean drumTemplate = id == DRUM_USER_TEMPLATE_ID;
            mCurrentTemplateChannel = id;
            if (arpTemplate)
            {
               mHostActions.debug("[LCXL] handleTemplateChange -> arp template (user template 8)");
            }
            else
            {
               mHostActions.debug("[LCXL] handleTemplateChange -> user template " + (id + 1) +
                  (drumTemplate ? " (drum)" : ""));
            }
            mFactoryTemplateActive = false;
            setFactoryLayersEnabled(false);
            setArpLayerActive(arpTemplate);
            setDrumLayerActive(drumTemplate);
         }
         else
         {
            mHostActions.debug("[LCXL] handleTemplateChange -> factory template channel " + id);
            mCurrentTemplateChannel = id;
            mFactoryTemplateActive = true;
            setArpLayerActive(false);
            setDrumLayerActive(false);
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
      // Do not repaint LEDs in raw user templates without an active overlay layer.
      if (!mFactoryTemplateActive && !mArpLayerActive && !mDrumLayerActive)
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
      if (mDrumLayerActive)
      {
         paintDrumButtons();
         return;
      }

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
            final int amber = SimpleLedColor.Amber.value();
            final int amberLow = SimpleLedColor.AmberLow.value();
            final int off = SimpleLedColor.Off.value();
            switch (mTrackControl)
            {
               case Mute -> controlColor = track.mute().get()
                  ? SimpleLedColor.GreenLow.value()
                  : SimpleLedColor.Green.value();
               case Solo -> controlColor = track.solo().get()
                  ? amber
                  : amberLow;
               case RecordArm -> controlColor = track.arm().get()
                  ? SimpleLedColor.Red.value()
                  : SimpleLedColor.RedLow.value();
               case None ->
               {
                  final RemoteControl param = mTrackRemoteControls[i].getParameter(3);
                  final boolean exists = param.exists().get();
                  final double value = exists ? param.value().get() : 0;
                  controlColor = exists ? levelColor(value, off, amberLow, amber) : off;
               }
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

   private void paintDrumButtons()
   {
      final boolean soloMode = mDrumLayerController != null && mDrumLayerController.isSoloMode();
      final TrackControl drumControlMode = mDrumLayerController != null ? mDrumLayerController.getTrackControlMode() : TrackControl.None;
      final int selectedPad = mDrumLayerController != null ? mDrumLayerController.getSelectedPadIndex() : -1;

      for (int i = 0; i < DrumLayerController.PADS_PER_BANK; i++)
      {
         int topColor = SimpleLedColor.Off.value();
         int bottomColor = SimpleLedColor.Off.value();
         if (mDrumPadBank != null)
         {
            final DrumPad pad = mDrumPadBank.getItemAt(i);
            final boolean exists = pad.exists().get();
            if (exists)
            {
               topColor = i == selectedPad ? SimpleLedColor.Yellow.value() : SimpleLedColor.AmberLow.value();
               final boolean muteState = pad.mute().get();
               final boolean soloState = pad.solo().get();
               if (soloMode)
               {
                  bottomColor = soloState
                     ? SimpleLedColor.Yellow.value()
                     : SimpleLedColor.YellowLow.value();
               }
               else if (drumControlMode == TrackControl.Mute)
               {
                  bottomColor = muteState
                     ? SimpleLedColor.GreenLow.value()
                     : SimpleLedColor.Green.value();
               }
               else
               {
                  final RemoteControlsPage rc = mDrumPadRemoteControls[i];
                  final RemoteControl param = rc != null ? rc.getParameter(3) : null;
                  final boolean paramExists = param != null && param.exists().get();
                  final double value = paramExists ? param.value().get() : 0;
                  bottomColor = paramExists
                     ? levelColor(value, SimpleLedColor.Off.value(), SimpleLedColor.GreenLow.value(), SimpleLedColor.Green.value())
                     : SimpleLedColor.Off.value();
               }
            }
         }
         mBottomButtonsLed[i].setColor(topColor);
         mBottomButtonsLed[8 + i].setColor(bottomColor);
      }
   }

   protected void paintKnobs()
   {
      if (mDrumLayerActive)
      {
         paintDrumKnobs();
         return;
      }

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

   private void paintDrumKnobs()
   {
      final int off = SimpleLedColor.Off.value();
      final int yellow = SimpleLedColor.Yellow.value();
      final int yellowLow = SimpleLedColor.YellowLow.value();
      final int amber = SimpleLedColor.Amber.value();
      final int amberLow = SimpleLedColor.AmberLow.value();
      final int red = SimpleLedColor.Red.value();
      final int redLow = SimpleLedColor.RedLow.value();

      for (int padIndex = 0; padIndex < DrumLayerController.PADS_PER_BANK; padIndex++)
      {
         final DrumPad pad = mDrumPadBank != null ? mDrumPadBank.getItemAt(padIndex) : null;
         final boolean padExists = pad != null && pad.exists().get();
         final RemoteControlsPage remoteControlsPage = mDrumPadRemoteControls[padIndex];
         for (int paramIndex = 0; paramIndex < 3; paramIndex++)
         {
            final int ledIndex = paramIndex * 8 + padIndex;
            int color = off;
            if (remoteControlsPage != null && padExists)
            {
               final RemoteControl parameter = remoteControlsPage.getParameter(paramIndex);
               final boolean exists = parameter.exists().get();
               final double value = exists ? parameter.value().get() : 0;
               switch (paramIndex)
               {
                  case 0 -> color = levelColor(value, off, yellowLow, yellow);
                  case 1 -> color = levelColor(value, off, amberLow, amber);
                  case 2 -> color = levelColor(value, off, redLow, red);
                  default -> color = off;
               }
               if (!exists)
               {
                  color = off;
               }
            }
            mKnobsLed[ledIndex].setColor(color);
         }
      }
   }

   protected void paintRightButtons()
   {
      final int yellow = SimpleLedColor.Yellow.value();
      final int off = SimpleLedColor.Off.value();

      if (mDrumLayerActive)
      {
         final boolean canScrollBack = mDrumPadBank != null && mDrumPadBank.canScrollBackwards().get();
         final boolean canScrollForward = mDrumPadBank != null && mDrumPadBank.canScrollForwards().get();
         mDeviceLed.setColor(off);
         final TrackControl drumControlMode = mDrumLayerController != null ? mDrumLayerController.getTrackControlMode() : TrackControl.None;
         mMuteLed.setColor(drumControlMode == TrackControl.Mute ? yellow : off);
         mSoloLed.setColor(mDrumLayerController != null && mDrumLayerController.isSoloMode() ? yellow : off);
         mRecordArmLed.setColor(off);
         mUpButtonLed.setColor(off);
         mDownButtonLed.setColor(off);
         mLeftButtonLed.setColor(canScrollBack ? yellow : off);
         mRightButtonLed.setColor(canScrollForward ? yellow : off);
         return;
      }

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
   private HostNotifications mHostActions;
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
   private DrumLayerController mDrumLayerController;
   private DeviceLocator mDeviceLocator;
   private SettableBooleanValue mAuditionOnDrumSelect;
   private SettableBooleanValue mDrumAccentMomentary;
   private DrumPadBank mDrumPadBank;
   private final RemoteControlsPage[] mDrumPadRemoteControls =
      new RemoteControlsPage[DrumLayerController.PADS_PER_BANK];

   private boolean mIsDeviceOn = false;
   private boolean mIgnoreNextSysex = false;
   private boolean mFactoryTemplateActive = true;
   private boolean mArpLayerActive = false;
   private boolean mDrumLayerActive = false;
   private int mCurrentTemplateChannel = Mode.Send2FullDevice.getChannel();
   private TrackControl mTrackControl = TrackControl.None;
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
   private SettableBooleanValue mAutoAttachToFirst;

   private Layer mSend2Device1Layer;
   private Layer mSend2Pan1Layer;
   private Layer mSend3Layer;
   private Layer mSend1Device2Layer;
   private Layer mDevice3Layer;
   private Layer mTrack3layer;
   private Layer mSend2FullDeviceLayer;
   private Layer mTrackRemoteButtonLayer;
   private Layer mMuteLayer;
   private Layer mSoloLayer;
   private Layer mRecordArmLayer;
   private Layer mMainLayer;
   private Layer mDeviceLayer;
   private Layer mSend2ProjectLayer;
   private Layer mDrumLayer;
}
