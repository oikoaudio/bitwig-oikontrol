package com.oikoaudio.fire.note;

import com.oikoaudio.fire.lights.BiColorLightState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotePlayModeTest {

    @Test
    void delegatesLifecycleAndPadPressToLiveCollaborators() {
        final List<String> events = new ArrayList<>();
        final NotePlayMode mode = new NotePlayMode(
                new NoteLiveControlSurface(
                        new NoteLivePerformanceControls(
                                value -> events.add("sustain:" + value),
                                value -> events.add("sostenuto:" + value),
                                () -> events.add("repeat"),
                                () -> true,
                                (title, detail) -> {}),
                        new NoteLiveEncoderModeControls(
                                layer(events, "channel"),
                                layer(events, "mixer"),
                                layer(events, "user1"),
                                layer(events, "user2"),
                                ignored -> {}),
                        new NoteEncoderTouchResetHandler(
                                new com.oikoaudio.fire.control.TouchResetGesture(4, 0L, 0L, 2),
                                () -> false,
                                (task, delayMs) -> {},
                                0L,
                                () -> {}),
                        (title, detail) -> {},
                        (title, detail) -> {},
                        () -> {}),
                new NoteLivePadPerformer(new NoteLivePadPerformer.MidiOut() {
                    @Override
                    public void noteOn(final int midiNote, final int velocity) {
                        events.add("on:" + midiNote + ":" + velocity);
                    }

                    @Override
                    public void noteOff(final int midiNote) {
                        events.add("off:" + midiNote);
                    }
                }, pad -> 60 + pad, (baseVelocity, rawVelocity) -> baseVelocity));

        mode.activate();
        mode.handleMute1(true);
        mode.handleMute2(true);
        mode.handleMute3(true);
        mode.handlePadPress(0, true, 99, 100);
        mode.deactivate(() -> events.add("release"));

        assertEquals(BiColorLightState.GREEN_HALF, mode.mute1LightState());
        assertEquals(BiColorLightState.OFF, mode.mute2LightState());
        assertEquals(BiColorLightState.GREEN_FULL, mode.mute3LightState());
        assertEquals(List.of("sustain:127", "sostenuto:127", "repeat", "on:60:100", "release", "sustain:0", "sostenuto:0"),
                events.stream().filter(s -> !s.startsWith("deactivate:") && !s.startsWith("activate:")).toList());
    }

    private static NoteLiveEncoderModeControls.LayerHandle layer(final List<String> events, final String name) {
        return new NoteLiveEncoderModeControls.LayerHandle() {
            @Override
            public void activate() {
                events.add("activate:" + name);
            }

            @Override
            public void deactivate() {
                events.add("deactivate:" + name);
            }
        };
    }
}
