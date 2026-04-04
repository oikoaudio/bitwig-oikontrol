package com.bitwig.extensions.controllers.novation.launch_control_xl.drum;

/**
 * Pure logic for computing the next accent value based on button interaction and configured mode.
 */
public final class AccentModeLogic
{
   private AccentModeLogic ()
   {
      // Utility class
   }

   /**
    * Compute the next accent value for the remote parameter.
    *
    * @param isMomentary True if accent buttons should behave momentarily
    * @param isPress True if this is a button press, false for release
    * @param currentValue The current parameter value (0-127)
    * @return The value to set (0-127). For toggle mode on release the current value is returned.
    */
   public static int nextValue (final boolean isMomentary, final boolean isPress, final int currentValue)
   {
      if (isMomentary)
         return isPress ? 127 : 0;

      if (!isPress)
         return currentValue;

      return currentValue <= 0 ? 127 : 0;
   }
}
