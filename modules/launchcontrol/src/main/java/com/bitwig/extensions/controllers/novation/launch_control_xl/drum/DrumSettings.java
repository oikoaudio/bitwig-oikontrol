package com.bitwig.extensions.controllers.novation.launch_control_xl.drum;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.SettableBooleanValue;

/**
 * Holds configurable drum-layer settings sourced from Bitwig preferences.
 */
public final class DrumSettings
{
   private final SettableBooleanValue auditionOnSelect;
   private final SettableBooleanValue accentMomentary;

   private DrumSettings(final SettableBooleanValue auditionOnSelect,
                        final SettableBooleanValue accentMomentary)
   {
      this.auditionOnSelect = auditionOnSelect;
      this.accentMomentary = accentMomentary;
   }

   public static DrumSettings from(final ControllerHost host)
   {
      final String category = "LaunchControl XL";
      final SettableBooleanValue auditionOnSelect = host.getPreferences().getBooleanSetting(
         "Audition on drum pad select",
         category,
         true);
      final SettableBooleanValue accentMomentary = host.getPreferences().getBooleanSetting(
         "Drum accent buttons momentary",
         category,
         true);
      return new DrumSettings(auditionOnSelect, accentMomentary);
   }

   public SettableBooleanValue auditionOnSelect()
   {
      return auditionOnSelect;
   }

   public SettableBooleanValue accentMomentary()
   {
      return accentMomentary;
   }

   public boolean auditionOnSelectEnabled()
   {
      return auditionOnSelect.get();
   }

   public boolean accentMomentaryEnabled()
   {
      return accentMomentary.get();
   }
}
