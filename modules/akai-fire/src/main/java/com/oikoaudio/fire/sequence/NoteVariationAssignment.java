package com.oikoaudio.fire.sequence;

/** One validated per-note value ready for a mode adapter to write. */
public record NoteVariationAssignment(NoteVariationEvent.Id eventId, double value) {}
