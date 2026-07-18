package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void pagingClampsAtTheFirstLaneAndTimePages() {
        MulticlipPageState state = MulticlipPageState.initial(6);

        state = state.pageLanes(-1).pageTime(-1);

        assertEquals(0, state.lanePage());
        assertEquals(0, state.activeChildPosition());
        assertEquals(0, state.firstVisibleStep());
    }

    @Test
    void reportsLaneAndTimePagingAvailabilityForButtonFeedback() {
        MulticlipPageState state = MulticlipPageState.initial(6);
        assertFalse(state.canPageLanes(-1));
        assertTrue(state.canPageLanes(1));
        assertFalse(state.canPageTime(-1));

        state = state.pageLanes(1).pageTime(1);
        assertTrue(state.canPageLanes(-1));
        assertFalse(state.canPageLanes(1));
        assertTrue(state.canPageTime(-1));
    }
}
