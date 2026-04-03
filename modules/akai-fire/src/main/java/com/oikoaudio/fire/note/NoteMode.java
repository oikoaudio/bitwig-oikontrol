package com.oikoaudio.fire.note;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.MusicalScale;
import com.bitwig.extensions.framework.MusicalScaleLibrary;
import com.bitwig.extensions.framework.values.Midi;
import com.oikoaudio.fire.AkaiFireDrumSeqExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;
import com.oikoaudio.fire.utils.PatternButtons;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NoteMode extends Layer {
    private static final int PIANO_HIGHLIGHT_INDEX = -1;
    private static final int MIN_OCTAVE = 0;
    private static final int MAX_OCTAVE = 7;
    private static final int MIN_TRANSPOSE = 0;
    private static final int MAX_TRANSPOSE = MAX_OCTAVE * 12 + 11;
    private static final int HELD_NOTE_VELOCITY = 100;
    private static final int STEP_PAD_OFFSET = 32;
    private static final int STEP_COUNT = 32;
    private static final double STEP_LENGTH = 0.25;
    private static final double MIN_GATE_RATIO = 0.25;
    private static final double MAX_GATE_RATIO = 1.0;
    private static final RgbLigthState ROOT_COLOR = new RgbLigthState(120, 64, 0, true);
    private static final RgbLigthState IN_SCALE_COLOR = new RgbLigthState(0, 72, 110, true);
    private static final RgbLigthState PIANO_BLACK_KEY_COLOR = new RgbLigthState(0, 56, 120, true);
    private static final RgbLigthState PIANO_WHITE_KEY_COLOR = RgbLigthState.GRAY_2;
    private static final RgbLigthState OUT_OF_SCALE_COLOR = RgbLigthState.GRAY_1;
    private static final RgbLigthState EMPTY_STEP_A = RgbLigthState.GRAY_1;
    private static final RgbLigthState EMPTY_STEP_B = RgbLigthState.GRAY_2;
    private static final RgbLigthState OCCUPIED_STEP = new RgbLigthState(0, 90, 38, true);
    private static final RgbLigthState HELD_STEP = new RgbLigthState(120, 88, 0, true);
    private static final RgbLigthState DEFERRED_TOP = new RgbLigthState(110, 38, 0, true);
    private static final RgbLigthState DEFERRED_BOTTOM = new RgbLigthState(36, 16, 0, true);

    private final AkaiFireDrumSeqExtension driver;
    private final OledDisplay oled;
    private final NoteInput noteInput;
    private final PatternButtons patternButtons;
    private final NoteRepeatHandler noteRepeatHandler;
    private final MusicalScaleLibrary scaleLibrary = MusicalScaleLibrary.getInstance();
    private final Integer[] noteTranslationTable = new Integer[128];
    private final Set<Integer> heldPads = new HashSet<>();
    private final Set<Integer> heldStepPads = new HashSet<>();
    private final Map<Integer, Set<Integer>> clipNotesByStep = new HashMap<>();
    private final OikordBank oikordBank = new OikordBank();
    private final PinnableCursorClip noteStepClip;

    private int scaleIndex = PIANO_HIGHLIGHT_INDEX;
    private int transposeBase = 36;
    private boolean inKey = false;
    private boolean noteStepActive = false;
    private NoteStepSubMode currentStepSubMode = NoteStepSubMode.OIKORD_STEP;
    private int oikordPage = 0;
    private int selectedOikordSlot = 0;
    private double oikordGateRatio = 0.92;

    private enum NoteStepSubMode {
        OIKORD_STEP("Oikord Step", BiColorLightState.GREEN_HALF, BiColorLightState.GREEN_FULL),
        CLIP_STEP_RECORD("Clip Step Record", BiColorLightState.AMBER_HALF, BiColorLightState.AMBER_FULL);

        private final String displayName;
        private final BiColorLightState idleLight;
        private final BiColorLightState activeLight;

        NoteStepSubMode(final String displayName, final BiColorLightState idleLight,
                        final BiColorLightState activeLight) {
            this.displayName = displayName;
            this.idleLight = idleLight;
            this.activeLight = activeLight;
        }

        public String displayName() {
            return displayName;
        }

        public BiColorLightState idleLight() {
            return idleLight;
        }

        public BiColorLightState activeLight() {
            return activeLight;
        }

        public NoteStepSubMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public NoteMode(final AkaiFireDrumSeqExtension driver, final NoteRepeatHandler noteRepeatHandler) {
        super(driver.getLayers(), "NOTE_MODE_LAYER");
        this.driver = driver;
        this.oled = driver.getOled();
        this.noteInput = driver.getNoteInput();
        this.patternButtons = driver.getPatternButtons();
        this.noteRepeatHandler = noteRepeatHandler;

        final CursorTrack cursorTrack = driver.getViewControl().getCursorTrack();
        this.noteStepClip = cursorTrack.createLauncherCursorClip("NOTE_STEP", "NOTE_STEP", STEP_COUNT, 128);
        this.noteStepClip.scrollToKey(0);
        this.noteStepClip.addStepDataObserver(this::handleStepData);

        for (int i = 0; i < noteTranslationTable.length; i++) {
            noteTranslationTable[i] = -1;
        }

        bindPads();
        bindButtons();
        bindEncoders();
    }

    private void bindPads() {
        final RgbButton[] pads = driver.getRgbButtons();
        for (int index = 0; index < pads.length; index++) {
            final int padIndex = index;
            pads[index].bindPressed(this, pressed -> handlePadPress(padIndex, pressed), () -> getPadLight(padIndex));
        }
    }

    private void bindButtons() {
        final BiColorButton stepSeqButton = driver.getButton(NoteAssign.STEP_SEQ);
        stepSeqButton.bindPressed(this, this::handleStepSeqPressed, this::getStepSeqLightState);

        final BiColorButton bankLeftButton = driver.getButton(NoteAssign.BANK_L);
        bankLeftButton.bindPressed(this, pressed -> handleOctaveButton(pressed, -1), this::getBankLeftLightState);

        final BiColorButton bankRightButton = driver.getButton(NoteAssign.BANK_R);
        bankRightButton.bindPressed(this, pressed -> handleOctaveButton(pressed, 1), this::getBankRightLightState);

        final BiColorButton scalePrevButton = driver.getButton(NoteAssign.MUTE_1);
        scalePrevButton.bindPressed(this, pressed -> handleScaleButton(pressed, -1), () -> BiColorLightState.AMBER_HALF);

        final BiColorButton scaleNextButton = driver.getButton(NoteAssign.MUTE_2);
        scaleNextButton.bindPressed(this, pressed -> handleScaleButton(pressed, 1), () -> BiColorLightState.AMBER_HALF);

        final BiColorButton infoButton = driver.getButton(NoteAssign.MUTE_3);
        infoButton.bindPressed(this, pressed -> {
            if (pressed) {
                showContextInfo();
            }
        }, () -> BiColorLightState.OFF);

        final BiColorButton spareButton = driver.getButton(NoteAssign.MUTE_4);
        spareButton.bindPressed(this, pressed -> { }, () -> BiColorLightState.OFF);
    }

    private void bindEncoders() {
        final TouchEncoder[] encoders = driver.getEncoders();
        encoders[0].bindEncoder(this, this::handleEncoder0);
        encoders[0].bindTouched(this, pressed -> showTouchedState(pressed, "Root"));

        encoders[1].bindEncoder(this, this::handleEncoder1);
        encoders[1].bindTouched(this, pressed -> showTouchedState(pressed, "Scale"));

        encoders[2].bindEncoder(this, this::handleEncoder2);
        encoders[2].bindTouched(this, pressed -> showTouchedState(pressed, "Octave"));

        encoders[3].bindEncoder(this, this::handleEncoder3);
        encoders[3].bindTouched(this, pressed -> showTouchedState(pressed, "Layout"));

        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.bindEncoder(this, this::handleMainEncoder);
        mainEncoder.bindTouched(this, this::handleMainEncoderPress);
    }

    private void handleEncoder0(final int inc) {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            browseSelectedOikord(inc);
            return;
        }
        adjustRoot(inc);
    }

    private void handleEncoder1(final int inc) {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            adjustOikordPage(inc);
            return;
        }
        adjustScale(inc);
    }

    private void handleEncoder2(final int inc) {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            adjustOikordGate(inc);
            return;
        }
        adjustOctave(inc);
    }

    private void handleEncoder3(final int inc) {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            if (inc != 0) {
                showCurrentOikord();
            }
            return;
        }
        if (inc != 0) {
            toggleLayout();
        }
    }

    private void handleStepSeqPressed(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (driver.isGlobalShiftHeld()) {
            currentStepSubMode = currentStepSubMode.next();
            noteStepActive = true;
            enterCurrentStepSubMode();
            oled.valueInfo("Step Mode", currentStepSubMode.displayName());
            return;
        }
        if (noteStepActive) {
            returnToLivePlay();
        } else {
            noteStepActive = true;
            enterCurrentStepSubMode();
        }
    }

    private void handlePadPress(final int padIndex, final boolean pressed) {
        if (!noteStepActive) {
            handleLivePadPress(padIndex, pressed);
            return;
        }
        if (currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            handleOikordStepPadPress(padIndex, pressed);
            return;
        }
        handleClipStepRecordPadPress(padIndex, pressed);
    }

    private void handleLivePadPress(final int padIndex, final boolean pressed) {
        if (pressed) {
            heldPads.add(padIndex);
        } else {
            heldPads.remove(padIndex);
        }
    }

    private void handleOikordStepPadPress(final int padIndex, final boolean pressed) {
        if (padIndex < STEP_PAD_OFFSET) {
            if (pressed) {
                selectedOikordSlot = padIndex;
                if (!heldStepPads.isEmpty()) {
                    assignSelectedOikordToHeldSteps();
                } else {
                    showCurrentOikord();
                }
            }
            return;
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
        if (pressed) {
            heldStepPads.add(stepIndex);
            showHeldStepInfo(stepIndex);
        } else {
            heldStepPads.remove(stepIndex);
        }
    }

    private void handleClipStepRecordPadPress(final int padIndex, final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (padIndex < STEP_PAD_OFFSET) {
            oled.valueInfo("Clip Step Rec", "Deferred");
        } else {
            oled.valueInfo("Clip Step Rec", "Step " + (padIndex - STEP_PAD_OFFSET + 1));
        }
    }

    private void handleStepData(final int x, final int y, final int state) {
        if (x < 0 || x >= STEP_COUNT) {
            return;
        }
        final Set<Integer> stepNotes = clipNotesByStep.computeIfAbsent(x, ignored -> new HashSet<>());
        if (state == NoteStep.State.Empty.ordinal()) {
            stepNotes.remove(y);
            if (stepNotes.isEmpty()) {
                clipNotesByStep.remove(x);
            }
            return;
        }
        stepNotes.add(y);
    }

    private void enterCurrentStepSubMode() {
        releaseHeldLiveNotes();
        heldStepPads.clear();
        clearTranslation();
        if (currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            showCurrentOikord();
            return;
        }
        oled.valueInfo("Step Mode", "Clip Step Record");
    }

    private void returnToLivePlay() {
        noteStepActive = false;
        heldStepPads.clear();
        applyLayout();
        showState("Mode");
    }

    private void assignSelectedOikordToHeldSteps() {
        final OikordBank.Slot slot = oikordBank.slot(oikordPage, selectedOikordSlot);
        final int[] notes = slot.render(getScale(), getRootNote());
        for (final int stepIndex : heldStepPads) {
            noteStepClip.clearStepsAtX(0, stepIndex);
            for (final int midiNote : notes) {
                noteStepClip.setStep(stepIndex, midiNote, HELD_NOTE_VELOCITY, STEP_LENGTH * oikordGateRatio);
            }
        }
        oled.valueInfo(slot.family(), slot.name());
    }

    private void browseSelectedOikord(final int amount) {
        if (amount == 0) {
            return;
        }
        selectedOikordSlot = Math.max(0, Math.min(OikordBank.PAGE_SIZE - 1, selectedOikordSlot + amount));
        showCurrentOikord();
    }

    private void adjustOikordPage(final int amount) {
        if (amount == 0) {
            return;
        }
        final int nextPage = Math.max(0, Math.min(OikordBank.PAGE_COUNT - 1, oikordPage + amount));
        if (nextPage == oikordPage) {
            return;
        }
        oikordPage = nextPage;
        showCurrentOikord();
    }

    private void adjustOikordGate(final int amount) {
        if (amount == 0) {
            return;
        }
        oikordGateRatio = Math.max(MIN_GATE_RATIO, Math.min(MAX_GATE_RATIO, oikordGateRatio + amount * 0.05));
        oled.valueInfo("Gate", "%d%%".formatted((int) Math.round(oikordGateRatio * 100)));
    }

    private void handleOctaveButton(final boolean pressed, final int amount) {
        if (!pressed) {
            return;
        }
        adjustOctave(amount);
    }

    private void handleScaleButton(final boolean pressed, final int amount) {
        if (!pressed) {
            return;
        }
        adjustScale(amount);
    }

    private void showTouchedState(final boolean pressed, final String label) {
        if (pressed) {
            showState(label);
        } else {
            oled.clearScreenDelayed();
        }
    }

    private BiColorLightState getBankLeftLightState() {
        return getOctave() > MIN_OCTAVE ? BiColorLightState.HALF : BiColorLightState.OFF;
    }

    private BiColorLightState getBankRightLightState() {
        return getOctave() < MAX_OCTAVE ? BiColorLightState.HALF : BiColorLightState.OFF;
    }

    private BiColorLightState getStepSeqLightState() {
        return noteStepActive ? currentStepSubMode.activeLight() : currentStepSubMode.idleLight();
    }

    private void toggleLayout() {
        applyLayoutChange(() -> {
            inKey = !inKey;
            if (inKey && scaleIndex == PIANO_HIGHLIGHT_INDEX) {
                scaleIndex = 1;
            }
        });
        showState("Layout");
    }

    public void cycleLayout() {
        toggleLayout();
    }

    public BiColorLightState getModeButtonLightState() {
        return inKey ? BiColorLightState.RED_FULL : BiColorLightState.AMBER_FULL;
    }

    private void adjustRoot(final int amount) {
        if (amount == 0) {
            return;
        }
        final int nextRoot = getRootNote() + amount;
        if (nextRoot < 0 || nextRoot > 11) {
            return;
        }
        applyLayoutChange(() -> transposeBase = getOctave() * 12 + nextRoot);
        showState("Root");
    }

    private void adjustScale(final int amount) {
        if (amount == 0) {
            return;
        }
        final int minScale = inKey ? 1 : PIANO_HIGHLIGHT_INDEX;
        final int nextScale = scaleIndex + amount;
        if (nextScale < minScale || nextScale >= scaleLibrary.getMusicalScalesCount()) {
            return;
        }
        applyLayoutChange(() -> scaleIndex = nextScale);
        showState("Scale");
    }

    private void adjustOctave(final int amount) {
        if (amount == 0) {
            return;
        }
        final int nextOctave = Math.max(MIN_OCTAVE, Math.min(MAX_OCTAVE, getOctave() + amount));
        if (nextOctave == getOctave()) {
            return;
        }
        applyLayoutChange(() -> transposeBase = nextOctave * 12 + getRootNote());
        showState("Octave");
    }

    private void adjustTransposeSemitone(final int amount) {
        if (amount == 0) {
            return;
        }
        final int nextBase = transposeBase + amount;
        if (nextBase < MIN_TRANSPOSE || nextBase > MAX_TRANSPOSE) {
            return;
        }
        applyLayoutChange(() -> transposeBase = nextBase);
        showState("Root");
    }

    private void handleMainEncoder(final int inc) {
        if (driver.isPopupBrowserActive()) {
            return;
        }
        final boolean fine = driver.isGlobalShiftHeld();
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (AkaiFireDrumSeqExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            noteRepeatHandler.handleMainEncoder(inc, driver.isGlobalAltHeld());
        } else if (AkaiFireDrumSeqExtension.MAIN_ENCODER_SHUFFLE_ROLE.equals(mainEncoderRole)) {
            driver.adjustGrooveShuffleAmount(inc, fine);
        } else {
            driver.adjustMainCursorParameter(inc, fine);
        }
    }

    private void handleMainEncoderPress(final boolean pressed) {
        if (driver.isPopupBrowserActive()) {
            return;
        }
        if (pressed && driver.isGlobalShiftHeld()) {
            driver.cycleMainEncoderRolePreference();
            return;
        }
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (AkaiFireDrumSeqExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            noteRepeatHandler.handlePressed(pressed);
        } else if (AkaiFireDrumSeqExtension.MAIN_ENCODER_SHUFFLE_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showGrooveShuffleInfo();
            } else {
                driver.toggleGrooveEnabled();
            }
        } else if (pressed) {
            driver.showMainCursorParameterInfo();
        } else {
            driver.resetMainCursorParameter();
        }
    }

    private void applyLayout() {
        final NoteGridLayout layout = createLayout();
        for (int i = 0; i < noteTranslationTable.length; i++) {
            noteTranslationTable[i] = -1;
        }
        for (int padIndex = 0; padIndex < NoteGridLayout.PAD_COUNT; padIndex++) {
            final int translatedNote = layout.noteForPad(padIndex);
            noteTranslationTable[0x36 + padIndex] = translatedNote;
        }
        noteInput.setKeyTranslationTable(noteTranslationTable);
    }

    private void applyLayoutChange(final Runnable stateChange) {
        if (noteStepActive) {
            stateChange.run();
            showContextInfo();
            return;
        }
        final Map<Integer, Integer> oldHeldNotes = collectHeldNotes(createLayout());
        sendHeldNotes(oldHeldNotes, false);
        stateChange.run();
        applyLayout();
        sendHeldNotes(collectHeldNotes(createLayout()), true);
    }

    private Map<Integer, Integer> collectHeldNotes(final NoteGridLayout layout) {
        final Map<Integer, Integer> heldNotes = new HashMap<>();
        for (final int padIndex : heldPads) {
            final int midiNote = layout.noteForPad(padIndex);
            if (midiNote >= 0) {
                heldNotes.merge(midiNote, 1, Integer::sum);
            }
        }
        return heldNotes;
    }

    private void sendHeldNotes(final Map<Integer, Integer> notes, final boolean noteOn) {
        final int status = noteOn ? Midi.NOTE_ON : Midi.NOTE_OFF;
        final int velocity = noteOn ? HELD_NOTE_VELOCITY : 0;
        for (final int midiNote : notes.keySet()) {
            noteInput.sendRawMidiEvent(status, midiNote, velocity);
        }
    }

    private void releaseHeldLiveNotes() {
        sendHeldNotes(collectHeldNotes(createLayout()), false);
        heldPads.clear();
    }

    private RgbLigthState getPadLight(final int padIndex) {
        if (!noteStepActive) {
            return getLivePadLight(padIndex);
        }
        if (currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            return getOikordStepPadLight(padIndex);
        }
        return getClipStepRecordPadLight(padIndex);
    }

    private RgbLigthState getLivePadLight(final int padIndex) {
        final NoteGridLayout layout = createLayout();
        final int midiNote = layout.noteForPad(padIndex);
        final RgbLigthState base;
        if (midiNote < 0) {
            base = RgbLigthState.OFF;
        } else if (!inKey && scaleIndex == PIANO_HIGHLIGHT_INDEX) {
            if (layout.roleForPad(padIndex) == NoteGridLayout.PadRole.ROOT) {
                base = ROOT_COLOR;
            } else if (NoteGridLayout.isBlackKey(midiNote)) {
                base = PIANO_BLACK_KEY_COLOR;
            } else {
                base = PIANO_WHITE_KEY_COLOR;
            }
        } else {
            final NoteGridLayout.PadRole role = layout.roleForPad(padIndex);
            base = switch (role) {
                case ROOT -> ROOT_COLOR;
                case IN_SCALE -> IN_SCALE_COLOR;
                case OUT_OF_SCALE -> OUT_OF_SCALE_COLOR;
                case UNAVAILABLE -> RgbLigthState.OFF;
            };
        }
        return heldPads.contains(padIndex) ? base.getBrightest() : base;
    }

    private RgbLigthState getOikordStepPadLight(final int padIndex) {
        if (padIndex < STEP_PAD_OFFSET) {
            final OikordBank.Slot slot = oikordBank.slot(oikordPage, padIndex);
            final RgbLigthState base = getFamilyColor(slot.family());
            return padIndex == selectedOikordSlot ? base.getBrightest() : base;
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
        final RgbLigthState base = heldStepPads.contains(stepIndex)
                ? HELD_STEP
                : clipNotesByStep.containsKey(stepIndex)
                ? OCCUPIED_STEP
                : (stepIndex / 4) % 2 == 0 ? EMPTY_STEP_A : EMPTY_STEP_B;
        return heldStepPads.contains(stepIndex) ? base.getBrightest() : base;
    }

    private RgbLigthState getClipStepRecordPadLight(final int padIndex) {
        if (padIndex < STEP_PAD_OFFSET) {
            return DEFERRED_TOP;
        }
        final int stepIndex = padIndex - STEP_PAD_OFFSET;
        if (heldStepPads.contains(stepIndex)) {
            return HELD_STEP.getBrightest();
        }
        return clipNotesByStep.containsKey(stepIndex) ? OCCUPIED_STEP : DEFERRED_BOTTOM;
    }

    private RgbLigthState getFamilyColor(final String family) {
        return switch (family) {
            case "Barker" -> new RgbLigthState(120, 70, 0, true);
            case "Plaits" -> new RgbLigthState(0, 90, 110, true);
            case "Sus Motion" -> new RgbLigthState(12, 100, 58, true);
            case "Quartal" -> new RgbLigthState(0, 58, 120, true);
            case "Cluster" -> new RgbLigthState(70, 0, 110, true);
            case "Minor Drift" -> new RgbLigthState(110, 20, 36, true);
            case "Dorian Lift" -> new RgbLigthState(30, 90, 18, true);
            default -> new RgbLigthState(88, 64, 0, true);
        };
    }

    private void showState(final String focus) {
        if ("Scale".equals(focus)) {
            oled.valueInfo("Scale", getScaleDisplayName());
            return;
        }
        if ("Root".equals(focus)) {
            oled.valueInfo("Root", "%s%d".formatted(NoteGridLayout.noteName(getRootNote()), getOctave()));
            return;
        }
        if ("Octave".equals(focus)) {
            oled.valueInfo("Octave", Integer.toString(getOctave()));
            return;
        }
        if ("Layout".equals(focus)) {
            oled.valueInfo("Layout", inKey ? "In Key" : "Chromatic");
            return;
        }
        oled.lineInfo("Root %s%d".formatted(NoteGridLayout.noteName(getRootNote()), getOctave()),
                noteStepActive
                        ? "Step: %s\n%s".formatted(currentStepSubMode.displayName(),
                        currentStepSubMode == NoteStepSubMode.OIKORD_STEP ? currentOikordDisplay() : "Deferred")
                        : "Scale: %s\n%s".formatted(getScaleDisplayName(), inKey ? "In Key" : "Chromatic"));
    }

    private void showContextInfo() {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            showCurrentOikord();
            return;
        }
        if (noteStepActive) {
            oled.valueInfo("Step Mode", currentStepSubMode.displayName());
            return;
        }
        showState("Mode");
    }

    private void showCurrentOikord() {
        final OikordBank.Slot slot = oikordBank.slot(oikordPage, selectedOikordSlot);
        oled.valueInfo(slot.family(), "%s P%d".formatted(slot.name(), oikordPage + 1));
    }

    private String currentOikordDisplay() {
        final OikordBank.Slot slot = oikordBank.slot(oikordPage, selectedOikordSlot);
        return "%s P%d".formatted(slot.shortLabel(), oikordPage + 1);
    }

    private void showHeldStepInfo(final int stepIndex) {
        oled.valueInfo("Step", Integer.toString(stepIndex + 1));
    }

    private MusicalScale getScale() {
        final int effectiveScaleIndex = scaleIndex == PIANO_HIGHLIGHT_INDEX ? 1 : scaleIndex;
        return scaleLibrary.getMusicalScale(effectiveScaleIndex);
    }

    private String getScaleDisplayName() {
        if (scaleIndex == PIANO_HIGHLIGHT_INDEX) {
            return "Piano";
        }
        return switch (getScale().getName()) {
            case "Ionan (Major)" -> "Major";
            case "Aeolian (Minor)" -> "Minor";
            case "Phrygian Dominant" -> "Phryg Dom";
            case "Double Harmonic" -> "Dbl Harm";
            case "Harmonic Minor" -> "Harm Min";
            case "Melodic Minor (ascending)" -> "Mel Min";
            case "Hungarian Minor" -> "Hung Min";
            case "Ukranian Dorian" -> "Ukr Dor";
            case "Super Locrian" -> "Sup Loc";
            case "Half-Whole Diminished" -> "Half-Whole";
            case "Major Pentatonic" -> "Maj Pent";
            case "Minor Pentatonic" -> "Min Pent";
            case "Major Blues" -> "Maj Blues";
            case "Whole Tone" -> "Whole";
            case "Whole Half" -> "WholeHalf";
            case "BeBop Major" -> "Bebop Maj";
            case "BeBop Dorian" -> "Bebop Dor";
            case "BeBop Mixolydian" -> "Bebop Mix";
            case "BeBop Minor" -> "Bebop Min";
            default -> getScale().getName();
        };
    }

    private int getRootNote() {
        return Math.floorMod(transposeBase, 12);
    }

    private int getOctave() {
        return transposeBase / 12;
    }

    private NoteGridLayout createLayout() {
        return new NoteGridLayout(getScale(), getRootNote(), getOctave(), inKey);
    }

    private void clearTranslation() {
        heldPads.clear();
        for (int i = 0; i < noteTranslationTable.length; i++) {
            noteTranslationTable[i] = -1;
        }
        noteInput.setKeyTranslationTable(noteTranslationTable);
    }

    private void handlePatternUp(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            adjustOikordPage(1);
            return;
        }
        adjustTransposeSemitone(1);
    }

    private void handlePatternDown(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            adjustOikordPage(-1);
            return;
        }
        adjustTransposeSemitone(-1);
    }

    private BiColorLightState getPatternButtonLight() {
        if (noteStepActive && currentStepSubMode == NoteStepSubMode.OIKORD_STEP) {
            return BiColorLightState.GREEN_HALF;
        }
        return BiColorLightState.HALF;
    }

    @Override
    protected void onActivate() {
        noteStepActive = false;
        heldStepPads.clear();
        patternButtons.setUpCallback(this::handlePatternUp, this::getPatternButtonLight);
        patternButtons.setDownCallback(this::handlePatternDown, this::getPatternButtonLight);
        applyLayout();
        showState("Mode");
    }

    @Override
    protected void onDeactivate() {
        patternButtons.setUpCallback(pressed -> { }, () -> BiColorLightState.OFF);
        patternButtons.setDownCallback(pressed -> { }, () -> BiColorLightState.OFF);
        noteStepActive = false;
        heldStepPads.clear();
        clearTranslation();
    }
}
