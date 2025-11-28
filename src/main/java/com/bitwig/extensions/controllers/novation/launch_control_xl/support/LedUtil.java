package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

/**
 * Utility helpers for translating parameter values into LED colours.
 */
public final class LedUtil
{
   private LedUtil()
   {
   }

   /**
    * Map a normalized value (0..1) to off/dim/bright LED colours with simple thresholds.
    */
   public static int levelColor(final double value, final int offColor, final int dimColor, final int brightColor)
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
}
