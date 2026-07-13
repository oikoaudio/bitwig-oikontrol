package com.bitwig.extensions.controllers.novation.launch_control_xl;

import com.bitwig.extensions.controllers.novation.launch_control_xl.factory.FactoryLayerController;
import com.bitwig.extensions.controllers.novation.launch_control_xl.factory.FactoryUiSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchControlXlTrackControlTest {

   @Test
   void factoryTrackBankMatchesPhysicalStripCount() {
      assertEquals(8, LaunchControlXlControllerExtension.STRIP_COUNT);
   }

   @Test
   void trackPageScrollSelectsAfterBitwigRefreshDelay() {
      assertEquals(75, LaunchControlXlControllerExtension.TRACK_PAGE_SELECTION_DELAY_MS);
   }

   @Test
   void hiddenDeactivatedTracksUseBitwigDisabledItemSkipping() {
      assertEquals(true, LaunchControlXlControllerExtension.trackVisibilityShouldSkipDisabled(false));
      assertEquals(false, LaunchControlXlControllerExtension.trackVisibilityShouldSkipDisabled(true));
   }

   @Test
   void matcherAttachmentDefersUntilHardwareAndManagerAreReady() {
      assertTrue(LaunchControlXlControllerExtension.hardwareAttachShouldDefer(false, false));
      assertTrue(LaunchControlXlControllerExtension.hardwareAttachShouldDefer(true, false));
      assertTrue(LaunchControlXlControllerExtension.hardwareAttachShouldDefer(false, true));
      assertFalse(LaunchControlXlControllerExtension.hardwareAttachShouldDefer(true, true));
   }

   @Test
   void surfaceClassificationPreservesFactoryRawAndCustomTemplatePrecedence() {
      assertEquals(FactoryUiSnapshot.Surface.FACTORY,
         LaunchControlXlControllerExtension.factorySurface(true, false, false, false));
      assertEquals(FactoryUiSnapshot.Surface.RAW_USER,
         LaunchControlXlControllerExtension.factorySurface(false, false, false, false));
      assertEquals(FactoryUiSnapshot.Surface.DRUM,
         LaunchControlXlControllerExtension.factorySurface(true, false, true, false));
      assertEquals(FactoryUiSnapshot.Surface.ARP,
         LaunchControlXlControllerExtension.factorySurface(true, true, false, false));
      assertEquals(FactoryUiSnapshot.Surface.DEVICE_PAGES,
         LaunchControlXlControllerExtension.factorySurface(true, false, false, true));
   }

   @Test
   void onlyExclusiveArmFollowsTrackSelection() {
      assertEquals(false, LaunchControlXlControllerExtension.trackBooleanShouldSelect(
         LaunchControlXlControllerExtension.TrackBooleanTarget.MUTE, true));
      assertEquals(false, LaunchControlXlControllerExtension.trackBooleanShouldSelect(
         LaunchControlXlControllerExtension.TrackBooleanTarget.SOLO, true));
      assertEquals(false, LaunchControlXlControllerExtension.trackBooleanShouldSelect(
         LaunchControlXlControllerExtension.TrackBooleanTarget.ARM, false));
      assertEquals(true, LaunchControlXlControllerExtension.trackBooleanShouldSelect(
         LaunchControlXlControllerExtension.TrackBooleanTarget.ARM, true));
   }

   @Test
   void extractedControllerOwnsTrackControlToggleState() {
      final FakeFactoryPort port = new FakeFactoryPort();
      final FactoryLayerController controller = new FactoryLayerController(port, 8);

      controller.toggleTrackControl(FactoryUiSnapshot.TrackControl.MUTE);
      assertEquals(FactoryUiSnapshot.TrackControl.MUTE, controller.trackControl());
      assertEquals(FactoryUiSnapshot.TrackControl.MUTE, port.appliedTrackControl);

      controller.toggleTrackControl(FactoryUiSnapshot.TrackControl.MUTE);
      assertEquals(FactoryUiSnapshot.TrackControl.NONE, controller.trackControl());
      assertEquals(FactoryUiSnapshot.TrackControl.NONE, port.appliedTrackControl);
   }

   @Test
   void extractedControllerPreservesExclusiveArmAndCursorFollow() {
      final FakeFactoryPort port = new FakeFactoryPort();
      port.exists[0] = true;
      port.exists[1] = true;
      port.arm[0] = true;
      final FactoryLayerController controller = new FactoryLayerController(port, 8);
      controller.setExclusiveTrackArmEnabled(true);

      controller.toggleTrackBoolean(1, FactoryLayerController.TrackBooleanTarget.ARM);

      assertFalse(port.arm[0]);
      assertTrue(port.arm[1]);
      assertEquals(1, port.selectedTrack);
   }

   @Test
   void extractedControllerChecksTrackPagingAndSelectsAfterRefresh() {
      final FakeFactoryPort port = new FakeFactoryPort();
      final FactoryLayerController controller = new FactoryLayerController(port, 8);

      controller.scrollTrackPage(-1);
      assertEquals(0, port.trackPageMoves);

      port.canScrollBack = true;
      controller.scrollTrackPage(-1);
      assertEquals(-1, port.trackPageMoves);
      assertTrue(port.firstTrackSelectionScheduled);
   }

   @Test
   void extractedControllerRoutesFactoryValuesPagesAndModePolicy() {
      final FakeFactoryPort port = new FakeFactoryPort();
      port.exists[2] = true;
      final FactoryLayerController controller = new FactoryLayerController(port, 8);

      controller.setTrackValue(2, FactoryLayerController.TrackValueTarget.PAN, 0.5);
      controller.setSendValue(2, 1, 0.75);
      controller.setDeviceRemoteValue(2, 0, 0.25);
      controller.setTrackRemoteValue(2, 1, 0.9);
      controller.toggleTrackRemoteButton(2);
      controller.selectDeviceRemotePage(3);
      controller.selectMode(FactoryUiSnapshot.Mode.SEND_3);

      assertEquals("track:2:PAN:0.5", port.lastValueAction);
      assertEquals("send:2:1:0.75", port.lastSendAction);
      assertEquals("device:2:0:0.25", port.lastDeviceAction);
      assertEquals("track-remote:2:1:0.9", port.lastTrackRemoteAction);
      assertEquals(2, port.toggledTrackRemote);
      assertEquals(3, port.selectedDevicePage);
      assertEquals(FactoryUiSnapshot.Mode.SEND_3, port.appliedMode);
      assertEquals(3, port.sendBankSize);
   }

   private static final class FakeFactoryPort implements FactoryLayerController.Port {
      private final boolean[] exists = new boolean[8];
      private final boolean[] arm = new boolean[8];
      private FactoryUiSnapshot.TrackControl appliedTrackControl;
      private FactoryUiSnapshot.Mode appliedMode;
      private int sendBankSize;
      private int selectedTrack = -1;
      private int trackPageMoves;
      private boolean canScrollBack;
      private boolean firstTrackSelectionScheduled;
      private String lastValueAction;
      private String lastSendAction;
      private String lastDeviceAction;
      private String lastTrackRemoteAction;
      private int toggledTrackRemote = -1;
      private int selectedDevicePage = -1;

      @Override public boolean trackExists(final int strip) { return exists[strip]; }
      @Override public boolean trackArm(final int strip) { return arm[strip]; }
      @Override public void setTrackArm(final int strip, final boolean value) { arm[strip] = value; }
      @Override public void toggleTrackMute(final int strip) { }
      @Override public void toggleTrackSolo(final int strip) { }
      @Override public void selectTrack(final int strip) { selectedTrack = strip; }
      @Override public void scrollSendBank(final int strip, final int direction) { }
      @Override public boolean canScrollTrackPage(final int direction) { return direction < 0 && canScrollBack; }
      @Override public void scrollTrackPage(final int direction) { trackPageMoves += direction; }
      @Override public void scheduleFirstTrackSelection() { firstTrackSelectionScheduled = true; }
      @Override public void setTrackValue(final int strip, final FactoryLayerController.TrackValueTarget target,
                                          final double value) {
         lastValueAction = "track:" + strip + ":" + target + ":" + value;
      }
      @Override public void setSendValue(final int strip, final int send, final double value) {
         lastSendAction = "send:" + strip + ":" + send + ":" + value;
      }
      @Override public void setDeviceRemoteValue(final int strip, final int parameter, final double value) {
         lastDeviceAction = "device:" + strip + ":" + parameter + ":" + value;
      }
      @Override public void setTrackRemoteValue(final int strip, final int parameter, final double value) {
         lastTrackRemoteAction = "track-remote:" + strip + ":" + parameter + ":" + value;
      }
      @Override public void setSelectedDeviceRemoteValue(final int parameter, final double value) { }
      @Override public void setProjectRemoteValue(final int parameter, final double value) { }
      @Override public void toggleTrackRemoteButton(final int strip) { toggledTrackRemote = strip; }
      @Override public void selectDeviceRemotePage(final int page) { selectedDevicePage = page; }
      @Override public void selectPreviousDevice() { }
      @Override public void selectNextDevice() { }
      @Override public void applyMode(final FactoryUiSnapshot.Mode mode) { appliedMode = mode; }
      @Override public void setSendBankSize(final int size) { sendBankSize = size; }
      @Override public void applyTrackControl(final FactoryUiSnapshot.TrackControl control) {
         appliedTrackControl = control;
      }
      @Override public void applyDeviceOn(final boolean deviceOn) { }
   }
}
