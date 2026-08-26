package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.BrowserResultsItem;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.InsertionPoint;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.StringValue;
import com.oikoaudio.fire.display.OledDisplay;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PopupBrowserControllerTest {
    @Test
    void emptyCurrentTrackIgnoresStalePrimaryDeviceWhenBrowsing() {
        final ControllerHost scheduler = mock(ControllerHost.class);
        final PopupBrowser browser = mock(PopupBrowser.class);
        final BooleanValue browserExists = mock(BooleanValue.class);
        final PopupBrowserController.Host host = mock(PopupBrowserController.Host.class);
        final ViewCursorControl viewControl = mock(ViewCursorControl.class);
        final PinnableCursorDevice stalePrimary = mock(PinnableCursorDevice.class);
        final BooleanValue primaryExists = mock(BooleanValue.class);
        final DeviceBank deviceBank = mock(DeviceBank.class);
        final Device firstDevice = mock(Device.class);
        final BooleanValue firstDeviceExists = mock(BooleanValue.class);
        final CursorTrack cursorTrack = mock(CursorTrack.class);
        final InsertionPoint trackEnd = mock(InsertionPoint.class);
        when(browser.exists()).thenReturn(browserExists);
        when(browserExists.get()).thenReturn(false);
        when(host.browserButtonPressed()).thenReturn(true);
        when(viewControl.getPrimaryDevice()).thenReturn(stalePrimary);
        when(stalePrimary.exists()).thenReturn(primaryExists);
        when(primaryExists.get()).thenReturn(true);
        when(viewControl.getDeviceBank()).thenReturn(deviceBank);
        when(deviceBank.getDevice(0)).thenReturn(firstDevice);
        when(firstDevice.exists()).thenReturn(firstDeviceExists);
        when(firstDeviceExists.get()).thenReturn(false);
        when(viewControl.getCursorTrack()).thenReturn(cursorTrack);
        when(cursorTrack.endOfDeviceChainInsertionPoint()).thenReturn(trackEnd);
        final PopupBrowserController controller =
                new PopupBrowserController(
                        scheduler,
                        browser,
                        mock(BrowserResultsItem.class),
                        viewControl,
                        mock(OledDisplay.class),
                        host);

        controller.handleBrowserPressed(true);
        final ArgumentCaptor<Runnable> delayedOpen = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).scheduleTask(delayedOpen.capture(), anyLong());
        delayedOpen.getValue().run();

        verify(trackEnd).browse();
        verify(stalePrimary, never()).replaceDeviceInsertionPoint();
        verify(host).notifyAction("Browser", "Add");
    }

    @Test
    void opensAtHeldDrumPadCapturedWhenBrowserIsPressed() {
        final ControllerHost scheduler = mock(ControllerHost.class);
        final PopupBrowser browser = mock(PopupBrowser.class);
        final BooleanValue browserExists = mock(BooleanValue.class);
        final PopupBrowserController.Host host = mock(PopupBrowserController.Host.class);
        final InsertionPoint padInsertionPoint = mock(InsertionPoint.class);
        when(browser.exists()).thenReturn(browserExists);
        when(browserExists.get()).thenReturn(false);
        when(host.browserButtonPressed()).thenReturn(true);
        when(host.heldDrumPadInsertionPoint()).thenReturn(padInsertionPoint, (InsertionPoint) null);
        final PopupBrowserController controller =
                new PopupBrowserController(
                        scheduler,
                        browser,
                        mock(BrowserResultsItem.class),
                        mock(ViewCursorControl.class),
                        mock(OledDisplay.class),
                        host);

        controller.handleBrowserPressed(true);
        final ArgumentCaptor<Runnable> delayedOpen = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).scheduleTask(delayedOpen.capture(), anyLong());
        delayedOpen.getValue().run();

        verify(padInsertionPoint).browse();
        verify(host).notifyAction("Browser", "Pad");
    }

    @Test
    void releasingButtonInvalidatesDelayedOpen() {
        final ControllerHost scheduler = mock(ControllerHost.class);
        final PopupBrowser browser = mock(PopupBrowser.class);
        final BrowserResultsItem results = mock(BrowserResultsItem.class);
        final PopupBrowserController.Host host = mock(PopupBrowserController.Host.class);
        final ViewCursorControl viewControl = mock(ViewCursorControl.class);
        final BooleanValue browserExists = mock(BooleanValue.class);
        when(browserExists.get()).thenReturn(false);
        when(browser.exists()).thenReturn(browserExists);
        when(host.browserButtonPressed()).thenReturn(true);

        final PopupBrowserController controller =
                new PopupBrowserController(
                        scheduler, browser, results, viewControl, mock(OledDisplay.class), host);
        controller.handleBrowserPressed(true);
        final ArgumentCaptor<Runnable> delayedOpen = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).scheduleTask(delayedOpen.capture(), anyLong());

        controller.handleBrowserPressed(false);
        delayedOpen.getValue().run();

        verifyNoInteractions(viewControl);
        assertFalse(controller.isActive());
    }

    @Test
    void navigatesResultsAndCommitsFromMainEncoder() {
        final ControllerHost scheduler = mock(ControllerHost.class);
        final PopupBrowser browser = mock(PopupBrowser.class);
        final BrowserResultsItem results = mock(BrowserResultsItem.class);
        final PopupBrowserController.Host host = mock(PopupBrowserController.Host.class);
        final BooleanValue browserExists = mock(BooleanValue.class);
        final BooleanValue resultExists = mock(BooleanValue.class);
        final StringValue resultName = mock(StringValue.class);
        when(browserExists.get()).thenReturn(true);
        when(resultExists.get()).thenReturn(true);
        when(resultName.get()).thenReturn("Polysynth");
        when(browser.exists()).thenReturn(browserExists);
        when(results.exists()).thenReturn(resultExists);
        when(results.name()).thenReturn(resultName);

        final PopupBrowserController controller =
                new PopupBrowserController(
                        scheduler,
                        browser,
                        results,
                        mock(ViewCursorControl.class),
                        mock(OledDisplay.class),
                        host);

        controller.adjustSelection(2);
        controller.handleMainEncoderPress(true);

        verify(browser, times(2)).selectNextFile();
        verify(browser).commit();
        verify(host).notifyAction("Browser", "Commit");
    }

    @Test
    void retainsMainEncoderGestureUntilReleaseAfterCommitClosesBrowser() {
        final PopupBrowser browser = mock(PopupBrowser.class);
        final BooleanValue browserExists = mock(BooleanValue.class);
        when(browserExists.get()).thenReturn(true, true, false);
        when(browser.exists()).thenReturn(browserExists);
        final PopupBrowserController controller =
                new PopupBrowserController(
                        mock(ControllerHost.class),
                        browser,
                        mock(BrowserResultsItem.class),
                        mock(ViewCursorControl.class),
                        mock(OledDisplay.class),
                        mock(PopupBrowserController.Host.class));

        controller.handleMainEncoderPress(true);

        assertTrue(controller.isHandlingMainEncoderGesture());

        controller.handleMainEncoderPress(false);

        assertFalse(controller.isHandlingMainEncoderGesture());
    }

    @Test
    void cancelClosesOnlyAnActiveBrowser() {
        final PopupBrowser browser = mock(PopupBrowser.class);
        final BooleanValue browserExists = mock(BooleanValue.class);
        when(browserExists.get()).thenReturn(true);
        when(browser.exists()).thenReturn(browserExists);
        final PopupBrowserController.Host host = mock(PopupBrowserController.Host.class);
        final PopupBrowserController controller =
                new PopupBrowserController(
                        mock(ControllerHost.class),
                        browser,
                        mock(BrowserResultsItem.class),
                        mock(ViewCursorControl.class),
                        mock(OledDisplay.class),
                        host);

        controller.cancel();

        verify(browser).cancel();
        verify(host).notifyAction("Browser", "Closed");
    }
}
