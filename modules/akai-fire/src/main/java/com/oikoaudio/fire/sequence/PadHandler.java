package com.oikoaudio.fire.sequence;

import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.ViewCursorControl;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.display.DisplayInfo;
import com.oikoaudio.fire.display.DisplayTarget;
import com.oikoaudio.fire.display.OledDisplay.TextJustification;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.sequence.NoteAction.Type;
import com.oikoaudio.fire.utils.PatternButtons;
import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.BooleanValueObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PadHandler {
    private static final int DRUM_PAD_BUTTON_OFFSET = 16;
    private static final int PAD_NOTE_BASE = 0x36;

    final DrumSequenceMode parent;
    private final AkaiFireOikontrolExtension driver;
    private final PinnableCursorClip cursorClip;
    private final NoteInput noteInput;
    private final DrumPadBank padBank;

    private final NoteRepeatHandler noteRepeatHandler;

    private final List<PadContainer> pads = new ArrayList<>();
    private final Set<Integer> padsHeld = new HashSet<>();

    RgbLigthState currentPadColor = RgbLigthState.PURPLE;

    PadContainer selectedPad;
    int selectedPadIndex = -1;
    private int drumScrollOffset;

    private final BooleanValueObject[] playing = new BooleanValueObject[16];
    private final BooleanValueObject notePlayingActive = new BooleanValueObject();

    private final boolean[] drumTracker = new boolean[16];
    private final Integer[] notesToDrumTable = new Integer[128];
    private final int[] notesToPadsTable = new int[128];
    private final int[] padNotes = new int[16];
    private final DisplayTarget displayTarget;
    private final DisplayInfo padDisplayInfo;

    public PadHandler(final AkaiFireOikontrolExtension driver, final DrumSequenceMode parent, final Layer mainLayer,
                      final Layer muteLayer, final Layer soloLayer, final NoteRepeatHandler noteRepeatHandler) {
        this.driver = driver;
        this.parent = parent;
        cursorClip = parent.getCursorClip();
        noteInput = driver.getNoteInput();
        for (int i = 0; i < padNotes.length; i++) {
            padNotes[i] = PAD_NOTE_BASE + DRUM_PAD_BUTTON_OFFSET + i;
        }
        final ViewCursorControl control = driver.getViewControl();

        padBank = control.getDrumPadBank();
        padBank.canScrollBackwards().markInterested();
        padBank.canScrollForwards().markInterested();
        padBank.scrollPosition().markInterested();

        setupPlaying(driver.getViewControl());

        displayTarget = new DisplayTarget(parent.getOled());

        final RgbButton[] rgbButtons = driver.getRgbButtons();
        for (int i = 0; i < 16; i++) {
            final RgbButton button = rgbButtons[i + DRUM_PAD_BUTTON_OFFSET];
            final PadContainer pad = new PadContainer(this, i, control.getDrumPadBank().getItemAt(i), playing[i]);

            bindMain(button, mainLayer, pad);
            button.bind(muteLayer, () -> {
                pad.pad.mute().toggle();
                parent.notifyMuteAction();
            });
            button.bindLight(muteLayer, pad::mutingColors);
            button.bind(soloLayer, () -> {
                pad.pad.solo().toggle();
                parent.notifySoloAction();
            });
            button.bindLight(soloLayer, pad::soloingColors);
        }

        this.noteRepeatHandler = noteRepeatHandler;
        noteRepeatHandler.getNoteRepeatActive().addValueObserver(this::handleNoteRepeatChanged);

        notePlayingActive.addValueObserver(active -> {
            if (active) {
                applyScale();
            } else if (!notePlayingEnabled()) {
                disableNotePlaying();
            }
        });
        initButtons(mainLayer, driver);
        padDisplayInfo = new DisplayInfo() //
                .addLine("Selected Pad", 1, 0, TextJustification.CENTER) //
                .addLine(() -> selectedPad != null ? selectedPad.getName() : "", 2, 3, TextJustification.CENTER) //
                .create();
    }

    private void bindMain(final RgbButton button, final Layer mainLayer, final PadContainer pad) {
        pads.add(pad);
        button.bindPressed(mainLayer, p -> handlePadSelection(pad, p), pad::getColor);
    }

    private void initButtons(final Layer mainLayer, final AkaiFireOikontrolExtension driver) {
    }

    private void handlePadSelection(final PadContainer pad, final boolean pressed) {
        if (!pressed) {
            padsHeld.remove(pad.index);
        } else {
            if (parent.isCopyHeld()) {
                doNotesPadCopy(pad);
            } else if (parent.isShiftHeld()) {
                pad.pad.color().set(getPadColor(pad.pad));
            } else if (parent.isDeleteHeld()) {
                if (pad.index == selectedPadIndex) {
                    cursorClip.clearStepsAtY(0, 0);
                } else {
                    parent.registerPendingAction(new NoteAction(selectedPadIndex, pad.index, Type.CLEAR));
                    pad.pad.selectInEditor();
                }
            } else {
                pad.pad.selectInEditor();
                if (driver.isAuditionOnDrumSelectEnabled()) {
                    auditionPad(pad.index, 100);
                }
                padsHeld.add(pad.index);
            }
        }
    }

    private void auditionPad(final int padIndex, final int velocity) {
        final int midiNote = drumScrollOffset + padIndex;
        if (midiNote < 0 || midiNote > 127) {
            return;
        }
        final int appliedVelocity = velocity > 0 ? 100 : 0;
        noteInput.sendRawMidiEvent(0x90, midiNote, appliedVelocity);
        noteInput.sendRawMidiEvent(0x80, midiNote, 0);
    }

    private Color getPadColor(DrumPad pad) {
        Color[] colors = {
            Color.fromHex("#d92e24"), // red
            Color.fromHex("#ff5706"), // orange
            Color.fromHex("#44c8ff"), // dark blue
            Color.fromHex("#0099d9"), // light blue
            Color.fromHex("#009d47"), // dark green
            Color.fromHex("#3ebb62"), // light green
            Color.fromHex("#d99d10"), // yellow
            Color.fromHex("#c9c9c9"), // white
            Color.fromHex("#5761c6"), // dark purple
            Color.fromHex("#bc76f0"), // light purple
        };
        Color currentColor = pad.color().get();

        int colorIndex = 0;
        for (int i = 0; i < colors.length; i++) {
            if (colors[i].toHex().equals(currentColor.toHex())) {
                colorIndex = i + 1;
            }
        }
        if (colorIndex == colors.length) {
            colorIndex = 0;
        }
        return colors[colorIndex];
    }

    void executeCopy(final List<NoteStep> notes, final boolean copyParams) {
        cursorClip.clearStepsAtY(0, 0);
        for (final NoteStep noteStep : notes) {
            // TODO: this is an API bug
            cursorClip.setStep(noteStep.x(), 0, 100, 0.25);
//            cursorClip.setStep(noteStep.x(), 0, (int) (noteStep.velocity() * 127), noteStep.duration());
            if (copyParams) {
                parent.registerExpectedNoteChange(noteStep.x(), noteStep);
            }
        }
    }

    void executeClear(final int origIndex) {
        cursorClip.clearStepsAtY(0, 0);
        if (origIndex != -1) {
            pads.get(origIndex).pad.selectInEditor();
        }
    }

    /**
     * The Pad has to be another pad then the currently selected pad. Copies notes
     * to that destination.
     *
     * @param pad destination pad of copy.
     */
    private void doNotesPadCopy(final PadContainer pad) {
        if (pad.index != selectedPadIndex) {
            final List<NoteStep> notes = parent.getOnNotes();
            parent.registerPendingAction(new NoteAction(selectedPadIndex, pad.index, Type.COPY_PAD, notes));
            cursorClip.scrollToKey(drumScrollOffset + pad.index);
            pad.pad.selectInEditor();
        }
    }

    public void executePadSelection(final PadContainer pad) {
        currentPadColor = pad.getBitwigPadColor();
        selectedPad = pad;
        focusOnSelectedPad();
        selectedPadIndex = pad.getIndex();

        displayTarget.setFocusIndex(selectedPadIndex);
        displayTarget.setName(selectedPad.getName());

        parent.getOled().showInfo(padDisplayInfo);

        selectedPad.updateDisplay(displayTarget.getTypeIndex());
        final NoteAction pendingAction = parent.getPendingAction();
        if (pendingAction != null && pendingAction.getDestPadIndex() == selectedPadIndex) {
            if (pendingAction.getType() == Type.CLEAR) {
                executeClear(pendingAction.getSrcPadIndex());
            } else if (pendingAction.getType() == Type.COPY_PAD) {
                executeCopy(pendingAction.getCopyNotes(), !parent.isShiftHeld());
            }
            parent.clearPendingAction();
        }
    }

    public void focusOnSelectedPad() {
        final int padIndex = selectedPad != null ? selectedPad.index : 0;
        cursorClip.scrollToKey(drumScrollOffset + padIndex);
    }

    public String getPadInfo() {
        if (selectedPad != null) {
            return selectedPad.getName();
        }
        return "";
    }

    public boolean isPadBeingHeld() {
        return !padsHeld.isEmpty();
    }

    public RgbLigthState getCurrentPadColor() {
        return currentPadColor;
    }

    public int getSelectedPadIndex() {
        return selectedPadIndex;
    }

    /**
     * Returns the MIDI key for the currently selected pad, or -1 if none.
     */
    public int getSelectedPadMidi() {
        if (selectedPadIndex < 0) {
            return -1;
        }
        return drumScrollOffset + selectedPadIndex;
    }

    private void setupPlaying(final ViewCursorControl control) {
        final DrumPadBank drumPadBank = control.getDrumPadBank();
        final CursorTrack cursorTrack = control.getCursorTrack();
        for (int i = 0; i < notesToDrumTable.length; i++) {
            notesToDrumTable[i] = -1;
            notesToPadsTable[i] = -1;
        }
        noteInput.setKeyTranslationTable(notesToDrumTable);
        drumPadBank.scrollPosition().addValueObserver(offset -> {
            drumScrollOffset = offset;
            focusOnSelectedPad();
            applyScale();
        });
        for (int i = 0; i < 16; i++) {
            playing[i] = new BooleanValueObject();
            playing[i].set(false);
        }
        cursorTrack.playingNotes().addValueObserver(this::handleNotes);
    }

    private void handleNotes(final PlayingNote[] notes) {
        if (!parent.isActive()) {
            return;
        }
        for (int i = 0; i < 16; i++) {
            drumTracker[i] = false;
        }
        for (final PlayingNote playingNote : notes) {
            final int padIndex = notesToPadsTable[playingNote.pitch()];
            if (padIndex != -1) {
                playing[padIndex].set(true);
                drumTracker[padIndex] = true;
            }
        }
        for (int i = 0; i < 16; i++) {
            if (!drumTracker[i]) {
                playing[i].set(false);
            }
        }
    }

    boolean notePlayingEnabled() {
        return notePlayingActive.get() || noteRepeatHandler.getNoteRepeatActive().get();
    }

    private void handleNoteRepeatChanged(final boolean nrActive) {
        if (nrActive) {
            noteRepeatHandler.activate();
            applyScale();
        } else {
            noteRepeatHandler.deactivate();
            if (!notePlayingEnabled()) {
                disableNotePlaying();
            }
        }
    }

    void disableNotePlaying() {
        if (!parent.isActive()) {
            return;
        }
        padsHeld.clear();
        for (int i = 0; i < 128; i++) {
            notesToDrumTable[i] = -1;
        }
        noteInput.setKeyTranslationTable(notesToDrumTable);
    }

    void applyScale() {
        if (!parent.isActive()) {
            return;
        }
        for (int i = 0; i < 128; i++) {
            notesToPadsTable[i] = -1;
        }
        for (int i = 0; i < 16; i++) {
            final int padnote = padNotes[i];
            final int noteToPadIndex = drumScrollOffset + i;
            if (noteToPadIndex < 128) {
                notesToDrumTable[padnote] = noteToPadIndex;
                notesToPadsTable[noteToPadIndex] = i;
            }
        }
        if (notePlayingEnabled()) {
            noteInput.setKeyTranslationTable(notesToDrumTable);
        }
    }

    public void handleMainEncoder(final int inc) {
        noteRepeatHandler.handleMainEncoder(inc, parent.isAltHeld());
    }

    private BiColorLightState canScrollUp(final BiColorButton button) {
        if (padBank.scrollPosition().get() + (parent.isShiftHeld() ? 16 : 4) < 128) {
            if (button.isPressed()) {
                return BiColorLightState.FULL;
            }
            return BiColorLightState.HALF;
        }
        return BiColorLightState.OFF;
    }

    private BiColorLightState canScrollDown(final BiColorButton button) {
        if (padBank.scrollPosition().get() - (parent.isShiftHeld() ? 16 : 4) >= 0) {
            if (button.isPressed()) {
                return BiColorLightState.FULL;
            }
            return BiColorLightState.HALF;
        }
        return BiColorLightState.OFF;
    }

    void scrollForward(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (parent.isShiftHeld()) {
            padBank.scrollBy(4);
        } else {
            padBank.scrollBy(16);
        }
    }

    void scrollBackward(final boolean pressed) {
        if (!pressed) {
            return;
        }
        if (parent.isShiftHeld()) {

            padBank.scrollBy(-4);
        } else {
            padBank.scrollBy(-16);
        }
    }

    public DisplayTarget getDisplayTarget() {
        return displayTarget;
    }

    public void activateView(final int typeIndex, final String paramName) {
        // Try to fetch the real parameter name from the active remote controls page.
        CursorRemoteControlsPage remotePage = parent.getActiveRemoteControlsPage();
        String realParamName = paramName; // fallback value

        // For macro parameters, our typeIndex starts at 10.
        int offset = 10;
        int remoteParamIndex = typeIndex - offset;

        // Make sure the computed index is within the valid range.
        if (remotePage != null && remoteParamIndex >= 0 && remoteParamIndex < remotePage.getParameterCount()) {
            Parameter parameter = remotePage.getParameter(remoteParamIndex);
            if (parameter != null) {
                // Mark the parameter name as interested so we can read its current value.
                realParamName = parameter.name().get();
            }
        }

        displayTarget.setTypeIndex(typeIndex, realParamName);
        displayTarget.activate();
    }



    public void deactivateView() {
        displayTarget.deactivate();
    }

    public void modifyValue(final int typeIndex, final int inc) {
        if (selectedPad == null) {
            return;
        }
        selectedPad.modifyValue(typeIndex, inc, parent.isShiftHeld(), parent.isAltHeld());
    }

    public void bindPadParameters(final Layer layer) {
        for (final PadContainer pad : pads) {
            pad.bindParameters(layer);
        }
    }

    public void bindPadMacros(final Layer layer) {
        for (final PadContainer pad : pads) {
            pad.bindMacros(layer);
        }
    }

    public void bindPadMacrosShift(final Layer layer) {
        for (final PadContainer pad : pads) {
            pad.bindMacrosShift(layer);
        }
    }

    public void updateDisplay(final int index) {
        if (selectedPad != null) {
            selectedPad.updateDisplay(index);
        }

    }


    public NoteRepeatHandler getNoteRepeaterHandler() {
        return noteRepeatHandler;
    }

    private void handlePatternUpPressed(final boolean pressed) {
        if (pressed && parent.isAltHeld()) {
            scrollForward(true);
        }
    }

    private void handlePatternDownPressed(final boolean pressed) {
        if (pressed && parent.isAltHeld()) {
            scrollBackward(true);
        }
    }
}
