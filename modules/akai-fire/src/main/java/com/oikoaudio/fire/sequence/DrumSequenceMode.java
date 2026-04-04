package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.AkaiFireDrumSeqExtension;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.EncoderStepAccumulator;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.utils.PatternButtons;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.api.NoteStep.State;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.StepViewPosition;

import java.util.*;
import java.util.stream.Collectors;

public class DrumSequenceMode extends Layer implements StepSequencerHost {
    private final ControllerHost host;
    private final AkaiFireDrumSeqExtension driver;
    private Application app;

    private final IntSetValue heldSteps = new IntSetValue();
    private final Set<Integer> addedSteps = new HashSet<>();
    private final Set<Integer> modifiedSteps = new HashSet<>();
    private final HashMap<Integer, NoteStep> expectedNoteChanges = new HashMap<>();
    // Maintain fractional offsets for held notes.
    private final Map<NoteStep, Double> fractionalOffsets = new HashMap<>();
    private final Map<Integer, Map<Integer, Integer>> currentNotesInClip = new HashMap<>();

    private final NoteStep[] assignments = new NoteStep[32];
    private static final double FINE_STEP_SIZE = 1.0 / 64.0;

    private final OledDisplay oled;

    private final Layer mainLayer;
    private final Layer shiftLayer;
    private Layer currentLayer;
    private final Layer muteLayer;
    private final Layer soloLayer;
    private final SequencEncoderHandler encoderLayer;
    private final EuclidState euclidState = new EuclidState();

    private final CursorTrack cursorTrack;
    private final PinnableCursorClip cursorClip;
    private  Clip bigCursorClip;

//    private Clip cursorClipLauncher;

    private final StepViewPosition positionHandler;
    private final ResolutionHander resolutionHandler;
    private final SeqClipHandler clipHandler;
    private final RecurrenceEditor recurrenceEditor;
    private final PadHandler padHandler;

    private final BooleanValueObject muteMode = new BooleanValueObject();
    private final BooleanValueObject soloMode = new BooleanValueObject();
    private final BooleanValueObject selectHeld = new BooleanValueObject();
    private final BooleanValueObject copyHeld = new BooleanValueObject();
    private final BooleanValueObject deleteHeld = new BooleanValueObject();
    private final BooleanValueObject fixedLengthHeld = new BooleanValueObject();
    private final BooleanValueObject shiftActive = new BooleanValueObject();
    private final BooleanValueObject altActive = new BooleanValueObject();
    private final BooleanValueObject lengthDisplay = new BooleanValueObject();

    private final BooleanValueObject muteActionsTaken = new BooleanValueObject();
    private final BooleanValueObject soloActionsTaken = new BooleanValueObject();

    private int playingStep;
    // This defines the length
    private final double gatePercent = 0.48;
    private boolean markIgnoreOrigLen = false;
    private final AccentHandler accentHandler;
    private final NoteRepeatHandler noteRepeatHandler;
    private NoteAction pendingAction;
    private NoteStep copyNote = null;
    private int blinkState;
    private final List<Integer> pendingBankHeldSteps = new ArrayList<>();
    private final Map<Integer, Integer> heldStepFineStarts = new HashMap<>();
    private final Map<Integer, Integer> pendingBankHeldFineStarts = new HashMap<>();
    private int pendingBankDir = 0;
    private boolean pendingBankForceFractional = false;
    private boolean pendingBankWasAlt = false;
    private boolean mainEncoderPressConsumed = false;
    private CursorRemoteControlsPage activeRemoteControlsPage;
    private final EncoderStepAccumulator euclidLengthEncoder = new EncoderStepAccumulator(3);
    private final EncoderStepAccumulator euclidPulsesEncoder = new EncoderStepAccumulator(3);
    private final EncoderStepAccumulator euclidRotationEncoder = new EncoderStepAccumulator(3);
    private final EncoderStepAccumulator euclidAccentEncoder = new EncoderStepAccumulator(3);


    public DrumSequenceMode(final AkaiFireDrumSeqExtension driver, final NoteRepeatHandler noteRepeatHandler) {

        super(driver.getLayers(), "DRUM_SEQUENCE_LAYER");
        this.driver = driver;
        this.noteRepeatHandler = noteRepeatHandler;
        host = driver.getHost();
        oled = driver.getOled();
        app = host.createApplication();
        mainLayer = new Layer(getLayers(), getName() + "_MAIN");
        shiftLayer = new Layer(getLayers(), getName() + "_SHIFT");
        muteLayer = new Layer(getLayers(), getName() + "_MUTE");
        soloLayer = new Layer(getLayers(), getName() + "_SOLO");

        currentLayer = mainLayer;
        accentHandler = new AccentHandler(this);
        resolutionHandler = new ResolutionHander(this);

        cursorTrack = driver.getViewControl().getCursorTrack();
        cursorTrack.name().markInterested();
        cursorTrack.isPinned().markInterested();
        cursorClip = cursorTrack.createLauncherCursorClip("SQClip", "SQClip", 32, 1);
        bigCursorClip = host.createLauncherCursorClip( 16*8*2*2,128);
        bigCursorClip.setStepSize(FINE_STEP_SIZE);
        bigCursorClip.addStepDataObserver(this::observingNotes);
        bigCursorClip.scrollToKey(0);



        cursorClip.addNoteStepObserver(this::handleNoteStep);
        cursorClip.playingStep().addValueObserver(this::handlePlayingStep);
        cursorClip.getLoopLength().addValueObserver(clipLength -> {
            if (markIgnoreOrigLen) {
                markIgnoreOrigLen = false;
            }
        });
        cursorClip.isPinned().markInterested();

        positionHandler = new StepViewPosition(cursorClip, 32, "AKAI");

        padHandler = new PadHandler(driver, this, mainLayer, muteLayer, soloLayer, noteRepeatHandler);
        clipHandler = new SeqClipHandler(driver, this, mainLayer);
        recurrenceEditor = new RecurrenceEditor(driver, this);

        initSequenceSection(driver);
        initModeButtons(driver);
        initButtonBehaviour(driver);
        encoderLayer = new SequencEncoderHandler(this, driver);

        muteMode.addValueObserver(active -> {
            if (active) {
                muteLayer.activate();
            } else {
                muteLayer.deactivate();
            }
        });

        soloMode.addValueObserver(active -> {
            if (active) {
                soloLayer.activate();
            } else {
                soloLayer.deactivate();
            }
        });
        copyHeld.addValueObserver(held -> {
            if (!held && copyNote != null) {
                copyNote = null;
            }
        });

        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.setStepSize(0.4);
        mainEncoder.bindEncoder(mainLayer, this::handleMainEncoder);
        mainEncoder.bindTouched(mainLayer, this::handeMainEncoderPress);
    }


