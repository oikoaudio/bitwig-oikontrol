package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MulticlipPageStateTest {
    @Test
    void lanePagingPreservesTheActiveRowAndClampsPartialPages() {
        MulticlipPageState state = MulticlipPageState.initial(10).withActiveChildPosition(1);
        assertEquals(1, state.activeRow());

        state = state.pageLanes(1);
        assertEquals(5, state.activeChildPosition());
        assertEquals(1, state.activeRow());

        state = state.pageLanes(1);
        assertEquals(9, state.activeChildPosition());
        assertEquals(1, state.activeRow());
    }

    @Test
    void externalTrackSelectionRevealsItsLanePage() {
        final MulticlipPageState state = MulticlipPageState.initial(16).withActiveChildPosition(13);
        assertEquals(3, state.lanePage());
        assertEquals(1, state.activeRow());
    }

    @Test
    void timePagingMovesInSixteenStepWindows() {
        MulticlipPageState state = MulticlipPageState.initial(4);
        state = state.pageTime(1).pageTime(1).pageTime(-1);
        assertEquals(16, state.firstVisibleStep());
    }
}
