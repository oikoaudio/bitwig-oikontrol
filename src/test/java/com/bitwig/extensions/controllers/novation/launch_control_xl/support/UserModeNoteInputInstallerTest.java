package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserModeNoteInputInstallerTest {

   private final MidiIn midiIn = mock(MidiIn.class);
   private final NoteInput noteInput = mock(NoteInput.class);

   @Test
   void createsNamedNoteInput() {
      when(midiIn.createNoteInput(Mockito.anyString(), Mockito.any(String[].class)))
         .thenReturn(noteInput);

      final NoteInput result = UserModeNoteInputInstaller.ensureUserModeInput(midiIn);

      final ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
      verify(midiIn).createNoteInput(Mockito.eq("Launch Control XL Oikontrol"), captor.capture());
      final String[] masks = captor.getValue();
      assertEquals(56, masks.length);
      verify(noteInput).setShouldConsumeEvents(false);
      assertSame(noteInput, result);
   }

   @Test
   void excludesReservedChannelFromMask() {
      when(midiIn.createNoteInput(Mockito.anyString(), Mockito.any(String[].class)))
         .thenReturn(noteInput);

      UserModeNoteInputInstaller.ensureUserModeInput(midiIn, 7);

      final ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
      verify(midiIn).createNoteInput(Mockito.eq("Launch Control XL Oikontrol"), captor.capture());
      final String[] masks = captor.getValue();
      assertEquals(49, masks.length);
      assertTrue(Arrays.stream(masks).noneMatch(mask -> mask.startsWith("87")
         || mask.startsWith("97")
         || mask.startsWith("A7")
         || mask.startsWith("B7")
         || mask.startsWith("C7")
         || mask.startsWith("D7")
         || mask.startsWith("E7")));
      verify(noteInput).setShouldConsumeEvents(false);
   }

   @Test
   void nullMidiInThrows() {
      assertThrows(NullPointerException.class, () -> UserModeNoteInputInstaller.ensureUserModeInput(null));
   }
}
