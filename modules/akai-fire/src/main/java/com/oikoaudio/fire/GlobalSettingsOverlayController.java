package com.oikoaudio.fire;

import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.oikoaudio.fire.TopLevelModeState.Mode;
import com.oikoaudio.fire.control.EncoderStepAccumulator;
import com.oikoaudio.fire.control.ParameterEncoderBinding;
import com.oikoaudio.fire.control.RgbButton;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.control.VelocitySettings;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLightState;
import com.oikoaudio.fire.music.SharedPitchContextController;
import com.oikoaudio.fire.note.NoteGridLayout;
import com.oikoaudio.fire.sequence.EncoderMode;

/** Owns Global Settings Overlay state, bindings, interaction, and presentation. */
final class GlobalSettingsOverlayController {
    private static final int PAD_COLUMNS = 16;
    private static final int PAD_ROWS = 4;
    private static final int SHOW_DEACTIVATED_TRACKS_PAD = 63;
    private static final int ROOT_ENCODER_THRESHOLD = 16;
    private static final int SCALE_ENCODER_THRESHOLD = 8;
    private static final int OCTAVE_ENCODER_THRESHOLD = 8;
    private static final int VELOCITY_CENTER_DEFAULT = 100;
    private static final RgbLightState LOGO_ON = new RgbLightState(127, 20, 0, true);
    private static final RgbLightState LOGO_OFF = RgbLightState.OFF;
    private static final RgbLightState TOGGLE_ON = new RgbLightState(0, 96, 96, true);
    private static final RgbLightState TOGGLE_OFF = new RgbLightState(0, 32, 32, true);
    private static final boolean[][] LOGO = {
            {true, true, true, false, true, true, true, false, true, true, true, false, true, true, true, true},
            {true, false, false, false, false, true, false, false, true, false, true, false, true, false, false, false},
            {true, true, false, false, false, true, false, false, true, true, false, false, true, true, true, false},
            {true, false, false, false, true, true, true, false, true, false, true, false, true, true, true, true}
    };

    private final State state = new State();
    private final Host host;
    private final Layer layer;
    private final OledDisplay oled;
    private final SharedPitchContextController pitchContext;
    private final VelocitySettings velocitySettings;
    private final FirePreferences preferences;
    private final ViewCursorControl viewControl;
    private final EncoderStepAccumulator[] accumulators = {
            new EncoderStepAccumulator(ROOT_ENCODER_THRESHOLD),
            new EncoderStepAccumulator(SCALE_ENCODER_THRESHOLD),
            new EncoderStepAccumulator(OCTAVE_ENCODER_THRESHOLD),
            new EncoderStepAccumulator(SCALE_ENCODER_THRESHOLD)
    };

    GlobalSettingsOverlayController(final Layers layers,
                                    final TouchEncoder[] encoders,
                                    final RgbButton[] pads,
                                    final com.oikoaudio.fire.control.BiColorButton knobModeButton,
                                    final OledDisplay oled,
                                    final SharedPitchContextController pitchContext,
                                    final VelocitySettings velocitySettings,
                                    final FirePreferences preferences,
                                    final ViewCursorControl viewControl,
                                    final Host host) {
        this.host = host;
        this.oled = oled;
        this.pitchContext = pitchContext;
        this.velocitySettings = velocitySettings;
        this.preferences = preferences;
        this.viewControl = viewControl;
        layer = new Layer(layers, "GlobalSettings");
        bind(encoders, pads, knobModeButton);
    }

    private void bind(final TouchEncoder[] encoders,
                      final RgbButton[] pads,
                      final com.oikoaudio.fire.control.BiColorButton knobModeButton) {
        for (int index = 0; index < encoders.length; index++) {
            final int encoderIndex = index;
            encoders[index].bindEncoder(layer, inc -> {
                final int steps = accumulators[encoderIndex].consume(inc);
                if (steps != 0) {
                    adjust(encoderIndex, steps);
                }
            });
            encoders[index].bindTouched(layer, touched -> handleTouch(encoderIndex, touched));
        }
        for (int padIndex = 0; padIndex < pads.length; padIndex++) {
            final int currentPad = padIndex;
            pads[padIndex].bindPressed(layer,
                    pressed -> handlePad(currentPad, pressed),
                    () -> padState(currentPad));
        }
        knobModeButton.bindPressed(layer, this::handlePageAdvance, this::modeLightState);
    }

