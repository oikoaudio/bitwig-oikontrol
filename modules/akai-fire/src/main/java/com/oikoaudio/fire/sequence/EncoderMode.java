package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.lights.BiColorLightState;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Parameter;

public enum EncoderMode {
    CHANNEL(BiColorLightState.MODE_CHANNEL, "1: Velocity\n2: Chance\n3: Repeats\n4: Pitch", //
        new EncoderAccess[]{NoteStepAccess.VELOCITY, NoteStepAccess.CHANCE, NoteStepAccess.REPEATS, NoteStepAccess.PITCH}),
    MIXER(BiColorLightState.MODE_MIXER, "1: Velocity Spread\n2: Pressure\n3: Length\n4: Occurrence",
            new EncoderAccess[]{NoteStepAccess.VELOCITY_SPREAD, NoteStepAccess.PRESSURE, //
                    NoteStepAccess.DURATION, NoteStepAccess.OCCURENCE}), //
    MIXER_SHIFT(BiColorLightState.MODE_MIXER, "1: Repeat Curve\n2: Repeat Vel Curve\n3: Repeat Vel End\n4: Occurrence",
            new EncoderAccess[]{NoteStepAccess.REPEATCURVE, NoteStepAccess.REPEAT_VEL_CRV, //
                    NoteStepAccess.REPEAT_VEL_END, NoteStepAccess.OCCURENCE}),
    USER_1(BiColorLightState.MODE_USER1, "1: Level\n2: Pan\n3: Fx1\n4: Fx2", new EncoderAccess[]{}),
	USER_2(BiColorLightState.MODE_USER2, "1: Attack / Tunes\n2: Decay / Decay \n3: Sustain / Param 1\n4: Release / Param 2", new EncoderAccess[]{}),
    USER_2_SHIFT(BiColorLightState.MODE_USER2, "1: param5\n2: param6 \n3: param7\n4: param8", new EncoderAccess[]{});

    private final BiColorLightState state;
    private final String info;
    private final EncoderAccess[] assignments;

    EncoderMode(final BiColorLightState state, final String info, final EncoderAccess[] assignments) {
        this.state = state;
        this.info = info;
        this.assignments = assignments;
    }

    public EncoderAccess[] getAssignments() {
        return assignments;
    }

    public BiColorLightState getState() {
        return state;
    }

    public String getInfo() {
        return info;
    }

    public String getDynamicInfo(final CursorRemoteControlsPage remotePage) {
        if (remotePage == null) {
            return info; // Fallback to the static string if no remote page is available
        }
        StringBuilder sb = new StringBuilder();
        // If this is the USER_2_SHIFT mode, start at index 4; otherwise, start at 0.
        int startIndex = (this == USER_2_SHIFT) ? 4 : 0;
        int displayCount = 4; // We always want to show 4 parameters.
        for (int i = startIndex; i < startIndex + displayCount; i++) {
            // Make sure we don't exceed the available parameters
            if (i < remotePage.getParameterCount()) {
                Parameter param = remotePage.getParameter(i);
                String name = param.name().get();
                sb.append((i - startIndex + 1) + ": " + name);
                if (i < startIndex + displayCount - 1) {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }


}