    private void observingNotes(int x, int y, int state) {


        host.println("Observing note: x=" + x + ", y=" + y + ", stat=" + state);
        // Get or create the inner map for step x.
        Map<Integer, Integer> stepNotes = currentNotesInClip.get(x);
        if (stepNotes == null) {
            stepNotes = new HashMap<>();
            currentNotesInClip.put(x, stepNotes);
        }
        if (state == State.Empty.ordinal()) {
            stepNotes.remove(y);
            host.println("Removed note at x=" + x + ", y=" + y);
        } else {
            stepNotes.put(y, state);
            host.println("Stored note at x=" + x + ", y=" + y + ", stat=" + state);
        }
    }


    private void initModeButtons(final AkaiFireDrumSeqExtension driver) {
        final MultiStateHardwareLight[] stateLights = driver.getStateLights();
        bindEditButton(driver.getButton(NoteAssign.MUTE_1), "Select", selectHeld, stateLights[0], muteMode,
                muteActionsTaken);
        bindEditButton(driver.getButton(NoteAssign.MUTE_2), "Last Step", fixedLengthHeld, stateLights[1], soloMode,
                soloActionsTaken);
        bindEditButton(driver.getButton(NoteAssign.MUTE_3), "Copy", copyHeld, stateLights[2], null, null);
        bindEditButton(driver.getButton(NoteAssign.MUTE_4), "Delete/Reset", deleteHeld, stateLights[3], null, null);
        final BiColorButton deleteButton = driver.getButton(NoteAssign.MUTE_4);
        deleteButton.bind(mainLayer, deleteHeld, BiColorLightState.GREEN_FULL, BiColorLightState.OFF);
    }

    private void initButtonBehaviour(final AkaiFireDrumSeqExtension driver) {

        final BiColorButton accentButton = driver.getButton(NoteAssign.STEP_SEQ);
        accentButton.bindPressed(mainLayer, this::handleStepSeqPressed, this::getStepSeqLightState);

        final BiColorButton shiftButton = driver.getButton(NoteAssign.SHIFT);
        shiftButton.bind(mainLayer, shiftActive, BiColorLightState.GREEN_HALF, BiColorLightState.OFF);

        final BiColorButton altButton = driver.getButton(NoteAssign.ALT);
        altButton.bind(mainLayer, altActive, BiColorLightState.GREEN_HALF, BiColorLightState.OFF);

        final BiColorButton shiftLeftButton = driver.getButton(NoteAssign.BANK_L);
        shiftLeftButton.bindPressed(mainLayer, p -> handleBankButton(p, -1), BiColorLightState.HALF,
                BiColorLightState.OFF);

        final BiColorButton shiftRightButton = driver.getButton(NoteAssign.BANK_R);
        shiftRightButton.bindPressed(mainLayer, p -> handleBankButton(p, 1), BiColorLightState.HALF,
                BiColorLightState.OFF);

        //This is used for a centralized location for pattern buttons
        bindPatternButtons(driver);

    }

    //TODO DOGGY
    private void initSequenceSection(final AkaiFireDrumSeqExtension driver) {
        final RgbButton[] rgbButtons = driver.getRgbButtons();
        for (int i = 0; i < 32; i++) {
            final RgbButton button = rgbButtons[i + 32];
            final int index = i;
            button.bindPressed(mainLayer, p -> handleSeqSelection(index, p), () -> stepState(index));
        }
    }

    private void handleSeqSelection(final int index, final boolean pressed) {
        final NoteStep note = assignments[index];
        if (!pressed) {
            heldSteps.remove(index);
            heldStepFineStarts.remove(index);
            if (copyHeld.get() || fixedLengthHeld.get()) {
                // do nothing
            } else if (note != null && note.state() == State.NoteOn && !addedSteps.contains(index)) {
                if (!modifiedSteps.contains(index)) {
                    cursorClip.toggleStep(index, 0, accentHandler.getCurrenVel());
                } else {
                    modifiedSteps.remove(index);
                }
            }
            addedSteps.remove(index);
        } else {
            heldSteps.add(index);
            primeHeldStepFineStart(index, note);
            if (fixedLengthHeld.get()) {
                stepActionFixedLength(index);
            } else if (copyHeld.get()) {
                handleNoteCopyAction(index, note);
            } else if (accentHandler.isHolding()) {
                handleAccentStepAction(index, note);
            } else {
                if (note == null || note.state() == State.Empty || note.state() == State.NoteSustain) {
                    cursorClip.setStep(index, 0, accentHandler.getCurrenVel(),
                            positionHandler.getGridResolution() * gatePercent);
                    addedSteps.add(index);
                    heldStepFineStarts.put(index, coarseLower(index));
                }
            }
        }
    }

    private void handleNoteCopyAction(final int index, final NoteStep note) {
        if (copyNote != null) {
            if (index == copyNote.x()) {
                return;
            }
            final int vel = (int) Math.round(copyNote.velocity() * 127);
            final double duration = copyNote.duration();
            expectedNoteChanges.put(index, copyNote);
            cursorClip.setStep(index, 0, vel, duration);
            heldStepFineStarts.put(index, coarseLower(index));
        } else if (note != null && note.state() == State.NoteOn) {
            copyNote = note;
        }
    }

