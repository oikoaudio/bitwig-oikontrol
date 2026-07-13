package com.bitwig.extensions.controllers.novation.launch_control_xl.factory;

import com.bitwig.extensions.controllers.novation.common.SimpleLedColor;
import com.bitwig.extensions.controllers.novation.launch_control_xl.support.LedUtil;

import java.util.Arrays;

public final class FactoryLedRenderer
{
   private FactoryLedRenderer() {}

   public record LedFrame(int[] topButtons, int[] bottomButtons, int[] knobs, int[] rightButtons)
   {
      public LedFrame
      {
         topButtons = topButtons.clone();
         bottomButtons = bottomButtons.clone();
         knobs = knobs.clone();
         rightButtons = rightButtons.clone();
      }

      @Override public int[] topButtons() { return topButtons.clone(); }
      @Override public int[] bottomButtons() { return bottomButtons.clone(); }
      @Override public int[] knobs() { return knobs.clone(); }
      @Override public int[] rightButtons() { return rightButtons.clone(); }
   }

   public static LedFrame render(final FactoryUiSnapshot state)
   {
      final int[] top = new int[8];
      final int[] bottom = new int[8];
      final int[] knobs = new int[24];
      final FactoryUiSnapshot.Strip[] strips = state.strips();
      final FactoryUiSnapshot.Value[] deviceRemotes = state.deviceRemotes();
      final FactoryUiSnapshot.Value[] projectRemotes = state.projectRemotes();
      Arrays.fill(knobs, color(SimpleLedColor.Off));
      for (int i = 0; i < 8; i++)
      {
         final FactoryUiSnapshot.Strip strip = strips[i];
         top[i] = strip.exists()
            ? (state.selectedTrack() == i ? color(SimpleLedColor.Amber) : color(SimpleLedColor.AmberLow))
            : color(SimpleLedColor.Off);
         bottom[i] = bottomColor(state, strip, i);
         renderKnobs(state, strip, i, knobs, deviceRemotes, projectRemotes);
      }
      return new LedFrame(top, bottom, knobs, rightButtons(state));
   }

   private static int bottomColor(final FactoryUiSnapshot state, final FactoryUiSnapshot.Strip strip,
                                  final int index)
   {
      if (state.deviceOn())
      {
         if (state.selectedRemotePage() == index)
            return color(SimpleLedColor.Amber);
         return index < state.remotePageCount() ? color(SimpleLedColor.AmberLow) : color(SimpleLedColor.Off);
      }
      if (!strip.exists())
         return color(SimpleLedColor.Off);
      return switch (state.trackControl())
      {
         case MUTE -> strip.mute() ? color(SimpleLedColor.GreenLow) : color(SimpleLedColor.Green);
         case SOLO -> strip.solo() ? color(SimpleLedColor.Amber) : color(SimpleLedColor.AmberLow);
         case RECORD_ARM -> strip.arm() ? color(SimpleLedColor.Red) : color(SimpleLedColor.RedLow);
         case NONE -> level(strip.trackControl(), SimpleLedColor.AmberLow, SimpleLedColor.Amber);
      };
   }

   private static void renderKnobs(final FactoryUiSnapshot state, final FactoryUiSnapshot.Strip strip,
                                   final int index, final int[] knobs,
                                   final FactoryUiSnapshot.Value[] deviceRemotes,
                                   final FactoryUiSnapshot.Value[] projectRemotes)
   {
      if (!strip.exists() || state.mode() == FactoryUiSnapshot.Mode.NONE)
         return;
      final FactoryUiSnapshot.Value[] sends = strip.sends();
      final FactoryUiSnapshot.Value[] device = strip.deviceParameters();
      final FactoryUiSnapshot.Value[] track = strip.trackParameters();
      switch (state.mode())
      {
         case SEND_2_DEVICE_1 -> set(knobs, index, send(sends, 0), send(sends, 1), device(device, 0));
         case SEND_2_PAN_1 -> set(knobs, index, send(sends, 0), send(sends, 1), color(SimpleLedColor.Red));
         case SEND_3 -> set(knobs, index, send(sends, 0), send(sends, 1), send(sends, 2));
         case SEND_1_DEVICE_2 -> set(knobs, index, send(sends, 0), device(device, 0), device(device, 1));
         case DEVICE_3 -> set(knobs, index, device(device, 0), device(device, 1), device(device, 2));
         case TRACK_3 -> set(knobs, index, device(track, 0), device(track, 1), device(track, 2));
         case SEND_2_FULL_DEVICE -> set(knobs, index, send(sends, 0), send(sends, 1), device(deviceRemotes, index));
         case SEND_2_PROJECT -> set(knobs, index, send(sends, 0), send(sends, 1), device(projectRemotes, index));
         case NONE -> { }
      }
   }

   private static int[] rightButtons(final FactoryUiSnapshot state)
   {
      final int on = color(SimpleLedColor.Yellow);
      final int off = color(SimpleLedColor.Off);
      return new int[]{state.deviceOn() ? on : off,
         state.trackControl() == FactoryUiSnapshot.TrackControl.MUTE ? on : off,
         state.trackControl() == FactoryUiSnapshot.TrackControl.SOLO ? on : off,
         state.trackControl() == FactoryUiSnapshot.TrackControl.RECORD_ARM ? on : off,
         state.sendCanScrollBack() ? on : off, state.sendCanScrollForward() ? on : off,
         state.deviceOn() ? (state.deviceHasPrevious() ? on : off) : (state.trackCanScrollBack() ? on : off),
         state.deviceOn() ? (state.deviceHasNext() ? on : off) : (state.trackCanScrollForward() ? on : off)};
   }

   private static int send(final FactoryUiSnapshot.Value[] values, final int index)
   {
      return level(value(values, index), SimpleLedColor.GreenLow, SimpleLedColor.Green);
   }

   private static int device(final FactoryUiSnapshot.Value[] values, final int index)
   {
      return level(value(values, index), SimpleLedColor.AmberLow, SimpleLedColor.Amber);
   }

   private static FactoryUiSnapshot.Value value(final FactoryUiSnapshot.Value[] values, final int index)
   {
      return values != null && index >= 0 && index < values.length ? values[index] : FactoryUiSnapshot.Value.missing();
   }

   private static int level(final FactoryUiSnapshot.Value value, final SimpleLedColor dim, final SimpleLedColor bright)
   {
      return value != null && value.exists()
         ? LedUtil.levelColor(value.value(), color(SimpleLedColor.Off), color(dim), color(bright))
         : color(SimpleLedColor.Off);
   }

   private static void set(final int[] knobs, final int index, final int top, final int middle, final int bottom)
   {
      knobs[index] = top;
      knobs[8 + index] = middle;
      knobs[16 + index] = bottom;
   }

   private static int color(final SimpleLedColor color) { return color.value(); }
}
