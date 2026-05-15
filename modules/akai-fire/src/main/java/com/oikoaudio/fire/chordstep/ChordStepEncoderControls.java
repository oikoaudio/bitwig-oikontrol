package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.control.ContinuousEncoderScaler;
import com.oikoaudio.fire.control.EncoderStepAccumulator;
import com.oikoaudio.fire.control.EncoderTouchResetHandler;
import com.oikoaudio.fire.control.EncoderValueProfile;
import com.oikoaudio.fire.control.MixerEncoderProfile;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.control.TouchResetGesture;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.sequence.EncoderBank;
import com.oikoaudio.fire.sequence.EncoderBankLayout;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.sequence.EncoderSlotBinding;
import com.oikoaudio.fire.sequence.NoteStepAccess;
import com.oikoaudio.fire.sequence.StepSequencerEncoderHandler;

import java.util.EnumMap;
import java.util.Map;

final class ChordStepEncoderControls {
    private static final int CHORD_ROOT_ENCODER_THRESHOLD = 16;
    private static final int CHORD_OCTAVE_ENCODER_THRESHOLD = 8;
    private static final int CHORD_FAMILY_ENCODER_THRESHOLD = 8;
    private static final long TOUCH_RESET_HOLD_MS = 750L;
    private static final long TOUCH_RESET_RECENT_ADJUSTMENT_SUPPRESS_MS = 300L;
    private static final int TOUCH_RESET_TOLERATED_ADJUSTMENT_UNITS = 2;

