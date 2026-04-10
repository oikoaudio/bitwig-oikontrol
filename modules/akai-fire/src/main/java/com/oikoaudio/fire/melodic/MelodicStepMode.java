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
import com.oikoaudio.fire.control.EncoderStepAccumulator;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.note.NoteMode;
import com.oikoaudio.fire.sequence.EncoderBank;
import com.oikoaudio.fire.sequence.EncoderBankLayout;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.sequence.EncoderSlotBinding;
import com.oikoaudio.fire.control.MixerEncoderProfile;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;
import com.oikoaudio.fire.sequence.SeqClipHandler;
import com.oikoaudio.fire.sequence.SeqClipRowHost;
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
    private static final int ENGINE_ENCODER_THRESHOLD = 3;
    private static final int MUTATION_MODE_ENCODER_THRESHOLD = 3;
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
    private final BooleanValueObject lengthDisplay = new BooleanValueObject();
    private final BooleanValueObject selectHeld = new BooleanValueObject();
    private final BooleanValueObject copyHeld = new BooleanValueObject();
    private final BooleanValueObject deleteHeld = new BooleanValueObject();
    private final BooleanValueObject fixedLengthHeld = new BooleanValueObject();
    private final SeqClipHandler clipHandler;
    private final Set<Integer> auditioningPoolPitches = new HashSet<>();
    private final MotifGenerator motifGenerator = new MotifGenerator();
    private final CallResponseGenerator callResponseGenerator = new CallResponseGenerator();
    private final EuclideanPhraseGenerator euclideanGenerator = new EuclideanPhraseGenerator();
    private final AcidGenerator acidGenerator = new AcidGenerator();
    private final RollingBassGenerator rollingBassGenerator = new RollingBassGenerator();
    private final OctaveJumpGenerator octaveJumpGenerator = new OctaveJumpGenerator();
    private final MelodicMutator mutator = new MelodicMutator();

    private MelodicPattern cachedPattern = MelodicPattern.empty(DEFAULT_LOOP_STEPS);
    private MelodicPattern basePattern = MelodicPattern.empty(DEFAULT_LOOP_STEPS);
    private int selectedStep = 0;
    private Integer heldStep = null;
    private boolean heldStepConsumed = false;
    private int playingStep = -1;
    private int selectedClipSlotIndex = -1;
    private RgbLigthState selectedClipColor = MelodicRenderer.ACTIVE_STEP;
    private int loopSteps = DEFAULT_LOOP_STEPS;
    private final LinkedHashSet<Integer> allowedPitches = new LinkedHashSet<>();
    private boolean poolUserEdited = false;
    private Generator poolGeneratorSource = null;
    private boolean accentActive = false;
    private boolean accentButtonHeld = false;
    private boolean accentModified = false;
    private boolean mainEncoderPressConsumed = false;
    private double density = 0.45;
    private double tension = 0.25;
    private double octaveActivity = 0.1;
    private int euclideanPulses = 5;
    private int euclideanRotation = 0;
    private double mutateIntensity = 0.45;
    private long seed = 1L;
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
        MOTIF("Motif"),
        CALL_RESPONSE("Call/Resp"),
        EUCLIDEAN("Euclid"),
        ROLLING("Rolling"),
        OCTAVE("Octave");

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
        this.clipHandler = new SeqClipHandler(this);
        bindPads();
        bindButtons();
        bindMainEncoder();
        this.encoderBankLayout = createEncoderBankLayout();
        this.encoderLayer = new StepSequencerEncoderHandler(this, driver);
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
                    applyTransform(cachedPattern.rotated(-1), "Rotate", "Left", false);
                }
            }
        }, this::bankLightState);

        final BiColorButton bankRightButton = driver.getButton(NoteAssign.BANK_R);
        bankRightButton.bindPressed(this, pressed -> {
            if (pressed) {
                if (driver.isGlobalAltHeld()) {
                    applyRepeatDouble();
                } else {
                    applyTransform(cachedPattern.rotated(1), "Rotate", "Right", false);
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
        accentButtonHeld = pressed;
        if (pressed) {
            oled.valueInfo("Accent", accentActive ? "On" : "Off");
        } else {
            if (!accentModified) {
                accentActive = !accentActive;
                oled.valueInfo("Accent", accentActive ? "On" : "Off");
            } else {
                oled.clearScreenDelayed();
            }
            accentModified = false;
        }
    }

    private void handlePadPress(final int padIndex, final boolean pressed) {
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
            if (heldStep != null && heldStep == stepIndex) {
                if (!heldStepConsumed && !accentButtonHeld && !fixedLengthHeld.get() && !deleteHeld.get()) {
                    toggleStep(stepIndex);
                }
                heldStep = null;
                heldStepConsumed = false;
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
        heldStep = stepIndex;
        heldStepConsumed = false;
        if (accentButtonHeld) {
            heldStepConsumed = true;
            accentModified = true;
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
        final MelodicPattern.Step step = cachedPattern.step(stepIndex);
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
        applyPattern(cachedPattern.withStep(step.withPitch(pitch)), "Pitch", pitchName(pitch));
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

    private void loadGeneratorDefaults(final Generator selectedGenerator) {
        switch (selectedGenerator) {
            case MOTIF -> {
                density = 0.40;
                tension = 0.25;
                octaveActivity = 1.0;
            }
            case CALL_RESPONSE -> {
                density = 0.46;
                tension = 0.28;
                octaveActivity = 1.0;
            }
            case EUCLIDEAN -> {
                density = 0.55;
                tension = 0.30;
                octaveActivity = 1.0;
                euclideanPulses = Math.max(3, Math.min(loopSteps, 5));
            }
            case ACID -> {
                density = 0.52;
                tension = 0.62;
                octaveActivity = 1.0;
            }
            case ROLLING -> {
                density = 1.0;
                tension = 0.24;
                octaveActivity = 1.0;
            }
            case OCTAVE -> {
                density = 0.48;
                tension = 0.22;
                octaveActivity = 0.60;
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
        for (int i = 0; i < cachedPattern.loopSteps(); i++) {
            final MelodicPattern.Step step = cachedPattern.step(i);
            if (!step.active() || step.pitch() == null) {
                continue;
            }
            if (i == 0 || i % 4 == 0 || i == cachedPattern.loopSteps() - 1) {
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
            case EUCLIDEAN -> new int[]{0, 1, 2, 3, 4};
            case ACID -> new int[]{0, 1, 2, 3, 4, 5, 6, 7};
            case ROLLING -> new int[]{0, 1, 2, 3, 4, 5, 6};
            case OCTAVE -> new int[]{0, 2, 4, 7, 9, 11, 13};
        };
        final int targetCount = switch (generator) {
            case MOTIF -> 3 + random.nextInt(tension >= 0.5 ? 3 : 2);
            case CALL_RESPONSE -> 4 + random.nextInt(tension >= 0.5 ? 3 : 2);
            case EUCLIDEAN -> 2 + random.nextInt(2 + (tension >= 0.6 ? 1 : 0));
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
        final MelodicGenerator activeGenerator = switch (generator) {
            case MOTIF -> motifGenerator;
            case CALL_RESPONSE -> callResponseGenerator;
            case EUCLIDEAN -> euclideanGenerator;
            case ACID -> acidGenerator;
            case ROLLING -> rollingBassGenerator;
            case OCTAVE -> octaveJumpGenerator;
        };
        MelodicPattern generated = activeGenerator.generate(context, parameters);
        generated = enrichLatentSteps(generated);
        generated = constrainPatternToPool(generated);
        basePattern = generated;
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
            case EUCLIDEAN -> "Euc";
            case ACID -> "Acd";
            case ROLLING -> "Rol";
            case OCTAVE -> "Oct";
        };
    }

    private MelodicGenerator.GenerateParameters generatorParametersForCurrentEngine(final long generationSeed) {
        return switch (generator) {
            case MOTIF -> new MelodicGenerator.GenerateParameters(
                    loopSteps, density, tension, octaveActivity, euclideanPulses, euclideanRotation, generationSeed);
            case CALL_RESPONSE -> new MelodicGenerator.GenerateParameters(
                    loopSteps, Math.max(0.35, density), Math.max(0.2, tension),
                    octaveActivity, euclideanPulses, euclideanRotation, generationSeed);
            case EUCLIDEAN -> new MelodicGenerator.GenerateParameters(
                    loopSteps, density, tension, octaveActivity,
                    Math.max(1, euclideanPulses), euclideanRotation, generationSeed);
            case ACID -> new MelodicGenerator.GenerateParameters(
                    loopSteps,
                    Math.max(0.35, density),
                    Math.max(0.55, tension),
                    Math.max(0.15, octaveActivity),
                    Math.max(4, euclideanPulses),
                    euclideanRotation,
                    generationSeed);
            case ROLLING -> new MelodicGenerator.GenerateParameters(
                    loopSteps,
                    density,
                    Math.max(0.2, tension),
                    Math.min(0.25, Math.max(0.05, octaveActivity)),
                    Math.max(6, euclideanPulses),
                    euclideanRotation,
                    generationSeed);
            case OCTAVE -> new MelodicGenerator.GenerateParameters(
                    loopSteps,
                    Math.max(0.35, density),
                    Math.max(0.15, tension),
                    Math.max(0.45, octaveActivity),
                    Math.max(4, euclideanPulses),
                    euclideanRotation,
                    generationSeed);
        };
    }

    private void mutatePattern(final boolean fromOriginalPattern) {
        if (!ensureClipAvailable()) {
            return;
        }
        final MelodicPattern sourcePattern = fromOriginalPattern ? basePattern : cachedPattern;
        if (activeStepCount(sourcePattern) == 0) {
            generatePattern();
            return;
        }
        final long mutationSeed = seed;
        MelodicPattern mutated = mutator.mutate(sourcePattern, phraseContext(), mutationMode, mutateIntensity, 0.7, mutationSeed);
        mutated = enrichLatentSteps(mutated);
        mutated = mutationMode == MelodicMutator.Mode.PRESERVE_RHYTHM
                ? revoicePatternToPoolVariant(mutated, mutateIntensity, mutationSeed)
                : constrainPatternToPoolLocally(mutated);
        basePattern = mutated;
        applyPattern(mutated, fromOriginalPattern ? "Mutate Orig" : "Mutate", mutationLabel(mutationMode));
        seed = nextSeed(mutationSeed);
    }

    private void toggleStep(final int stepIndex) {
        final MelodicPattern.Step step = cachedPattern.step(stepIndex);
        if (step.active()) {
            clearStep(stepIndex);
            return;
        }
        final MelodicPattern.Step created = step.pitch() != null
                ? applyCurrentAccentState(step.withActive(true))
                : restoreGeneratedStepOrDefault(stepIndex);
        applyPattern(cachedPattern.withStep(created), "Step", Integer.toString(stepIndex + 1));
    }

    private void clearStep(final int stepIndex) {
        final MelodicPattern.Step current = cachedPattern.step(stepIndex);
        final MelodicPattern.Step hidden = current.pitch() != null
                ? current.withActive(false)
                : MelodicPattern.Step.rest(stepIndex);
        MelodicPattern pattern = cachedPattern.withStep(hidden);
        if (stepIndex + 1 < STEP_COUNT && pattern.step(stepIndex + 1).tieFromPrevious()) {
            pattern = pattern.withStep(pattern.step(stepIndex + 1).withTieFromPrevious(false));
        }
        applyPattern(pattern, "Clear", "Step " + (stepIndex + 1));
    }

    private void toggleAccent(final int stepIndex) {
        final MelodicPattern.Step step = ensureStep(stepIndex);
        applyPattern(cachedPattern.withStep(step.withAccent(!step.accent())
                        .withVelocity(step.accent() ? DEFAULT_VELOCITY : 118)),
                "Accent", "Step " + (stepIndex + 1));
    }

    private void toggleTie(final int stepIndex) {
        final MelodicPattern.Step step = ensureStep(stepIndex);
        if (stepIndex + 1 >= STEP_COUNT) {
            return;
        }
        final MelodicPattern.Step next = cachedPattern.step(stepIndex + 1);
        final boolean newTie = !next.tieFromPrevious();
        MelodicPattern pattern = cachedPattern.withStep(step);
        pattern = pattern.withStep(next.withTieFromPrevious(newTie));
        applyPattern(pattern, "Tie", newTie ? "On" : "Off");
    }

    private void toggleSlide(final int stepIndex) {
        final MelodicPattern.Step step = ensureStep(stepIndex);
        applyPattern(cachedPattern.withStep(step.withSlide(!step.slide()).withGate(step.slide() ? DEFAULT_GATE : 1.05)),
                "Slide", step.slide() ? "Off" : "On");
    }

    private MelodicPattern.Step ensureStep(final int stepIndex) {
        final MelodicPattern.Step current = cachedPattern.step(stepIndex);
        if (current.active()) {
            return current;
        }
        final MelodicPattern.Step created = defaultStep(stepIndex);
        cachedPattern = cachedPattern.withStep(created);
        return created;
    }

    private MelodicPattern.Step restoreGeneratedStepOrDefault(final int stepIndex) {
        final MelodicPattern.Step base = basePattern.step(stepIndex);
        if (base.active() && base.pitch() != null) {
            return applyCurrentAccentState(base.withIndex(stepIndex));
        }
        return defaultStep(stepIndex);
    }

    private MelodicPattern.Step defaultStep(final int stepIndex) {
        final int pitch = defaultPoolPitch();
        return applyCurrentAccentState(new MelodicPattern.Step(stepIndex, true, false, pitch,
                DEFAULT_VELOCITY, DEFAULT_GATE, false, false));
    }

    private MelodicPattern.Step applyCurrentAccentState(final MelodicPattern.Step step) {
        return accentActive
                ? step.withAccent(true).withVelocity(118)
                : step.withAccent(false).withVelocity(DEFAULT_VELOCITY);
    }

    private void setLoopSteps(final int newLoopSteps) {
        loopSteps = Math.max(1, Math.min(STEP_COUNT, newLoopSteps));
        applyPattern(cachedPattern.withLoopSteps(loopSteps), "Loop", Integer.toString(loopSteps));
    }

    private void applyPattern(final MelodicPattern pattern, final String label, final String value) {
        if (!ensureClipAvailable()) {
            return;
        }
        refreshClipCursor();
        loopSteps = pattern.loopSteps();
        cachedPattern = pattern.withLoopSteps(loopSteps);
        MelodicClipAdapter.writeToClip(cursorClip, cachedPattern, STEP_LENGTH);
        oled.valueInfo(label, value);
        driver.notifyPopup(label, value);
    }

    private void applyTransform(final MelodicPattern pattern, final String label, final String value,
                                final boolean syncPoolFromPattern) {
        if (syncPoolFromPattern) {
            seedPitchPoolFromPattern(pattern);
            poolUserEdited = true;
        }
        basePattern = pattern;
        applyPattern(pattern, label, value);
    }

    private void revoiceCurrentPatternToPool(final String label, final String value) {
        if (!ensureClipAvailable()) {
            return;
        }
        final MelodicPattern source = activeStepCount(cachedPattern) > 0 ? cachedPattern : basePattern;
        if (activeStepCount(source) == 0) {
            oled.valueInfo(label, value);
            driver.notifyPopup(label, value);
            return;
        }
        final MelodicPattern revoiced = constrainPatternToPool(source);
        basePattern = revoiced;
        applyPattern(revoiced, label, value);
    }

    private void adjustSelectedPitch(final int amount) {
        if (amount == 0) {
            return;
        }
        final int stepIndex = editingStepIndex();
        final MelodicPattern.Step step = ensureStep(stepIndex);
        final int currentPitch = step.pitch() == null ? phraseContext().baseMidiNote() : step.pitch();
        applyPattern(cachedPattern.withStep(step.withPitch(Math.max(0, Math.min(127, currentPitch + amount)))),
                "Pitch", Integer.toString(currentPitch + amount));
    }

    private void adjustSelectedOctave(final int amount) {
        if (amount == 0) {
            return;
        }
        final int stepIndex = editingStepIndex();
        final MelodicPattern.Step step = ensureStep(stepIndex);
        final int currentPitch = step.pitch() == null ? phraseContext().baseMidiNote() : step.pitch();
        final int shiftedPitch = Math.max(0, Math.min(127, currentPitch + amount * 12));
        applyPattern(cachedPattern.withStep(step.withPitch(shiftedPitch)),
                "Octave", pitchName(shiftedPitch));
    }

    private void adjustSelectedGate(final int amount) {
        if (amount == 0) {
            return;
        }
        final MelodicPattern.Step step = ensureStep(editingStepIndex());
        applyPattern(cachedPattern.withStep(step.withGate(step.gate() + amount * 0.05)),
                "Gate", "%.2f".formatted(step.gate() + amount * 0.05));
    }

    private void adjustSelectedVelocity(final int amount) {
        if (amount == 0) {
            return;
        }
        final MelodicPattern.Step step = ensureStep(editingStepIndex());
        applyPattern(cachedPattern.withStep(step.withVelocity(step.velocity() + amount)),
                "Velocity", Integer.toString(step.velocity() + amount));
    }

    private void cycleArticulation(final int amount) {
        if (amount == 0) {
            return;
        }
        final int stepIndex = editingStepIndex();
        final MelodicPattern.Step step = ensureStep(stepIndex);
        final int current = step.tieFromPrevious() ? 3 : step.slide() ? 2 : step.accent() ? 1 : 0;
        final int next = Math.floorMod(current + amount, 4);
        MelodicPattern pattern = cachedPattern.withStep(step.withAccent(false).withSlide(false));
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
            return;
        }
        driver.markMainEncoderTurned();
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

    private void adjustChannelShape(final int amount) {
        if (generator == Generator.EUCLIDEAN) {
            adjustEuclideanPulses(amount);
            return;
        }
        if (generator == Generator.ROLLING) {
            adjustDensity(amount);
            return;
        }
        adjustOctaveActivity(amount);
    }

    private String channelShapeLabel() {
        if (generator == Generator.EUCLIDEAN) {
            return "Pulses";
        }
        if (generator == Generator.ROLLING) {
            return "Density";
        }
        return "Shape";
    }

    private void adjustPostDensity(final int amount) {
        if (amount == 0 || !ensureClipAvailable()) {
            return;
        }
        MelodicPattern pattern = cachedPattern;
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
            final MelodicPattern.Step base = basePattern.step(i);
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

    private void cycleMutationMode(final int direction) {
        final MelodicMutator.Mode[] values = MelodicMutator.Mode.values();
        final int nextIndex = Math.max(0, Math.min(values.length - 1, mutationMode.ordinal() + direction));
        mutationMode = values[nextIndex];
        oled.valueInfo("Mut Type", mutationLabel(mutationMode));
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
            case EUCLIDEAN -> switch (Math.floorMod(stepIndex, 3)) {
                case 0 -> 0;
                case 1 -> 2;
                default -> 5;
            };
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
        return phraseContext().collapsedScaleRange(PITCH_POOL_PAD_COUNT);
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
        applyTransform(repeatDouble(cachedPattern), "Double", "Repeat", false);
    }

    private void applyHalveLength() {
        if (loopSteps <= 1) {
            oled.valueInfo("Half", "Min Len");
            return;
        }
        applyTransform(cachedPattern.withLoopSteps(Math.max(1, loopSteps / 2)),
                "Half", Integer.toString(Math.max(1, loopSteps / 2)), false);
    }

    private void applyMirrorDouble() {
        if (loopSteps * 2 > STEP_COUNT) {
            oled.valueInfo("Mirror", "Max Len");
            return;
        }
        applyTransform(mirrorDouble(cachedPattern), "Double", "Mirror", false);
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
        }
        rebuildCachedPattern();
    }

    private void handlePlayingStep(final int clipPlayingStep) {
        this.playingStep = clipPlayingStep >= 0 && clipPlayingStep < STEP_COUNT ? clipPlayingStep : -1;
    }

    private void rebuildCachedPattern() {
        final MelodicPattern observed = MelodicClipAdapter.fromNoteSteps(noteStepsByPosition, loopSteps, STEP_LENGTH);
        cachedPattern = mergeObservedWithLatent(observed, cachedPattern);
        basePattern = mergeObservedWithLatent(observed, basePattern);
    }

    private MelodicPattern mergeObservedWithLatent(final MelodicPattern observed, final MelodicPattern latentSource) {
        final List<MelodicPattern.Step> steps = new ArrayList<>(MelodicPattern.MAX_STEPS);
        for (int i = 0; i < MelodicPattern.MAX_STEPS; i++) {
            final MelodicPattern.Step observedStep = observed.step(i);
            if (observedStep.pitch() != null || latentSource == null) {
                steps.add(observedStep);
                continue;
            }
            final MelodicPattern.Step latentStep = latentSource.step(i);
            if (latentStep.pitch() != null) {
                steps.add(latentStep.withIndex(i).withActive(false));
            } else {
                steps.add(observedStep);
            }
        }
        return new MelodicPattern(steps, observed.loopSteps());
    }

    private void observeSelectedClip() {
        for (int i = 0; i < clipSlotBank.getSizeOfBank(); i++) {
            final ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
            slot.exists().markInterested();
            slot.isSelected().markInterested();
            slot.hasContent().markInterested();
            slot.color().markInterested();
            slot.isPlaying().markInterested();
            slot.isRecording().markInterested();
            slot.exists().addValueObserver(ignored -> refreshSelectedClipState());
            slot.isSelected().addValueObserver(ignored -> refreshSelectedClipState());
            slot.hasContent().addValueObserver(ignored -> refreshSelectedClipState());
            slot.color().addValueObserver((r, g, b) -> refreshSelectedClipState());
            slot.isPlaying().addValueObserver(ignored -> refreshSelectedClipState());
            slot.isRecording().addValueObserver(ignored -> refreshSelectedClipState());
        }
        refreshSelectedClipState();
    }

    private void refreshSelectedClipState() {
        selectedClipSlotIndex = -1;
        selectedClipColor = MelodicRenderer.ACTIVE_STEP;
        for (int i = 0; i < clipSlotBank.getSizeOfBank(); i++) {
            final ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
            if (slot.exists().get() && slot.isSelected().get()) {
                selectedClipSlotIndex = i;
                selectedClipColor = ColorLookup.getColor(slot.color().get());
                break;
            }
        }
    }

    private boolean ensureClipAvailable() {
        if (!cursorTrack.canHoldNoteData().get()) {
            oled.valueInfo("Audio Track", "Use note track");
            driver.notifyPopup("Audio Track", "Use note track");
            return false;
        }
        refreshSelectedClipState();
        if (selectedClipSlotIndex >= 0) {
            refreshClipCursor();
            return true;
        }
        oled.valueInfo("No Clip", "Select clip");
        driver.notifyPopup("No Clip", "Select clip");
        return false;
    }

    private void refreshClipCursor() {
        boolean clipSelected = false;
        final int preferredSlotIndex = driver.getViewControl().getSelectedClipSlotIndex();
        if (preferredSlotIndex >= 0 && preferredSlotIndex < clipSlotBank.getSizeOfBank()) {
            final ClipLauncherSlot preferredSlot = clipSlotBank.getItemAt(preferredSlotIndex);
            if (preferredSlot.exists().get()) {
                preferredSlot.select();
                clipSelected = true;
            }
        }
        if (!clipSelected) {
            clipSelected = selectPlayingClipSlot();
        }
        if (!clipSelected) {
            selectSelectedClipSlot();
        }
        cursorClip.scrollToKey(0);
        cursorClip.scrollToStep(0);
    }

    private boolean selectPlayingClipSlot() {
        for (int i = 0; i < clipSlotBank.getSizeOfBank(); i++) {
            final ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
            if (slot.exists().get() && (slot.isPlaying().get() || slot.isRecording().get())) {
                slot.select();
                return true;
            }
        }
        return false;
    }

    private void selectSelectedClipSlot() {
        for (int i = 0; i < clipSlotBank.getSizeOfBank(); i++) {
            final ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
            if (slot.exists().get() && slot.isSelected().get()) {
                slot.select();
                return;
            }
        }
    }

    private MelodicPhraseContext phraseContext() {
        final NoteMode noteMode = driver.getNoteMode();
        return new MelodicPhraseContext(noteMode.getCurrentScale(), noteMode.getMelodicStepRootNoteClass(),
                noteMode.getMelodicStepBaseMidiNote());
    }

    private RgbLigthState getPadLight(final int padIndex) {
        if (padIndex < CLIP_ROW_PAD_COUNT) {
            return clipHandler.getPadLight(padIndex);
        }
        if (padIndex < STEP_PAD_OFFSET) {
            return getPitchPoolPadLight(padIndex - PITCH_POOL_PAD_OFFSET);
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
        return MelodicRenderer.stepLight(cachedPattern.step(stepIndex), heldStep != null && heldStep == stepIndex,
                stepIndex < loopSteps, stepIndex == playingStep, stepIndex, selectedClipColor);
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
        for (int i = 0; i < cachedPattern.loopSteps(); i++) {
            final MelodicPattern.Step step = cachedPattern.step(i);
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
        if (accentButtonHeld) {
            return accentActive ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
        }
        return accentActive ? BiColorLightState.AMBER_HALF : BiColorLightState.GREEN_FULL;
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
                "1: Engine\n2: Density\n3: Shape\n4: Mut Type",
                new EncoderSlotBinding[]{
                        engineSlot(),
                        valueSlot(this::adjustDensity, () -> "Density"),
                        valueSlot(this::adjustChannelShape, this::channelShapeLabel),
                        mutationModeSlot()
                }));
        banks.put(EncoderMode.MIXER, new EncoderBank(
                "1: Length\n2: Mirror\n3: Reverse\n4: Invert",
                new EncoderSlotBinding[]{
                        actionSlot("Length", this::adjustLengthProcess),
                        actionSlot("Mirror", this::adjustMirrorProcess),
                        actionSlot("Reverse", this::adjustReverseProcess),
                        actionSlot("Invert", this::adjustInvertProcess)
                }));
        banks.put(EncoderMode.USER_1, new EncoderBank(
                "1: Tension\n2: Pulses\n3: Rotation\n4: Mut %",
                new EncoderSlotBinding[]{
                        valueSlot(this::adjustTension, () -> "Tension"),
                        valueSlot(this::adjustEuclideanPulses, () -> "Pulses"),
                        valueSlot(this::adjustEuclideanRotation, () -> "Rotation"),
                        valueSlot(this::adjustMutateIntensity, () -> "Mut %")
                }));
        banks.put(EncoderMode.USER_2, new EncoderBank(
                "1: Octave\n2: Gate\n3: Velocity\n4: Artic",
                new EncoderSlotBinding[]{
                        valueSlot(this::adjustSelectedOctave, () -> stepDetail("Oct")),
                        valueSlot(this::adjustSelectedGate, () -> stepDetail("Gate")),
                        valueSlot(this::adjustSelectedVelocity, () -> stepDetail("Velocity")),
                        valueSlot(this::cycleArticulation, () -> stepDetail("Artic"))
                }));
        return new EncoderBankLayout(banks);
    }

    private void cycleGenerator(final int direction) {
        final Generator[] values = Generator.values();
        final int nextIndex = Math.max(0, Math.min(values.length - 1, generator.ordinal() + direction));
        setGenerator(values[nextIndex]);
    }

    private EncoderSlotBinding engineSlot() {
        final EncoderStepAccumulator accumulator = new EncoderStepAccumulator(ENGINE_ENCODER_THRESHOLD);
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    final int steps = accumulator.consume(inc);
                    if (steps > 0) {
                        cycleGenerator(1);
                    } else if (steps < 0) {
                        cycleGenerator(-1);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        oled.valueInfo("Engine", generator.label);
                    } else {
                        accumulator.reset();
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private EncoderSlotBinding mutationModeSlot() {
        final EncoderStepAccumulator accumulator = new EncoderStepAccumulator(MUTATION_MODE_ENCODER_THRESHOLD);
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    final int steps = accumulator.consume(inc);
                    if (steps > 0) {
                        cycleMutationMode(1);
                    } else if (steps < 0) {
                        cycleMutationMode(-1);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        oled.valueInfo("Mut Type", mutationLabel(mutationMode));
                    } else {
                        accumulator.reset();
                        oled.clearScreenDelayed();
                    }
                });
            }
        };
    }

    private String stepDetail(final String label) {
        return "%s S%d".formatted(label, selectedStep + 1);
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
                encoder.bindEncoder(layer, adjuster::accept);
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        oled.valueInfo(label.get(), view.label);
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
                encoder.bindEncoder(layer, inc -> {
                    MixerEncoderProfile.adjustParameter(parameter, driver.isGlobalShiftHeld(), inc);
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
                encoder.bindEncoder(layer, adjuster::accept);
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        oled.valueInfo(label, "Turn");
                    } else {
                        oled.clearScreenDelayed();
                    }
                });
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
            applyTransform(swivelPattern(cachedPattern), "Swivel", "Halves", false);
        }
    }

    private void adjustReverseProcess(final int inc) {
        if (inc == 0) {
            return;
        }
        applyTransform(cachedPattern.reversed(), "Reverse", "Pattern", false);
    }

    private void adjustInvertProcess(final int inc) {
        if (inc > 0) {
            applyTransform(contourInvertUp(cachedPattern), "Invert", "Up", true);
        } else if (inc < 0) {
            applyTransform(contourInvertDown(cachedPattern), "Invert", "Down", true);
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
                if (driver.isGlobalShiftHeld() && driver.isGlobalAltHeld()) {
                    clearCurrentClip();
                } else if (driver.isGlobalShiftHeld()) {
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
        accentButtonHeld = false;
        fixedLengthHeld.set(false);
        deleteHeld.set(false);
        stopPitchPoolAuditions();
        encoderLayer.deactivate();
        noteRepeatHandler.deactivate();
    }

    private void clearCurrentClip() {
        if (!ensureClipAvailable()) {
            return;
        }
        refreshClipCursor();
        cursorClip.clearSteps();
        noteStepsByPosition.clear();
        cachedPattern = MelodicPattern.empty(loopSteps);
        basePattern = MelodicPattern.empty(loopSteps);
        playingStep = -1;
        selectedStep = Math.min(selectedStep, Math.max(0, loopSteps - 1));
        oled.valueInfo("Clip", "Cleared");
        driver.notifyPopup("Clip", "Cleared");
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
        final Map<Integer, NoteStep> notes = noteStepsByPosition.getOrDefault(selectedStep, Map.of());
        return notes.values().stream()
                .filter(note -> note.state() == NoteStep.State.NoteOn)
                .sorted(Comparator.comparingInt(NoteStep::y))
                .toList();
    }

    @Override
    public List<NoteStep> getFocusedNotes() {
        return getHeldNotes();
    }

    @Override
    public String getDetails(final List<NoteStep> heldNotes) {
        return "Step " + (selectedStep + 1);
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
