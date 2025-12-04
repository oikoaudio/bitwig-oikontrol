package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

import com.bitwig.extension.controller.api.NoteInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NoteInputConfiguratorTest {

   @Test
   void configuresNonConsumingNoteInput() {
      final NoteInput noteInput = mock(NoteInput.class);

      final NoteInput result = NoteInputConfigurator.asNonConsuming(noteInput);

      verify(noteInput).setShouldConsumeEvents(false);
      assertSame(noteInput, result);
   }

   @Test
   void nullInputThrows() {
      assertThrows(NullPointerException.class, () -> NoteInputConfigurator.asNonConsuming(null));
   }
}