    boolean isActive() {
        return state.isActive();
    }

    boolean isLatched() {
        return state.isLatched();
    }

    boolean toggleLatch() {
        return state.toggleLatch(true);
    }

    void closeLatch() {
        state.closeLatch();
    }

    void refreshState() {
        final boolean momentaryComboHeld = host.browserButtonPressed()
                && host.shiftHeld()
                && !host.altHeld()
                && !host.popupBrowserActive();
        if (state.shouldBeActive(momentaryComboHeld, host.popupBrowserActive())) {
            activate();
        } else {
            deactivate();
        }
    }

    void activate() {
        if (state.isActive()) {
            showOverview();
            return;
        }
        state.setActive(true);
        host.prepareActivation();
        layer.activate();
        showOverview();
        host.refreshSurfaceLights();
    }

    void deactivate() {
        if (!state.isActive()) {
            return;
        }
        state.setActive(false);
        layer.deactivate();
        host.restoreActiveMode();
        oled.clearScreenDelayed();
        host.refreshSurfaceLights();
    }

    boolean dismissForModeButton(final Mode targetMode) {
        if (!canDismissForPlainModeButton()) {
            return false;
        }
        state.closeLatch();
        final boolean alreadyInTargetMode = host.activeMode() == targetMode;
        deactivate();
        return alreadyInTargetMode;
    }

    boolean dismissForStepButton() {
        if (!canDismissForPlainModeButton()) {
            return false;
        }
        state.closeLatch();
        final boolean alreadyInStepFamily = switch (host.activeMode()) {
            case CHORD_STEP, MELODIC_STEP, FUGUE_STEP -> true;
            default -> false;
        };
        deactivate();
        return alreadyInStepFamily;
    }

    private boolean canDismissForPlainModeButton() {
        return state.isActive() && !host.shiftHeld() && !host.altHeld();
    }

    private void showOverview() {
        if (state.page() == EncoderMode.USER_1) {
            oled.detailInfo("Global Settings",
                    "Page: %s\n1: Pin Track %s\n2: Pin Device %s\n3: Pin Clip %s\n4: --".formatted(
                            pageLabel(),
                            pinStateLabel(viewControl.getCursorTrack().isPinned().get()),
                            pinOverviewLabel(viewControl.getSelectedDevice().isPinned().get(),
                                    viewControl.getSelectedDevice().exists().get()),
                            pinOverviewLabel(viewControl.getSelectedClip().isPinned().get(),
                                    viewControl.getSelectedClip().exists().get())));
            return;
        }
        if (state.page() == EncoderMode.USER_2) {
            oled.detailInfo("Global Settings",
                    "Page: %s\n1: Create %s\n2: Record %s\n3: --\n4: --".formatted(
                            pageLabel(), preferences.defaultClipLength(), preferences.launcherRecordLength()));
            return;
        }
        if (state.page() == EncoderMode.MIXER) {
            oled.detailInfo("Global Settings",
                    "Page: %s\n1: Vel Sens %d%%\n2: Vel Ctr %d\n3: Pad Bright %s\n4: Pad Sat %s".formatted(
                            pageLabel(), velocitySettings.sensitivity(), velocitySettings.centerVelocity(),
                            brightnessLabel(), saturationLabel()));
            return;
        }
        oled.detailInfo("Global Settings",
                "Page: %s\n1: Root %s\n2: Scale %s\n3: Oct %d\n4: Note OLED %s\nTracks: %s".formatted(
                        pageLabel(), NoteGridLayout.noteName(pitchContext.getRootNote()),
                        pitchContext.getScaleDisplayName(), pitchContext.getOctave(), preferences.noteChordDisplay(),
                        preferences.showDeactivatedTracks() ? "All" : "Active"));
    }