    private RgbLigthState stepState(final int index) {
        final int steps = positionHandler.getAvailableSteps();
        if (index < steps) {
            final State state = assignments[index] == null ? State.Empty : assignments[index].state();

            if (state == State.Empty) {
                return emptyNoteState(index);
            } else if (state == State.NoteSustain) {
                if (lengthDisplay.get()) {
                    if (index == playingStep) {
                        return padHandler.getCurrentPadColor().getBrightend();
                    }
                    return padHandler.getCurrentPadColor().getVeryDimmed();
                }
                return emptyNoteState(index);
            }

            if (copyNote != null && copyNote.x() == index) {
                if (blinkState % 4 < 2) {
                    return RgbLigthState.GRAY_1;
                }
                return padHandler.getCurrentPadColor();
            }
            if (index == playingStep) {
                return padHandler.getCurrentPadColor().getBrightend();
            }
            return padHandler.getCurrentPadColor();

        }
        return RgbLigthState.OFF;
    }

    private RgbLigthState emptyNoteState(final int index) {
        if (index == playingStep) {
            return RgbLigthState.WHITE;
        }
        if (index / 4 % 2 == 0) {
            return RgbLigthState.GRAY_1;
        } else {
            return RgbLigthState.GRAY_2;
        }
    }

    // Declare a field to hold the original (normal) step size.
    // private final double originalStepSize = 1.0 / 16.0; // one grid step = 1/16 beat (a 64th note)

    // Modify your movePattern method to choose between whole and fractional shifting:
    private void movePattern(final int dir, final boolean forceFractional, final List<Integer> heldStepSnapshot,
                             final Map<Integer, Integer> heldFineStartSnapshot) {
        final boolean heldOnly = !heldStepSnapshot.isEmpty();
        final boolean fractional = forceFractional || heldOnly;
        oled.paramInfo(heldOnly ? "STEP NUDGE" : fractional ? "NUDGE" : "SHIFT", dir > 0 ? "+1" : "-1");
        if (fractional) {
            movePatternFractional(bigCursorClip, dir, heldOnly, heldStepSnapshot, heldFineStartSnapshot);
        } else {
            movePatternWhole(dir);
        }
    }

    // Existing whole-step shifting (unchanged):
    private void movePatternWhole(final int dir) {
        final List<NoteStep> notes = getOnNotes();
        final int availableSteps = positionHandler.getAvailableSteps();
        cursorClip.clearStepsAtY(0, 0);

        for (final NoteStep noteStep : notes) {
            int pos = noteStep.x() + dir;
            if (pos < 0) {
                pos = availableSteps - 1;
            } else if (pos >= availableSteps) {
                pos = 0;
            }
            if (!shiftActive.get()) {
                expectedNoteChanges.put(pos, noteStep);
            }
            cursorClip.setStep(pos, 0, (int) Math.round(noteStep.velocity() * 127), noteStep.duration());
        }
    }


    /**
     * Map a fine-grid x (0–511) to the coarse step index (0–31), and its bounds.
     * Each coarse step spans {@code finePerStep} fine ticks (e.g., 4 ticks when grid is 1/16 and fine is 1/64).
     */
    private int finePerStep() {
        // positionHandler grid resolution is the coarse step size (e.g., 1/16).
        // FINE_STEP_SIZE is 1/64, so fine ticks per coarse step ~4; clamp to at least 1.
        return Math.max(1, (int)Math.round(positionHandler.getGridResolution() / FINE_STEP_SIZE));
    }

    private int coarseStepIndex(int fineX) {
        int fps = finePerStep();
        return Math.max(0, Math.min(31, fineX / fps));
    }

    private int coarseLower(int coarseIndex) {
        return coarseIndex * finePerStep();
    }

    private int coarseUpper(int coarseIndex) {
        return coarseLower(coarseIndex) + finePerStep() - 1;
    }

    private Integer resolveHeldFineStart(final int coarseIndex, final int targetMidi) {
        if (targetMidi < 0 || coarseIndex < 0 || coarseIndex >= positionHandler.getAvailableSteps()) {
            return null;
        }
        for (int fineX = coarseLower(coarseIndex); fineX <= coarseUpper(coarseIndex); fineX++) {
            final Map<Integer, Integer> notes = currentNotesInClip.get(fineX);
            if (notes != null && notes.containsKey(targetMidi)) {
                return fineX;
            }
        }
        return null;
    }

    private void primeHeldStepFineStart(final int index, final NoteStep note) {
        final int targetMidi = padHandler.getSelectedPadMidi();
        final Integer resolved = resolveHeldFineStart(index, targetMidi);
        if (resolved != null) {
            heldStepFineStarts.put(index, resolved);
        } else if (note != null && note.state() == State.NoteOn) {
            heldStepFineStarts.put(index, coarseLower(index));
        } else {
            heldStepFineStarts.remove(index);
        }
    }


    /**
     * Nudges (moves) notes in the fine grid while keeping them within the allowed
     * range for their corresponding normal note. The allowed range for a given normal note _n_
     * is from getAllowedLowerBound(n) to getAllowedUpperBound(n). We only process notes
     * that have a state of 2 (in our filtered inner map) and only if their normal note is held.
     */

