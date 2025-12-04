package com.bitwig.extensions.controllers.novation.launch_control_xl.drum;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DrumLedRendererTest
{
   @Test
   void highlightsSelectedPad ()
   {
      final DrumLedRenderer.PadState [] pads = new DrumLedRenderer.PadState[8];
      pads[0] = new DrumLedRenderer.PadState(true, false, false, false, 0);
      pads[1] = new DrumLedRenderer.PadState(true, false, false, false, 0);
      final DrumUiState state = new DrumUiState(pads, 1, false, false);
      final DrumLedRenderer.LedFrame frame = DrumLedRenderer.render(state);
      assertEquals(62, frame.topColors()[1]); // yellow for selected
   }

   @Test
   void muteModeUsesGreen ()
   {
      final DrumLedRenderer.PadState [] pads = new DrumLedRenderer.PadState[8];
      pads[0] = new DrumLedRenderer.PadState(true, true, false, false, 0);
      final DrumUiState state = new DrumUiState(pads, -1, false, true);
      final DrumLedRenderer.LedFrame frame = DrumLedRenderer.render(state);
      assertEquals(59, frame.bottomColors()[0]); // green low for muted
   }

   @Test
   void accentUsesLevelColor ()
   {
      final DrumLedRenderer.PadState [] pads = new DrumLedRenderer.PadState[8];
      pads[0] = new DrumLedRenderer.PadState(true, false, false, true, 127);
      final DrumUiState state = new DrumUiState(pads, -1, false, false);
      final DrumLedRenderer.LedFrame frame = DrumLedRenderer.render(state);
      assertEquals(60, frame.bottomColors()[0]); // green (bright)
   }
}
