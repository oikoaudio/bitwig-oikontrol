package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.Parameter;
import com.oikoaudio.fire.control.EncoderValueProfile;
import com.oikoaudio.fire.control.ParameterEncoderBinding;
import com.oikoaudio.fire.display.OledDisplay;

/** Encoder targets rooted at the pinned group Drum Machine, independent of child selection. */
final class MulticlipDrumPadEncoderController {
    private static final int PAD_COUNT = 16;
    private static final int LOWEST_PAD_NOTE = 36;
    private final OledDisplay oled;
    private final Parameter[][] mixerParameters = new Parameter[PAD_COUNT][4];
    private final CursorRemoteControlsPage[] remotePages = new CursorRemoteControlsPage[PAD_COUNT];
    private int activePad;

    MulticlipDrumPadEncoderController(final DrumPadBank padBank, final OledDisplay oled) {
        this.oled = oled;
        padBank.scrollPosition().markInterested();
        padBank.scrollPosition().set(LOWEST_PAD_NOTE);
        for (int index = 0; index < PAD_COUNT; index++) {
            final DrumPad pad = padBank.getItemAt(index);
            mixerParameters[index][0] = pad.volume();
            mixerParameters[index][1] = pad.pan();
            mixerParameters[index][2] = pad.sendBank().getItemAt(0);
            mixerParameters[index][3] = pad.sendBank().getItemAt(1);
            for (final Parameter parameter : mixerParameters[index]) {
                ParameterEncoderBinding.markInterested(parameter);
            }
            final CursorRemoteControlsPage page =
                    pad.createDeviceBank(1).getDevice(0).createCursorRemoteControlsPage(8);
            page.selectedPageIndex().markInterested();
            page.pageCount().markInterested();
            page.pageNames().markInterested();
            page.getName().markInterested();
            for (int parameter = 0; parameter < 8; parameter++) {
                ParameterEncoderBinding.markInterested(page.getParameter(parameter));
            }
            remotePages[index] = page;
        }
    }

    void setActivePad(final int activePad) {
        if (activePad >= 0 && activePad < PAD_COUNT) {
            this.activePad = activePad;
        }
    }

    CursorRemoteControlsPage activeRemoteControlsPage() {
        return remotePages[activePad];
    }

    void adjust(final MulticlipEncoderTarget target, final boolean fine, final int increment) {
        final Parameter parameter = parameter(target);
        if (!ParameterEncoderBinding.isMapped(parameter)) {
            oled.valueInfo(label(target), "Unmapped");
            return;
        }
        final EncoderValueProfile profile =
                target.kind() == MulticlipEncoderTarget.Kind.PAD_MIXER
                                && target.parameterIndex() == 1
                        ? EncoderValueProfile.PAN
                        : EncoderValueProfile.LARGE_RANGE;
        ParameterEncoderBinding.adjustParameter(parameter, fine, increment, profile);
        ParameterEncoderBinding.showValue(parameter, label(target), oled::valueInfo);
    }

    private Parameter parameter(final MulticlipEncoderTarget target) {
        return switch (target.kind()) {
            case PAD_MIXER -> mixerParameters[activePad][target.parameterIndex()];
            case PAD_REMOTE -> remotePages[activePad].getParameter(target.parameterIndex());
            default -> null;
        };
    }

    private static String label(final MulticlipEncoderTarget target) {
        if (target.kind() == MulticlipEncoderTarget.Kind.PAD_REMOTE) {
            return "Remote " + (target.parameterIndex() + 1);
        }
        return switch (target.parameterIndex()) {
            case 0 -> "Pad Volume";
            case 1 -> "Pad Pan";
            case 2 -> "Pad Send 1";
            default -> "Pad Send 2";
        };
    }
}
