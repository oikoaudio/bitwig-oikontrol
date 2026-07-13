package com.oikoaudio.fire.melodic;

import com.oikoaudio.fire.display.EncoderFooterLegend;
import com.oikoaudio.fire.sequence.EncoderBank;
import com.oikoaudio.fire.sequence.EncoderBankLayout;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.sequence.EncoderSlotBinding;
import java.util.EnumMap;
import java.util.Map;

/** Defines the immutable Melodic Step encoder-bank mapping. */
public final class MelodicStepEncoderLayout {
    private final EncoderBankLayout layout;

    public MelodicStepEncoderLayout(
            final EncoderSlotBinding[] channel,
            final EncoderSlotBinding[] mixer,
            final EncoderSlotBinding[] user1,
            final EncoderSlotBinding[] user2) {
        final Map<EncoderMode, EncoderBank> banks = new EnumMap<>(EncoderMode.class);
        banks.put(
                EncoderMode.CHANNEL,
                new EncoderBank(
                        "1: Engine\n2: Density\n3: Pool Oct\n4: Mut Type",
                        EncoderFooterLegend.of("Engn", "Dens", "Pool", "MutT"),
                        channel));
        banks.put(
                EncoderMode.MIXER,
                new EncoderBank("1: Length\n2: Swivel / Mirror x2\n3: Reverse\n4: Invert", mixer));
        banks.put(
                EncoderMode.USER_1,
                new EncoderBank(
                        "1: Velocity\n2: Pressure\n3: Timbre\n4: Pitch",
                        EncoderFooterLegend.of("Velo", "Pres", "Timb", "Ptch"),
                        user1));
        banks.put(
                EncoderMode.USER_2,
                new EncoderBank(
                        "1: Gate Len\n2: Chance\n3: Vel Spread\n4: Repeat",
                        EncoderFooterLegend.of("GLen", "Chnc", "VSpr", "Rpt"),
                        user2));
        layout = new EncoderBankLayout(banks);
    }

    public EncoderBankLayout layout() {
        return layout;
    }
}
