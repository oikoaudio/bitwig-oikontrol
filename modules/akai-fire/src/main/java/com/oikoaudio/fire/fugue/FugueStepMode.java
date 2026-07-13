package com.oikoaudio.fire.fugue;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.control.ModeButtonLights;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.melodic.MelodicPattern;
import com.oikoaudio.fire.note.NoteGridLayout;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.sequence.NoteVariationAmounts;
import com.oikoaudio.fire.sequence.NoteVariationGesture;
import com.oikoaudio.fire.sequence.NoteVariationParameter;
import com.oikoaudio.fire.sequence.ObservedNoteVariationAdapter;
import com.oikoaudio.fire.sequence.StepPadLightHelper;
import com.oikoaudio.fire.utils.PatternButtons;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class FugueStepMode extends Layer {
    private static final int STEP_COUNT = FuguePattern.MAX_STEPS;
    private static final int DEFAULT_LOOP_STEPS = 32;
    private static final double STEP_LENGTH = 0.125;
    private static final int LINE_COUNT = 4;
    private static final int PAD_COLUMNS = 16;
    private static final int SOURCE_CHANGE_REBUILD_DELAY_MS = 20;
    private static final int BAR_STEPS = 32;
    private static final int MAX_LOOP_STEPS = STEP_COUNT;
    private static final int MIN_LOOP_STEPS = 1;
    private static final int[] CLIP_LENGTH_STEPS = {
        8, 16, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, 480, 512
    };
    private static final RgbLightState[] LINE_COLORS = {
        new RgbLightState(112, 0, 0, true),
        new RgbLightState(112, 44, 0, true),
        new RgbLightState(104, 88, 0, true),
        new RgbLightState(0, 88, 32, true)
    };

    private final AkaiFireOikontrolExtension driver;
    private final OledDisplay oled;
    private final PatternButtons patternButtons;
    private final PinnableCursorClip cursorClip;
    private final FugueTemplatePadController templatePads;
    private final FugueObservationController observations =
            new FugueObservationController(STEP_COUNT);
    private final FugueLineSettings[] lineSettings = {
        FugueLineSettings.init(),
        FuguePreset.BASS_OCTAVE.settings(),
        FuguePreset.THIRD_TRIPLET.settings(),
        FuguePreset.TENTH_DOUBLE.settings()
    };
    private final FuguePreset[] linePresets = {
        FuguePreset.INIT,
        FuguePreset.BASS_OCTAVE,
        FuguePreset.THIRD_TRIPLET,
        FuguePreset.TENTH_DOUBLE
    };
    private final boolean[] lineEnabled = {true, true, true, true};

    private final FugueEncoderControls encoderControls = new FugueEncoderControls();
    private final NoteVariationAmounts noteVariationAmounts;
    private final ObservedNoteVariationAdapter noteVariationAdapter =
            new ObservedNoteVariationAdapter(STEP_COUNT);
    private final Set<Integer> activeVariationTouches = new HashSet<>();
    private int loopSteps = DEFAULT_LOOP_STEPS;
    private boolean mainEncoderPressConsumed = false;

    public FugueStepMode(final AkaiFireOikontrolExtension driver) {
        super(driver.getLayers(), "FUGUE_STEP_MODE");
        this.driver = driver;
        this.oled = driver.getOled();
        this.patternButtons = driver.getPatternButtons();
        this.noteVariationAmounts = driver.getNoteVariationAmounts();

        final ControllerHost host = driver.getHost();
        final CursorTrack cursorTrack =
                host.createCursorTrack("FUGUE_STEP", "Fugue Step", 8, 16, true);
        cursorTrack.name().markInterested();
        cursorTrack.canHoldNoteData().markInterested();
        this.cursorClip =
                cursorTrack.createLauncherCursorClip(
                        "FUGUE_STEP_CLIP", "FUGUE_STEP_CLIP", STEP_COUNT, 128);
        this.cursorClip.setStepSize(STEP_LENGTH);
        this.cursorClip.scrollToKey(0);
        this.cursorClip.scrollToStep(0);
        this.cursorClip.getLoopLength().markInterested();
        this.cursorClip.getPlayStart().markInterested();
        this.cursorClip
                .getLoopLength()
                .addValueObserver(
                        length ->
                                loopSteps =
                                        Math.max(
                                                1,
                                                Math.min(
                                                        STEP_COUNT,
                                                        (int) Math.round(length / STEP_LENGTH))));
        this.cursorClip.addNoteStepObserver(
                noteStep -> {
                    noteVariationAdapter.handleObservedNote(noteStep);
                    handleNoteStepObject(noteStep);
                });
        this.cursorClip.playingStep().addValueObserver(observations::updatePlayingStep);
        this.templatePads =
                new FugueTemplatePadController(
                        new TemplatePadClipPort(),
                        STEP_LENGTH,
                        this::defaultTemplatePitch,
                        (pitch, degrees) ->
                                ScaleAwareTransposer.transposeByScaleDegrees(
                                        pitch,
                                        degrees,
                                        driver.getSharedMusicalScale(),
                                        driver.getSharedRootNote()));

        new FugueControlBindings(driver, this, new ControlPort());
        bindLineStatusLights();
        encoderControls.bind(driver, this, new EncoderHandler());
        bindMainEncoder();
    }

    public BiColorLightState getModeButtonLightState() {
        return driver.isGlobalAltHeld() ? driver.getStepFillLightState() : ModeButtonLights.MODE_3;
    }

    public void notifyBlink(final int blinkTicks) {}

    @Override
    protected void onActivate() {
        observations.setActive(true);
        refreshSourceCacheFromClip();
        patternButtons.setUpCallback(
                pressed -> {
                    if (pressed) {
                        cyclePreset(activeLineIndex(), 1);
                    }
                },
                () -> BiColorLightState.GREEN_HALF);
        patternButtons.setDownCallback(
                pressed -> {
                    if (pressed) {
                        regenerateAllDerivedLines();
                    }
                },
                () -> BiColorLightState.AMBER_HALF);
        applyEncoderFooterLegend();
        showEncoderModeInfo();
        oled.clearScreenDelayed();
    }

    @Override
    protected void onDeactivate() {
        observations.setActive(false);
        patternButtons.setUpCallback(pressed -> {}, () -> BiColorLightState.OFF);
        patternButtons.setDownCallback(pressed -> {}, () -> BiColorLightState.OFF);
        oled.setFooterLegend(null);
    }

    private void handleBankButton(final boolean pressed, final int amount) {
        if (!pressed) {
            return;
        }
        if (driver.isGlobalAltHeld()) {
            if (amount < 0) {
                halveClipLength();
            } else {
                doubleClipLength();
            }
            return;
        }
        adjustStartOffset(activeLineIndex(), amount);
    }

    private void bindMainEncoder() {
        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.bindEncoder(this, this::handleMainEncoder);
        mainEncoder.bindTouched(this, this::handleMainEncoderPress);
    }

    private void handleMainEncoder(final int inc) {
        if (driver.isPopupBrowserActive()) {
            driver.routeBrowserMainEncoder(inc);
            return;
        }
        if (inc == 0) {
            return;
        }
        if (templatePads.isEditing()) {
            templatePads.transpose(inc);
            showTemplatePadEditEncoderValue(3);
            return;
        }
        driver.markMainEncoderTurned();
        if (driver.handleMainEncoderGlobalChord(inc)) {
            return;
        }
        final boolean fine = driver.isGlobalShiftHeld();
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (driver.routeGlobalMainEncoderRole(inc, fine)) {
            return;
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(
                mainEncoderRole)) {
            oled.valueInfo("Note Repeat", "Fugue only");
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_DRUM_GRID_ROLE.equals(mainEncoderRole)) {
            oled.valueInfo("Drum Grid", "Drum only");
        } else {
            driver.adjustMainCursorParameter(inc, fine);
        }
    }

    private void handleMainEncoderPress(final boolean pressed) {
        if (driver.isPopupBrowserActive()) {
            driver.routeBrowserMainEncoderPress(pressed);
            return;
        }
        if (templatePads.isEditing()) {
            if (pressed) {
                oled.valueInfo("Template Pitch", templatePads.pitchLabel());
            } else {
                oled.clearScreenDelayed();
            }
            return;
        }
        driver.setMainEncoderPressed(pressed);
        if (pressed && driver.isGlobalAltHeld()) {
            mainEncoderPressConsumed = true;
            driver.toggleCurrentDeviceWindow();
            return;
        }
        if (pressed && driver.isGlobalShiftHeld()) {
            mainEncoderPressConsumed = true;
            driver.cycleMainEncoderRolePreference();
            return;
        }
        if (!pressed && mainEncoderPressConsumed) {
            mainEncoderPressConsumed = false;
            return;
        }
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (!pressed && !driver.wasMainEncoderTurnedWhilePressed()) {
            driver.toggleMainEncoderRolePreference();
            return;
        }
        if (AkaiFireOikontrolExtension.MAIN_ENCODER_TEMPO_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showTempoInfo();
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_SHUFFLE_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showGrooveShuffleInfo();
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TRACK_SELECT_ROLE.equals(
                mainEncoderRole)) {
            if (pressed) {
                driver.showSelectedTrackInfo(false);
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_PLAYBACK_START_ROLE.equals(
                mainEncoderRole)) {
            if (pressed) {
                oled.valueInfo("Play Start", "Grid step");
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(
                mainEncoderRole)) {
            if (pressed) {
                oled.valueInfo("Note Repeat", "Fugue only");
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_DRUM_GRID_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                oled.valueInfo("Drum Grid", "Drum only");
            } else {
                oled.clearScreenDelayed();
            }
        } else if (pressed) {
            driver.showMainCursorParameterInfo();
        }
    }

    private void showEncoderTouchValue(final int encoderIndex, final boolean touched) {
        if (!touched) {
            oled.clearScreenDelayed();
            return;
        }
        if (templatePads.isEditing()) {
            if (handleTemplatePadEditReset(encoderIndex)) {
                return;
            }
            showTemplatePadEditEncoderValue(encoderIndex);
            return;
        }
        if (activeLineIndex() == FugueClipAdapter.SOURCE_CHANNEL) {
            if (handleSourceLineReset(encoderIndex)) {
                return;
            }
            showTemplateEncoderValue(encoderIndex);
            return;
        }
        if (handleDerivedLineReset(activeLineIndex(), encoderIndex)) {
            return;
        }
        showDerivedLineEncoderValue(activeLineIndex(), encoderIndex);
    }

    private boolean handleTemplatePadEditReset(final int encoderIndex) {
        return switch (encoderIndex) {
            case 0 ->
                    driver.handleKnobModeEncoderReset(
                            true,
                            true,
                            "Template Vel",
                            "No reset",
                            templatePads::resetVelocity,
                            () -> showTemplatePadEditEncoderValue(encoderIndex));
            case 1 ->
                    driver.handleKnobModeEncoderReset(
                            true,
                            true,
                            "Template Chc",
                            "No reset",
                            templatePads::resetChance,
                            () -> showTemplatePadEditEncoderValue(encoderIndex));
            case 2 ->
                    driver.handleKnobModeEncoderReset(
                            true,
                            true,
                            "Template Gate",
                            "No reset",
                            templatePads::resetGate,
                            () -> showTemplatePadEditEncoderValue(encoderIndex));
            case 3 ->
                    driver.handleKnobModeEncoderReset(
                            true,
                            true,
                            "Template Pitch",
                            "No reset",
                            templatePads::resetPitch,
                            () -> showTemplatePadEditEncoderValue(encoderIndex));
            default -> false;
        };
    }

    private boolean handleSourceLineReset(final int encoderIndex) {
        if (driver.isGlobalAltHeld()) {
            return switch (encoderIndex) {
                case 0 ->
                        driver.handleKnobModeEncoderReset(
                                true,
                                true,
                                "Line 1 Vel",
                                "No reset",
                                () -> {
                                    lineSettings[FugueClipAdapter.SOURCE_CHANNEL] =
                                            lineSettings[FugueClipAdapter.SOURCE_CHANNEL]
                                                    .withVelocityOffset(0);
                                    refreshSourceCacheFromClip();
                                },
                                () -> showTemplateEncoderValue(encoderIndex));
                case 1 ->
                        driver.handleKnobModeEncoderReset(
                                true,
                                true,
                                "Line 1 Chc",
                                "No reset",
                                () -> {
                                    lineSettings[FugueClipAdapter.SOURCE_CHANNEL] =
                                            lineSettings[FugueClipAdapter.SOURCE_CHANNEL]
                                                    .withChancePercent(100);
                                    applySourceChancePercent();
                                },
                                () -> showTemplateEncoderValue(encoderIndex));
                case 2 ->
                        driver.handleKnobModeEncoderReset(
                                true,
                                true,
                                "Line 1 Gate",
                                "No reset",
                                () -> {
                                    final int previousGatePercent =
                                            lineSettings[FugueClipAdapter.SOURCE_CHANNEL]
                                                    .gatePercent();
                                    lineSettings[FugueClipAdapter.SOURCE_CHANNEL] =
                                            lineSettings[FugueClipAdapter.SOURCE_CHANNEL]
                                                    .withGatePercent(100);
                                    applySourceGateScale(previousGatePercent);
                                },
                                () -> showTemplateEncoderValue(encoderIndex));
                case 3 ->
                        driver.handleKnobModeEncoderReset(
                                true,
                                true,
                                "Clip Start",
                                "No reset",
                                () -> setClipPlayStart(0),
                                () -> showTemplateEncoderValue(encoderIndex));
                default -> false;
            };
        }
        return switch (encoderIndex) {
            case 0 ->
                    driver.handleKnobModeEncoderReset(
                            true,
                            true,
                            "Root",
                            "No reset",
                            () -> driver.setSharedRootNote(0),
                            () -> showTemplateEncoderValue(encoderIndex));
            case 1 ->
                    driver.handleKnobModeEncoderReset(
                            true,
                            true,
                            "Scale",
                            "No reset",
                            () -> driver.setSharedScaleIndex(1),
                            () -> showTemplateEncoderValue(encoderIndex));
            case 2 ->
                    driver.handleKnobModeEncoderReset(
                            true,
                            true,
                            "Clip Length",
                            "No reset",
                            () -> setClipLoopSteps(DEFAULT_LOOP_STEPS),
                            () -> showTemplateEncoderValue(encoderIndex));
            case 3 ->
                    driver.handleKnobModeEncoderReset(
                            true,
                            true,
                            "Clip Start",
                            "No reset",
                            () -> setClipPlayStart(0),
                            () -> showTemplateEncoderValue(encoderIndex));
            default -> false;
        };
    }

    private boolean handleDerivedLineReset(final int line, final int encoderIndex) {
        if (driver.isGlobalAltHeld()) {
            return switch (encoderIndex) {
                case 0 ->
                        resetDerivedLineSetting(
                                line,
                                encoderIndex,
                                "Velocity",
                                lineSettings[line].withVelocityOffset(0));
                case 1 ->
                        resetDerivedLineSetting(
                                line,
                                encoderIndex,
                                "Chance",
                                lineSettings[line].withChancePercent(100));
                case 2 ->
                        resetDerivedLineSetting(
                                line,
                                encoderIndex,
                                "Gate",
                                lineSettings[line].withGatePercent(100));
                case 3 ->
                        resetDerivedLineSetting(
                                line,
                                encoderIndex,
                                "Interval",
                                lineSettings[line].withPitchDegreeOffset(
                                        linePresets[line].settings().pitchDegreeOffset()));
                default -> false;
            };
        }
        if (driver.isGlobalShiftHeld() && encoderIndex == 0) {
            return driver.handleKnobModeEncoderReset(
                    true,
                    true,
                    lineLabel(line) + " Preset",
                    "No reset",
                    () -> {
                        linePresets[line] = FuguePreset.INIT;
                        lineSettings[line] = FugueLineSettings.init();
                        regenerateDerivedLine(line, "Preset", linePresets[line].label());
                    },
                    () -> showDerivedLineEncoderValue(line, encoderIndex));
        }
        return switch (encoderIndex) {
            case 0 ->
                    resetDerivedLineSetting(
                            line,
                            encoderIndex,
                            "Direction",
                            lineSettings[line].withDirection(
                                    linePresets[line].settings().direction()));
            case 1 ->
                    resetDerivedLineSetting(
                            line,
                            encoderIndex,
                            "Tempo",
                            lineSettings[line].withSpeed(linePresets[line].settings().speed()));
            case 2 ->
                    resetDerivedLineSetting(
                            line, encoderIndex, "Start", lineSettings[line].withStartOffset(0));
            case 3 ->
                    resetDerivedLineSetting(
                            line,
                            encoderIndex,
                            "Interval",
                            lineSettings[line].withPitchDegreeOffset(
                                    linePresets[line].settings().pitchDegreeOffset()));
            default -> false;
        };
    }

    private boolean resetDerivedLineSetting(
            final int line,
            final int encoderIndex,
            final String title,
            final FugueLineSettings settings) {
        return driver.handleKnobModeEncoderReset(
                true,
                true,
                title,
                "No reset",
                () -> {
                    lineSettings[line] = settings;
                    regenerateDerivedLine(line, title, "Reset");
                },
                () -> showDerivedLineEncoderValue(line, encoderIndex));
    }

    private void showTemplatePadEditEncoderValue(final int encoderIndex) {
        switch (encoderIndex) {
            case 0 ->
                    oled.valueInfo(
                            "Template Velocity", Integer.toString(templatePads.edit().velocity()));
            case 1 ->
                    oled.valueInfo(
                            "Template Chance",
                            "%d%%"
                                    .formatted(
                                            (int)
                                                    Math.round(
                                                            templatePads.edit().chance() * 100.0)));
            case 2 ->
                    oled.valueInfo(
                            "Template Gate", "%.2f".formatted(templatePads.edit().duration()));
            case 3 -> oled.valueInfo("Template Pitch", templatePads.pitchLabel());
            default -> {}
        }
    }

    private void showTemplateEncoderValue(final int encoderIndex) {
        if (driver.isGlobalAltHeld()) {
            final FugueLineSettings settings = lineSettings[FugueClipAdapter.SOURCE_CHANNEL];
            switch (encoderIndex) {
                case 0 ->
                        oled.valueInfo(
                                "Line 1 Velocity", "%+d".formatted(settings.velocityOffset()));
                case 1 -> oled.valueInfo("Line 1 Chance", settings.chancePercent() + "%");
                case 2 -> oled.valueInfo("Line 1 Gate", settings.gatePercent() + "%");
                case 3 ->
                        oled.valueInfo(
                                "Clip Start", formatPlayStart(cursorClip.getPlayStart().get()));
                default -> {}
            }
            return;
        }
        switch (encoderIndex) {
            case 0 ->
                    oled.valueInfo(
                            "Template Root", NoteGridLayout.noteName(driver.getSharedRootNote()));
            case 1 -> oled.valueInfo("Template Scale", driver.getSharedScaleDisplayName());
            case 2 -> oled.valueInfo("Clip Length", formatSteps(currentLoopSteps()));
            case 3 ->
                    oled.valueInfo("Clip Start", formatPlayStart(cursorClip.getPlayStart().get()));
            default -> {}
        }
    }

    private void showDerivedLineEncoderValue(final int line, final int encoderIndex) {
        final FugueLineSettings settings = lineSettings[line];
        switch (encoderIndex) {
            case 0 -> {
                if (driver.isGlobalAltHeld()) {
                    oled.valueInfo(
                            lineLabel(line) + " Velocity",
                            "%+d".formatted(settings.velocityOffset()));
                } else if (driver.isGlobalShiftHeld()) {
                    oled.valueInfo(lineLabel(line) + " Preset", linePresets[line].label());
                } else {
                    oled.valueInfo(lineLabel(line) + " Direction", settings.direction().label());
                }
            }
            case 1 -> {
                if (driver.isGlobalAltHeld()) {
                    oled.valueInfo(lineLabel(line) + " Chance", settings.chancePercent() + "%");
                } else {
                    oled.valueInfo(lineLabel(line) + " Tempo", settings.speed().label());
                }
            }
            case 2 -> {
                if (driver.isGlobalAltHeld()) {
                    oled.valueInfo(lineLabel(line) + " Gate", settings.gatePercent() + "%");
                } else {
                    oled.valueInfo(
                            lineLabel(line) + " Start",
                            Integer.toString(settings.startOffset() + 1));
                }
            }
            case 3 ->
                    oled.valueInfo(
                            lineLabel(line) + " Interval",
                            FuguePitchIntervals.label(settings.pitchDegreeOffset()));
            default -> {}
        }
    }

    private void handlePadPress(final int padIndex, final boolean pressed) {
        final int line = padIndex / PAD_COLUMNS;
        final int column = padIndex % PAD_COLUMNS;
        if (line == FugueClipAdapter.SOURCE_CHANNEL) {
            handleTemplatePadPress(column, pressed);
            return;
        }
        if (!pressed) {
            return;
        }
        selectLine(line);
        setStartOffset(line, bucketStart(column));
    }

    private void handleTemplatePadPress(final int column, final boolean pressed) {
        if (pressed) {
            selectLine(FugueClipAdapter.SOURCE_CHANNEL);
            templatePads.press(column, loopSteps);
            oled.valueInfo("Template", templatePads.pitchLabel());
            return;
        }
        final String label = templatePads.pitchLabel();
        final FugueTemplatePadController.ReleaseResult result = templatePads.release(column);
        if (result == FugueTemplatePadController.ReleaseResult.NO_OP) {
            return;
        }
        regenerateAllEnabledDerivedLinesSilently();
        if (result == FugueTemplatePadController.ReleaseResult.REMOVED) {
            oled.valueInfo("Template", "Removed");
        } else if (result == FugueTemplatePadController.ReleaseResult.ADDED) {
            oled.valueInfo("Template", "Added " + label);
        }
        oled.clearScreenDelayed();
    }

    private int defaultTemplatePitch() {
        return driver.getSharedMusicalScale()
                .computeNote(driver.getSharedRootNote(), driver.getSharedOctave() + 1, 0);
    }

    private void cacheSourceNote(final NoteStep note) {
        observations.cacheSource(note);
    }

    private final class ControlPort implements FugueControlBindings.Port {
        @Override
        public void padPress(final int padIndex, final boolean pressed) {
            handlePadPress(padIndex, pressed);
        }

        @Override
        public RgbLightState padLight(final int padIndex) {
            return getPadLight(padIndex);
        }

        @Override
        public void bankButton(final boolean pressed, final int amount) {
            handleBankButton(pressed, amount);
        }

        @Override
        public void lineButton(final int index, final boolean pressed) {
            toggleLineEnabled(index, pressed);
        }

        @Override
        public BiColorLightState lineLight(final int index) {
            return FugueStepMode.this.lineLight(index);
        }

        @Override
        public void encoderModeButton(final boolean pressed) {
            handleEncoderModeButton(pressed);
        }

        @Override
        public BiColorLightState encoderModeLight() {
            return FugueStepMode.this.encoderModeLight();
        }
    }

    private final class EncoderHandler implements FugueEncoderControls.Handler {
        @Override
        public void turn(final int encoderIndex, final int increment) {
            if (handleNoteVariationTurn(encoderIndex, increment)) {
                return;
            }
            if (templatePads.isEditing()) {
                switch (encoderIndex) {
                    case 0 -> templatePads.adjustVelocity(increment);
                    case 1 -> templatePads.adjustChance(increment);
                    case 2 -> templatePads.adjustGate(increment);
                    case 3 -> templatePads.transpose(increment);
                    default -> {}
                }
                showTemplatePadEditEncoderValue(encoderIndex);
                return;
            }
            switch (encoderIndex) {
                case 0 -> adjustDirectionOrPreset(increment);
                case 1 -> adjustSpeed(activeLineIndex(), increment);
                case 2 -> adjustStartOffset(activeLineIndex(), increment);
                case 3 -> adjustPitch(increment);
                default -> {}
            }
        }

        @Override
        public void touch(final int encoderIndex, final boolean touched) {
            if (handleNoteVariationTouch(encoderIndex, touched)) {
                return;
            }
            showEncoderTouchValue(encoderIndex, touched);
        }
    }

    private boolean handleNoteVariationTurn(final int encoderIndex, final int increment) {
        if (driver.isKnobModeHeld()
                || NoteVariationGesture.turn(driver.isGlobalShiftHeld(), driver.isGlobalAltHeld())
                        != NoteVariationGesture.Action.ADJUST_AMOUNT) {
            return false;
        }
        final Optional<NoteVariationParameter> parameter = noteVariationParameter(encoderIndex);
        if (parameter.isEmpty()) {
            return false;
        }
        final double amount = noteVariationAmounts.adjust(parameter.get(), increment * 0.05);
        oled.valueInfo(
                parameter.get().displayName() + " Rand",
                "%d%%".formatted(Math.round(amount * 100.0)));
        return true;
    }

    private boolean handleNoteVariationTouch(final int encoderIndex, final boolean touched) {
        if (!touched && activeVariationTouches.remove(encoderIndex)) {
            oled.clearScreenDelayed();
            return true;
        }
        if (!touched
                || NoteVariationGesture.touch(
                                driver.isGlobalShiftHeld(),
                                driver.isGlobalAltHeld(),
                                driver.isKnobModeHeld())
                        != NoteVariationGesture.Action.APPLY) {
            return false;
        }
        final Optional<NoteVariationParameter> parameter = noteVariationParameter(encoderIndex);
        if (parameter.isEmpty()) {
            return false;
        }
        activeVariationTouches.add(encoderIndex);
        applyNoteVariation(parameter.get());
        return true;
    }

    static Optional<NoteVariationParameter> noteVariationParameter(final int encoderIndex) {
        return switch (encoderIndex) {
            case 0 -> Optional.of(NoteVariationParameter.VELOCITY);
            case 1 -> Optional.of(NoteVariationParameter.CHANCE);
            default -> Optional.empty();
        };
    }

    private void applyNoteVariation(final NoteVariationParameter parameter) {
        final ObservedNoteVariationAdapter.Result result =
                noteVariationAdapter.apply(
                        parameter,
                        noteVariationDefault(parameter),
                        noteVariationAmounts.amount(parameter),
                        ThreadLocalRandom.current().nextLong(),
                        loopSteps);
        switch (result.status()) {
            case APPLIED -> oled.valueInfo("Randomized", result.noteCount() + " notes");
            case RESET ->
                    oled.valueInfo(
                            "Reset " + parameter.displayName(), result.noteCount() + " notes");
            case EMPTY -> oled.valueInfo("No notes", "Active loop");
            case TOO_LARGE -> oled.valueInfo("Clip too large", "No changes");
        }
    }

    static double noteVariationDefault(final NoteVariationParameter parameter) {
        return switch (parameter) {
            case VELOCITY -> 96.0 / 127.0;
            case CHANCE -> 1.0;
            default -> throw new IllegalArgumentException("Unsupported Fugue variation parameter");
        };
    }

    private final class TemplatePadClipPort implements FugueTemplatePadController.ClipPort {
        @Override
        public java.util.Optional<FugueTemplatePadController.Note> findNote(
                final int start, final int end) {
            return observations
                    .steps()
                    .getOrDefault(FugueClipAdapter.SOURCE_CHANNEL, Map.of())
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getKey() >= start && entry.getKey() < end)
                    .flatMap(entry -> entry.getValue().values().stream())
                    .filter(note -> note.state() == NoteStep.State.NoteOn)
                    .map(
                            note ->
                                    new FugueTemplatePadController.Note(
                                            note.x(),
                                            note.y(),
                                            (int) Math.round(note.velocity() * 127.0),
                                            note.chance(),
                                            note.duration()))
                    .min(FugueTemplatePadController.NOTE_ORDER);
        }

        @Override
        public void clear(final int step, final int pitch) {
            cursorClip.clearStep(FugueClipAdapter.SOURCE_CHANNEL, step, pitch);
            removeCachedSourceNote(step, pitch);
        }

        @Override
        public void write(final FugueTemplatePadController.Edit edit) {
            cursorClip.setStep(
                    FugueClipAdapter.SOURCE_CHANNEL,
                    edit.step(),
                    edit.pitch(),
                    edit.velocity(),
                    edit.duration());
            final NoteStep note =
                    cursorClip.getStep(FugueClipAdapter.SOURCE_CHANNEL, edit.step(), edit.pitch());
            note.setChance(Math.min(0.999, edit.chance()));
            note.setIsChanceEnabled(edit.chance() < 0.999);
            cacheSourceNote(note);
        }
    }

    private void removeCachedSourceNote(final int step, final int pitch) {
        observations.removeSource(step, pitch);
    }

    private RgbLightState getPadLight(final int padIndex) {
        final int line = padIndex / PAD_COLUMNS;
        final int column = padIndex % PAD_COLUMNS;
        if (line < 0 || line >= LINE_COUNT) {
            return RgbLightState.OFF;
        }
        return FugueRenderer.padLight(
                linePattern(line),
                column,
                loopSteps,
                observations.playingStep(),
                shiftedClipStartColumn(),
                LINE_COLORS[line],
                line == activeLineIndex(),
                lineEnabled[line]);
    }

    private MelodicPattern.Step bucketStep(final FuguePattern pattern, final int column) {
        return FugueRenderer.bucketStep(pattern, column, loopSteps);
    }

    private int activeBucketCount() {
        return FugueRenderer.activeBucketCount(loopSteps);
    }

    private int bucketStart(final int column) {
        return FugueRenderer.bucketStart(column, loopSteps);
    }

    private int bucketEnd(final int column) {
        return FugueRenderer.bucketEnd(column, loopSteps);
    }

    private int playingBucket() {
        return FugueRenderer.playingBucket(observations.playingStep(), loopSteps);
    }

    private int shiftedClipStartColumn() {
        return StepPadLightHelper.nearestColumnForShiftedClipStart(
                cursorClip.getPlayStart().get(), currentLoopSteps() * STEP_LENGTH, PAD_COLUMNS);
    }

    private void setClipPlayStart(final int startStep) {
        final double playStart =
                Math.max(0.0, Math.min((loopSteps - 1) * STEP_LENGTH, startStep * STEP_LENGTH));
        cursorClip.getPlayStart().set(playStart);
        oled.valueInfo("Clip Start", formatPlayStart(playStart));
    }

    private String formatPlayStart(final double beatTime) {
        final int stepIndex = (int) Math.round(beatTime / STEP_LENGTH);
        final int quarterNote = (int) Math.floor(beatTime);
        final int bar = quarterNote / 4;
        final int beat = quarterNote % 4;
        final int sixteenth = stepIndex % 4;
        return "%d.%d.%d".formatted(bar + 1, beat + 1, sixteenth + 1);
    }

    private FuguePattern linePattern(final int line) {
        if (line == 0) {
            return sourcePattern();
        }
        if (!lineEnabled[line]) {
            return FuguePattern.empty(loopSteps);
        }
        return MelodicLineTransformer.transform(
                sourcePattern(),
                lineSettings[line],
                driver.getSharedMusicalScale(),
                driver.getSharedRootNote());
    }

    private FuguePattern sourcePattern() {
        return FugueClipAdapter.sourceFromChannelOne(observations.steps(), loopSteps, STEP_LENGTH);
    }

    private void handleEncoderModeButton(final boolean pressed) {
        if (pressed) {
            return;
        }
        if (driver.consumeKnobModeGesture()) {
            oled.clearScreenDelayed();
            return;
        }
        encoderControls.cycle();
        encoderModeChanged();
    }

    private void selectEncoderMode(final EncoderMode mode) {
        encoderControls.select(mode);
        encoderModeChanged();
    }

    private void encoderModeChanged() {
        applyEncoderFooterLegend();
        showEncoderModeInfo();
        oled.clearScreenDelayed();
    }

    private void showEncoderModeInfo() {
        oled.detailInfo(encoderControls.title(), encoderControls.details());
    }

    private void applyEncoderFooterLegend() {
        oled.setFooterLegend(encoderControls.footer());
    }

    private void selectLine(final int line) {
        encoderControls.selectLine(line);
        encoderModeChanged();
    }

    private int activeLineIndex() {
        return encoderControls.selectedLine();
    }

    private BiColorLightState encoderModeLight() {
        return encoderControls.light();
    }

    private BiColorLightState lineLight(final int line) {
        return FugueRenderer.lineLight(lineEnabled[line]);
    }

    private void bindLineStatusLights() {
        final MultiStateHardwareLight[] stateLights = driver.getStateLights();
        for (int index = 0; index < stateLights.length; index++) {
            final int lightIndex = index;
            bindLightState(() -> lineStatusLight(lightIndex), stateLights[lightIndex]);
        }
    }

    private BiColorLightState lineStatusLight(final int line) {
        return FugueRenderer.lineStatusLight(lineEnabled[line]);
    }

    private void toggleLineEnabled(final int line, final boolean pressed) {
        if (!pressed) {
            return;
        }
        lineEnabled[line] = !lineEnabled[line];
        if (line > 0) {
            regenerateLine(line);
        }
        oled.valueInfo(lineLabel(line), lineEnabled[line] ? "On" : "Muted");
        oled.clearScreenDelayed();
    }

    private void adjustDirectionOrPreset(final int inc) {
        if (activeLineIndex() == FugueClipAdapter.SOURCE_CHANNEL) {
            if (driver.isGlobalAltHeld()) {
                adjustVelocityOffset(FugueClipAdapter.SOURCE_CHANNEL, inc);
                applySourceVelocityDelta(inc * 4);
                return;
            }
            adjustTemplateRoot(inc);
            return;
        }
        if (driver.isGlobalAltHeld()) {
            adjustVelocityOffset(activeLineIndex(), inc);
            return;
        }
        if (driver.isGlobalShiftHeld()) {
            cyclePreset(activeLineIndex(), inc);
            return;
        }
        final int line = activeLineIndex();
        lineSettings[line] =
                lineSettings[line].withDirection(lineSettings[line].direction().next(inc));
        afterLineSettingsChanged(line, "Direction", lineSettings[line].direction().label());
    }

    private void adjustSpeed(final int line, final int inc) {
        if (line == FugueClipAdapter.SOURCE_CHANNEL) {
            if (driver.isGlobalAltHeld()) {
                adjustChance(FugueClipAdapter.SOURCE_CHANNEL, inc);
                applySourceChancePercent();
                return;
            }
            adjustTemplateScale(inc);
            return;
        }
        if (driver.isGlobalAltHeld()) {
            adjustChance(line, inc);
            return;
        }
        lineSettings[line] = lineSettings[line].withSpeed(lineSettings[line].speed().next(inc));
        afterLineSettingsChanged(line, "Tempo", lineSettings[line].speed().label());
    }

    private void adjustStartOffset(final int line, final int inc) {
        if (line == FugueClipAdapter.SOURCE_CHANNEL) {
            if (driver.isGlobalAltHeld()) {
                final int previousGatePercent =
                        lineSettings[FugueClipAdapter.SOURCE_CHANNEL].gatePercent();
                adjustGate(FugueClipAdapter.SOURCE_CHANNEL, inc);
                applySourceGateScale(previousGatePercent);
                return;
            }
            adjustTemplateClipLength(inc);
            return;
        }
        if (driver.isGlobalAltHeld()) {
            adjustGate(line, inc);
            return;
        }
        lineSettings[line] =
                lineSettings[line].withStartOffset(lineSettings[line].startOffset() + inc);
        afterLineSettingsChanged(
                line, "Start", Integer.toString(lineSettings[line].startOffset() + 1));
    }

    private void setStartOffset(final int line, final int startOffset) {
        lineSettings[line] = lineSettings[line].withStartOffset(startOffset);
        afterLineSettingsChanged(
                line, "Start", Integer.toString(lineSettings[line].startOffset() + 1));
    }

    private void adjustPitch(final int inc) {
        final int line = activeLineIndex();
        if (line == FugueClipAdapter.SOURCE_CHANNEL) {
            adjustTemplatePlayStart(inc);
            return;
        }
        if (driver.isGlobalAltHeld()) {
            adjustPitchOctaveJump(line, inc);
            return;
        }
        adjustPitchDegreeOffset(line, inc);
    }

    private void adjustPitchDegreeOffset(final int line, final int inc) {
        final int nextInterval =
                FuguePitchIntervals.nextDegreeInterval(lineSettings[line].pitchDegreeOffset(), inc);
        lineSettings[line] = lineSettings[line].withPitchDegreeOffset(nextInterval);
        afterLineSettingsChanged(line, "Interval", FuguePitchIntervals.label(nextInterval));
    }

    private void adjustPitchSemitoneOffset(final int line, final int inc) {
        lineSettings[line] =
                lineSettings[line].withPitchSemitoneOffset(
                        lineSettings[line].pitchSemitoneOffset() + inc);
        afterLineSettingsChanged(
                line, "Semitone", "%+d".formatted(lineSettings[line].pitchSemitoneOffset()));
    }

    private void adjustPitchOctaveJump(final int line, final int inc) {
        final int nextInterval =
                FuguePitchIntervals.octaveJump(lineSettings[line].pitchDegreeOffset(), inc);
        lineSettings[line] = lineSettings[line].withPitchDegreeOffset(nextInterval);
        afterLineSettingsChanged(line, "Interval", FuguePitchIntervals.label(nextInterval));
    }

    private void adjustVelocityOffset(final int line, final int inc) {
        lineSettings[line] =
                lineSettings[line].withVelocityOffset(
                        lineSettings[line].velocityOffset() + inc * 4);
        afterLineSettingsChanged(
                line, "Velocity", "%+d".formatted(lineSettings[line].velocityOffset()));
    }

    private void adjustChance(final int line, final int inc) {
        lineSettings[line] =
                lineSettings[line].withChancePercent(lineSettings[line].chancePercent() + inc * 5);
        afterLineSettingsChanged(line, "Chance", lineSettings[line].chancePercent() + "%");
    }

    private void adjustGate(final int line, final int inc) {
        lineSettings[line] =
                lineSettings[line].withGatePercent(lineSettings[line].gatePercent() + inc * 5);
        afterLineSettingsChanged(line, "Gate", lineSettings[line].gatePercent() + "%");
    }

    private void applySourceVelocityDelta(final int delta) {
        forEachSourceNote(
                note ->
                        note.setVelocity(
                                Math.max(
                                                1.0,
                                                Math.min(
                                                        127.0,
                                                        Math.round(note.velocity() * 127.0)
                                                                + delta))
                                        / 127.0));
    }

    private void applySourceChancePercent() {
        final double chance =
                Math.min(
                        0.999,
                        lineSettings[FugueClipAdapter.SOURCE_CHANNEL].chancePercent() / 100.0);
        forEachSourceNote(
                note -> {
                    note.setChance(chance);
                    note.setIsChanceEnabled(chance < 0.999);
                });
    }

    private void applySourceGateScale(final int previousGatePercent) {
        final int nextGatePercent = lineSettings[FugueClipAdapter.SOURCE_CHANNEL].gatePercent();
        final double factor = nextGatePercent / (double) Math.max(1, previousGatePercent);
        forEachSourceNote(
                note -> note.setDuration(Math.max(STEP_LENGTH * 0.02, note.duration() * factor)));
    }

    private void forEachSourceNote(final java.util.function.Consumer<NoteStep> action) {
        final Map<Integer, Map<Integer, NoteStep>> channelSteps =
                observations.steps().getOrDefault(FugueClipAdapter.SOURCE_CHANNEL, Map.of());
        for (final Map<Integer, NoteStep> notesAtStep : channelSteps.values()) {
            for (final NoteStep note : notesAtStep.values()) {
                if (note.state() == NoteStep.State.NoteOn) {
                    action.accept(note);
                }
            }
        }
    }

    private void cyclePreset(final int line, final int inc) {
        if (line == FugueClipAdapter.SOURCE_CHANNEL) {
            showTemplateInfo();
            return;
        }
        linePresets[line] = linePresets[line].next(inc);
        lineSettings[line] = linePresets[line].settings();
        afterLineSettingsChanged(line, "Preset", linePresets[line].label());
    }

    private void afterLineSettingsChanged(final int line, final String title, final String value) {
        if (line > 0) {
            regenerateLine(line);
        }
        oled.valueInfo(lineLabel(line) + " " + title, value);
        oled.clearScreenDelayed();
    }

    private void regenerateDerivedLine(final int line, final String title, final String value) {
        if (line > 0) {
            regenerateLine(line);
        }
        oled.valueInfo(lineLabel(line) + " " + title, value);
        oled.clearScreenDelayed();
    }

    private void setClipLoopSteps(final int newLoopSteps) {
        cursorClip.getLoopLength().set(newLoopSteps * STEP_LENGTH);
        loopSteps = newLoopSteps;
        hostRebuildAfterClipLengthChange("Clip Length", formatSteps(newLoopSteps));
    }

    private void adjustTemplateRoot(final int inc) {
        driver.adjustSharedRootNote(inc);
        oled.valueInfo("Template Root", NoteGridLayout.noteName(driver.getSharedRootNote()));
        oled.clearScreenDelayed();
        regenerateAllEnabledDerivedLinesSilently();
    }

    private void adjustTemplateScale(final int inc) {
        driver.adjustSharedScaleIndex(inc, -1);
        oled.valueInfo("Template Scale", driver.getSharedScaleDisplayName());
        oled.clearScreenDelayed();
        regenerateAllEnabledDerivedLinesSilently();
    }

    private void adjustTemplateClipLength(final int inc) {
        final int currentLoopSteps = currentLoopSteps();
        final int nextLoopSteps = nextClipLengthSteps(currentLoopSteps, inc);
        if (nextLoopSteps == currentLoopSteps) {
            oled.valueInfo("Clip Length", nextLoopSteps == MAX_LOOP_STEPS ? "Max" : "Min");
            oled.clearScreenDelayed();
            return;
        }
        cursorClip.getLoopLength().set(nextLoopSteps * STEP_LENGTH);
        loopSteps = nextLoopSteps;
        hostRebuildAfterClipLengthChange("Clip Length", formatSteps(nextLoopSteps));
    }

    private int nextClipLengthSteps(final int currentLoopSteps, final int inc) {
        if (inc == 0) {
            return currentLoopSteps;
        }
        if (inc > 0) {
            for (final int candidate : CLIP_LENGTH_STEPS) {
                if (candidate > currentLoopSteps) {
                    return candidate;
                }
            }
            return CLIP_LENGTH_STEPS[CLIP_LENGTH_STEPS.length - 1];
        }
        for (int i = CLIP_LENGTH_STEPS.length - 1; i >= 0; i--) {
            final int candidate = CLIP_LENGTH_STEPS[i];
            if (candidate < currentLoopSteps) {
                return candidate;
            }
        }
        return CLIP_LENGTH_STEPS[0];
    }

    private void adjustTemplatePlayStart(final int inc) {
        final int currentStart = (int) Math.round(cursorClip.getPlayStart().get() / STEP_LENGTH);
        setClipPlayStart(Math.max(0, Math.min(loopSteps - 1, currentStart + inc)));
    }

    private void showTemplateInfo() {
        refreshSourceCacheFromClip();
        oled.detailInfo(
                "Template",
                "Root %s\nScale %s\nLen %s\nNotes %d"
                        .formatted(
                                NoteGridLayout.noteName(driver.getSharedRootNote()),
                                driver.getSharedScaleDisplayName(),
                                formatSteps(currentLoopSteps()),
                                sourceStepCount()));
        oled.clearScreenDelayed();
    }

    private void regenerateAllEnabledDerivedLinesSilently() {
        if (!hasSourceNotes()) {
            return;
        }
        for (int line = FugueClipAdapter.FIRST_DERIVED_CHANNEL;
                line <= FugueClipAdapter.LAST_DERIVED_CHANNEL;
                line++) {
            if (lineEnabled[line]) {
                regenerateLine(line);
            }
        }
    }

    private int sourceStepCount() {
        final FuguePattern source = sourcePattern();
        int count = 0;
        for (int i = 0; i < source.loopSteps(); i++) {
            for (final MelodicPattern.Step step : source.notesAt(i)) {
                if (step.active() && !step.tieFromPrevious()) {
                    count++;
                }
            }
        }
        return count;
    }

    private void regenerateAllDerivedLines() {
        refreshSourceCacheFromClip();
        if (!hasSourceNotes()) {
            oled.valueInfo("Fugue", "No Ch1 notes");
            oled.clearScreenDelayed();
            return;
        }
        for (int line = FugueClipAdapter.FIRST_DERIVED_CHANNEL;
                line <= FugueClipAdapter.LAST_DERIVED_CHANNEL;
                line++) {
            regenerateLine(line);
        }
        oled.valueInfo("Fugue", "Rebuilt lines");
        oled.clearScreenDelayed();
    }

    private void doubleClipLength() {
        final int currentLoopSteps = currentLoopSteps();
        if (currentLoopSteps >= MAX_LOOP_STEPS) {
            oled.valueInfo("Clip Length", "Max");
            oled.clearScreenDelayed();
            return;
        }
        final int newLoopSteps = Math.min(MAX_LOOP_STEPS, currentLoopSteps * 2);
        cursorClip.getLoopLength().set(newLoopSteps * STEP_LENGTH);
        FugueClipAdapter.duplicateChannelRange(
                cursorClip,
                observations.steps(),
                FugueClipAdapter.SOURCE_CHANNEL,
                0,
                currentLoopSteps,
                currentLoopSteps);
        loopSteps = newLoopSteps;
        hostRebuildAfterClipLengthChange("Length", formatSteps(newLoopSteps));
    }

    private void halveClipLength() {
        final int currentLoopSteps = currentLoopSteps();
        if (currentLoopSteps <= MIN_LOOP_STEPS) {
            oled.valueInfo("Clip Length", "Min");
            oled.clearScreenDelayed();
            return;
        }
        final int newLoopSteps = Math.max(MIN_LOOP_STEPS, currentLoopSteps / 2);
        cursorClip.getLoopLength().set(newLoopSteps * STEP_LENGTH);
        loopSteps = newLoopSteps;
        hostRebuildAfterClipLengthChange("Length", formatSteps(newLoopSteps));
    }

    private void hostRebuildAfterClipLengthChange(final String title, final String value) {
        driver.getHost()
                .scheduleTask(
                        () -> {
                            refreshSourceCacheFromClip();
                            regenerateAllEnabledDerivedLinesSilently();
                        },
                        SOURCE_CHANGE_REBUILD_DELAY_MS);
        oled.valueInfo(title, value);
        oled.clearScreenDelayed();
    }

    private int currentLoopSteps() {
        return Math.max(
                MIN_LOOP_STEPS,
                Math.min(
                        MAX_LOOP_STEPS,
                        (int) Math.round(cursorClip.getLoopLength().get() / STEP_LENGTH)));
    }

    private String formatSteps(final int steps) {
        final double beats = steps * STEP_LENGTH;
        final double bars = beats / 4.0;
        if (Math.rint(bars) == bars) {
            return "%d Bars".formatted((int) bars);
        }
        return "%d Steps".formatted(steps);
    }

    private void regenerateLine(final int line) {
        if (line <= FugueClipAdapter.SOURCE_CHANNEL || line >= LINE_COUNT) {
            return;
        }
        if (!lineEnabled[line]) {
            FugueClipAdapter.clearChannel(cursorClip, observations.steps(), line);
            return;
        }
        final FuguePattern source = sourcePattern();
        if (!hasNotes(source)) {
            return;
        }
        final FuguePattern transformed =
                MelodicLineTransformer.transform(
                        source,
                        lineSettings[line],
                        driver.getSharedMusicalScale(),
                        driver.getSharedRootNote());
        FugueClipAdapter.writeDerivedLine(
                cursorClip, observations.steps(), line, transformed, STEP_LENGTH);
    }

    private void handleNoteStepObject(final NoteStep noteStep) {
        observations.observe(noteStep);
    }

    private void refreshSourceCacheFromClip() {
        observations.refreshSource(
                (step, pitch) -> cursorClip.getStep(FugueClipAdapter.SOURCE_CHANNEL, step, pitch));
    }

    private boolean hasSourceNotes() {
        return hasNotes(sourcePattern());
    }

    private boolean hasNotes(final FuguePattern pattern) {
        for (int i = 0; i < pattern.loopSteps(); i++) {
            for (final MelodicPattern.Step step : pattern.notesAt(i)) {
                if (step.active() && !step.tieFromPrevious() && step.pitch() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private String lineLabel(final int line) {
        return line == FugueClipAdapter.SOURCE_CHANNEL ? "Template" : "Var " + (line + 1);
    }
}
