package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;

import java.util.Objects;

/**
 * Creates the user-mode NoteInput that leaves MIDI events available for Bitwig mappings.
 */
public final class UserModeNoteInputInstaller {

   private static final String NOTE_INPUT_NAME = "Launch Control XL Oikontrol";
   private static final int[] STATUS_BASES = {0x80, 0x90, 0xA0, 0xB0, 0xC0, 0xD0, 0xE0};
   private static final int USER_TEMPLATE_CHANNELS = 8;
   private static final int NO_RESERVED_CHANNEL = -1;

   private UserModeNoteInputInstaller() {
   }

   public static NoteInput ensureUserModeInput(final MidiIn midiIn) {
      return ensureUserModeInput(midiIn, NO_RESERVED_CHANNEL);
   }

   public static NoteInput ensureUserModeInput(final MidiIn midiIn, final int reservedChannel) {
      final MidiIn safeMidiIn = Objects.requireNonNull(midiIn, "midiIn");
      final String[] masks = buildChannelMasks(reservedChannel);
      final NoteInput input = safeMidiIn.createNoteInput(NOTE_INPUT_NAME, masks);
      return NoteInputConfigurator.asNonConsuming(input);
   }

   private static String[] buildChannelMasks(final int reservedChannel) {
      final boolean hasReserved = reservedChannel >= 0 && reservedChannel < USER_TEMPLATE_CHANNELS;
      final int activeChannels = hasReserved ? USER_TEMPLATE_CHANNELS - 1 : USER_TEMPLATE_CHANNELS;
      final String[] masks = new String[STATUS_BASES.length * activeChannels];
      int index = 0;
      for (int channel = 0; channel < USER_TEMPLATE_CHANNELS; channel++) {
         if (hasReserved && channel == reservedChannel) {
            continue;
         }
         for (final int statusBase : STATUS_BASES) {
            final int status = statusBase + channel;
            masks[index++] = String.format("%02X????", status);
         }
      }
      return masks;
   }
}
