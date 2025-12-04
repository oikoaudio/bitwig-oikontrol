package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tracks whether the rhbitwig arp layer should be active based on preferences and current template slot.
 */
public final class ArpLayerState {

   private static final int MIN_SLOT = 1;
   private static final int MAX_SLOT = 8;

   private final List<Consumer<Boolean>> listeners = new ArrayList<>();
   private boolean enabled = false;
   private int configuredSlot = MIN_SLOT;
   private int currentSlot = -1;
   private boolean active = false;

   public void addListener(final Consumer<Boolean> listener) {
      listeners.add(listener);
      listener.accept(active);
   }

   public void setEnabled(final boolean enabled) {
      if (this.enabled == enabled) {
         return;
      }
      this.enabled = enabled;
      updateActiveState();
   }

   public void setConfiguredSlot(final int slot) {
      final int clamped = clampSlot(slot);
      if (this.configuredSlot == clamped) {
         return;
      }
      this.configuredSlot = clamped;
      updateActiveState();
   }

   public void setCurrentSlot(final int slot) {
      final int clamped = clampSlot(slot);
      if (this.currentSlot == clamped) {
         return;
      }
      this.currentSlot = clamped;
      updateActiveState();
   }

   public void clearCurrentSlot() {
      if (this.currentSlot == -1) {
         return;
      }
      this.currentSlot = -1;
      updateActiveState();
   }

   public boolean isEnabled() {
      return enabled;
   }

   public int getConfiguredSlot() {
      return configuredSlot;
   }

   public int getCurrentSlot() {
      return currentSlot;
   }

   public boolean isActive() {
      return active;
   }

   private void updateActiveState() {
      final boolean newActive = enabled && currentSlot != -1 && currentSlot == configuredSlot;
      if (newActive == active) {
         return;
      }
      active = newActive;
      listeners.forEach(listener -> listener.accept(active));
   }

   private static int clampSlot(final int slot) {
      return Math.max(MIN_SLOT, Math.min(MAX_SLOT, slot));
   }
}
