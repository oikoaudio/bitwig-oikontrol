package com.oikoaudio.fire;

import com.bitwig.extension.controller.api.BrowserResultsItem;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.oikoaudio.fire.display.OledDisplay;

/** Owns Akai Popup Browser gesture policy, delayed opening, result priming, and encoder navigation. */
final class PopupBrowserController {
    private static final int[] RESULTS_PRIME_DELAYS_MS = {0, 1, 10, 30};
    private static final int OPEN_DEFER_MS = 40;

    private final ControllerHost scheduler;
    private final PopupBrowser browser;
    private final BrowserResultsItem resultsCursor;
    private final ViewCursorControl viewControl;
    private final OledDisplay oled;
    private final Host host;
    private int pressToken;

    static PopupBrowserController create(final ControllerHost controllerHost,
                                         final ViewCursorControl viewControl,
                                         final OledDisplay oled,
                                         final Host host) {
        final PopupBrowser browser = controllerHost.createPopupBrowser();
        browser.exists().markInterested();
        final BrowserResultsItem resultsCursor = browser.resultsColumn().createCursorItem();
        resultsCursor.exists().markInterested();
        resultsCursor.isSelected().markInterested();
        resultsCursor.name().markInterested();
        return new PopupBrowserController(controllerHost, browser, resultsCursor, viewControl, oled, host);
    }

    PopupBrowserController(final ControllerHost scheduler,
                           final PopupBrowser browser,
                           final BrowserResultsItem resultsCursor,
                           final ViewCursorControl viewControl,
                           final OledDisplay oled,
                           final Host host) {
        this.scheduler = scheduler;
        this.browser = browser;
        this.resultsCursor = resultsCursor;
        this.viewControl = viewControl;
        this.oled = oled;
        this.host = host;
    }

    boolean isActive() {
        return browser.exists().get();
    }

    void handleBrowserPressed(final boolean pressed) {
        if (!pressed) {
            pressToken++;
            host.refreshOverlayState();
            if (!host.overlayActive()) {
                oled.clearScreenDelayed();
            }
            return;
        }
        if (host.overlayActive() && !host.shiftHeld()) {
            host.closeOverlayLatch();
            host.refreshOverlayState();
            host.notifyAction("Settings", "Closed");
            return;
        }
        if (isActive()) {
            cancel();
            return;
        }
        if (host.shiftHeld() && !host.altHeld()) {
            final boolean wasLatched = host.overlayLatched();
            host.toggleOverlayLatch();
            if (wasLatched) {
                host.deactivateOverlay();
                host.notifyAction("Settings", "Closed");
                return;
            }
            host.refreshOverlayState();
            host.notifyAction("Settings", "Latched");
            return;
        }
        host.refreshOverlayState();
        final int scheduledToken = ++pressToken;
        scheduler.scheduleTask(() -> maybeOpen(scheduledToken), OPEN_DEFER_MS);
    }

    private void maybeOpen(final int scheduledToken) {
        if (scheduledToken != pressToken || !host.browserButtonPressed()) {
            return;
        }
        if (host.overlayActive() || isActive()) {
            return;
        }
        openAtSelectedInsertionPoint();
    }

    private void openAtSelectedInsertionPoint() {
        final PinnableCursorDevice primaryDevice = viewControl.getPrimaryDevice();
        final boolean shiftHeld = host.shiftHeld();
        final boolean altHeld = host.altHeld();
        if (shiftHeld && altHeld) {
            if (primaryDevice.exists().get()) {
                primaryDevice.beforeDeviceInsertionPoint().browse();
            } else {
                viewControl.getCursorTrack().startOfDeviceChainInsertionPoint().browse();
            }
            scheduleResultsPrime();
            host.notifyAction("Browser", "Before");
            return;
        }
        if (altHeld) {
            if (primaryDevice.exists().get()) {
                primaryDevice.afterDeviceInsertionPoint().browse();
            } else {
                viewControl.getCursorTrack().endOfDeviceChainInsertionPoint().browse();
            }
            scheduleResultsPrime();
            host.notifyAction("Browser", "After");
            return;
        }
        if (primaryDevice.exists().get()) {
            primaryDevice.replaceDeviceInsertionPoint().browse();
            scheduleResultsPrime();
            host.notifyAction("Browser", "Replace");
        } else {
            viewControl.getCursorTrack().endOfDeviceChainInsertionPoint().browse();
            scheduleResultsPrime();
            host.notifyAction("Browser", "Add");
        }
    }

    private void scheduleResultsPrime() {
        for (final int delayMs : RESULTS_PRIME_DELAYS_MS) {
            scheduler.scheduleTask(this::primeResultsSelection, delayMs);
        }
    }

    private void primeResultsSelection() {
        if (!isActive()) {
            return;
        }
        if (resultsCursor.exists().get()) {
            if (!resultsCursor.isSelected().get()) {
                resultsCursor.isSelected().set(true);
            }
            return;
        }
        browser.selectFirstFile();
        if (resultsCursor.exists().get()) {
            resultsCursor.isSelected().set(true);
        }
    }

    void adjustSelection(final int inc) {
        if (!isActive()) {
            return;
        }
        if (inc > 0) {
            for (int index = 0; index < inc; index++) {
                browser.selectNextFile();
            }
        } else if (inc < 0) {
            for (int index = 0; index < -inc; index++) {
                browser.selectPreviousFile();
            }
        }
        oled.valueInfo("Browser", resultsCursor.exists().get() ? resultsCursor.name().get() : "No Results");
    }

    void handleMainEncoderPress(final boolean pressed) {
        if (pressed && isActive()) {
            commit();
        }
    }

    void commit() {
        if (!isActive()) {
            return;
        }
        browser.commit();
        host.notifyAction("Browser", "Commit");
    }

    void cancel() {
        if (!isActive()) {
            return;
        }
        browser.cancel();
        host.notifyAction("Browser", "Closed");
    }

    interface Host {
        boolean browserButtonPressed();

        boolean shiftHeld();

        boolean altHeld();

        boolean overlayActive();

        boolean overlayLatched();

        void toggleOverlayLatch();

        void closeOverlayLatch();

        void deactivateOverlay();

        void refreshOverlayState();

        void notifyAction(String title, String value);
    }
}
