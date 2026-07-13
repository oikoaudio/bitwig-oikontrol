package com.oikoaudio.fire.note;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.lights.BiColorLightState;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class NoteLivePerformanceControlsTest {

    @Test
    void mute1TogglesSustainAndShowsStatus() {
        final List<Integer> sustainValues = new ArrayList<>();
        final List<String> status = new ArrayList<>();
        final NoteLivePerformanceControls controls =
                new NoteLivePerformanceControls(
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
        final NoteLivePerformanceControls controls =
                new NoteLivePerformanceControls(
                        ignored -> {},
                        sostenutoValues::add,
                        () -> {},
                        () -> false,
                        (title, detail) -> status.add(title + ":" + detail));

        controls.handleMute2(true);

        assertEquals(List.of(127), sostenutoValues);
        assertEquals(List.of("Sostenuto:On"), status);
        assertEquals(BiColorLightState.GREEN_FULL, controls.mute2LightState());
    }

    @Test
    void mute3TapTogglesNoteRepeatOnRelease() {
        final List<String> calls = new ArrayList<>();
        final AtomicBoolean active = new AtomicBoolean();
        final NoteLivePerformanceControls controls =
                new NoteLivePerformanceControls(
                        ignored -> {},
                        ignored -> {},
                        () -> {
                            calls.add("toggle");
                            active.set(!active.get());
                        },
                        active::get,
                        (title, detail) -> {});

        controls.handleMute3(true);
        assertEquals(List.of(), calls);
        controls.handleMute3(false);

        assertEquals(List.of("toggle"), calls);
        assertEquals(BiColorLightState.GREEN_FULL, controls.mute3LightState());
    }

    @Test
    void mute3SelectTurnConsumesTheTapWithoutTogglingNoteRepeat() {
        final List<String> calls = new ArrayList<>();
        final NoteLivePerformanceControls controls =
                new NoteLivePerformanceControls(
                        ignored -> {},
                        ignored -> {},
                        () -> calls.add("toggle"),
                        () -> false,
                        (title, detail) -> {});

        controls.handleMute3(true);
        assertEquals(true, controls.consumeMute3EncoderTurn());
        controls.handleMute3(false);

        assertEquals(List.of(), calls);
        assertEquals(false, controls.consumeMute3EncoderTurn());
    }

    @Test
    void mute4TogglesHoldAndShowsStatus() {
        final List<String> status = new ArrayList<>();
        final AtomicBoolean active = new AtomicBoolean();
        final NoteLivePerformanceControls controls =
                new NoteLivePerformanceControls(
                        ignored -> {},
                        ignored -> {},
                        () -> {},
                        () -> false,
                        () -> {
                            active.set(!active.get());
                            return active.get();
                        },
                        active::get,
                        (title, detail) -> status.add(title + ":" + detail));

        controls.handleMute4(false);
        controls.handleMute4(true);
        controls.handleMute4(true);

        assertEquals(List.of("Hold:On", "Hold:Off"), status);
        assertEquals(BiColorLightState.GREEN_HALF, controls.mute4LightState());
    }

    @Test
    void activeHoldLightsMute4() {
        final NoteLivePerformanceControls controls =
                new NoteLivePerformanceControls(
                        ignored -> {},
                        ignored -> {},
                        () -> {},
                        () -> false,
                        () -> true,
                        () -> true,
                        (title, detail) -> {});

        assertEquals(BiColorLightState.GREEN_FULL, controls.mute4LightState());
    }

    @Test
    void resetTogglesSendsZeroForActiveCcStates() {
        final List<Integer> sustainValues = new ArrayList<>();
        final List<Integer> sostenutoValues = new ArrayList<>();
        final NoteLivePerformanceControls controls =
                new NoteLivePerformanceControls(
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
        assertEquals(BiColorLightState.GREEN_HALF, controls.mute2LightState());
    }
}
