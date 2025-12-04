package com.bitwig.extensions.controllers.novation.launch_control_xl.drum;

import com.bitwig.extensions.controllers.novation.launch_control_xl.support.LedUtil;

/**
 * Pure LED rendering logic for the drum layer.
 */
public final class DrumLedRenderer
{
   private DrumLedRenderer ()
   {
      // Utility
   }

   public record PadState(boolean exists, boolean mute, boolean solo, boolean accentExists, double accentValue) {}

   public record LedFrame(int[] topColors, int[] bottomColors) {}

   /**
    * Compute LED colors for the drum layer.
    *
    * @param state The current UI state snapshot
    * @return A frame with top and bottom colors
    */
   public static LedFrame render (final DrumUiState state)
   {
      final int[] top = new int[8];
      final int[] bottom = new int[8];

      for (int i = 0; i < 8; i++)
      {
         final PadState pad = state.pads()[i];
         if (pad != null && pad.exists)
         {
            top[i] = i == state.selectedPad () ? SimpleColors.YELLOW : SimpleColors.AMBER_LOW;

            if (state.soloMode ())
               bottom[i] = pad.solo ? SimpleColors.YELLOW : SimpleColors.YELLOW_LOW;
            else if (state.muteMode ())
               bottom[i] = pad.mute ? SimpleColors.GREEN_LOW : SimpleColors.GREEN;
            else if (pad.accentExists)
               bottom[i] = LedUtil.levelColor (pad.accentValue, SimpleColors.OFF, SimpleColors.GREEN_LOW, SimpleColors.GREEN);
            else
               bottom[i] = SimpleColors.OFF;
         }
         else
         {
            top[i] = SimpleColors.OFF;
            bottom[i] = SimpleColors.OFF;
         }
      }

      return new LedFrame (top, bottom);
   }

   private static final class SimpleColors
   {
      private static final int OFF = 0;
      private static final int AMBER_LOW = 29; // matches SimpleLedColor.AmberLow
      private static final int YELLOW = 62;    // matches SimpleLedColor.Yellow
      private static final int YELLOW_LOW = 61;
      private static final int GREEN = 60;
      private static final int GREEN_LOW = 59;
   }
}
