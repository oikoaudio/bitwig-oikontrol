package com.oikoaudio.fire.multiclip;

final class MulticlipSceneActionResolver {
    enum Action {
        IGNORE,
        LAUNCH_AND_FOLLOW,
        SELECT,
        COPY_CLIP,
        COPY_SCENE
    }

    private MulticlipSceneActionResolver() {}

    static Action resolve(
            final boolean pressed,
            final boolean sceneExists,
            final boolean altHeld,
            final boolean selectHeld,
            final boolean copyHeld,
            final boolean shiftHeld) {
        if (!pressed) {
            return Action.IGNORE;
        }
        if (copyHeld) {
            return shiftHeld ? Action.COPY_SCENE : Action.COPY_CLIP;
        }
        if (altHeld || selectHeld) {
            return Action.SELECT;
        }
        return sceneExists ? Action.LAUNCH_AND_FOLLOW : Action.IGNORE;
    }
}
