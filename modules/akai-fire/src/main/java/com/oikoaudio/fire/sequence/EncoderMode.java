package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.lights.BiColorLightState;

public enum EncoderMode {
    CHANNEL(BiColorLightState.MODE_CHANNEL, "1: Note Length\n2: Chance\n3: Vel Spread\n4: Repeat",
        new EncoderAccess[]{NoteStepAccess.DURATION, NoteStepAccess.CHANCE, NoteStepAccess.VELOCITY_SPREAD,
            NoteStepAccess.REPEATS}),
    MIXER(BiColorLightState.MODE_MIXER, "1: Volume\n2: Pan\n3: Send 1\n4: Send 2", new EncoderAccess[]{}), //
    USER_1(BiColorLightState.MODE_USER1, "1: Velocity\n2: Pressure\n3: Timbre\n4: Pitch Expr",
        new EncoderAccess[]{NoteStepAccess.VELOCITY, NoteStepAccess.PRESSURE, NoteStepAccess.TIMBRE,
            NoteStepAccess.PITCH}),
	USER_2(BiColorLightState.MODE_USER2, "1: Note Length\n2: Chance\n3: Vel Spread\n4: Repeat",
        new EncoderAccess[]{});

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

}
