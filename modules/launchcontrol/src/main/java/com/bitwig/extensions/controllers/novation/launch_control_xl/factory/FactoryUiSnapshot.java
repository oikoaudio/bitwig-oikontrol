package com.bitwig.extensions.controllers.novation.launch_control_xl.factory;

public record FactoryUiSnapshot(Surface surface,
                                Mode mode,
                                TrackControl trackControl,
                                boolean deviceOn,
                                int selectedRemotePage,
                                int remotePageCount,
                                int selectedTrack,
                                Strip[] strips,
                                Value[] deviceRemotes,
                                Value[] projectRemotes,
                                boolean sendCanScrollBack,
                                boolean sendCanScrollForward,
                                boolean trackCanScrollBack,
                                boolean trackCanScrollForward,
                                boolean deviceHasPrevious,
                                boolean deviceHasNext)
{
   public FactoryUiSnapshot
   {
      strips = strips.clone();
      deviceRemotes = deviceRemotes.clone();
      projectRemotes = projectRemotes.clone();
   }

   @Override public Strip[] strips() { return strips.clone(); }
   @Override public Value[] deviceRemotes() { return deviceRemotes.clone(); }
   @Override public Value[] projectRemotes() { return projectRemotes.clone(); }

   public FactoryUiSnapshot withDeviceOn(final boolean value)
   {
      return new FactoryUiSnapshot(surface, mode, trackControl, value, selectedRemotePage, remotePageCount, selectedTrack,
         strips, deviceRemotes, projectRemotes, sendCanScrollBack, sendCanScrollForward, trackCanScrollBack,
         trackCanScrollForward, deviceHasPrevious, deviceHasNext);
   }

   public FactoryUiSnapshot withNavigation(final boolean sendBack, final boolean sendForward,
                                           final boolean trackBack, final boolean trackForward,
                                           final boolean devicePrevious, final boolean deviceNext)
   {
      return new FactoryUiSnapshot(surface, mode, trackControl, deviceOn, selectedRemotePage, remotePageCount, selectedTrack,
         strips, deviceRemotes, projectRemotes, sendBack, sendForward, trackBack, trackForward,
         devicePrevious, deviceNext);
   }

   public enum Surface { FACTORY, DRUM, ARP, DEVICE_PAGES, RAW_USER }

   public enum Mode { SEND_2_FULL_DEVICE, SEND_2_DEVICE_1, SEND_2_PROJECT, SEND_3, SEND_1_DEVICE_2,
      DEVICE_3, TRACK_3, NONE, SEND_2_PAN_1 }

   public enum TrackControl { NONE, MUTE, SOLO, RECORD_ARM }

   public record Value(boolean exists, double value)
   {
      public static Value of(final double value) { return new Value(true, value); }
      public static Value missing() { return new Value(false, 0); }
   }

   public record Strip(boolean exists, boolean mute, boolean solo, boolean arm, Value trackControl,
                       Value[] sends, Value[] deviceParameters, Value[] trackParameters)
   {
      public Strip
      {
         sends = sends.clone();
         deviceParameters = deviceParameters.clone();
         trackParameters = trackParameters.clone();
      }

      @Override public Value[] sends() { return sends.clone(); }
      @Override public Value[] deviceParameters() { return deviceParameters.clone(); }
      @Override public Value[] trackParameters() { return trackParameters.clone(); }

      public static Strip existing(final boolean mute, final boolean solo, final boolean arm,
                                   final Value trackControl, final Value[] sends,
                                   final Value[] deviceParameters, final Value[] trackParameters)
      {
         return new Strip(true, mute, solo, arm, trackControl, sends, deviceParameters, trackParameters);
      }

      public static Strip missing()
      {
         final Value[] missing = {Value.missing(), Value.missing(), Value.missing()};
         return new Strip(false, false, false, false, Value.missing(), missing, missing, missing);
      }
   }
}
