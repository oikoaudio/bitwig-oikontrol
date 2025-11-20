package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableRangedValue;

import java.util.Objects;

/**
 * Encapsulates preferences exposed to Bitwig's controller panel.
 */
public final class LaunchControlPreferences {

   private static final String CATEGORY = "Launch Control XL Oikontrol";
   private static final String PRESERVE_MAPPINGS_LABEL = "Preserve User Mappings";
   private static final String ENABLE_ARP_LABEL = "Enable Arp Layer";
   private static final String ARP_SLOT_LABEL = "Arp User Mode Slot";
   private static final int MIN_USER_SLOT = 1;
   private static final int MAX_USER_SLOT = 8;

   private final SettableBooleanValue preserveUserMappings;
   private final SettableBooleanValue enableArpLayer;
   private final SettableRangedValue arpUserModeSlot;

   public LaunchControlPreferences(final Preferences preferences) {
      final Preferences safePreferences = Objects.requireNonNull(preferences, "preferences");
      preserveUserMappings =
         safePreferences.getBooleanSetting(PRESERVE_MAPPINGS_LABEL, CATEGORY, false);
      preserveUserMappings.markInterested();
      enableArpLayer =
         safePreferences.getBooleanSetting(ENABLE_ARP_LABEL, CATEGORY, false);
      enableArpLayer.markInterested();
      arpUserModeSlot = safePreferences.getNumberSetting(ARP_SLOT_LABEL, CATEGORY,
         MIN_USER_SLOT, MAX_USER_SLOT, 1, "Slot", 1);
      arpUserModeSlot.markInterested();
   }

   public boolean shouldPreserveUserMappings() {
      return preserveUserMappings.get();
   }

   public SettableBooleanValue preserveUserMappingsSetting() {
      return preserveUserMappings;
   }

   public boolean isArpLayerEnabled() {
      return enableArpLayer.get();
   }

   public SettableBooleanValue enableArpLayerSetting() {
      return enableArpLayer;
   }

   public int getArpUserModeSlot() {
      final double value = arpUserModeSlot.get();
      final double clamped = Math.max(MIN_USER_SLOT, Math.min(MAX_USER_SLOT, value));
      return (int) Math.round(clamped);
   }

   public SettableRangedValue arpUserModeSlotSetting() {
      return arpUserModeSlot;
   }
}
