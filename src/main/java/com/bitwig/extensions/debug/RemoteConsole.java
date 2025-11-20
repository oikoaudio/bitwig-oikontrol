package com.bitwig.extensions.debug;

import com.bitwig.extension.controller.api.ControllerHost;

/**
 * Minimal stub to keep rhbitwig logging calls compiling.
 */
public final class RemoteConsole {
    public static final RemoteConsole out = new RemoteConsole();

    private ControllerHost host;

    private RemoteConsole() {
    }

    public void registerHost(final ControllerHost host) {
        this.host = host;
    }

    public void printSysEx(final String prefix, final byte[] data) {
        if (host != null) {
            host.println(prefix);
        }
    }

    public void println(final String format, final Object... params) {
        if (host != null) {
            final String message = format.replace("{}", "%s");
            host.println(String.format(message, params));
        }
    }

    public String getStackTrace(final int max) {
        return "";
    }
}