    private void adjust(final int encoderIndex, final int inc) {
        if (inc == 0) {
            return;
        }
        if (state.page() == EncoderMode.MIXER) {
            adjustInput(encoderIndex, inc);
            return;
        }
        if (state.page() == EncoderMode.USER_1) {
            adjustPins(encoderIndex, inc);
            return;
        }
        if (state.page() == EncoderMode.USER_2) {
            adjustClip(encoderIndex, inc);
            return;
        }
        switch (encoderIndex) {
            case 0 -> {
                pitchContext.setRootNote(Math.max(0, Math.min(11, pitchContext.getRootNote() + inc)));
                oled.valueInfo("Root", NoteGridLayout.noteName(pitchContext.getRootNote()));
            }
            case 1 -> {
                pitchContext.adjustScaleIndex(inc, -1);
                oled.valueInfo("Scale", pitchContext.getScaleDisplayName());
            }
            case 2 -> {
                pitchContext.adjustOctave(inc);
                oled.valueInfo("Octave", Integer.toString(pitchContext.getOctave()));
            }
            case 3 -> adjustNoteChordDisplay(inc);
            default -> showOverview();
        }
    }

    private void handleTouch(final int encoderIndex, final boolean touched) {
        if (!touched) {
            accumulators[encoderIndex].reset();
            showOverview();
            return;
        }
        if (handleResetTouch(encoderIndex)) {
            return;
        }
        if (state.page() == EncoderMode.MIXER) {
            showInput(encoderIndex);
        } else if (state.page() == EncoderMode.USER_1) {
            showPin(encoderIndex);
        } else if (state.page() == EncoderMode.USER_2) {
            showClip(encoderIndex);
        } else {
            showPitch(encoderIndex);
        }
    }

    private void showPitch(final int encoderIndex) {
        switch (encoderIndex) {
            case 0 -> oled.valueInfo("Root", NoteGridLayout.noteName(pitchContext.getRootNote()));
            case 1 -> oled.valueInfo("Scale", pitchContext.getScaleDisplayName());
            case 2 -> oled.valueInfo("Octave", Integer.toString(pitchContext.getOctave()));
            case 3 -> oled.valueInfo("Note OLED", preferences.noteChordDisplay());
            default -> showOverview();
        }
    }

    private boolean handleResetTouch(final int encoderIndex) {
        return switch (state.page()) {
            case MIXER -> handleInputReset(encoderIndex);
            case USER_1 -> handlePinReset(encoderIndex);
            case USER_2 -> handleClipReset(encoderIndex);
            default -> handlePitchReset(encoderIndex);
        };
    }

    private boolean handlePitchReset(final int encoderIndex) {
        return switch (encoderIndex) {
            case 0 -> reset(true, "Root", "No reset",
                    () -> pitchContext.setRootNote(preferences.defaultRootKey()),
                    () -> oled.valueInfo("Root", NoteGridLayout.noteName(pitchContext.getRootNote())));
            case 1 -> reset(true, "Scale", "No reset",
                    () -> pitchContext.setScaleIndex(pitchContext.resolveDefaultScaleIndex(preferences.defaultScale())),
                    () -> oled.valueInfo("Scale", pitchContext.getScaleDisplayName()));
            case 2 -> reset(true, "Octave", "No reset",
                    () -> pitchContext.setOctave(preferences.defaultNoteInputOctave()),
                    () -> oled.valueInfo("Octave", Integer.toString(pitchContext.getOctave())));
            case 3 -> reset(true, "Note OLED", "No reset",
                    () -> preferences.setNoteChordDisplay(FireControlPreferences.NOTE_CHORD_DISPLAY_PADS),
                    () -> oled.valueInfo("Note OLED", FireControlPreferences.NOTE_CHORD_DISPLAY_PADS));
            default -> false;
        };
    }

    private boolean handleInputReset(final int encoderIndex) {
        return switch (encoderIndex) {
            case 0 -> reset(true, "Velocity Sens", "No reset",
                    () -> velocitySettings.setSensitivity(preferences.defaultVelocitySensitivity()),
                    () -> oled.valueInfo("Velocity Sens", velocitySettings.sensitivity() + "%"));
            case 1 -> reset(true, "Velocity Ctr", "No reset",
                    () -> velocitySettings.setCenterVelocity(VELOCITY_CENTER_DEFAULT),
                    () -> oled.valueInfo("Velocity Ctr", Integer.toString(velocitySettings.centerVelocity())));
            case 2 -> reset(true, "Pad Bright", "No reset",
                    () -> preferences.setPadBrightness(FireControlPreferences.PAD_BRIGHTNESS_DEFAULT),
                    () -> oled.valueInfo("Pad Bright", brightnessLabel(FireControlPreferences.PAD_BRIGHTNESS_DEFAULT)));
            case 3 -> reset(true, "Pad Sat", "No reset",
                    () -> preferences.setPadSaturation(FireControlPreferences.PAD_SATURATION_DEFAULT),
                    () -> oled.valueInfo("Pad Sat", saturationLabel(FireControlPreferences.PAD_SATURATION_DEFAULT)));
            default -> false;
        };
    }

