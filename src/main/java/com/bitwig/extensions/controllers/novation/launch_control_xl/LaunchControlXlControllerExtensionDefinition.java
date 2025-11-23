package com.bitwig.extensions.controllers.novation.launch_control_xl;

import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public class LaunchControlXlControllerExtensionDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Novation";
   }

   @Override
   public String getHardwareModel()
   {
      return "Launch Control XL";
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 1;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 1;
   }

   @Override
   public void listAutoDetectionMidiPortNames(
      final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      list.add(
         new String[]{"Launch Control XL"},
         new String[]{"Launch Control XL"});

      list.add(
         new String[]{"Launch Control XL MIDI 1"},
         new String[]{"Launch Control XL MIDI 1"});

      // Weird, but it happened on Linux...
      list.add(
         new String[]{"Launch Control XL Launch Contro"},
         new String[]{"Launch Control XL Launch Contro"});
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new LaunchControlXlControllerExtension(this, host);
   }

   @Override
   public String getName()
   {
      // Use a distinct name so it doesn't collide with Bitwig's factory entry.
      return "Launch Control XL by Oiko Audio";
   }

   @Override
   public String getAuthor()
   {
      return "David Fredman / Oiko Audio";
   }

   @Override
   public String getVersion()
   {
      return "1.1";
   }

   @Override
   public UUID getId()
   {
      return EXTENSION_UUID;
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 24;
   }

   @Override
   public String getHelpFilePath()
   {
      // Served from src/main/resources/Documentation/index.html inside the bwextension.
      return "Documentation/index.html";
   }

   public static LaunchControlXlControllerExtensionDefinition getInstance()
   {
      return INSTANCE;
   }

   private static final LaunchControlXlControllerExtensionDefinition INSTANCE = new LaunchControlXlControllerExtensionDefinition();

   // Use a unique UUID to avoid clashing with Bitwig's factory Launch Control XL entry.
   private static final UUID EXTENSION_UUID = UUID.fromString("9f1e9af6-5a1c-4f06-8f4a-b8b686c71111");
}
