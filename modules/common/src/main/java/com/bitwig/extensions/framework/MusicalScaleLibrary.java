package com.bitwig.extensions.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public final class MusicalScaleLibrary
{
   private MusicalScaleLibrary()
   {
      /* Bitwig-aligned core scales */
      addScale(new MusicalScale("Chromatic", new int[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 }));
      addScale(new MusicalScale("Major", new int[]{ 0, 2, 4, 5, 7, 9, 11 }));
      addScale(new MusicalScale("Minor", new int[]{ 0, 2, 3, 5, 7, 8, 10 }));
      addScale(new MusicalScale("Dorian", new int[]{ 0, 2, 3, 5, 7, 9, 10 }));
      addScale(new MusicalScale("Phrygian", new int[]{ 0, 1, 3, 5, 7, 8, 10 }));
      addScale(new MusicalScale("Lydian", new int[]{ 0, 2, 4, 6, 7, 9, 11 }));
      addScale(new MusicalScale("Mixolydian", new int[]{ 0, 2, 4, 5, 7, 9, 10 }));
      addScale(new MusicalScale("Locrian", new int[]{ 0, 1, 3, 5, 6, 8, 10 }));
      addScale(new MusicalScale("Harmonic Major", new int[]{ 0, 2, 4, 5, 7, 8, 11 }));
      addScale(new MusicalScale("Harmonic Minor", new int[]{ 0, 2, 3, 5, 7, 8, 11 }));
      addScale(new MusicalScale("Overtone Scale", new int[]{ 0, 2, 4, 6, 7, 9, 10 }));
      addScale(new MusicalScale("Jazz Minor", new int[]{ 0, 2, 3, 5, 7, 9, 11 }));
      addScale(new MusicalScale("Blues Major", new int[]{ 0, 2, 3, 4, 7, 9 }));
      addScale(new MusicalScale("Blues Minor", new int[]{ 0, 3, 5, 6, 7, 10 }));
      addScale(new MusicalScale("Double Harmonic Major", new int[]{ 0, 1, 4, 5, 7, 8, 11 }));
      addScale(new MusicalScale("Double Harmonic Minor", new int[]{ 0, 2, 3, 6, 7, 8, 11 }));
      addScale(new MusicalScale("Whole Tone", new int[]{ 0, 2, 4, 6, 8, 10 }));
      addScale(new MusicalScale("Half-diminished", new int[]{ 0, 2, 3, 5, 6, 8, 10 }));
      addScale(new MusicalScale("Diminished WH", new int[]{ 0, 2, 3, 5, 6, 8, 9, 11 }));
      addScale(new MusicalScale("Diminished HW", new int[]{ 0, 1, 3, 4, 6, 7, 9, 10 }));
      addScale(new MusicalScale("Major Pentatonic", new int[]{ 0, 2, 4, 7, 9 }));
      addScale(new MusicalScale("Minor Pentatonic", new int[]{ 0, 3, 5, 7, 10 }));
      addScale(new MusicalScale("Major Triad", new int[]{ 0, 4, 7 }));
      addScale(new MusicalScale("Minor Triad", new int[]{ 0, 3, 7 }));

      /* Extras beyond the Bitwig-like core list */
      addScale(new MusicalScale("Hirajoshi", new int[]{ 0, 2, 3, 7, 8 }));
      addScale(new MusicalScale("Iwato", new int[]{ 0, 1, 5, 6, 10 }));
      addScale(new MusicalScale("Kumoi", new int[]{ 0, 2, 3, 7, 9 }));
      addScale(new MusicalScale("In Sen", new int[]{ 0, 1, 5, 7, 10 }));
      addScale(new MusicalScale("Yo Scale", new int[]{ 0, 2, 5, 7, 9 }));
      addScale(new MusicalScale("Todi", new int[]{ 0, 1, 3, 5, 6, 7, 11 }));
      addScale(new MusicalScale("Phrygian Dominant", new int[]{ 0, 1, 4, 5, 7, 8, 10 }));
      addScale(new MusicalScale("Marva", new int[]{ 0, 1, 4, 6, 7, 9, 11 }));
      addScale(new MusicalScale("Ukranian Dorian", new int[]{ 0, 2, 3, 6, 7, 9, 10 }));
      addScale(new MusicalScale("Super Locrian", new int[]{ 0, 1, 3, 4, 6, 8, 10 }));
      addScale(new MusicalScale("Bebop Major", new int[]{ 0, 2, 4, 5, 7, 8, 9, 11 }));
      addScale(new MusicalScale("Bebop Dorian", new int[]{ 0, 2, 3, 4, 5, 7, 9, 10 }));
      addScale(new MusicalScale("Bebop Mixolydian", new int[]{ 0, 2, 4, 5, 7, 9, 10, 11 }));
      addScale(new MusicalScale("Bebop Minor", new int[]{ 0, 2, 3, 5, 7, 8, 10, 11 }));

      /* Legacy aliases kept for compatibility with older code/tests */
      addAlias("Ionan (Major)", "Major");
      addAlias("Aeolian (Minor)", "Minor");
      addAlias("Melodic Minor (ascending)", "Jazz Minor");
      addAlias("Double Harmonic", "Double Harmonic Major");
      addAlias("Blues", "Blues Minor");
      addAlias("Major Blues", "Blues Major");
      addAlias("Whole Half", "Diminished WH");
      addAlias("Half-Whole Diminished", "Diminished HW");
      addAlias("Yo scale", "Yo Scale");
      addAlias("BeBop Major", "Bebop Major");
      addAlias("BeBop Dorian", "Bebop Dorian");
      addAlias("BeBop Mixolydian", "Bebop Mixolydian");
      addAlias("BeBop Minor", "Bebop Minor");
      addAlias("Hungarian Minor", "Double Harmonic Minor");

      final int numScales = mMusicalScales.size();
      mScalesName = new String[numScales];
      for (int i = 0; i < numScales; ++i)
         mScalesName[i] = mMusicalScales.get(i).getName();
   }

   private void addScale(final MusicalScale musicalScale)
   {
      assert !mMusicalScaleHashMap.containsKey(musicalScale.getName());
      assert !mMusicalScales.contains(musicalScale);

      /* Check that no other scale has the same notes */
      for (final MusicalScale scale : mMusicalScales)
         assert !Arrays.equals(scale.getNotes(), musicalScale.getNotes());

      musicalScale.setIndexInLibrary(mMusicalScales.size());
      mMusicalScales.add(musicalScale);
      mMusicalScaleHashMap.put(musicalScale.getName(), musicalScale);
   }

   private void addAlias(final String alias, final String canonicalName)
   {
      assert !mMusicalScaleHashMap.containsKey(alias);
      final MusicalScale canonical = mMusicalScaleHashMap.get(canonicalName);
      assert canonical != null;
      mMusicalScaleHashMap.put(alias, canonical);
   }

   static public MusicalScaleLibrary getInstance()
   {
      return mInstance;
   }

   public final int getMusicalScalesCount()
   {
      return mMusicalScales.size();
   }

   public final MusicalScale getMusicalScale(final int index)
   {
      if (index < 0 || index >= mMusicalScales.size())
         return null;
      return mMusicalScales.get(index);
   }

   public MusicalScale getMusicalScale(final String scaleName)
   {
      return mMusicalScaleHashMap.get(scaleName);
   }

   public String[] getScalesName()
   {
      return mScalesName;
   }

   private static final MusicalScaleLibrary mInstance = new MusicalScaleLibrary();
   private final List<MusicalScale> mMusicalScales = new ArrayList<>();
   private final HashMap<String, MusicalScale> mMusicalScaleHashMap = new HashMap<>();
   private final String[] mScalesName;
}
