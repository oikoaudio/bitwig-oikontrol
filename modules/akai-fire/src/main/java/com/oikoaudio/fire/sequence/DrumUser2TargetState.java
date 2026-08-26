package com.oikoaudio.fire.sequence;

/** Session-local target selection for Drum XOX's User 2 encoder page. */
final class DrumUser2TargetState {
    enum Target {
        PAD_REMOTES,
        KIT_REMOTES,
        EUCLID
    }

    private Target target = Target.PAD_REMOTES;

    Target target() {
        return target;
    }

    Target cycle() {
        target =
                switch (target) {
                    case PAD_REMOTES -> Target.KIT_REMOTES;
                    case KIT_REMOTES -> Target.EUCLID;
                    case EUCLID -> Target.PAD_REMOTES;
                };
        return target;
    }

    static int remoteParameterIndex(final int encoderIndex, final boolean altHeld) {
        return encoderIndex + (altHeld ? 4 : 0);
    }
}
