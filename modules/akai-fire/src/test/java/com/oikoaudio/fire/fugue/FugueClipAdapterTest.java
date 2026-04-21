package com.oikoaudio.fire.fugue;

import com.bitwig.extension.controller.api.NoteStep;
import com.oikoaudio.fire.melodic.MelodicPattern;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FugueClipAdapterTest {

    @Test
    void sourceOnlyUsesChannelOne() {
        final Map<Integer, Map<Integer, Map<Integer, NoteStep>>> steps = new HashMap<>();
        put(steps, note(0, 0, 60));
        put(steps, note(1, 0, 72));

        final FuguePattern source = FugueClipAdapter.sourceFromChannelOne(steps, 16, 0.25);

        assertTrue(source.step(0).active());
        assertEquals(60, source.step(0).pitch());
        assertFalse(source.step(1).active());
    }

    @Test
    void explicitChannelReadIgnoresOtherChannels() {
        final Map<Integer, Map<Integer, Map<Integer, NoteStep>>> steps = new HashMap<>();
        put(steps, note(0, 0, 60));
        put(steps, note(2, 0, 79));

        final FuguePattern source = FugueClipAdapter.fromChannel(steps, 2, 16, 0.25);

        assertTrue(source.step(0).active());
        assertEquals(79, source.step(0).pitch());
    }

    @Test
    void sourceKeepsMultipleNotesOnSameStep() {
        final Map<Integer, Map<Integer, Map<Integer, NoteStep>>> steps = new HashMap<>();
        put(steps, note(0, 0, 67));
        put(steps, note(0, 0, 60));

        final FuguePattern source = FugueClipAdapter.sourceFromChannelOne(steps, 16, 0.125);

        assertEquals(2, source.notesAt(0).size());
        assertEquals(60, source.notesAt(0).get(0).pitch());
        assertEquals(67, source.notesAt(0).get(1).pitch());
    }

    @Test
    void sourceUsesStepLengthForGateSoFineGridCanRepresentSixteenthNotes() {
        final Map<Integer, Map<Integer, Map<Integer, NoteStep>>> steps = new HashMap<>();
        put(steps, note(0, 0, 60, 0.25));

        final FuguePattern source = FugueClipAdapter.sourceFromChannelOne(steps, 16, 0.125);

        assertEquals(2.0, source.step(0).gate());
    }

    private static void put(final Map<Integer, Map<Integer, Map<Integer, NoteStep>>> steps, final NoteStep note) {
        steps.computeIfAbsent(note.channel(), ignored -> new HashMap<>())
                .computeIfAbsent(note.x(), ignored -> new HashMap<>())
                .put(note.y(), note);
    }

    private static NoteStep note(final int channel, final int x, final int pitch) {
        return note(channel, x, pitch, 0.25);
    }

    private static NoteStep note(final int channel, final int x, final int pitch, final double duration) {
        return (NoteStep) Proxy.newProxyInstance(
                NoteStep.class.getClassLoader(),
                new Class[]{NoteStep.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "channel" -> channel;
                    case "x" -> x;
                    case "y" -> pitch;
                    case "state" -> NoteStep.State.NoteOn;
                    case "velocity", "chance" -> 0.75;
                    case "duration" -> duration;
                    case "recurrenceLength", "recurrenceMask" -> 0;
                    case "toString" -> "NoteStepStub";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(final Class<?> returnType) {
        if (returnType == void.class) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == double.class) {
            return 0.0;
        }
        return null;
    }
}
