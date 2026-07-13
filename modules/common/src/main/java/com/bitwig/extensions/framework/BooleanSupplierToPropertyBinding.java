package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.BooleanHardwareProperty;
import java.util.function.BooleanSupplier;

class BooleanSupplierToPropertyBinding extends Binding<BooleanSupplier, BooleanHardwareProperty> {
    public BooleanSupplierToPropertyBinding(
            final BooleanSupplier source, final BooleanHardwareProperty target) {
        super(target, source, target);
    }

    @Override
    protected void deactivate() {
        getTarget().setValueSupplier(null);
    }

    @Override
    protected void activate() {
        getTarget().setValueSupplier(getSource());
    }
}
