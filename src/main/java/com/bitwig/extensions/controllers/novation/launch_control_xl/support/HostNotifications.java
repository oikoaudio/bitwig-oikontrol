package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

import com.bitwig.extension.controller.api.ControllerHost;

/**
 * Thin wrapper around Bitwig's {@link ControllerHost} to centralize popups and debug logging.
 */
public final class HostNotifications
{
   private final ControllerHost host;
   private final boolean debugEnabled;

   public HostNotifications(final ControllerHost host, final boolean debugEnabled)
   {
      this.host = host;
      this.debugEnabled = debugEnabled;
   }

   /** Display a popup notification in Bitwig. */
   public void showPopup(final String message)
   {
      this.host.showPopupNotification(message);
   }

   /** Write a debug line to the Bitwig controller console (when enabled). */
   public void debug(final String message)
   {
      if (this.debugEnabled)
         this.host.println(message);
   }
}