    private void movePatternFractional(Clip clip, int dir, boolean heldOnly, List<Integer> heldStepSnapshot,
                                       Map<Integer, Integer> heldFineStartSnapshot) {
        // Prevent held pads from being toggled off on release after a nudge.
        heldStepSnapshot.forEach(modifiedSteps::add);

        final int targetMidi = padHandler.getSelectedPadMidi();
        if (targetMidi < 0) {
            return;
        }

        final int finePerStep = finePerStep();
        final int maxFine = positionHandler.getAvailableSteps() * finePerStep;
        if (maxFine <= 0) {
            return;
        }

        // Build explicit targets instead of sweeping the entire fine grid.
        List<Integer> targets = new ArrayList<>();
        Map<Integer, Integer> heldTargets = new HashMap<>();
        if (heldOnly) {
            heldStepSnapshot.stream()
                    .filter(idx -> idx >= 0 && idx < positionHandler.getAvailableSteps())
                    .distinct()
                    .sorted()
                    .forEach(coarse -> {
                        Integer fineX = heldFineStartSnapshot.get(coarse);
                        if (fineX == null) {
                            fineX = resolveHeldFineStart(coarse, targetMidi);
                            if (fineX != null) {
                                heldStepFineStarts.put(coarse, fineX);
                            }
                        }
                        if (fineX != null && fineX >= 0 && fineX < maxFine) {
                            targets.add(fineX);
                            heldTargets.put(fineX, coarse);
                        }
                    });
        } else {
            final Set<Integer> coarseSeen = new HashSet<>();
            for (Map.Entry<Integer, Map<Integer, Integer>> entry : currentNotesInClip.entrySet()) {
                int fineX = entry.getKey();
                if (fineX < 0 || fineX >= maxFine) {
                    continue;
                }
                int coarseIndex = coarseStepIndex(fineX);
                if (coarseSeen.contains(coarseIndex)) {
                    continue;
                }
                if (entry.getValue().containsKey(targetMidi)) {
                    targets.add(fineX);
                    coarseSeen.add(coarseIndex);
                }
            }
        }
        targets.sort(dir > 0 ? Comparator.reverseOrder() : Comparator.naturalOrder());

        for (Integer fineX : targets) {
            Map<Integer, Integer> stepNotes = currentNotesInClip.get(fineX);
            if (stepNotes == null && !heldOnly) {
                continue;
            }
            Integer state = stepNotes == null ? null : stepNotes.get(targetMidi);
            if (!heldOnly && (state == null || state == State.Empty.ordinal())) {
                continue;
            }
            if (heldOnly && (state == null || state == State.Empty.ordinal())) {
                state = State.NoteOn.ordinal();
            }

            int newFineX = fineX + dir;
            if (newFineX < 0) {
                newFineX += maxFine;
            } else if (newFineX >= maxFine) {
                newFineX -= maxFine;
            }
            int delta = newFineX - fineX;
            if (delta == 0) {
                continue;
            }

            try {
                clip.moveStep(fineX, targetMidi, delta, 0);
            } catch (Exception e) {
                host.errorln("Error moving note at fineX " + fineX + " y = " + targetMidi + ": " + e.getMessage());
                continue;
            }

            // Update local cache: remove old, add new.
            if (stepNotes != null) {
                stepNotes.remove(targetMidi);
            }
            if (stepNotes != null && stepNotes.isEmpty()) {
                currentNotesInClip.remove(fineX);
            }
            Map<Integer, Integer> newMap = currentNotesInClip.computeIfAbsent(newFineX, k -> new HashMap<>());
            newMap.put(targetMidi, state);
            if (heldOnly) {
                final Integer coarseIndex = heldTargets.get(fineX);
                if (coarseIndex != null) {
                    heldStepFineStarts.put(coarseIndex, newFineX);
                }
            }

        }
    }


    private BiColorLightState getPinnedState() {
        return cursorTrack.isPinned().get() ? BiColorLightState.HALF : BiColorLightState.OFF;
    }

    private void handleClipPinning(final boolean pressed) {
        if (pressed) {
            cursorTrack.isPinned().toggle();
            oled.paramInfo((cursorTrack.isPinned().get() ? "UNPIN" : "PIN") + " Track", "TR:" + cursorTrack.name().get());
        } else {
            oled.clearScreenDelayed();
        }
    }

    private void handleMainEncoder(final int inc) {
        if (driver.isPopupBrowserActive()) {
            return;
        }
        if (accentHandler.isHolding()) {
            accentHandler.handleMainEncoder(inc);
        } else {
            final String mainEncoderRole = driver.getMainEncoderRolePreference();
            final boolean fine = isShiftHeld();
            if (FireControlPreferences.MAIN_ENCODER_NOTE_REPEAT.equals(mainEncoderRole)) {
                padHandler.handleMainEncoder(inc);
            } else if (FireControlPreferences.MAIN_ENCODER_SHUFFLE.equals(mainEncoderRole)) {
                driver.adjustGrooveShuffleAmount(inc, fine);
            } else if (FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED.equals(mainEncoderRole)) {
                driver.adjustMainCursorParameter(inc, fine);
            }
        }
    }

    private void handleAccentStepAction(final int index, final NoteStep note) {
        accentHandler.markModified();
        if (note == null || note.state() == State.Empty || note.state() == State.NoteSustain) {
            cursorClip.setStep(index, 0, accentHandler.getAccentedVelocity(),
                    positionHandler.getGridResolution() * gatePercent);
            addedSteps.add(index);
            heldStepFineStarts.put(index, coarseLower(index));
            return;
        }

        final int targetVelocity = accentHandler.isAccented(note)
                ? accentHandler.getStandardVelocity()
                : accentHandler.getAccentedVelocity();
        note.setVelocity(targetVelocity / 127.0);
        modifiedSteps.add(index);
    }

    private void handeMainEncoderPress(final boolean press) {
        if (driver.isPopupBrowserActive()) {
            return;
        }
        if (accentHandler.isHolding()) {
            accentHandler.handeMainEncoderPress(press);
            return;
        }
        if (press && isShiftHeld()) {
            mainEncoderPressConsumed = true;
            driver.cycleMainEncoderRolePreference();
            return;
        }
        if (!press && mainEncoderPressConsumed) {
            mainEncoderPressConsumed = false;
            return;
        }
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (FireControlPreferences.MAIN_ENCODER_LAST_TOUCHED.equals(mainEncoderRole)) {
            if (press) {
                driver.showMainCursorParameterInfo();
            } else {
                driver.resetMainCursorParameter();
            }
        } else if (FireControlPreferences.MAIN_ENCODER_NOTE_REPEAT.equals(mainEncoderRole)) {
            padHandler.getNoteRepeaterHandler().handlePressed(press);
        } else if (FireControlPreferences.MAIN_ENCODER_SHUFFLE.equals(mainEncoderRole)) {
            if (press) {
                driver.showGrooveShuffleInfo();
            } else {
                driver.toggleGrooveEnabled();
            }
        }
    }

