package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class LayerActivationTest {

    @Test
    void laterActiveLayerReplacesConflictingBindingsFromLowerLayer() {
        final Layers layers = new Layers(mockControllerExtension());
        final Layer lower = new Layer(layers, "LOWER");
        final Layer upper = new Layer(layers, "UPPER");
        final Object exclusivity = new Object();

        final TestBinding lowerBinding = new TestBinding(exclusivity);
        final TestBinding upperBinding = new TestBinding(exclusivity);
        lower.addBinding(lowerBinding);
        upper.addBinding(upperBinding);

        lower.activate();
        assertTrue(lowerBinding.isActive());
        assertEquals(1, lowerBinding.activations);

        upper.activate();
        assertFalse(lowerBinding.isActive());
        assertEquals(1, lowerBinding.deactivations);
        assertTrue(upperBinding.isActive());
        assertEquals(1, upperBinding.activations);
    }

    @Test
    void deactivatingLayerDeactivatesItsBindings() {
        final Layers layers = new Layers(mockControllerExtension());
        final Layer layer = new Layer(layers, "LAYER");
        final TestBinding binding = new TestBinding(new Object());
        layer.addBinding(binding);

        layer.activate();
        assertTrue(binding.isActive());

        layer.deactivate();
        assertFalse(binding.isActive());
        assertEquals(1, binding.activations);
        assertEquals(1, binding.deactivations);
    }

    private static ControllerExtension mockControllerExtension() {
        final ControllerExtension controllerExtension = mock(ControllerExtension.class);
        final ControllerHost host = mock(ControllerHost.class);
        doReturn(host).when(controllerExtension).getHost();
        doReturn(mock(HardwareActionBindable.class)).when(host).createAction(any(Runnable.class), any());
        return controllerExtension;
    }

    private static final class TestBinding extends Binding<Object, Object> {
        private int activations = 0;
        private int deactivations = 0;

        private TestBinding(final Object exclusivityObject) {
            super(exclusivityObject, new Object(), new Object());
        }

        @Override
        protected void deactivate() {
            deactivations++;
        }

        @Override
        protected void activate() {
            activations++;
        }
    }
}
