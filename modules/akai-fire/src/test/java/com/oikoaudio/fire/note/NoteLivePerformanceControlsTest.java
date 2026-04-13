package com.oikoaudio.fire.note;

import com.oikoaudio.fire.lights.BiColorLightState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoteLivePerformanceControlsTest {

    @Test
    void mute1TogglesSustainAndShowsStatus() {
        final List<Integer> sustainValues = new ArrayList<>();
        final List<String> status = new ArrayList<>();
        final NoteLivePerformanceControls controls = new NoteLivePerformanceControls(
                sustainValues::add,
                ignored -> {},
                () -> {},
                () -> false,
                (title, detail) -> status.add(title + ":" + detail));

        controls.handleMute1(true);
        controls.handleMute1(true);

        assertEquals(List.of(127, 0), sustainValues);
        assertEquals(List.of("Sustain:On", "Sustain:Off"), status);
        assertEquals(BiColorLightState.GREEN_HALF, controls.mute1LightState());
    }

    @Test
    void mute2TogglesSostenutoAndShowsStatus() {
        final List<Integer> sostenutoValues = new ArrayList<>();
        final List<String> status = new ArrayList<>();
        final NoteLivePerformanceControls controls = new NoteLivePerformanceControls(
                ignored -> {},
                sostenutoValues::add,
                () -> {},
                () -> false,
                (title, detail) -> status.add(title + ":" + detail));

        controls.handleMute2(true);

        assertEquals(List.of(127), sostenutoValues);
        assertEquals(List.of("Sostenuto:On"), status);
        assertEquals(BiColorLightState.AMBER_FULL, controls.mute2LightState());
    }

    @Test
    void mute3TogglesNoteRepeatOnlyOnPress() {
        final List<String> calls = new ArrayList<>();
        final AtomicBoolean active = new AtomicBoolean();
        final NoteLivePerformanceControls controls = new NoteLivePerformanceControls(
                ignored -> {},
                ignored -> {},
                () -> {
                    calls.add("toggle");
                    active.set(!active.get());
                },
                active::get,
                (title, detail) -> {});

        controls.handleMute3(false);
        controls.handleMute3(true);

        assertEquals(List.of("toggle"), calls);
        assertEquals(BiColorLightState.GREEN_FULL, controls.mute3LightState());
    }

    @Test
    void resetTogglesSendsZeroForActiveCcStates() {
        final List<Integer> sustainValues = new ArrayList<>();
        final List<Integer> sostenutoValues = new ArrayList<>();
        final NoteLivePerformanceControls controls = new NoteLivePerformanceControls(
                sustainValues::add,
                sostenutoValues::add,
                () -> {},
                () -> false,
                (title, detail) -> {});

        controls.handleMute1(true);
        controls.handleMute2(true);
        controls.resetToggles();

        assertEquals(List.of(127, 0), sustainValues);
        assertEquals(List.of(127, 0), sostenutoValues);
        assertEquals(BiColorLightState.GREEN_HALF, controls.mute1LightState());
        assertEquals(BiColorLightState.OFF, controls.mute2LightState());
    }
}