    public BooleanValueObject getShiftActive() {
        return shiftActive;
    }

    public BooleanValueObject getDeleteHeld() {
        return deleteHeld;
    }

    public void notifyBlink(final int blinkTicks) {
        blinkState = blinkTicks;
        if (clipHandler != null)
        clipHandler.notifyBlink(blinkTicks);
    }

    public OledDisplay getOled() {
        return oled;
    }

    private void handleEuclidAdjust(int index, int inc) {
        final int oldLength = euclidState.getLength();
        final int oldPulses = euclidState.getPulses();
        final int oldRotation = euclidState.getRotation();
        final int oldAccentPulses = euclidState.getAccentPulses();
        switch (index) {
            case 0 -> euclidState.incLength(inc);
            case 1 -> euclidState.incPulses(inc);
            case 2 -> euclidState.incRotation(inc);
            case 3 -> euclidState.incAccentPulses(inc);
            default -> {
            }
        }
        if ((index == 0 || index == 1 || index == 2 || index == 3)
                && (oldLength != euclidState.getLength()
                || oldPulses != euclidState.getPulses()
                || oldRotation != euclidState.getRotation()
                || oldAccentPulses != euclidState.getAccentPulses())) {
            applyEuclid(true);
        }
        oled.paramInfo(valueForIndex(index), infoForIndex(index));
    }

    private String infoForIndex(int idx) {
        return switch (idx) {
            case 0 -> "LEN";
            case 1 -> "PULS";
            case 2 -> "ROT";
            case 3 -> "ACC";
            default -> "";
        };
    }

    private String valueForIndex(int idx) {
        return switch (idx) {
            case 0 -> String.valueOf(euclidState.getLength());
            case 1 -> String.valueOf(euclidState.getPulses());
            case 2 -> String.valueOf(euclidState.getRotation());
            case 3 -> String.valueOf(euclidState.getAccentPulses());
            default -> "";
        };
    }

