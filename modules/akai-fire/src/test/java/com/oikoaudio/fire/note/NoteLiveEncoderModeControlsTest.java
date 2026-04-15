package com.oikoaudio.fire.note;

import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.sequence.EncoderMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoteLiveEncoderModeControlsTest {

    @Test
    void resetToChannelActivatesChannelLayerAndAppliesStepSize() {
        final List<String> events = new ArrayList<>();
        final NoteLiveEncoderModeControls controls = createControls(events);

        controls.advanceMode();
        events.clear();

        controls.resetToChannel();

        assertEquals(EncoderMode.CHANNEL, controls.mode());
        assertEquals(BiColorLightState.MODE_CHANNEL, controls.lightState());
        assertEquals("1: Mod\n2: Pitch Bend\n3: Pitch Gliss\n4: Scale", controls.modeInfo());
        assertEquals(List.of(
                "deactivate:CHANNEL",
                "deactivate:MIXER",
                "deactivate:USER_1",
                "deactivate:USER_2",
                "step:CHANNEL",
                "activate:CHANNEL"), events);
    }

    @Test
    void advanceModeCyclesThroughLivePages() {
        final List<String> events = new ArrayList<>();
        final NoteLiveEncoderModeControls controls = createControls(events);

        controls.advanceMode();
        controls.advanceMode();
        controls.advanceMode();
        controls.advanceMode();

        assertEquals(EncoderMode.CHANNEL, controls.mode());
        assertEquals(BiColorLightState.MODE_CHANNEL, controls.lightState());
        assertEquals(List.of(
                "deactivate:CHANNEL",
                "deactivate:MIXER",
                "deactivate:USER_1",
                "deactivate:USER_2",
                "step:MIXER",
                "activate:MIXER",
                "deactivate:CHANNEL",
                "deactivate:MIXER",
                "deactivate:USER_1",
                "deactivate:USER_2",
                "step:USER_1",
                "activate:USER_1",
                "deactivate:CHANNEL",
                "deactivate:MIXER",
                "deactivate:USER_1",
                "deactivate:USER_2",
                "step:USER_2",
                "activate:USER_2",
                "deactivate:CHANNEL",
                "deactivate:MIXER",
                "deactivate:USER_1",
                "deactivate:USER_2",
                "step:CHANNEL",
                "activate:CHANNEL"), events);
    }

    private static NoteLiveEncoderModeControls createControls(final List<String> events) {
        return new NoteLiveEncoderModeControls(
                layer(events, EncoderMode.CHANNEL),
                layer(events, EncoderMode.MIXER),
                layer(events, EncoderMode.USER_1),
                layer(events, EncoderMode.USER_2),
                mode -> events.add("step:" + mode),
                NoteLiveEncoderModeControls::modeInfo);
    }

    private static NoteLiveEncoderModeControls.LayerHandle layer(final List<String> events, final EncoderMode mode) {
        return new NoteLiveEncoderModeControls.LayerHandle() {
            @Override
            public void activate() {
                events.add("activate:" + mode);
            }

            @Override
            public void deactivate() {
                events.add("deactivate:" + mode);
            }
        };
    }
}
