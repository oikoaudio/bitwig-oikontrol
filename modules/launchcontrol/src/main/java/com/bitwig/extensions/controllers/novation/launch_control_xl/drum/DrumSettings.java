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
   private final SettableBooleanValue showDeactivatedTracks;

   private DrumSettings(final SettableBooleanValue auditionOnSelect,
                        final SettableBooleanValue accentMomentary,
                        final SettableBooleanValue showDeactivatedTracks)
   {
      this.auditionOnSelect = auditionOnSelect;
      this.accentMomentary = accentMomentary;
      this.showDeactivatedTracks = showDeactivatedTracks;
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
      final SettableBooleanValue showDeactivatedTracks = host.getPreferences().getBooleanSetting(
         "Show deactivated tracks",
         category,
         false);
      showDeactivatedTracks.markInterested();
      return new DrumSettings(auditionOnSelect, accentMomentary, showDeactivatedTracks);
   }

   public SettableBooleanValue auditionOnSelect()
   {
      return auditionOnSelect;
   }

   public SettableBooleanValue accentMomentary()
   {
      return accentMomentary;
   }

   public SettableBooleanValue showDeactivatedTracks()
   {
      return showDeactivatedTracks;
   }

   public boolean auditionOnSelectEnabled()
   {
      return auditionOnSelect.get();
   }

   public boolean accentMomentaryEnabled()
   {
      return accentMomentary.get();
   }

   public boolean showDeactivatedTracksEnabled()
   {
      return showDeactivatedTracks.get();
   }
}
