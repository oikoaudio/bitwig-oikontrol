package com.oikoaudio.fire.chordstep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.sequence.NoteStepAccess;
import org.junit.jupiter.api.Test;

class ChordStepInsertionDefaultsTest {
    @Test
    void adjustsAndResetsEachPolyStepInsertionDefault() {
        final ChordStepInsertionDefaults defaults = new ChordStepInsertionDefaults(100, 0.25);

        assertTrue(defaults.adjust(NoteStepAccess.VELOCITY, 3));
        assertTrue(defaults.adjust(NoteStepAccess.PRESSURE, 2));
        assertTrue(defaults.adjust(NoteStepAccess.TIMBRE, -3));
        assertTrue(defaults.adjust(NoteStepAccess.PITCH, 2));
        assertTrue(defaults.adjust(NoteStepAccess.PAN, -5));
        assertTrue(defaults.adjust(NoteStepAccess.GAIN, 5));
        assertTrue(defaults.adjust(NoteStepAccess.DURATION, 10));
        assertTrue(defaults.adjust(NoteStepAccess.CHANCE, -2));
        assertTrue(defaults.adjust(NoteStepAccess.VELOCITY_SPREAD, 4));
        assertTrue(defaults.adjust(NoteStepAccess.REPEATS, 2));

        assertEquals(103, defaults.velocity());
        assertEquals(0.02, defaults.pressure(), 0.000001);
        assertEquals(-0.03, defaults.timbre(), 0.000001);
        assertEquals(2.0, defaults.pitch(), 0.000001);
        assertEquals(-0.05, defaults.pan(), 0.000001);
        assertEquals(0.55, defaults.gain(), 0.000001);
        assertEquals(0.275, defaults.duration(), 0.000001);
        assertEquals(0.9, defaults.chance(), 0.000001);
        assertEquals(0.04, defaults.velocitySpread(), 0.000001);
        assertEquals(2, defaults.repeats());

        for (final NoteStepAccess access : ChordStepInsertionDefaults.SUPPORTED_ACCESSORS) {
            defaults.reset(access);
        }

        assertEquals(100, defaults.velocity());
        assertEquals(0.0, defaults.pressure());
        assertEquals(0.0, defaults.timbre());
        assertEquals(0.0, defaults.pitch());
        assertEquals(0.0, defaults.pan());
        assertEquals(0.5, defaults.gain());
        assertEquals(0.25, defaults.duration());
        assertEquals(1.0, defaults.chance());
        assertEquals(0.0, defaults.velocitySpread());
        assertEquals(0, defaults.repeats());
        assertFalse(defaults.adjust(NoteStepAccess.OCCURENCE, 1));
    }

    @Test
    void snapshotAppliesDefaultsRequiredByTheNextInsertedNote() {
        final ChordStepInsertionDefaults defaults = new ChordStepInsertionDefaults(100, 0.25);
        defaults.adjust(NoteStepAccess.PRESSURE, 20);
        defaults.adjust(NoteStepAccess.TIMBRE, -10);
        defaults.adjust(NoteStepAccess.PITCH, 3);
        defaults.adjust(NoteStepAccess.PAN, -20);
        defaults.adjust(NoteStepAccess.GAIN, 10);
        defaults.adjust(NoteStepAccess.CHANCE, -4);
        defaults.adjust(NoteStepAccess.VELOCITY_SPREAD, 5);
        defaults.adjust(NoteStepAccess.REPEATS, 2);
        final NoteStep note = mock(NoteStep.class);

        defaults.snapshot().applyTo(note);

        verify(note).setPressure(0.2);
        verify(note).setTimbre(-0.1);
        verify(note).setTranspose(3.0);
        verify(note).setPan(-0.2);
        verify(note).setGain(0.6);
        verify(note).setChance(0.8);
        verify(note).setIsChanceEnabled(true);
        verify(note).setVelocitySpread(0.05);
        verify(note).setRepeatCount(2);
        verify(note).setIsRepeatEnabled(true);
    }
}
