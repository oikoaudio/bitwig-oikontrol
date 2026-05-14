package com.oikoaudio.fire.nestedrhythm;

import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.EncoderStepAccumulator;
import com.oikoaudio.fire.control.EncoderValueProfile;
import com.oikoaudio.fire.control.MixerEncoderProfile;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.note.NoteGridLayout;
import com.oikoaudio.fire.sequence.ClipRowHandler;
import com.oikoaudio.fire.sequence.EncoderBank;
import com.oikoaudio.fire.sequence.EncoderBankLayout;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.sequence.EncoderSlotBinding;
import com.oikoaudio.fire.sequence.NoteClipAvailability;
import com.oikoaudio.fire.sequence.NoteClipCursorRefresher;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;
import com.oikoaudio.fire.sequence.SeqClipRowHost;
import com.oikoaudio.fire.sequence.StepPadLightHelper;
import com.oikoaudio.fire.sequence.StepSequencerEncoderHandler;
import com.oikoaudio.fire.sequence.StepSequencerHost;
import com.oikoaudio.fire.sequence.RecurrencePattern;
import com.oikoaudio.fire.utils.PatternButtons;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class NestedRhythmMode extends Layer implements StepSequencerHost, SeqClipRowHost {
    private static final int CLIP_ROW_PAD_COUNT = 16;
    private static final int STRUCTURE_PAD_OFFSET = 16;
    private static final int STRUCTURE_PAD_COUNT = 32;
    private static final int VELOCITY_PAD_OFFSET = 48;
    private static final int VELOCITY_PAD_COUNT = 16;
    private static final int MAX_BAR_COUNT = 4;
    private static final int CLIP_FINE_STEP_COUNT = NestedRhythmGenerator.maxFineStepsFor(MAX_BAR_COUNT, 16, 2);
    private static final double CLIP_STEP_SIZE = 1.0 / NestedRhythmGenerator.FINE_STEPS_PER_QUARTER;
    private static final int STEPPED_ENCODER_THRESHOLD = 5;
    private static final int STEPPED_ENCODER_FINE_THRESHOLD = 9;
    private static final RgbLigthState BASE_COLOR = new RgbLigthState(0, 90, 34, true);
    private static final int[] RATCHET_DIVISION_VALUES = {
            2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
    };
    private static final int[] CLIP_BAR_COUNT_VALUES = {1, 2, 4};
    private static final double[] RATE_VALUES = NestedRhythmGenerator.supportedRates();
    private static final double DENSITY_DISPLAY_MIN = 0.20;
    private static final double DENSITY_DISPLAY_STEP = 0.01;

    private final AkaiFireOikontrolExtension driver;
    private final OledDisplay oled;
    private final PatternButtons patternButtons;
    private final CursorTrack cursorTrack;
    private final PinnableCursorClip cursorClip;
    private final ClipLauncherSlotBank clipSlotBank;
    private final CursorRemoteControlsPage remoteControlsPage;
    private final ClipRowHandler clipHandler;
    private final StepSequencerEncoderHandler encoderLayer;
    private final EncoderBankLayout encoderBankLayout;
    private final NestedRhythmGenerator generator = new NestedRhythmGenerator();
    private final BooleanValueObject selectHeld = new BooleanValueObject();
    private final BooleanValueObject fixedLengthHeld = new BooleanValueObject();
    private final BooleanValueObject copyHeld = new BooleanValueObject();
    private final BooleanValueObject deleteHeld = new BooleanValueObject();
    private final BooleanValueObject lengthDisplay = new BooleanValueObject();
    private final Map<Integer, PendingPulseWrite> pendingPulseWrites = new java.util.HashMap<>();

    private final List<EditablePulse> editablePulses = new ArrayList<>();
    private boolean mainEncoderPressConsumed = false;
    private int selectedPulseIndex = -1;
    private int heldPulseIndex = -1;
    private int pressedHitIndex = -1;
    private boolean heldPulseConsumed = false;
    private int selectedClipSlotIndex = -1;
    private boolean selectedClipHasContent = false;
    private boolean clipLengthSyncSuppressed = false;
    private RgbLigthState selectedClipColor = BASE_COLOR;
    private int playingFineStep = -1;
    private double density = 1.0;
    private double rate = 1.0;
    private double cluster = 0.0;
    private double recurrenceDepth = 0.0;
    private int tupletDivisions = 3;
    private int tupletTargets = 0;
    private int tupletTargetPhase = 0;
    private int ratchetDivisions = 2;
    private int ratchetTargets = 0;
    private int ratchetTargetPhase = 0;
    private NestedRhythmGenerator.RatchetTargetMode ratchetTargetMode =
            NestedRhythmGenerator.RatchetTargetMode.DEFAULT;
    private NestedRhythmGenerator.DensityDirection densityDirection =
            NestedRhythmGenerator.DensityDirection.KEEP_STRONG;
    private double velocityDepth = NestedRhythmGenerator.DEFAULT_VELOCITY_DEPTH;
    private int velocityCenter = 100;
    private int velocityRotation = 0;
    private double pressureCenter = 0.0;
    private double pressureSpread = 0.0;
    private int pressureRotation = 0;
    private double timbreCenter = 0.0;
    private double timbreSpread = 0.0;
    private int timbreRotation = 0;
    private double pitchExpressionCenter = 0.0;
    private double pitchExpressionSpread = 0.0;
    private int pitchExpressionRotation = 0;
    private double chanceBaseline = 1.0;
    private double chancePlayProbability = 1.0;
    private int chanceRotation = 0;
    private int clipBarCount = 1;
    private int lastStepIndex = NestedRhythmLoopLength.STEP_COUNT - 1;

    public NestedRhythmMode(final AkaiFireOikontrolExtension driver) {
        super(driver.getLayers(), "NESTED_RYTM_MODE");
        this.driver = driver;
        this.oled = driver.getOled();
        this.patternButtons = driver.getPatternButtons();

        final ControllerHost host = driver.getHost();
        this.cursorTrack = host.createCursorTrack("NESTED_RHYTHM", "Nested Rhythm", 8, CLIP_ROW_PAD_COUNT, true);
        this.cursorTrack.name().markInterested();
        this.cursorTrack.canHoldNoteData().markInterested();
        this.clipSlotBank = cursorTrack.clipLauncherSlotBank();
        this.cursorClip = cursorTrack.createLauncherCursorClip("NESTED_RHYTHM_CLIP",
                "NESTED_RHYTHM_CLIP", CLIP_FINE_STEP_COUNT, 128);
        this.cursorClip.setStepSize(CLIP_STEP_SIZE);
        this.cursorClip.scrollToKey(0);
        this.cursorClip.scrollToStep(0);
        this.cursorClip.getLoopLength().markInterested();
        this.cursorClip.getPlayStart().markInterested();
        this.cursorClip.getLoopLength().addValueObserver(this::syncClipLengthFromBeats);
        this.cursorClip.addNoteStepObserver(this::handleNoteStepObject);
        this.cursorClip.playingStep().addValueObserver(this::handlePlayingStep);
        final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice("NESTED_RHYTHM_DEVICE",
                "Nested Rhythm Device", 8, CursorDeviceFollowMode.FOLLOW_SELECTION);
        this.remoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8);
        for (int i = 0; i < remoteControlsPage.getParameterCount(); i++) {
            final Parameter parameter = remoteControlsPage.getParameter(i);
            parameter.name().markInterested();
            parameter.displayedValue().markInterested();
            parameter.value().markInterested();
        }
        this.clipHandler = new ClipRowHandler(this);
        bindPads();
        bindButtons();
        bindMainEncoder();
        this.encoderBankLayout = createEncoderBankLayout();
        this.encoderLayer = new StepSequencerEncoderHandler(this, driver);
    }

    public void notifyBlink(final int blinkTicks) {
        clipHandler.notifyBlink(blinkTicks);
    }

    public void handleStepButton(final boolean pressed) {
        if (!pressed) {
            oled.clearScreenDelayed();
            return;
        }
        if (driver.isGlobalAltHeld()) {
            driver.toggleFillMode();
            oled.valueInfo("Fill", driver.isFillModeActive() ? "On" : "Off");
        }
    }

    public BiColorLightState getModeButtonLightState() {
        return driver.isGlobalAltHeld() ? driver.getStepFillLightState() : BiColorLightState.AMBER_FULL;
    }

    @Override
    protected void onActivate() {
        refreshClipCursor();
        refreshSelectedClipState();
        syncClipLengthFromDaw();
        patternButtons.setUpCallback(pressed -> {
            if (pressed) {
                clearPulseEdits();
            }
        }, () -> BiColorLightState.AMBER_HALF);
        patternButtons.setDownCallback(pressed -> {
            if (pressed) {
                generatePattern("Generate", summaryLabel());
            }
        }, () -> BiColorLightState.GREEN_HALF);
        encoderLayer.activate();
        maybeGenerateOnEmptySelectedClip();
    }

    @Override
    protected void onDeactivate() {
        patternButtons.setUpCallback(pressed -> { }, () -> BiColorLightState.OFF);
        patternButtons.setDownCallback(pressed -> { }, () -> BiColorLightState.OFF);
        selectHeld.set(false);
        fixedLengthHeld.set(false);
        copyHeld.set(false);
        deleteHeld.set(false);
        encoderLayer.deactivate();
    }

    private void bindPads() {
        final var pads = driver.getRgbButtons();
        for (int index = 0; index < pads.length; index++) {
            final int padIndex = index;
            pads[index].bindPressed(this, pressed -> handlePadPress(padIndex, pressed), () -> getPadLight(padIndex));
        }
    }

    private void bindButtons() {
        driver.getButton(NoteAssign.MUTE_1).bindPressed(this, pressed -> {
            selectHeld.set(pressed);
            if (pressed) {
                oled.valueInfo("Select", "Clip row");
            } else {
                oled.clearScreenDelayed();
            }
        }, () -> selectHeld.get() ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF);

        driver.getButton(NoteAssign.MUTE_2).bindPressed(this, pressed -> {
            fixedLengthHeld.set(pressed);
            if (pressed) {
                oled.valueInfo("Last Step", "Structure pad");
            } else {
                oled.clearScreenDelayed();
            }
        }, () -> fixedLengthHeld.get() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF);

        driver.getButton(NoteAssign.MUTE_3).bindPressed(this, pressed -> {
            copyHeld.set(pressed);
            if (pressed) {
                oled.valueInfo("Copy", "Clip row");
            } else {
                oled.clearScreenDelayed();
            }
        }, () -> copyHeld.get() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF);

        driver.getButton(NoteAssign.MUTE_4).bindPressed(this, pressed -> {
            if (pressed && driver.isGlobalAltHeld()) {
                clearPulseEdits();
                return;
            }
            deleteHeld.set(pressed);
            if (pressed) {
                oled.valueInfo("Delete", "Clip / hit");
            } else {
                oled.clearScreenDelayed();
            }
        }, () -> deleteHeld.get() ? BiColorLightState.RED_FULL : BiColorLightState.RED_HALF);

        final BiColorButton bankLeftButton = driver.getButton(NoteAssign.BANK_L);
        bankLeftButton.bindPressed(this, pressed -> handleBankButton(pressed, -1),
                () -> driver.isGlobalAltHeld() ? BiColorLightState.HALF : BiColorLightState.OFF);
        final BiColorButton bankRightButton = driver.getButton(NoteAssign.BANK_R);
        bankRightButton.bindPressed(this, pressed -> handleBankButton(pressed, 1),
                () -> driver.isGlobalAltHeld() ? BiColorLightState.HALF : BiColorLightState.OFF);
    }

    private void bindMainEncoder() {
        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.bindEncoder(this, this::handleMainEncoder);
        mainEncoder.bindTouched(this, this::handleMainEncoderPress);
    }

    private void handleBankButton(final boolean pressed, final int direction) {
        if (!pressed || !driver.isGlobalAltHeld()) {
            return;
        }
        adjustRelativeClipLength(direction);
    }

    private void handlePadPress(final int padIndex, final boolean pressed) {
        if (padIndex < CLIP_ROW_PAD_COUNT && hasHeldPulse()) {
            handleRecurrencePadPress(padIndex, pressed);
            return;
        }
        if (padIndex < CLIP_ROW_PAD_COUNT) {
            clipHandler.handlePadPress(padIndex, pressed);
            return;
        }
        if (fixedLengthHeld.get() && padIndex < VELOCITY_PAD_OFFSET) {
            if (pressed) {
                setLastStep(padIndex - STRUCTURE_PAD_OFFSET);
            }
            return;
        }
        if (padIndex < VELOCITY_PAD_OFFSET) {
            if (!pressed) {
                heldPulseIndex = -1;
            } else {
                holdPulseForStructurePad(padIndex - STRUCTURE_PAD_OFFSET);
            }
            return;
        }
        final int hitIndex = padIndex - VELOCITY_PAD_OFFSET;
        if (!pressed) {
            if (hitIndex == pressedHitIndex && !heldPulseConsumed && !driver.isGlobalShiftHeld()
                    && hitIndex >= 0 && hitIndex < editablePulses.size()) {
                final EditablePulse pulse = editablePulses.get(hitIndex);
                pulse.enabled = !pulse.enabled;
                applyEditablePattern("Hit", pulse.enabled ? "On" : "Off");
            }
            pressedHitIndex = -1;
            heldPulseConsumed = false;
            heldPulseIndex = -1;
            return;
        }
        if (hitIndex >= editablePulses.size()) {
            return;
        }
        heldPulseIndex = hitIndex;
        pressedHitIndex = hitIndex;
        heldPulseConsumed = false;
        showHeldPulse();
        if (driver.isGlobalShiftHeld()) {
            editablePulses.get(hitIndex).resetEdits();
            heldPulseConsumed = true;
            applyEditablePattern("Hit", "Reset");
        }
    }

    private void holdPulseForStructurePad(final int structurePad) {
        final int bin = Math.max(0, Math.min(STRUCTURE_PAD_COUNT - 1, structurePad));
        int bestIndex = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < editablePulses.size(); index++) {
            final EditablePulse pulse = editablePulses.get(index);
            final int pulseBin = structureBinFor(pulse.fineStart);
            final int distance = Math.abs(pulseBin - bin);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        if (bestIndex >= 0) {
            heldPulseIndex = bestIndex;
            showHeldPulse();
        }
    }

    private RgbLigthState getPadLight(final int padIndex) {
        if (padIndex < CLIP_ROW_PAD_COUNT && hasHeldPulse()) {
            return recurrencePadLight(padIndex);
        }
        if (padIndex < CLIP_ROW_PAD_COUNT) {
            return clipHandler.getPadLight(padIndex);
        }
        if (fixedLengthHeld.get() && padIndex < VELOCITY_PAD_OFFSET) {
            return lastStepPadLight(padIndex - STRUCTURE_PAD_OFFSET);
        }
        if (padIndex < VELOCITY_PAD_OFFSET) {
            return structurePadLight(padIndex - STRUCTURE_PAD_OFFSET);
        }
        return velocityPadLight(padIndex - VELOCITY_PAD_OFFSET);
    }

    private RgbLigthState lastStepPadLight(final int stepIndex) {
        if (stepIndex > lastStepIndex) {
            return RgbLigthState.OFF;
        }
        final RgbLigthState base = clipBaseColor();
        return stepIndex == lastStepIndex ? base.getBrightest() : base.getVeryDimmed();
    }

    private RgbLigthState structurePadLight(final int bin) {
        final EditablePulse pulse = strongestPulseInBin(bin);
        final int playingBin = playingFineStep < 0 ? -1 : structureBinFor(playingFineStep);
        if (pulse == null) {
            return StepPadLightHelper.renderEmptyStep(bin, playingBin);
        }
        final RgbLigthState base = pulse.enabled
                ? colorForVelocity(pulse.effectiveVelocity(), clipBaseColor())
                : disabledPulseColor();
        return playingBin == bin ? StepPadLightHelper.renderPlayheadHighlight(base) : base;
    }

    private RgbLigthState velocityPadLight(final int hitIndex) {
        if (hitIndex >= editablePulses.size()) {
            return RgbLigthState.OFF;
        }
        final int playingHitIndex = playingPulseIndex();
        final EditablePulse pulse = editablePulses.get(hitIndex);
        final RgbLigthState base = pulse.enabled
                ? colorForVelocity(pulse.effectiveVelocity(), clipBaseColor())
                : disabledPulseColor();
        return playingHitIndex == hitIndex ? StepPadLightHelper.renderPlayheadHighlight(base) : base;
    }

    private EditablePulse strongestPulseInBin(final int bin) {
        EditablePulse best = null;
        for (final EditablePulse pulse : editablePulses) {
            if (structureBinFor(pulse.fineStart) != bin) {
                continue;
            }
            if (best == null || pulse.effectiveVelocity() > best.effectiveVelocity()) {
                best = pulse;
            }
        }
        return best;
    }

    private int structureBinFor(final int fineStart) {
        final double normalized = fineStart / (double) Math.max(1, totalFineStepCount());
        return Math.max(0, Math.min(STRUCTURE_PAD_COUNT - 1,
                (int) Math.floor(normalized * STRUCTURE_PAD_COUNT)));
    }

    private int playingPulseIndex() {
        if (playingFineStep < 0) {
            return -1;
        }
        for (int index = 0; index < editablePulses.size(); index++) {
            final EditablePulse pulse = editablePulses.get(index);
            if (pulse.enabled && pulse.containsFineStep(playingFineStep, totalFineStepCount())) {
                return index;
            }
        }
        return -1;
    }

    private void showHeldPulse() {
        if (heldPulseIndex < 0 || heldPulseIndex >= editablePulses.size()) {
            oled.valueInfo("Hit", "None");
            return;
        }
        final EditablePulse pulse = editablePulses.get(heldPulseIndex);
        final RecurrencePattern recurrence = pulse.effectiveRecurrence();
        oled.valueInfo("Hit %d".formatted(heldPulseIndex + 1),
                "%s Vel %d Rec %s".formatted(pulse.roleLabel(), pulse.effectiveVelocity(), recurrence.summary()));
    }

    private void handleRecurrencePadPress(final int padIndex, final boolean pressed) {
        if (!pressed || padIndex >= RecurrencePattern.EDITOR_DEFAULT_SPAN || !hasHeldPulse()) {
            return;
        }
        final EditablePulse pulse = editablePulses.get(activePulseIndex());
        final RecurrencePattern updated = pulse.effectiveRecurrence().toggledAt(padIndex);
        pulse.recurrenceLength = updated.length();
        pulse.recurrenceMask = updated.mask();
        pulse.recurrenceEdited = true;
        heldPulseConsumed = true;
        applyEditablePattern("Recurrence", updated.summary());
    }

    private RgbLigthState recurrencePadLight(final int padIndex) {
        if (!hasHeldPulse()) {
            return clipHandler.getPadLight(padIndex);
        }
        if (padIndex >= RecurrencePattern.EDITOR_DEFAULT_SPAN) {
            return RgbLigthState.OFF;
        }
        final EditablePulse pulse = editablePulses.get(activePulseIndex());
        final RecurrencePattern recurrence = pulse.effectiveRecurrence();
        final int span = recurrence.effectiveSpan();
        if (padIndex >= span) {
            return RgbLigthState.OFF;
        }
        final int mask = recurrence.effectiveMask(span);
        return ((mask >> padIndex) & 0x1) == 1
                ? clipBaseColor().getBrightend()
                : clipBaseColor().getDimmed();
    }

    private void handlePlayingStep(final int fineStep) {
        this.playingFineStep = fineStep >= 0 ? fineStep : -1;
    }

    private void handleMainEncoder(final int inc) {
        if (inc == 0 || driver.isPopupBrowserActive()) {
            if (driver.isPopupBrowserActive()) {
                driver.routeBrowserMainEncoder(inc);
            }
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
            oled.valueInfo("Note Repeat", "Nested only");
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
                oled.valueInfo("Note Repeat", "Nested only");
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

    private void generatePattern(final String label, final String value) {
        if (!ensureClipAvailable()) {
            return;
        }
        generatePatternInCurrentState(label, value);
    }

    private void generatePatternInCurrentState(final String label, final String value) {
        final List<EditablePulse> previousPulses = List.copyOf(editablePulses);
        final int previousSelectionFineStart = heldPulseIndex >= 0 && heldPulseIndex < editablePulses.size()
                ? editablePulses.get(heldPulseIndex).fineStart
                : -1;
        final NestedRhythmPattern pattern = generator.generate(currentSettings());
        editablePulses.clear();
        for (final NestedRhythmPattern.PulseEvent event : pattern.events()) {
            final EditablePulse pulse = new EditablePulse(event);
            pulse.applyGeneratedRecurrence();
            editablePulses.add(restoreLocalEdits(pulse, previousPulses));
        }
        selectedPulseIndex = findSelectedPulseIndex(previousSelectionFineStart);
        heldPulseIndex = -1;
        writeEditablePulses();
        oled.valueInfo(label, value);
        driver.notifyPopup(label, value);
    }

    private void applyEditablePattern(final String label, final String value) {
        if (!ensureClipAvailable()) {
            return;
        }
        writeEditablePulses();
        oled.valueInfo(label, value);
        driver.notifyPopup(label, value);
    }

    private void writeEditablePulses() {
        refreshClipCursor();
        cursorClip.setStepSize(CLIP_STEP_SIZE);
        pendingPulseWrites.clear();
        cursorClip.clearSteps();
        cursorClip.getLoopLength().set(loopLengthBeats());
        final List<EditablePulse> active = editablePulses.stream()
                .filter(pulse -> pulse.enabled)
                .sorted(Comparator.comparingInt(pulse -> pulse.fineStart))
                .toList();
        for (final EditablePulse pulse : active) {
            pendingPulseWrites.put(pulse.fineStart, PendingPulseWrite.fromPulse(pulse));
            cursorClip.setStep(pulse.fineStart, pulse.midiNote,
                    pulse.effectiveVelocity(), pulse.effectiveBeatDuration());
        }
    }

    private void applyExpressionValues(final NoteStep step, final PendingPulseWrite pulse) {
        step.setPressure(pulse.pressure());
        step.setTimbre(pulse.timbre());
        step.setTranspose(pulse.pitchExpression());
        final double chance = Math.min(0.999, pulse.chance());
        step.setChance(chance);
        step.setIsChanceEnabled(chance < 0.999);
        final RecurrencePattern recurrence = pulse.recurrence();
        step.setRecurrence(recurrence.bitwigLength(), recurrence.bitwigMask());
    }

    private void handleNoteStepObject(final NoteStep noteStep) {
        if (noteStep.state() == NoteStep.State.Empty) {
            return;
        }
        final PendingPulseWrite pending = pendingPulseWrites.get(noteStep.x());
        if (pending == null || pending.midiNote() != noteStep.y()) {
            return;
        }
        applyExpressionValues(noteStep, pending);
        pendingPulseWrites.remove(noteStep.x());
    }

    private void clearPulseEdits() {
        for (final EditablePulse pulse : editablePulses) {
            pulse.resetEdits();
        }
        applyEditablePattern("Hits", "Reset");
    }

    private void refreshGeneratedRecurrenceDefaults() {
        for (final EditablePulse pulse : editablePulses) {
            if (!pulse.recurrenceEdited) {
                pulse.applyGeneratedRecurrence();
            }
        }
    }

    private EditablePulse restoreLocalEdits(final EditablePulse current, final List<EditablePulse> previousPulses) {
        final EditablePulse match = findOverlayMatch(current, previousPulses);
        if (match != null) {
            current.copyLocalEditsFrom(match);
        }
        return current;
    }

    private EditablePulse findOverlayMatch(final EditablePulse current, final List<EditablePulse> previousPulses) {
        EditablePulse best = null;
        int bestScore = Integer.MAX_VALUE;
        for (final EditablePulse previous : previousPulses) {
            final int score = matchScore(current, previous);
            if (score < bestScore) {
                bestScore = score;
                best = previous;
            }
        }
        return bestScore <= overlayMatchTolerance() ? best : null;
    }

    private int matchScore(final EditablePulse current, final EditablePulse previous) {
        if (current.midiNote != previous.midiNote || current.role != previous.role) {
            return Integer.MAX_VALUE;
        }
        final int fineStartDelta = Math.abs(current.fineStart - previous.fineStart);
        final int orderDelta = Math.abs(current.order - previous.order);
        return fineStartDelta + orderDelta * 16;
    }

    private int overlayMatchTolerance() {
        return Math.max(24, totalFineStepCount() / 64);
    }

    private int findSelectedPulseIndex(final int previousSelectionFineStart) {
        if (editablePulses.isEmpty()) {
            return -1;
        }
        if (previousSelectionFineStart < 0) {
            return Math.min(Math.max(selectedPulseIndex, 0), editablePulses.size() - 1);
        }
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < editablePulses.size(); index++) {
            final int distance = Math.abs(editablePulses.get(index).fineStart - previousSelectionFineStart);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private int activePulseIndex() {
        return heldPulseIndex >= 0 && heldPulseIndex < editablePulses.size() ? heldPulseIndex : -1;
    }

    private NestedRhythmGenerator.Settings currentSettings() {
        return new NestedRhythmGenerator.Settings(
                driver.getSharedBaseMidiNote(),
                density,
                tupletDivisions,
                tupletTargets,
                tupletTargetPhase,
                ratchetDivisions,
                ratchetTargets,
                ratchetTargetPhase,
                velocityDepth,
                velocityCenter,
                velocityRotation,
                0,
                cluster,
                meterNumerator(),
                meterDenominator(),
                clipBarCount,
                rate,
                ratchetTargetMode,
                densityDirection);
    }

    private RgbLigthState colorForVelocity(final int velocity, final RgbLigthState base) {
        if (velocity >= 112) {
            return base.getBrightest();
        }
        if (velocity >= 96) {
            return base.getBrightend();
        }
        if (velocity >= 78) {
            return base;
        }
        return base.getDimmed();
    }

    private RgbLigthState clipBaseColor() {
        return selectedClipColor != null ? selectedClipColor : BASE_COLOR;
    }

    private RgbLigthState disabledPulseColor() {
        return clipBaseColor().getVeryDimmed();
    }

    private String summaryLabel() {
        final String tuplet = tupletTargets == 0
                ? "No Tuplet"
                : "T%d x%d".formatted(tupletTargets, tupletDivisions);
        final String ratchet = ratchetTargets == 0
                ? "No Ratchet"
                : "R%d x%d".formatted(ratchetTargets, ratchetDivisions);
        return "%s / %s / %s x%d".formatted(tuplet, ratchet, meterLabel(), clipBarCount);
    }

    private EncoderBankLayout createEncoderBankLayout() {
        final Map<EncoderMode, EncoderBank> banks = new EnumMap<>(EncoderMode.class);
        banks.put(EncoderMode.CHANNEL, new EncoderBank(
                "1: Density / Alt Dens Dir / Shift Rec\n2: Tuplet / Alt Tup.Div / Shift Target Phase\n3: Ratchet / Alt Rat.Div / Shift Target Phase\n4: Cluster / Alt Play Start / Shift Rate",
                new EncoderSlotBinding[]{
                        modifierContinuousWithSteppedAltSlot(
                                view("Density", this::densityLabel, this::adjustDensity),
                                view("Dens Dir", this::densityDirectionLabel, this::adjustDensityDirection),
                                view("Recurrence", this::recurrenceDepthLabel, this::adjustRecurrenceDepth)),
                        modifierChoiceSlot(
                                view("Tuplet", this::tupletTargetLabel, this::adjustTupletTargets),
                                view("Tup.Div", this::tupletDivisionLabel, this::adjustTupletDivisions),
                                view("Target Phase", this::tupletTargetPhaseLabel, this::adjustTupletTargetPhase)),
                        modifierChoiceSlot(
                                view("Ratchet", this::ratchetTargetLabel, this::adjustRatchetTargets),
                                view("Rat.Div", this::ratchetDivisionLabel, this::adjustRatchetDivisions),
                                view("Target Phase", this::ratchetTargetPhaseLabel, this::adjustRatchetTargetPhase)),
                        modifierContinuousWithSteppedModifiersSlot(
                                view("Cluster", this::clusterLabel, this::adjustCluster),
                                view("Play Start", this::playStartLabel, this::adjustPlayStart),
                                view("Rate", this::rateLabel, this::adjustRate))
                }));
        banks.put(EncoderMode.MIXER, new EncoderBank(
                "1: Volume\n2: Pan\n3: Send 1\n4: Send 2",
                new EncoderSlotBinding[]{
                        mixerSlot(0, "Volume"),
                        mixerSlot(1, "Pan"),
                        mixerSlot(2, "Send 1"),
                        mixerSlot(3, "Send 2")
                }));
        banks.put(EncoderMode.USER_1, new EncoderBank(
                "1: Velocity\n2: Pressure\n3: Timbre\n4: Chance / Alt Base / Shift Rot",
                new EncoderSlotBinding[]{
                        modifierChoiceSlot(
                                view("Velocity", this::velocityPrimaryLabel, this::adjustVelocityPrimary),
                                view("Vel Center", this::velocityCenterLabel, this::adjustVelocityCenter),
                                view("Vel Rotate", () -> Integer.toString(velocityRotation), this::adjustVelocityRotation)),
                        modifierChoiceSlot(
                                view("Pressure", this::pressurePrimaryLabel, this::adjustPressurePrimary),
                                view("Prs Center", this::pressureCenterLabel, this::adjustPressureCenter),
                                view("Prs Rotate", this::pressureRotationLabel, this::adjustPressureRotation)),
                        modifierChoiceSlot(
                                view("Timbre", this::timbrePrimaryLabel, this::adjustTimbrePrimary),
                                view("Tmb Center", this::timbreCenterLabel, this::adjustTimbreCenter),
                                view("Tmb Rotate", this::timbreRotationLabel, this::adjustTimbreRotation)),
                        modifierChoiceSlot(
                                view("Chance", this::chancePrimaryLabel, this::adjustChancePrimary),
                                view("Chance Base", this::chanceBaselineLabel, this::adjustChanceBaseline),
                                view("Chance Rot", this::chanceRotationLabel, this::adjustChanceRotation))
                }));
        banks.put(EncoderMode.USER_2, new EncoderBank(
                "1: Pitch\n2: Pitch Expr\n3: Length / Alt Play Start / Shift Rate\n4: Reset Hits / Alt Rat Mode",
                new EncoderSlotBinding[]{
                        choiceSlot("Pitch", this::pitchLabel, this::adjustPitch),
                        modifierChoiceSlot(
                                view("Pitch Expr", this::pitchExpressionPrimaryLabel, this::adjustPitchExpressionPrimary),
                                view("Ptc Center", this::pitchExpressionCenterLabel, this::adjustPitchExpressionCenter),
                                view("Ptc Rotate", this::pitchExpressionRotationLabel, this::adjustPitchExpressionRotation)),
                        modifierChoiceSlot(
                                view("Length", this::clipLengthLabel, this::adjustClipBarCount),
                                view("Play Start", this::playStartLabel, this::adjustPlayStart),
                                view("Rate", this::rateLabel, this::adjustRate)),
                        modifierChoiceSlot(
                                view("Reset", () -> editablePulses.isEmpty() ? "No Hits" : "Ready",
                                        this::resetHitsFromEncoder),
                                view("Rat Mode", this::ratchetTargetModeLabel, this::adjustRatchetTargetMode),
                                null)
                }));
        return new EncoderBankLayout(banks);
    }

    private ModifierEncoderView view(final String label,
                                     final java.util.function.Supplier<String> valueSupplier,
                                     final java.util.function.IntConsumer adjuster) {
        return new ModifierEncoderView(label, valueSupplier, adjuster);
    }

    private EncoderSlotBinding modifierChoiceSlot(final ModifierEncoderView primary,
                                                  final ModifierEncoderView alt,
                                                  final ModifierEncoderView shift) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                final EncoderStepAccumulator normal = new EncoderStepAccumulator(STEPPED_ENCODER_THRESHOLD);
                final EncoderStepAccumulator altAccumulator = new EncoderStepAccumulator(STEPPED_ENCODER_THRESHOLD);
                final EncoderStepAccumulator shiftAccumulator = new EncoderStepAccumulator(STEPPED_ENCODER_THRESHOLD);
                encoder.bindEncoder(layer, inc -> {
                    final ModifierEncoderView currentView = activeView(primary, alt, shift);
                    final EncoderStepAccumulator accumulator = driver.isGlobalShiftHeld() && shift != null
                            ? shiftAccumulator
                            : driver.isGlobalAltHeld() && alt != null ? altAccumulator : normal;
                    final int steps = accumulator.consume(inc);
                    if (steps != 0) {
                        if (hasHeldPulse()) {
                            heldPulseConsumed = true;
                        }
                        currentView.adjuster().accept(steps);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        final ModifierEncoderView currentView = activeView(primary, alt, shift);
                        oled.valueInfo(currentView.label(), currentView.valueSupplier().get());
                    } else {
                        normal.reset();
                        altAccumulator.reset();
                        shiftAccumulator.reset();
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private EncoderSlotBinding modifierContinuousSlot(final ModifierEncoderView primary,
                                                      final ModifierEncoderView alt,
                                                      final ModifierEncoderView shift) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    if (inc != 0 && hasHeldPulse()) {
                        heldPulseConsumed = true;
                    }
                    activeView(primary, alt, shift).adjuster().accept(inc);
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        final ModifierEncoderView currentView = activeView(primary, alt, shift);
                        oled.valueInfo(currentView.label(), currentView.valueSupplier().get());
                    } else {
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private EncoderSlotBinding modifierContinuousWithSteppedAltSlot(final ModifierEncoderView primary,
                                                                    final ModifierEncoderView alt,
                                                                    final ModifierEncoderView shift) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                final EncoderStepAccumulator altAccumulator = new EncoderStepAccumulator(STEPPED_ENCODER_THRESHOLD);
                encoder.bindEncoder(layer, inc -> {
                    if (inc != 0 && hasHeldPulse()) {
                        heldPulseConsumed = true;
                    }
                    if (driver.isGlobalAltHeld() && alt != null) {
                        final int steps = altAccumulator.consume(inc);
                        if (steps != 0) {
                            alt.adjuster().accept(steps);
                        }
                        return;
                    }
                    activeView(primary, null, shift).adjuster().accept(inc);
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        final ModifierEncoderView currentView = activeView(primary, alt, shift);
                        oled.valueInfo(currentView.label(), currentView.valueSupplier().get());
                    } else {
                        altAccumulator.reset();
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private EncoderSlotBinding modifierContinuousWithSteppedModifiersSlot(final ModifierEncoderView primary,
                                                                          final ModifierEncoderView alt,
                                                                          final ModifierEncoderView shift) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                final EncoderStepAccumulator altAccumulator = new EncoderStepAccumulator(STEPPED_ENCODER_THRESHOLD);
                final EncoderStepAccumulator shiftAccumulator = new EncoderStepAccumulator(STEPPED_ENCODER_THRESHOLD);
                encoder.bindEncoder(layer, inc -> {
                    if (inc != 0 && hasHeldPulse()) {
                        heldPulseConsumed = true;
                    }
                    if (driver.isGlobalShiftHeld() && shift != null) {
                        final int steps = shiftAccumulator.consume(inc);
                        if (steps != 0) {
                            shift.adjuster().accept(steps);
                        }
                        return;
                    }
                    if (driver.isGlobalAltHeld() && alt != null) {
                        final int steps = altAccumulator.consume(inc);
                        if (steps != 0) {
                            alt.adjuster().accept(steps);
                        }
                        return;
                    }
                    primary.adjuster().accept(inc);
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        final ModifierEncoderView currentView = activeView(primary, alt, shift);
                        oled.valueInfo(currentView.label(), currentView.valueSupplier().get());
                    } else {
                        altAccumulator.reset();
                        shiftAccumulator.reset();
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private ModifierEncoderView activeView(final ModifierEncoderView primary,
                                           final ModifierEncoderView alt,
                                           final ModifierEncoderView shift) {
        if (driver.isGlobalShiftHeld() && shift != null) {
            return shift;
        }
        if (driver.isGlobalAltHeld() && alt != null) {
            return alt;
        }
        return primary;
    }

    private EncoderSlotBinding continuousSlot(final String label,
                                              final java.util.function.Supplier<String> valueSupplier,
                                              final java.util.function.IntConsumer adjuster) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    if (inc != 0 && hasHeldPulse()) {
                        heldPulseConsumed = true;
                    }
                    adjuster.accept(inc);
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        oled.valueInfo(label, valueSupplier.get());
                    } else {
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private EncoderSlotBinding countSlot(final String label,
                                         final java.util.function.Supplier<String> valueSupplier,
                                         final java.util.function.IntConsumer adjuster) {
        return choiceSlot(label, valueSupplier, adjuster);
    }

    private EncoderSlotBinding choiceSlot(final String label,
                                          final java.util.function.Supplier<String> valueSupplier,
                                          final java.util.function.IntConsumer adjuster) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindThresholdedEncoder(layer, STEPPED_ENCODER_THRESHOLD, STEPPED_ENCODER_FINE_THRESHOLD,
                        driver::isGlobalShiftHeld, steps -> {
                            if (steps != 0 && hasHeldPulse()) {
                                heldPulseConsumed = true;
                            }
                            adjuster.accept(steps);
                        });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        oled.valueInfo(label, valueSupplier.get());
                    } else {
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private void adjustDensity(final int amount) {
        if (amount == 0) {
            return;
        }
        final double displayedDensity = clampUnit(displayDensity() + amount * DENSITY_DISPLAY_STEP);
        density = internalDensityForDisplay(displayedDensity);
        generatePattern("Density", densityLabel());
    }

    private void adjustDensityDirection(final int amount) {
        if (amount == 0) {
            return;
        }
        densityDirection = densityDirection == NestedRhythmGenerator.DensityDirection.KEEP_STRONG
                ? NestedRhythmGenerator.DensityDirection.KEEP_WEAK
                : NestedRhythmGenerator.DensityDirection.KEEP_STRONG;
        generatePattern("Dens Dir", densityDirectionLabel());
    }

    private void adjustCluster(final int amount) {
        if (amount == 0) {
            return;
        }
        cluster = clampUnit(cluster + amount * 0.05);
        generatePattern("Cluster", clusterLabel());
    }

    private void adjustRecurrenceDepth(final int amount) {
        if (amount == 0) {
            return;
        }
        recurrenceDepth = clampUnit(recurrenceDepth + amount * 0.05);
        refreshGeneratedRecurrenceDefaults();
        applyEditablePattern("Recurrence", recurrenceDepthLabel());
    }

    private void adjustTupletTargets(final int amount) {
        tupletTargets = Math.max(0, Math.min(totalTupletHalfBars(), tupletTargets + amount));
        generatePattern("Tuplet", tupletTargetLabel());
    }

    private void adjustTupletDivisions(final int amount) {
        tupletDivisions = stepCount(normalizeTupletDivisions(tupletDivisions), amount, availableTupletDivisions());
        generatePattern("Tup.Div", tupletDivisionLabel());
    }

    private void adjustTupletTargetPhase(final int amount) {
        tupletTargetPhase = Math.floorMod(tupletTargetPhase + amount, totalTupletHalfBars());
        generatePattern("Target Phase", tupletTargetPhaseLabel());
    }

    private void adjustRatchetTargets(final int amount) {
        ratchetTargets = Math.max(0, Math.min(totalRatchetRegions(), ratchetTargets + amount));
        generatePattern("Ratchet", ratchetTargetLabel());
    }

    private void adjustRatchetDivisions(final int amount) {
        ratchetDivisions = stepCount(ratchetDivisions, amount, RATCHET_DIVISION_VALUES);
        generatePattern("Rat.Div", ratchetDivisionLabel());
    }

    private void adjustRatchetTargetPhase(final int amount) {
        final int totalRegions = totalRatchetRegions();
        if (totalRegions <= 0) {
            ratchetTargets = 0;
            ratchetTargetPhase = 0;
            generatePattern("Target Phase", ratchetTargetPhaseLabel());
            return;
        }
        ratchetTargetPhase = Math.floorMod(ratchetTargetPhase + amount, totalRegions);
        generatePattern("Target Phase", ratchetTargetPhaseLabel());
    }

    private void adjustRatchetTargetMode(final int amount) {
        if (amount == 0) {
            return;
        }
        ratchetTargetMode = ratchetTargetMode == NestedRhythmGenerator.RatchetTargetMode.DEFAULT
                ? NestedRhythmGenerator.RatchetTargetMode.BARLOW
                : NestedRhythmGenerator.RatchetTargetMode.DEFAULT;
        generatePattern("Rat Mode", ratchetTargetModeLabel());
    }

    private void adjustVelocityDepth(final int amount) {
        if (amount == 0) {
            return;
        }
        velocityDepth = Math.max(NestedRhythmGenerator.MIN_VELOCITY_DEPTH,
                Math.min(NestedRhythmGenerator.MAX_VELOCITY_DEPTH, velocityDepth + amount * 0.05));
        generatePattern("Vel Depth", velocityDepthLabel());
    }

    private void adjustVelocityPrimary(final int amount) {
        if (hasHeldPulse()) {
            adjustSelectedHitVelocity(amount);
            return;
        }
        adjustVelocityDepth(amount);
    }

    private void adjustVelocityCenter(final int amount) {
        if (amount == 0) {
            return;
        }
        velocityCenter = Math.max(1, Math.min(127, velocityCenter + amount));
        generatePattern("Vel Center", velocityCenterLabel());
    }

    private void adjustVelocityRotation(final int amount) {
        velocityRotation = Math.floorMod(velocityRotation + amount, NestedRhythmGenerator.contourLength());
        generatePattern("Vel Rotate", Integer.toString(velocityRotation));
    }

    private void adjustPressureCenter(final int amount) {
        if (amount == 0) {
            return;
        }
        pressureCenter = clampUnit(pressureCenter + amount * 0.05);
        applyEditablePattern("Pressure", pressureCenterLabel());
    }

    private void adjustPressurePrimary(final int amount) {
        if (hasHeldPulse()) {
            adjustSelectedHitPressure(amount);
            return;
        }
        adjustPressureSpread(amount);
    }

    private void adjustPressureSpread(final int amount) {
        if (amount == 0) {
            return;
        }
        pressureSpread = clampUnit(pressureSpread + amount * 0.05);
        applyEditablePattern("Prs Spread", pressureSpreadLabel());
    }

    private void adjustPressureRotation(final int amount) {
        pressureRotation = Math.floorMod(pressureRotation + amount, NestedRhythmGenerator.contourLength());
        applyEditablePattern("Prs Rotate", pressureRotationLabel());
    }

    private void adjustTimbreCenter(final int amount) {
        if (amount == 0) {
            return;
        }
        timbreCenter = clampSignedUnit(timbreCenter + amount * 0.05);
        applyEditablePattern("Timbre", timbreCenterLabel());
    }

    private void adjustTimbrePrimary(final int amount) {
        if (hasHeldPulse()) {
            adjustSelectedHitTimbre(amount);
            return;
        }
        adjustTimbreSpread(amount);
    }

    private void adjustTimbreSpread(final int amount) {
        if (amount == 0) {
            return;
        }
        timbreSpread = clampUnit(timbreSpread + amount * 0.05);
        applyEditablePattern("Tmb Spread", timbreSpreadLabel());
    }

    private void adjustTimbreRotation(final int amount) {
        timbreRotation = Math.floorMod(timbreRotation + amount, NestedRhythmGenerator.contourLength());
        applyEditablePattern("Tmb Rotate", timbreRotationLabel());
    }

    private void adjustPitchExpressionCenter(final int amount) {
        if (amount == 0) {
            return;
        }
        pitchExpressionCenter = clampPitchExpression(pitchExpressionCenter + amount);
        applyEditablePattern("Pitch Expr", pitchExpressionCenterLabel());
    }

    private void adjustPitchExpressionPrimary(final int amount) {
        if (hasHeldPulse()) {
            adjustSelectedHitPitchExpression(amount);
            return;
        }
        adjustPitchExpressionSpread(amount);
    }

    private void adjustPitchExpressionSpread(final int amount) {
        if (amount == 0) {
            return;
        }
        pitchExpressionSpread = clampPitchExpressionSpread(pitchExpressionSpread + amount);
        applyEditablePattern("Ptc Spread", pitchExpressionSpreadLabel());
    }

    private void adjustPitchExpressionRotation(final int amount) {
        pitchExpressionRotation = Math.floorMod(pitchExpressionRotation + amount, NestedRhythmGenerator.contourLength());
        applyEditablePattern("Ptc Rotate", pitchExpressionRotationLabel());
    }

    private void adjustClipBarCount(final int amount) {
        if (amount == 0 || !ensureClipAvailable()) {
            return;
        }
        final double currentLength = observedLoopLengthBeats();
        final double nextLength = NestedRhythmLoopLength.steppedBarLengthBeats(
                currentLength,
                NestedRhythmGenerator.beatsPerBar(meterNumerator(), meterDenominator()),
                CLIP_BAR_COUNT_VALUES,
                amount);
        if (Math.abs(nextLength - currentLength) <= 0.0001) {
            oled.valueInfo("Length", amount < 0 ? "Min" : "Max");
            driver.notifyPopup("Length", amount < 0 ? "Min" : "Max");
            return;
        }
        writeControllerLoopLength(nextLength, "Length");
    }

    private void adjustRate(final int amount) {
        if (amount == 0) {
            return;
        }
        rate = stepRate(rate, amount);
        normalizeTupletControls();
        normalizeRatchetControls();
        generatePattern("Rate", rateLabel());
    }

    private void adjustRelativeClipLength(final int direction) {
        if (direction == 0 || !ensureClipAvailable()) {
            return;
        }
        final double currentLength = observedLoopLengthBeats();
        final double nextLength = NestedRhythmLoopLength.relativeLengthBeats(
                currentLength, direction, 0.25, maxLoopLengthBeats());
        if (Math.abs(nextLength - currentLength) <= 0.0001) {
            oled.valueInfo("Clip Length", direction < 0 ? "Min" : "Max");
            driver.notifyPopup("Clip Length", direction < 0 ? "Min" : "Max");
            return;
        }
        writeControllerLoopLength(nextLength, "Clip Length");
    }

    private void writeControllerLoopLength(final double targetBeats, final String label) {
        clipLengthSyncSuppressed = true;
        try {
            applyClipLengthFromBeats(targetBeats);
            final double normalizedLength = loopLengthBeats();
            cursorClip.getLoopLength().set(normalizedLength);
            clampPlayStartToLoop();
            generatePatternInCurrentState(label, clipLengthLabel());
        } finally {
            clipLengthSyncSuppressed = false;
        }
    }

    private void adjustPlayStart(final int amount) {
        if (amount == 0 || !ensureClipAvailable()) {
            return;
        }
        refreshClipCursor();
        final double next = NestedRhythmPlayStart.increment(
                cursorClip.getPlayStart().get(), loopLengthBeats(), meterDenominator(), amount);
        cursorClip.getPlayStart().set(next);
        oled.valueInfo("Play Start", playStartLabel());
        driver.notifyPopup("Play Start", playStartLabel());
    }

    private void adjustChancePrimary(final int amount) {
        if (hasHeldPulse()) {
            adjustSelectedHitChance(amount);
            return;
        }
        adjustChancePlayProbability(amount);
    }

    private void adjustChancePlayProbability(final int amount) {
        if (amount == 0) {
            return;
        }
        chancePlayProbability = clampUnit(chancePlayProbability + amount * 0.05);
        applyEditablePattern("Chance", chancePrimaryLabel());
    }

    private void adjustChanceBaseline(final int amount) {
        if (amount == 0) {
            return;
        }
        chanceBaseline = clampUnit(chanceBaseline + amount * 0.05);
        applyEditablePattern("Chance Base", chanceBaselineLabel());
    }

    private void adjustChanceRotation(final int amount) {
        chanceRotation = Math.floorMod(chanceRotation + amount, NestedRhythmGenerator.contourLength());
        applyEditablePattern("Chance Rot", chanceRotationLabel());
    }

    private void adjustSelectedHitVelocity(final int amount) {
        if (!hasHeldPulse() || amount == 0) {
            return;
        }
        final EditablePulse pulse = editablePulses.get(activePulseIndex());
        pulse.velocityOffset = Math.max(-48, Math.min(48, pulse.velocityOffset + amount * 2));
        applyEditablePattern("Hit Vel", Integer.toString(pulse.effectiveVelocity()));
    }

    private void adjustSelectedHitPressure(final int amount) {
        if (!hasHeldPulse() || amount == 0) {
            return;
        }
        final EditablePulse pulse = editablePulses.get(activePulseIndex());
        pulse.pressureOffset = clampSignedUnit(pulse.pressureOffset + amount * 0.05);
        applyEditablePattern("Pressure", pressurePrimaryLabel());
    }

    private void adjustSelectedHitTimbre(final int amount) {
        if (!hasHeldPulse() || amount == 0) {
            return;
        }
        final EditablePulse pulse = editablePulses.get(activePulseIndex());
        pulse.timbreOffset = clampSignedUnit(pulse.timbreOffset + amount * 0.05);
        applyEditablePattern("Timbre", timbrePrimaryLabel());
    }

    private void adjustSelectedHitPitchExpression(final int amount) {
        if (!hasHeldPulse() || amount == 0) {
            return;
        }
        final EditablePulse pulse = editablePulses.get(activePulseIndex());
        pulse.pitchExpressionOffset = clampPitchExpression(pulse.pitchExpressionOffset + amount);
        applyEditablePattern("Pitch Expr", pitchExpressionPrimaryLabel());
    }

    private void adjustSelectedHitChance(final int amount) {
        if (!hasHeldPulse() || amount == 0) {
            return;
        }
        final EditablePulse pulse = editablePulses.get(activePulseIndex());
        pulse.chanceOffset = clampSignedUnit(pulse.chanceOffset + amount * 0.05);
        applyEditablePattern("Chance", chancePrimaryLabel());
    }

    private void adjustSelectedHitGate(final int amount) {
        if (editablePulses.isEmpty() || amount == 0) {
            return;
        }
        if (hasHeldPulse()) {
            final EditablePulse pulse = editablePulses.get(activePulseIndex());
            pulse.gateScale = clampGateScale(pulse.gateScale + amount * 0.05);
            applyEditablePattern("Hit Gate", "%.2f".formatted(pulse.gateScale));
            return;
        }
        final double nextGateScale = clampGateScale(globalGateScale() + amount * 0.05);
        for (final EditablePulse pulse : editablePulses) {
            pulse.gateScale = nextGateScale;
        }
        applyEditablePattern("Gate All", "%.2f".formatted(nextGateScale));
    }

    private void adjustPitch(final int amount) {
        if (amount == 0) {
            return;
        }
        final int previousBaseNote = driver.getSharedBaseMidiNote();
        final int nextBaseNote = Math.max(0, Math.min(95, previousBaseNote + amount));
        driver.setSharedRootNote(Math.floorMod(nextBaseNote, 12));
        driver.setSharedOctave(Math.floorDiv(nextBaseNote, 12));
        if (!editablePulses.isEmpty()) {
            final int delta = nextBaseNote - previousBaseNote;
            for (final EditablePulse pulse : editablePulses) {
                pulse.midiNote = Math.max(0, Math.min(127, pulse.midiNote + delta));
            }
            applyEditablePattern("Pitch", pitchLabel());
            return;
        }
        generatePattern("Pitch", pitchLabel());
    }

    private void resetHitsFromEncoder(final int amount) {
        if (amount != 0) {
            clearPulseEdits();
        }
    }

    private void setLastStep(final int stepIndex) {
        if (!ensureClipAvailable()) {
            return;
        }
        lastStepIndex = NestedRhythmLoopLength.normalizeLastStepIndex(stepIndex);
        refreshClipCursor();
        cursorClip.getLoopLength().set(loopLengthBeats());
        clampPlayStartToLoop();
        final String label = Integer.toString(lastStepIndex + 1);
        oled.valueInfo("Last Step", label);
        driver.notifyPopup("Last Step", label);
    }

    private EncoderSlotBinding mixerSlot(final int index, final String label) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                final Parameter parameter = switch (index) {
                    case 0 -> cursorTrack.volume();
                    case 1 -> cursorTrack.pan();
                    case 2 -> cursorTrack.sendBank().getItemAt(0);
                    default -> cursorTrack.sendBank().getItemAt(1);
                };
                parameter.name().markInterested();
                parameter.displayedValue().markInterested();
                parameter.value().markInterested();
                encoder.bindContinuousEncoder(layer, driver::isGlobalShiftHeld, inc -> {
                    EncoderValueProfile.LARGE_RANGE.adjustParameter(parameter, driver.isGlobalShiftHeld(), inc);
                    oled.valueInfo(label, parameter.displayedValue().get());
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        oled.valueInfo(label, parameter.displayedValue().get());
                    } else {
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private void ignoreEncoder(final int amount) {
    }

    private String selectedVelocityLabel() {
        return hasHeldPulse() ? Integer.toString(editablePulses.get(activePulseIndex()).effectiveVelocity()) : "None";
    }

    private String velocityDepthLabel() {
        return "%.2fx".formatted(velocityDepth);
    }

    private String velocityCenterLabel() {
        return Integer.toString(velocityCenter);
    }

    private String velocityPrimaryLabel() {
        return hasHeldPulse()
                ? Integer.toString(editablePulses.get(activePulseIndex()).effectiveVelocity())
                : velocityDepthLabel();
    }

    private String densityLabel() {
        return percentLabel(displayDensity());
    }

    private String densityDirectionLabel() {
        return densityDirection == NestedRhythmGenerator.DensityDirection.KEEP_WEAK ? "Weak" : "Strong";
    }

    private double displayDensity() {
        return DENSITY_DISPLAY_MIN + density * (1.0 - DENSITY_DISPLAY_MIN);
    }

    private double internalDensityForDisplay(final double displayedDensity) {
        return clampUnit((displayedDensity - DENSITY_DISPLAY_MIN) / (1.0 - DENSITY_DISPLAY_MIN));
    }

    private String pressureCenterLabel() {
        return percentLabel(pressureCenter);
    }

    private String pressurePrimaryLabel() {
        return hasHeldPulse()
                ? percentLabel(editablePulses.get(activePulseIndex()).effectivePressure())
                : pressureSpreadLabel();
    }

    private String pressureSpreadLabel() {
        return percentLabel(pressureSpread);
    }

    private String pressureRotationLabel() {
        return Integer.toString(pressureRotation);
    }

    private String timbreCenterLabel() {
        return signedPercentLabel(timbreCenter);
    }

    private String timbrePrimaryLabel() {
        return hasHeldPulse()
                ? signedPercentLabel(editablePulses.get(activePulseIndex()).effectiveTimbre())
                : timbreSpreadLabel();
    }

    private String timbreSpreadLabel() {
        return percentLabel(timbreSpread);
    }

    private String timbreRotationLabel() {
        return Integer.toString(timbreRotation);
    }

    private String pitchExpressionCenterLabel() {
        return "%.0f st".formatted(pitchExpressionCenter);
    }

    private String pitchExpressionPrimaryLabel() {
        return hasHeldPulse()
                ? "%.0f st".formatted(editablePulses.get(activePulseIndex()).effectivePitchExpression())
                : pitchExpressionSpreadLabel();
    }

    private String pitchExpressionSpreadLabel() {
        return "%.0f st".formatted(pitchExpressionSpread);
    }

    private String pitchExpressionRotationLabel() {
        return Integer.toString(pitchExpressionRotation);
    }

    private String chanceBaselineLabel() {
        return percentLabel(chanceBaseline);
    }

    private String chancePrimaryLabel() {
        return hasHeldPulse()
                ? percentLabel(editablePulses.get(activePulseIndex()).effectiveChance())
                : generatedChanceMinimumLabel();
    }

    private String chancePlayProbabilityLabel() {
        return percentLabel(chancePlayProbability);
    }

    private String generatedChanceMinimumLabel() {
        if (editablePulses.isEmpty()) {
            return chancePlayProbabilityLabel();
        }
        double minimum = 1.0;
        for (final EditablePulse pulse : editablePulses) {
            minimum = Math.min(minimum, pulse.effectiveChance());
        }
        return percentLabel(minimum);
    }

    private String chanceRotationLabel() {
        return Integer.toString(chanceRotation);
    }

    private String recurrenceDepthLabel() {
        return percentLabel(recurrenceDepth);
    }

    private String ratchetTargetLabel() {
        return ratchetTargets == 0 ? "Off" : ratchetTargets + " cells";
    }

    private String ratchetDivisionLabel() {
        return "x" + ratchetDivisions;
    }

    private String ratchetTargetModeLabel() {
        return ratchetTargetMode == NestedRhythmGenerator.RatchetTargetMode.BARLOW ? "B" : "Default";
    }

    private String clusterLabel() {
        return percentLabel(cluster);
    }

    private String pitchLabel() {
        return NoteGridLayout.noteName(driver.getSharedRootNote()) + driver.getSharedOctave();
    }

    private String clipLengthLabel() {
        final double bars = loopLengthBeats()
                / NestedRhythmGenerator.beatsPerBar(meterNumerator(), meterDenominator());
        final int roundedBars = (int) Math.round(bars);
        if (Math.abs(bars - roundedBars) <= 0.0001) {
            return "%d Bar%s".formatted(roundedBars, roundedBars == 1 ? "" : "s");
        }
        return "%.2f Bars".formatted(bars);
    }

    private String rateLabel() {
        if (Math.abs(rate - 0.25) <= 0.0001) {
            return "1/4x";
        }
        if (Math.abs(rate - 0.5) <= 0.0001) {
            return "1/2x";
        }
        return "%.0fx".formatted(rate);
    }

    private String playStartLabel() {
        final double step = NestedRhythmPlayStart.beatStep(meterDenominator());
        final double playStart = NestedRhythmPlayStart.wrap(cursorClip.getPlayStart().get(), loopLengthBeats());
        final double beatIndexDouble = playStart / step;
        final int beatIndex = (int) Math.round(beatIndexDouble);
        if (Math.abs(beatIndexDouble - beatIndex) > 0.0001) {
            return "%.2f".formatted(playStart);
        }
        final int barIndex = beatIndex / meterNumerator();
        final int beatInBar = beatIndex % meterNumerator();
        return "%d.%d".formatted(barIndex + 1, beatInBar + 1);
    }

    private String meterLabel() {
        return "%d/%d".formatted(meterNumerator(), meterDenominator());
    }

    private String tupletTargetLabel() {
        return tupletTargets == 0 ? "Off" : tupletTargets + " spans";
    }

    private String tupletDivisionLabel() {
        return "x" + tupletDivisions;
    }

    private String tupletTargetPhaseLabel() {
        if (tupletTargets == 0) {
            return "Off";
        }
        if (tupletTargets >= totalTupletHalfBars()) {
            return "Whole";
        }
        final List<Integer> priorityOrder = NestedRhythmGenerator.tupletTargetPriorityOrder(totalTupletHalfBars());
        final List<Integer> targets = new ArrayList<>(tupletTargets);
        for (int index = 0; index < tupletTargets; index++) {
            targets.add(priorityOrder.get(Math.floorMod(index + tupletTargetPhase, priorityOrder.size())));
        }
        targets.sort(Comparator.naturalOrder());
        final List<String> labels = new ArrayList<>(targets.size());
        for (final int target : targets) {
            labels.add(Integer.toString(target + 1));
        }
        return String.join("+", labels);
    }

    private String ratchetTargetPhaseLabel() {
        final int totalRegions = totalRatchetRegions();
        if (ratchetTargets == 0 || totalRegions <= 0) {
            return "Off";
        }
        final StringBuilder label = new StringBuilder();
        for (int index = 0; index < ratchetTargets; index++) {
            if (index > 0) {
                label.append('+');
            }
            label.append("T").append(Math.floorMod(index + ratchetTargetPhase, totalRegions) + 1);
        }
        return label.toString();
    }

    private String selectedGateLabel() {
        if (editablePulses.isEmpty()) {
            return "None";
        }
        if (hasHeldPulse()) {
            return "%.2f".formatted(editablePulses.get(activePulseIndex()).gateScale);
        }
        final double globalGateScale = globalGateScale();
        return allGateScalesEqual(globalGateScale) ? "All %.2f".formatted(globalGateScale) : "All Mixed";
    }

    private String selectedHitLabel() {
        return hasHeldPulse() ? Integer.toString(activePulseIndex() + 1) : "None";
    }

    private String percentLabel(final double value) {
        return "%d%%".formatted((int) Math.round(value * 100.0));
    }

    private String signedPercentLabel(final double value) {
        return "%+d%%".formatted((int) Math.round(value * 100.0));
    }

    private boolean hasHeldPulse() {
        return activePulseIndex() >= 0;
    }

    private String countLabel(final int count) {
        return count == 0 ? "Off" : Integer.toString(count);
    }

    private int stepCount(final int current, final int amount, final int[] values) {
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                index = i;
                break;
            }
        }
        return values[Math.max(0, Math.min(values.length - 1, index + amount))];
    }

    private double stepRate(final double current, final int amount) {
        int index = 0;
        for (int i = 0; i < RATE_VALUES.length; i++) {
            if (Math.abs(RATE_VALUES[i] - current) <= 0.0001) {
                index = i;
                break;
            }
        }
        return RATE_VALUES[Math.max(0, Math.min(RATE_VALUES.length - 1, index + amount))];
    }

    private double clampUnit(final double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double clampGateScale(final double value) {
        return Math.max(0.2, Math.min(1.5, value));
    }

    private double clampSignedUnit(final double value) {
        return Math.max(-1.0, Math.min(1.0, value));
    }

    private double clampPitchExpression(final double value) {
        return Math.max(-24.0, Math.min(24.0, value));
    }

    private double clampPitchExpressionSpread(final double value) {
        return Math.max(0.0, Math.min(24.0, value));
    }

    private double globalGateScale() {
        if (editablePulses.isEmpty()) {
            return 1.0;
        }
        double sum = 0.0;
        for (final EditablePulse pulse : editablePulses) {
            sum += pulse.gateScale;
        }
        return sum / editablePulses.size();
    }

    private boolean allGateScalesEqual(final double reference) {
        for (final EditablePulse pulse : editablePulses) {
            if (Math.abs(pulse.gateScale - reference) > 0.0001) {
                return false;
            }
        }
        return true;
    }

    private void clampPlayStartToLoop() {
        refreshClipCursor();
        cursorClip.getPlayStart().set(NestedRhythmPlayStart.wrap(cursorClip.getPlayStart().get(), loopLengthBeats()));
    }

    private void refreshSelectedClipState() {
        final SelectedClipSlotState state = SelectedClipSlotState.scan(clipSlotBank, BASE_COLOR);
        selectedClipSlotIndex = state.slotIndex();
        selectedClipHasContent = state.hasContent();
        selectedClipColor = state.color();
        syncClipLengthFromDaw();
        normalizeTupletControls();
        normalizeRatchetControls();
    }

    private void syncClipLengthFromDaw() {
        syncClipLengthFromBeats(cursorClip.getLoopLength().get());
    }

    private void syncClipLengthFromBeats(final double loopLength) {
        if (clipLengthSyncSuppressed) {
            return;
        }
        applyClipLengthFromBeats(loopLength);
    }

    private void applyClipLengthFromBeats(final double loopLength) {
        if (loopLength <= 0.0001) {
            return;
        }
        final NestedRhythmLoopLength.Settings settings = NestedRhythmLoopLength.settingsFromBeats(
                loopLength,
                NestedRhythmGenerator.beatsPerBar(meterNumerator(), meterDenominator()),
                CLIP_BAR_COUNT_VALUES);
        clipBarCount = settings.barCount();
        lastStepIndex = settings.lastStepIndex();
        normalizeTupletControls();
        normalizeRatchetControls();
    }

    private void maybeGenerateOnEmptySelectedClip() {
        if (!cursorTrack.canHoldNoteData().get() || selectedClipSlotIndex < 0 || selectedClipHasContent) {
            return;
        }
        generatePattern("Generate", summaryLabel());
    }

    private boolean ensureClipAvailable() {
        refreshSelectedClipState();
        final NoteClipAvailability.Failure failure = NoteClipAvailability.requireSelectedClipSlot(
                cursorTrack.canHoldNoteData().get(), selectedClipSlotIndex >= 0);
        if (failure != null) {
            oled.valueInfo(failure.title(), failure.oledDetail());
            driver.notifyPopup(failure.title(), failure.popupDetail());
            return false;
        }
        refreshClipCursor();
        return true;
    }

    private void refreshClipCursor() {
        NoteClipCursorRefresher.refresh(
                clipSlotBank,
                driver.getViewControl().getSelectedClipSlotIndex(),
                this::refreshSelectedClipState,
                () -> selectedClipSlotIndex,
                () -> cursorClip.scrollToKey(0),
                () -> cursorClip.scrollToStep(0),
                () -> cursorClip.setStepSize(CLIP_STEP_SIZE));
    }

    @Override
    public boolean isSelectHeld() {
        return selectHeld.get();
    }

    @Override
    public CursorRemoteControlsPage getActiveRemoteControlsPage() {
        return remoteControlsPage;
    }

    @Override
    public boolean isPadBeingHeld() {
        return hasHeldPulse();
    }

    @Override
    public List<NoteStep> getOnNotes() {
        return List.of();
    }

    @Override
    public List<NoteStep> getHeldNotes() {
        return List.of();
    }

    @Override
    public String getDetails(final List<NoteStep> heldNotes) {
        return selectedHitLabel();
    }

    @Override
    public double getGridResolution() {
        return CLIP_STEP_SIZE;
    }

    @Override
    public BooleanValueObject getLengthDisplay() {
        return lengthDisplay;
    }

    @Override
    public BooleanValueObject getDeleteHeld() {
        return deleteHeld;
    }

    @Override
    public String getPadInfo() {
        return "Nested";
    }

    @Override
    public void exitRecurrenceEdit() {
    }

    @Override
    public void enterRecurrenceEdit(final List<NoteStep> notes) {
    }

    @Override
    public void updateRecurrencLength(final int length) {
    }

    @Override
    public void registerModifiedSteps(final List<NoteStep> notes) {
    }

    @Override
    public EncoderBankLayout getEncoderBankLayout() {
        return encoderBankLayout;
    }

    @Override
    public boolean isCopyHeld() {
        return copyHeld.get();
    }

    @Override
    public boolean isDeleteHeld() {
        return deleteHeld.get();
    }

    @Override
    public boolean isShiftHeld() {
        return driver.isGlobalShiftHeld();
    }

    @Override
    public AkaiFireOikontrolExtension getDriver() {
        return driver;
    }

    @Override
    public OledDisplay getOled() {
        return oled;
    }

    @Override
    public ClipLauncherSlotBank getClipSlotBank() {
        return clipSlotBank;
    }

    @Override
    public PinnableCursorClip getClipCursor() {
        return cursorClip;
    }

    @Override
    public void notifyPopup(final String title, final String value) {
        driver.notifyPopup(title, value);
    }

    @Override
    public int getClipCreateLengthBeats() {
        return Math.max(1, (int) Math.ceil(loopLengthBeats()));
    }

    private int meterNumerator() {
        return Math.max(1, driver.getTransportTimeSignatureNumerator());
    }

    private int meterDenominator() {
        return Math.max(1, driver.getTransportTimeSignatureDenominator());
    }

    private int totalFineStepCount() {
        return NestedRhythmGenerator.fineStepsPerBar(meterNumerator(), meterDenominator()) * clipBarCount;
    }

    private double loopLengthBeats() {
        return NestedRhythmLoopLength.loopLengthBeats(
                NestedRhythmGenerator.beatsPerBar(meterNumerator(), meterDenominator()) * clipBarCount,
                lastStepIndex);
    }

    private double observedLoopLengthBeats() {
        final double observed = cursorClip.getLoopLength().get();
        return observed > 0.0001 ? observed : loopLengthBeats();
    }

    private double maxLoopLengthBeats() {
        return NestedRhythmGenerator.beatsPerBar(meterNumerator(), meterDenominator()) * MAX_BAR_COUNT;
    }

    private int totalRatchetRegions() {
        return NestedRhythmGenerator.ratchetParentRegionCount(
                meterNumerator(),
                meterDenominator(),
                clipBarCount,
                tupletDivisions,
                tupletTargets,
                tupletTargetPhase,
                cluster,
                rate,
                ratchetDivisions);
    }

    private void normalizeRatchetControls() {
        final int totalRegions = totalRatchetRegions();
        ratchetTargets = Math.max(0, Math.min(totalRegions, ratchetTargets));
        ratchetTargetPhase = totalRegions <= 0 ? 0 : Math.floorMod(ratchetTargetPhase, totalRegions);
    }

    private int totalTupletHalfBars() {
        return NestedRhythmGenerator.tupletTargetSpanCount(
                meterNumerator(), meterDenominator(), clipBarCount, rate);
    }

    private int[] availableTupletDivisions() {
        return NestedRhythmGenerator.supportedTupletDivisions(meterNumerator(), meterDenominator(), rate);
    }

    private int normalizeTupletDivisions(final int count) {
        final int[] supported = availableTupletDivisions();
        int best = supported[0];
        int bestDistance = Integer.MAX_VALUE;
        for (final int supportedCount : supported) {
            final int distance = Math.abs(supportedCount - count);
            if (distance < bestDistance || (distance == bestDistance && supportedCount < best)) {
                bestDistance = distance;
                best = supportedCount;
            }
        }
        return best;
    }

    private void normalizeTupletControls() {
        tupletTargets = Math.max(0, Math.min(totalTupletHalfBars(), tupletTargets));
        tupletTargetPhase = Math.floorMod(tupletTargetPhase, totalTupletHalfBars());
        tupletDivisions = normalizeTupletDivisions(tupletDivisions);
    }

    private final class EditablePulse {
        private final int order;
        private final int fineStart;
        private final int baseDuration;
        private int midiNote;
        private final int baseVelocity;
        private final NestedRhythmPattern.Role role;
        private final double indispensability;
        private int velocityOffset = 0;
        private double gateScale = 1.0;
        private double pressureOffset = 0.0;
        private double timbreOffset = 0.0;
        private double pitchExpressionOffset = 0.0;
        private double chanceOffset = 0.0;
        private int recurrenceLength = 0;
        private int recurrenceMask = 0;
        private boolean recurrenceEdited = false;
        private boolean enabled = true;

        private EditablePulse(final NestedRhythmPattern.PulseEvent event) {
            this.order = event.order();
            this.fineStart = event.fineStart();
            this.baseDuration = event.duration();
            this.midiNote = event.midiNote();
            this.baseVelocity = event.velocity();
            this.role = event.role();
            this.indispensability = event.indispensability();
        }

        private int effectiveVelocity() {
            return Math.max(1, Math.min(127, baseVelocity + velocityOffset));
        }

        private double effectivePressure() {
            return clampUnit(NestedRhythmContourShaper.shapeUnitExpression(
                    order, pressureCenter, pressureSpread, pressureRotation) + pressureOffset);
        }

        private double effectiveTimbre() {
            return clampSignedUnit(NestedRhythmContourShaper.shapeSignedExpression(
                    order, timbreCenter, timbreSpread, timbreRotation) + timbreOffset);
        }

        private double effectivePitchExpression() {
            return clampPitchExpression(NestedRhythmContourShaper.shapePitchExpression(
                    order, pitchExpressionCenter, pitchExpressionSpread, pitchExpressionRotation)
                    + pitchExpressionOffset);
        }

        private double effectiveChance() {
            return clampUnit(NestedRhythmContourShaper.shapeChance(
                    order, role, indispensability, chanceBaseline, chancePlayProbability, chanceRotation)
                    + chanceOffset);
        }

        private RecurrencePattern effectiveRecurrence() {
            return RecurrencePattern.of(recurrenceLength, recurrenceMask);
        }

        private void applyGeneratedRecurrence() {
            final RecurrencePattern recurrence = NestedRhythmContourShaper.generatedRecurrence(
                    order, role, indispensability, recurrenceDepth);
            recurrenceLength = recurrence.length();
            recurrenceMask = recurrence.mask();
        }

        private double effectiveDuration() {
            return Math.max(1.0, baseDuration * gateScale);
        }

        private double effectiveBeatDuration() {
            return effectiveDuration() * CLIP_STEP_SIZE;
        }

        private boolean containsFineStep(final int fineStep, final int clipFineSteps) {
            final int duration = Math.max(1, (int) Math.round(effectiveDuration()));
            final int end = Math.floorMod(fineStart + duration, clipFineSteps);
            if (duration >= clipFineSteps) {
                return true;
            }
            if (fineStart < end) {
                return fineStep >= fineStart && fineStep < end;
            }
            return fineStep >= fineStart || fineStep < end;
        }

        private void resetEdits() {
            velocityOffset = 0;
            gateScale = 1.0;
            pressureOffset = 0.0;
            timbreOffset = 0.0;
            pitchExpressionOffset = 0.0;
            chanceOffset = 0.0;
            recurrenceLength = 0;
            recurrenceMask = 0;
            recurrenceEdited = false;
            enabled = true;
        }

        private void copyLocalEditsFrom(final EditablePulse other) {
            velocityOffset = other.velocityOffset;
            gateScale = other.gateScale;
            pressureOffset = other.pressureOffset;
            timbreOffset = other.timbreOffset;
            pitchExpressionOffset = other.pitchExpressionOffset;
            chanceOffset = other.chanceOffset;
            recurrenceLength = other.recurrenceLength;
            recurrenceMask = other.recurrenceMask;
            recurrenceEdited = other.recurrenceEdited;
            enabled = other.enabled;
        }

        private String roleLabel() {
            return switch (role) {
                case PRIMARY_ANCHOR -> "Anchor";
                case SECONDARY_ANCHOR -> "Support";
                case TUPLET_LEAD -> "Tuplet Lead";
                case TUPLET_INTERIOR -> "Tuplet";
                case RATCHET_LEAD -> "Ratchet Lead";
                case RATCHET_INTERIOR -> "Ratchet";
                case PICKUP -> "Pickup";
            };
        }
    }

    private record ModifierEncoderView(String label, java.util.function.Supplier<String> valueSupplier,
                                       java.util.function.IntConsumer adjuster) {
    }

    private record PendingPulseWrite(int midiNote,
                                     double pressure,
                                     double timbre,
                                     double pitchExpression,
                                     double chance,
                                     RecurrencePattern recurrence) {
        private static PendingPulseWrite fromPulse(final EditablePulse pulse) {
            return new PendingPulseWrite(
                    pulse.midiNote,
                    pulse.effectivePressure(),
                    pulse.effectiveTimbre(),
                    pulse.effectivePitchExpression(),
                    pulse.effectiveChance(),
                    pulse.effectiveRecurrence());
        }
    }
}
