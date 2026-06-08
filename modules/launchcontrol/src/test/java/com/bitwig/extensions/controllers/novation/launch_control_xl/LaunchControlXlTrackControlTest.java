package com.bitwig.extensions.controllers.novation.launch_control_xl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