    private void handleOccurrenceAdjust(final int inc) {
        final List<NoteStep> heldNotes = getHeldNotes();
        if (heldNotes.isEmpty()) {
            return;
        }
        final NoteOccurrence[] values = NoteOccurrence.values();
        for (final NoteStep note : heldNotes) {
            final NoteOccurrence current = note.occurrence();
            int currentIndex = -1;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == current) {
                    currentIndex = i;
                    break;
                }
            }
            final int nextIndex = Math.max(0, Math.min(values.length - 1, currentIndex + inc));
            note.setOccurrence(values[nextIndex]);
        }
        registerModifiedSteps(heldNotes);
    }

    private String currentOccurrenceValue() {
        final List<NoteStep> heldNotes = getHeldNotes();
        if (heldNotes.isEmpty()) {
            return "-";
        }
        return heldNotes.get(0).occurrence().toString().replace("_", " ");
    }

    public void applyEuclid(boolean clearFirst) {
        if (driver.isEuclidFullClipEnabled()) {
            applyEuclidFullClip();
        } else {
            applyEuclidVisiblePage();
        }
    }

    private void applyEuclidVisiblePage() {
        int padMidi = padHandler.getSelectedPadMidi();
        if (padMidi < 0) {
            return;
        }
        final int available = positionHandler.getAvailableSteps();
        if (available <= 0) {
            return;
        }
        int patternLen = Math.max(1, Math.min(euclidState.getLength(), available));
        boolean[] pattern = EuclidUtil.build(patternLen, Math.min(euclidState.getPulses(), patternLen));
        pattern = EuclidUtil.rotate(pattern, euclidState.getRotation());
        final boolean[] accentPattern = buildAccentPattern(pattern, patternLen);
        if (euclidState.isInverted()) {
            for (int i = 0; i < pattern.length; i++) {
                pattern[i] = !pattern[i];
            }
        }
        final double grid = positionHandler.getGridResolution();
        final double gateBase = grid > 0 ? grid * 0.48 : FINE_STEP_SIZE;
        final double gate = Math.max(gateBase, 1.0 / 256.0);
        cursorClip.clearStepsAtY(0, 0);
        for (int i = 0; i < available; i++) {
            if (pattern[i % patternLen]) {
                final int velocity = accentPattern[i % patternLen]
                        ? accentHandler.getAccentedVelocity()
                        : accentHandler.getStandardVelocity();
                cursorClip.setStep(i, 0, velocity, gate);
            }
        }
        oled.detailInfo("EUCLID", "Applied Page");
        oled.clearScreenDelayed();
    }

    private void applyEuclidFullClip() {
        int padMidi = padHandler.getSelectedPadMidi();
        if (padMidi < 0) {
            return;
        }
        final int totalSteps = positionHandler.getSteps();
        if (totalSteps <= 0) {
            return;
        }
        int patternLen = Math.max(1, Math.min(euclidState.getLength(), totalSteps));
        boolean[] pattern = EuclidUtil.build(patternLen, Math.min(euclidState.getPulses(), patternLen));
        pattern = EuclidUtil.rotate(pattern, euclidState.getRotation());
        final List<Integer> activeSteps = new ArrayList<>();
        for (int i = 0; i < patternLen; i++) {
            if (pattern[i]) {
                activeSteps.add(i);
            }
        }
        final boolean[] accentPattern = new boolean[patternLen];
        final int accentCount = Math.min(euclidState.getAccentPulses(), activeSteps.size());
        if (accentCount > 0 && !activeSteps.isEmpty()) {
            final boolean[] accentSubset = EuclidUtil.build(activeSteps.size(), accentCount);
            for (int i = 0; i < activeSteps.size(); i++) {
                if (accentSubset[i]) {
                    accentPattern[activeSteps.get(i)] = true;
                }
            }
        }
        if (euclidState.isInverted()) {
            for (int i = 0; i < pattern.length; i++) {
                pattern[i] = !pattern[i];
            }
        }
        final double grid = positionHandler.getGridResolution();
        final double gateBase = grid > 0 ? grid * 0.48 : FINE_STEP_SIZE;
        final double gate = Math.max(gateBase, 1.0 / 256.0);
        final int originalPage = positionHandler.getCurrentPage();
        final int pages = positionHandler.getPages();
        cursorClip.clearStepsAtY(0, 0);
        for (int page = 0; page < pages; page++) {
            cursorClip.scrollToStep(page * 32);
            final int pageStart = page * 32;
            final int pageSteps = Math.min(32, Math.max(0, totalSteps - pageStart));
            for (int localIndex = 0; localIndex < pageSteps; localIndex++) {
                final int globalIndex = pageStart + localIndex;
                if (pattern[globalIndex % patternLen]) {
                    final int velocity = accentPattern[globalIndex % patternLen]
                            ? accentHandler.getAccentedVelocity()
                            : accentHandler.getStandardVelocity();
                    cursorClip.setStep(localIndex, 0, velocity, gate);
                }
            }
        }
        cursorClip.scrollToStep(originalPage * 32);
        oled.detailInfo("EUCLID", "Applied Full Clip");
        oled.clearScreenDelayed();
    }

    private boolean[] buildAccentPattern(final boolean[] pattern, final int patternLen) {
        final List<Integer> activeSteps = new ArrayList<>();
        for (int i = 0; i < patternLen; i++) {
            if (pattern[i]) {
                activeSteps.add(i);
            }
        }
        final boolean[] accentPattern = new boolean[patternLen];
        final int accentCount = Math.min(euclidState.getAccentPulses(), activeSteps.size());
        if (accentCount > 0 && !activeSteps.isEmpty()) {
            final boolean[] accentSubset = EuclidUtil.build(activeSteps.size(), accentCount);
            for (int i = 0; i < activeSteps.size(); i++) {
                if (accentSubset[i]) {
                    accentPattern[activeSteps.get(i)] = true;
                }
            }
        }
        return accentPattern;
    }
    private void bindEditButton(final BiColorButton button, final String name, final BooleanValueObject value,
                                final MultiStateHardwareLight stateLight, final BooleanValueObject altValue,
                                final BooleanValueObject altActionHappenedFlag) {
        if (altValue == null) {
            final FunctionInfo info1 = FunctionInfo.INFO1.get(button.getNoteAssign());
            button.bind(mainLayer, value, BiColorLightState.GREEN_FULL, BiColorLightState.OFF);
            mainLayer.bindLightState(() -> BiColorLightState.AMBER_HALF, stateLight);
            value.addValueObserver(active -> handleEditValueChanged(button, active, info1));
            mainLayer.bindLightState(
                    () -> button.isPressed() ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF, stateLight);
        } else {
            final BooleanValueObject alternateFunctionActive = new BooleanValueObject();
            final FunctionInfo info1 = FunctionInfo.INFO1.get(button.getNoteAssign());
            value.addValueObserver(active -> handleEditValueChanged(button, active, info1));
            final FunctionInfo info2 = FunctionInfo.INFO2.get(button.getNoteAssign());
            altValue.addValueObserver(active -> handleEditValueChanged(button, active, info2));
            button.bindPressed(mainLayer,
                    pressed -> handleModeButtonWithAlternatePressed(value, altValue, alternateFunctionActive,
                            altActionHappenedFlag, info1, info2, pressed),  //
                    () -> button.isPressed() ? BiColorLightState.GREEN_FULL : BiColorLightState.OFF);
            mainLayer.bindLightState(() -> {
                final boolean active = button.isPressed() && !getShiftActive().get();
                if (alternateFunctionActive.get()) {
                    return active ? BiColorLightState.RED_FULL : BiColorLightState.RED_HALF;
                }
                return active ? BiColorLightState.AMBER_FULL : BiColorLightState.AMBER_HALF;
            }, stateLight);
        }
    }

    private void handleModeButtonWithAlternatePressed(final BooleanValueObject mainValue,
                                                      final BooleanValueObject altValue,
                                                      final BooleanValueObject alternateFunctionActive,
                                                      final BooleanValueObject actionTakenFlag,
                                                      final FunctionInfo info1, final FunctionInfo info2,
                                                      final Boolean pressed) {
        if (pressed) {
            if (getShiftActive().get()) {
                alternateFunctionActive.set(!alternateFunctionActive.get());
            } else {
                alternateFunctionActive.set(false);
            }
            boolean isAlternateFunctionActive = alternateFunctionActive.get();

            mainValue.set(!alternateFunctionActive.get());
            altValue.set(alternateFunctionActive.get());

            actionTakenFlag.set(true);
            oled.functionInfo(
                    getPadInfo(),
                    isAlternateFunctionActive ? info2.getName(false) : info1.getName(false),
                    isAlternateFunctionActive ? info2.getDetail() : info1.getDetail()
            );
        }

        if (!pressed) {
            mainValue.set(false);
            if (!alternateFunctionActive.get()) {
                altValue.set(false);
            }

            actionTakenFlag.set(false);
            oled.clearScreenDelayed();
        }
    }

    public String getPadInfo() {
        return padHandler.getPadInfo();
    }

    private void handleEditValueChanged(final BiColorButton button, final boolean active, final FunctionInfo info) {
        if (active) {
            if (padHandler.notePlayingEnabled()) {
                padHandler.disableNotePlaying();
            }
            oled.functionInfo(getPadInfo(), info.getName(shiftActive.get()), info.getDetail());
        } else {
            oled.clearScreenDelayed();
            if (padHandler.notePlayingEnabled()) {
                padHandler.applyScale();
            }
        }
    }

    @Override
    public double getGridResolution() {
        return positionHandler.getGridResolution();
    }

    @Override
    public String getDetails(final List<NoteStep> heldNotes) {
        return getPadInfo() + " <" + heldNotes.size() + ">";
    }

    public void registerModifiedSteps(final List<NoteStep> notes) {
        notes.forEach(s -> modifiedSteps.add(s.x()));
    }

    @Override
    public List<NoteStep> getHeldNotes() {
        return heldSteps.stream()
                // Only use indices within 0 to 31.
                .filter(idx -> idx >= 0 && idx <= 31)
                .map(idx -> assignments[idx])
                .filter(ns -> ns != null && ns.state() == State.NoteOn)
                .collect(Collectors.toList());
    }

    @Override
    public List<NoteStep> getOnNotes() {
        return Arrays.stream(assignments)
                .filter(ns -> ns != null && ns.state() == State.NoteOn)
                .collect(Collectors.toList());
    }

    public void registerPendingAction(final NoteAction action) {
        pendingAction = action;
    }

    public NoteAction getPendingAction() {
        return pendingAction;
    }

    public void clearPendingAction() {
        pendingAction = null;
    }

    private void stepActionFixedLength(final int index) {
        final double newLen = positionHandler.lengthWithLastStep(index);

        if (shiftActive.get()) {
            // NOTE: duplicate content when doubling the size of clip
            double curLen = cursorClip.getLoopLength().get();
            while (newLen % curLen == 0 && newLen > curLen) {
                curLen = curLen * 2;
                cursorClip.duplicateContent();
            }
        }

        adjustMode(newLen);
        cursorClip.getLoopLength().set(newLen);
    }

    private void adjustMode(final double clipLength) {
        final int notes = (int) (clipLength / 0.25);
        adjustMode(notes);
    }

    private void adjustMode(final int notes) {
        if (notes % 8 == 0) {
            cursorClip.launchMode().set("default");
        } else {
            cursorClip.launchMode().set(FireControlPreferences.toClipLaunchModeValue(driver.getClipLaunchModePreference()));
        }
    }

    private void handleNoteStep(final NoteStep noteStep) {
       int jaja =  noteStep.x();
        final int newStep = noteStep.x();

        assignments[newStep] = noteStep;
        if (expectedNoteChanges.containsKey(newStep)) {
            final NoteStep previousStep = expectedNoteChanges.get(newStep);
            expectedNoteChanges.remove(newStep);
            applyValues(noteStep, previousStep);
        }
    }

    private void applyValues(final NoteStep dest, final NoteStep src) {
        // TODO: this is a bug, somewhere the chance is lost
        dest.setChance(src.chance()); // src.chance()
        dest.setTimbre(src.timbre());
        dest.setPressure(src.pressure());
        dest.setRepeatCount(src.repeatCount());
        dest.setRepeatVelocityCurve(src.repeatVelocityCurve());
        dest.setPan(src.pan());
        dest.setRepeatVelocityEnd(src.repeatVelocityEnd());
        dest.setRecurrence(src.recurrenceLength(), src.recurrenceMask());
        dest.setOccurrence(src.occurrence());
    }

    private void handlePlayingStep(final int playingStep) {
        if (playingStep == -1) {
            this.playingStep = -1;
        }
        this.playingStep = playingStep - positionHandler.getStepOffset();
    }

    @Override
    protected void onActivate() {
        currentLayer = mainLayer;
        mainLayer.activate();
        encoderLayer.activate();
        padHandler.applyScale();
    }

    @Override
    protected void onDeactivate() {
        currentLayer.deactivate();
        shiftLayer.deactivate();
        encoderLayer.deactivate();
        padHandler.disableNotePlaying();
    }

    public void retrigger() {
        cursorClip.launch();
    }

    public StepViewPosition getPositionHandler() {
        return positionHandler;
    }

    PinnableCursorClip getCursorClip() {
        return cursorClip;
    }

    boolean isShiftHeld() {
        return shiftActive.get();
    }

    public boolean isAltHeld() {
        return altActive.get();
    }


    boolean isCopyHeld() {
        return copyHeld.get();
    }

    boolean isDeleteHeld() {
        return deleteHeld.get();
    }

    @Override
    public boolean isSelectHeld() {
        return selectHeld.get();
    }

    public void exitRecurrenceEdit() {
        recurrenceEditor.exitRecurrenceEdit();
    }

    public void enterRecurrenceEdit(final List<NoteStep> notes) {
        recurrenceEditor.enterRecurrenceEdit(notes);
    }

    public void updateRecurrencLength(final int length) {
        recurrenceEditor.updateLength(length);
    }

    public IntSetValue getHeldSteps() {
        return heldSteps;
    }

    public boolean isPadBeingHeld() {
        return padHandler.isPadBeingHeld();
    }

    public void registerExpectedNoteChange(final int x, final NoteStep noteStep) {
        expectedNoteChanges.put(noteStep.x(), noteStep);
    }

    public BooleanValueObject getLengthDisplay() {
        return lengthDisplay;
    }

    public void notifyMuteAction() {
        muteActionsTaken.set(true);
    }

    public void notifySoloAction() {
        soloActionsTaken.set(true);
    }

    public AccentHandler getAccentHandler() {
        return accentHandler;
    }

    public PadHandler getPadHandler() {
        return padHandler;
    }

    public Application getApplication() {return app; }

    public void setActiveRemoteControlsPage(final CursorRemoteControlsPage remoteControlsPage) {
     this.activeRemoteControlsPage = remoteControlsPage;
    }

    public CursorRemoteControlsPage getActiveRemoteControlsPage() {
        return activeRemoteControlsPage;
    }

    @Override
    public void bindMixerPage(final SequencEncoderHandler handler, final Layer layer, final TouchEncoder[] encoders) {
        final String[] mixerParamNames = {"Volume", "Panning", "Send 1", "Send 2"};
        for (int i = 0; i < encoders.length; i++) {
            final int index = i;
            final String name = mixerParamNames[i];
            encoders[i].bindEncoder(layer, inc -> padHandler.modifyValue(index, inc));
            encoders[i].bindTouched(layer, touched -> {
                if (touched) {
                    padHandler.activateView(index, name);
                    padHandler.updateDisplay(index);
                } else {
                    padHandler.deactivateView();
                }
            });
        }
        padHandler.bindPadParameters(layer);
    }

    @Override
    public void bindUser2Page(final SequencEncoderHandler handler, final Layer layer, final TouchEncoder[] encoders) {
        encoders[0].setStepSize(0.1);
        encoders[1].setStepSize(0.1);
        encoders[0].bindEncoder(layer, inc -> {
            final int effective = euclidLengthEncoder.consume(inc);
            if (effective != 0) {
                handleEuclidAdjust(0, effective);
            }
        });
        encoders[1].bindEncoder(layer, inc -> {
            final int effective = euclidPulsesEncoder.consume(inc);
            if (effective != 0) {
                handleEuclidAdjust(1, effective);
            }
        });
        encoders[2].bindEncoder(layer, inc -> {
            final int effective = euclidRotationEncoder.consume(inc);
            if (effective != 0) {
                handleEuclidAdjust(2, effective);
            }
        });
        encoders[0].bindTouched(layer, touched -> handleUser2Touch(touched, 0));
        encoders[1].bindTouched(layer, touched -> handleUser2Touch(touched, 1));
        encoders[2].bindTouched(layer, touched -> handleUser2Touch(touched, 2));
        encoders[3].bindEncoder(layer, inc -> {
            final int effective = euclidAccentEncoder.consume(inc);
            if (effective != 0) {
                handleEuclidAdjust(3, effective);
            }
        });
        encoders[3].bindTouched(layer, touched -> handleUser2Touch(touched, 3));
    }

    private void handleUser2Touch(final boolean touched, final int index) {
        if (!touched) {
            if (index == 0) {
                euclidLengthEncoder.reset();
            } else if (index == 1) {
                euclidPulsesEncoder.reset();
            } else if (index == 2) {
                euclidRotationEncoder.reset();
            } else if (index == 3) {
                euclidAccentEncoder.reset();
            }
            oled.clearScreenDelayed();
            return;
        }
        oled.valueInfo(infoForIndex(index), valueForIndex(index));
    }

    @Override
    public String getModeInfo(final EncoderMode mode) {
        if (mode == EncoderMode.USER_2) {
            return "1: Euclid Len\n2: Euclid Pulses\n3: Euclid Rotation\n4: Accent Density";
        }
        return StepSequencerHost.super.getModeInfo(mode);
    }

    private void handleStepSeqPressed(final boolean pressed) {
        if (shiftActive.get()) {
            if (pressed) {
                driver.toggleFillMode();
                oled.valueInfo("Fill", driver.getFillLightState() == BiColorLightState.AMBER_FULL ? "On" : "Off");
            }
            return;
        }
        accentHandler.handlePressed(pressed);
    }

    private BiColorLightState getStepSeqLightState() {
        if (shiftActive.get()) {
            return driver.getFillLightState();
        }
        return accentHandler.getLightState();
    }

    private void handleBankButton(final boolean pressed, final int dir) {
        if (pressed) {
            pendingBankDir = dir;
            pendingBankForceFractional = shiftActive.get();
            pendingBankWasAlt = altActive.get();
            pendingBankHeldSteps.clear();
            pendingBankHeldFineStarts.clear();
            final int targetMidi = padHandler.getSelectedPadMidi();
            heldSteps.stream().sorted().forEach(index -> {
                pendingBankHeldSteps.add(index);
                Integer fineX = heldStepFineStarts.get(index);
                if (fineX == null) {
                    fineX = resolveHeldFineStart(index, targetMidi);
                    if (fineX != null) {
                        heldStepFineStarts.put(index, fineX);
                    }
                }
                if (fineX != null) {
                    pendingBankHeldFineStarts.put(index, fineX);
                }
            });
            if (pendingBankWasAlt) {
                resolutionHandler.adjust(dir);
            }
            return;
        }
        if (pendingBankWasAlt) {
            pendingBankWasAlt = false;
            pendingBankHeldSteps.clear();
            pendingBankHeldFineStarts.clear();
            return;
        }
        movePattern(pendingBankDir, pendingBankForceFractional, new ArrayList<>(pendingBankHeldSteps),
                new HashMap<>(pendingBankHeldFineStarts));
        pendingBankHeldSteps.clear();
        pendingBankHeldFineStarts.clear();
    }

    private void bindPatternButtons(AkaiFireDrumSeqExtension driver) {
        // Get the shared PatternButtons instance (make sure it’s created during init)
        PatternButtons patternButtons = driver.getPatternButtons();
        if (patternButtons == null) {
            host.println("PatternButtons is null in DrumSequenceMode.bindPatternButtons()");
            return;
        }
        // Bind a unified callback for the UP button:
        patternButtons.setUpCallback(pressed -> {
            if (pressed) {
                if (altActive.get()) {
                    // When Alt is held, scroll pads
                    padHandler.scrollForward(true);
                } else {
                    // Otherwise, toggle the encoder shift mode
                    encoderLayer.toggleShiftForCurrentMode();
                }
            }
        }, () -> BiColorLightState.HALF);

        // Bind a unified callback for the DOWN button:
        patternButtons.setDownCallback(pressed -> {
            if (pressed) {
                if (altActive.get()) {
                    // When Alt is held, scroll pads backward
                    padHandler.scrollBackward(true);
                } else {
                    // Otherwise, toggle the encoder shift mode
                    encoderLayer.toggleShiftForCurrentMode();
                }
            }
        }, () -> BiColorLightState.HALF);
    }


}