    private final AkaiFireOikontrolExtension driver;
    private final OledDisplay oled;
    private final CursorTrack cursorTrack;
    private final Host host;
    private final EncoderStepAccumulator chordRootEncoder = new EncoderStepAccumulator(CHORD_ROOT_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator chordOctaveEncoder = new EncoderStepAccumulator(CHORD_OCTAVE_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator chordFamilyEncoder = new EncoderStepAccumulator(CHORD_FAMILY_ENCODER_THRESHOLD);
    private final EncoderTouchResetHandler encoderTouchResetHandler;
    private final EncoderBankLayout layout;
    private boolean mainEncoderPressConsumed = false;

    ChordStepEncoderControls(final AkaiFireOikontrolExtension driver,
                             final OledDisplay oled,
                             final CursorTrack cursorTrack,
                             final Host host) {
        this.driver = driver;
        this.oled = oled;
        this.cursorTrack = cursorTrack;
        this.host = host;
        this.encoderTouchResetHandler = new EncoderTouchResetHandler(
                new TouchResetGesture(4, TOUCH_RESET_HOLD_MS, TOUCH_RESET_RECENT_ADJUSTMENT_SUPPRESS_MS,
                        TOUCH_RESET_TOLERATED_ADJUSTMENT_UNITS),
                driver::isEncoderTouchResetEnabled,
                (task, delayMs) -> driver.getHost().scheduleTask(task, delayMs),
                TOUCH_RESET_HOLD_MS,
                oled::clearScreenDelayed);
        this.layout = createLayout();
    }

    EncoderBankLayout layout() {
        return layout;
    }

    void bindMainEncoder(final Layer layer) {
        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.bindEncoder(layer, this::handleMainEncoder);
        mainEncoder.bindTouched(layer, this::handleMainEncoderPress);
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
            oled.valueInfo("Note Repeat", "Live only");
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
        if (!pressed && mainEncoderPressConsumed) {
            mainEncoderPressConsumed = false;
            return;
        }
        if (pressed && driver.isGlobalShiftHeld()) {
            mainEncoderPressConsumed = true;
            driver.cycleMainEncoderRolePreference();
            return;
        }
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (!pressed && !driver.wasMainEncoderTurnedWhilePressed()) {
            driver.toggleMainEncoderRolePreference();
            return;
        }
        if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                oled.valueInfo("Note Repeat", "Live only");
            }
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

    private EncoderBankLayout createLayout() {
        final Map<EncoderMode, EncoderBank> banks = new EnumMap<>(EncoderMode.class);
        banks.put(EncoderMode.CHANNEL, new EncoderBank(
                "1: Octave/Root\n2: Velocity\n3: Chord Family\n4: Interpret/Invert",
                new EncoderSlotBinding[]{
                        chordPitchContextSlot(),
                        chordBuildVelocitySlot(),
                        chordSlot(2, chordFamilyEncoder,
                                amount -> {
                                    if (driver.isGlobalAltHeld()) {
                                        host.adjustChordPage(amount);
                                    } else {
                                        host.adjustChordFamily(amount);
                                    }
                                },
                                host::showCurrentChord, host::resetChordFamilySelection),
                        interpretSlot()
                }));
        banks.put(EncoderMode.MIXER, new EncoderBank(
                "1: Volume\n2: Pan\n3: Send 1\n4: Send 2",
                new EncoderSlotBinding[]{
                        chordMixerSlot(0, "Volume"),
                        chordMixerSlot(1, "Pan"),
                        chordMixerSlot(2, "Send 1"),
                        chordMixerSlot(3, "Send 2")
                }));
        banks.put(EncoderMode.USER_1, new EncoderBank(
                "1: Velocity\n2: Pressure\n3: Timbre\n4: Pitch Expr",
                new EncoderSlotBinding[]{
                        noteAccessSlot(NoteStepAccess.VELOCITY),
                        noteAccessSlot(NoteStepAccess.PRESSURE),
                        noteAccessSlot(NoteStepAccess.TIMBRE),
                        noteAccessSlot(NoteStepAccess.PITCH)
                }));
        banks.put(EncoderMode.USER_2, new EncoderBank(
                "1: Note Length\n2: Chance\n3: Vel Spread\n4: Repeat",
                new EncoderSlotBinding[]{
                        noteAccessSlot(NoteStepAccess.DURATION),
                        noteAccessSlot(NoteStepAccess.CHANCE),
                        noteAccessSlot(NoteStepAccess.VELOCITY_SPREAD),
                        noteAccessSlot(NoteStepAccess.REPEATS)
                }));
        return new EncoderBankLayout(banks);
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

    private EncoderSlotBinding chordPitchContextSlot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return 0.25;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    final boolean rootContext = driver.isGlobalAltHeld();
                    final EncoderStepAccumulator accumulator = rootContext ? chordRootEncoder : chordOctaveEncoder;
                    final int amount = accumulator.consume(inc);
                    if (amount == 0) {
                        return;
                    }
                    handler.recordTouchAdjustment(slotIndex, Math.abs(amount));
                    encoderTouchResetHandler.markAdjusted(rootContext ? 1 : 0);
                    if (rootContext) {
                        host.adjustChordRoot(amount);
                    } else {
                        host.adjustChordOctave(amount);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        final boolean rootContext = driver.isGlobalAltHeld();
                        handler.beginTouchReset(slotIndex, () -> {
                            (rootContext ? chordRootEncoder : chordOctaveEncoder).reset();
                            if (rootContext) {
                                host.resetChordRoot();
                                host.showChordRootInfo();
                            } else {
                                host.resetChordOctave();
                                host.showChordOctaveInfo();
                            }
                        });
                        if (rootContext) {
                            host.showChordRootInfo();
                        } else {
                            host.showChordOctaveInfo();
                        }
                        return;
                    }
                    handler.endTouchReset(slotIndex);
                    oled.clearScreenDelayed();
                });
            }
        };
    }

    private EncoderSlotBinding chordBuildVelocitySlot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    handler.recordTouchAdjustment(slotIndex, Math.abs(inc));
                    if (driver.isGlobalShiftHeld()) {
                        host.adjustDefaultChordVelocity(inc);
                    } else {
                        host.adjustChordVelocitySensitivity(inc);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        handler.beginTouchReset(slotIndex, () -> {
                            host.resetChordVelocityDefaults();
                            host.showChordVelocityInfo();
                        });
                        host.showChordVelocityInfo();
                        return;
                    }
                    handler.endTouchReset(slotIndex);
                    oled.clearScreenDelayed();
                });
            }
        };
    }

    private EncoderSlotBinding chordMixerSlot(final int index, final String label) {
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
                encoder.bindContinuousEncoder(layer, driver::isGlobalShiftHeld,
                        ContinuousEncoderScaler.Profile.STRONG,
                        inc -> adjustMixerParameter(parameter, label, inc));
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

    private void adjustMixerParameter(final Parameter parameter, final String fallbackLabel, final int inc) {
        EncoderValueProfile.LARGE_RANGE.adjustParameter(parameter, driver.isGlobalShiftHeld(), inc);
        oled.valueInfo(fallbackLabel, parameter.displayedValue().get());
    }

    private EncoderSlotBinding chordSlot(final int slotIndex, final EncoderStepAccumulator accumulator,
                                         final ChordAdjuster adjuster, final Runnable showInfo,
                                         final Runnable resetAction) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return 0.25;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int boundSlotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    final int amount = accumulator.consume(inc);
                    if (amount != 0) {
                        handler.recordTouchAdjustment(boundSlotIndex, Math.abs(amount));
                        encoderTouchResetHandler.markAdjusted(slotIndex);
                        adjuster.adjust(amount);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        handler.beginTouchReset(boundSlotIndex, () -> {
                            accumulator.reset();
                            resetAction.run();
                            showInfo.run();
                        });
                        showInfo.run();
                        return;
                    }
                    handler.endTouchReset(boundSlotIndex);
                    oled.clearScreenDelayed();
                });
            }
        };
    }

    private EncoderSlotBinding interpretSlot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return 1.0;
            }

            @Override
            public void bind(final StepSequencerEncoderHandler handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    if (inc != 0) {
                        handler.recordTouchAdjustment(slotIndex, Math.abs(inc));
                        if (driver.isGlobalShiftHeld()) {
                            host.adjustChordSharedScale(inc);
                        } else if (driver.isGlobalAltHeld()) {
                            host.invertCurrentChord(inc > 0 ? 1 : -1);
                        } else {
                            host.adjustChordInterpretation(inc);
                        }
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        if (driver.isGlobalShiftHeld()) {
                            handler.beginTouchReset(slotIndex, () -> { });
                            oled.valueInfo("Scale", host.getScaleDisplayName());
                            return;
                        }
                        if (driver.isGlobalAltHeld()) {
                            handler.beginTouchReset(slotIndex, () -> { });
                            oled.valueInfo("Invert", "Turn encoder");
                            return;
                        }
                        handler.beginTouchReset(slotIndex, () -> {
                            host.resetChordInterpretation();
                            host.showChordInterpretationInfo();
                        });
                        host.showChordInterpretationInfo();
                        return;
                    }
                    handler.endTouchReset(slotIndex);
                    oled.clearScreenDelayed();
                });
            }
        };
    }

    @FunctionalInterface
    private interface ChordAdjuster {
        void adjust(int amount);
    }

    interface Host {
        void adjustChordRoot(int amount);

        void resetChordRoot();

        void showChordRootInfo();

        void adjustChordOctave(int amount);

        void resetChordOctave();

        void showChordOctaveInfo();

        void adjustDefaultChordVelocity(int inc);

        void adjustChordVelocitySensitivity(int inc);

        void resetChordVelocityDefaults();

        void showChordVelocityInfo();

        void adjustChordPage(int amount);

        void adjustChordFamily(int amount);

        void showCurrentChord();

        void resetChordFamilySelection();

        void adjustChordSharedScale(int amount);

        String getScaleDisplayName();

        void invertCurrentChord(int direction);

        void adjustChordInterpretation(int amount);

        void resetChordInterpretation();

        void showChordInterpretationInfo();
    }
}
