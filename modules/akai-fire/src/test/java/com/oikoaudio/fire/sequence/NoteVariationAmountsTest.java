package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NoteVariationAmountsTest {

    @Test
    void amountsAreIndependentAndClamped() {
        final NoteVariationAmounts amounts = new NoteVariationAmounts();

        amounts.adjust(NoteVariationParameter.TIMBRE, 0.4);
        amounts.adjust(NoteVariationParameter.PRESSURE, 1.5);
        amounts.adjust(NoteVariationParameter.TIMBRE, -0.1);

        assertEquals(0.3, amounts.amount(NoteVariationParameter.TIMBRE), 0.000001);
        assertEquals(1.0, amounts.amount(NoteVariationParameter.PRESSURE));
        assertEquals(0.0, amounts.amount(NoteVariationParameter.PAN));
    }
}
