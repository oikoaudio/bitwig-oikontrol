package com.oikoaudio.fire.sequence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncoderBankTest {
    @Test
    void derivesFooterLegendFromModeInfoByDefault() {
        final EncoderBank bank = new EncoderBank(
                "1: Engine\n2: Density\n3: Pool Oct\n4: Mut Type",
                new EncoderSlotBinding[]{
                        slot(), slot(), slot(), slot()
                });

        assertEquals("Engn Dens Pool MutT", bank.footerLegend());
    }

    @Test
    void canUseExplicitFooterLegend() {
        final EncoderBank bank = new EncoderBank(
                "1: Volume\n2: Pan\n3: Send 1\n4: Send 2",
                "Vol  Pan  S1  S2",
                new EncoderSlotBinding[]{
                        slot(), slot(), slot(), slot()
                });

        assertEquals("Vol  Pan  S1  S2", bank.footerLegend());
    }

    private static EncoderSlotBinding slot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return 1.0;
            }

            @Override
            public void bind(final StepSequencerEncoderLayer handler,
                             final com.bitwig.extensions.framework.Layer layer,
                             final com.oikoaudio.fire.control.TouchEncoder encoder,
                             final int slotIndex) {
            }
        };
    }
}
