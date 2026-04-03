package com.bitwig.extensions.controllers.novation.launch_control_xl.drum;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PadNoteMapperTest
{
   @Test
   void firstPageStartsAtC1 ()
   {
      assertEquals (36, PadNoteMapper.computeNote (36, 0));
      assertEquals (43, PadNoteMapper.computeNote (36, 7));
   }

   @Test
   void secondPageAddsEight ()
   {
      assertEquals (44, PadNoteMapper.computeNote (44, 0));
      assertEquals (51, PadNoteMapper.computeNote (44, 7));
   }

   @Test
   void thirdPageAddsSixteen ()
   {
      assertEquals (52, PadNoteMapper.computeNote (52, 0));
      assertEquals (59, PadNoteMapper.computeNote (52, 7));
   }
}
