package com.bitwig.extensions.controllers.novation.launch_control_xl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LaunchControlXlTrackControlTest {

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