    private boolean handleClipReset(final int encoderIndex) {
        return switch (encoderIndex) {
            case 0 -> reset(true, "Create Len", "No reset",
                    () -> preferences.setDefaultClipLength(FireControlPreferences.CLIP_LENGTH_2_BARS),
                    () -> oled.valueInfo("Create Len", FireControlPreferences.CLIP_LENGTH_2_BARS));
            case 1 -> reset(true, "Record Len", "No reset",
                    () -> preferences.setLauncherRecordLength(
                            FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_2_BARS),
                    () -> oled.valueInfo("Record Len",
                            FireControlPreferences.LAUNCHER_RECORD_LENGTH_FIXED_2_BARS));
            default -> reset(false, "Clip", "No reset", () -> { }, this::showOverview);
        };
    }

    private boolean handlePinReset(final int encoderIndex) {
        return switch (encoderIndex) {
            case 0 -> reset(true, "Pin Track", "No reset",
                    () -> viewControl.getCursorTrack().isPinned().set(false),
                    () -> oled.valueInfo("Pin Track",
                            pinStateLabel(viewControl.getCursorTrack().isPinned().get())));
            case 1 -> reset(viewControl.getSelectedDevice().exists().get(), "Pin Device", "No reset",
                    () -> viewControl.getSelectedDevice().isPinned().set(false),
                    () -> showPinInfo("Pin Device", viewControl.getSelectedDevice().isPinned().get(),
                            viewControl.getSelectedDevice().exists().get()));
            case 2 -> reset(viewControl.getSelectedClip().exists().get(), "Pin Clip", "No reset",
                    () -> viewControl.getSelectedClip().isPinned().set(false),
                    () -> showPinInfo("Pin Clip", viewControl.getSelectedClip().isPinned().get(),
                            viewControl.getSelectedClip().exists().get()));
            default -> reset(false, "Pins", "No reset", () -> { }, this::showOverview);
        };
    }

    private boolean reset(final boolean resettable,
                          final String label,
                          final String unavailable,
                          final Runnable resetAction,
                          final Runnable showAction) {
        return ParameterEncoderBinding.handleExplicitResetTouch(true, host.explicitResetControl(), resettable,
                label, unavailable, resetAction, showAction, oled::valueInfo);
    }

    private void handlePad(final int padIndex, final boolean pressed) {
        if (!pressed || padIndex != SHOW_DEACTIVATED_TRACKS_PAD) {
            return;
        }
        preferences.setShowDeactivatedTracks(!preferences.showDeactivatedTracks());
        oled.valueInfo("Tracks", preferences.showDeactivatedTracks() ? "All" : "Active");
        host.refreshSurfaceLights();
    }

    private void handlePageAdvance(final boolean pressed) {
        if (pressed) {
            return;
        }
        if (host.consumeKnobModeGesture()) {
            oled.clearScreenDelayed();
            return;
        }
        state.advancePage();
        for (final EncoderStepAccumulator accumulator : accumulators) {
            accumulator.reset();
        }
        showOverview();
        host.refreshSurfaceLights();
    }

    private BiColorLightState modeLightState() {
        return state.page().getState();
    }

    private String pageLabel() {
        return switch (state.page()) {
            case MIXER -> "Input";
            case USER_2 -> "Clip";
            case USER_1 -> "Pins";
            default -> "Pitch";
        };
    }

    private void adjustInput(final int encoderIndex, final int inc) {
        switch (encoderIndex) {
            case 0 -> {
                if (velocitySettings.adjustSensitivity(inc)) {
                    oled.paramInfo("Velocity Sens", velocitySettings.sensitivity(), "Global Input", 0, 100);
                }
            }
            case 1 -> {
                if (velocitySettings.adjustCenterVelocity(inc)) {
                    oled.paramInfo("Velocity Center", velocitySettings.centerVelocity(), "Global Input",
                            velocitySettings.minCenterVelocity(), velocitySettings.maxCenterVelocity());
                }
            }
            case 2 -> adjustBrightness(inc);
            case 3 -> adjustSaturation(inc);
            default -> showOverview();
        }
    }

