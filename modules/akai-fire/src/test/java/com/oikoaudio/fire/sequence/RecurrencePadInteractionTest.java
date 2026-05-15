package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.lights.RgbLigthState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurrencePadInteractionTest {
    @Test
    void togglesPressedPadsWhenSpanAnchorIsDisabled() {
        final RecurrencePadInteraction interaction = new RecurrencePadInteraction(false, 0L);
        final List<Integer> toggles = new ArrayList<>();

        assertTrue(interaction.handlePadPress(3, true, true, RecurrencePattern.of(8, 0b11111111),
                () -> { }, toggles::add, ignored -> { }));

        assertEquals(List.of(3), toggles);
    }

    @Test
    void ignoresTogglePadsOutsideEffectiveSpan() {
        final RecurrencePadInteraction interaction = new RecurrencePadInteraction(false, 0L);
        final List<Integer> toggles = new ArrayList<>();

        assertTrue(interaction.handlePadPress(3, true, true, RecurrencePattern.of(2, 0b01),
                () -> { }, toggles::add, ignored -> { }));

        assertEquals(List.of(), toggles);
    }

    @Test
    void tapOnPadZeroTogglesWhenSpanAnchorIsEnabled() {
        final RecurrencePadInteraction interaction = new RecurrencePadInteraction(true, 0L);
        final List<Integer> toggles = new ArrayList<>();
        final List<Integer> spans = new ArrayList<>();

        interaction.handlePadPress(0, true, true, RecurrencePattern.of(8, 0b11111111),
                () -> { }, toggles::add, spans::add);
        interaction.handlePadPress(0, false, true, RecurrencePattern.of(8, 0b11111111),
                () -> { }, toggles::add, spans::add);

        assertEquals(List.of(0), toggles);
        assertEquals(List.of(), spans);
    }

    @Test
    void holdingPadZeroAndPressingAnotherPadAppliesSpan() {
        final RecurrencePadInteraction interaction = new RecurrencePadInteraction(true, 0L);
        final List<Integer> toggles = new ArrayList<>();
        final List<Integer> spans = new ArrayList<>();

        interaction.handlePadPress(0, true, true, RecurrencePattern.of(8, 0b11111111),
                () -> { }, toggles::add, spans::add);
        interaction.handlePadPress(3, true, true, RecurrencePattern.of(8, 0b11111111),
                () -> { }, toggles::add, spans::add);
        interaction.handlePadPress(0, false, true, RecurrencePattern.of(8, 0b11111111),
                () -> { }, toggles::add, spans::add);

        assertEquals(List.of(), toggles);
        assertEquals(List.of(4), spans);
    }

    @Test
    void delayedRowDisplayUsesHoldThreshold() {
        final RecurrencePadInteraction interaction = new RecurrencePadInteraction(false, 10_000L);

        interaction.beginHoldIfNeeded(false);

        assertFalse(interaction.shouldShowRow(true));
        assertFalse(interaction.shouldShowRow(false));
    }

    @Test
    void padLightUsesSharedRecurrenceColors() {
        final RecurrencePadInteraction interaction = new RecurrencePadInteraction(false, 0L);
        final RgbLigthState base = RgbLigthState.PURPLE;

        assertSame(base.getBrightend(), interaction.padLight(0, RecurrencePattern.of(4, 0b0001), base));
        assertSame(base.getDimmed(), interaction.padLight(1, RecurrencePattern.of(4, 0b0001), base));
        assertSame(RgbLigthState.OFF, interaction.padLight(4, RecurrencePattern.of(4, 0b0001), base));
    }
}
