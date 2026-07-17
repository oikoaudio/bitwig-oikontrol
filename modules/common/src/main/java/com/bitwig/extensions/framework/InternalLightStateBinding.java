package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import java.util.function.Supplier;

public class InternalLightStateBinding
        extends Binding<Supplier<InternalHardwareLightState>, MultiStateHardwareLight> {
    protected InternalLightStateBinding(
            final Supplier<InternalHardwareLightState> source,
            final MultiStateHardwareLight target) {
        super(target, source, target);
    }

    @Override
    protected void deactivate() {
        getTarget().state().setValueSupplier(null);
    }

    @Override
    protected void activate() {
        getTarget().state().setValueSupplier(getSource());
    }
}
