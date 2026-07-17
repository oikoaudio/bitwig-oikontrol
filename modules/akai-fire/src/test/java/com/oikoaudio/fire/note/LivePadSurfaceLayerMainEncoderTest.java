package com.oikoaudio.fire.note;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import org.junit.jupiter.api.Test;

class LivePadSurfaceLayerMainEncoderTest {

    @Test
    void shiftPressCyclesAwayFromNoteRepeatRole() {
        assertEquals(
                LivePadSurfaceLayer.MainEncoderPressTarget.CYCLE_ROLE,
                LivePadSurfaceLayer.mainEncoderPressTarget(
                        true, true, AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE));
    }

    @Test
    void noteRepeatRoleReceivesMainEncoderPresses() {
        assertEquals(
                LivePadSurfaceLayer.MainEncoderPressTarget.NOTE_REPEAT,
                LivePadSurfaceLayer.mainEncoderPressTarget(
                        true, false, AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE));
        assertEquals(
                LivePadSurfaceLayer.MainEncoderPressTarget.NOTE_REPEAT,
                LivePadSurfaceLayer.mainEncoderPressTarget(
                        false, false, AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE));
    }

    @Test
    void tempoRoleRoutesTurnsAndPressesToTempoAfterLeavingNoteRepeat() {
        assertEquals(
                LivePadSurfaceLayer.MainEncoderTurnTarget.CURRENT_ROLE,
                LivePadSurfaceLayer.mainEncoderTurnTarget(
                        AkaiFireOikontrolExtension.MAIN_ENCODER_TEMPO_ROLE));
        assertEquals(
                LivePadSurfaceLayer.MainEncoderPressTarget.CURRENT_ROLE,
                LivePadSurfaceLayer.mainEncoderPressTarget(
                        true, false, AkaiFireOikontrolExtension.MAIN_ENCODER_TEMPO_ROLE));
    }
}
