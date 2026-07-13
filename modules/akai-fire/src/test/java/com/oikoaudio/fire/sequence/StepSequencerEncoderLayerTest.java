package com.oikoaudio.fire.sequence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StepSequencerEncoderLayerTest {

    @Test
    void explicitLayoutAvoidsQueryingPartiallyConstructedHost() {
        final StepSequencerHost host = mock(StepSequencerHost.class);
        final AkaiFireOikontrolExtension driver = mock(AkaiFireOikontrolExtension.class);
        final Layers layers = mock(Layers.class);
        final ControllerExtension extension = mock(ControllerExtension.class);

        when(layers.getControllerExtension()).thenReturn(extension);
        when(extension.getHost()).thenReturn(mock(ControllerHost.class));
        when(driver.getLayers()).thenReturn(layers);
        when(driver.getOled()).thenReturn(mock(OledDisplay.class));
        when(driver.getEncoders())
                .thenReturn(
                        new TouchEncoder[] {
                            mock(TouchEncoder.class),
                            mock(TouchEncoder.class),
                            mock(TouchEncoder.class),
                            mock(TouchEncoder.class)
                        });
        when(driver.getButton(org.mockito.ArgumentMatchers.any()))
                .thenReturn(mock(BiColorButton.class));

        new StepSequencerEncoderLayer(host, driver, layout());

        verify(host, never()).getEncoderBankLayout();
    }

    private static EncoderBankLayout layout() {
        final Map<EncoderMode, EncoderBank> banks = new EnumMap<>(EncoderMode.class);
        for (final EncoderMode mode : EncoderMode.values()) {
            banks.put(mode, new EncoderBank(mode.name(), slots()));
        }
        return new EncoderBankLayout(banks);
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
                    final StepSequencerEncoderLayer encoderLayer,
                    final Layer layer,
                    final TouchEncoder encoder,
                    final int slotIndex) {}
        };
    }
}
