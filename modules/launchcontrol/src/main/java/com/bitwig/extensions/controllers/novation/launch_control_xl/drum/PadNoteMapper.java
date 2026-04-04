package com.bitwig.extensions.controllers.novation.launch_control_xl.drum;

/**
 * Pure utility to calculate outgoing MIDI note numbers for drum pad audition.
 */
public final class PadNoteMapper
{
   private PadNoteMapper ()
   {
      // Utility class
   }

   /**
    * Calculate the MIDI note for a pad given the current scroll position.
    *
    * Bitwig's drum pad bank scroll position returns the MIDI note number of the first visible pad.
    *
    * @param scrollPosition MIDI note of the first pad in view
    * @param padIndex Index of the pad inside the current bank page (0-based)
    * @return MIDI note number to send
    */
   public static int computeNote (final int scrollPosition, final int padIndex)
   {
      return scrollPosition + padIndex;
   }
}
