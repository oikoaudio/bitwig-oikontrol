package com.bitwig.extensions.controllers.novation.launch_control_xl.drum;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccentModeLogicTest
{
   @Test
   void momentaryPressAndRelease ()
   {
      assertEquals (127, AccentModeLogic.nextValue (true, true, 0));
      assertEquals (0, AccentModeLogic.nextValue (true, false, 127));
   }

   @Test
   void togglePressTogglesState ()
   {
      assertEquals (127, AccentModeLogic.nextValue (false, true, 0));
      assertEquals (0, AccentModeLogic.nextValue (false, true, 64));
   }

   @Test
   void toggleReleaseKeepsState ()
   {
      assertEquals (64, AccentModeLogic.nextValue (false, false, 64));
      assertEquals (0, AccentModeLogic.nextValue (false, false, 0));
   }
}
