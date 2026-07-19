package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.DeviceLayer;
import com.bitwig.extension.controller.api.DeviceLayerBank;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.Parameter;
import com.oikoaudio.fire.control.EncoderValueProfile;
import com.oikoaudio.fire.control.ParameterEncoderBinding;
import com.oikoaudio.fire.display.OledDisplay;
import java.util.function.BooleanSupplier;

/** Group-rooted pad/output-chain mixer targets plus first-instrument remote navigation. */
final class MulticlipDrumPadEncoderController {
    private static final int PAD_COUNT = 16;
    private static final int LOWEST_PAD_NOTE = 36;
    private final OledDisplay oled;
    private final Parameter[][] padMixerParameters = new Parameter[PAD_COUNT][4];
    private final Parameter[][] layerMixerParameters = new Parameter[PAD_COUNT][4];
    private final CursorRemoteControlsPage[] remotePages = new CursorRemoteControlsPage[PAD_COUNT];
    private final CursorRemoteControlsPage groupRemotePage;
    private final BooleanSupplier drumPadsAvailable;
    private final BooleanSupplier groupInstrumentAvailable;
    private int activePad;

    MulticlipDrumPadEncoderController(
            final DrumPadBank padBank,
            final DeviceLayerBank layerBank,
            final CursorRemoteControlsPage groupRemotePage,
            final BooleanSupplier drumPadsAvailable,
            final BooleanSupplier groupInstrumentAvailable,
            final OledDisplay oled) {
        this.oled = oled;
        this.groupRemotePage = groupRemotePage;
        this.drumPadsAvailable = drumPadsAvailable;
        this.groupInstrumentAvailable = groupInstrumentAvailable;
        observeRemotePage(groupRemotePage);
        padBank.scrollPosition().markInterested();
        padBank.scrollPosition().set(LOWEST_PAD_NOTE);
        layerBank.scrollPosition().markInterested();
        layerBank.scrollPosition().set(0);
        for (int index = 0; index < PAD_COUNT; index++) {
            final DrumPad pad = padBank.getItemAt(index);
            padMixerParameters[index][0] = pad.volume();
            padMixerParameters[index][1] = pad.pan();
            padMixerParameters[index][2] = pad.sendBank().getItemAt(0);
            padMixerParameters[index][3] = pad.sendBank().getItemAt(1);
            for (final Parameter parameter : padMixerParameters[index]) {
                ParameterEncoderBinding.markInterested(parameter);
            }
            final CursorRemoteControlsPage page =
                    pad.createDeviceBank(1).getDevice(0).createCursorRemoteControlsPage(8);
            observeRemotePage(page);
            remotePages[index] = page;

            final DeviceLayer layer = layerBank.getItemAt(index);
            layer.exists().markInterested();
            layerMixerParameters[index][0] = layer.volume();
            layerMixerParameters[index][1] = layer.pan();
            layerMixerParameters[index][2] = layer.sendBank().getItemAt(0);
            layerMixerParameters[index][3] = layer.sendBank().getItemAt(1);
            for (final Parameter parameter : layerMixerParameters[index]) {
                ParameterEncoderBinding.markInterested(parameter);
            }
        }
    }

    void setActivePad(final int activePad) {
        if (activePad >= 0 && activePad < PAD_COUNT) {
            this.activePad = activePad;
        }
    }

    CursorRemoteControlsPage activeRemoteControlsPage() {
        if (drumPadsAvailable.getAsBoolean()) {
            return remotePages[activePad];
        }
        return groupInstrumentAvailable.getAsBoolean() ? groupRemotePage : null;
    }

    boolean usesDrumPadRemotePage() {
        return drumPadsAvailable.getAsBoolean();
    }

    private static void observeRemotePage(final CursorRemoteControlsPage page) {
        page.selectedPageIndex().markInterested();
        page.pageCount().markInterested();
        page.pageNames().markInterested();
        page.getName().markInterested();
        for (int parameter = 0; parameter < 8; parameter++) {
            ParameterEncoderBinding.markInterested(page.getParameter(parameter));
        }
    }

    void adjustMixer(final int index, final boolean fine, final int increment) {
        final Parameter parameter = mixerParameter(index);
        if (!ParameterEncoderBinding.isMapped(parameter)) {
            oled.valueInfo(mixerLabel(index), "Unmapped");
            return;
        }
        final EncoderValueProfile profile =
                index == 1 ? EncoderValueProfile.PAN : EncoderValueProfile.LARGE_RANGE;
        ParameterEncoderBinding.adjustParameter(parameter, fine, increment, profile);
        ParameterEncoderBinding.showValue(parameter, mixerLabel(index), oled::valueInfo);
    }

    boolean hasMixerParameter(final int index) {
        return index >= 0
                && index < activeMixerParameters().length
                && ParameterEncoderBinding.isMapped(activeMixerParameters()[index]);
    }

    void showMixer(final int index) {
        final Parameter parameter = mixerParameter(index);
        if (!ParameterEncoderBinding.isMapped(parameter)) {
            oled.valueInfo(mixerLabel(index), "Unmapped");
            return;
        }
        ParameterEncoderBinding.showValue(parameter, mixerLabel(index), oled::valueInfo);
    }

    void resetMixer(final int index) {
        final Parameter parameter = mixerParameter(index);
        if (ParameterEncoderBinding.isMapped(parameter)) {
            ParameterEncoderBinding.ResetPolicy.PARAMETER_DEFAULT.reset(parameter);
        }
    }

    private Parameter mixerParameter(final int index) {
        final Parameter[] parameters = activeMixerParameters();
        return index >= 0 && index < parameters.length ? parameters[index] : null;
    }

    private Parameter[] activeMixerParameters() {
        return drumPadsAvailable.getAsBoolean()
                ? padMixerParameters[activePad]
                : layerMixerParameters[activePad];
    }

    private String mixerLabel(final int parameterIndex) {
        final String target = drumPadsAvailable.getAsBoolean() ? "Pad " : "Chain ";
        return target
                + switch (parameterIndex) {
                    case 0 -> "Volume";
                    case 1 -> "Pan";
                    case 2 -> "Send 1";
                    default -> "Send 2";
                };
    }
}
