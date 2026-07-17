package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.StringHardwareProperty;
import java.util.function.Supplier;

class StringSupplierToPropertyBinding extends Binding<Supplier<String>, StringHardwareProperty> {
    public StringSupplierToPropertyBinding(
            final Supplier<String> source, final StringHardwareProperty target) {
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
