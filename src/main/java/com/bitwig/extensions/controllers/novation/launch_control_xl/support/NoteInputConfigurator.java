package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

import com.bitwig.extension.controller.api.NoteInput;

import java.util.Objects;

/**
 * Centralises NoteInput configuration so we always disable event consumption.
 */
public final class NoteInputConfigurator {

   private NoteInputConfigurator() {
   }

   public static NoteInput asNonConsuming(final NoteInput noteInput) {
      final NoteInput safeInput = Objects.requireNonNull(noteInput, "noteInput");
      safeInput.setShouldConsumeEvents(false);
      return safeInput;
   }
}
