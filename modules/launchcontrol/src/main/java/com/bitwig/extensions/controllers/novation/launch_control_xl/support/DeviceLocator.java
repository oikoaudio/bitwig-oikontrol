package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps track of devices on tracks and provides cached lookup/focus results.
 */
public final class DeviceLocator
{
   public enum Role
   {
      DRUM,
      ARP
   }

   public static final class FocusResult
   {
      private final int trackIndex;
      private final Track track;
      private final Device device;

      FocusResult(final int trackIndex, final Track track, final Device device)
      {
         this.trackIndex = trackIndex;
         this.track = track;
         this.device = device;
      }

      public int trackIndex()
      {
         return this.trackIndex;
      }

      public Track track()
      {
         return this.track;
      }

      public Device device()
      {
         return this.device;
      }
   }

   private static final class RoleState
   {
      private final String nameMatch;
      private final Device[] devices;
      private int cachedIndex = -1;

      RoleState(final String nameMatch, final Device[] devices)
      {
         this.nameMatch = nameMatch;
         this.devices = devices;
      }

      void clearCache()
      {
         this.cachedIndex = -1;
      }
   }

   private final TrackBank trackBank;
   private final Map<Role, RoleState> states = new EnumMap<> (Role.class);

   public DeviceLocator (final ControllerHost host, final int width)
   {
     this.trackBank = host.createMainTrackBank (width, 0, 0);
     for (int i = 0; i < width; i++)
        this.trackBank.getItemAt (i).exists ().markInterested ();

     this.states.put (Role.DRUM, this.createRoleState ("drum machine", width));
     this.states.put (Role.ARP, this.createRoleState ("arpeggiator", width));
   }

   private RoleState createRoleState (final String match, final int width)
   {
      final Device[] devices = new Device[width];
      for (int i = 0; i < width; i++)
      {
         final Track track = this.trackBank.getItemAt (i);
         final Device device = track.createDeviceBank (1).getItemAt (0);
         device.exists ().markInterested ();
         device.name ().markInterested ();
         devices[i] = device;
      }
      return new RoleState (match == null ? null : match.toLowerCase (), devices);
   }

   /**
    * Try to focus the cached device index for the role (if one exists).
    *
    * @return Empty if no cache exists or focus failed.
    */
   public Optional<FocusResult> focusCached (final Role role)
   {
      final RoleState state = this.states.get (role);
      if (state == null || state.cachedIndex < 0)
         return Optional.empty ();

      final Optional<FocusResult> result = this.focus (role, state.cachedIndex);
      if (result.isEmpty ())
         state.clearCache ();
      return result;
   }

   /**
    * Scan all tracks for the first matching device of the given role and cache the index.
    */
   public Optional<FocusResult> focusFirst (final Role role)
   {
      final RoleState state = this.states.get (role);
      if (state == null)
         return Optional.empty ();

      final int index = this.findFirstMatchingIndex (state);
      if (index < 0)
      {
         state.clearCache ();
         return Optional.empty ();
      }

      final Optional<FocusResult> result = this.focus (role, index);
      if (result.isPresent ())
         state.cachedIndex = index;
      else
         state.clearCache ();
      return result;
   }

   /** Clear the cached index for the given role. */
   public void clearCache (final Role role)
   {
      final RoleState state = this.states.get (role);
      if (state != null)
         state.clearCache ();
   }

   /**
    * Validate the device at the given index and return a {@link FocusResult} if it exists and matches
    * the role.
    */
   private Optional<FocusResult> focus (final Role role, final int index)
   {
      final RoleState state = this.states.get (role);
      if (state == null || index < 0 || index >= state.devices.length)
         return Optional.empty ();

      final Track track = this.trackBank.getItemAt (index);
      if (track == null || !track.exists ().get ())
         return Optional.empty ();

      final Device device = state.devices[index];
      if (device == null || !device.exists ().get ())
         return Optional.empty ();

      final String name = device.name ().get ();
      if (state.nameMatch != null && (name == null || !name.toLowerCase ().contains (state.nameMatch)))
         return Optional.empty ();

      return Optional.of (new FocusResult (index, track, device));
   }

   private int findFirstMatchingIndex (final RoleState state)
   {
      for (int i = 0; i < state.devices.length; i++)
      {
         final Device device = state.devices[i];
         if (device == null || !device.exists ().get ())
            continue;

         final String name = device.name ().get ();
         if (state.nameMatch != null && (name == null || !name.toLowerCase ().contains (state.nameMatch)))
            continue;
         return i;
      }
      return -1;
   }
}
