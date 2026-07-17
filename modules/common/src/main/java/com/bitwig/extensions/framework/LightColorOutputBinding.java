package com.bitwig.extensions.framework;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import java.util.function.Supplier;

class LightColorOutputBinding extends Binding<Supplier<Color>, MultiStateHardwareLight> {

    protected LightColorOutputBinding(
            final Supplier<Color> source, final MultiStateHardwareLight target) {
        super(target, source, target);
    }

    @Override
    protected void deactivate() {
        getTarget().setColorSupplier(null);
    }

    @Override
    protected void activate() {
        getTarget().setColorSupplier(getSource());
    }
}
