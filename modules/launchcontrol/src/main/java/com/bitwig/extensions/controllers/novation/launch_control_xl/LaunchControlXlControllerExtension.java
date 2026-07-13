package com.bitwig.extensions.controllers.novation.launch_control_xl;

import static com.bitwig.extensions.controllers.novation.launch_control_xl.support.LedUtil.levelColor;

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
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.launch_control_xl.drum.DrumLedRenderer;
import com.bitwig.extensions.controllers.novation.launch_control_xl.drum.DrumMapping;
import com.bitwig.extensions.controllers.novation.launch_control_xl.drum.DrumUiState;
import com.bitwig.extensions.controllers.novation.launch_control_xl.drum.DrumSettings;
import com.bitwig.extensions.controllers.novation.launch_control_xl.factory.FactoryLedRenderer;
import com.bitwig.extensions.controllers.novation.launch_control_xl.factory.FactoryLayerController;
import com.bitwig.extensions.controllers.novation.launch_control_xl.factory.FactoryUiSnapshot;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.DeviceLocator;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.DeviceLocator.FocusResult;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.DeviceLocator.Role;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.TemplateChangeMessageParser;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.HardwareBindingManager;
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
   static final boolean DEBUG_TELEMETRY = false;
   static final int DEVICE_DISCOVERY_WIDTH = 128;
   static final boolean EXCLUSIVE_TRACK_ARM_DEFAULT = false;
   private static HostNotifications sHostActions;

   // Launch Control XL (default user mode) MIDI note and CC numbers
   static final int[] TRACK_FOCUS_NOTES = {41, 42, 43, 44, 57, 58, 59, 60};
   static final int[] TRACK_CONTROL_NOTES = {73, 74, 75, 76, 89, 90, 91, 92};
   static final int[] KNOB_CC_OFFSETS = {13, 29, 49};
   static final int SLIDER_CC_BASE = 77;
   static final int STRIP_COUNT = 8;
   static final int TRACK_PAGE_SELECTION_DELAY_MS = 75;
   private static final int SEND_UP_CC = 104;
   private static final int SEND_DOWN_CC = 105;
   private static final int TRACK_LEFT_CC = 106;
   private static final int TRACK_RIGHT_CC = 107;
   private static final int DEVICE_NOTE = 105;
   private static final int MUTE_NOTE = 106;
   private static final int SOLO_NOTE = 107;
   private static final int RECORD_ARM_NOTE = 108;
   // user template dedicated to device remote pages (Template 6, zero-based channel 5)
   private static final int DEVICE_PAGES_USER_TEMPLATE_ID = 5;

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

   enum TrackBooleanTarget
   {
      MUTE,
      SOLO,
      ARM
   }

   enum TrackValueTarget
   {
      VOLUME,
      PAN
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
      sHostActions = mHostActions;
      mDevicePerTrackSupported = mHost.getHostApiVersion() < 25;

      mMidiIn = mHost.getMidiInPort(0);
      mMidiOut = mHost.getMidiOutPort(0);
      mArpLayerController = new RhArpLayerController(mHost);
      mUserModeNoteInput = UserModeNoteInputInstaller.ensureUserModeInput(
         mMidiIn,
         ARP_USER_TEMPLATE_ID,
         DRUM_USER_TEMPLATE_ID,
         DEVICE_PAGES_USER_TEMPLATE_ID);
      mAutoAttachToFirst = mHost.getPreferences().getBooleanSetting(
         "Auto-attach to first Drum Machine and Arpeggiator",
         "LaunchControl XL",
         true);
      mExclusiveTrackArm = mHost.getPreferences().getBooleanSetting(
         "Exclusive Track Arm",
         "LaunchControl XL",
         EXCLUSIVE_TRACK_ARM_DEFAULT);
      mExclusiveTrackArm.markInterested();
      mExclusiveTrackArmEnabled = mExclusiveTrackArm.get();
      mExclusiveTrackArm.addValueObserver(value -> {
         mExclusiveTrackArmEnabled = value;
         if (mFactoryLayerController != null)
            mFactoryLayerController.setExclusiveTrackArmEnabled(value);
      });
      mDrumSettings = DrumSettings.from(mHost);

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
      initDeviceRemotePages();

      mDrumPadBank = mCursorDevice.createDrumPadBank(DrumLayerController.PADS_PER_BANK);

      final Project project = mHost.getProject();
      final Track rootTrackGroup = project.getRootTrackGroup();
      mProjectRemoteControlsCursor = rootTrackGroup.createCursorRemoteControlsPage("project-remotes", 8, null);

      for (int i = 0; i < 8; ++i)
      {
         markParameterInterested(mRemoteControls.getParameter(i));
         markParameterInterested(mProjectRemoteControlsCursor.getParameter(i));
      }

      mTrackBank = mHost.createMainTrackBank(STRIP_COUNT, 3, 0);
      mTrackBank.followCursorTrack(mCursorTrack);
      mTrackBank.setShouldShowClipLauncherFeedback(true);
      applyTrackVisibilityPreference(mDrumSettings.showDeactivatedTracksEnabled());
      mDrumSettings.showDeactivatedTracks().addValueObserver(this::applyTrackVisibilityPreference);
      mTrackBank.canScrollBackwards().markInterested();
      mTrackBank.canScrollForwards().markInterested();

      mTrackBank.cursorIndex().markInterested();
      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         track.solo().markInterested();
         track.arm().markInterested();
         track.mute().markInterested();
         track.volume().markInterested();
         track.exists().markInterested();
         track.isActivated().markInterested();

         final SendBank sendBank = track.sendBank();
         sendBank.canScrollBackwards().markInterested();
         sendBank.canScrollForwards().markInterested();
         for (int j = 0; j < 3; ++j)
         {
            sendBank.getItemAt(j).exists().markInterested();
            sendBank.getItemAt(j).value().markInterested();
         }

         if (mDevicePerTrackSupported)
         {
            mTrackDeviceCursors[i] = track.createCursorDevice("track-device-" + i, 0);
            mTrackCursorDeviceRemoteControls[i] = mTrackDeviceCursors[i].createCursorRemoteControlsPage(4);
            mTrackCursorDeviceRemoteControls[i].setHardwareLayout(HardwareControlType.KNOB, 1);
         }

         mTrackRemoteControls[i] = track.createCursorRemoteControlsPage(4);

         for (int j = 0; j < 4; ++j)
         {
            if (mTrackCursorDeviceRemoteControls[i] != null)
               markParameterInterested(mTrackCursorDeviceRemoteControls[i].getParameter(j));
            markParameterInterested(mTrackRemoteControls[i].getParameter(j));
         }
      }

      initDiscoveryBanks();

      createHardwareSurface();
      mFactoryLayerController = new FactoryLayerController(new FactoryPort(), STRIP_COUNT);
      mFactoryLayerController.setExclusiveTrackArmEnabled(mExclusiveTrackArmEnabled);
      createLayers();

      mMainLayer.activate();
      selectMode(Mode.Send2FullDevice);
      mFactoryLayerController.setTrackControl(FactoryUiSnapshot.TrackControl.NONE);
      mFactoryLayerController.setDeviceOn(false);
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

   /**
    * Pre-create independent cursor pages for the selected device so each hardware row can pin to a
    * specific remote page (1–6 for knobs/buttons, 7 for navigation).
    */
   private void initDeviceRemotePages()
   {
      for (int pageIndex = 0; pageIndex < mDeviceRemotePages.length; pageIndex++)
      {
         final CursorRemoteControlsPage rc =
            mCursorDevice.createCursorRemoteControlsPage("device-page-" + (pageIndex + 1), 8, "");
         rc.selectedPageIndex().markInterested();
         rc.pageCount().markInterested();
         final int index = pageIndex;
         rc.pageCount().addValueObserver(count -> {
            if (count > index)
            {
               rc.selectedPageIndex().set(index);
            }
         });
         if (rc.pageCount().get() > index)
         {
            rc.selectedPageIndex().set(index);
         }
         rc.setHardwareLayout(HardwareControlType.KNOB, 8);
         mDeviceRemotePages[pageIndex] = rc;

         for (int i = 0; i < 8; i++)
         {
            markParameterInterested(rc.getParameter(i));
         }
      }
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

      mBindingManager = new HardwareBindingManager(
         mMidiIn,
         mHardwareKnobs,
         mHardwareSliders,
         mBtTrackFocus,
         mBtTrackControl,
         mBtSendUp,
         mBtSendDown,
         mBtTrackLeft,
         mBtTrackRight,
         mBtDevice,
         mBtMute,
         mBtSolo,
         mBtRecordArm,
         mKnobCcNumbers,
         mSliderCcNumbers);

      mHardwareReady = true;
      attachHardwareMatchers();
      mPendingAttach = false;
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
      // During init the LCXL sends a template-change sysex before we finish creating hardware
      // controls. If that happens, park the request and re-run once hardware is ready so we don't
      // lose all matchers (which would make every control dead).
      if (hardwareAttachShouldDefer(mHardwareReady, mBindingManager != null))
      {
         mPendingAttach = true;
         return;
      }

      final int channel = mDrumLayerActive
         ? DRUM_USER_TEMPLATE_ID
         : mDevicePagesLayerActive ? DEVICE_PAGES_USER_TEMPLATE_ID : mCurrentTemplateChannel;
      mHostActions.debug("[LCXL] attachHardwareMatchers mode=" + mMode + " channel=" + channel +
         " factoryActive=" + mFactoryTemplateActive + " arpActive=" + mArpLayerActive +
         " drumActive=" + mDrumLayerActive + " devicePagesActive=" + mDevicePagesLayerActive);

      if (mDrumLayerActive)
      {
         mBindingManager.attachDrum(
            channel,
            DrumMapping.TOP_NOTES,
            DrumMapping.BOTTOM_NOTES,
            DEVICE_NOTE,
            MUTE_NOTE,
            SOLO_NOTE,
            RECORD_ARM_NOTE,
            channel);
      }
      else
      {
         mBindingManager.attachFactory(
            channel,
            TRACK_FOCUS_NOTES,
            TRACK_CONTROL_NOTES,
            DEVICE_NOTE,
            MUTE_NOTE,
            SOLO_NOTE,
            RECORD_ARM_NOTE,
            SEND_UP_CC,
            SEND_DOWN_CC,
            TRACK_LEFT_CC,
            TRACK_RIGHT_CC,
            channel);
      }

      mPendingAttach = false;
   }

   static boolean hardwareAttachShouldDefer(final boolean hardwareReady, final boolean bindingManagerReady)
   {
      return !hardwareReady || !bindingManagerReady;
   }

   private void clearHardwareMatchers()
   {
      mHostActions.debug("[LCXL] clearHardwareMatchers");
      if (mBindingManager != null)
      {
         mBindingManager.clear();
      }
   }

   private void createLayers()
   {
      final Layers layers = new Layers(this);
      mMainLayer = new Layer(layers, "Main");

      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final int stripIndex = i;
         mMainLayer.bind(mHardwareSliders[i], value -> mFactoryLayerController.setTrackValue(
            stripIndex, FactoryLayerController.TrackValueTarget.VOLUME, value));
         mMainLayer.bindPressed(mBtTrackFocus[i], () -> mFactoryLayerController.selectTrack(stripIndex));
      }

      mMainLayer.bindPressed(mBtSendUp, () -> {
         mFactoryLayerController.scrollAllSendBanks(-1);
      });
      mMainLayer.bindPressed(mBtSendDown, () -> {
         mFactoryLayerController.scrollAllSendBanks(1);
      });
      mMainLayer.bindPressed(mBtTrackLeft, () -> mFactoryLayerController.scrollTrackPage(-1));
      mMainLayer.bindPressed(mBtTrackRight, () -> mFactoryLayerController.scrollTrackPage(1));
      mMainLayer.bindPressed(mBtDevice, () -> mFactoryLayerController.setDeviceOn(true));
      mMainLayer.bindReleased(mBtDevice, () -> mFactoryLayerController.setDeviceOn(false));
      mMainLayer.bindPressed(mBtMute, () -> mFactoryLayerController.toggleTrackControl(FactoryUiSnapshot.TrackControl.MUTE));
      mMainLayer.bindPressed(mBtSolo, () -> mFactoryLayerController.toggleTrackControl(FactoryUiSnapshot.TrackControl.SOLO));
      mMainLayer.bindPressed(mBtRecordArm,
         () -> mFactoryLayerController.toggleTrackControl(FactoryUiSnapshot.TrackControl.RECORD_ARM));

      createModeLayers(layers);
      createTrackControlsLayers(layers);
      createDeviceLayer(layers);
      createDrumLayer(layers);
      createDevicePagesLayer(layers);
   }

   private void createDeviceLayer(final Layers layers)
   {
      mDeviceLayer = new Layer(layers, "Device");
      mDeviceLayer.bindPressed(mBtTrackLeft, () -> mFactoryLayerController.selectPreviousDevice());
      mDeviceLayer.bindPressed(mBtTrackRight, () -> mFactoryLayerController.selectNextDevice());

      for (int i = 0; i < 8; ++i)
      {
         final int I = i;
         mDeviceLayer.bindPressed(mBtTrackControl[i], () -> mFactoryLayerController.selectDeviceRemotePage(I));
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
         mDrumSettings.auditionOnSelectEnabled(),
         mDrumSettings.accentMomentaryEnabled());
      mDrumSettings.auditionOnSelect().addValueObserver(value -> {
         if (mDrumLayerController != null)
            mDrumLayerController.setAuditionOnSelect(value);
      });
      mDrumSettings.accentMomentary().addValueObserver(value -> {
         if (mDrumLayerController != null)
            mDrumLayerController.setAccentMomentary(value);
      });
      mDrumLayer.bindPressed(mBtMute, mDrumLayerController::toggleMuteMode);
      mDrumLayer.bindPressed(mBtSolo, mDrumLayerController::toggleSoloMode);
      mDrumLayerController.init();
   }

   private void createDevicePagesLayer(final Layers layers)
   {
      mDevicePagesLayer = new Layer(layers, "Device Pages (User Template 6)");
      mDevicePagesController = new DevicePagesController(
         mDeviceRemotePages,
         mHardwareKnobs,
         mHardwareSliders,
         mKnobsLed,
         mBottomButtonsLed);

      // Rows 1–3 => pages 1–3, faders => page 4.
      for (int i = 0; i < 8; ++i)
      {
         mDevicePagesLayer.bind(mHardwareKnobs[i], mDeviceRemotePages[0].getParameter(i));
         mDevicePagesLayer.bind(mHardwareKnobs[8 + i], mDeviceRemotePages[1].getParameter(i));
         mDevicePagesLayer.bind(mHardwareKnobs[16 + i], mDeviceRemotePages[2].getParameter(i));
         mDevicePagesLayer.bind(mHardwareSliders[i], mDeviceRemotePages[3].getParameter(i));

         final int idx = i;
         mDevicePagesLayer.bindPressed(mBtTrackFocus[i],
            () -> toggleRemoteParameter(mDeviceRemotePages[4].getParameter(idx)));
         mDevicePagesLayer.bindPressed(mBtTrackControl[i],
            () -> toggleRemoteParameter(mDeviceRemotePages[5].getParameter(idx)));
      }

      // Page 7: parameter 0 bumped by Send Up/Down; parameter 1 bumped by Track Left/Right.
      mDevicePagesLayer.bindPressed(mBtSendUp, () -> bumpRemoteParameter(mDeviceRemotePages[6].getParameter(0), 1));
      mDevicePagesLayer.bindPressed(mBtSendDown, () -> bumpRemoteParameter(mDeviceRemotePages[6].getParameter(0), -1));
      mDevicePagesLayer.bindPressed(mBtTrackLeft, () -> bumpRemoteParameter(mDeviceRemotePages[6].getParameter(1), -1));
      mDevicePagesLayer.bindPressed(mBtTrackRight, () -> bumpRemoteParameter(mDeviceRemotePages[6].getParameter(1), 1));

      mDevicePagesLayer.setIsActive(false);
   }

   private void createModeLayers(final Layers layers)
   {
      mSend2FullDeviceLayer = new Layer(layers, "2 Sends Full Device");
      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final int stripIndex = i;
         mSend2FullDeviceLayer.bind(mHardwareKnobs[i], value -> mFactoryLayerController.setSendValue(stripIndex, 0, value));
         mSend2FullDeviceLayer.bind(mHardwareKnobs[8 + i], value -> mFactoryLayerController.setSendValue(stripIndex, 1, value));
         mSend2FullDeviceLayer.bind(mHardwareKnobs[16 + i],
            value -> mFactoryLayerController.setSelectedDeviceRemoteValue(stripIndex, value));
      }

      mSend2ProjectLayer = new Layer(layers, "2 Sends and Project Remotes");
      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final int stripIndex = i;
         mSend2ProjectLayer.bind(mHardwareKnobs[i], value -> mFactoryLayerController.setSendValue(stripIndex, 0, value));
         mSend2ProjectLayer.bind(mHardwareKnobs[8 + i], value -> mFactoryLayerController.setSendValue(stripIndex, 1, value));
         mSend2ProjectLayer.bind(mHardwareKnobs[16 + i],
            value -> mFactoryLayerController.setProjectRemoteValue(stripIndex, value));
      }

      mSend2Device1Layer = new Layer(layers, "2 Sends 1 Device");
      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final int stripIndex = i;
         mSend2Device1Layer.bind(mHardwareKnobs[i], value -> mFactoryLayerController.setSendValue(stripIndex, 0, value));
         mSend2Device1Layer.bind(mHardwareKnobs[8 + i], value -> mFactoryLayerController.setSendValue(stripIndex, 1, value));
         mSend2Device1Layer.bind(mHardwareKnobs[16 + i],
            value -> mFactoryLayerController.setDeviceRemoteValue(stripIndex, 0, value));
      }

      mSend1Device2Layer = new Layer(layers, "1 Sends 2 Device");
      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final int stripIndex = i;
         mSend1Device2Layer.bind(mHardwareKnobs[i], value -> mFactoryLayerController.setSendValue(stripIndex, 0, value));
         mSend1Device2Layer.bind(mHardwareKnobs[8 + i],
            value -> mFactoryLayerController.setDeviceRemoteValue(stripIndex, 0, value));
         mSend1Device2Layer.bind(mHardwareKnobs[16 + i],
            value -> mFactoryLayerController.setDeviceRemoteValue(stripIndex, 1, value));
      }

      mDevice3Layer = new Layer(layers, "3 Device");
      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final int stripIndex = i;
         mDevice3Layer.bind(mHardwareKnobs[i], value -> mFactoryLayerController.setDeviceRemoteValue(stripIndex, 0, value));
         mDevice3Layer.bind(mHardwareKnobs[8 + i], value -> mFactoryLayerController.setDeviceRemoteValue(stripIndex, 1, value));
         mDevice3Layer.bind(mHardwareKnobs[16 + i], value -> mFactoryLayerController.setDeviceRemoteValue(stripIndex, 2, value));
      }

      mSend2Pan1Layer = new Layer(layers, "2 Sends 1 Pan");
      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final int stripIndex = i;
         mSend2Pan1Layer.bind(mHardwareKnobs[i], value -> mFactoryLayerController.setSendValue(stripIndex, 0, value));
         mSend2Pan1Layer.bind(mHardwareKnobs[8 + i], value -> mFactoryLayerController.setSendValue(stripIndex, 1, value));
         mSend2Pan1Layer.bind(mHardwareKnobs[16 + i], value -> mFactoryLayerController.setTrackValue(
            stripIndex, FactoryLayerController.TrackValueTarget.PAN, value));
      }

      mSend3Layer = new Layer(layers, "3 Sends");
      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final int stripIndex = i;
         mSend3Layer.bind(mHardwareKnobs[i], value -> mFactoryLayerController.setSendValue(stripIndex, 0, value));
         mSend3Layer.bind(mHardwareKnobs[8 + i], value -> mFactoryLayerController.setSendValue(stripIndex, 1, value));
         mSend3Layer.bind(mHardwareKnobs[16 + i], value -> mFactoryLayerController.setSendValue(stripIndex, 2, value));
      }

      mTrack3layer = new Layer(layers, "3 Track Remotes");
      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final int stripIndex = i;
         mTrack3layer.bind(mHardwareKnobs[i], value -> mFactoryLayerController.setTrackRemoteValue(stripIndex, 0, value));
         mTrack3layer.bind(mHardwareKnobs[8 + i], value -> mFactoryLayerController.setTrackRemoteValue(stripIndex, 1, value));
         mTrack3layer.bind(mHardwareKnobs[16 + i], value -> mFactoryLayerController.setTrackRemoteValue(stripIndex, 2, value));
      }
   }

   private void createTrackControlsLayers(final Layers layers)
   {
      mMuteLayer = new Layer(layers, "Mute");
      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final int stripIndex = i;
         mMuteLayer.bindPressed(mBtTrackControl[i], () -> mFactoryLayerController.toggleTrackBoolean(
            stripIndex, FactoryLayerController.TrackBooleanTarget.MUTE));
      }

      mSoloLayer = new Layer(layers, "Solo");
      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final int stripIndex = i;
         mSoloLayer.bindPressed(mBtTrackControl[i], () -> mFactoryLayerController.toggleTrackBoolean(
            stripIndex, FactoryLayerController.TrackBooleanTarget.SOLO));
      }

      mRecordArmLayer = new Layer(layers, "Record Arm");
      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final int stripIndex = i;
         mRecordArmLayer.bindPressed(mBtTrackControl[i], () -> mFactoryLayerController.toggleTrackBoolean(
            stripIndex, FactoryLayerController.TrackBooleanTarget.ARM));
      }

      mTrackRemoteButtonLayer = new Layer(layers, "Track Remote Button");
      for (int i = 0; i < 8; ++i)
      {
         final int stripIndex = i;
         mTrackRemoteButtonLayer.bindPressed(mBtTrackControl[i],
            () -> mFactoryLayerController.toggleTrackRemoteButton(stripIndex));
      }
   }

   private void setFactoryLayersEnabled(final boolean enabled)
   {
      mHostActions.debug("[LCXL] setFactoryLayersEnabled=" + enabled);
      if (enabled)
      {
         // Reactivate only the current mode-specific layers; do not turn on every layer at once.
         mMainLayer.setIsActive(true);
         selectMode(mMode);
         mFactoryLayerController.setTrackControl(mFactoryLayerController.trackControl());
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
      // First give the arp layer exclusive access to its channel.
      if (handleArpMidi(status, data1, data2))
      {
         return;
      }
      // Then let the drum layer consume user-template events while active.
      if (mDrumLayerActive && mDrumLayerController != null && mDrumLayerController.handleMidi(status, data1, data2))
      {
         return;
      }
      if (mDrumLayerActive)
      {
         mHostActions.debug(String.format("[LCXL] DRUM MIDI status=%02X data1=%02X data2=%02X (channel %d)",
            status, data1, data2, status & 0x0F));
      }
      if (mDevicePagesLayerActive)
      {
         mHostActions.debug(String.format("[LCXL] DEVICE-PAGES MIDI status=%02X data1=%02X data2=%02X (channel %d)",
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

   private void toggleRemoteParameter(final RemoteControl param)
   {
      if (param == null || !param.exists().get())
      {
         return;
      }
      final double current = param.value().get();
      param.value().set(current > 0 ? 0 : 127, 127);
   }

   private void bumpRemoteParameter(final RemoteControl param, final double delta)
   {
      if (param == null || !param.exists().get())
      {
         return;
      }
      final double current = param.value().get();
      final double next = Math.max(0, Math.min(127, current + delta));
      param.value().set(next, 127);
   }

   private void setRemoteValue(final RemoteControl param, final double value)
   {
      if (param == null || !param.exists().get())
      {
         return;
      }
      param.value().set(value);
   }

   private void setTrackValue(final int stripIndex, final TrackValueTarget target, final double value)
   {
      final Track track = trackForStrip(stripIndex);
      if (track == null)
      {
         return;
      }
      switch (target)
      {
         case VOLUME -> track.volume().set(value);
         case PAN -> track.pan().set(value);
      }
   }

   private void setSendValue(final int stripIndex, final int sendIndex, final double value)
   {
      final SendBank sendBank = sendBankForStrip(stripIndex);
      if (sendBank == null)
      {
         return;
      }
      final SettableRangedValue send = sendBank.getItemAt(sendIndex).value();
      if (sendBank.getItemAt(sendIndex).exists().get())
      {
         send.set(value);
      }
   }

   static boolean trackBooleanShouldSelect(final TrackBooleanTarget target, final boolean exclusiveTrackArmEnabled)
   {
      return target == TrackBooleanTarget.ARM && exclusiveTrackArmEnabled;
   }

   private RemoteControl deviceParamForStrip(final int stripIndex, final int paramIndex)
   {
      return trackForStrip(stripIndex) != null ? deviceParam(stripIndex, paramIndex) : null;
   }

   private RemoteControl trackParamForStrip(final int stripIndex, final int paramIndex)
   {
      return trackForStrip(stripIndex) != null ? mTrackRemoteControls[stripIndex].getParameter(paramIndex) : null;
   }

   private SendBank sendBankForStrip(final int stripIndex)
   {
      final Track track = trackForStrip(stripIndex);
      return track != null ? track.sendBank() : null;
   }

   private Track trackForStrip(final int stripIndex)
   {
      if (stripIndex < 0 || stripIndex >= STRIP_COUNT)
      {
         return null;
      }
      final Track track = mTrackBank.getItemAt(stripIndex);
      return isControllableTrack(track) ? track : null;
   }

   private boolean isControllableTrack(final Track track)
   {
      return track != null
         && track.exists().get()
         && (mDrumSettings != null && mDrumSettings.showDeactivatedTracksEnabled() || track.isActivated().get());
   }

   static boolean trackVisibilityShouldSkipDisabled(final boolean showDeactivatedTracks)
   {
      return !showDeactivatedTracks;
   }

   private void applyTrackVisibilityPreference(final boolean showDeactivatedTracks)
   {
      if (mTrackBank != null)
      {
         mTrackBank.setSkipDisabledItems(trackVisibilityShouldSkipDisabled(showDeactivatedTracks));
      }
   }

   /**
    * Returns a device remote parameter for the given track slot, falling back to track remotes when
    * per-track cursor devices are unavailable (Bitwig 6 compatibility).
    */
   private RemoteControl deviceParam(final int trackIndex, final int paramIndex)
   {
      final CursorRemoteControlsPage devicePage = mTrackCursorDeviceRemoteControls[trackIndex];
      if (devicePage != null)
      {
         return devicePage.getParameter(paramIndex);
      }
      return mTrackRemoteControls[trackIndex].getParameter(paramIndex);
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

   /**
    * Engage or disengage the single-device pages user layer (user template 6). Rebinds the hardware
    * to the dedicated remote pages and shows a popup when activated.
    */
   private void setDevicePagesLayerActive(final boolean active)
   {
      if (mDevicePagesLayer == null || mDevicePagesLayerActive == active)
      {
         return;
      }
      mDevicePagesLayerActive = active;
      if (active)
      {
         mHostActions.debug("[LCXL] device pages layer engaged (user template 6)");
         attachHardwareMatchers();
         mDevicePagesLayer.setIsActive(true);
         mHostActions.showPopup("Device pages layer active (Template 6)");
      }
      else
      {
         mHostActions.debug("[LCXL] device pages layer disengaged");
         mDevicePagesLayer.setIsActive(false);
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
               if (mPendingAttach)
               {
                  attachHardwareMatchers();
               }
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
         if (mPendingAttach)
         {
            attachHardwareMatchers();
         }
      }
   }

   private void selectMode(final Mode mode)
   {
      mMode = mode;
      mFactoryLayerController.selectMode(toFactoryMode(mode));
      mHostActions.showPopup(mode.getNotification());
   }

   private void setSizeOfSendBank(final int size)
   {
      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final Track track = mTrackBank.getItemAt(i);
         final SendBank sendBank = track.sendBank();
         sendBank.setSizeOfBank(size);
      }
   }

   private final class FactoryPort implements FactoryLayerController.Port
   {
      @Override
      public boolean trackExists(final int strip)
      {
         return trackForStrip(strip) != null;
      }

      @Override
      public boolean trackArm(final int strip)
      {
         final Track track = trackForStrip(strip);
         return track != null && track.arm().get();
      }

      @Override
      public void setTrackArm(final int strip, final boolean value)
      {
         final Track track = trackForStrip(strip);
         if (track != null)
            track.arm().set(value);
      }

      @Override
      public void toggleTrackMute(final int strip)
      {
         final Track track = trackForStrip(strip);
         if (track != null)
            track.mute().toggle();
      }

      @Override
      public void toggleTrackSolo(final int strip)
      {
         final Track track = trackForStrip(strip);
         if (track != null)
            track.solo().toggle(false);
      }

      @Override
      public void selectTrack(final int strip)
      {
         final Track track = trackForStrip(strip);
         if (track != null)
            mCursorTrack.selectChannel(track);
      }

      @Override
      public void scrollSendBank(final int strip, final int direction)
      {
         final SendBank sendBank = sendBankForStrip(strip);
         if (sendBank == null)
            return;
         if (direction < 0)
            sendBank.scrollBackwards();
         else if (direction > 0)
            sendBank.scrollForwards();
      }

      @Override
      public boolean canScrollTrackPage(final int direction)
      {
         return direction < 0 ? mTrackBank.canScrollBackwards().get()
            : direction > 0 && mTrackBank.canScrollForwards().get();
      }

      @Override
      public void scrollTrackPage(final int direction)
      {
         if (direction < 0)
            mTrackBank.scrollPageBackwards();
         else if (direction > 0)
            mTrackBank.scrollPageForwards();
      }

      @Override
      public void scheduleFirstTrackSelection()
      {
         mHost.scheduleTask(() -> mFactoryLayerController.selectTrack(0), TRACK_PAGE_SELECTION_DELAY_MS);
      }

      @Override
      public void setTrackValue(final int strip, final FactoryLayerController.TrackValueTarget target,
                                final double value)
      {
         LaunchControlXlControllerExtension.this.setTrackValue(strip,
            target == FactoryLayerController.TrackValueTarget.VOLUME ? TrackValueTarget.VOLUME : TrackValueTarget.PAN,
            value);
      }

      @Override
      public void setSendValue(final int strip, final int send, final double value)
      {
         LaunchControlXlControllerExtension.this.setSendValue(strip, send, value);
      }

      @Override
      public void setDeviceRemoteValue(final int strip, final int parameter, final double value)
      {
         setRemoteValue(deviceParamForStrip(strip, parameter), value);
      }

      @Override
      public void setTrackRemoteValue(final int strip, final int parameter, final double value)
      {
         setRemoteValue(trackParamForStrip(strip, parameter), value);
      }

      @Override
      public void setSelectedDeviceRemoteValue(final int parameter, final double value)
      {
         setRemoteValue(mRemoteControls.getParameter(parameter), value);
      }

      @Override
      public void setProjectRemoteValue(final int parameter, final double value)
      {
         setRemoteValue(mProjectRemoteControlsCursor.getParameter(parameter), value);
      }

      @Override
      public void toggleTrackRemoteButton(final int strip)
      {
         toggleRemoteParameter(trackParamForStrip(strip, 3));
      }

      @Override
      public void selectDeviceRemotePage(final int page)
      {
         mRemoteControls.selectedPageIndex().set(page);
      }

      @Override public void selectPreviousDevice() { mCursorDevice.selectPrevious(); }
      @Override public void selectNextDevice() { mCursorDevice.selectNext(); }

      @Override
      public void applyMode(final FactoryUiSnapshot.Mode mode)
      {
         mSend2Device1Layer.setIsActive(mode == FactoryUiSnapshot.Mode.SEND_2_DEVICE_1);
         mSend2Pan1Layer.setIsActive(mode == FactoryUiSnapshot.Mode.SEND_2_PAN_1);
         mSend3Layer.setIsActive(mode == FactoryUiSnapshot.Mode.SEND_3);
         mSend1Device2Layer.setIsActive(mode == FactoryUiSnapshot.Mode.SEND_1_DEVICE_2);
         mDevice3Layer.setIsActive(mode == FactoryUiSnapshot.Mode.DEVICE_3);
         mTrack3layer.setIsActive(mode == FactoryUiSnapshot.Mode.TRACK_3);
         mSend2FullDeviceLayer.setIsActive(mode == FactoryUiSnapshot.Mode.SEND_2_FULL_DEVICE);
         mSend2ProjectLayer.setIsActive(mode == FactoryUiSnapshot.Mode.SEND_2_PROJECT);
      }

      @Override public void setSendBankSize(final int size) { setSizeOfSendBank(size); }

      @Override
      public void applyTrackControl(final FactoryUiSnapshot.TrackControl control)
      {
         final TrackControl drumTrackControl = fromFactoryTrackControl(control);
         mMuteLayer.setIsActive(control == FactoryUiSnapshot.TrackControl.MUTE);
         mSoloLayer.setIsActive(control == FactoryUiSnapshot.TrackControl.SOLO);
         mRecordArmLayer.setIsActive(control == FactoryUiSnapshot.TrackControl.RECORD_ARM);
         mTrackRemoteButtonLayer.setIsActive(control == FactoryUiSnapshot.TrackControl.NONE);
         if (mDrumLayerController != null)
            mDrumLayerController.setTrackControlMode(drumTrackControl);
         if (mDrumLayerActive)
         {
            switch (control)
            {
               case MUTE -> mHostActions.showPopup("Drum layer: Mute buttons");
               case NONE -> mHostActions.showPopup("Drum layer: Default buttons");
               default -> { }
            }
         }
      }

      @Override
      public void applyDeviceOn(final boolean deviceOn)
      {
         mDeviceLayer.setIsActive(deviceOn);
      }
   }

   private void onSysex(final String sysex)
   {
      if (mIgnoreNextSysex)
      {
         mIgnoreNextSysex = false;
         return;
      }

      // Pass sysex to the arp layer first so it can react to its own messages (e.g., timing changes).
      if (mArpLayerController != null)
      {
         mArpLayerController.handleSysex(sysex);
      }

      // Template-change sysex drives factory/user mode switches and layer activation.
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
            final boolean devicePagesTemplate = id == DEVICE_PAGES_USER_TEMPLATE_ID;
            mCurrentTemplateChannel = id;
            if (arpTemplate)
            {
               mHostActions.debug("[LCXL] handleTemplateChange -> arp template (user template 8)");
            }
            else
            {
               mHostActions.debug("[LCXL] handleTemplateChange -> user template " + (id + 1) +
                  (drumTemplate ? " (drum)" : devicePagesTemplate ? " (device pages)" : ""));
            }
            mFactoryTemplateActive = false;
            setFactoryLayersEnabled(false);
            setArpLayerActive(arpTemplate);
            setDrumLayerActive(drumTemplate);
            setDevicePagesLayerActive(devicePagesTemplate);
         }
         else
         {
            mHostActions.debug("[LCXL] handleTemplateChange -> factory template channel " + id);
            mCurrentTemplateChannel = id;
            mFactoryTemplateActive = true;
            setArpLayerActive(false);
            setDrumLayerActive(false);
            setDevicePagesLayerActive(false);
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
      if (!mFactoryTemplateActive && !mArpLayerActive && !mDrumLayerActive && !mDevicePagesLayerActive)
      {
         return;
      }

      final FactoryLedRenderer.LedFrame factoryFrame = mFactoryTemplateActive || mArpLayerActive
         ? FactoryLedRenderer.render(factoryUiSnapshot())
         : null;
      paintRightButtons(factoryFrame);
      paintKnobs(factoryFrame);
      paintBottomButtons(factoryFrame);

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

   private void paintBottomButtons(final FactoryLedRenderer.LedFrame factoryFrame)
   {
      if (mDrumLayerActive)
      {
         paintDrumButtons();
         return;
      }

      if (mDevicePagesLayerActive)
      {
         paintDevicePagesButtons();
         return;
      }

      final FactoryLedRenderer.LedFrame frame = factoryFrame;
      final int[] topButtons = frame.topButtons();
      final int[] bottomButtons = frame.bottomButtons();
      for (int i = 0; i < STRIP_COUNT; ++i)
      {
         final int focusColor = mArpLayerController != null
            ? mArpLayerController.applyFocusColor(i, topButtons[i])
            : topButtons[i];
         mBottomButtonsLed[i].setColor(focusColor);
         final int appliedControlColor = mArpLayerController != null
            ? mArpLayerController.applyControlColor(i, bottomButtons[i])
            : bottomButtons[i];
         mBottomButtonsLed[8 + i].setColor(appliedControlColor);
      }
   }

   private FactoryUiSnapshot factoryUiSnapshot()
   {
      final FactoryUiSnapshot.Strip[] strips = new FactoryUiSnapshot.Strip[STRIP_COUNT];
      final FactoryUiSnapshot.Value[] deviceRemotes = new FactoryUiSnapshot.Value[STRIP_COUNT];
      final FactoryUiSnapshot.Value[] projectRemotes = new FactoryUiSnapshot.Value[STRIP_COUNT];
      for (int i = 0; i < STRIP_COUNT; i++)
      {
         final Track track = trackForStrip(i);
         if (track == null)
         {
            strips[i] = FactoryUiSnapshot.Strip.missing();
         }
         else
         {
            final SendBank sends = track.sendBank();
            final FactoryUiSnapshot.Value[] sendValues = new FactoryUiSnapshot.Value[3];
            final FactoryUiSnapshot.Value[] deviceValues = new FactoryUiSnapshot.Value[3];
            final FactoryUiSnapshot.Value[] trackValues = new FactoryUiSnapshot.Value[3];
            for (int parameter = 0; parameter < 3; parameter++)
            {
               sendValues[parameter] = sends != null && sends.getItemAt(parameter).exists().get()
                  ? FactoryUiSnapshot.Value.of(sends.getItemAt(parameter).value().get())
                  : FactoryUiSnapshot.Value.missing();
               deviceValues[parameter] = snapshotValue(deviceParamForStrip(i, parameter));
               trackValues[parameter] = snapshotValue(trackParamForStrip(i, parameter));
            }
            strips[i] = FactoryUiSnapshot.Strip.existing(track.mute().get(), track.solo().get(), track.arm().get(),
               snapshotValue(trackParamForStrip(i, 3)), sendValues, deviceValues, trackValues);
         }
         deviceRemotes[i] = snapshotValue(mRemoteControls.getParameter(i));
         projectRemotes[i] = snapshotValue(mProjectRemoteControlsCursor.getParameter(i));
      }
      final SendBank firstSends = sendBankForStrip(0);
      return new FactoryUiSnapshot(factorySurface(), factoryMode(), mFactoryLayerController.trackControl(),
         mFactoryLayerController.deviceOn(),
         mRemoteControls.selectedPageIndex().get(), mRemoteControls.pageCount().get(),
         mTrackBank.cursorIndex().get(), strips, deviceRemotes, projectRemotes,
         firstSends != null && firstSends.canScrollBackwards().get(),
         firstSends != null && firstSends.canScrollForwards().get(),
         mTrackBank.canScrollBackwards().get(), mTrackBank.canScrollForwards().get(),
         mCursorDevice.hasPrevious().get(), mCursorDevice.hasNext().get());
   }

   private FactoryUiSnapshot.Value snapshotValue(final RemoteControl parameter)
   {
      return parameter != null && parameter.exists().get()
         ? FactoryUiSnapshot.Value.of(parameter.value().get())
         : FactoryUiSnapshot.Value.missing();
   }

   private FactoryUiSnapshot.Mode factoryMode()
   {
      return toFactoryMode(mMode);
   }

   private static FactoryUiSnapshot.Mode toFactoryMode(final Mode mode)
   {
      return switch (mode)
      {
         case Send2FullDevice -> FactoryUiSnapshot.Mode.SEND_2_FULL_DEVICE;
         case Send2Device1 -> FactoryUiSnapshot.Mode.SEND_2_DEVICE_1;
         case Send2Project -> FactoryUiSnapshot.Mode.SEND_2_PROJECT;
         case Send3 -> FactoryUiSnapshot.Mode.SEND_3;
         case Send1Device2 -> FactoryUiSnapshot.Mode.SEND_1_DEVICE_2;
         case Device3 -> FactoryUiSnapshot.Mode.DEVICE_3;
         case Track3 -> FactoryUiSnapshot.Mode.TRACK_3;
         case None -> FactoryUiSnapshot.Mode.NONE;
         case Send2Pan1 -> FactoryUiSnapshot.Mode.SEND_2_PAN_1;
      };
   }

   private FactoryUiSnapshot.Surface factorySurface()
   {
      return factorySurface(mFactoryTemplateActive, mArpLayerActive, mDrumLayerActive, mDevicePagesLayerActive);
   }

   static FactoryUiSnapshot.Surface factorySurface(final boolean factoryTemplateActive,
                                                   final boolean arpLayerActive,
                                                   final boolean drumLayerActive,
                                                   final boolean devicePagesLayerActive)
   {
      if (drumLayerActive)
         return FactoryUiSnapshot.Surface.DRUM;
      if (arpLayerActive)
         return FactoryUiSnapshot.Surface.ARP;
      if (devicePagesLayerActive)
         return FactoryUiSnapshot.Surface.DEVICE_PAGES;
      return factoryTemplateActive ? FactoryUiSnapshot.Surface.FACTORY : FactoryUiSnapshot.Surface.RAW_USER;
   }

   private static TrackControl fromFactoryTrackControl(final FactoryUiSnapshot.TrackControl trackControl)
   {
      return switch (trackControl)
      {
         case NONE -> TrackControl.None;
         case MUTE -> TrackControl.Mute;
         case SOLO -> TrackControl.Solo;
         case RECORD_ARM -> TrackControl.RecordArm;
      };
   }

   private void paintDrumButtons()
   {
      final boolean soloMode = mDrumLayerController != null && mDrumLayerController.isSoloMode();
      final TrackControl drumControlMode = mDrumLayerController != null ? mDrumLayerController.getTrackControlMode() : TrackControl.None;
      final int selectedPad = mDrumLayerController != null ? mDrumLayerController.getSelectedPadIndex() : -1;

      final DrumLedRenderer.PadState [] pads = new DrumLedRenderer.PadState[DrumLayerController.PADS_PER_BANK];
      if (mDrumPadBank != null)
      {
         for (int i = 0; i < DrumLayerController.PADS_PER_BANK; i++)
         {
            final DrumPad pad = mDrumPadBank.getItemAt(i);
            final boolean exists = pad.exists().get();
            final boolean mute = exists && pad.mute().get();
            final boolean solo = exists && pad.solo().get();
            final RemoteControlsPage rc = mDrumPadRemoteControls[i];
            final RemoteControl param = rc != null ? rc.getParameter(3) : null;
            final boolean paramExists = param != null && param.exists().get();
            final double value = paramExists ? param.value().get() : 0;
            pads[i] = new DrumLedRenderer.PadState(exists, mute, solo, paramExists, value);
         }
      }

      // Produce a snapshot of pad state and delegate LED colour decisions to the renderer.
      final DrumUiState state = new DrumUiState(pads, selectedPad, soloMode, drumControlMode == TrackControl.Mute);
      final DrumLedRenderer.LedFrame frame = DrumLedRenderer.render(state);
      for (int i = 0; i < DrumLayerController.PADS_PER_BANK; i++)
      {
         mBottomButtonsLed[i].setColor(frame.topColors()[i]);
         mBottomButtonsLed[8 + i].setColor(frame.bottomColors()[i]);
      }
   }

   private void paintDevicePagesKnobs()
   {
      if (mDevicePagesController != null)
         mDevicePagesController.paintKnobs();
   }

   private void paintDevicePagesButtons()
   {
      if (mDevicePagesController != null)
         mDevicePagesController.paintButtons();
   }

   private void paintKnobs(final FactoryLedRenderer.LedFrame factoryFrame)
   {
      if (mDrumLayerActive)
      {
         paintDrumKnobs();
         return;
      }

      if (mDevicePagesLayerActive)
      {
         paintDevicePagesKnobs();
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

      final int[] colors = factoryFrame.knobs();
      for (int i = 0; i < colors.length; i++)
         mKnobsLed[i].setColor(colors[i]);
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

   private void paintRightButtons(final FactoryLedRenderer.LedFrame factoryFrame)
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

      final int[] colors = factoryFrame.rightButtons();
      mDeviceLed.setColor(colors[0]);
      mMuteLed.setColor(colors[1]);
      mSoloLed.setColor(colors[2]);
      mRecordArmLed.setColor(colors[3]);
      mUpButtonLed.setColor(colors[4]);
      mDownButtonLed.setColor(colors[5]);
      mLeftButtonLed.setColor(colors[6]);
      mRightButtonLed.setColor(colors[7]);
   }

   private ControllerHost mHost;
   private HostNotifications mHostActions;
   private MidiIn mMidiIn;
   private MidiOut mMidiOut;
   private HardwareBindingManager mBindingManager;
   private NoteInput mUserModeNoteInput;
   private TrackBank mTrackBank;
   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;
   private final CursorRemoteControlsPage[] mDeviceRemotePages = new CursorRemoteControlsPage[7];
   private final CursorDevice[] mTrackDeviceCursors = new CursorDevice[STRIP_COUNT];
   private final CursorRemoteControlsPage[] mTrackCursorDeviceRemoteControls = new CursorRemoteControlsPage[STRIP_COUNT];
   private final CursorRemoteControlsPage[] mTrackRemoteControls = new CursorRemoteControlsPage[STRIP_COUNT];
   private boolean mDevicePerTrackSupported;
   private CursorRemoteControlsPage mProjectRemoteControlsCursor;
   private RhArpLayerController mArpLayerController;
   private DrumLayerController mDrumLayerController;
   private DevicePagesController mDevicePagesController;
   private FactoryLayerController mFactoryLayerController;
   private DeviceLocator mDeviceLocator;
   private DrumSettings mDrumSettings;
   private DrumPadBank mDrumPadBank;
   private final RemoteControlsPage[] mDrumPadRemoteControls =
      new RemoteControlsPage[DrumLayerController.PADS_PER_BANK];

   private boolean mIgnoreNextSysex = false;
   private boolean mFactoryTemplateActive = true;
   private boolean mArpLayerActive = false;
   private boolean mDrumLayerActive = false;
   private boolean mDevicePagesLayerActive = false;
   private int mCurrentTemplateChannel = Mode.Send2FullDevice.getChannel();
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
   private SettableBooleanValue mExclusiveTrackArm;
   private boolean mExclusiveTrackArmEnabled = EXCLUSIVE_TRACK_ARM_DEFAULT;
   // Template-change sysex can arrive before the hardware is constructed; these flags ensure we
   // defer binding until the manager and controls exist.
   private boolean mHardwareReady;
   private boolean mPendingAttach;

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
   private Layer mDevicePagesLayer;

   static void logDebug(final String message)
   {
      if (DEBUG_TELEMETRY && sHostActions != null)
      {
         sHostActions.debug(message);
      }
   }

}
