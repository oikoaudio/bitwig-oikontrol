package com.oikoaudio.fire;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class AkaiFireOikontrolDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("c1f8d20a-3da4-4d2c-8ce1-8b2aa6a8e5b6");

    public AkaiFireOikontrolDefinition() {
    }

    @Override
    public String getName() {
        return "Oikontrol Fire";
    }

    @Override
    public String getAuthor() {
        return "";
    }

    @Override
    public String getVersion() {
        return "2.7.0";
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public String getHardwareVendor() {
        return "Akai";
    }

    @Override
    public String getHardwareModel() {
        return "Akai Fire";
    }

    @Override
    public String getHelpFilePath() {
        return "index.html";
    }

    @Override
    public String getSupportFolderPath() {
        return "";
    }

    @Override
    public int getRequiredAPIVersion() {
        return 24;
    }

    @Override
    public int getNumMidiInPorts() {
        return 1;
    }

    @Override
    public int getNumMidiOutPorts() {
        return 1;
    }

    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list,
                                               final PlatformType platformType) {
        if (platformType == PlatformType.WINDOWS) {
            list.add(new String[]{"FL STUDIO FIRE"}, new String[]{"FL STUDIO FIRE"});
        } else if (platformType == PlatformType.MAC) {
            list.add(new String[]{"FL STUDIO FIRE"}, new String[]{"FL STUDIO FIRE"});
        } else if (platformType == PlatformType.LINUX) {
            list.add(new String[]{"FL STUDIO FIRE"}, new String[]{"FL STUDIO FIRE"});
        }
    }

    @Override
    public AkaiFireOikontrolExtension createInstance(final ControllerHost host) {
        return new AkaiFireOikontrolExtension(this, host);
    }
}
