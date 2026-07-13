package com.oikoaudio.fire.note;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.oikoaudio.fire.control.EncoderTouchDisplayHandler;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.sequence.EncoderMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class NoteLiveControlSurfaceTest {

    @Test
    void activateResetsEncoderModeToChannel() {
        final List<String> events = new ArrayList<>();
        final NoteLiveControlSurface surface = createSurface(events);

        surface.handleModeAdvance(true, false);
        events.clear();

        surface.activate();

        assertEquals(
                List.of(
                        "deactivate:CHANNEL",
                        "deactivate:MIXER",
                        "deactivate:USER_1",
                        "deactivate:USER_2",
                        "step:CHANNEL",
                        "activate:CHANNEL"),
                events);
    }

    @Test
    void deactivateResetsPerformanceTogglesAndEncoderLayers() {
        final List<String> ccEvents = new ArrayList<>();
        final List<String> layerEvents = new ArrayList<>();
        final NoteLivePerformanceControls performanceControls =
                new NoteLivePerformanceControls(
                        value -> ccEvents.add("sustain:" + value),
                        value -> ccEvents.add("sostenuto:" + value),
                        () -> {},
                        () -> false,
                        (title, detail) -> {});
        final NoteLiveControlSurface surface =
                new NoteLiveControlSurface(
                        performanceControls,
                        createEncoderControls(layerEvents),
                        createTouchDisplayHandler(new ArrayList<>()),
                        (title, detail) -> {},
                        (title, detail) -> {},
                        () -> {});

        surface.handleMute1(true);
        surface.handleMute2(true);
        ccEvents.clear();

        surface.deactivate();

        assertEquals(List.of("sustain:0", "sostenuto:0"), ccEvents);
        assertEquals(
                List.of(
                        "deactivate:CHANNEL",
                        "deactivate:MIXER",
                        "deactivate:USER_1",
                        "deactivate:USER_2"),
                layerEvents);
    }

    @Test
    void modeAdvanceCyclesAndShowsDetailOnlyInLivePlay() {
        final List<String> details = new ArrayList<>();
        final NoteLiveControlSurface surface =
                new NoteLiveControlSurface(
                        new NoteLivePerformanceControls(
                                ignored -> {},
                                ignored -> {},
                                () -> {},
                                () -> false,
                                (title, detail) -> {}),
                        createEncoderControls(new ArrayList<>()),
                        createTouchDisplayHandler(new ArrayList<>()),
                        (title, detail) -> {},
                        (title, detail) -> details.add(title + ":" + detail),
                        () -> {});

        surface.handleModeAdvance(true, true);
        surface.handleModeAdvance(false, false);
        surface.handleModeAdvance(true, false);

        assertEquals(BiColorLightState.MODE_MIXER, surface.modeLightState());
        assertEquals(List.of("Encoder Mode:1: Volume\n2: Pan\n3: Send 1\n4: Send 2"), details);
    }

    @Test
    void muteLightsDelegateToPerformanceControls() {
        final AtomicBoolean repeatActive = new AtomicBoolean();
        final NoteLiveControlSurface surface =
                new NoteLiveControlSurface(
                        new NoteLivePerformanceControls(
                                ignored -> {},
                                ignored -> {},
                                () -> repeatActive.set(!repeatActive.get()),
                                repeatActive::get,
                                (title, detail) -> {}),
                        createEncoderControls(new ArrayList<>()),
                        createTouchDisplayHandler(new ArrayList<>()),
                        (title, detail) -> {},
                        (title, detail) -> {},
                        () -> {});

        surface.handleMute1(true);
        surface.handleMute2(true);
        surface.handleMute3(true);

        assertEquals(BiColorLightState.GREEN_FULL, surface.mute1LightState());
        assertEquals(BiColorLightState.GREEN_FULL, surface.mute2LightState());
        assertEquals(BiColorLightState.GREEN_FULL, surface.mute3LightState());
    }

    @Test
    void muteLightsUseConsistentDimOffStates() {
        final AtomicBoolean repeatActive = new AtomicBoolean();
        final AtomicBoolean holdActive = new AtomicBoolean();
        final NoteLiveControlSurface surface =
                new NoteLiveControlSurface(
                        new NoteLivePerformanceControls(
                                ignored -> {},
                                ignored -> {},
                                () -> repeatActive.set(!repeatActive.get()),
                                repeatActive::get,
                                () -> {
                                    holdActive.set(!holdActive.get());
                                    return holdActive.get();
                                },
                                holdActive::get,
                                (title, detail) -> {}),
                        createEncoderControls(new ArrayList<>()),
                        createTouchDisplayHandler(new ArrayList<>()),
                        (title, detail) -> {},
                        (title, detail) -> {},
                        () -> {});

        assertEquals(BiColorLightState.GREEN_HALF, surface.mute1LightState());
        assertEquals(BiColorLightState.GREEN_HALF, surface.mute2LightState());
        assertEquals(BiColorLightState.GREEN_HALF, surface.mute3LightState());
        assertEquals(BiColorLightState.GREEN_HALF, surface.mute4LightState());

        surface.handleMute1(true);
        surface.handleMute2(true);
        surface.handleMute3(true);
        surface.handleMute4(true);

        assertEquals(BiColorLightState.GREEN_FULL, surface.mute1LightState());
        assertEquals(BiColorLightState.GREEN_FULL, surface.mute2LightState());
        assertEquals(BiColorLightState.GREEN_FULL, surface.mute3LightState());
        assertEquals(BiColorLightState.GREEN_FULL, surface.mute4LightState());
    }

    @Test
    void performanceStatusLightsUseTrackSelectGreenValues() {
        assertEquals(
                BiColorLightState.AMBER_HALF,
                LivePadSurfaceLayer.performanceStatusLightState(BiColorLightState.GREEN_HALF));
        assertEquals(
                BiColorLightState.AMBER_FULL,
                LivePadSurfaceLayer.performanceStatusLightState(BiColorLightState.GREEN_FULL));
    }

    private static NoteLiveControlSurface createSurface(final List<String> events) {
        return new NoteLiveControlSurface(
                new NoteLivePerformanceControls(
                        ignored -> {}, ignored -> {}, () -> {}, () -> false, (title, detail) -> {}),
                createEncoderControls(events),
                createTouchDisplayHandler(new ArrayList<>()),
                (title, detail) -> {},
                (title, detail) -> {},
                () -> {});
    }

    @Test
    void expressionTouchShowsValueAndClearsOnRelease() {
        final List<String> events = new ArrayList<>();
        final NoteLiveControlSurface surface =
                new NoteLiveControlSurface(
                        new NoteLivePerformanceControls(
                                ignored -> {},
                                ignored -> {},
                                () -> {},
                                () -> false,
                                (title, detail) -> {}),
                        createEncoderControls(new ArrayList<>()),
                        createTouchDisplayHandler(new ArrayList<>()),
                        (title, detail) -> events.add("value:" + title + ":" + detail),
                        (title, detail) -> {},
                        () -> events.add("clear"));

        surface.handleExpressionTouch(true, "Mod", "27");
        surface.handleExpressionTouch(false, "Mod", "27");

        assertEquals(List.of("value:Mod:27", "clear"), events);
    }

    @Test
    void resettableTouchAndAdjustmentDelegateToTouchHandler() {
        final List<String> events = new ArrayList<>();
        final NoteLiveControlSurface surface =
                new NoteLiveControlSurface(
                        new NoteLivePerformanceControls(
                                ignored -> {},
                                ignored -> {},
                                () -> {},
                                () -> false,
                                (title, detail) -> {}),
                        createEncoderControls(new ArrayList<>()),
                        new EncoderTouchDisplayHandler(() -> events.add("clear")),
                        (title, detail) -> {},
                        (title, detail) -> {},
                        () -> {});

        surface.handleResettableTouch(3, true, () -> events.add("show"), () -> events.add("reset"));
        surface.handleResettableTouch(
                3, false, () -> events.add("show"), () -> events.add("reset"));

        assertEquals(List.of("show", "clear"), events);
    }

    private static NoteLiveEncoderModeControls createEncoderControls(final List<String> events) {
        return new NoteLiveEncoderModeControls(
                layer(events, EncoderMode.CHANNEL),
                layer(events, EncoderMode.MIXER),
                layer(events, EncoderMode.USER_1),
                layer(events, EncoderMode.USER_2),
                mode -> events.add("step:" + mode),
                NoteLiveEncoderModeControls::modeInfo);
    }

    private static EncoderTouchDisplayHandler createTouchDisplayHandler(final List<String> events) {
        return new EncoderTouchDisplayHandler(() -> events.add("clear"));
    }

    private static NoteLiveEncoderModeControls.LayerHandle layer(
            final List<String> events, final EncoderMode mode) {
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
