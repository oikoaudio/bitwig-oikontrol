package com.oikoaudio.fire.fugue;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.PadBankRowControlBindings;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.melodic.MelodicPattern;
import com.oikoaudio.fire.melodic.MelodicRenderer;
import com.oikoaudio.fire.note.NoteGridLayout;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.utils.PatternButtons;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class FugueStepMode extends Layer {
    private static final int STEP_COUNT = FuguePattern.MAX_STEPS;
    private static final int DEFAULT_LOOP_STEPS = 32;
    private static final double STEP_LENGTH = 0.125;
    private static final int LINE_COUNT = 4;
    private static final int PAD_COLUMNS = 16;
    private static final int ENCODER_THRESHOLD = 5;
    private static final int ENCODER_FINE_THRESHOLD = 10;
    private static final int SOURCE_CHANGE_REBUILD_DELAY_MS = 20;
    private static final int BAR_STEPS = 32;
    private static final int MAX_LOOP_STEPS = STEP_COUNT;
    private static final int MIN_LOOP_STEPS = 1;
    private static final int[] CLIP_LENGTH_STEPS = {
            8, 16, 32, 64, 96, 128, 160, 192, 224, 256,
            288, 320, 352, 384, 416, 448, 480, 512
    };
    private static final RgbLigthState[] LINE_COLORS = {
            new RgbLigthState(112, 0, 0, true),
            new RgbLigthState(112, 44, 0, true),
            new RgbLigthState(104, 88, 0, true),
            new RgbLigthState(0, 88, 32, true)
    };

    private final AkaiFireOikontrolExtension driver;
    private final OledDisplay oled;
    private final PatternButtons patternButtons;
    private final PinnableCursorClip cursorClip;
    private final Map<Integer, Map<Integer, Map<Integer, NoteStep>>> noteStepsByChannel = new HashMap<>();
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

    private EncoderMode activeEncoderMode = EncoderMode.CHANNEL;
    private int loopSteps = DEFAULT_LOOP_STEPS;
    private int playingStep = -1;
    private boolean mainEncoderPressConsumed = false;
    private boolean active = false;
    private TemplatePadEdit templatePadEdit = null;

    public FugueStepMode(final AkaiFireOikontrolExtension driver) {
        super(driver.getLayers(), "FUGUE_STEP_MODE");
        this.driver = driver;
        this.oled = driver.getOled();
        this.patternButtons = driver.getPatternButtons();

        final ControllerHost host = driver.getHost();
        final CursorTrack cursorTrack = host.createCursorTrack("FUGUE_STEP", "Fugue Step", 8, 16, true);
        cursorTrack.name().markInterested();
        cursorTrack.canHoldNoteData().markInterested();
        this.cursorClip = cursorTrack.createLauncherCursorClip("FUGUE_STEP_CLIP", "FUGUE_STEP_CLIP", STEP_COUNT, 128);
        this.cursorClip.setStepSize(STEP_LENGTH);
        this.cursorClip.scrollToKey(0);
        this.cursorClip.scrollToStep(0);
        this.cursorClip.getLoopLength().markInterested();
        this.cursorClip.getPlayStart().markInterested();
        this.cursorClip.getLoopLength().addValueObserver(length ->
                loopSteps = Math.max(1, Math.min(STEP_COUNT, (int) Math.round(length / STEP_LENGTH))));
        this.cursorClip.addNoteStepObserver(this::handleNoteStepObject);
        this.cursorClip.playingStep().addValueObserver(this::handlePlayingStep);

        new PadBankRowControlBindings(driver, this, fugueStepControlBindingsHost(),
                new PadBankRowControlBindings.ExtraButtonBinding(NoteAssign.KNOB_MODE,
                        this::handleEncoderModeButton, this::encoderModeLight)).bind();
        bindEncoders();
        bindMainEncoder();
    }

    public BiColorLightState getModeButtonLightState() {
        return driver.isGlobalAltHeld() ? driver.getStepFillLightState() : BiColorLightState.RED_FULL;
    }

    public void notifyBlink(final int blinkTicks) {
    }

    @Override
    protected void onActivate() {
        active = true;
        refreshSourceCacheFromClip();
        patternButtons.setUpCallback(pressed -> {
            if (pressed) {
                cyclePreset(activeLineIndex(), 1);
            }
        }, () -> BiColorLightState.GREEN_HALF);
        patternButtons.setDownCallback(pressed -> {
            if (pressed) {
                regenerateAllDerivedLines();
            }
        }, () -> BiColorLightState.AMBER_HALF);
        showEncoderModeInfo();
        oled.clearScreenDelayed();
    }

    @Override
    protected void onDeactivate() {
        active = false;
        patternButtons.setUpCallback(pressed -> { }, () -> BiColorLightState.OFF);
        patternButtons.setDownCallback(pressed -> { }, () -> BiColorLightState.OFF);
    }

    private PadBankRowControlBindings.Host fugueStepControlBindingsHost() {
        return new PadBankRowControlBindings.Host() {
            @Override
            public void handlePadPress(final int padIndex, final boolean pressed) {
                FugueStepMode.this.handlePadPress(padIndex, pressed);
            }

            @Override
            public RgbLigthState padLight(final int padIndex) {
                return FugueStepMode.this.getPadLight(padIndex);
            }

            @Override
            public void handleBankButton(final boolean pressed, final int amount) {
                FugueStepMode.this.handleBankButton(pressed, amount);
            }

            @Override
            public BiColorLightState bankLightState() {
                return BiColorLightState.HALF;
            }

            @Override
            public void handleRowButton(final int index, final boolean pressed) {
                toggleLineEnabled(index, pressed);
            }

            @Override
            public BiColorLightState rowLightState(final int index) {
                return FugueStepMode.this.lineLight(index);
            }
        };
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

    private void bindEncoders() {
        final TouchEncoder[] encoders = driver.getEncoders();
        encoders[0].bindThresholdedEncoder(this, ENCODER_THRESHOLD, ENCODER_FINE_THRESHOLD,
                driver::isGlobalShiftHeld, inc -> {
                    if (templatePadEdit != null) {
                        adjustTemplatePadVelocity(inc);
                    } else {
                        adjustDirectionOrPreset(inc);
                    }
                });
        encoders[0].bindTouched(this, touched -> showEncoderTouchValue(0, touched));
        encoders[1].bindThresholdedEncoder(this, ENCODER_THRESHOLD, ENCODER_FINE_THRESHOLD,
                driver::isGlobalShiftHeld, inc -> {
                    if (templatePadEdit != null) {
                        adjustTemplatePadChance(inc);
                    } else {
                        adjustSpeed(activeLineIndex(), inc);
                    }
                });
        encoders[1].bindTouched(this, touched -> showEncoderTouchValue(1, touched));
        encoders[2].bindThresholdedEncoder(this, ENCODER_THRESHOLD, ENCODER_FINE_THRESHOLD,
                driver::isGlobalShiftHeld, inc -> {
                    if (templatePadEdit != null) {
                        adjustTemplatePadGate(inc);
                    } else {
                        adjustStartOffset(activeLineIndex(), inc);
                    }
                });
        encoders[2].bindTouched(this, touched -> showEncoderTouchValue(2, touched));
        encoders[3].bindThresholdedEncoder(this, ENCODER_THRESHOLD, ENCODER_FINE_THRESHOLD,
                driver::isGlobalShiftHeld, inc -> {
                    if (templatePadEdit != null) {
                        transposeTemplatePadEdit(inc);
                    } else {
                        adjustPitch(inc);
                    }
                });
        encoders[3].bindTouched(this, touched -> showEncoderTouchValue(3, touched));
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
        if (templatePadEdit != null) {
            transposeTemplatePadEdit(inc);
            return;
        }
        driver.markMainEncoderTurned();
        if (driver.handleMainEncoderGlobalChord(inc)) {
            return;
        }
        final boolean fine = driver.isGlobalShiftHeld();
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (AkaiFireOikontrolExtension.MAIN_ENCODER_TEMPO_ROLE.equals(mainEncoderRole)) {
            driver.adjustTempo(inc, fine);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_SHUFFLE_ROLE.equals(mainEncoderRole)) {
            driver.adjustGrooveShuffleAmount(inc, fine);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TRACK_SELECT_ROLE.equals(mainEncoderRole)) {
            driver.adjustSelectedTrack(inc, driver.isMainEncoderPressed());
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
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
        if (templatePadEdit != null) {
            if (pressed) {
                oled.valueInfo("Template Pitch", currentTemplatePadEditLabel());
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
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TRACK_SELECT_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showSelectedTrackInfo(false);
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
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
        if (templatePadEdit != null) {
            showTemplatePadEditEncoderValue(encoderIndex);
            return;
        }
        if (activeLineIndex() == FugueClipAdapter.SOURCE_CHANNEL) {
            showTemplateEncoderValue(encoderIndex);
            return;
        }
        showDerivedLineEncoderValue(activeLineIndex(), encoderIndex);
    }

    private void showTemplatePadEditEncoderValue(final int encoderIndex) {
        switch (encoderIndex) {
            case 0 -> oled.valueInfo("Template Velocity", Integer.toString(templatePadVelocity()));
            case 1 -> oled.valueInfo("Template Chance", "%d%%".formatted((int) Math.round(templatePadChance() * 100.0)));
            case 2 -> oled.valueInfo("Template Gate", "%.2f".formatted(templatePadDuration()));
            case 3 -> oled.valueInfo("Template Pitch", currentTemplatePadEditLabel());
            default -> { }
        }
    }

    private void showTemplateEncoderValue(final int encoderIndex) {
        if (driver.isGlobalAltHeld()) {
            final FugueLineSettings settings = lineSettings[FugueClipAdapter.SOURCE_CHANNEL];
            switch (encoderIndex) {
                case 0 -> oled.valueInfo("Line 1 Velocity", "%+d".formatted(settings.velocityOffset()));
                case 1 -> oled.valueInfo("Line 1 Chance", settings.chancePercent() + "%");
                case 2 -> oled.valueInfo("Line 1 Gate", settings.gatePercent() + "%");
                case 3 -> oled.valueInfo("Clip Start", formatPlayStart(cursorClip.getPlayStart().get()));
                default -> { }
            }
            return;
        }
        switch (encoderIndex) {
            case 0 -> oled.valueInfo("Template Root", NoteGridLayout.noteName(driver.getSharedRootNote()));
            case 1 -> oled.valueInfo("Template Scale", driver.getSharedScaleDisplayName());
            case 2 -> oled.valueInfo("Clip Length", formatSteps(currentLoopSteps()));
            case 3 -> oled.valueInfo("Clip Start", formatPlayStart(cursorClip.getPlayStart().get()));
            default -> { }
        }
    }

    private void showDerivedLineEncoderValue(final int line, final int encoderIndex) {
        final FugueLineSettings settings = lineSettings[line];
        switch (encoderIndex) {
            case 0 -> {
                if (driver.isGlobalAltHeld()) {
                    oled.valueInfo(lineLabel(line) + " Velocity", "%+d".formatted(settings.velocityOffset()));
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
                    oled.valueInfo(lineLabel(line) + " Start", Integer.toString(settings.startOffset() + 1));
                }
            }
            case 3 -> oled.valueInfo(lineLabel(line) + " Interval",
                    FuguePitchIntervals.label(settings.pitchDegreeOffset()));
            default -> { }
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
            templatePadEdit = createTemplatePadEdit(column);
            oled.valueInfo("Template", currentTemplatePadEditLabel());
            return;
        }
        if (templatePadEdit == null || templatePadEdit.column != column) {
            return;
        }
        finishTemplatePadEdit();
    }

    private TemplatePadEdit createTemplatePadEdit(final int column) {
        final int start = bucketStart(column);
        final int end = bucketEnd(column);
        return findTemplateNoteInRange(start, end)
                .map(note -> new TemplatePadEdit(column, start, note.x(), note.y(), true, false,
                        (int) Math.round(note.velocity() * 127.0), note.chance(), note.duration()))
                .orElseGet(() -> new TemplatePadEdit(column, start, start, defaultTemplatePitch(), false, false,
                        96, 1.0, STEP_LENGTH * 2));
    }

    private java.util.Optional<NoteStep> findTemplateNoteInRange(final int start, final int end) {
        final Map<Integer, Map<Integer, NoteStep>> channelSteps =
                noteStepsByChannel.getOrDefault(FugueClipAdapter.SOURCE_CHANNEL, Map.of());
        return channelSteps.entrySet().stream()
                .filter(entry -> entry.getKey() >= start && entry.getKey() < end)
                .flatMap(entry -> entry.getValue().values().stream())
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .min(Comparator.comparingInt(NoteStep::y).thenComparingInt(NoteStep::x));
    }

    private int defaultTemplatePitch() {
        return driver.getSharedMusicalScale().computeNote(driver.getSharedRootNote(), driver.getSharedOctave() + 1, 0);
    }

    private void finishTemplatePadEdit() {
        final TemplatePadEdit edit = templatePadEdit;
        templatePadEdit = null;
        if (!edit.changed && edit.existed) {
            cursorClip.clearStep(FugueClipAdapter.SOURCE_CHANNEL, edit.step, edit.pitch);
            removeCachedSourceNote(edit.step, edit.pitch);
            regenerateAllEnabledDerivedLinesSilently();
            oled.valueInfo("Template", "Removed");
            oled.clearScreenDelayed();
            return;
        }
        if (!edit.changed) {
            writeTemplatePadEditNote(edit.withChanged(true));
            final String label = currentTemplatePadEditLabel();
            templatePadEdit = null;
            regenerateAllEnabledDerivedLinesSilently();
            oled.valueInfo("Template", "Added " + label);
            oled.clearScreenDelayed();
            return;
        }
        regenerateAllEnabledDerivedLinesSilently();
        oled.clearScreenDelayed();
    }

    private void transposeTemplatePadEdit(final int inc) {
        if (templatePadEdit == null) {
            return;
        }
        final TemplatePadEdit edit = templatePadEdit;
        final int nextPitch = ScaleAwareTransposer.transposeByScaleDegrees(edit.pitch, inc,
                driver.getSharedMusicalScale(), driver.getSharedRootNote());
        if (nextPitch == edit.pitch && edit.existed) {
            return;
        }
        if (edit.existed) {
            cursorClip.clearStep(FugueClipAdapter.SOURCE_CHANNEL, edit.step, edit.pitch);
            removeCachedSourceNote(edit.step, edit.pitch);
        }
        writeTemplatePadEditNote(edit.withPitch(nextPitch).withChanged(true));
        oled.valueInfo("Template Pitch", currentTemplatePadEditLabel());
    }

    private void adjustTemplatePadVelocity(final int inc) {
        if (templatePadEdit == null) {
            return;
        }
        writeTemplatePadEditNote(templatePadEdit.withVelocity(templatePadVelocity() + inc * 4).withChanged(true));
        oled.valueInfo("Template Velocity", Integer.toString(templatePadVelocity()));
    }

    private void adjustTemplatePadChance(final int inc) {
        if (templatePadEdit == null) {
            return;
        }
        writeTemplatePadEditNote(templatePadEdit.withChance(templatePadChance() + inc * 0.05).withChanged(true));
        oled.valueInfo("Template Chance", "%d%%".formatted((int) Math.round(templatePadChance() * 100.0)));
    }

    private void adjustTemplatePadGate(final int inc) {
        if (templatePadEdit == null) {
            return;
        }
        writeTemplatePadEditNote(templatePadEdit.withDuration(templatePadDuration() + inc * STEP_LENGTH).withChanged(true));
        oled.valueInfo("Template Gate", "%.2f".formatted(templatePadDuration()));
    }

    private void writeTemplatePadEditNote(final TemplatePadEdit nextEdit) {
        if (templatePadEdit != null && templatePadEdit.existed) {
            cursorClip.clearStep(FugueClipAdapter.SOURCE_CHANNEL, templatePadEdit.step, templatePadEdit.pitch);
            removeCachedSourceNote(templatePadEdit.step, templatePadEdit.pitch);
        }
        cursorClip.setStep(FugueClipAdapter.SOURCE_CHANNEL, nextEdit.step, nextEdit.pitch,
                nextEdit.velocity, nextEdit.duration);
        final NoteStep note = cursorClip.getStep(FugueClipAdapter.SOURCE_CHANNEL, nextEdit.step, nextEdit.pitch);
        note.setChance(Math.min(0.999, nextEdit.chance));
        note.setIsChanceEnabled(nextEdit.chance < 0.999);
        cacheSourceNote(note);
        templatePadEdit = nextEdit.withExisted(true);
    }

    private int templatePadVelocity() {
        return templatePadEdit == null ? 96 : templatePadEdit.velocity;
    }

    private double templatePadChance() {
        return templatePadEdit == null ? 1.0 : templatePadEdit.chance;
    }

    private double templatePadDuration() {
        return templatePadEdit == null ? STEP_LENGTH * 2 : templatePadEdit.duration;
    }

    private String currentTemplatePadEditLabel() {
        if (templatePadEdit == null) {
            return "";
        }
        return "%s%d".formatted(NoteGridLayout.noteName(templatePadEdit.pitch), templatePadEdit.pitch / 12 - 1);
    }

    private void cacheSourceNote(final NoteStep note) {
        if (note.state() != NoteStep.State.NoteOn) {
            return;
        }
        noteStepsByChannel.computeIfAbsent(FugueClipAdapter.SOURCE_CHANNEL, ignored -> new HashMap<>())
                .computeIfAbsent(note.x(), ignored -> new HashMap<>())
                .put(note.y(), note);
    }

    private void removeCachedSourceNote(final int step, final int pitch) {
        final Map<Integer, Map<Integer, NoteStep>> channelSteps = noteStepsByChannel.get(FugueClipAdapter.SOURCE_CHANNEL);
        if (channelSteps == null) {
            return;
        }
        final Map<Integer, NoteStep> notesAtStep = channelSteps.get(step);
        if (notesAtStep != null) {
            notesAtStep.remove(pitch);
            if (notesAtStep.isEmpty()) {
                channelSteps.remove(step);
            }
        }
        if (channelSteps.isEmpty()) {
            noteStepsByChannel.remove(FugueClipAdapter.SOURCE_CHANNEL);
        }
    }

    private RgbLigthState getPadLight(final int padIndex) {
        final int line = padIndex / PAD_COLUMNS;
        final int column = padIndex % PAD_COLUMNS;
        if (line < 0 || line >= LINE_COUNT) {
            return RgbLigthState.OFF;
        }
        final FuguePattern pattern = linePattern(line);
        final boolean inLoop = column < activeBucketCount();
        final boolean playing = playingBucket() == column;
        final RgbLigthState color = line == activeLineIndex() ? LINE_COLORS[line].getBrightend() : LINE_COLORS[line];
        final RgbLigthState rendered = MelodicRenderer.stepLight(bucketStep(pattern, column), false, inLoop, playing,
                column, color);
        return lineEnabled[line] ? rendered : rendered.getVeryDimmed();
    }

    private MelodicPattern.Step bucketStep(final FuguePattern pattern, final int column) {
        if (column >= activeBucketCount()) {
            return MelodicPattern.Step.rest(column);
        }
        final int start = bucketStart(column);
        final int end = bucketEnd(column);
        for (int step = start; step < end; step++) {
            final MelodicPattern.Step candidate = pattern.step(step);
            if (candidate.active() && !candidate.tieFromPrevious()) {
                return candidate.withIndex(column);
            }
        }
        return MelodicPattern.Step.rest(column);
    }

    private int activeBucketCount() {
        return Math.max(1, Math.min(PAD_COLUMNS, loopSteps));
    }

    private int bucketStart(final int column) {
        return column * loopSteps / PAD_COLUMNS;
    }

    private int bucketEnd(final int column) {
        return Math.max(bucketStart(column) + 1, (column + 1) * loopSteps / PAD_COLUMNS);
    }

    private int playingBucket() {
        if (playingStep < 0 || playingStep >= loopSteps) {
            return -1;
        }
        return Math.max(0, Math.min(PAD_COLUMNS - 1, playingStep * PAD_COLUMNS / loopSteps));
    }

    private void setClipPlayStart(final int startStep) {
        final double playStart = Math.max(0.0, Math.min((loopSteps - 1) * STEP_LENGTH, startStep * STEP_LENGTH));
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
        return MelodicLineTransformer.transform(sourcePattern(), lineSettings[line],
                driver.getSharedMusicalScale(), driver.getSharedRootNote());
    }

    private FuguePattern sourcePattern() {
        return FugueClipAdapter.sourceFromChannelOne(noteStepsByChannel, loopSteps, STEP_LENGTH);
    }

    private void handleEncoderModeButton(final boolean pressed) {
        if (!pressed) {
            oled.clearScreenDelayed();
            return;
        }
        selectEncoderMode(nextEncoderMode(activeEncoderMode));
    }

    private void selectEncoderMode(final EncoderMode mode) {
        activeEncoderMode = mode;
        showEncoderModeInfo();
        oled.clearScreenDelayed();
    }

    private void showEncoderModeInfo() {
        if (activeLineIndex() == FugueClipAdapter.SOURCE_CHANNEL) {
            oled.detailInfo("Fugue Settings", "1 Root\n2 Scale\n3 Clip Len\n4 Clip Start");
            return;
        }
        oled.detailInfo(lineLabel(activeLineIndex()), "1 Dir\n2 Tempo\n3 Start\n4 Pitch");
    }

    private void selectLine(final int line) {
        selectEncoderMode(switch (Math.max(0, Math.min(LINE_COUNT - 1, line))) {
            case 1 -> EncoderMode.MIXER;
            case 2 -> EncoderMode.USER_1;
            case 3 -> EncoderMode.USER_2;
            default -> EncoderMode.CHANNEL;
        });
    }

    private int activeLineIndex() {
        return switch (activeEncoderMode) {
            case CHANNEL -> 0;
            case MIXER -> 1;
            case USER_1 -> 2;
            case USER_2 -> 3;
        };
    }

    private EncoderMode nextEncoderMode(final EncoderMode mode) {
        return switch (mode) {
            case CHANNEL -> EncoderMode.MIXER;
            case MIXER -> EncoderMode.USER_1;
            case USER_1 -> EncoderMode.USER_2;
            case USER_2 -> EncoderMode.CHANNEL;
        };
    }

    private BiColorLightState encoderModeLight() {
        return activeEncoderMode.getState();
    }

    private BiColorLightState lineLight(final int line) {
        if (line == activeLineIndex()) {
            return lineEnabled[line] ? BiColorLightState.GREEN_FULL : BiColorLightState.RED_FULL;
        }
        return lineEnabled[line] ? BiColorLightState.GREEN_HALF : BiColorLightState.RED_HALF;
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
        lineSettings[line] = lineSettings[line].withDirection(lineSettings[line].direction().next(inc));
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
                final int previousGatePercent = lineSettings[FugueClipAdapter.SOURCE_CHANNEL].gatePercent();
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
        lineSettings[line] = lineSettings[line].withStartOffset(lineSettings[line].startOffset() + inc);
        afterLineSettingsChanged(line, "Start", Integer.toString(lineSettings[line].startOffset() + 1));
    }

    private void setStartOffset(final int line, final int startOffset) {
        lineSettings[line] = lineSettings[line].withStartOffset(startOffset);
        afterLineSettingsChanged(line, "Start", Integer.toString(lineSettings[line].startOffset() + 1));
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
        final int nextInterval = FuguePitchIntervals.nextDegreeInterval(lineSettings[line].pitchDegreeOffset(), inc);
        lineSettings[line] = lineSettings[line].withPitchDegreeOffset(nextInterval);
        afterLineSettingsChanged(line, "Interval", FuguePitchIntervals.label(nextInterval));
    }

    private void adjustPitchSemitoneOffset(final int line, final int inc) {
        lineSettings[line] = lineSettings[line].withPitchSemitoneOffset(lineSettings[line].pitchSemitoneOffset() + inc);
        afterLineSettingsChanged(line, "Semitone", "%+d".formatted(lineSettings[line].pitchSemitoneOffset()));
    }

    private void adjustPitchOctaveJump(final int line, final int inc) {
        final int nextInterval = FuguePitchIntervals.octaveJump(lineSettings[line].pitchDegreeOffset(), inc);
        lineSettings[line] = lineSettings[line].withPitchDegreeOffset(nextInterval);
        afterLineSettingsChanged(line, "Interval", FuguePitchIntervals.label(nextInterval));
    }

    private void adjustVelocityOffset(final int line, final int inc) {
        lineSettings[line] = lineSettings[line].withVelocityOffset(lineSettings[line].velocityOffset() + inc * 4);
        afterLineSettingsChanged(line, "Velocity", "%+d".formatted(lineSettings[line].velocityOffset()));
    }

    private void adjustChance(final int line, final int inc) {
        lineSettings[line] = lineSettings[line].withChancePercent(lineSettings[line].chancePercent() + inc * 5);
        afterLineSettingsChanged(line, "Chance", lineSettings[line].chancePercent() + "%");
    }

    private void adjustGate(final int line, final int inc) {
        lineSettings[line] = lineSettings[line].withGatePercent(lineSettings[line].gatePercent() + inc * 5);
        afterLineSettingsChanged(line, "Gate", lineSettings[line].gatePercent() + "%");
    }

    private void applySourceVelocityDelta(final int delta) {
        forEachSourceNote(note -> note.setVelocity(Math.max(1.0, Math.min(127.0,
                Math.round(note.velocity() * 127.0) + delta)) / 127.0));
    }

    private void applySourceChancePercent() {
        final double chance = Math.min(0.999,
                lineSettings[FugueClipAdapter.SOURCE_CHANNEL].chancePercent() / 100.0);
        forEachSourceNote(note -> {
            note.setChance(chance);
            note.setIsChanceEnabled(chance < 0.999);
        });
    }

    private void applySourceGateScale(final int previousGatePercent) {
        final int nextGatePercent = lineSettings[FugueClipAdapter.SOURCE_CHANNEL].gatePercent();
        final double factor = nextGatePercent / (double) Math.max(1, previousGatePercent);
        forEachSourceNote(note -> note.setDuration(Math.max(STEP_LENGTH * 0.02, note.duration() * factor)));
    }

    private void forEachSourceNote(final java.util.function.Consumer<NoteStep> action) {
        final Map<Integer, Map<Integer, NoteStep>> channelSteps =
                noteStepsByChannel.getOrDefault(FugueClipAdapter.SOURCE_CHANNEL, Map.of());
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
        oled.detailInfo("Template",
                "Root %s\nScale %s\nLen %s\nNotes %d".formatted(
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
        for (int line = FugueClipAdapter.FIRST_DERIVED_CHANNEL; line <= FugueClipAdapter.LAST_DERIVED_CHANNEL; line++) {
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
        for (int line = FugueClipAdapter.FIRST_DERIVED_CHANNEL; line <= FugueClipAdapter.LAST_DERIVED_CHANNEL; line++) {
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
        FugueClipAdapter.duplicateChannelRange(cursorClip, noteStepsByChannel, FugueClipAdapter.SOURCE_CHANNEL,
                0, currentLoopSteps, currentLoopSteps);
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
        driver.getHost().scheduleTask(() -> {
            refreshSourceCacheFromClip();
            regenerateAllEnabledDerivedLinesSilently();
        }, SOURCE_CHANGE_REBUILD_DELAY_MS);
        oled.valueInfo(title, value);
        oled.clearScreenDelayed();
    }

    private int currentLoopSteps() {
        return Math.max(MIN_LOOP_STEPS, Math.min(MAX_LOOP_STEPS,
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
            FugueClipAdapter.clearChannel(cursorClip, noteStepsByChannel, line);
            return;
        }
        final FuguePattern source = sourcePattern();
        if (!hasNotes(source)) {
            return;
        }
        final FuguePattern transformed = MelodicLineTransformer.transform(source, lineSettings[line],
                driver.getSharedMusicalScale(), driver.getSharedRootNote());
        FugueClipAdapter.writeDerivedLine(cursorClip, noteStepsByChannel, line, transformed, STEP_LENGTH);
    }

    private void handleNoteStepObject(final NoteStep noteStep) {
        if (!active) {
            return;
        }
        final int channel = noteStep.channel();
        final int x = noteStep.x();
        final int y = noteStep.y();
        final Map<Integer, Map<Integer, NoteStep>> channelSteps =
                noteStepsByChannel.computeIfAbsent(channel, ignored -> new HashMap<>());
        final Map<Integer, NoteStep> notesAtStep = channelSteps.computeIfAbsent(x, ignored -> new HashMap<>());
        if (noteStep.state() == NoteStep.State.Empty) {
            notesAtStep.remove(y);
            if (notesAtStep.isEmpty()) {
                channelSteps.remove(x);
            }
            if (channelSteps.isEmpty()) {
                noteStepsByChannel.remove(channel);
            }
            return;
        }
        notesAtStep.put(y, noteStep);
    }

    private void refreshSourceCacheFromClip() {
        noteStepsByChannel.clear();
        final Map<Integer, Map<Integer, NoteStep>> sourceSteps =
                noteStepsByChannel.computeIfAbsent(FugueClipAdapter.SOURCE_CHANNEL, ignored -> new HashMap<>());
        for (int x = 0; x < FuguePattern.MAX_STEPS; x++) {
            for (int y = 0; y < 128; y++) {
                final NoteStep step = cursorClip.getStep(FugueClipAdapter.SOURCE_CHANNEL, x, y);
                if (step.state() == NoteStep.State.NoteOn) {
                    sourceSteps.computeIfAbsent(x, ignored -> new HashMap<>()).put(y, step);
                }
            }
        }
        if (sourceSteps.isEmpty()) {
            noteStepsByChannel.remove(FugueClipAdapter.SOURCE_CHANNEL);
        }
    }

    private void handlePlayingStep(final int clipPlayingStep) {
        this.playingStep = clipPlayingStep >= 0 && clipPlayingStep < STEP_COUNT ? clipPlayingStep : -1;
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

    private record TemplatePadEdit(int column, int bucketStart, int step, int pitch, boolean existed, boolean changed,
                                   int velocity, double chance, double duration) {
        TemplatePadEdit withPitch(final int value) {
            return new TemplatePadEdit(column, bucketStart, step, value, existed, changed, velocity, chance, duration);
        }

        TemplatePadEdit withVelocity(final int value) {
            return new TemplatePadEdit(column, bucketStart, step, pitch, existed, changed,
                    Math.max(1, Math.min(127, value)), chance, duration);
        }

        TemplatePadEdit withChance(final double value) {
            return new TemplatePadEdit(column, bucketStart, step, pitch, existed, changed, velocity,
                    Math.max(0.0, Math.min(1.0, value)), duration);
        }

        TemplatePadEdit withDuration(final double value) {
            return new TemplatePadEdit(column, bucketStart, step, pitch, existed, changed, velocity, chance,
                    Math.max(STEP_LENGTH * 0.02, value));
        }

        TemplatePadEdit withExisted(final boolean value) {
            return new TemplatePadEdit(column, bucketStart, step, pitch, value, changed, velocity, chance, duration);
        }

        TemplatePadEdit withChanged(final boolean value) {
            return new TemplatePadEdit(column, bucketStart, step, pitch, existed, value, velocity, chance, duration);
        }
    }
}
