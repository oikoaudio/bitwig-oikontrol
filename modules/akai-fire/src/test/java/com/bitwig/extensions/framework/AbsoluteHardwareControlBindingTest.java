package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.AbsoluteHardwarControlBindable;
import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AbsoluteHardwareControlBindingTest {

    @Test
    void activationUsesCurrentRange() {
        final AbsoluteHardwareControl source = mock(AbsoluteHardwareControl.class);
        final AbsoluteHardwarControlBindable target = mock(AbsoluteHardwarControlBindable.class);
        final com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding hardwareBinding =
                mock(com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding.class);
        when(source.addBindingWithRange(target, 0.25, 0.75)).thenReturn(hardwareBinding);

        final AbsoluteHardwareControlBinding binding = new AbsoluteHardwareControlBinding(source, target);
        binding.setRange(0.25, 0.75);

        binding.setIsActive(true);

        verify(source).addBindingWithRange(target, 0.25, 0.75);
        verifyNoInteractions(hardwareBinding);
    }

    @Test
    void activeBindingPropagatesIncrementalRangeChanges() {
        final AbsoluteHardwareControl source = mock(AbsoluteHardwareControl.class);
        final AbsoluteHardwarControlBindable target = mock(AbsoluteHardwarControlBindable.class);
        final com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding hardwareBinding =
                mock(com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding.class);
        when(source.addBindingWithRange(target, 0.0, 1.0)).thenReturn(hardwareBinding);

        final AbsoluteHardwareControlBinding binding = new AbsoluteHardwareControlBinding(source, target);
        binding.setIsActive(true);
        binding.setMin(0.2);
        binding.setMax(0.8);
        binding.setRange(0.3, 0.7);

        verify(hardwareBinding).setMinNormalizedValue(0.2);
        verify(hardwareBinding).setMaxNormalizedValue(0.8);
        verify(hardwareBinding).setNormalizedRange(0.3, 0.7);
    }

    @Test
    void deactivationRemovesUnderlyingHardwareBinding() {
        final AbsoluteHardwareControl source = mock(AbsoluteHardwareControl.class);
        final AbsoluteHardwarControlBindable target = mock(AbsoluteHardwarControlBindable.class);
        final com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding hardwareBinding =
                mock(com.bitwig.extension.controller.api.AbsoluteHardwareControlBinding.class);
        when(source.addBindingWithRange(target, 0.0, 1.0)).thenReturn(hardwareBinding);

        final AbsoluteHardwareControlBinding binding = new AbsoluteHardwareControlBinding(source, target);
        binding.setIsActive(true);
        binding.setIsActive(false);

        verify(hardwareBinding).removeBinding();
    }
}
