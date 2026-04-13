package com.bitwig.extensions.framework;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class LayerActivationTest {

    @Test
    void laterActiveLayerReplacesConflictingBindingsFromLowerLayer() {
        final Layers layers = new Layers(testControllerExtension());
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
        final Layers layers = new Layers(testControllerExtension());
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

    private static ControllerExtension testControllerExtension() {
        final ControllerHost host = mock(ControllerHost.class);
        doReturn(mock(HardwareActionBindable.class)).when(host).createAction(any(Runnable.class), any());
        return new ControllerExtension(testDefinition(), host) {
            @Override
            public void init() {
            }

            @Override
            public void exit() {
            }

            @Override
            public void flush() {
            }
        };
    }

    private static ControllerExtensionDefinition testDefinition() {
        return new ControllerExtensionDefinition() {
            @Override
            public String getName() {
                return "Test";
            }

            @Override
            public String getAuthor() {
                return "Test";
            }

            @Override
            public String getVersion() {
                return "1";
            }

            @Override
            public UUID getId() {
                return UUID.fromString("00000000-0000-0000-0000-000000000001");
            }

            @Override
            public int getRequiredAPIVersion() {
                return 18;
            }

            @Override
            public String getHardwareVendor() {
                return "Test";
            }

            @Override
            public String getHardwareModel() {
                return "Test";
            }

            @Override
            public int getNumMidiInPorts() {
                return 0;
            }

            @Override
            public int getNumMidiOutPorts() {
                return 0;
            }

            @Override
            public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                                       final PlatformType platformType) {
            }

            @Override
            public ControllerExtension createInstance(final ControllerHost host) {
                throw new UnsupportedOperationException();
            }
        };
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
