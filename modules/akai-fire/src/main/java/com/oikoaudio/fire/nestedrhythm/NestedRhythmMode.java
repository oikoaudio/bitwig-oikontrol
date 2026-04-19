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
import com.oikoaudio.fire.control.EncoderStepAccumulator;
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
    private static final RgbLigthState SELECTED_HIT_COLOR = new RgbLigthState(110, 76, 0, true);
    private static final RgbLigthState SELECTED_DISABLED_HIT_COLOR = new RgbLigthState(52, 36, 0, true);
    private static final int[] RATCHET_COUNT_VALUES = {0, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] CLIP_BAR_COUNT_VALUES = {1, 2, 4};

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
    private final BooleanValueObject copyHeld = new BooleanValueObject();
    private final BooleanValueObject deleteHeld = new BooleanValueObject();
    private final BooleanValueObject lengthDisplay = new BooleanValueObject();

    private final List<EditablePulse> editablePulses = new ArrayList<>();
    private boolean mainEncoderPressConsumed = false;
    private int selectedPulseIndex = -1;
    private int selectedClipSlotIndex = -1;
    private RgbLigthState selectedClipColor = BASE_COLOR;
    private int playingFineStep = -1;
    private double density = 1.0;
    private int tupletCount = 0;
    private int tupletCover = 1;
    private int tupletPhase = 1;
    private int ratchetCount = 0;
    private int ratchetWidth = 1;
    private int ratchetPhase = 0;
    private double velocityDepth = 1.0;
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
    private int clipBarCount = 1;

    public NestedRhythmMode(final AkaiFireOikontrolExtension driver) {
        super(driver.getLayers(), "NESTED_RHYTHM_MODE");
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
        patternButtons.setUpCallback(pressed -> {
            if (pressed) {
                generatePattern("Generate", summaryLabel());
            }
        }, () -> BiColorLightState.GREEN_HALF);
        patternButtons.setDownCallback(pressed -> {
            if (pressed) {
                clearPulseEdits();
            }
        }, () -> BiColorLightState.AMBER_HALF);
        encoderLayer.activate();
    }

    @Override
    protected void onDeactivate() {
        patternButtons.setUpCallback(pressed -> { }, () -> BiColorLightState.OFF);
        patternButtons.setDownCallback(pressed -> { }, () -> BiColorLightState.OFF);
        selectHeld.set(false);
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
            if (pressed) {
                clearPulseEdits();
            }
        }, () -> BiColorLightState.AMBER_HALF);

        driver.getButton(NoteAssign.MUTE_3).bindPressed(this, pressed -> {
            copyHeld.set(pressed);
            if (pressed) {
                oled.valueInfo("Copy", "Clip row");
            } else {
                oled.clearScreenDelayed();
            }
        }, () -> copyHeld.get() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF);

        driver.getButton(NoteAssign.MUTE_4).bindPressed(this, pressed -> {
            deleteHeld.set(pressed);
            if (pressed) {
                oled.valueInfo("Delete", "Clip / hit");
            } else {
                oled.clearScreenDelayed();
            }
        }, () -> deleteHeld.get() ? BiColorLightState.RED_FULL : BiColorLightState.RED_HALF);
    }

    private void bindMainEncoder() {
        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.bindEncoder(this, this::handleMainEncoder);
        mainEncoder.bindTouched(this, this::handleMainEncoderPress);
    }

    private void handlePadPress(final int padIndex, final boolean pressed) {
        if (padIndex < CLIP_ROW_PAD_COUNT) {
            clipHandler.handlePadPress(padIndex, pressed);
            return;
        }
        if (!pressed) {
            return;
        }
        if (padIndex < VELOCITY_PAD_OFFSET) {
            selectPulseForStructurePad(padIndex - STRUCTURE_PAD_OFFSET);
            return;
        }
        final int hitIndex = padIndex - VELOCITY_PAD_OFFSET;
        if (hitIndex >= editablePulses.size()) {
            return;
        }
        if (driver.isGlobalShiftHeld()) {
            editablePulses.get(hitIndex).resetEdits();
            applyEditablePattern("Hit", "Reset");
            return;
        }
        final EditablePulse pulse = editablePulses.get(hitIndex);
        selectedPulseIndex = hitIndex;
        pulse.enabled = !pulse.enabled;
        applyEditablePattern("Hit", pulse.enabled ? "On" : "Off");
    }

    private void selectPulseForStructurePad(final int structurePad) {
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
            selectedPulseIndex = bestIndex;
            showSelectedPulse();
        }
    }

    private RgbLigthState getPadLight(final int padIndex) {
        if (padIndex < CLIP_ROW_PAD_COUNT) {
            return clipHandler.getPadLight(padIndex);
        }
        if (padIndex < VELOCITY_PAD_OFFSET) {
            return structurePadLight(padIndex - STRUCTURE_PAD_OFFSET);
        }
        return velocityPadLight(padIndex - VELOCITY_PAD_OFFSET);
    }

    private RgbLigthState structurePadLight(final int bin) {
        final EditablePulse pulse = strongestPulseInBin(bin);
        final int playingBin = playingFineStep < 0 ? -1 : structureBinFor(playingFineStep);
        if (pulse == null) {
            return StepPadLightHelper.renderEmptyStep(bin, playingBin);
        }
        if (selectedPulseIndex >= 0 && selectedPulseIndex < editablePulses.size()
                && editablePulses.get(selectedPulseIndex) == pulse) {
            final RgbLigthState selectedColor = pulse.enabled ? SELECTED_HIT_COLOR : SELECTED_DISABLED_HIT_COLOR;
            return playingBin == bin
                    ? StepPadLightHelper.renderPlayheadHighlight(selectedColor)
                    : selectedColor;
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
        if (selectedPulseIndex == hitIndex) {
            final RgbLigthState selectedColor = pulse.enabled ? SELECTED_HIT_COLOR : SELECTED_DISABLED_HIT_COLOR;
            return playingHitIndex == hitIndex
                    ? StepPadLightHelper.renderPlayheadHighlight(selectedColor)
                    : selectedColor;
        }
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

    private void showSelectedPulse() {
        if (selectedPulseIndex < 0 || selectedPulseIndex >= editablePulses.size()) {
            oled.valueInfo("Hit", "None");
            return;
        }
        final EditablePulse pulse = editablePulses.get(selectedPulseIndex);
        oled.valueInfo("Hit %d".formatted(selectedPulseIndex + 1),
                "%s Vel %d Gate %.2f".formatted(pulse.roleLabel(), pulse.effectiveVelocity(), pulse.gateScale));
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
        final List<EditablePulse> previousPulses = List.copyOf(editablePulses);
        final int previousSelectionFineStart = hasSelectedPulse() ? editablePulses.get(selectedPulseIndex).fineStart : -1;
        final NestedRhythmPattern pattern = generator.generate(currentSettings());
        editablePulses.clear();
        for (final NestedRhythmPattern.PulseEvent event : pattern.events()) {
            editablePulses.add(restoreLocalEdits(new EditablePulse(event), previousPulses));
        }
        selectedPulseIndex = findSelectedPulseIndex(previousSelectionFineStart);
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
        cursorClip.clearSteps();
        cursorClip.getLoopLength().set(loopLengthBeats());
        final List<EditablePulse> active = editablePulses.stream()
                .filter(pulse -> pulse.enabled)
                .sorted(Comparator.comparingInt(pulse -> pulse.fineStart))
                .toList();
        for (final EditablePulse pulse : active) {
            cursorClip.setStep(pulse.fineStart, pulse.midiNote,
                    pulse.effectiveVelocity(), pulse.effectiveBeatDuration());
            applyExpressionValues(cursorClip.getStep(0, pulse.fineStart, pulse.midiNote), pulse);
        }
    }

    private void applyExpressionValues(final NoteStep step, final EditablePulse pulse) {
        final int order = pulse.order;
        step.setPressure(shapedUnitExpression(order, pressureCenter, pressureSpread, pressureRotation));
        step.setTimbre(shapedSignedExpression(order, timbreCenter, timbreSpread, timbreRotation));
        step.setTranspose(shapedPitchExpression(order, pitchExpressionCenter, pitchExpressionSpread,
                pitchExpressionRotation));
    }

    private double shapedUnitExpression(final int order,
                                        final double center,
                                        final double spread,
                                        final int rotation) {
        return clampUnit(center + spread * contourNormalized(order, rotation));
    }

    private double shapedSignedExpression(final int order,
                                          final double center,
                                          final double spread,
                                          final int rotation) {
        return clampSignedUnit(center + spread * contourNormalized(order, rotation));
    }

    private double shapedPitchExpression(final int order,
                                         final double center,
                                         final double spread,
                                         final int rotation) {
        return clampPitchExpression(center + spread * contourNormalized(order, rotation));
    }

    private double contourNormalized(final int order, final int rotation) {
        return NestedRhythmGenerator.contourAt(order + rotation) / 18.0;
    }

    private void clearPulseEdits() {
        for (final EditablePulse pulse : editablePulses) {
            pulse.resetEdits();
        }
        applyEditablePattern("Hits", "Reset");
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

    private NestedRhythmGenerator.Settings currentSettings() {
        return new NestedRhythmGenerator.Settings(
                driver.getSharedBaseMidiNote(),
                density,
                tupletCount,
                tupletCover,
                tupletPhase,
                ratchetCount,
                ratchetWidth,
                ratchetPhase,
                velocityDepth,
                velocityCenter,
                velocityRotation,
                0,
                meterNumerator(),
                meterDenominator(),
                clipBarCount);
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
        final String tuplet = tupletCount == 0 || tupletCover == 0
                ? "No Tuplet"
                : "%d / %s".formatted(tupletCount, coveredTupletRegionLabel());
        final String ratchet = ratchetCount == 0 ? "No Ratchet" : "R%d W%d".formatted(ratchetCount, ratchetWidth);
        return "%s / %s / %s x%d".formatted(tuplet, ratchet, meterLabel(), clipBarCount);
    }

    private String coverageLabel() {
        if (tupletCount == 0 || tupletCover == 0) {
            return "Off";
        }
        return "%d HB".formatted(tupletCover);
    }

    private EncoderBankLayout createEncoderBankLayout() {
        final Map<EncoderMode, EncoderBank> banks = new EnumMap<>(EncoderMode.class);
        banks.put(EncoderMode.CHANNEL, new EncoderBank(
                "1: Density\n2: Tuplet / Alt Cover\n3: Ratchet / Alt Width\n4: Vel / Alt Rat / Shift Tup",
                new EncoderSlotBinding[]{
                        continuousSlot("Density", () -> "%.2f".formatted(density), this::adjustDensity),
                        modifierChoiceSlot(
                                view("Tuplet", () -> countLabel(tupletCount), this::adjustTupletCount),
                                view("Cover", this::coverageLabel, this::adjustTupletCover),
                                null),
                        modifierChoiceSlot(
                                view("Ratchet", () -> countLabel(ratchetCount), this::adjustRatchetCount),
                                view("Width", () -> Integer.toString(ratchetWidth), this::adjustRatchetWidth),
                                null),
                        modifierChoiceSlot(
                                view("Vel Rotate", () -> Integer.toString(velocityRotation), this::adjustVelocityRotation),
                                view("Ratchet Phase", this::ratchetPhaseLabel, this::adjustRatchetPhase),
                                view("Tuplet Phase", this::tupletPhaseLabel, this::adjustTupletPhase))
                }));
        banks.put(EncoderMode.MIXER, new EncoderBank(
                "1: -\n2: -\n3: -\n4: -",
                new EncoderSlotBinding[]{
                        choiceSlot("-", () -> "-", this::ignoreEncoder),
                        choiceSlot("-", () -> "-", this::ignoreEncoder),
                        choiceSlot("-", () -> "-", this::ignoreEncoder),
                        choiceSlot("-", () -> "-", this::ignoreEncoder)
                }));
        banks.put(EncoderMode.USER_1, new EncoderBank(
                "1: Velocity\n2: Pressure\n3: Timbre\n4: Pitch Expr",
                new EncoderSlotBinding[]{
                        modifierChoiceSlot(
                                view("Velocity", this::velocityCenterLabel, this::adjustVelocityCenter),
                                view("Vel Spread", this::velocityDepthLabel, this::adjustVelocityDepth),
                                view("Vel Rotate", () -> Integer.toString(velocityRotation), this::adjustVelocityRotation)),
                        modifierChoiceSlot(
                                view("Pressure", this::pressureCenterLabel, this::adjustPressureCenter),
                                view("Prs Spread", this::pressureSpreadLabel, this::adjustPressureSpread),
                                view("Prs Rotate", this::pressureRotationLabel, this::adjustPressureRotation)),
                        modifierChoiceSlot(
                                view("Timbre", this::timbreCenterLabel, this::adjustTimbreCenter),
                                view("Tmb Spread", this::timbreSpreadLabel, this::adjustTimbreSpread),
                                view("Tmb Rotate", this::timbreRotationLabel, this::adjustTimbreRotation)),
                        modifierChoiceSlot(
                                view("Pitch Expr", this::pitchExpressionCenterLabel, this::adjustPitchExpressionCenter),
                                view("Ptc Spread", this::pitchExpressionSpreadLabel, this::adjustPitchExpressionSpread),
                                view("Ptc Rotate", this::pitchExpressionRotationLabel, this::adjustPitchExpressionRotation))
                }));
        banks.put(EncoderMode.USER_2, new EncoderBank(
                "1: Pitch\n2: Length\n3: Reset Hits\n4: Meter",
                new EncoderSlotBinding[]{
                        choiceSlot("Pitch", this::pitchLabel, this::adjustPitch),
                        choiceSlot("Length", this::clipLengthLabel, this::adjustClipBarCount),
                        choiceSlot("Reset", () -> editablePulses.isEmpty() ? "No Hits" : "Ready", this::resetHitsFromEncoder),
                        choiceSlot("Meter", this::meterLabel, this::ignoreEncoder)
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
                encoder.bindEncoder(layer, adjuster::accept);
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
                        driver::isGlobalShiftHeld, adjuster::accept);
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
        density = clampUnit(density + amount * 0.05);
        generatePattern("Density", "%.2f".formatted(density));
    }

    private void adjustTupletCount(final int amount) {
        tupletCount = stepCount(normalizeTupletCount(tupletCount), amount, availableTupletCounts());
        generatePattern("Tuplet", countLabel(tupletCount));
    }

    private void adjustTupletCover(final int amount) {
        tupletCover = Math.max(0, Math.min(totalTupletHalfBars(), tupletCover + amount));
        tupletCount = normalizeTupletCount(tupletCount);
        generatePattern("Coverage", coverageLabel());
    }

    private void adjustTupletPhase(final int amount) {
        tupletPhase = Math.floorMod(tupletPhase + amount, totalTupletHalfBars());
        generatePattern("Tuplet Phase", tupletPhaseLabel());
    }

    private void adjustRatchetCount(final int amount) {
        ratchetCount = stepCount(ratchetCount, amount, RATCHET_COUNT_VALUES);
        generatePattern("Ratchet", countLabel(ratchetCount));
    }

    private void adjustRatchetWidth(final int amount) {
        ratchetWidth = Math.max(1, Math.min(totalRatchetRegions(), ratchetWidth + amount));
        generatePattern("Ratchet Width", Integer.toString(ratchetWidth));
    }

    private void adjustRatchetPhase(final int amount) {
        ratchetPhase = Math.floorMod(ratchetPhase + amount, totalRatchetRegions());
        generatePattern("Ratchet Phase", ratchetPhaseLabel());
    }

    private void adjustVelocityDepth(final int amount) {
        if (amount == 0) {
            return;
        }
        velocityDepth = Math.max(0.25, Math.min(2.0, velocityDepth + amount * 0.05));
        generatePattern("Vel Depth", velocityDepthLabel());
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
        clipBarCount = stepCount(clipBarCount, amount, CLIP_BAR_COUNT_VALUES);
        tupletCover = Math.max(0, Math.min(totalTupletHalfBars(), tupletCover));
        tupletPhase = Math.floorMod(tupletPhase, totalTupletHalfBars());
        tupletCount = normalizeTupletCount(tupletCount);
        generatePattern("Length", clipLengthLabel());
    }

    private void adjustSelectedHitVelocity(final int amount) {
        if (!hasSelectedPulse() || amount == 0) {
            return;
        }
        final EditablePulse pulse = editablePulses.get(selectedPulseIndex);
        pulse.velocityOffset = Math.max(-48, Math.min(48, pulse.velocityOffset + amount * 2));
        applyEditablePattern("Hit Vel", Integer.toString(pulse.effectiveVelocity()));
    }

    private void adjustSelectedHitGate(final int amount) {
        if (editablePulses.isEmpty() || amount == 0) {
            return;
        }
        if (hasSelectedPulse()) {
            final EditablePulse pulse = editablePulses.get(selectedPulseIndex);
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
        final int nextBaseNote = Math.max(0, Math.min(95, driver.getSharedBaseMidiNote() + amount));
        driver.setSharedRootNote(Math.floorMod(nextBaseNote, 12));
        driver.setSharedOctave(Math.floorDiv(nextBaseNote, 12));
        generatePattern("Pitch", pitchLabel());
    }

    private void resetHitsFromEncoder(final int amount) {
        if (amount != 0) {
            clearPulseEdits();
        }
    }

    private void ignoreEncoder(final int amount) {
    }

    private String selectedVelocityLabel() {
        return hasSelectedPulse() ? Integer.toString(editablePulses.get(selectedPulseIndex).effectiveVelocity()) : "None";
    }

    private String velocityDepthLabel() {
        return "%.2fx".formatted(velocityDepth);
    }

    private String velocityCenterLabel() {
        return Integer.toString(velocityCenter);
    }

    private String pressureCenterLabel() {
        return percentLabel(pressureCenter);
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

    private String timbreSpreadLabel() {
        return percentLabel(timbreSpread);
    }

    private String timbreRotationLabel() {
        return Integer.toString(timbreRotation);
    }

    private String pitchExpressionCenterLabel() {
        return "%.0f st".formatted(pitchExpressionCenter);
    }

    private String pitchExpressionSpreadLabel() {
        return "%.0f st".formatted(pitchExpressionSpread);
    }

    private String pitchExpressionRotationLabel() {
        return Integer.toString(pitchExpressionRotation);
    }

    private String pitchLabel() {
        return NoteGridLayout.noteName(driver.getSharedRootNote()) + driver.getSharedOctave();
    }

    private String clipLengthLabel() {
        return "%d Bar%s".formatted(clipBarCount, clipBarCount == 1 ? "" : "s");
    }

    private String meterLabel() {
        return "%d/%d".formatted(meterNumerator(), meterDenominator());
    }

    private String coveredTupletRegionLabel() {
        if (tupletCover <= 0) {
            return "Off";
        }
        final List<String> labels = new ArrayList<>(tupletCover);
        for (int index = 0; index < tupletCover; index++) {
            labels.add(Integer.toString(tupletHalfBarAt(index) + 1));
        }
        return String.join("+", labels);
    }

    private int tupletHalfBarAt(final int index) {
        return Math.floorMod(tupletPhase + index, totalTupletHalfBars());
    }

    private String tupletPhaseLabel() {
        if (tupletCount == 0 || tupletCover == 0) {
            return "Off";
        }
        if (tupletCover >= totalTupletHalfBars()) {
            return "Whole";
        }
        return coveredTupletRegionLabel();
    }

    private String ratchetPhaseLabel() {
        if (ratchetCount == 0) {
            return "Off";
        }
        final StringBuilder label = new StringBuilder();
        for (int index = 0; index < ratchetWidth; index++) {
            if (index > 0) {
                label.append('+');
            }
            label.append(ratchetRegionLabel(ratchetBeatAt(index)));
        }
        return label.toString();
    }

    private int ratchetBeatAt(final int index) {
        final int totalRegions = totalRatchetRegions();
        final int baseRegion = NestedRhythmGenerator.phraseRatchetRegionAt(meterNumerator(), clipBarCount, index);
        return Math.floorMod(baseRegion + ratchetPhase, totalRegions);
    }

    private String ratchetRegionLabel(final int absoluteBeatIndex) {
        final int beatCount = meterNumerator();
        final int barIndex = absoluteBeatIndex / beatCount;
        final int beatInBar = absoluteBeatIndex % beatCount;
        if (clipBarCount == 1) {
            return Integer.toString(beatInBar + 1);
        }
        return "%d.%d".formatted(barIndex + 1, beatInBar + 1);
    }

    private String selectedGateLabel() {
        if (editablePulses.isEmpty()) {
            return "None";
        }
        if (hasSelectedPulse()) {
            return "%.2f".formatted(editablePulses.get(selectedPulseIndex).gateScale);
        }
        final double globalGateScale = globalGateScale();
        return allGateScalesEqual(globalGateScale) ? "All %.2f".formatted(globalGateScale) : "All Mixed";
    }

    private String selectedHitLabel() {
        return hasSelectedPulse() ? Integer.toString(selectedPulseIndex + 1) : "None";
    }

    private String percentLabel(final double value) {
        return "%d%%".formatted((int) Math.round(value * 100.0));
    }

    private String signedPercentLabel(final double value) {
        return "%+d%%".formatted((int) Math.round(value * 100.0));
    }

    private boolean hasSelectedPulse() {
        return selectedPulseIndex >= 0 && selectedPulseIndex < editablePulses.size();
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

    private void refreshSelectedClipState() {
        final SelectedClipSlotState state = SelectedClipSlotState.scan(clipSlotBank, BASE_COLOR);
        selectedClipSlotIndex = state.slotIndex();
        selectedClipColor = state.color();
        tupletCover = Math.max(0, Math.min(totalTupletHalfBars(), tupletCover));
        tupletPhase = Math.floorMod(tupletPhase, totalTupletHalfBars());
        tupletCount = normalizeTupletCount(tupletCount);
        ratchetWidth = Math.max(1, Math.min(totalRatchetRegions(), ratchetWidth));
        ratchetPhase = Math.floorMod(ratchetPhase, totalRatchetRegions());
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
        return false;
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
        return NestedRhythmGenerator.beatsPerBar(meterNumerator(), meterDenominator()) * clipBarCount;
    }

    private int totalRatchetRegions() {
        return Math.max(1, meterNumerator() * clipBarCount);
    }

    private int totalTupletHalfBars() {
        return Math.max(1, clipBarCount * 2);
    }

    private int[] availableTupletCounts() {
        return NestedRhythmGenerator.supportedTupletCounts(
                meterNumerator(),
                meterDenominator(),
                Math.max(1, tupletCover));
    }

    private int normalizeTupletCount(final int count) {
        final int[] supported = availableTupletCounts();
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

    private static final class EditablePulse {
        private final int order;
        private final int fineStart;
        private final int baseDuration;
        private final int midiNote;
        private final int baseVelocity;
        private final NestedRhythmPattern.Role role;
        private int velocityOffset = 0;
        private double gateScale = 1.0;
        private boolean enabled = true;

        private EditablePulse(final NestedRhythmPattern.PulseEvent event) {
            this.order = event.order();
            this.fineStart = event.fineStart();
            this.baseDuration = event.duration();
            this.midiNote = event.midiNote();
            this.baseVelocity = event.velocity();
            this.role = event.role();
        }

        private int effectiveVelocity() {
            return Math.max(1, Math.min(127, baseVelocity + velocityOffset));
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
            enabled = true;
        }

        private void copyLocalEditsFrom(final EditablePulse other) {
            velocityOffset = other.velocityOffset;
            gateScale = other.gateScale;
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
}
