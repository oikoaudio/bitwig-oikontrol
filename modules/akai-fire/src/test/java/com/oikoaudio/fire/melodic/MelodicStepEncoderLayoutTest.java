package com.oikoaudio.fire.melodic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.oikoaudio.fire.display.EncoderFooterLegend;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.sequence.EncoderSlotBinding;
import org.junit.jupiter.api.Test;

class MelodicStepEncoderLayoutTest {
    @Test
    void preservesEveryBankSlotAndFooterAssignment() {
        final EncoderSlotBinding[] channel = slots();
        final EncoderSlotBinding[] mixer = slots();
        final EncoderSlotBinding[] user1 = slots();
        final EncoderSlotBinding[] user2 = slots();
        final MelodicStepEncoderLayout layout =
                new MelodicStepEncoderLayout(channel, mixer, user1, user2);

        assertSame(channel[0], layout.layout().bank(EncoderMode.CHANNEL).slots()[0]);
        assertSame(mixer[3], layout.layout().bank(EncoderMode.MIXER).slots()[3]);
        assertSame(user1[2], layout.layout().bank(EncoderMode.USER_1).slots()[2]);
        assertSame(user2[1], layout.layout().bank(EncoderMode.USER_2).slots()[1]);
        assertEquals(
                EncoderFooterLegend.of("Engn", "Dens", "Pool", "MutT"),
                layout.layout().bank(EncoderMode.CHANNEL).footerLegend());
        assertEquals(
                EncoderFooterLegend.of("Velo", "Pres", "Timb", "Ptch"),
                layout.layout().bank(EncoderMode.USER_1).footerLegend());
        assertEquals(
                EncoderFooterLegend.of("GLen", "Chnc", "VSpr", "Rpt"),
                layout.layout().bank(EncoderMode.USER_2).footerLegend());
    }

    @Test
    void poolContextEncoderUsesTheSharedPitchModifierGrammar() {
        assertEquals(
                MelodicStepEncoderLayout.PoolContextTarget.POOL_OCTAVE,
                MelodicStepEncoderLayout.poolContextTarget(false, false));
        assertEquals(
                MelodicStepEncoderLayout.PoolContextTarget.SHARED_ROOT,
                MelodicStepEncoderLayout.poolContextTarget(false, true));
        assertEquals(
                MelodicStepEncoderLayout.PoolContextTarget.SHARED_SCALE,
                MelodicStepEncoderLayout.poolContextTarget(true, false));
        assertEquals(
                MelodicStepEncoderLayout.PoolContextTarget.SHARED_SCALE,
                MelodicStepEncoderLayout.poolContextTarget(true, true));
    }

    private static EncoderSlotBinding[] slots() {
        return new EncoderSlotBinding[] {slot(), slot(), slot(), slot()};
    }

    private static EncoderSlotBinding slot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return 1.0;
            }

            @Override
            public void bind(
                    final com.oikoaudio.fire.sequence.StepSequencerEncoderLayer handler,
                    final com.bitwig.extensions.framework.Layer layer,
                    final com.oikoaudio.fire.control.TouchEncoder encoder,
                    final int slotIndex) {}
        };
    }
}