    private void adjustPins(final int encoderIndex, final int inc) {
        switch (encoderIndex) {
            case 0 -> applyPinEncoder("Pin Track", viewControl.getCursorTrack().isPinned(), true, inc);
            case 1 -> applyPinEncoder("Pin Device", viewControl.getSelectedDevice().isPinned(),
                    viewControl.getSelectedDevice().exists().get(), inc);
            case 2 -> applyPinEncoder("Pin Clip", viewControl.getSelectedClip().isPinned(),
                    viewControl.getSelectedClip().exists().get(), inc);
            default -> showOverview();
        }
    }

    private void showPin(final int encoderIndex) {
        switch (encoderIndex) {
            case 0 -> oled.valueInfo("Pin Track", pinStateLabel(viewControl.getCursorTrack().isPinned().get()));
            case 1 -> showPinInfo("Pin Device", viewControl.getSelectedDevice().isPinned().get(),
                    viewControl.getSelectedDevice().exists().get());
            case 2 -> showPinInfo("Pin Clip", viewControl.getSelectedClip().isPinned().get(),
                    viewControl.getSelectedClip().exists().get());
            default -> showOverview();
        }
    }

    private void applyPinEncoder(final String label,
                                 final SettableBooleanValue pinValue,
                                 final boolean targetExists,
                                 final int inc) {
        if (!targetExists) {
            oled.valueInfo(label, "No Target");
            return;
        }
        final boolean targetPinned = inc > 0;
        pinValue.set(targetPinned);
        oled.valueInfo(label, pinStateLabel(targetPinned));
    }

    private void showPinInfo(final String label, final boolean pinned, final boolean targetExists) {
        oled.valueInfo(label, targetExists ? pinStateLabel(pinned) : "No Target");
    }

    private String pinOverviewLabel(final boolean pinned, final boolean targetExists) {
        return targetExists ? pinStateLabel(pinned) : "--";
    }

    private String pinStateLabel(final boolean pinned) {
        return pinned ? "On" : "Off";
    }

    private void showInput(final int encoderIndex) {
        switch (encoderIndex) {
            case 0 -> oled.valueInfo("Velocity Sens", velocitySettings.sensitivity() + "%");
            case 1 -> oled.valueInfo("Velocity Center", Integer.toString(velocitySettings.centerVelocity()));
            case 2 -> oled.valueInfo("Pad Bright", brightnessLabel());
            case 3 -> oled.valueInfo("Pad Sat", saturationLabel());
            default -> showOverview();
        }
    }

    private void adjustClip(final int encoderIndex, final int inc) {
        switch (encoderIndex) {
            case 0 -> adjustDefaultClipLength(inc);
            case 1 -> adjustLauncherRecordLength(inc);
            default -> showOverview();
        }
    }

    private void showClip(final int encoderIndex) {
        switch (encoderIndex) {
            case 0 -> oled.valueInfo("Create Len", preferences.defaultClipLength());
            case 1 -> oled.valueInfo("Record Len", preferences.launcherRecordLength());
            default -> showOverview();
        }
    }

    private void adjustBrightness(final int inc) {
        final double next = FireControlPreferences.normalizePadBrightness(
                preferences.padBrightness() + inc * FireControlPreferences.PAD_BRIGHTNESS_STEP);
        preferences.setPadBrightness(next);
        oled.valueInfo("Pad Bright", brightnessLabel(next));
    }

    private void adjustSaturation(final int inc) {
        final double next = FireControlPreferences.normalizePadSaturation(
                preferences.padSaturation() + inc * FireControlPreferences.PAD_SATURATION_STEP);
        preferences.setPadSaturation(next);
        oled.valueInfo("Pad Sat", saturationLabel(next));
    }

    private void adjustDefaultClipLength(final int inc) {
        final int nextIndex = steppedIndex(FireControlPreferences.DEFAULT_CLIP_LENGTHS,
                preferences.defaultClipLength(), inc);
        preferences.setDefaultClipLength(FireControlPreferences.DEFAULT_CLIP_LENGTHS[nextIndex]);
        oled.valueInfo("Create Len", FireControlPreferences.DEFAULT_CLIP_LENGTHS[nextIndex]);
    }

