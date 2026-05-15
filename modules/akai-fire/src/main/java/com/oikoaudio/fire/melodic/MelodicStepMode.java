package com.oikoaudio.fire.melodic;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.ContinuousEncoderScaler;
import com.oikoaudio.fire.control.EncoderStepAccumulator;
import com.oikoaudio.fire.control.EncoderValueProfile;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.EncoderBank;
import com.oikoaudio.fire.sequence.EncoderBankLayout;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.sequence.EncoderSlotBinding;
import com.oikoaudio.fire.control.MixerEncoderProfile;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;
import com.oikoaudio.fire.sequence.NoteClipAvailability;
import com.oikoaudio.fire.sequence.NoteClipCursorRefresher;
import com.oikoaudio.fire.sequence.NoteStepAccess;
import com.oikoaudio.fire.sequence.RecurrencePattern;
import com.oikoaudio.fire.sequence.SelectedClipSlotObserver;
import com.oikoaudio.fire.sequence.SelectedClipSlotState;
import com.oikoaudio.fire.sequence.ClipSlotSelectionResolver;
import com.oikoaudio.fire.sequence.ClipRowHandler;
import com.oikoaudio.fire.sequence.SeqClipRowHost;
import com.oikoaudio.fire.sequence.AccentLatchState;
import com.oikoaudio.fire.sequence.StepSequencerEncoderHandler;
import com.oikoaudio.fire.sequence.StepSequencerHost;
import com.oikoaudio.fire.utils.PatternButtons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class MelodicStepMode extends Layer implements StepSequencerHost, SeqClipRowHost {
    private static final int CLIP_ROW_PAD_COUNT = 16;
    private static final int PITCH_POOL_PAD_OFFSET = 16;
    private static final int PITCH_POOL_PAD_COUNT = 16;
    private static final int STEP_PAD_OFFSET = 32;
    private static final int STEP_COUNT = 32;
    private static final int DEFAULT_LOOP_STEPS = 16;
    private static final double STEP_LENGTH = 0.25;
    private static final int MAX_CLIP_LENGTH_BEATS = (int) (STEP_COUNT * STEP_LENGTH);
    private static final int DEFAULT_VELOCITY = 96;
    private static final double DEFAULT_GATE = 0.8;
    private static final int ENGINE_ENCODER_THRESHOLD = 5;
    private static final int MUTATION_MODE_ENCODER_THRESHOLD = 5;
    private static final int AUDITION_VELOCITY = 96;

    private final AkaiFireOikontrolExtension driver;
    private final OledDisplay oled;
    private final PatternButtons patternButtons;
    private final NoteRepeatHandler noteRepeatHandler;
    private final NoteInput noteInput;
    private final CursorTrack cursorTrack;
    private final PinnableCursorClip cursorClip;
    private final ClipLauncherSlotBank clipSlotBank;
    private final CursorRemoteControlsPage remoteControlsPage;
    private final StepSequencerEncoderHandler encoderLayer;
    private final EncoderBankLayout encoderBankLayout;
    private final Map<Integer, Map<Integer, NoteStep>> noteStepsByPosition = new HashMap<>();
    private final MelodicStepClipWriter clipWriter = new MelodicStepClipWriter();
    private final BooleanValueObject lengthDisplay = new BooleanValueObject();
    private final BooleanValueObject selectHeld = new BooleanValueObject();
    private final BooleanValueObject copyHeld = new BooleanValueObject();
    private final BooleanValueObject deleteHeld = new BooleanValueObject();
    private final BooleanValueObject fixedLengthHeld = new BooleanValueObject();
    private final ClipRowHandler clipHandler;
    private final Set<Integer> auditioningPoolPitches = new HashSet<>();
    private final MotifGenerator motifGenerator = new MotifGenerator();
    private final CallResponseGenerator callResponseGenerator = new CallResponseGenerator();
    private final AcidGenerator acidGenerator = new AcidGenerator();
    private final RollingBassGenerator rollingBassGenerator = new RollingBassGenerator();
    private final OctaveJumpGenerator octaveJumpGenerator = new OctaveJumpGenerator();
    private final MelodicMutator mutator = new MelodicMutator();

    private final MelodicStepPatternState patternState = new MelodicStepPatternState(DEFAULT_LOOP_STEPS);
    private int selectedStep = 0;
    private Integer heldStep = null;
    private final LinkedHashSet<Integer> heldSteps = new LinkedHashSet<>();
    private boolean heldStepConsumed = false;
    private boolean recurrenceSpanAnchorHeld = false;
    private boolean recurrenceSpanGestureUsed = false;
    private int playingStep = -1;
    private int selectedClipSlotIndex = -1;
    private RgbLigthState selectedClipColor = MelodicRenderer.ACTIVE_STEP;
    private int loopSteps = DEFAULT_LOOP_STEPS;
    private final LinkedHashSet<Integer> allowedPitches = new LinkedHashSet<>();
    private boolean poolUserEdited = false;
    private Generator poolGeneratorSource = null;
    private final AccentLatchState accentState = new AccentLatchState();
    private boolean mainEncoderPressConsumed = false;
    private double density = 0.45;
    private double timeVariance = 0.0;
    private double tension = 0.25;
    private double octaveActivity = 0.1;
    private double legato = 0.1;
    private int euclideanPulses = 5;
    private int euclideanRotation = 0;
    private double mutateIntensity = 0.45;
    private long seed;
    private int poolLayoutRootPitch = -1;
    private View view = View.PROCESS;
    private Generator generator = Generator.ACID;
    private MelodicMutator.Mode mutationMode = MelodicMutator.Mode.PRESERVE_RHYTHM;

    private enum View {
        NOTES("Notes"),
        EXPRESSION("Expression"),
        PROCESS("Process");

        private final String label;

        View(final String label) {
            this.label = label;
        }
    }

    private enum Generator {
        ACID("Acid"),
        CALL_RESPONSE("Call/Resp"),
        ROLLING("Rolling"),
        OCTAVE("Octave"),
        MOTIF("Motif");

        private final String label;

        Generator(final String label) {
            this.label = label;
        }
    }

    public MelodicStepMode(final AkaiFireOikontrolExtension driver, final NoteRepeatHandler noteRepeatHandler) {
        super(driver.getLayers(), "MELODIC_STEP_MODE");
        this.driver = driver;
        this.oled = driver.getOled();
        this.patternButtons = driver.getPatternButtons();
        this.noteRepeatHandler = noteRepeatHandler;
        this.noteInput = driver.getNoteInput();

        final ControllerHost host = driver.getHost();
        this.cursorTrack = host.createCursorTrack("MELODIC_STEP", "Melodic Step", 8, CLIP_ROW_PAD_COUNT, true);
        this.cursorTrack.name().markInterested();
        this.cursorTrack.canHoldNoteData().markInterested();
        this.clipSlotBank = cursorTrack.clipLauncherSlotBank();
        this.cursorClip = cursorTrack.createLauncherCursorClip("MELODIC_STEP_CLIP", "MELODIC_STEP_CLIP", STEP_COUNT, 128);
        this.cursorClip.scrollToKey(0);
        this.cursorClip.scrollToStep(0);
        this.cursorClip.addNoteStepObserver(this::handleNoteStepObject);
        this.cursorClip.playingStep().addValueObserver(this::handlePlayingStep);
        this.cursorClip.getLoopLength().markInterested();
        this.cursorClip.getLoopLength().addValueObserver(length -> {
            loopSteps = Math.max(1, Math.min(STEP_COUNT, (int) Math.round(length / STEP_LENGTH)));
            rebuildCachedPattern();
        });
        final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice("MELODIC_STEP_DEVICE",
                "Melodic Step Device", 8, CursorDeviceFollowMode.FOLLOW_SELECTION);
        this.remoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8);
        for (int i = 0; i < remoteControlsPage.getParameterCount(); i++) {
            final Parameter parameter = remoteControlsPage.getParameter(i);
            parameter.name().markInterested();
            parameter.displayedValue().markInterested();
            parameter.value().markInterested();
        }
        observeSelectedClip();
        this.clipHandler = new ClipRowHandler(this);
        bindPads();
        bindButtons();
        bindMainEncoder();
        this.encoderBankLayout = createEncoderBankLayout();
        this.encoderLayer = new StepSequencerEncoderHandler(this, driver);
        this.seed = driver.initialMelodicSeed();
        this.poolLayoutRootPitch = nearestPhraseRootPitch(phraseContext().baseMidiNote());
    }

    private void bindPads() {
        final var pads = driver.getRgbButtons();
        for (int index = 0; index < pads.length; index++) {
            final int padIndex = index;
            pads[index].bindPressed(this, pressed -> handlePadPress(padIndex, pressed), () -> getPadLight(padIndex));
        }
    }

    private void bindButtons() {
        final BiColorButton bankLeftButton = driver.getButton(NoteAssign.BANK_L);
        bankLeftButton.bindPressed(this, pressed -> {
            if (pressed) {
                if (driver.isGlobalAltHeld()) {
                    applyHalveLength();
                } else {
                    applyTransform(patternState.currentPattern().rotated(-1), "Rotate", "Left", false);
                }
            }
        }, this::bankLightState);

        final BiColorButton bankRightButton = driver.getButton(NoteAssign.BANK_R);
        bankRightButton.bindPressed(this, pressed -> {
            if (pressed) {
                if (driver.isGlobalAltHeld()) {
                    applyRepeatDouble();
                } else {
                    applyTransform(patternState.currentPattern().rotated(1), "Rotate", "Right", false);
                }
            }
        }, this::bankLightState);

        driver.getButton(NoteAssign.MUTE_1).bindPressed(this, pressed -> {
            if (pressed) {
                selectHeld.set(true);
                oled.valueInfo("Select", "Load clip");
            } else {
                selectHeld.set(false);
                oled.clearScreenDelayed();
            }
        }, () -> selectHeld.get() ? BiColorLightState.GREEN_FULL : BiColorLightState.GREEN_HALF);

        driver.getButton(NoteAssign.MUTE_2).bindPressed(this, pressed -> {
            fixedLengthHeld.set(pressed);
            if (pressed) {
                oled.valueInfo("Last Step", "Target step");
            } else {
                oled.clearScreenDelayed();
            }
        }, () -> fixedLengthHeld.get() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF);

        driver.getButton(NoteAssign.MUTE_3).bindPressed(this, pressed -> {
            if (pressed) {
                copyHeld.set(true);
                oled.valueInfo("Paste", "Clip target");
            } else {
                copyHeld.set(false);
                oled.clearScreenDelayed();
            }
        }, () -> copyHeld.get() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF);

        driver.getButton(NoteAssign.MUTE_4).bindPressed(this, pressed -> {
            if (pressed) {
                deleteHeld.set(true);
                oled.valueInfo("Delete", "Clip / step target");
            } else {
                deleteHeld.set(false);
                oled.clearScreenDelayed();
            }
        }, () -> deleteHeld.get() ? BiColorLightState.RED_FULL : BiColorLightState.OFF);
    }

    private void bindMainEncoder() {
        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.bindEncoder(this, this::handleMainEncoder);
        mainEncoder.bindTouched(this, this::handleMainEncoderPress);
    }

    public void notifyBlink(final int blinkTicks) {
        clipHandler.notifyBlink(blinkTicks);
    }

    public void handleStepButton(final boolean pressed) {
        if (driver.isGlobalAltHeld()) {
            if (pressed) {
                driver.toggleFillMode();
                oled.valueInfo("Fill", driver.isFillModeActive() ? "On" : "Off");
            }
            return;
        }
        final AccentLatchState.Transition transition = accentState.handlePressed(pressed);
        if (transition == AccentLatchState.Transition.PRESSED) {
            oled.valueInfo("Accent", accentState.isActive() ? "On" : "Off");
            return;
        }
        if (transition == AccentLatchState.Transition.TOGGLED_ON_RELEASE) {
            oled.valueInfo("Accent", accentState.isActive() ? "On" : "Off");
            return;
        }
        oled.clearScreenDelayed();
    }

    private void handlePadPress(final int padIndex, final boolean pressed) {
        if (!heldSteps.isEmpty() && padIndex < CLIP_ROW_PAD_COUNT && handleRecurrencePadPress(padIndex, pressed)) {
            return;
        }
        if (padIndex < CLIP_ROW_PAD_COUNT) {
            clipHandler.handlePadPress(padIndex, pressed);
            return;
        }
        if (padIndex < STEP_PAD_OFFSET && !pressed) {
            stopPitchPoolAudition(padIndex - PITCH_POOL_PAD_OFFSET);
            return;
        }
        if (padIndex >= STEP_PAD_OFFSET && !pressed) {
            final int stepIndex = padIndex - STEP_PAD_OFFSET;
            final boolean accentGesture = accentState.isHeld() || accentState.isActive();
            if (heldSteps.remove(stepIndex)) {
                if (!heldStepConsumed && heldSteps.isEmpty() && !accentGesture && !fixedLengthHeld.get() && !deleteHeld.get()) {
                    toggleStep(stepIndex);
                }
                heldStep = heldSteps.isEmpty() ? null : heldSteps.iterator().next();
                if (heldSteps.isEmpty()) {
                    heldStepConsumed = false;
                    recurrenceSpanAnchorHeld = false;
                    recurrenceSpanGestureUsed = false;
                }
            }
            return;
        }
        if (!pressed) {
            return;
        }
        if (padIndex < STEP_PAD_OFFSET) {
            togglePitchPoolPad(padIndex - PITCH_POOL_PAD_OFFSET);
            return;
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
        final boolean accentGesture = accentState.isHeld() || accentState.isActive();
        heldSteps.add(stepIndex);
        heldStep = stepIndex;
        heldStepConsumed = heldSteps.size() > 1;
        if (accentGesture) {
            heldStepConsumed = true;
            if (accentState.isHeld()) {
                accentState.markModified();
            }
            toggleAccent(stepIndex);
            return;
        }
        if (fixedLengthHeld.get()) {
            heldStepConsumed = true;
            setLoopSteps(stepIndex + 1);
            return;
        }
        if (copyHeld.get()) {
            heldStepConsumed = true;
            oled.valueInfo("Paste", "Clip row only");
            return;
        }
        if (deleteHeld.get()) {
            heldStepConsumed = true;
            clearStep(stepIndex);
            return;
        }
        selectedStep = stepIndex;
        final MelodicPattern.Step step = patternState.currentPattern().step(stepIndex);
        oled.valueInfo("Step " + (stepIndex + 1),
                step.active() && step.pitch() != null ? Integer.toString(step.pitch()) : "Rest");
    }

    private void togglePitchPoolPad(final int padIndex) {
        final int pitch = pitchPoolPitch(padIndex);
        if (pitch < 0) {
            return;
        }
        startPitchPoolAudition(pitch);
        if (heldStep != null) {
            heldStepConsumed = true;
            assignPitchToStep(heldStep, pitch);
            return;
        }
        if (allowedPitches.contains(pitch)) {
            allowedPitches.remove(pitch);
            oled.valueInfo("Pool -", pitchName(pitch));
        } else {
            allowedPitches.add(pitch);
            oled.valueInfo("Pool +", pitchName(pitch));
        }
        poolUserEdited = true;
    }

    private void startPitchPoolAudition(final int pitch) {
        if (!driver.isStepSeqPadAuditionEnabled() || pitch < 0 || pitch > 127 || auditioningPoolPitches.contains(pitch)) {
            return;
        }
        noteInput.sendRawMidiEvent(Midi.NOTE_ON, pitch, AUDITION_VELOCITY);
        auditioningPoolPitches.add(pitch);
    }

    private void stopPitchPoolAudition(final int padIndex) {
        if (!driver.isStepSeqPadAuditionEnabled()) {
            return;
        }
        final int pitch = pitchPoolPitch(padIndex);
        if (!auditioningPoolPitches.remove(pitch)) {
            return;
        }
        noteInput.sendRawMidiEvent(Midi.NOTE_OFF, pitch, 0);
    }

    private void stopPitchPoolAuditions() {
        if (auditioningPoolPitches.isEmpty()) {
            return;
        }
        for (final int pitch : auditioningPoolPitches) {
            noteInput.sendRawMidiEvent(Midi.NOTE_OFF, pitch, 0);
        }
        auditioningPoolPitches.clear();
    }

    private void assignPitchToStep(final int stepIndex, final int pitch) {
        final MelodicPattern.Step step = ensureStep(stepIndex);
        applyPattern(patternState.currentPattern().withStep(step.withPitch(pitch)), "Pitch", pitchName(pitch));
    }

    private boolean handleRecurrencePadPress(final int padIndex, final boolean pressed) {
        if (heldSteps.isEmpty() || padIndex >= 8) {
            return false;
        }
        if (padIndex == 0 && !pressed && recurrenceSpanAnchorHeld) {
            recurrenceSpanAnchorHeld = false;
            if (recurrenceSpanGestureUsed) {
                recurrenceSpanGestureUsed = false;
                return true;
            }
        } else if (!pressed) {
            return true;
        }
        final List<Integer> targets = heldRecurrenceTargets();
        if (targets.isEmpty()) {
            return true;
        }
        heldStepConsumed = true;
        if (padIndex == 0) {
            recurrenceSpanAnchorHeld = true;
            recurrenceSpanGestureUsed = false;
            return true;
        }
        if (recurrenceSpanAnchorHeld) {
            recurrenceSpanGestureUsed = true;
            applyHeldRecurrenceSpan(targets, padIndex + 1);
            return true;
        }
        final MelodicPattern.Step step = patternState.currentPattern().step(targets.get(0));
        final RecurrencePattern recurrence = recurrenceOf(step);
        final int span = recurrence.effectiveSpan();
        if (padIndex >= span) {
            return true;
        }
        final RecurrencePattern updated = recurrence.toggledAt(padIndex);
        applyHeldRecurrence(targets, recurrencePattern -> recurrencePattern.toggledAt(padIndex), "Recurrence");
        return true;
    }

    private void setView(final View newView) {
        view = newView;
        oled.valueInfo("View", newView.label);
    }

    private void setGenerator(final Generator newGenerator) {
        generator = newGenerator;
        loadGeneratorDefaults(newGenerator);
        oled.valueInfo("Generate", newGenerator.label);
    }

    private MelodicGenerator activeGenerator() {
        return switch (generator) {
            case MOTIF -> motifGenerator;
            case CALL_RESPONSE -> callResponseGenerator;
            case ACID -> acidGenerator;
            case ROLLING -> rollingBassGenerator;
            case OCTAVE -> octaveJumpGenerator;
        };
    }

    private void loadGeneratorDefaults(final Generator selectedGenerator) {
        switch (selectedGenerator) {
            case MOTIF -> {
                density = 0.40;
                timeVariance = 0.0;
                tension = 0.25;
                octaveActivity = 1.0;
                legato = 0.10;
            }
            case CALL_RESPONSE -> {
                density = 0.46;
                timeVariance = 0.0;
                tension = 0.28;
                octaveActivity = 1.0;
                legato = 0.08;
            }
            case ACID -> {
                density = 0.52;
                timeVariance = 0.0;
                tension = 0.62;
                octaveActivity = 1.0;
                legato = 0.36;
            }
            case ROLLING -> {
                density = 1.0;
                timeVariance = 0.0;
                tension = 0.24;
                octaveActivity = 1.0;
                legato = 0.0;
            }
            case OCTAVE -> {
                density = 0.48;
                timeVariance = 0.0;
                tension = 0.22;
                octaveActivity = 0.60;
                legato = 0.06;
            }
        }
    }

    private void generatePitchPool() {
        buildGeneratedPitchPool(seed, true);
        revoiceCurrentPatternToPool("Pool", "New");
    }

    private void mutatePitchPool() {
        final List<Integer> layout = pitchPoolLayoutPitches();
        if (layout.isEmpty()) {
            generatePitchPool();
            return;
        }
        if (allowedPitches.isEmpty()) {
            generatePitchPool();
            return;
        }
        final long mutationSeed = seed;
        final Random random = new Random(mutationSeed);
        final LinkedHashSet<Integer> mutated = new LinkedHashSet<>(allowedPitches);
        final List<Integer> ordered = new ArrayList<>(layout);
        List<Integer> selected = new ArrayList<>(mutated);
        Collections.sort(selected);

        final int budget = mutateIntensity >= 0.75 ? 2 : 1;
        for (int i = 0; i < budget; i++) {
            final double actionRoll = random.nextDouble();
            if (actionRoll < 0.45 && selected.size() > 2) {
                movePoolNote(mutated, ordered, selected, random);
            } else if (actionRoll < 0.75 && selected.size() > 2) {
                removePoolNote(mutated, selected, random);
            } else {
                addPoolNote(mutated, ordered, selected, random);
            }
            selected = new ArrayList<>(mutated);
            Collections.sort(selected);
        }

        allowedPitches.clear();
        allowedPitches.addAll(mutated);
        poolUserEdited = true;
        seed = nextSeed(mutationSeed);
        revoiceCurrentPatternToPool("Pool", "Mutated");
    }

    private void movePoolNote(final LinkedHashSet<Integer> mutated, final List<Integer> ordered,
                              final List<Integer> selected, final Random random) {
        final Integer source = chooseMutablePoolPitch(selected, random);
        if (source == null) {
            addPoolNote(mutated, ordered, selected, random);
            return;
        }
        mutated.remove(source);
        final int sourceIndex = nearestPitchIndex(source, ordered);
        final int maxOffset = Math.max(1, 1 + (int) Math.round(tension * 5));
        final int direction = random.nextBoolean() ? 1 : -1;
        final int offset = 1 + random.nextInt(maxOffset);
        final int candidateIndex = Math.max(0, Math.min(ordered.size() - 1, sourceIndex + direction * offset));
        mutated.add(ordered.get(candidateIndex));
    }

    private void removePoolNote(final LinkedHashSet<Integer> mutated, final List<Integer> selected, final Random random) {
        final Integer removed = chooseMutablePoolPitch(selected, random);
        if (removed != null) {
            mutated.remove(removed);
        }
    }

    private void addPoolNote(final LinkedHashSet<Integer> mutated, final List<Integer> ordered,
                             final List<Integer> selected, final Random random) {
        final int anchorPitch = selected.isEmpty()
                ? ordered.get(nearestPitchIndex(phraseContext().baseMidiNote(), ordered))
                : selected.get(random.nextInt(selected.size()));
        final int anchorIndex = nearestPitchIndex(anchorPitch, ordered);
        final int maxOffset = Math.max(1, 1 + (int) Math.round(tension * 4));
        final int direction = random.nextBoolean() ? 1 : -1;
        final int offset = 1 + random.nextInt(maxOffset);
        final int candidateIndex = Math.max(0, Math.min(ordered.size() - 1, anchorIndex + direction * offset));
        mutated.add(ordered.get(candidateIndex));
    }

    private Integer chooseMutablePoolPitch(final List<Integer> selected, final Random random) {
        if (selected.isEmpty()) {
            return null;
        }
        final Set<Integer> protectedPitches = protectedPoolPitches();
        final List<Integer> mutable = selected.stream()
                .filter(pitch -> !protectedPitches.contains(pitch))
                .toList();
        final List<Integer> source = mutable.isEmpty() ? selected : mutable;
        return source.get(random.nextInt(source.size()));
    }

    private Set<Integer> protectedPoolPitches() {
        final Set<Integer> protectedPitches = new HashSet<>();
        for (int i = 0; i < patternState.currentPattern().loopSteps(); i++) {
            final MelodicPattern.Step step = patternState.currentPattern().step(i);
            if (!step.active() || step.pitch() == null) {
                continue;
            }
            if (i == 0 || i % 4 == 0 || i == patternState.currentPattern().loopSteps() - 1) {
                protectedPitches.add(nearestAllowedPitch(step.pitch()));
            }
        }
        return protectedPitches;
    }

    private void buildGeneratedPitchPool(final long poolSeed, final boolean advanceSeed) {
        final Random random = new Random(poolSeed);
        final List<Integer> layout = pitchPoolLayoutPitches();
        if (layout.isEmpty()) {
            allowedPitches.clear();
            allowedPitches.add(phraseContext().baseMidiNote());
            oled.valueInfo("Pool", "Base note");
            if (advanceSeed) {
                seed = nextSeed(poolSeed);
            }
            return;
        }
        final int baseIndex = nearestPitchIndex(phraseContext().baseMidiNote(), layout);
        final int rootPitch = nearestLayoutRootPitch(layout);
        final LinkedHashSet<Integer> generatedPool = new LinkedHashSet<>();
        if (generator == Generator.ROLLING) {
            final MelodicPhraseContext context = phraseContext();
            final int rollingCenter = Math.max(0, context.baseMidiNote() - 12);
            final int rollingBaseIndex = nearestPitchIndex(rollingCenter, layout);
            final int rollingRootPitch = nearestLayoutRootPitch(layout, rollingCenter);
            final int rollingRootIndex = nearestPitchIndex(rollingRootPitch, layout);
            final int family = random.nextInt(4);
            generatedPool.add(layout.get(rollingRootIndex));
            generatedPool.add(layout.get(rollingBaseIndex));
            switch (family) {
                case 0 -> {
                    generatedPool.add(layout.get(Math.min(layout.size() - 1, rollingRootIndex + 1)));
                    generatedPool.add(layout.get(Math.min(layout.size() - 1, rollingRootIndex + 2)));
                    if (random.nextDouble() < 0.45 && rollingRootIndex > 0) {
                        generatedPool.add(layout.get(rollingRootIndex - 1));
                    }
                    generatedPool.add(layout.get(Math.min(layout.size() - 1, rollingRootIndex + 4)));
                }
                case 1 -> {
                    if (rollingRootIndex > 0) {
                        generatedPool.add(layout.get(rollingRootIndex - 1));
                    }
                    generatedPool.add(layout.get(Math.min(layout.size() - 1, rollingRootIndex + 1)));
                    generatedPool.add(layout.get(Math.min(layout.size() - 1, rollingRootIndex + 3)));
                    generatedPool.add(layout.get(Math.min(layout.size() - 1, rollingRootIndex + 5)));
                }
                case 2 -> {
                    generatedPool.add(layout.get(Math.min(layout.size() - 1, rollingRootIndex + 2)));
                    generatedPool.add(layout.get(Math.min(layout.size() - 1, rollingRootIndex + 4)));
                    if (random.nextDouble() < 0.35) {
                        generatedPool.add(layout.get(Math.min(layout.size() - 1, rollingRootIndex + 1)));
                    }
                    if (rollingRootIndex > 0) {
                        generatedPool.add(layout.get(rollingRootIndex - 1));
                    }
                }
                default -> {
                    if (rollingRootIndex > 0) {
                        generatedPool.add(layout.get(rollingRootIndex - 1));
                    }
                    generatedPool.add(layout.get(Math.min(layout.size() - 1, rollingRootIndex + 1)));
                    generatedPool.add(layout.get(Math.min(layout.size() - 1, rollingRootIndex + 2)));
                    generatedPool.add(layout.get(Math.min(layout.size() - 1, rollingRootIndex + 4)));
                    generatedPool.add(layout.get(Math.min(layout.size() - 1, rollingRootIndex + 6)));
                }
            }
            if (tension >= 0.65 && density >= 0.9 && random.nextDouble() < 0.35) {
                generatedPool.add(layout.get(Math.min(layout.size() - 1, rollingRootIndex + 5)));
            }
            allowedPitches.clear();
            allowedPitches.addAll(generatedPool);
            poolUserEdited = false;
            poolGeneratorSource = generator;
            oled.valueInfo("Pool", "%d notes".formatted(allowedPitches.size()));
            driver.notifyPopup("Pitch Pool", "%d notes".formatted(allowedPitches.size()));
            if (advanceSeed) {
                seed = nextSeed(poolSeed);
            }
            return;
        }
        final int[] offsetPool = switch (generator) {
            case MOTIF -> new int[]{0, 1, 2, 3, 4, 5, 6};
            case CALL_RESPONSE -> new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
            case ACID -> new int[]{0, 1, 2, 3, 4, 5, 6, 7};
            case ROLLING -> new int[]{0, 1, 2, 3, 4, 5, 6};
            case OCTAVE -> new int[]{0, 2, 4, 7, 9, 11, 13};
        };
        final int targetCount = switch (generator) {
            case MOTIF -> 3 + random.nextInt(tension >= 0.5 ? 3 : 2);
            case CALL_RESPONSE -> 4 + random.nextInt(tension >= 0.5 ? 3 : 2);
            case ACID -> 5 + random.nextInt(2 + (tension >= 0.6 ? 1 : 0));
            case ROLLING -> 3 + random.nextInt(2 + (density >= 0.65 ? 1 : 0));
            case OCTAVE -> 3 + random.nextInt(2);
        };
        generatedPool.add(layout.get(baseIndex));
        generatedPool.add(rootPitch);
        final List<Integer> shuffledOffsets = new ArrayList<>(offsetPool.length);
        for (final int offset : offsetPool) {
            shuffledOffsets.add(offset);
        }
        Collections.shuffle(shuffledOffsets, random);
        for (final int offset : shuffledOffsets) {
            if (generatedPool.size() >= targetCount) {
                break;
            }
            final int direction = switch (generator) {
                case OCTAVE -> random.nextBoolean() ? -1 : 1;
                case ACID -> random.nextDouble() < 0.35 ? -1 : 1;
                default -> 1;
            };
            final int index = Math.max(0, Math.min(layout.size() - 1, baseIndex + offset * direction));
            generatedPool.add(layout.get(index));
        }
        if (generatedPool.isEmpty()) {
            generatedPool.add(layout.get(baseIndex));
        }
        allowedPitches.clear();
        allowedPitches.addAll(generatedPool);
        poolUserEdited = false;
        poolGeneratorSource = generator;
        oled.valueInfo("Pool", "%d notes".formatted(allowedPitches.size()));
        driver.notifyPopup("Pitch Pool", "%d notes".formatted(allowedPitches.size()));
        if (advanceSeed) {
            seed = nextSeed(poolSeed);
        }
    }

    private int nearestLayoutRootPitch(final List<Integer> layout) {
        return nearestLayoutRootPitch(layout, phraseContext().baseMidiNote());
    }

    private int nearestLayoutRootPitch(final List<Integer> layout, final int targetPitch) {
        final MelodicPhraseContext context = phraseContext();
        int bestPitch = layout.get(0);
        int bestDistance = Integer.MAX_VALUE;
        for (final int pitch : layout) {
            if (!context.scale().isRootMidiNote(context.rootNote(), pitch)) {
                continue;
            }
            final int distance = Math.abs(pitch - targetPitch);
            if (distance < bestDistance || (distance == bestDistance && pitch < bestPitch)) {
                bestPitch = pitch;
                bestDistance = distance;
            }
        }
        return bestPitch;
    }

    private void generatePattern() {
        if (!ensureClipAvailable()) {
            return;
        }
        final long generationSeed = seed;
        final MelodicPhraseContext context = phraseContext();
        if (allowedPitches.isEmpty() || (!poolUserEdited && poolGeneratorSource != generator)) {
            buildGeneratedPitchPool(generationSeed, false);
        }
        final MelodicGenerator.GenerateParameters parameters = generatorParametersForCurrentEngine(generationSeed);
        final MelodicGenerator activeGenerator = activeGenerator();
        MelodicPattern generated = activeGenerator.generate(context, parameters);
        generated = enrichLatentSteps(generated);
        generated = constrainPatternToPool(generated);
        patternState.setBasePattern(generated);
        final String familyLabel = activeGenerator.lastFamilyLabel();
        applyPattern(generated, "Generate",
                familyLabel == null || familyLabel.isBlank()
                        ? shortGeneratorLabel(generator)
                        : "%s.%s".formatted(shortGeneratorLabel(generator), familyLabel));
        seed = nextSeed(generationSeed);
    }

    private String shortGeneratorLabel(final Generator selectedGenerator) {
        return switch (selectedGenerator) {
            case MOTIF -> "Mtf";
            case CALL_RESPONSE -> "C&R";
            case ACID -> "Acd";
            case ROLLING -> "Rol";
            case OCTAVE -> "Oct";
        };
    }

    private MelodicGenerator.GenerateParameters generatorParametersForCurrentEngine(final long generationSeed) {
        return switch (generator) {
            case MOTIF -> new MelodicGenerator.GenerateParameters(
                    loopSteps, density, tension, octaveActivity, legato, euclideanPulses, euclideanRotation, timeVariance, generationSeed);
            case CALL_RESPONSE -> new MelodicGenerator.GenerateParameters(
                    loopSteps, Math.max(0.35, density), Math.max(0.2, tension),
                    octaveActivity, legato, euclideanPulses, euclideanRotation, timeVariance, generationSeed);
            case ACID -> new MelodicGenerator.GenerateParameters(
                    loopSteps,
                    Math.max(0.35, density),
                    Math.max(0.55, tension),
                    Math.max(0.15, octaveActivity),
                    legato,
                    Math.max(4, euclideanPulses),
                    euclideanRotation,
                    timeVariance,
                    generationSeed);
            case ROLLING -> new MelodicGenerator.GenerateParameters(
                    loopSteps,
                    density,
                    Math.max(0.2, tension),
                    Math.min(0.25, Math.max(0.05, octaveActivity)),
                    legato,
                    Math.max(6, euclideanPulses),
                    euclideanRotation,
                    timeVariance,
                    generationSeed);
            case OCTAVE -> new MelodicGenerator.GenerateParameters(
                    loopSteps,
                    Math.max(0.35, density),
                    Math.max(0.15, tension),
                    Math.max(0.45, octaveActivity),
                    legato,
                    Math.max(4, euclideanPulses),
                    euclideanRotation,
                    timeVariance,
                    generationSeed);
        };
    }

    private void mutatePattern(final boolean fromOriginalPattern) {
        if (!ensureClipAvailable()) {
            return;
        }
        final MelodicPattern sourcePattern = fromOriginalPattern ? patternState.basePattern() : patternState.currentPattern();
        if (activeStepCount(sourcePattern) == 0) {
            generatePattern();
            return;
        }
        final long mutationSeed = seed;
        MelodicPattern mutated = mutator.mutate(sourcePattern, phraseContext(),
                mutationMode, mutateIntensity, 0.7, mutationSeed);
        mutated = enrichLatentSteps(mutated);
        mutated = mutationMode == MelodicMutator.Mode.PRESERVE_RHYTHM
                ? revoicePatternToPoolVariant(mutated, mutateIntensity, mutationSeed)
                : constrainPatternToPoolLocally(mutated);
        patternState.setBasePattern(mutated);
        applyPattern(mutated, fromOriginalPattern ? "Mutate Orig" : "Mutate", mutationLabel(mutationMode));
        seed = nextSeed(mutationSeed);
    }

    private void toggleStep(final int stepIndex) {
        final MelodicPattern.Step step = patternState.currentPattern().step(stepIndex);
        if (step.active()) {
            clearStep(stepIndex);
            return;
        }
        final MelodicPattern.Step created = step.pitch() != null
                ? applyCurrentAccentState(step.withActive(true))
                : restoreGeneratedStepOrDefault(stepIndex);
        applyPattern(patternState.currentPattern().withStep(created), "Step", Integer.toString(stepIndex + 1));
    }

    private void clearStep(final int stepIndex) {
        final MelodicPattern.Step current = patternState.currentPattern().step(stepIndex);
        final MelodicPattern.Step hidden = current.pitch() != null
                ? current.withActive(false)
                : MelodicPattern.Step.rest(stepIndex);
        MelodicPattern pattern = patternState.currentPattern().withStep(hidden);
        if (stepIndex + 1 < STEP_COUNT && pattern.step(stepIndex + 1).tieFromPrevious()) {
            pattern = pattern.withStep(pattern.step(stepIndex + 1).withTieFromPrevious(false));
        }
        applyPattern(pattern, "Clear", "Step " + (stepIndex + 1));
    }

    private void toggleAccent(final int stepIndex) {
        final MelodicPattern.Step step = ensureStep(stepIndex);
        applyPattern(patternState.currentPattern().withStep(step.withAccent(!step.accent())
                        .withVelocity(step.accent() ? DEFAULT_VELOCITY : 118)),
                "Accent", "Step " + (stepIndex + 1));
    }

    private void toggleTie(final int stepIndex) {
        final MelodicPattern.Step step = ensureStep(stepIndex);
        if (stepIndex + 1 >= STEP_COUNT) {
            return;
        }
        final MelodicPattern.Step next = patternState.currentPattern().step(stepIndex + 1);
        final boolean newTie = !next.tieFromPrevious();
        MelodicPattern pattern = patternState.currentPattern().withStep(step);
        pattern = pattern.withStep(next.withTieFromPrevious(newTie));
        applyPattern(pattern, "Tie", newTie ? "On" : "Off");
    }

    private void toggleSlide(final int stepIndex) {
        final MelodicPattern.Step step = ensureStep(stepIndex);
        applyPattern(patternState.currentPattern().withStep(step.withSlide(!step.slide()).withGate(step.slide() ? DEFAULT_GATE : 1.05)),
                "Slide", step.slide() ? "Off" : "On");
    }

    private MelodicPattern.Step ensureStep(final int stepIndex) {
        return patternState.ensureStep(stepIndex, () -> defaultStep(stepIndex));
    }

    private MelodicPattern.Step restoreGeneratedStepOrDefault(final int stepIndex) {
        return applyCurrentAccentState(patternState.restoreGeneratedStepOrDefault(stepIndex, () -> defaultStep(stepIndex)));
    }

    private MelodicPattern.Step defaultStep(final int stepIndex) {
        final int pitch = defaultPoolPitch();
        return applyCurrentAccentState(new MelodicPattern.Step(stepIndex, true, false, pitch,
                DEFAULT_VELOCITY, DEFAULT_GATE, false, false));
    }

    private MelodicPattern.Step applyCurrentAccentState(final MelodicPattern.Step step) {
        return accentState.isActive()
                ? step.withAccent(true).withVelocity(118)
                : step.withAccent(false).withVelocity(DEFAULT_VELOCITY);
    }

    private void setLoopSteps(final int newLoopSteps) {
        loopSteps = Math.max(1, Math.min(STEP_COUNT, newLoopSteps));
        applyPattern(patternState.currentPattern().withLoopSteps(loopSteps), "Loop", Integer.toString(loopSteps));
    }

    private void applyPattern(final MelodicPattern pattern, final String label, final String value) {
        if (!ensureClipAvailable()) {
            return;
        }
        refreshClipCursor();
        loopSteps = pattern.loopSteps();
        patternState.setCurrentPattern(pattern.withLoopSteps(loopSteps));
        clipWriter.writeToClip(cursorClip, patternState.currentPattern(), STEP_LENGTH);
        oled.valueInfo(label, value);
        driver.notifyPopup(label, value);
    }

    private void applyStepRecurrence(final int stepIndex, final MelodicPattern.Step updated, final String label) {
        if (!ensureClipAvailable()) {
            return;
        }
        final NoteStep liveStep = primaryNoteStepAt(stepIndex);
        if (liveStep == null) {
            oled.valueInfo(label, "No note");
            return;
        }
        patternState.setCurrentPattern(patternState.currentPattern().withStep(updated));
        patternState.setBasePattern(patternState.basePattern().withStep(updated.withIndex(stepIndex)));
        liveStep.setRecurrence(updated.bitwigRecurrenceLength(), updated.bitwigRecurrenceMask());
        oled.valueInfo(label, recurrenceOf(updated).summary());
        driver.notifyPopup(label, recurrenceOf(updated).summary());
    }

    private void applyTransform(final MelodicPattern pattern, final String label, final String value,
                                final boolean syncPoolFromPattern) {
        if (syncPoolFromPattern) {
            seedPitchPoolFromPattern(pattern);
            poolUserEdited = true;
        }
        patternState.setBasePattern(pattern);
        applyPattern(pattern, label, value);
    }

    private void revoiceCurrentPatternToPool(final String label, final String value) {
        if (!ensureClipAvailable()) {
            return;
        }
        final MelodicPattern source = activeStepCount(patternState.currentPattern()) > 0 ? patternState.currentPattern() : patternState.basePattern();
        if (activeStepCount(source) == 0) {
            oled.valueInfo(label, value);
            driver.notifyPopup(label, value);
            return;
        }
        final MelodicPattern revoiced = constrainPatternToPool(source);
        patternState.setBasePattern(revoiced);
        applyPattern(revoiced, label, value);
    }

    private void adjustSelectedPitch(final int amount) {
        if (amount == 0) {
            return;
        }
        final int stepIndex = editingStepIndex();
        final MelodicPattern.Step step = ensureStep(stepIndex);
        final int currentPitch = step.pitch() == null ? phraseContext().baseMidiNote() : step.pitch();
        applyPattern(patternState.currentPattern().withStep(step.withPitch(Math.max(0, Math.min(127, currentPitch + amount)))),
                "Pitch", Integer.toString(currentPitch + amount));
    }

    private void adjustSelectedOctave(final int amount) {
        if (amount == 0) {
            return;
        }
        if (heldSteps.size() > 1) {
            applyHeldStepValueEdit(heldEditableTargets(),
                    step -> step.withPitch(Math.max(0, Math.min(127, step.pitch() + amount * 12))),
                    "Octave", "%+d oct".formatted(amount));
            return;
        }
        final int stepIndex = editingStepIndex();
        final MelodicPattern.Step step = ensureStep(stepIndex);
        final int currentPitch = step.pitch() == null ? phraseContext().baseMidiNote() : step.pitch();
        final int shiftedPitch = Math.max(0, Math.min(127, currentPitch + amount * 12));
        applyPattern(patternState.currentPattern().withStep(step.withPitch(shiftedPitch)),
                "Octave", pitchName(shiftedPitch));
    }

    private void adjustSelectedGate(final int amount) {
        if (amount == 0) {
            return;
        }
        if (heldSteps.size() > 1) {
            applyHeldStepValueEdit(heldEditableTargets(),
                    step -> step.withGate(step.gate() + amount * 0.05),
                    "Gate Len", "%+.2f".formatted(amount * 0.05));
            return;
        }
        final MelodicPattern.Step step = ensureStep(editingStepIndex());
        applyPattern(patternState.currentPattern().withStep(step.withGate(step.gate() + amount * 0.05)),
                "Gate Len", "%.2f".formatted(step.gate() + amount * 0.05));
    }

    private void adjustSelectedVelocity(final int amount) {
        if (amount == 0) {
            return;
        }
        if (heldSteps.size() > 1) {
            applyHeldStepValueEdit(heldEditableTargets(),
                    step -> step.withVelocity(step.velocity() + amount),
                    "Velocity", "%+d".formatted(amount));
            return;
        }
        final MelodicPattern.Step step = ensureStep(editingStepIndex());
        applyPattern(patternState.currentPattern().withStep(step.withVelocity(step.velocity() + amount)),
                "Velocity", Integer.toString(step.velocity() + amount));
    }

    private void adjustSelectedChance(final int amount) {
        if (amount == 0) {
            return;
        }
        if (heldSteps.size() > 1) {
            applyHeldStepValueEdit(heldEditableTargets(),
                    step -> step.withChance(step.chance() + amount * 0.05),
                    "Chance", "%+d%%".formatted(amount * 5));
            return;
        }
        final MelodicPattern.Step step = ensureStep(editingStepIndex());
        final double nextChance = Math.max(0.0, Math.min(1.0, step.chance() + amount * 0.05));
        applyPattern(patternState.currentPattern().withStep(step.withChance(nextChance)),
                "Chance", "%d%%".formatted((int) Math.round(nextChance * 100.0)));
    }

    private void cycleArticulation(final int amount) {
        if (amount == 0) {
            return;
        }
        final int stepIndex = editingStepIndex();
        final MelodicPattern.Step step = ensureStep(stepIndex);
        final int current = step.tieFromPrevious() ? 3 : step.slide() ? 2 : step.accent() ? 1 : 0;
        final int next = Math.floorMod(current + amount, 4);
        MelodicPattern pattern = patternState.currentPattern().withStep(step.withAccent(false).withSlide(false));
        if (stepIndex + 1 < STEP_COUNT) {
            pattern = pattern.withStep(pattern.step(stepIndex + 1).withTieFromPrevious(false));
        }
        pattern = switch (next) {
            case 1 -> pattern.withStep(pattern.step(stepIndex).withAccent(true).withVelocity(118));
            case 2 -> pattern.withStep(pattern.step(stepIndex).withSlide(true).withGate(1.05));
            case 3 -> stepIndex + 1 < STEP_COUNT
                    ? pattern.withStep(pattern.step(stepIndex + 1).withTieFromPrevious(true))
                    : pattern;
            default -> pattern;
        };
        final String label = switch (next) {
            case 1 -> "Accent";
            case 2 -> "Slide";
            case 3 -> "Tie";
            default -> "Normal";
        };
        applyPattern(pattern, "Artic", label);
    }

    private void handleMainEncoder(final int inc) {
        if (driver.isPopupBrowserActive()) {
            driver.routeBrowserMainEncoder(inc);
            return;
        }
        driver.markMainEncoderTurned();
        if (driver.handleMainEncoderGlobalChord(inc)) {
            return;
        }
        final boolean fine = driver.isGlobalShiftHeld();
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            noteRepeatHandler.handleMainEncoder(inc, driver.isGlobalAltHeld());
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TEMPO_ROLE.equals(mainEncoderRole)) {
            driver.adjustTempo(inc, fine);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_SHUFFLE_ROLE.equals(mainEncoderRole)) {
            driver.adjustGrooveShuffleAmount(inc, fine);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TRACK_SELECT_ROLE.equals(mainEncoderRole)) {
            driver.adjustSelectedTrack(inc, driver.isMainEncoderPressed());
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
        if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            noteRepeatHandler.handlePressed(pressed);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TEMPO_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showTempoInfo();
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_SHUFFLE_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showGrooveShuffleInfo();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TRACK_SELECT_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showSelectedTrackInfo(false);
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

    private int editingStepIndex() {
        return heldStep != null ? heldStep : selectedStep;
    }

    private void adjustDensity(final int amount) {
        density = clampUnit(density + amount * 0.05);
        oled.valueInfo("Density", "%.2f".formatted(density));
    }

    private void adjustPoolOctave(final int amount) {
        if (amount == 0) {
            return;
        }
        poolLayoutRootPitch = shiftedPoolLayoutRootPitch(amount);
        if (allowedPitches.isEmpty()) {
            buildGeneratedPitchPool(seed, false);
        }
        if (allowedPitches.isEmpty()) {
            oled.valueInfo("Pool Oct", "No pool");
            return;
        }
        final List<Integer> layout = pitchPoolLayoutPitches();
        if (layout.isEmpty()) {
            return;
        }
        final LinkedHashSet<Integer> shifted = new LinkedHashSet<>();
        for (final int pitch : allowedPitches) {
            final int targetPitch = Math.max(0, Math.min(127, pitch + amount * 12));
            shifted.add(nearestLayoutPitch(targetPitch, layout));
        }
        if (shifted.equals(allowedPitches)) {
            oled.valueInfo("Pool Oct", poolOctaveSummary());
            return;
        }
        allowedPitches.clear();
        allowedPitches.addAll(shifted);
        poolUserEdited = true;
        oled.valueInfo("Pool Oct", poolOctaveSummary());
        driver.notifyPopup("Pool Oct", poolOctaveSummary());
    }

    private void adjustTimeVariance(final int amount) {
        timeVariance = clampUnit(timeVariance + amount * 0.05);
        oled.valueInfo("Time Var", "%.2f".formatted(timeVariance));
    }

    private void adjustChannelShape(final int amount) {
        adjustOctaveActivity(amount);
    }

    private String channelShapeLabel() {
        return switch (generator) {
            case ACID -> "Motion";
            case MOTIF -> "Contour";
            case CALL_RESPONSE -> "Answer";
            case ROLLING -> "Movement";
            case OCTAVE -> "Jump";
        };
    }

    private void adjustPostDensity(final int amount) {
        if (amount == 0 || !ensureClipAvailable()) {
            return;
        }
        MelodicPattern pattern = patternState.currentPattern();
        if (amount > 0) {
            for (int i = 0; i < amount; i++) {
                pattern = restoreOnce(pattern);
            }
        } else {
            for (int i = 0; i < -amount; i++) {
                pattern = thinOnce(pattern);
            }
        }
        applyPattern(pattern, "Density", Integer.toString(activeStepCount(pattern)));
    }

    private MelodicPattern thinOnce(final MelodicPattern pattern) {
        final MelodicPatternAnalyzer.Analysis analysis = MelodicPatternAnalyzer.analyze(pattern);
        for (int i = analysis.activeSteps().size() - 1; i >= 0; i--) {
            final int stepIndex = analysis.activeSteps().get(i);
            if (analysis.anchorSteps().contains(stepIndex)) {
                continue;
            }
            MelodicPattern out = pattern.withStep(MelodicPattern.Step.rest(stepIndex));
            if (stepIndex + 1 < STEP_COUNT && out.step(stepIndex + 1).tieFromPrevious()) {
                out = out.withStep(out.step(stepIndex + 1).withTieFromPrevious(false));
            }
            return out;
        }
        return pattern;
    }

    private MelodicPattern restoreOnce(final MelodicPattern pattern) {
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step current = pattern.step(i);
            final MelodicPattern.Step base = patternState.basePattern().step(i);
            if (!current.active() && base.active()) {
                final MelodicPattern.Step restored = allowedPitches.isEmpty() || base.pitch() == null
                        ? base.withIndex(i)
                        : base.withIndex(i).withPitch(nearestAllowedPitch(base.pitch()));
                return pattern.withStep(restored);
            }
        }
        return pattern;
    }

    private void adjustTension(final int amount) {
        tension = clampUnit(tension + amount * 0.05);
        oled.valueInfo("Tension", "%.2f".formatted(tension));
    }

    private void adjustOctaveActivity(final int amount) {
        octaveActivity = clampUnit(octaveActivity + amount * 0.05);
        oled.valueInfo(channelShapeLabel(), "%.2f".formatted(octaveActivity));
    }

    private void adjustLegato(final int amount) {
        legato = clampUnit(legato + amount * 0.05);
        oled.valueInfo("Legato", "%.2f".formatted(legato));
    }

    private void adjustEuclideanPulses(final int amount) {
        euclideanPulses = Math.max(1, Math.min(loopSteps, euclideanPulses + amount));
        oled.valueInfo("Pulses", Integer.toString(euclideanPulses));
    }

    private void adjustEuclideanRotation(final int amount) {
        euclideanRotation = Math.floorMod(euclideanRotation + amount, Math.max(1, loopSteps));
        oled.valueInfo("Rotation", Integer.toString(euclideanRotation));
    }

    private void adjustMutateIntensity(final int amount) {
        mutateIntensity = clampUnit(mutateIntensity + amount * 0.05);
        oled.valueInfo("Mut %", "%.2f".formatted(mutateIntensity));
    }

    private void showRecurrenceEditInfo(final int ignored) {
        final List<Integer> targets = heldRecurrenceTargets();
        if (targets.isEmpty()) {
            oled.valueInfo("Recurrence", "Hold step");
            return;
        }
        final MelodicPattern.Step step = patternState.currentPattern().step(targets.get(0));
        oled.valueInfo("Recurrence", targets.size() == 1
                ? recurrenceOf(step).summary()
                : "%d steps".formatted(targets.size()));
    }

    private void applyHeldRecurrenceSpan(final List<Integer> stepIndices, final int newSpan) {
        applyHeldRecurrence(stepIndices, recurrence -> recurrence.applySpanGesture(newSpan), "Recurrence");
    }

    private void cycleMutationMode(final int direction) {
        final MelodicMutator.Mode[] values = MelodicMutator.Mode.values();
        final int nextIndex = Math.max(0, Math.min(values.length - 1, mutationMode.ordinal() + direction));
        mutationMode = values[nextIndex];
        oled.valueInfo("Mut Type", mutationLabel(mutationMode));
    }

    private void cycleGeneratorSubtype(final int direction) {
        final MelodicGenerator activeGenerator = activeGenerator();
        if (!activeGenerator.supportsSubtypeSelection()) {
            oled.valueInfo("Subtype", "Any");
            return;
        }
        activeGenerator.cycleSubtype(direction);
        oled.valueInfo("Subtype", activeGenerator.currentSubtypeLabel());
    }

    private void adjustSeed(final int amount) {
        seed = Math.max(1, seed + amount);
        oled.valueInfo("Seed", Long.toString(seed));
    }

    private int defaultPoolPitch() {
        if (!allowedPitches.isEmpty()) {
            return allowedPitches.iterator().next();
        }
        return phraseContext().baseMidiNote();
    }

    private String poolOctaveSummary() {
        final int rootPitch = currentPoolLayoutRootPitch();
        return rootPitch < 0 ? "No pool" : pitchName(rootPitch);
    }

    private void seedPitchPoolFromPattern(final MelodicPattern pattern) {
        final List<Integer> layout = pitchPoolLayoutPitches();
        allowedPitches.clear();
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (!step.active() || step.pitch() == null) {
                continue;
            }
            allowedPitches.add(nearestLayoutPitch(step.pitch(), layout));
        }
    }

    private MelodicPattern constrainPatternToPool(final MelodicPattern pattern) {
        if (allowedPitches.isEmpty()) {
            return pattern;
        }
        final List<Integer> orderedPool = new ArrayList<>(allowedPitches);
        Collections.sort(orderedPool);
        final Map<Integer, Integer> broadPoolMapping = orderedPool.size() >= 2
                ? buildBroadPoolMapping(pattern, orderedPool)
                : Map.of();
        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (step.pitch() == null) {
                steps.add(step);
                continue;
            }
            final Integer mappedPitch = broadPoolMapping.get(step.pitch());
            steps.add(step.withPitch(mappedPitch != null ? mappedPitch : nearestAllowedPitch(step.pitch())));
        }
        return new MelodicPattern(steps, pattern.loopSteps());
    }

    private MelodicPattern constrainPatternToPoolLocally(final MelodicPattern pattern) {
        if (allowedPitches.isEmpty()) {
            return pattern;
        }
        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (step.pitch() == null) {
                steps.add(step);
                continue;
            }
            steps.add(step.withPitch(nearestAllowedPitch(step.pitch())));
        }
        return new MelodicPattern(steps, pattern.loopSteps());
    }

    private MelodicPattern revoicePatternToPoolVariant(final MelodicPattern pattern, final double intensity,
                                                       final long seedValue) {
        if (allowedPitches.isEmpty()) {
            return pattern;
        }
        MelodicPattern out = constrainPatternToPoolLocally(pattern);
        final MelodicPatternAnalyzer.Analysis analysis = MelodicPatternAnalyzer.analyze(out);
        final List<Integer> orderedPool = new ArrayList<>(allowedPitches);
        Collections.sort(orderedPool);
        final List<Integer> candidates = new ArrayList<>();
        for (final int stepIndex : analysis.activeSteps()) {
            final MelodicPattern.Step step = out.step(stepIndex);
            if (step.pitch() == null) {
                continue;
            }
            if (analysis.anchorSteps().contains(stepIndex)) {
                continue;
            }
            candidates.add(stepIndex);
        }
        if (candidates.isEmpty()) {
            return out;
        }

        final Random random = new Random(seedValue ^ 0x9E3779B97F4A7C15L);
        Collections.shuffle(candidates, random);
        final int budget = Math.max(1, Math.min(candidates.size(), mutateIntensity >= 0.75 ? 3 : mutateIntensity >= 0.4 ? 2 : 1));
        final Set<Integer> usedPitches = patternPitchSet(out);
        for (int i = 0; i < budget; i++) {
            final int stepIndex = candidates.get(i);
            final MelodicPattern.Step step = out.step(stepIndex);
            final Integer replacement = choosePoolVariantPitch(step.pitch(), orderedPool, usedPitches, random);
            if (replacement == null || replacement.equals(step.pitch())) {
                continue;
            }
            usedPitches.add(replacement);
            out = out.withStep(step.withPitch(replacement));
        }
        return out;
    }

    private Integer choosePoolVariantPitch(final Integer sourcePitch, final List<Integer> orderedPool,
                                           final Set<Integer> usedPitches, final Random random) {
        if (sourcePitch == null || orderedPool.isEmpty()) {
            return null;
        }
        final int currentIndex = nearestPitchIndex(sourcePitch, orderedPool);
        final int[] offsets = {2, -2, 1, -1, 3, -3, 4, -4};
        for (final int offset : offsets) {
            final int candidateIndex = currentIndex + offset;
            if (candidateIndex < 0 || candidateIndex >= orderedPool.size()) {
                continue;
            }
            final Integer candidatePitch = orderedPool.get(candidateIndex);
            if (!candidatePitch.equals(sourcePitch) && !usedPitches.contains(candidatePitch)) {
                return candidatePitch;
            }
        }
        final List<Integer> alternatives = new ArrayList<>();
        for (final Integer candidatePitch : orderedPool) {
            if (!candidatePitch.equals(sourcePitch)) {
                alternatives.add(candidatePitch);
            }
        }
        if (alternatives.isEmpty()) {
            return null;
        }
        alternatives.sort((left, right) -> Integer.compare(Math.abs(left - sourcePitch), Math.abs(right - sourcePitch)));
        final int limit = Math.min(3, alternatives.size());
        return alternatives.get(random.nextInt(limit));
    }

    private Set<Integer> patternPitchSet(final MelodicPattern pattern) {
        final Set<Integer> pitches = new HashSet<>();
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (step.active() && step.pitch() != null) {
                pitches.add(step.pitch());
            }
        }
        return pitches;
    }

    private Map<Integer, Integer> buildBroadPoolMapping(final MelodicPattern pattern, final List<Integer> orderedPool) {
        final List<Integer> distinctGenerated = new ArrayList<>();
        final Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (step.pitch() != null && seen.add(step.pitch())) {
                distinctGenerated.add(step.pitch());
            }
        }
        if (distinctGenerated.size() < 2) {
            return Map.of();
        }
        Collections.sort(distinctGenerated);
        final Map<Integer, Integer> mapping = new HashMap<>();
        for (int i = 0; i < distinctGenerated.size(); i++) {
            final double normalized = distinctGenerated.size() == 1 ? 0.0 : (double) i / (distinctGenerated.size() - 1);
            final int poolIndex = (int) Math.round(normalized * (orderedPool.size() - 1));
            mapping.put(distinctGenerated.get(i), orderedPool.get(poolIndex));
        }
        return mapping;
    }

    private MelodicPattern enrichLatentSteps(final MelodicPattern pattern) {
        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            steps.add(pattern.step(i));
        }
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = steps.get(i);
            if (step.pitch() != null) {
                continue;
            }
            final int pitch = latentPitchForStep(pattern, i);
            steps.set(i, new MelodicPattern.Step(i, false, false, pitch, DEFAULT_VELOCITY, DEFAULT_GATE, false, false));
        }
        return new MelodicPattern(steps, pattern.loopSteps());
    }

    private int latentPitchForStep(final MelodicPattern pattern, final int stepIndex) {
        for (int distance = 1; distance < pattern.loopSteps(); distance++) {
            final int left = stepIndex - distance;
            if (left >= 0) {
                final MelodicPattern.Step candidate = pattern.step(left);
                if (candidate.pitch() != null) {
                    return candidate.pitch();
                }
            }
            final int right = stepIndex + distance;
            if (right < pattern.loopSteps()) {
                final MelodicPattern.Step candidate = pattern.step(right);
                if (candidate.pitch() != null) {
                    return candidate.pitch();
                }
            }
        }
        return contextualLatentFallback(stepIndex);
    }

    private int contextualLatentFallback(final int stepIndex) {
        final int target = phraseContext().baseMidiNote() + switch (generator) {
            case ACID -> switch (Math.floorMod(stepIndex, 4)) {
                case 0 -> 0;
                case 1 -> 3;
                case 2 -> -2;
                default -> 5;
            };
            case CALL_RESPONSE -> switch (Math.floorMod(stepIndex, 8)) {
                case 0 -> 0;
                case 1 -> 2;
                case 2 -> 4;
                case 3 -> 5;
                case 4 -> 0;
                case 5 -> 3;
                case 6 -> 5;
                default -> 7;
            };
            case ROLLING -> switch (Math.floorMod(stepIndex, 4)) {
                case 0 -> 0;
                case 1 -> 2;
                case 2 -> 4;
                default -> 5;
            };
            case OCTAVE -> Math.floorMod(stepIndex, 2) == 0 ? 0 : 12;
            case MOTIF -> switch (Math.floorMod(stepIndex, 4)) {
                case 0 -> 0;
                case 1 -> 2;
                case 2 -> 4;
                default -> 7;
            };
        };
        if (!allowedPitches.isEmpty()) {
            return nearestAllowedPitch(target);
        }
        return Math.max(0, Math.min(127, target));
    }

    private int nearestAllowedPitch(final int targetPitch) {
        return nearestPitch(targetPitch, allowedPitches);
    }

    private int nearestLayoutPitch(final int targetPitch, final List<Integer> layout) {
        return nearestPitch(targetPitch, layout);
    }

    private int nearestPitchIndex(final int targetPitch, final List<Integer> candidates) {
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < candidates.size(); i++) {
            final int distance = Math.abs(candidates.get(i) - targetPitch);
            if (distance < bestDistance || (distance == bestDistance && candidates.get(i) < candidates.get(bestIndex))) {
                bestIndex = i;
                bestDistance = distance;
            }
        }
        return bestIndex;
    }

    private int nearestPitch(final int targetPitch, final Iterable<Integer> candidates) {
        int bestPitch = targetPitch;
        int bestDistance = Integer.MAX_VALUE;
        for (final int candidate : candidates) {
            final int distance = Math.abs(candidate - targetPitch);
            if (distance < bestDistance || (distance == bestDistance && candidate < bestPitch)) {
                bestPitch = candidate;
                bestDistance = distance;
            }
        }
        return bestPitch;
    }

    private int pitchPoolPitch(final int padIndex) {
        final List<Integer> pitches = pitchPoolLayoutPitches();
        return padIndex >= 0 && padIndex < pitches.size() ? pitches.get(padIndex) : -1;
    }

    private List<Integer> pitchPoolLayoutPitches() {
        final MelodicPhraseContext context = phraseContext();
        final int rootPitch = currentPoolLayoutRootPitch();
        final List<Integer> notes = new ArrayList<>(PITCH_POOL_PAD_COUNT);
        int candidate = rootPitch - 1;
        while (notes.size() < 4 && candidate >= 0) {
            if (context.scale().isMidiNoteInScale(context.rootNote(), candidate)) {
                notes.add(0, candidate);
            }
            candidate--;
        }
        notes.add(rootPitch);
        candidate = rootPitch + 1;
        while (notes.size() < PITCH_POOL_PAD_COUNT && candidate <= 127) {
            if (context.scale().isMidiNoteInScale(context.rootNote(), candidate)) {
                notes.add(candidate);
            }
            candidate++;
        }
        candidate = notes.isEmpty() ? rootPitch - 1 : notes.get(0) - 1;
        while (notes.size() < PITCH_POOL_PAD_COUNT && candidate >= 0) {
            if (context.scale().isMidiNoteInScale(context.rootNote(), candidate)) {
                notes.add(0, candidate);
            }
            candidate--;
        }
        while (notes.size() < PITCH_POOL_PAD_COUNT) {
            notes.add(notes.isEmpty() ? Math.max(0, Math.min(127, rootPitch)) : notes.get(notes.size() - 1));
        }
        return notes;
    }

    private int currentPoolLayoutRootPitch() {
        final MelodicPhraseContext context = phraseContext();
        final int fallback = nearestPhraseRootPitch(context.baseMidiNote());
        if (poolLayoutRootPitch < 0
                || Math.floorMod(poolLayoutRootPitch, 12) != context.rootNote()
                || !context.scale().isRootMidiNote(context.rootNote(), poolLayoutRootPitch)) {
            poolLayoutRootPitch = fallback;
        }
        return poolLayoutRootPitch;
    }

    private int shiftedPoolLayoutRootPitch(final int octaveDelta) {
        final int targetPitch = currentPoolLayoutRootPitch() + octaveDelta * 12;
        return nearestPhraseRootPitch(targetPitch);
    }

    private int nearestPhraseRootPitch(final int targetPitch) {
        final MelodicPhraseContext context = phraseContext();
        int bestPitch = Math.max(0, Math.min(127, targetPitch));
        int bestDistance = Integer.MAX_VALUE;
        for (int pitch = 0; pitch <= 127; pitch++) {
            if (!context.scale().isRootMidiNote(context.rootNote(), pitch)) {
                continue;
            }
            final int distance = Math.abs(pitch - targetPitch);
            if (distance < bestDistance || (distance == bestDistance && pitch < bestPitch)) {
                bestPitch = pitch;
                bestDistance = distance;
            }
        }
        return bestPitch;
    }

    private String pitchName(final int midiPitch) {
        final String[] names = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return "%s%d".formatted(names[Math.floorMod(midiPitch, 12)], midiPitch / 12 - 2);
    }

    private MelodicPattern invertedAroundRoot(final MelodicPattern pattern) {
        final int rootPitch = phraseContext().baseMidiNote();
        MelodicPattern out = pattern;
        for (int i = 0; i < STEP_COUNT; i++) {
            final MelodicPattern.Step step = out.step(i);
            if (!step.active() || step.pitch() == null) {
                continue;
            }
            final int distance = step.pitch() - rootPitch;
            final int invertedPitch = Math.max(0, Math.min(127, rootPitch - distance));
            out = out.withStep(step.withPitch(invertedPitch));
        }
        return out;
    }

    private void applyRepeatDouble() {
        if (loopSteps * 2 > STEP_COUNT) {
            oled.valueInfo("Double", "Max Len");
            return;
        }
        applyTransform(repeatDouble(patternState.currentPattern()), "Double", "Repeat", false);
    }

    private void applyHalveLength() {
        if (loopSteps <= 1) {
            oled.valueInfo("Half", "Min Len");
            return;
        }
        applyTransform(patternState.currentPattern().withLoopSteps(Math.max(1, loopSteps / 2)),
                "Half", Integer.toString(Math.max(1, loopSteps / 2)), false);
    }

    private void applyMirrorDouble() {
        if (loopSteps * 2 > STEP_COUNT) {
            oled.valueInfo("Mirror x2", "Max Len");
            return;
        }
        applyTransform(mirrorDouble(patternState.currentPattern()), "Mirror x2", "Mirror", false);
    }

    private MelodicPattern repeatDouble(final MelodicPattern pattern) {
        final int sourceLength = pattern.loopSteps();
        final int newLoopSteps = Math.min(STEP_COUNT, sourceLength * 2);
        final List<MelodicPattern.Step> steps = new ArrayList<>(pattern.steps());
        for (int i = 0; i < sourceLength && sourceLength + i < STEP_COUNT; i++) {
            final MelodicPattern.Step source = pattern.step(i);
            steps.set(sourceLength + i, source.withIndex(sourceLength + i).withTieFromPrevious(false));
        }
        return new MelodicPattern(steps, newLoopSteps);
    }

    private MelodicPattern mirrorDouble(final MelodicPattern pattern) {
        final int sourceLength = pattern.loopSteps();
        final int newLoopSteps = Math.min(STEP_COUNT, sourceLength * 2);
        final List<MelodicPattern.Step> steps = new ArrayList<>(pattern.steps());
        for (int i = 0; i < sourceLength && sourceLength + i < STEP_COUNT; i++) {
            final MelodicPattern.Step source = pattern.step(sourceLength - 1 - i);
            steps.set(sourceLength + i, source.withIndex(sourceLength + i).withTieFromPrevious(false));
        }
        return new MelodicPattern(steps, newLoopSteps);
    }

    private MelodicPattern swivelPattern(final MelodicPattern pattern) {
        final List<MelodicPattern.Step> steps = new ArrayList<>(pattern.steps());
        final int segmentLength = Math.max(1, pattern.loopSteps() / 2);
        for (int segmentStart = 0; segmentStart < pattern.loopSteps(); segmentStart += segmentLength) {
            final int segmentEnd = Math.min(pattern.loopSteps(), segmentStart + segmentLength);
            for (int i = 0; i < segmentEnd - segmentStart; i++) {
                final MelodicPattern.Step source = pattern.step(segmentEnd - 1 - i);
                steps.set(segmentStart + i, source.withIndex(segmentStart + i).withTieFromPrevious(false));
            }
        }
        return new MelodicPattern(steps, pattern.loopSteps());
    }

    private MelodicPattern contourInvertUp(final MelodicPattern pattern) {
        return contourInvert(pattern, true);
    }

    private MelodicPattern contourInvertDown(final MelodicPattern pattern) {
        return contourInvert(pattern, false);
    }

    private MelodicPattern contourInvert(final MelodicPattern pattern, final boolean upwards) {
        final List<Integer> activePitches = new ArrayList<>();
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = pattern.step(i);
            if (step.active() && step.pitch() != null) {
                activePitches.add(step.pitch());
            }
        }
        if (activePitches.isEmpty()) {
            return pattern;
        }
        final List<Integer> uniquePitches = activePitches.stream().distinct().sorted().toList();
        final int pivotPitch = upwards ? uniquePitches.get(0) : uniquePitches.get(uniquePitches.size() - 1);
        MelodicPattern out = pattern;
        boolean changed = false;
        for (int i = 0; i < pattern.loopSteps(); i++) {
            final MelodicPattern.Step step = out.step(i);
            if (step.pitch() == null) {
                continue;
            }
            int pitch = step.pitch();
            if (upwards && pitch == pivotPitch) {
                pitch = Math.min(127, pitch + 12);
            } else if (!upwards && pitch == pivotPitch) {
                pitch = Math.max(24, pitch - 12);
            }
            if (pitch != step.pitch()) {
                changed = true;
            }
            out = out.withStep(step.withPitch(pitch));
        }
        if (!changed && !uniquePitches.isEmpty()) {
            for (int i = 0; i < pattern.loopSteps(); i++) {
                final MelodicPattern.Step step = out.step(i);
                if (step.pitch() == null || step.pitch() != pivotPitch) {
                    continue;
                }
                final int shifted = upwards ? Math.min(127, step.pitch() + 12) : Math.max(24, step.pitch() - 12);
                out = out.withStep(step.withPitch(shifted));
                break;
            }
        }
        return out;
    }

    private long nextSeed(final long currentSeed) {
        return currentSeed >= Integer.MAX_VALUE ? 1L : currentSeed + 1L;
    }

    private int activeStepCount(final MelodicPattern pattern) {
        int count = 0;
        for (int i = 0; i < pattern.loopSteps(); i++) {
            if (pattern.step(i).active()) {
                count++;
            }
        }
        return count;
    }

    private double clampUnit(final double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private void handleNoteStepObject(final NoteStep noteStep) {
        final int x = noteStep.x();
        final int y = noteStep.y();
        final Map<Integer, NoteStep> notesAtStep = noteStepsByPosition.computeIfAbsent(x, ignored -> new HashMap<>());
        if (noteStep.state() == NoteStep.State.Empty) {
            notesAtStep.remove(y);
            if (notesAtStep.isEmpty()) {
                noteStepsByPosition.remove(x);
            }
        } else {
            notesAtStep.put(y, noteStep);
            final MelodicPattern.Step pending = clipWriter.pendingStepAt(x);
            if (pending != null && pending.pitch() != null && pending.pitch() == y) {
                if (Math.abs(noteStep.chance() - pending.chance()) > 0.0001) {
                    noteStep.setChance(pending.chance());
                }
                if (noteStep.recurrenceLength() != pending.bitwigRecurrenceLength()
                        || noteStep.recurrenceMask() != pending.bitwigRecurrenceMask()) {
                    noteStep.setRecurrence(pending.bitwigRecurrenceLength(), pending.bitwigRecurrenceMask());
                }
                clipWriter.clearPendingWrite(x);
            }
        }
        rebuildCachedPattern();
    }

    private void handlePlayingStep(final int clipPlayingStep) {
        this.playingStep = clipPlayingStep >= 0 && clipPlayingStep < STEP_COUNT ? clipPlayingStep : -1;
    }

    private void rebuildCachedPattern() {
        final MelodicPattern observed = MelodicClipAdapter.fromNoteSteps(noteStepsByPosition, loopSteps, STEP_LENGTH);
        patternState.applyObservedPattern(observed);
    }

    private void observeSelectedClip() {
        SelectedClipSlotObserver.observe(clipSlotBank, true, true, this::refreshSelectedClipState);
    }

    private void refreshSelectedClipState() {
        final SelectedClipSlotState state = SelectedClipSlotState.scan(clipSlotBank, MelodicRenderer.ACTIVE_STEP);
        selectedClipSlotIndex = state.slotIndex();
        selectedClipColor = state.color();
    }

    private boolean ensureClipAvailable() {
        refreshSelectedClipState();
        final NoteClipAvailability.Failure failure = NoteClipAvailability.requireSelectedClipSlot(
                cursorTrack.canHoldNoteData().get(), selectedClipSlotIndex >= 0);
        if (failure != null) {
            showClipAvailabilityFailure(failure);
            return false;
        }
        refreshClipCursor();
        return true;
    }

    private void showClipAvailabilityFailure(final NoteClipAvailability.Failure failure) {
        oled.valueInfo(failure.title(), failure.oledDetail());
        driver.notifyPopup(failure.title(), failure.popupDetail());
    }

    private void refreshClipCursor() {
        NoteClipCursorRefresher.refresh(
                clipSlotBank,
                driver.getViewControl().getSelectedClipSlotIndex(),
                this::refreshSelectedClipState,
                () -> selectedClipSlotIndex,
                () -> cursorClip.scrollToKey(0),
                () -> cursorClip.scrollToStep(0));
    }
    private MelodicPhraseContext phraseContext() {
        return new MelodicPhraseContext(driver.getSharedMusicalScale(), driver.getSharedRootNote(),
                driver.getSharedBaseMidiNote());
    }

    private RgbLigthState getPadLight(final int padIndex) {
        if (padIndex < CLIP_ROW_PAD_COUNT) {
            if (!heldSteps.isEmpty()) {
                return getRecurrencePadLight(padIndex);
            }
            return clipHandler.getPadLight(padIndex);
        }
        if (padIndex < STEP_PAD_OFFSET) {
            return getPitchPoolPadLight(padIndex - PITCH_POOL_PAD_OFFSET);
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
        return MelodicRenderer.stepLight(patternState.currentPattern().step(stepIndex), heldSteps.contains(stepIndex),
                stepIndex < loopSteps, stepIndex == playingStep, stepIndex, selectedClipColor);
    }

    private RgbLigthState getRecurrencePadLight(final int padIndex) {
        final List<Integer> targets = heldRecurrenceTargets();
        if (targets.isEmpty() || padIndex >= 8) {
            return RgbLigthState.OFF;
        }
        final MelodicPattern.Step step = patternState.currentPattern().step(targets.get(0));
        if (!step.active() || step.pitch() == null) {
            return RgbLigthState.OFF;
        }
        final RecurrencePattern recurrence = recurrenceOf(step);
        final int span = recurrence.effectiveSpan();
        if (padIndex >= span) {
            return RgbLigthState.OFF;
        }
        final int mask = recurrence.effectiveMask(span);
        return ((mask >> padIndex) & 0x1) == 1 ? selectedClipColor.getBrightend() : selectedClipColor.getDimmed();
    }

    private RecurrencePattern recurrenceOf(final MelodicPattern.Step step) {
        return RecurrencePattern.of(step.recurrenceLength(), step.recurrenceMask());
    }

    private List<Integer> heldRecurrenceTargets() {
        final List<Integer> targets = new ArrayList<>();
        for (final int stepIndex : heldSteps) {
            if (stepIndex < 0 || stepIndex >= STEP_COUNT) {
                continue;
            }
            final MelodicPattern.Step step = patternState.currentPattern().step(stepIndex);
            if (step.active() && step.pitch() != null) {
                targets.add(stepIndex);
            }
        }
        return targets;
    }

    private List<Integer> heldEditableTargets() {
        final List<Integer> targets = new ArrayList<>();
        for (final int stepIndex : heldSteps) {
            if (stepIndex < 0 || stepIndex >= STEP_COUNT) {
                continue;
            }
            final MelodicPattern.Step step = patternState.currentPattern().step(stepIndex);
            if (step.active() && step.pitch() != null) {
                targets.add(stepIndex);
            }
        }
        return targets;
    }

    private void applyHeldRecurrence(final List<Integer> stepIndices,
                                     final java.util.function.UnaryOperator<RecurrencePattern> updater,
                                     final String label) {
        if (!ensureClipAvailable()) {
            return;
        }
        MelodicPattern nextCached = patternState.currentPattern();
        MelodicPattern nextBase = patternState.basePattern();
        int updatedCount = 0;
        String summary = null;
        for (final int stepIndex : stepIndices) {
            final MelodicPattern.Step step = nextCached.step(stepIndex);
            if (!step.active() || step.pitch() == null) {
                continue;
            }
            final RecurrencePattern updated = updater.apply(recurrenceOf(step));
            final MelodicPattern.Step updatedStep = step.withRecurrence(updated.length(), updated.mask());
            nextCached = nextCached.withStep(updatedStep);
            nextBase = nextBase.withStep(updatedStep.withIndex(stepIndex));
            clipWriter.rememberPendingWrite(stepIndex, updatedStep);
            final NoteStep liveStep = primaryNoteStepAt(stepIndex);
            if (liveStep != null) {
                liveStep.setRecurrence(updatedStep.bitwigRecurrenceLength(), updatedStep.bitwigRecurrenceMask());
            }
            updatedCount++;
            summary = updated.summary();
        }
        if (updatedCount == 0) {
            oled.valueInfo(label, "No note");
            return;
        }
        patternState.setCurrentPattern(nextCached);
        patternState.setBasePattern(nextBase);
        final String value = updatedCount == 1 ? summary : "%d steps".formatted(updatedCount);
        oled.valueInfo(label, value);
        driver.notifyPopup(label, value);
    }

    private void applyHeldStepValueEdit(final List<Integer> stepIndices,
                                        final java.util.function.UnaryOperator<MelodicPattern.Step> updater,
                                        final String label,
                                        final String value) {
        if (!ensureClipAvailable()) {
            return;
        }
        if (stepIndices.isEmpty()) {
            oled.valueInfo(label, "No note");
            return;
        }
        MelodicPattern nextCached = patternState.currentPattern();
        MelodicPattern nextBase = patternState.basePattern();
        int updatedCount = 0;
        for (final int stepIndex : stepIndices) {
            final MelodicPattern.Step step = nextCached.step(stepIndex);
            if (!step.active() || step.pitch() == null) {
                continue;
            }
            final MelodicPattern.Step updatedStep = updater.apply(step);
            nextCached = nextCached.withStep(updatedStep);
            nextBase = nextBase.withStep(updatedStep.withIndex(stepIndex));
            updatedCount++;
        }
        if (updatedCount == 0) {
            oled.valueInfo(label, "No note");
            return;
        }
        patternState.setCurrentPattern(nextCached);
        patternState.setBasePattern(nextBase);
        applyPattern(nextCached, label, updatedCount == 1 ? value : "%s (%d)".formatted(value, updatedCount));
    }

    private NoteStep primaryNoteStepAt(final int stepIndex) {
        final Map<Integer, NoteStep> notesAtStep = noteStepsByPosition.get(stepIndex);
        if (notesAtStep == null || notesAtStep.isEmpty()) {
            return null;
        }
        return notesAtStep.values().stream()
                .filter(step -> step.state() == NoteStep.State.NoteOn)
                .min(Comparator.comparingInt(NoteStep::y))
                .orElse(null);
    }

    private RgbLigthState getPitchPoolPadLight(final int padIndex) {
        final int pitch = pitchPoolPitch(padIndex);
        if (pitch < 0) {
            return RgbLigthState.OFF;
        }
        final boolean enabled = allowedPitches.contains(pitch);
        final boolean root = phraseContext().scale().isRootMidiNote(phraseContext().rootNote(), pitch);
        final boolean usedInPattern = patternPitchSet().contains(pitch);
        return MelodicRenderer.pitchPoolLight(enabled, root, usedInPattern);
    }

    private Set<Integer> patternPitchSet() {
        final Set<Integer> pitches = new HashSet<>();
        for (int i = 0; i < patternState.currentPattern().loopSteps(); i++) {
            final MelodicPattern.Step step = patternState.currentPattern().step(i);
            if (step.active() && step.pitch() != null) {
                pitches.add(step.pitch());
            }
        }
        return pitches;
    }

    private BiColorLightState bankLightState() {
        return BiColorLightState.GREEN_HALF;
    }

    private BiColorLightState deleteLightState() {
        return deleteHeld.get() ? BiColorLightState.RED_FULL : BiColorLightState.RED_HALF;
    }

    public BiColorLightState getModeButtonLightState() {
        if (driver.isGlobalAltHeld()) {
            return driver.getStepFillLightState();
        }
        if (accentState.isHeld()) {
            return accentState.isActive() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
        }
        return accentState.isActive() ? BiColorLightState.AMBER_HALF : BiColorLightState.GREEN_FULL;
    }

    private String mutationLabel(final MelodicMutator.Mode mode) {
        return switch (mode) {
            case PRESERVE_RHYTHM -> "Keep Rhythm";
            case VARY_ENDING -> "Vary End";
            case SIMPLIFY -> "Simplify";
            case DENSIFY -> "Densify";
        };
    }

    private EncoderBankLayout createEncoderBankLayout() {
        final Map<EncoderMode, EncoderBank> banks = new EnumMap<>(EncoderMode.class);
        banks.put(EncoderMode.CHANNEL, new EncoderBank(
                "1: Engine\n2: Density\n3: Pool Oct\n4: Mut Type",
                new EncoderSlotBinding[]{
                        engineSlot(),
                        densitySlot(),
                        poolContextSlot(),
                        mutationModeSlot()
                }));
        banks.put(EncoderMode.MIXER, new EncoderBank(
                "1: Length\n2: Swivel / Mirror x2\n3: Reverse\n4: Invert",
                new EncoderSlotBinding[]{
                        alternateActionSlot("Length", this::adjustLengthProcess, this::channelShapeLabel, this::adjustChannelShape),
                        alternateActionSlot("Mirror x2", this::adjustMirrorProcess, () -> "Tension", this::adjustTension),
                        alternateActionSlot("Reverse", this::adjustReverseProcess, () -> "Legato", this::adjustLegato),
                        alternateActionSlot("Invert", this::adjustInvertProcess, () -> "Span via Row",
                                this::showRecurrenceEditInfo)
                }));
        banks.put(EncoderMode.USER_1, new EncoderBank(
                "1: Velocity\n2: Pressure\n3: Timbre\n4: Pitch",
                new EncoderSlotBinding[]{
                        noteAccessSlot(NoteStepAccess.VELOCITY),
                        noteAccessSlot(NoteStepAccess.PRESSURE),
                        noteAccessSlot(NoteStepAccess.TIMBRE),
                        noteAccessSlot(NoteStepAccess.PITCH)
                }));
        banks.put(EncoderMode.USER_2, new EncoderBank(
                "1: Gate Len\n2: Chance\n3: Vel Spread\n4: Repeat",
                new EncoderSlotBinding[]{
                        noteAccessSlot(NoteStepAccess.DURATION),
                        noteAccessSlot(NoteStepAccess.CHANCE),
                        noteAccessSlot(NoteStepAccess.VELOCITY_SPREAD),
                        noteAccessSlot(NoteStepAccess.REPEATS)
                }));
        return new EncoderBankLayout(banks);
    }

    private void cycleGenerator(final int direction) {
        final Generator[] values = Generator.values();
        final int nextIndex = Math.max(0, Math.min(values.length - 1, generator.ordinal() + direction));
        setGenerator(values[nextIndex]);
    }

    private EncoderSlotBinding engineSlot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindThresholdedEncoder(layer, ENGINE_ENCODER_THRESHOLD, ENGINE_ENCODER_THRESHOLD * 2,
                        driver::isGlobalShiftHeld, steps -> {
                            if (driver.isGlobalAltHeld()) {
                                cycleGeneratorSubtype(steps > 0 ? 1 : -1);
                            } else if (steps > 0) {
                                cycleGenerator(1);
                            } else {
                                cycleGenerator(-1);
                            }
                        });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        if (driver.isGlobalAltHeld()) {
                            oled.valueInfo(view.label, activeGenerator().currentSubtypeLabel());
                        } else {
                            oled.valueInfo(view.label, generator.label);
                        }
                    } else {
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private EncoderSlotBinding mutationModeSlot() {
        final EncoderStepAccumulator accumulator = new EncoderStepAccumulator(MUTATION_MODE_ENCODER_THRESHOLD);
        final EncoderStepAccumulator fineAccumulator = new EncoderStepAccumulator(MUTATION_MODE_ENCODER_THRESHOLD * 2);
        final ContinuousEncoderScaler altScaler = new ContinuousEncoderScaler();
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    if (driver.isGlobalAltHeld()) {
                        final int effective = altScaler.scale(inc, driver.isGlobalShiftHeld());
                        if (effective != 0) {
                            handler.recordTouchAdjustment(slotIndex, Math.abs(effective));
                            adjustMutateIntensity(effective);
                        }
                        return;
                    }
                    final EncoderStepAccumulator activeAccumulator =
                            driver.isGlobalShiftHeld() ? fineAccumulator : accumulator;
                    final int steps = activeAccumulator.consume(inc);
                    if (steps > 0) {
                        cycleMutationMode(1);
                    } else if (steps < 0) {
                        cycleMutationMode(-1);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        if (driver.isGlobalAltHeld()) {
                            oled.valueInfo("Mut %", "%.2f".formatted(mutateIntensity));
                        } else {
                            oled.valueInfo("Mut Type", mutationLabel(mutationMode));
                        }
                    } else {
                        accumulator.reset();
                        fineAccumulator.reset();
                        altScaler.reset();
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private EncoderSlotBinding densitySlot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    if (driver.isGlobalAltHeld()) {
                        adjustPostDensity(inc);
                    } else {
                        adjustDensity(inc);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        if (driver.isGlobalAltHeld()) {
                            oled.valueInfo("Thin/Fill", "Current phrase");
                        } else {
                            oled.valueInfo(view.label, "Density");
                        }
                    } else {
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private EncoderSlotBinding poolContextSlot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    if (driver.isGlobalAltHeld()) {
                        adjustGlobalRootKey(inc);
                    } else {
                        adjustPoolOctave(inc);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        if (driver.isGlobalAltHeld()) {
                            oled.valueInfo("Root", NoteAssignHelper.noteName(driver.getSharedRootNote()));
                        } else {
                            oled.valueInfo("Pool Oct", poolOctaveSummary());
                        }
                    } else {
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private EncoderSlotBinding valueSlot(final java.util.function.IntConsumer adjuster,
                                         final java.util.function.Supplier<String> label) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindContinuousEncoder(layer, driver::isGlobalShiftHeld, adjuster::accept);
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        oled.valueInfo(view.label, label.get());
                    } else {
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private void adjustGlobalRootKey(final int amount) {
        if (amount == 0) {
            return;
        }
        driver.adjustSharedRootNote(amount);
        final String rootName = NoteAssignHelper.noteName(driver.getSharedRootNote());
        oled.valueInfo("Root", rootName);
        driver.notifyPopup("Root", rootName);
    }

    private static final class NoteAssignHelper {
        private static final String[] NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

        private static String noteName(final int note) {
            return NAMES[Math.floorMod(note, 12)];
        }
    }

    private EncoderSlotBinding stepValueSlot(final java.util.function.IntConsumer adjuster,
                                             final String label) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindContinuousEncoder(layer, driver::isGlobalShiftHeld, adjuster::accept);
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        oled.valueInfo(label, "Step %d".formatted(selectedStep + 1));
                    } else {
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
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

    private EncoderSlotBinding actionSlot(final String label, final java.util.function.IntConsumer adjuster) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                final EncoderStepAccumulator accumulator = new EncoderStepAccumulator(ENGINE_ENCODER_THRESHOLD);
                final EncoderStepAccumulator fineAccumulator = new EncoderStepAccumulator(ENGINE_ENCODER_THRESHOLD * 2);
                encoder.bindEncoder(layer, inc -> {
                    final EncoderStepAccumulator activeAccumulator = driver.isGlobalShiftHeld()
                            ? fineAccumulator
                            : accumulator;
                    final int steps = activeAccumulator.consume(inc);
                    if (steps != 0) {
                        adjuster.accept(steps);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        oled.valueInfo(view.label, label);
                    } else {
                        accumulator.reset();
                        fineAccumulator.reset();
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private EncoderSlotBinding alternateActionSlot(final String primaryLabel, final java.util.function.IntConsumer primaryAdjuster,
                                                   final java.util.function.Supplier<String> alternateLabel,
                                                   final java.util.function.IntConsumer alternateAdjuster) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                final EncoderStepAccumulator primaryAccumulator = new EncoderStepAccumulator(ENGINE_ENCODER_THRESHOLD);
                final EncoderStepAccumulator primaryFineAccumulator = new EncoderStepAccumulator(ENGINE_ENCODER_THRESHOLD * 2);
                final ContinuousEncoderScaler altScaler = new ContinuousEncoderScaler();
                encoder.bindEncoder(layer, inc -> {
                    if (driver.isGlobalAltHeld()) {
                        final int effective = altScaler.scale(inc, driver.isGlobalShiftHeld());
                        if (effective != 0) {
                            alternateAdjuster.accept(effective);
                        }
                    } else {
                        final EncoderStepAccumulator activeAccumulator = driver.isGlobalShiftHeld()
                                ? primaryFineAccumulator
                                : primaryAccumulator;
                        final int steps = activeAccumulator.consume(inc);
                        if (steps != 0) {
                            primaryAdjuster.accept(steps);
                        }
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        oled.valueInfo(view.label, driver.isGlobalAltHeld() ? alternateLabel.get() : primaryLabel);
                    } else {
                        primaryAccumulator.reset();
                        primaryFineAccumulator.reset();
                        altScaler.reset();
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private EncoderSlotBinding noteAccessSlot(final NoteStepAccess access) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return access.getResolution();
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                handler.bindNoteAccess(layer, encoder, slotIndex, access);
            }
        };
    }

    private void adjustLengthProcess(final int inc) {
        if (inc > 0) {
            applyRepeatDouble();
        } else if (inc < 0) {
            applyHalveLength();
        }
    }

    private void adjustMirrorProcess(final int inc) {
        if (inc > 0) {
            applyMirrorDouble();
        } else if (inc < 0) {
            applyTransform(swivelPattern(patternState.currentPattern()), "Swivel", "Halves", false);
        }
    }

    private void adjustReverseProcess(final int inc) {
        if (inc == 0) {
            return;
        }
        applyTransform(patternState.currentPattern().reversed(), "Reverse", "Pattern", false);
    }

    private void adjustInvertProcess(final int inc) {
        if (inc > 0) {
            applyTransform(contourInvertUp(patternState.currentPattern()), "Invert", "Up", true);
        } else if (inc < 0) {
            applyTransform(contourInvertDown(patternState.currentPattern()), "Invert", "Down", true);
        }
    }

    @Override
    protected void onActivate() {
        refreshClipCursor();
        rebuildCachedPattern();
        view = View.PROCESS;
        patternButtons.setUpCallback(pressed -> {
            if (pressed) {
                if (driver.isGlobalShiftHeld()) {
                    setView(view == View.NOTES ? View.EXPRESSION : view == View.EXPRESSION ? View.PROCESS : View.NOTES);
                } else if (driver.isGlobalAltHeld()) {
                    mutatePitchPool();
                } else {
                    generatePitchPool();
                }
            }
        }, () -> BiColorLightState.GREEN_HALF);
        patternButtons.setDownCallback(pressed -> {
            if (pressed) {
                if (driver.isGlobalShiftHeld()) {
                    setView(view == View.NOTES ? View.PROCESS : view == View.EXPRESSION ? View.NOTES : View.EXPRESSION);
                } else if (driver.isGlobalAltHeld()) {
                    mutatePattern(false);
                } else {
                    generatePattern();
                }
            }
        }, () -> BiColorLightState.GREEN_HALF);
        encoderLayer.activate();
        selectedStep = Math.min(selectedStep, Math.max(0, loopSteps - 1));
    }

    @Override
    protected void onDeactivate() {
        patternButtons.setUpCallback(pressed -> { }, () -> BiColorLightState.OFF);
        patternButtons.setDownCallback(pressed -> { }, () -> BiColorLightState.OFF);
        accentState.clearHeld();
        fixedLengthHeld.set(false);
        deleteHeld.set(false);
        stopPitchPoolAuditions();
        encoderLayer.deactivate();
        noteRepeatHandler.deactivate();
    }

    @Override
    public boolean isSelectHeld() {
        return selectHeld.get();
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
    public int getClipCreateLengthBeats() {
        return MAX_CLIP_LENGTH_BEATS;
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
    public CursorRemoteControlsPage getActiveRemoteControlsPage() {
        return remoteControlsPage;
    }

    @Override
    public boolean isPadBeingHeld() {
        return false;
    }

    @Override
    public List<NoteStep> getOnNotes() {
        return noteStepsByPosition.values().stream()
                .flatMap(step -> step.values().stream())
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .collect(Collectors.toList());
    }

    @Override
    public List<NoteStep> getHeldNotes() {
        if (heldSteps.isEmpty()) {
            return noteStepsAtStep(selectedStep);
        }
        final List<NoteStep> notes = new ArrayList<>();
        for (final int stepIndex : heldSteps) {
            notes.addAll(noteStepsAtStep(stepIndex));
        }
        return notes;
    }

    @Override
    public List<NoteStep> getFocusedNotes() {
        return getHeldNotes();
    }

    @Override
    public String getDetails(final List<NoteStep> heldNotes) {
        return heldSteps.size() > 1 ? "%d steps".formatted(heldSteps.size()) : "Step " + (selectedStep + 1);
    }

    private List<NoteStep> noteStepsAtStep(final int stepIndex) {
        final Map<Integer, NoteStep> notes = noteStepsByPosition.getOrDefault(stepIndex, Map.of());
        return notes.values().stream()
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .sorted(Comparator.comparingInt(NoteStep::y))
                .toList();
    }

    @Override
    public double getGridResolution() {
        return STEP_LENGTH;
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
        return view.label;
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
    public int getDefaultStepVelocity() {
        return DEFAULT_VELOCITY;
    }

    @Override
    public double getDefaultStepDuration() {
        return STEP_LENGTH * DEFAULT_GATE;
    }
}
