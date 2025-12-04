package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArpLayerStateTest {

   @Test
   void tracksActivation() {
      final ArpLayerState state = new ArpLayerState();
      state.setConfiguredSlot(2);
      state.setCurrentSlot(2);
      assertFalse(state.isActive());

      state.setEnabled(true);
      assertTrue(state.isActive());

      state.setCurrentSlot(3);
      assertFalse(state.isActive());
   }

   @Test
   void notifiesListeners() {
      final ArpLayerState state = new ArpLayerState();
      final List<Boolean> updates = new ArrayList<>();
      state.addListener(updates::add);

      state.setEnabled(true);
      state.setCurrentSlot(1);
      assertTrue(updates.contains(Boolean.TRUE));

      state.clearCurrentSlot();
      assertTrue(updates.get(updates.size() - 1) == Boolean.FALSE);
  }
}
