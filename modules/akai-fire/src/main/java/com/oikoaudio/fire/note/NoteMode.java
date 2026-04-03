package com.oikoaudio.fire.note;

import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.MusicalScale;
import com.bitwig.extensions.framework.MusicalScaleLibrary;
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

import java.util.HashSet;
import java.util.Set;

public class NoteMode extends Layer {
    private static final int MIN_OCTAVE = 0;
    private static final int MAX_OCTAVE = 7;
    private static final RgbLigthState ROOT_COLOR = new RgbLigthState(120, 64, 0, true);
    private static final RgbLigthState IN_SCALE_COLOR = new RgbLigthState(0, 72, 110, true);
    private static final RgbLigthState OUT_OF_SCALE_COLOR = RgbLigthState.GRAY_1;

    private final AkaiFireDrumSeqExtension driver;
    private final OledDisplay oled;
    private final NoteInput noteInput;
    private final PatternButtons patternButtons;
    private final NoteRepeatHandler noteRepeatHandler;
    private final MusicalScaleLibrary scaleLibrary = MusicalScaleLibrary.getInstance();
    private final Integer[] noteTranslationTable = new Integer[128];
    private final Set<Integer> heldPads = new HashSet<>();

    private int scaleIndex = 1;
    private int rootNote = 0;
    private int octave = 3;
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
        infoButton.bindPressed(this, this::handleInfoPressed, () -> BiColorLightState.GREEN_HALF);

        final BiColorButton spareButton = driver.getButton(NoteAssign.MUTE_4);
        spareButton.bindPressed(this, pressed -> {
            if (pressed) {
                showState("Note Mode");
            } else {
                oled.clearScreenDelayed();
            }
        }, () -> BiColorLightState.OFF);
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

    private void handleStepSeqPressed(final boolean pressed) {
        if (!pressed) {
            oled.clearScreenDelayed();
            return;
        }
        toggleLayout();
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

    private void handleInfoPressed(final boolean pressed) {
        if (pressed) {
            showState("State");
        } else {
            oled.clearScreenDelayed();
        }
    }

    private void showTouchedState(final boolean pressed, final String label) {
        if (pressed) {
            showState(label);
        } else {
            oled.clearScreenDelayed();
        }
    }

    private BiColorLightState getStepSeqLightState() {
        return inKey ? BiColorLightState.GREEN_FULL : BiColorLightState.AMBER_HALF;
    }

    private BiColorLightState getBankLeftLightState() {
        return octave > MIN_OCTAVE ? BiColorLightState.HALF : BiColorLightState.OFF;
    }

    private BiColorLightState getBankRightLightState() {
        return octave < MAX_OCTAVE ? BiColorLightState.HALF : BiColorLightState.OFF;
    }

    private void toggleLayout() {
        inKey = !inKey;
        applyLayout();
        showState("Layout");
    }

    private void adjustRoot(final int amount) {
        if (amount == 0) {
            return;
        }
        rootNote = Math.floorMod(rootNote + amount, 12);
        applyLayout();
        showState("Root");
    }

    private void adjustScale(final int amount) {
        if (amount == 0) {
            return;
        }
        scaleIndex = Math.floorMod(scaleIndex + amount, scaleLibrary.getMusicalScalesCount());
        if (scaleIndex == 0) {
            scaleIndex = 1;
        }
        applyLayout();
        showState("Scale");
    }

    private void adjustOctave(final int amount) {
        if (amount == 0) {
            return;
        }
        octave = Math.max(MIN_OCTAVE, Math.min(MAX_OCTAVE, octave + amount));
        applyLayout();
        showState("Octave");
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
        final NoteGridLayout layout = new NoteGridLayout(getScale(), rootNote, octave, inKey);
        for (int i = 0; i < noteTranslationTable.length; i++) {
            noteTranslationTable[i] = -1;
        }
        for (int padIndex = 0; padIndex < NoteGridLayout.PAD_COUNT; padIndex++) {
            final int translatedNote = layout.noteForPad(padIndex);
            noteTranslationTable[0x36 + padIndex] = translatedNote;
        }
        noteInput.setKeyTranslationTable(noteTranslationTable);
    }

    private RgbLigthState getPadLight(final int padIndex) {
        final NoteGridLayout layout = new NoteGridLayout(getScale(), rootNote, octave, inKey);
        final NoteGridLayout.PadRole role = layout.roleForPad(padIndex);
        final RgbLigthState base = switch (role) {
            case ROOT -> ROOT_COLOR;
            case IN_SCALE -> IN_SCALE_COLOR;
            case OUT_OF_SCALE -> OUT_OF_SCALE_COLOR;
            case UNAVAILABLE -> RgbLigthState.OFF;
        };
        return heldPads.contains(padIndex) ? base.getBrightest() : base;
    }

    private void showState(final String focus) {
        oled.lineInfo("Note Mode",
                "%s %s\n%s%d\n%s".formatted(
                        focus,
                        inKey ? "In Key" : "Chromatic",
                        NoteGridLayout.noteName(rootNote),
                        octave,
                        getScale().getName()));
    }

    private MusicalScale getScale() {
        return scaleLibrary.getMusicalScale(scaleIndex);
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
                adjustRoot(1);
            }
        }, () -> BiColorLightState.HALF);
        patternButtons.setDownCallback(pressed -> {
            if (pressed) {
                adjustRoot(-1);
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
