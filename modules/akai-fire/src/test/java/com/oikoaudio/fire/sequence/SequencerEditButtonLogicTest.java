package com.oikoaudio.fire.sequence;

import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.oikoaudio.fire.NoteAssign;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SequencerEditButtonLogicTest {

    @Test
    void alternatePressActivatesPrimaryWhenShiftIsNotHeld() {
        final BooleanValueObject mainValue = new BooleanValueObject();
        final BooleanValueObject altValue = new BooleanValueObject();
        final BooleanValueObject alternateFunctionActive = new BooleanValueObject();
        final BooleanValueObject actionTaken = new BooleanValueObject();
        final List<String> events = new ArrayList<>();

        SequencerEditButtonLogic.handleAlternatePressed(
                mainValue,
                altValue,
                alternateFunctionActive,
                actionTaken,
                false,
                FunctionInfo.INFO1.get(NoteAssign.MUTE_1),
                FunctionInfo.INFO2.get(NoteAssign.MUTE_1),
                true,
                new TestFeedback(events));

        assertTrue(mainValue.get());
        assertFalse(altValue.get());
        assertFalse(alternateFunctionActive.get());
        assertTrue(actionTaken.get());
        assertEquals(List.of("activate:Select:Pad select"), events);
    }

    @Test
    void alternatePressWithShiftTogglesAlternateFunction() {
        final BooleanValueObject mainValue = new BooleanValueObject();
        final BooleanValueObject altValue = new BooleanValueObject();
        final BooleanValueObject alternateFunctionActive = new BooleanValueObject();
        final BooleanValueObject actionTaken = new BooleanValueObject();
        final List<String> events = new ArrayList<>();

        SequencerEditButtonLogic.handleAlternatePressed(
                mainValue,
                altValue,
                alternateFunctionActive,
                actionTaken,
                true,
                FunctionInfo.INFO1.get(NoteAssign.MUTE_1),
                FunctionInfo.INFO2.get(NoteAssign.MUTE_1),
                true,
                new TestFeedback(events));

        assertFalse(mainValue.get());
        assertTrue(altValue.get());
        assertTrue(alternateFunctionActive.get());
        assertTrue(actionTaken.get());
        assertEquals(List.of("activate:Mute:Pad mute"), events);
    }

    @Test
    void releaseClearsPrimaryAndActionFlag() {
        final BooleanValueObject mainValue = new BooleanValueObject();
        final BooleanValueObject altValue = new BooleanValueObject();
        final BooleanValueObject alternateFunctionActive = new BooleanValueObject();
        final BooleanValueObject actionTaken = new BooleanValueObject();
        mainValue.set(true);
        actionTaken.set(true);
        final List<String> events = new ArrayList<>();

        SequencerEditButtonLogic.handleAlternatePressed(
                mainValue,
                altValue,
                alternateFunctionActive,
                actionTaken,
                false,
                FunctionInfo.INFO1.get(NoteAssign.MUTE_1),
                FunctionInfo.INFO2.get(NoteAssign.MUTE_1),
                false,
                new TestFeedback(events));

        assertFalse(mainValue.get());
        assertFalse(altValue.get());
        assertFalse(actionTaken.get());
        assertEquals(List.of("deactivate"), events);
    }

    @Test
    void releaseKeepsAlternateValueLatchedWhenAlternateIsActive() {
        final BooleanValueObject mainValue = new BooleanValueObject();
        final BooleanValueObject altValue = new BooleanValueObject();
        final BooleanValueObject alternateFunctionActive = new BooleanValueObject();
        final BooleanValueObject actionTaken = new BooleanValueObject();
        altValue.set(true);
        alternateFunctionActive.set(true);
        actionTaken.set(true);

        SequencerEditButtonLogic.handleAlternatePressed(
                mainValue,
                altValue,
                alternateFunctionActive,
                actionTaken,
                true,
                FunctionInfo.INFO1.get(NoteAssign.MUTE_1),
                FunctionInfo.INFO2.get(NoteAssign.MUTE_1),
                false,
                new TestFeedback(new ArrayList<>()));

        assertFalse(mainValue.get());
        assertTrue(altValue.get());
        assertFalse(actionTaken.get());
    }

    private record TestFeedback(List<String> events) implements SequencerEditButtonBinder.EditFunctionFeedback {
        @Override
        public void activate(final FunctionInfo info, final boolean shiftHeld) {
            events.add("activate:" + info.getName(shiftHeld) + ":" + info.getDetail());
        }

        @Override
        public void deactivate() {
            events.add("deactivate");
        }
    }
}