    private void adjustLauncherRecordLength(final int inc) {
        final int nextIndex = steppedIndex(FireControlPreferences.LAUNCHER_RECORD_LENGTHS,
                preferences.launcherRecordLength(), inc);
        preferences.setLauncherRecordLength(FireControlPreferences.LAUNCHER_RECORD_LENGTHS[nextIndex]);
        oled.valueInfo("Record Len", FireControlPreferences.LAUNCHER_RECORD_LENGTHS[nextIndex]);
    }

    private void adjustNoteChordDisplay(final int inc) {
        final int nextIndex = steppedIndex(FireControlPreferences.NOTE_CHORD_DISPLAY_MODES,
                preferences.noteChordDisplay(), inc);
        preferences.setNoteChordDisplay(FireControlPreferences.NOTE_CHORD_DISPLAY_MODES[nextIndex]);
        oled.valueInfo("Note OLED", FireControlPreferences.NOTE_CHORD_DISPLAY_MODES[nextIndex]);
    }

    private int steppedIndex(final String[] values, final String current, final int inc) {
        int currentIndex = 0;
        for (int index = 0; index < values.length; index++) {
            if (values[index].equals(current)) {
                currentIndex = index;
                break;
            }
        }
        return Math.max(0, Math.min(values.length - 1, currentIndex + inc));
    }

    private String brightnessLabel() {
        return brightnessLabel(preferences.padBrightness());
    }

    private String brightnessLabel(final double value) {
        return "%.0f%%".formatted(FireControlPreferences.normalizePadBrightness(value));
    }

    private String saturationLabel() {
        return saturationLabel(preferences.padSaturation());
    }

    private String saturationLabel(final double value) {
        return "%.0f%%".formatted(FireControlPreferences.normalizePadSaturation(value));
    }

    private RgbLightState padState(final int padIndex) {
        if (padIndex == SHOW_DEACTIVATED_TRACKS_PAD) {
            return preferences.showDeactivatedTracks() ? TOGGLE_ON : TOGGLE_OFF;
        }
        final int row = padIndex / PAD_COLUMNS;
        final int column = padIndex % PAD_COLUMNS;
        if (row < 0 || row >= PAD_ROWS || column < 0 || column >= PAD_COLUMNS) {
            return RgbLightState.OFF;
        }
        return LOGO[row][column] ? LOGO_ON : LOGO_OFF;
    }

    interface Host {
        boolean browserButtonPressed();

        boolean popupBrowserActive();

        boolean shiftHeld();

        boolean altHeld();

        Mode activeMode();

        void prepareActivation();

        void restoreActiveMode();

        void refreshSurfaceLights();

        boolean consumeKnobModeGesture();

        ParameterEncoderBinding.ExplicitResetControl explicitResetControl();
    }

    static final class State {
        private final GlobalSettingsOverlayLatch latch = new GlobalSettingsOverlayLatch();
        private EncoderMode page = EncoderMode.CHANNEL;
        private boolean active;

        EncoderMode page() {
            return page;
        }

        EncoderMode advancePage() {
            page = switch (page) {
                case CHANNEL -> EncoderMode.MIXER;
                case MIXER -> EncoderMode.USER_2;
                case USER_2 -> EncoderMode.USER_1;
                default -> EncoderMode.CHANNEL;
            };
            return page;
        }

        boolean toggleLatch(final boolean available) {
            return latch.toggleLatch(available);
        }

        void closeLatch() {
            latch.close();
        }

        boolean shouldBeActive(final boolean momentaryComboHeld, final boolean popupBrowserActive) {
            return latch.shouldBeActive(momentaryComboHeld, popupBrowserActive);
        }

        boolean dismissForModeButton(final Mode activeMode,
                                     final Mode targetMode,
                                     final boolean shiftHeld,
                                     final boolean altHeld) {
            if (!active || shiftHeld || altHeld) {
                return false;
            }
            latch.close();
            active = false;
            return activeMode == targetMode;
        }

        boolean isActive() {
            return active;
        }

        void setActive(final boolean active) {
            this.active = active;
        }

        boolean isLatched() {
            return latch.isLatched();
        }
    }
}
