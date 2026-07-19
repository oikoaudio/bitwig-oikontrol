package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MulticlipSceneActionResolverTest {
    @Test
    void launchesAndFollowsAnExistingSceneForEditing() {
        assertEquals(
                MulticlipSceneActionResolver.Action.LAUNCH_AND_FOLLOW,
                MulticlipSceneActionResolver.resolve(true, true, false, false, false, false));
    }

    @Test
    void ignoresAPlainPadForANonexistentScene() {
        assertEquals(
                MulticlipSceneActionResolver.Action.IGNORE,
                MulticlipSceneActionResolver.resolve(true, false, false, false, false, false));
    }

    @Test
    void altAndMuteOneSelectWithoutLaunching() {
        assertEquals(
                MulticlipSceneActionResolver.Action.SELECT,
                MulticlipSceneActionResolver.resolve(true, false, true, false, false, false));
        assertEquals(
                MulticlipSceneActionResolver.Action.SELECT,
                MulticlipSceneActionResolver.resolve(true, false, false, true, false, false));
    }

    @Test
    void muteThreeCopiesTheClipOrTheWholeChildSceneWithShift() {
        assertEquals(
                MulticlipSceneActionResolver.Action.COPY_CLIP,
                MulticlipSceneActionResolver.resolve(true, false, false, false, true, false));
        assertEquals(
                MulticlipSceneActionResolver.Action.COPY_SCENE,
                MulticlipSceneActionResolver.resolve(true, false, false, false, true, true));
    }

    @Test
    void ignoresPadReleases() {
        assertEquals(
                MulticlipSceneActionResolver.Action.IGNORE,
                MulticlipSceneActionResolver.resolve(false, true, false, false, false, false));
    }
}
