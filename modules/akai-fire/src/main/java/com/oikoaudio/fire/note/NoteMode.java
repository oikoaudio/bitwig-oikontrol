package com.oikoaudio.fire.note;

import com.bitwig.extension.controller.api.NoteInput;
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
    private static final RgbLigthState ROOT_COLOR = new RgbLigthState(120, 64, 0, true);
    private static final RgbLigthState IN_SCALE_COLOR = new RgbLigthState(0, 72, 110, true);
    private static final RgbLigthState PIANO_BLACK_KEY_COLOR = new RgbLigthState(0, 56, 120, true);
    private static final RgbLigthState PIANO_WHITE_KEY_COLOR = RgbLigthState.GRAY_2;
    private static final RgbLigthState OUT_OF_SCALE_COLOR = RgbLigthState.GRAY_1;

    private final AkaiFireDrumSeqExtension driver;
    private final OledDisplay oled;
    private final NoteInput noteInput;
    private final PatternButtons patternButtons;
    private final NoteRepeatHandler noteRepeatHandler;
    private final MusicalScaleLibrary scaleLibrary = MusicalScaleLibrary.getInstance();
    private final Integer[] noteTranslationTable = new Integer[128];
    private final Set<Integer> heldPads = new HashSet<>();

    private int scaleIndex = PIANO_HIGHLIGHT_INDEX;
    private int transposeBase = 36;
    private boolean inKey = false;

    public NoteMode(final AkaiFireDrumSeqExtension driver, final NoteRepeatHandler noteRepeatHandler) {
        super(driver.getLayers(), "NOTE_MODE_LAYER");
        this.driver = driver;
        this.oled = driver.getOled();
        this.noteInput = driver.getNoteInput();
        this.patternButtons = driver.getPatternButtons();
        this.noteRepeatHandler = noteRepeatHandler;

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
        stepSeqButton.bindPressed(this, pressed -> { }, () -> BiColorLightState.OFF);

        final BiColorButton bankLeftButton = driver.getButton(NoteAssign.BANK_L);
        bankLeftButton.bindPressed(this, pressed -> handleOctaveButton(pressed, -1), this::getBankLeftLightState);

        final BiColorButton bankRightButton = driver.getButton(NoteAssign.BANK_R);
        bankRightButton.bindPressed(this, pressed -> handleOctaveButton(pressed, 1), this::getBankRightLightState);

        final BiColorButton scalePrevButton = driver.getButton(NoteAssign.MUTE_1);
        scalePrevButton.bindPressed(this, pressed -> handleScaleButton(pressed, -1), () -> BiColorLightState.AMBER_HALF);

        final BiColorButton scaleNextButton = driver.getButton(NoteAssign.MUTE_2);
        scaleNextButton.bindPressed(this, pressed -> handleScaleButton(pressed, 1), () -> BiColorLightState.AMBER_HALF);

        final BiColorButton infoButton = driver.getButton(NoteAssign.MUTE_3);
        infoButton.bindPressed(this, pressed -> { }, () -> BiColorLightState.OFF);

        final BiColorButton spareButton = driver.getButton(NoteAssign.MUTE_4);
        spareButton.bindPressed(this, pressed -> { }, () -> BiColorLightState.OFF);
    }

    private void bindEncoders() {
        final TouchEncoder[] encoders = driver.getEncoders();
        encoders[0].bindEncoder(this, inc -> adjustRoot(inc));
        encoders[0].bindTouched(this, pressed -> showTouchedState(pressed, "Root"));

        encoders[1].bindEncoder(this, inc -> adjustScale(inc));
        encoders[1].bindTouched(this, pressed -> showTouchedState(pressed, "Scale"));

        encoders[2].bindEncoder(this, inc -> adjustOctave(inc));
        encoders[2].bindTouched(this, pressed -> showTouchedState(pressed, "Octave"));

        encoders[3].bindEncoder(this, inc -> {
            if (inc != 0) {
                toggleLayout();
            }
        });
        encoders[3].bindTouched(this, pressed -> showTouchedState(pressed, "Layout"));

        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.bindEncoder(this, this::handleMainEncoder);
        mainEncoder.bindTouched(this, this::handleMainEncoderPress);
    }

    private void handlePadPress(final int padIndex, final boolean pressed) {
        if (pressed) {
            heldPads.add(padIndex);
        } else {
            heldPads.remove(padIndex);
        }
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

    private RgbLigthState getPadLight(final int padIndex) {
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
                "Scale: %s\n%s".formatted(getScaleDisplayName(), inKey ? "In Key" : "Chromatic"));
    }

    private MusicalScale getScale() {
        final int effectiveScaleIndex = scaleIndex == PIANO_HIGHLIGHT_INDEX ? 1 : scaleIndex;
        return scaleLibrary.getMusicalScale(effectiveScaleIndex);
    }

    private String getScaleName() {
        return scaleIndex == PIANO_HIGHLIGHT_INDEX ? "Piano" : getScale().getName();
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

    @Override
    protected void onActivate() {
        patternButtons.setUpCallback(pressed -> {
            if (pressed) {
                adjustTransposeSemitone(1);
            }
        }, () -> BiColorLightState.HALF);
        patternButtons.setDownCallback(pressed -> {
            if (pressed) {
                adjustTransposeSemitone(-1);
            }
        }, () -> BiColorLightState.HALF);
        applyLayout();
        showState("Mode");
    }

    @Override
    protected void onDeactivate() {
        patternButtons.setUpCallback(pressed -> { }, () -> BiColorLightState.OFF);
        patternButtons.setDownCallback(pressed -> { }, () -> BiColorLightState.OFF);
        clearTranslation();
    }
}
