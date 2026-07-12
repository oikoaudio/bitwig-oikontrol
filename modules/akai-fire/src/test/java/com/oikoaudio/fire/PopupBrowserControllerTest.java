package com.oikoaudio.fire;

import com.bitwig.extension.controller.api.BrowserResultsItem;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.StringValue;
import com.oikoaudio.fire.display.OledDisplay;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PopupBrowserControllerTest {
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

        final PopupBrowserController controller = new PopupBrowserController(
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

        final PopupBrowserController controller = new PopupBrowserController(
                scheduler, browser, results, mock(ViewCursorControl.class), mock(OledDisplay.class), host);

        controller.adjustSelection(2);
        controller.handleMainEncoderPress(true);

        verify(browser, times(2)).selectNextFile();
        verify(browser).commit();
        verify(host).notifyAction("Browser", "Commit");
    }

    @Test
    void cancelClosesOnlyAnActiveBrowser() {
        final PopupBrowser browser = mock(PopupBrowser.class);
        final BooleanValue browserExists = mock(BooleanValue.class);
        when(browserExists.get()).thenReturn(true);
        when(browser.exists()).thenReturn(browserExists);
        final PopupBrowserController.Host host = mock(PopupBrowserController.Host.class);
        final PopupBrowserController controller = new PopupBrowserController(
                mock(ControllerHost.class), browser, mock(BrowserResultsItem.class),
                mock(ViewCursorControl.class), mock(OledDisplay.class), host);

        controller.cancel();

        verify(browser).cancel();
        verify(host).notifyAction("Browser", "Closed");
    }
}
