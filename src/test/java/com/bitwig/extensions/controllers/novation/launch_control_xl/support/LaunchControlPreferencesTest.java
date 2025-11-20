package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LaunchControlPreferencesTest {

   private final Preferences preferences = mock(Preferences.class);
   private final SettableBooleanValue preserveSetting = mock(SettableBooleanValue.class);
   private final SettableBooleanValue enableArpSetting = mock(SettableBooleanValue.class);
   private final SettableRangedValue arpSlotSetting = mock(SettableRangedValue.class);

   @BeforeEach
   void setUp() {
      when(preferences.getBooleanSetting(Mockito.eq("Preserve User Mappings"), Mockito.anyString(), Mockito.anyBoolean()))
         .thenReturn(preserveSetting);
      when(preferences.getBooleanSetting(Mockito.eq("Enable Arp Layer"), Mockito.anyString(), Mockito.anyBoolean()))
         .thenReturn(enableArpSetting);
      when(preferences.getNumberSetting(Mockito.eq("Arp User Mode Slot"), Mockito.anyString(), Mockito.anyDouble(),
         Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyString(), Mockito.anyDouble()))
         .thenReturn(arpSlotSetting);
   }

   @Test
   void defaultsAreReported() {
      when(preserveSetting.get()).thenReturn(false);
      when(enableArpSetting.get()).thenReturn(false);
      when(arpSlotSetting.get()).thenReturn(1.0);

      final LaunchControlPreferences prefs = new LaunchControlPreferences(preferences);

      assertFalse(prefs.shouldPreserveUserMappings());
      assertFalse(prefs.isArpLayerEnabled());
      assertEquals(1, prefs.getArpUserModeSlot());
      verify(preferences).getBooleanSetting("Preserve User Mappings", "Launch Control XL Oikontrol", false);
      verify(preferences).getBooleanSetting("Enable Arp Layer", "Launch Control XL Oikontrol", false);
      verify(preferences).getNumberSetting("Arp User Mode Slot", "Launch Control XL Oikontrol",
         1, 8, 1, "Slot", 1);
   }

   @Test
   void reflectsPreferenceChanges() {
      when(preserveSetting.get()).thenReturn(true);
      when(enableArpSetting.get()).thenReturn(true);
      when(arpSlotSetting.get()).thenReturn(4.0);

      final LaunchControlPreferences prefs = new LaunchControlPreferences(preferences);

      assertTrue(prefs.shouldPreserveUserMappings());
      assertTrue(prefs.isArpLayerEnabled());
      assertEquals(4, prefs.getArpUserModeSlot());
   }

   @Test
   void clampsSlotRange() {
      when(arpSlotSetting.get()).thenReturn(10.0);
      final LaunchControlPreferences prefsHigh = new LaunchControlPreferences(preferences);
      assertEquals(8, prefsHigh.getArpUserModeSlot());

      when(arpSlotSetting.get()).thenReturn(0.0);
      final LaunchControlPreferences prefsLow = new LaunchControlPreferences(preferences);
      assertEquals(1, prefsLow.getArpUserModeSlot());
   }
}
