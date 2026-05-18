package com.oikoaudio.fire.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UndoRedoBankButtonHandlerTest {
    @Test
    void altPressOnLeftBankRequestsUndo() {
        assertEquals(UndoRedoBankButtonHandler.Action.UNDO,
                UndoRedoBankButtonHandler.actionFor(true, -1, true));
    }

    @Test
    void altPressOnRightBankRequestsRedo() {
        assertEquals(UndoRedoBankButtonHandler.Action.REDO,
                UndoRedoBankButtonHandler.actionFor(true, 1, true));
    }

    @Test
    void releaseOrUnmodifiedPressDoesNotConsumeBankButton() {
        assertEquals(UndoRedoBankButtonHandler.Action.NONE,
                UndoRedoBankButtonHandler.actionFor(false, -1, true));
        assertEquals(UndoRedoBankButtonHandler.Action.NONE,
                UndoRedoBankButtonHandler.actionFor(true, -1, false));
    }
}
