package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EditorPresentationPlanTest {
    @ParameterizedTest
    @CsvSource({
        "true,true,true,NATIVE_GROUP",
        "true,true,false,ACTIVE_CLIP",
        "true,false,false,ACTIVE_CLIP",
        "false,false,false,NONE"
    })
    void choosesNativeContextOrDeterministicActiveClipFallback(
            final boolean group,
            final boolean scene,
            final boolean retained,
            final EditorPresentationPlan expected) {
        assertEquals(expected, EditorPresentationPlan.choose(group, scene, retained));
    }
}
