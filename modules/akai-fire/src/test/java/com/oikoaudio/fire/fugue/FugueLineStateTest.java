package com.oikoaudio.fire.fugue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FugueLineStateTest {
    @Test
    void sourceOnlyClipStartsWithDerivedLinesOff() {
        final FugueLineState state = new FugueLineState();

        state.enterClip(false);

        assertTrue(state.isEnabled(FugueClipAdapter.SOURCE_CHANNEL));
        assertFalse(state.isEnabled(1));
        assertFalse(state.isEnabled(2));
        assertFalse(state.isEnabled(3));
        assertFalse(state.isProtected());
    }

    @Test
    void existingDerivedMaterialIsProtectedUntilExplicitlyClaimed() {
        final FugueLineState state = new FugueLineState();

        state.enterClip(true);

        assertTrue(state.isProtected());
        assertFalse(state.toggle(1));
        assertFalse(state.isEnabled(1));

        state.claimAllDerivedLines();

        assertFalse(state.isProtected());
        assertTrue(state.isEnabled(1));
        assertTrue(state.isEnabled(2));
        assertTrue(state.isEnabled(3));
    }

    @Test
    void unprotectedDerivedLineCanBeEnabledAndDisabled() {
        final FugueLineState state = new FugueLineState();
        state.enterClip(false);

        assertTrue(state.toggle(2));
        assertTrue(state.isEnabled(2));
        assertTrue(state.toggle(2));
        assertFalse(state.isEnabled(2));
    }
}
