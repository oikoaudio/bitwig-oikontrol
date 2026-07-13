package com.oikoaudio.fire.sequence;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class StepSequencerEncoderLayerTest {

    @Test
    void requiresCompletedLayoutAsAnExplicitConstructorDependency() {
        assertEquals(1, StepSequencerEncoderLayer.class.getConstructors().length);
        assertEquals(3, StepSequencerEncoderLayer.class.getConstructors()[0].getParameterCount());
    }

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

    @Test
    void bindsAllFourSlotsFromEveryExplicitBank() {
        final StepSequencerHost host = mock(StepSequencerHost.class);
        final AkaiFireOikontrolExtension driver = mock(AkaiFireOikontrolExtension.class);
        final Layers layers = mock(Layers.class);
        final ControllerExtension extension = mock(ControllerExtension.class);
        final AtomicInteger bindingCount = new AtomicInteger();

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

        new StepSequencerEncoderLayer(host, driver, layout(bindingCount));

        assertEquals(16, bindingCount.get());
    }

    private static EncoderBankLayout layout() {
        return layout(new AtomicInteger());
    }

    private static EncoderBankLayout layout(final AtomicInteger bindingCount) {
        final Map<EncoderMode, EncoderBank> banks = new EnumMap<>(EncoderMode.class);
        for (final EncoderMode mode : EncoderMode.values()) {
            banks.put(mode, new EncoderBank(mode.name(), slots(bindingCount)));
        }
        return new EncoderBankLayout(banks);
    }

    private static EncoderSlotBinding[] slots(final AtomicInteger bindingCount) {
        return new EncoderSlotBinding[] {
            slot(bindingCount), slot(bindingCount), slot(bindingCount), slot(bindingCount)
        };
    }

    private static EncoderSlotBinding slot(final AtomicInteger bindingCount) {
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
                    final int slotIndex) {
                bindingCount.incrementAndGet();
            }
        };
    }
}
