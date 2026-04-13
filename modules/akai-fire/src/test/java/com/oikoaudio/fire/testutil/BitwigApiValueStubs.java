package com.oikoaudio.fire.testutil;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.callback.ColorValueChangedCallback;
import com.bitwig.extension.callback.DoubleValueChangedCallback;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.SettableColorValue;

import java.lang.reflect.Proxy;

public final class BitwigApiValueStubs {
    private BitwigApiValueStubs() {
    }

    public static final class BooleanValueStub {
        private final boolean value;
        private int markInterestedCalls;
        private int addObserverCalls;

        public BooleanValueStub(final boolean value) {
            this.value = value;
        }

        public BooleanValue value() {
            return (BooleanValue) Proxy.newProxyInstance(
                    BooleanValue.class.getClassLoader(),
                    new Class[]{BooleanValue.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "get", "getAsBoolean" -> value;
                        case "markInterested" -> {
                            markInterestedCalls++;
                            yield null;
                        }
                        case "addValueObserver" -> {
                            addObserverCalls++;
                            yield null;
                        }
                        case "toString" -> "BooleanValueStub[" + value + "]";
                        default -> defaultValue(method.getReturnType());
                    });
        }

        public int markInterestedCalls() {
            return markInterestedCalls;
        }

        public int addObserverCalls() {
            return addObserverCalls;
        }
    }

    public static final class ColorValueStub {
        private final Color color;
        private int markInterestedCalls;
        private int addObserverCalls;

        public ColorValueStub(final int red255, final int green255, final int blue255) {
            this.color = Color.fromRGB255(red255, green255, blue255);
        }

        public SettableColorValue value() {
            return (SettableColorValue) Proxy.newProxyInstance(
                    SettableColorValue.class.getClassLoader(),
                    new Class[]{SettableColorValue.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "get" -> color;
                        case "markInterested" -> {
                            markInterestedCalls++;
                            yield null;
                        }
                        case "addValueObserver" -> {
                            addObserverCalls++;
                            yield null;
                        }
                        case "toString" -> "ColorValueStub";
                        default -> defaultValue(method.getReturnType());
                    });
        }

        public Color color() {
            return color;
        }

        public int markInterestedCalls() {
            return markInterestedCalls;
        }

        public int addObserverCalls() {
            return addObserverCalls;
        }
    }

    public static final class BeatTimeValueStub {
        private DoubleValueChangedCallback observer;

        public SettableBeatTimeValue value() {
            return (SettableBeatTimeValue) Proxy.newProxyInstance(
                    SettableBeatTimeValue.class.getClassLoader(),
                    new Class[]{SettableBeatTimeValue.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "addValueObserver" -> {
                            observer = (DoubleValueChangedCallback) args[0];
                            yield null;
                        }
                        case "toString" -> "BeatTimeValueStub";
                        default -> defaultValue(method.getReturnType());
                    });
        }

        public void emit(final double value) {
            if (observer != null) {
                observer.valueChanged(value);
            }
        }
    }

    private static Object defaultValue(final Class<?> returnType) {
        if (returnType == void.class) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == double.class) {
            return 0.0;
        }
        if (returnType == float.class) {
            return 0f;
        }
        return null;
    }
}
