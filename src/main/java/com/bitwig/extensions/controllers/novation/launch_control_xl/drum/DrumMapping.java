package com.bitwig.extensions.controllers.novation.launch_control_xl.drum;

/**
 * Shared mapping definitions for the drum layer (User Template 7).
 */
public final class DrumMapping
{
   private DrumMapping()
   {
   }

   public static final int PAD_NOTE_OFFSET = 35; // C1 minus one to align with pad index

   public static final int[] SLIDER_CCS = {77, 78, 79, 80, 81, 82, 83, 84};
   public static final int[] KNOB_ROW1_CCS = {13, 14, 15, 16, 17, 18, 19, 20};
   public static final int[] KNOB_ROW2_CCS = {29, 30, 31, 32, 33, 34, 35, 36};
   public static final int[] KNOB_ROW3_CCS = {49, 50, 51, 52, 53, 54, 55, 56};

   // User template 7 (drum): top row = track focus (C1–G1), bottom row matches arp track-control row
   public static final int[] TOP_NOTES = {36, 37, 38, 39, 40, 41, 42, 43};
   public static final int[] BOTTOM_NOTES = {73, 74, 75, 76, 89, 90, 91, 92};

   public static final int SEND_UP_CC = 104;
   public static final int SEND_DOWN_CC = 105;
   public static final int TRACK_LEFT_CC = 106;
   public static final int TRACK_RIGHT_CC = 107;
}
