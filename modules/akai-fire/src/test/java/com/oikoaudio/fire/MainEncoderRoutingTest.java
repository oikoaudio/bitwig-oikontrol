package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MainEncoderRoutingTest {
    @ParameterizedTest
    @CsvSource({
        "Last Touched Parameter,LAST_TOUCHED",
        "Note Repeat,NOTE_REPEAT",
        "Tempo,TEMPO",
        "Shuffle,SHUFFLE",
        "Track Select,TRACK_SELECT",
        "Playback Start,PLAYBACK_START",
        "Drum Grid,DRUM_GRID"
    })
    void exposesStableRoleVocabulary(final String preference, final MainEncoderRouting.Role role) {
        final AkaiFireOikontrolExtension extension = mock(AkaiFireOikontrolExtension.class);
        when(extension.getMainEncoderRolePreference()).thenReturn(preference);

        assertEquals(role, new MainEncoderRouting(extension).currentRole());
    }
}
