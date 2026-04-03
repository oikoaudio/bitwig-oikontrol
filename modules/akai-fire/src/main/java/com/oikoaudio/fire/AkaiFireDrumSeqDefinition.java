package com.oikoaudio.fire;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class AkaiFireDrumSeqDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("c1f8d20a-3da4-4d2c-8ce1-8b2aa6a8e5b6");

    public AkaiFireDrumSeqDefinition() {
    }

    @Override
    public String getName() {
        return "Akai Fire by Oiko Audio";
    }

    @Override
    public String getAuthor() {
        return "Eric Ahrens / Richie Hawtin / David Fredman (Oiko Audio)";
    }

    @Override
    public String getVersion() {
        return "0.3.1";
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
        return "Controllers/Akai/Hawtin AKAI Fire.pdf";
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
    public AkaiFireDrumSeqExtension createInstance(final ControllerHost host) {
        return new AkaiFireDrumSeqExtension(this, host);
    }
}
