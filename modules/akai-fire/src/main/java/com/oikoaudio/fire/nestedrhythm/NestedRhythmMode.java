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
    private static final RgbLigthState DISABLED_HIT_COLOR = new RgbLigthState(32, 10, 10, true);
    private static final int[] TUPLET_COUNT_VALUES = {0, 3, 5, 7};
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
    private NestedRhythmGenerator.TupletCoverage tupletCoverage = NestedRhythmGenerator.TupletCoverage.BACK_HALF;
    private int tupletPhase = 0;
    private int ratchetCount = 0;
    private int ratchetWidth = 1;
    private int ratchetPhase = 0;
    private double velocityDepth = 1.0;
    private int velocityCenter = 100;
    private int velocityRotation = 0;
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
        if (deleteHeld.get()) {
            final EditablePulse pulse = editablePulses.get(hitIndex);
            pulse.enabled = !pulse.enabled;
            applyEditablePattern("Hit", pulse.enabled ? "On" : "Off");
            return;
        }
        if (driver.isGlobalShiftHeld()) {
            editablePulses.get(hitIndex).resetEdits();
            applyEditablePattern("Hit", "Reset");
            return;
        }
        selectedPulseIndex = hitIndex;
        showSelectedPulse();
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
            return playingBin == bin
                    ? StepPadLightHelper.renderPlayheadHighlight(SELECTED_HIT_COLOR)
                    : SELECTED_HIT_COLOR;
        }
        final RgbLigthState base = pulse.enabled ? colorForVelocity(pulse.effectiveVelocity(), clipBaseColor()) : DISABLED_HIT_COLOR;
        return playingBin == bin ? StepPadLightHelper.renderPlayheadHighlight(base) : base;
    }

    private RgbLigthState velocityPadLight(final int hitIndex) {
        if (hitIndex >= editablePulses.size()) {
            return RgbLigthState.OFF;
        }
        final int playingHitIndex = playingPulseIndex();
        final EditablePulse pulse = editablePulses.get(hitIndex);
        if (selectedPulseIndex == hitIndex) {
            return playingHitIndex == hitIndex
                    ? StepPadLightHelper.renderPlayheadHighlight(SELECTED_HIT_COLOR)
                    : SELECTED_HIT_COLOR.getBrightest();
        }
        if (!pulse.enabled) {
            return DISABLED_HIT_COLOR;
        }
        final RgbLigthState base = colorForVelocity(pulse.effectiveVelocity(), BASE_COLOR);
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
        final NestedRhythmPattern pattern = generator.generate(currentSettings());
        editablePulses.clear();
        for (final NestedRhythmPattern.PulseEvent event : pattern.events()) {
            editablePulses.add(new EditablePulse(event));
        }
        selectedPulseIndex = editablePulses.isEmpty() ? -1 : Math.min(Math.max(selectedPulseIndex, 0), editablePulses.size() - 1);
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
        cursorClip.getLoopLength().set(BAR_LENGTH_BEATS);
        final List<EditablePulse> active = editablePulses.stream()
                .filter(pulse -> pulse.enabled)
                .sorted(Comparator.comparingInt(pulse -> pulse.fineStart))
                .toList();
        for (final EditablePulse pulse : active) {
            cursorClip.setStep(pulse.fineStart, pulse.midiNote,
                    pulse.effectiveVelocity(), pulse.effectiveBeatDuration());
        }
    }

    private void clearPulseEdits() {
        for (final EditablePulse pulse : editablePulses) {
            pulse.resetEdits();
        }
        applyEditablePattern("Hits", "Reset");
    }

    private NestedRhythmGenerator.Settings currentSettings() {
        return new NestedRhythmGenerator.Settings(
                driver.getSharedBaseMidiNote(),
                density,
                tupletCount,
                tupletCoverage,
                tupletPhase,
                ratchetCount,
                ratchetWidth,
                ratchetPhase,
                velocityDepth,
                velocityCenter,
                velocityRotation,
                0);
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

    private String summaryLabel() {
        final String tuplet = tupletCount == 0 ? "No Tuplet" : "%d %s".formatted(tupletCount, coverageLabel());
        final String ratchet = ratchetCount == 0 ? "No Ratchet" : "R%d W%d".formatted(ratchetCount, ratchetWidth);
        return "%s / %s".formatted(tuplet, ratchet);
    }

    private String coverageLabel() {
        return switch (tupletCoverage) {
            case NONE -> "Off";
            case BACK_HALF -> "Back";
            case BOTH -> "Both";
        };
    }

    private EncoderBankLayout createEncoderBankLayout() {
        final Map<EncoderMode, EncoderBank> banks = new EnumMap<>(EncoderMode.class);
        banks.put(EncoderMode.CHANNEL, new EncoderBank(
                "1: Density\n2: Tuplet / Alt Cover\n3: Ratchet / Alt Width\n4: Vel / Alt Rat / Shift Tup",
                new EncoderSlotBinding[]{
                        continuousSlot("Density", () -> "%.2f".formatted(density), this::adjustDensity),
                        modifierChoiceSlot(
                                view("Tuplet", () -> countLabel(tupletCount), this::adjustTupletCount),
                                view("Cover", this::coverageLabel, this::adjustTupletCoverage),
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
                "1: Vel / Alt Center\n2: -\n3: -\n4: -",
                new EncoderSlotBinding[]{
                        modifierChoiceSlot(
                                view("Vel Depth", this::velocityDepthLabel, this::adjustVelocityDepth),
                                view("Vel Center", this::velocityCenterLabel, this::adjustVelocityCenter),
                                null),
                        choiceSlot("-", () -> "-", this::ignoreEncoder),
                        choiceSlot("-", () -> "-", this::ignoreEncoder),
                        choiceSlot("-", () -> "-", this::ignoreEncoder)
                }));
        banks.put(EncoderMode.USER_1, new EncoderBank(
                "1: Hit Vel\n2: Hit Gate\n3: Hit On\n4: Hit Select",
                new EncoderSlotBinding[]{
                        continuousSlot("Hit Vel", this::selectedVelocityLabel, this::adjustSelectedHitVelocity),
                        continuousSlot("Hit Gate", this::selectedGateLabel, this::adjustSelectedHitGate),
                        choiceSlot("Hit On", this::selectedEnabledLabel, this::toggleSelectedHitEnabled),
                        choiceSlot("Hit", this::selectedHitLabel, this::adjustSelectedHitIndex)
                }));
        banks.put(EncoderMode.USER_2, new EncoderBank(
                "1: Pitch\n2: Reset Hits\n3: -\n4: -",
                new EncoderSlotBinding[]{
                        choiceSlot("Pitch", this::pitchLabel, this::adjustPitch),
                        choiceSlot("Reset", () -> editablePulses.isEmpty() ? "No Hits" : "Ready", this::resetHitsFromEncoder),
                        choiceSlot("-", () -> "-", this::ignoreEncoder),
                        choiceSlot("-", () -> "-", this::ignoreEncoder)
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
        tupletCount = stepCount(tupletCount, amount, TUPLET_COUNT_VALUES);
        generatePattern("Tuplet", countLabel(tupletCount));
    }

    private void adjustTupletCoverage(final int amount) {
        final NestedRhythmGenerator.TupletCoverage[] values = NestedRhythmGenerator.TupletCoverage.values();
        final int current = tupletCoverage.ordinal();
        tupletCoverage = values[Math.max(0, Math.min(values.length - 1, current + amount))];
        generatePattern("Coverage", coverageLabel());
    }

    private void adjustTupletPhase(final int amount) {
        tupletPhase = Math.floorMod(tupletPhase + amount, 2);
        generatePattern("Tuplet Phase", tupletPhaseLabel());
    }

    private void adjustRatchetCount(final int amount) {
        ratchetCount = stepCount(ratchetCount, amount, RATCHET_COUNT_VALUES);
        generatePattern("Ratchet", countLabel(ratchetCount));
    }

    private void adjustRatchetWidth(final int amount) {
        ratchetWidth = Math.max(1, Math.min(NestedRhythmGenerator.BEATS_PER_BAR, ratchetWidth + amount));
        generatePattern("Ratchet Width", Integer.toString(ratchetWidth));
    }

    private void adjustRatchetPhase(final int amount) {
        ratchetPhase = Math.floorMod(ratchetPhase + amount, NestedRhythmGenerator.BEATS_PER_BAR);
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
        velocityRotation = Math.max(0, Math.min(15, velocityRotation + amount));
        generatePattern("Vel Rotate", Integer.toString(velocityRotation));
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
        if (!hasSelectedPulse() || amount == 0) {
            return;
        }
        final EditablePulse pulse = editablePulses.get(selectedPulseIndex);
        pulse.gateScale = Math.max(0.2, Math.min(1.5, pulse.gateScale + amount * 0.05));
        applyEditablePattern("Hit Gate", "%.2f".formatted(pulse.gateScale));
    }

    private void toggleSelectedHitEnabled(final int amount) {
        if (!hasSelectedPulse() || amount == 0) {
            return;
        }
        final EditablePulse pulse = editablePulses.get(selectedPulseIndex);
        pulse.enabled = !pulse.enabled;
        applyEditablePattern("Hit", pulse.enabled ? "On" : "Off");
    }

    private void adjustSelectedHitIndex(final int amount) {
        if (editablePulses.isEmpty()) {
            return;
        }
        if (selectedPulseIndex < 0) {
            selectedPulseIndex = 0;
        } else {
            selectedPulseIndex = Math.max(0, Math.min(editablePulses.size() - 1, selectedPulseIndex + amount));
        }
        showSelectedPulse();
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

    private String pitchLabel() {
        return NoteGridLayout.noteName(driver.getSharedRootNote()) + driver.getSharedOctave();
    }

    private String tupletPhaseLabel() {
        if (tupletCount == 0 || tupletCoverage == NestedRhythmGenerator.TupletCoverage.NONE) {
            return "Off";
        }
        if (tupletCoverage == NestedRhythmGenerator.TupletCoverage.BOTH) {
            return "Whole";
        }
        return tupletPhase == 0 ? "Back" : "Front";
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
            label.append(ratchetBeatAt(index) + 1);
        }
        return label.toString();
    }

    private int ratchetBeatAt(final int index) {
        final int[] order = {0, 2, 1, 3};
        return order[Math.floorMod(index + ratchetPhase, order.length)];
    }

    private String selectedGateLabel() {
        return hasSelectedPulse() ? "%.2f".formatted(editablePulses.get(selectedPulseIndex).gateScale) : "None";
    }

    private String selectedEnabledLabel() {
        return hasSelectedPulse() && editablePulses.get(selectedPulseIndex).enabled ? "On" : "Off";
    }

    private String selectedHitLabel() {
        return hasSelectedPulse() ? Integer.toString(selectedPulseIndex + 1) : "None";
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

    private void refreshSelectedClipState() {
        final SelectedClipSlotState state = SelectedClipSlotState.scan(clipSlotBank, BASE_COLOR);
        selectedClipSlotIndex = state.slotIndex();
        selectedClipColor = state.color();
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
        return BAR_LENGTH_BEATS;
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

        private boolean containsFineStep(final int fineStep) {
            final int duration = Math.max(1, (int) Math.round(effectiveDuration()));
            final int end = Math.floorMod(fineStart + duration, NestedRhythmGenerator.FINE_STEPS_PER_BAR);
            if (duration >= NestedRhythmGenerator.FINE_STEPS_PER_BAR) {
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
